package net.minecraft.entity.mob;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Моб-шалкер — прикреплённый к блоку моллюск, способный телепортироваться
 * и стрелять снарядами-шалкерами. Открывает панцирь при обнаружении цели,
 * получая уязвимость к снарядам, но теряя броню в закрытом состоянии.
 * При попадании снаряда-шалкера в открытый панцирь может породить нового шалкера,
 * если плотность популяции в радиусе {@code SPAWN_DENSITY_RADIUS} блоков позволяет.
 */
public class ShulkerEntity extends GolemEntity implements Monster {

	private static final Identifier COVERED_ARMOR_MODIFIER_ID = Identifier.ofVanilla("covered");
	private static final EntityAttributeModifier COVERED_ARMOR_BONUS = new EntityAttributeModifier(
			COVERED_ARMOR_MODIFIER_ID, 20.0, EntityAttributeModifier.Operation.ADD_VALUE
	);
	protected static final TrackedData<Direction> ATTACHED_FACE = DataTracker.registerData(
			ShulkerEntity.class, TrackedDataHandlerRegistry.FACING
	);
	protected static final TrackedData<Byte> PEEK_AMOUNT = DataTracker.registerData(
			ShulkerEntity.class, TrackedDataHandlerRegistry.BYTE
	);
	protected static final TrackedData<Byte> COLOR = DataTracker.registerData(
			ShulkerEntity.class, TrackedDataHandlerRegistry.BYTE
	);
	private static final int LOOK_RANGE = 6;
	private static final byte NO_COLOR = 16;
	private static final int ATTACK_RANGE = 8;
	private static final double ATTACK_RANGE_SQUARED = 400.0;
	private static final int TARGET_RANGE = 8;
	private static final int TARGET_PRIORITY = 5;
	private static final float OPEN_SPEED = 0.05F;
	private static final Direction DEFAULT_ATTACHED_FACE = Direction.DOWN;
	private static final int TELEPORT_LERP_TICKS = 6;
	private static final int TELEPORT_ATTEMPTS = 5;
	private static final int TELEPORT_RANDOM_RANGE = 8;
	private static final double SPAWN_DENSITY_RADIUS = 8.0;
	private static final int MAX_SHULKER_DENSITY = 5;
	static final Vector3f SOUTH_VECTOR = Util.make(() -> {
		Vec3i vec3i = Direction.SOUTH.getVector();
		return new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
	});
	private float lastOpenProgress;
	private float openProgress;
	private @Nullable BlockPos lastAttachedBlock;
	private int teleportLerpTimer;

	public ShulkerEntity(EntityType<? extends ShulkerEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 5;
		lookControl = new ShulkerEntity.ShulkerLookControl(this);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F, 0.02F, true));
		goalSelector.add(4, new ShulkerEntity.ShootBulletGoal());
		goalSelector.add(7, new ShulkerEntity.PeekGoal());
		goalSelector.add(8, new LookAroundGoal(this));
		targetSelector.add(1, new RevengeGoal(this, this.getClass()).setGroupRevenge());
		targetSelector.add(2, new ShulkerEntity.TargetPlayerGoal(this));
		targetSelector.add(3, new ShulkerEntity.TargetOtherTeamGoal(this));
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.NONE;
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.HOSTILE;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_SHULKER_AMBIENT;
	}

	@Override
	public void playAmbientSound() {
		if (isClosed()) {
			return;
		}

		super.playAmbientSound();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_SHULKER_DEATH;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return isClosed() ? SoundEvents.ENTITY_SHULKER_HURT_CLOSED : SoundEvents.ENTITY_SHULKER_HURT;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(ATTACHED_FACE, DEFAULT_ATTACHED_FACE);
		builder.add(PEEK_AMOUNT, (byte) 0);
		builder.add(COLOR, NO_COLOR);
	}

	public static DefaultAttributeContainer.Builder createShulkerAttributes() {
		return MobEntity.createMobAttributes().add(EntityAttributes.MAX_HEALTH, 30.0);
	}

	@Override
	protected BodyControl createBodyControl() {
		return new ShulkerEntity.ShulkerBodyControl(this);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setAttachedFace(view.<Direction>read("AttachFace", Direction.INDEX_CODEC).orElse(DEFAULT_ATTACHED_FACE));
		dataTracker.set(PEEK_AMOUNT, view.getByte("Peek", (byte) 0));
		dataTracker.set(COLOR, view.getByte("Color", NO_COLOR));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("AttachFace", Direction.INDEX_CODEC, getAttachedFace());
		view.putByte("Peek", dataTracker.get(PEEK_AMOUNT));
		view.putByte("Color", dataTracker.get(COLOR));
	}

	@Override
	public void tick() {
		super.tick();

		if (!getEntityWorld().isClient()
				&& !hasVehicle()
				&& !canStay(getBlockPos(), getAttachedFace())
		) {
			tryAttachOrTeleport();
		}

		if (tickOpenProgress()) {
			moveEntities();
		}

		if (getEntityWorld().isClient()) {
			if (teleportLerpTimer > 0) {
				teleportLerpTimer--;
			} else {
				lastAttachedBlock = null;
			}
		}
	}

	private void tryAttachOrTeleport() {
		Direction direction = findAttachSide(getBlockPos());

		if (direction != null) {
			setAttachedFace(direction);
			return;
		}

		tryTeleport();
	}

	@Override
	protected Box calculateDefaultBoundingBox(Vec3d pos) {
		float extraLength = getExtraLength(openProgress);
		Direction direction = getAttachedFace().getOpposite();
		return calculateBoundingBox(getScale(), direction, extraLength, pos);
	}

	private static float getExtraLength(float openProgress) {
		return 0.5F - MathHelper.sin((0.5F + openProgress) * (float) Math.PI) * 0.5F;
	}

	private boolean tickOpenProgress() {
		lastOpenProgress = openProgress;
		float targetProgress = getPeekAmount() * 0.01F;

		if (openProgress == targetProgress) {
			return false;
		}

		if (openProgress > targetProgress) {
			openProgress = MathHelper.clamp(openProgress - OPEN_SPEED, targetProgress, 1.0F);
		} else {
			openProgress = MathHelper.clamp(openProgress + OPEN_SPEED, 0.0F, targetProgress);
		}

		return true;
	}

	private void moveEntities() {
		refreshPosition();

		float currentExtra = getExtraLength(openProgress);
		float lastExtra = getExtraLength(lastOpenProgress);
		Direction direction = getAttachedFace().getOpposite();
		float pushDelta = (currentExtra - lastExtra) * getScale();

		if (pushDelta <= 0.0F) {
			return;
		}

		for (Entity entity : getEntityWorld().getOtherEntities(
				this,
				calculateBoundingBox(getScale(), direction, lastExtra, currentExtra, getEntityPos()),
				EntityPredicates.EXCEPT_SPECTATOR.negate().and(other -> other.isConnectedThroughVehicle(this))
		)) {
			if (!(entity instanceof ShulkerEntity) && !entity.noClip) {
				entity.move(
						MovementType.SHULKER,
						new Vec3d(
								pushDelta * direction.getOffsetX(),
								pushDelta * direction.getOffsetY(),
								pushDelta * direction.getOffsetZ()
						)
				);
			}
		}
	}

	/**
	 * Вычисляет bounding box шалкера с учётом направления прикрепления и степени открытия панциря.
	 * Делегирует в перегрузку с {@code lastExtraLength = -1.0F}, что означает «без предыдущего состояния».
	 *
	 * @param scale масштаб сущности
	 * @param facing направление, в котором открывается панцирь
	 * @param extraLength текущая дополнительная длина панциря (0..1)
	 * @param pos центральная позиция сущности
	 * @return итоговый bounding box в мировых координатах
	 */
	public static Box calculateBoundingBox(float scale, Direction facing, float extraLength, Vec3d pos) {
		return calculateBoundingBox(scale, facing, -1.0F, extraLength, pos);
	}

	/**
	 * Вычисляет bounding box шалкера для диапазона открытия панциря между двумя тиками.
	 * Используется при выталкивании сущностей: охватывает весь объём, пройденный панцирем
	 * между {@code lastExtraLength} и {@code extraLength}.
	 *
	 * @param scale масштаб сущности
	 * @param facing направление открытия панциря
	 * @param lastExtraLength дополнительная длина на предыдущем тике
	 * @param extraLength дополнительная длина на текущем тике
	 * @param pos центральная позиция сущности
	 * @return bounding box, охватывающий весь диапазон движения панциря
	 */
	public static Box calculateBoundingBox(
			float scale,
			Direction facing,
			float lastExtraLength,
			float extraLength,
			Vec3d pos
	) {
		Box base = new Box(-scale * 0.5, 0.0, -scale * 0.5, scale * 0.5, scale, scale * 0.5);
		double maxExtra = Math.max(lastExtraLength, extraLength);
		double minExtra = Math.min(lastExtraLength, extraLength);
		Box stretched = base
				.stretch(
						facing.getOffsetX() * maxExtra * scale,
						facing.getOffsetY() * maxExtra * scale,
						facing.getOffsetZ() * maxExtra * scale
				)
				.shrink(
						-facing.getOffsetX() * (1.0 + minExtra) * scale,
						-facing.getOffsetY() * (1.0 + minExtra) * scale,
						-facing.getOffsetZ() * (1.0 + minExtra) * scale
				);
		return stretched.offset(pos.x, pos.y, pos.z);
	}

	@Override
	public boolean startRiding(Entity entity, boolean force, boolean emitEvent) {
		if (getEntityWorld().isClient()) {
			lastAttachedBlock = null;
			teleportLerpTimer = 0;
		}

		setAttachedFace(Direction.DOWN);
		return super.startRiding(entity, force, emitEvent);
	}

	@Override
	public void stopRiding() {
		super.stopRiding();

		if (getEntityWorld().isClient()) {
			lastAttachedBlock = getBlockPos();
		}

		lastBodyYaw = 0.0F;
		bodyYaw = 0.0F;
	}

	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		setYaw(0.0F);
		headYaw = getYaw();
		resetPosition();
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	public void move(MovementType type, Vec3d movement) {
		if (type == MovementType.SHULKER_BOX) {
			tryTeleport();
			return;
		}

		super.move(type, movement);
	}

	@Override
	public Vec3d getVelocity() {
		return Vec3d.ZERO;
	}

	@Override
	public void setVelocity(Vec3d velocity) {
	}

	@Override
	public void setPosition(double x, double y, double z) {
		BlockPos prevPos = getBlockPos();

		if (hasVehicle()) {
			super.setPosition(x, y, z);
		} else {
			super.setPosition(MathHelper.floor(x) + 0.5, MathHelper.floor(y + 0.5), MathHelper.floor(z) + 0.5);
		}

		if (age == 0) {
			return;
		}

		BlockPos newPos = getBlockPos();

		if (!newPos.equals(prevPos)) {
			dataTracker.set(PEEK_AMOUNT, (byte) 0);
			velocityDirty = true;

			if (getEntityWorld().isClient() && !hasVehicle() && !newPos.equals(lastAttachedBlock)) {
				lastAttachedBlock = prevPos;
				teleportLerpTimer = TELEPORT_LERP_TICKS;
				lastRenderX = getX();
				lastRenderY = getY();
				lastRenderZ = getZ();
			}
		}
	}

	protected @Nullable Direction findAttachSide(BlockPos pos) {
		for (Direction direction : Direction.values()) {
			if (canStay(pos, direction)) {
				return direction;
			}
		}

		return null;
	}

	boolean canStay(BlockPos pos, Direction direction) {
		if (isInvalidPosition(pos)) {
			return false;
		}

		Direction opposite = direction.getOpposite();

		if (!getEntityWorld().isDirectionSolid(pos.offset(direction), this, opposite)) {
			return false;
		}

		Box box = calculateBoundingBox(getScale(), opposite, 1.0F, pos.toBottomCenterPos()).contract(1.0E-6);
		return getEntityWorld().isSpaceEmpty(this, box);
	}

	private boolean isInvalidPosition(BlockPos pos) {
		BlockState blockState = getEntityWorld().getBlockState(pos);

		if (blockState.isAir()) {
			return false;
		}

		// Шалкер может находиться внутри движущегося поршня — это единственное исключение
		return !(blockState.isOf(Blocks.MOVING_PISTON) && pos.equals(getBlockPos()));
	}

	/**
	 * Пытается телепортироваться на случайную позицию в радиусе {@code TELEPORT_RANDOM_RANGE} блоков.
	 * Делает до {@code TELEPORT_ATTEMPTS} попыток найти свободный блок воздуха с твёрдой поверхностью
	 * для прикрепления. При успехе сбрасывает цель и закрывает панцирь.
	 *
	 * @return {@code true}, если телепортация прошла успешно
	 */
	protected boolean tryTeleport() {
		if (isAiDisabled() || !isAlive()) {
			return false;
		}

		BlockPos origin = getBlockPos();

		for (int attempt = 0; attempt < TELEPORT_ATTEMPTS; attempt++) {
			BlockPos candidatePos = origin.add(
					MathHelper.nextBetween(random, -TELEPORT_RANDOM_RANGE, TELEPORT_RANDOM_RANGE),
					MathHelper.nextBetween(random, -TELEPORT_RANDOM_RANGE, TELEPORT_RANDOM_RANGE),
					MathHelper.nextBetween(random, -TELEPORT_RANDOM_RANGE, TELEPORT_RANDOM_RANGE)
			);

			if (candidatePos.getY() <= getEntityWorld().getBottomY()) {
				continue;
			}

			if (!getEntityWorld().isAir(candidatePos)) {
				continue;
			}

			if (!getEntityWorld().getWorldBorder().contains(candidatePos)) {
				continue;
			}

			if (!getEntityWorld().isSpaceEmpty(this, new Box(candidatePos).contract(1.0E-6))) {
				continue;
			}

			Direction attachSide = findAttachSide(candidatePos);

			if (attachSide == null) {
				continue;
			}

			detach();
			setAttachedFace(attachSide);
			playSound(SoundEvents.ENTITY_SHULKER_TELEPORT, 1.0F, 1.0F);
			setPosition(candidatePos.getX() + 0.5, candidatePos.getY(), candidatePos.getZ() + 0.5);
			getEntityWorld().emitGameEvent(GameEvent.TELEPORT, origin, GameEvent.Emitter.of(this));
			dataTracker.set(PEEK_AMOUNT, (byte) 0);
			setTarget(null);
			return true;
		}

		return false;
	}

	@Override
	public PositionInterpolator getInterpolator() {
		return null;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isClosed()) {
			Entity projectile = source.getSource();

			if (projectile instanceof PersistentProjectileEntity) {
				return false;
			}
		}

		if (!super.damage(world, source, amount)) {
			return false;
		}

		if (getHealth() < getMaxHealth() * 0.5F && random.nextInt(4) == 0) {
			tryTeleport();
		} else if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
			Entity sourceEntity = source.getSource();

			if (sourceEntity != null && sourceEntity.getType() == EntityType.SHULKER_BULLET) {
				spawnNewShulker();
			}
		}

		return true;
	}

	private boolean isClosed() {
		return getPeekAmount() == 0;
	}

	/**
	 * Пытается породить нового шалкера на текущей позиции после успешной телепортации.
	 * Вероятность спавна снижается линейно с ростом числа живых шалкеров в радиусе
	 * {@code SPAWN_DENSITY_RADIUS} блоков: при {@code MAX_SHULKER_DENSITY} и более — спавн невозможен.
	 */
	private void spawnNewShulker() {
		Vec3d spawnPos = getEntityPos();
		Box searchBox = getBoundingBox();

		if (isClosed() || !tryTeleport()) {
			return;
		}

		int nearbyCount = getEntityWorld()
				.getEntitiesByType(EntityType.SHULKER, searchBox.expand(SPAWN_DENSITY_RADIUS), Entity::isAlive)
				.size();
		float spawnChance = (nearbyCount - 1) / (float) MAX_SHULKER_DENSITY;

		if (getEntityWorld().random.nextFloat() >= spawnChance) {
			ShulkerEntity newShulker = EntityType.SHULKER.create(getEntityWorld(), SpawnReason.BREEDING);

			if (newShulker != null) {
				newShulker.setColor(getColorOptional());
				newShulker.refreshPositionAfterTeleport(spawnPos);
				getEntityWorld().spawnEntity(newShulker);
			}
		}
	}

	@Override
	public boolean isCollidable(@Nullable Entity entity) {
		return isAlive();
	}

	public Direction getAttachedFace() {
		return dataTracker.get(ATTACHED_FACE);
	}

	private void setAttachedFace(Direction face) {
		dataTracker.set(ATTACHED_FACE, face);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (ATTACHED_FACE.equals(data)) {
			setBoundingBox(calculateBoundingBox());
		}

		super.onTrackedDataSet(data);
	}

	private int getPeekAmount() {
		return dataTracker.get(PEEK_AMOUNT);
	}

	/**
	 * Устанавливает степень открытия панциря (0 = закрыт, 100 = полностью открыт).
	 * На сервере управляет бонусом брони: закрытый панцирь даёт +20 к броне через
	 * {@code COVERED_ARMOR_BONUS}, открытый — снимает модификатор.
	 *
	 * @param peekAmount степень открытия от 0 до 100
	 */
	void setPeekAmount(int peekAmount) {
		if (!getEntityWorld().isClient()) {
			getAttributeInstance(EntityAttributes.ARMOR).removeModifier(COVERED_ARMOR_MODIFIER_ID);

			if (peekAmount == 0) {
				getAttributeInstance(EntityAttributes.ARMOR).addPersistentModifier(COVERED_ARMOR_BONUS);
				playSound(SoundEvents.ENTITY_SHULKER_CLOSE, 1.0F, 1.0F);
				emitGameEvent(GameEvent.CONTAINER_CLOSE);
			} else {
				playSound(SoundEvents.ENTITY_SHULKER_OPEN, 1.0F, 1.0F);
				emitGameEvent(GameEvent.CONTAINER_OPEN);
			}
		}

		dataTracker.set(PEEK_AMOUNT, (byte) peekAmount);
	}

	public float getOpenProgress(float tickProgress) {
		return MathHelper.lerp(tickProgress, lastOpenProgress, openProgress);
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		bodyYaw = 0.0F;
		lastBodyYaw = 0.0F;
	}

	@Override
	public int getMaxLookPitchChange() {
		return 180;
	}

	@Override
	public int getMaxHeadRotation() {
		return 180;
	}

	@Override
	public void pushAwayFrom(Entity entity) {
	}

	/**
	 * Вычисляет смещение рендера для плавной интерполяции позиции после телепортации.
	 * Использует квадратичное затухание: смещение убывает по параболе от {@code teleportLerpTimer}
	 * до нуля за {@code TELEPORT_LERP_TICKS} тиков, создавая эффект «скольжения» к новой позиции.
	 *
	 * @param tickProgress прогресс текущего тика (0..1) для интерполяции между тиками
	 * @return вектор смещения рендера или {@code null}, если интерполяция не активна
	 */
	public @Nullable Vec3d getRenderPositionOffset(float tickProgress) {
		if (lastAttachedBlock == null || teleportLerpTimer <= 0) {
			return null;
		}

		double lerpFactor = (teleportLerpTimer - tickProgress) / (double) TELEPORT_LERP_TICKS;
		lerpFactor *= lerpFactor;
		lerpFactor *= getScale();

		BlockPos currentPos = getBlockPos();
		double offsetX = (currentPos.getX() - lastAttachedBlock.getX()) * lerpFactor;
		double offsetY = (currentPos.getY() - lastAttachedBlock.getY()) * lerpFactor;
		double offsetZ = (currentPos.getZ() - lastAttachedBlock.getZ()) * lerpFactor;
		return new Vec3d(-offsetX, -offsetY, -offsetZ);
	}

	@Override
	protected float clampScale(float scale) {
		return Math.min(scale, 3.0F);
	}

	private void setColor(Optional<DyeColor> color) {
		dataTracker.set(COLOR, color.<Byte>map(dyeColor -> (byte) dyeColor.getIndex()).orElse(NO_COLOR));
	}

	public Optional<DyeColor> getColorOptional() {
		return Optional.ofNullable(getColor());
	}

	public @Nullable DyeColor getColor() {
		byte colorIndex = dataTracker.get(COLOR);
		return colorIndex != NO_COLOR && colorIndex <= 15 ? DyeColor.byIndex(colorIndex) : null;
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.SHULKER_COLOR
				? castComponentValue((ComponentType<T>) type, getColor())
				: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.SHULKER_COLOR);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.SHULKER_COLOR) {
			setColor(Optional.of(castComponentValue(DataComponentTypes.SHULKER_COLOR, value)));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}

	class PeekGoal extends Goal {

		private int counter;

		@Override
		public boolean canStart() {
			return ShulkerEntity.this.getTarget() == null
					&& ShulkerEntity.this.random.nextInt(toGoalTicks(40)) == 0
					&& ShulkerEntity.this.canStay(
							ShulkerEntity.this.getBlockPos(),
							ShulkerEntity.this.getAttachedFace()
					);
		}

		@Override
		public boolean shouldContinue() {
			return ShulkerEntity.this.getTarget() == null && counter > 0;
		}

		@Override
		public void start() {
			counter = getTickCount(20 * (1 + ShulkerEntity.this.random.nextInt(3)));
			ShulkerEntity.this.setPeekAmount(30);
		}

		@Override
		public void stop() {
			if (ShulkerEntity.this.getTarget() == null) {
				ShulkerEntity.this.setPeekAmount(0);
			}
		}

		@Override
		public void tick() {
			counter--;
		}
	}

	class ShootBulletGoal extends Goal {

		private int counter;

		public ShootBulletGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
		}

		@Override
		public boolean canStart() {
			LivingEntity target = ShulkerEntity.this.getTarget();

			if (target == null || !target.isAlive()) {
				return false;
			}

			return ShulkerEntity.this.getEntityWorld().getDifficulty() != Difficulty.PEACEFUL;
		}

		@Override
		public void start() {
			counter = 20;
			ShulkerEntity.this.setPeekAmount(100);
		}

		@Override
		public void stop() {
			ShulkerEntity.this.setPeekAmount(0);
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (ShulkerEntity.this.getEntityWorld().getDifficulty() == Difficulty.PEACEFUL) {
				return;
			}

			counter--;
			LivingEntity target = ShulkerEntity.this.getTarget();

			if (target == null) {
				return;
			}

			ShulkerEntity.this.getLookControl().lookAt(target, 180.0F, 180.0F);
			double distSq = ShulkerEntity.this.squaredDistanceTo(target);

			if (distSq >= ATTACK_RANGE_SQUARED) {
				ShulkerEntity.this.setTarget(null);
			} else if (counter <= 0) {
				counter = 20 + ShulkerEntity.this.random.nextInt(10) * 20 / 2;
				ShulkerEntity.this.getEntityWorld().spawnEntity(
						new ShulkerBulletEntity(
								ShulkerEntity.this.getEntityWorld(),
								ShulkerEntity.this,
								target,
								ShulkerEntity.this.getAttachedFace().getAxis()
						)
				);
				ShulkerEntity.this.playSound(
						SoundEvents.ENTITY_SHULKER_SHOOT,
						2.0F,
						(ShulkerEntity.this.random.nextFloat() - ShulkerEntity.this.random.nextFloat()) * 0.2F + 1.0F
				);
			}

			super.tick();
		}
	}

	static class ShulkerBodyControl extends BodyControl {

		public ShulkerBodyControl(MobEntity mobEntity) {
			super(mobEntity);
		}

		@Override
		public void tick() {
		}
	}

	/**
	 * Управляет поворотом головы шалкера с учётом направления прикрепления.
	 * Стандартная система поворота не подходит, так как шалкер может быть прикреплён
	 * к любой грани блока — угол рассчитывается через проекцию вектора взгляда
	 * на локальные оси системы координат, определяемые направлением прикрепления.
	 */
	class ShulkerLookControl extends LookControl {

		public ShulkerLookControl(final MobEntity entity) {
			super(entity);
		}

		@Override
		protected void clampHeadYaw() {
		}

		@Override
		protected Optional<Float> getTargetYaw() {
			Direction facing = ShulkerEntity.this.getAttachedFace().getOpposite();
			Vector3f forwardVec = facing.getRotationQuaternion().transform(new Vector3f(ShulkerEntity.SOUTH_VECTOR));
			Vec3i facingVec = facing.getVector();
			Vector3f sideVec = new Vector3f(facingVec.getX(), facingVec.getY(), facingVec.getZ());
			sideVec.cross(forwardVec);

			double deltaX = x - entity.getX();
			double deltaY = y - entity.getEyeY();
			double deltaZ = z - entity.getZ();
			Vector3f toTarget = new Vector3f((float) deltaX, (float) deltaY, (float) deltaZ);

			float sideComponent = sideVec.dot(toTarget);
			float forwardComponent = forwardVec.dot(toTarget);

			if (Math.abs(sideComponent) <= 1.0E-5F && Math.abs(forwardComponent) <= 1.0E-5F) {
				return Optional.empty();
			}

			return Optional.of((float) (MathHelper.atan2(-sideComponent, forwardComponent) * 180.0F / (float) Math.PI));
		}

		@Override
		protected Optional<Float> getTargetPitch() {
			return Optional.of(0.0F);
		}
	}

	static class TargetOtherTeamGoal extends ActiveTargetGoal<LivingEntity> {

		public TargetOtherTeamGoal(ShulkerEntity shulker) {
			super(shulker, LivingEntity.class, 10, true, false, (entity, world) -> entity instanceof Monster);
		}

		@Override
		public boolean canStart() {
			return mob.getScoreboardTeam() != null && super.canStart();
		}

		@Override
		protected Box getSearchBox(double distance) {
			Direction direction = ((ShulkerEntity) mob).getAttachedFace();

			if (direction.getAxis() == Direction.Axis.X) {
				return mob.getBoundingBox().expand(4.0, distance, distance);
			}

			return direction.getAxis() == Direction.Axis.Z
					? mob.getBoundingBox().expand(distance, distance, 4.0)
					: mob.getBoundingBox().expand(distance, 4.0, distance);
		}
	}

	class TargetPlayerGoal extends ActiveTargetGoal<PlayerEntity> {

		public TargetPlayerGoal(final ShulkerEntity shulker) {
			super(shulker, PlayerEntity.class, true);
		}

		@Override
		public boolean canStart() {
			return ShulkerEntity.this.getEntityWorld().getDifficulty() != Difficulty.PEACEFUL
					&& super.canStart();
		}

		@Override
		protected Box getSearchBox(double distance) {
			Direction direction = ((ShulkerEntity) mob).getAttachedFace();

			if (direction.getAxis() == Direction.Axis.X) {
				return mob.getBoundingBox().expand(4.0, distance, distance);
			}

			return direction.getAxis() == Direction.Axis.Z
					? mob.getBoundingBox().expand(distance, distance, 4.0)
					: mob.getBoundingBox().expand(distance, 4.0, distance);
		}
	}
}
