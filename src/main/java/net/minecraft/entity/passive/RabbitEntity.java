package net.minecraft.entity.passive;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarrotsBlock;
import net.minecraft.component.ComponentType;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.control.JumpControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.Util;
import net.minecraft.util.function.ValueLists;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.rule.GameRules;
import org.jspecify.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * Кролик — прыгающее пассивное существо с несколькими вариантами окраски.
 * Особый вариант {@link Variant#EVIL} (Кролик-убийца) агрессивен и атакует игроков.
 * Использует кастомный {@link RabbitJumpControl} и {@link RabbitMoveControl} для прыжковой локомоции.
 */
public class RabbitEntity extends AnimalEntity {

	public static final double WALK_SPEED = 0.6;
	public static final double RUN_SPEED = 0.8;
	public static final double SPRINT_SPEED = 1.0;
	public static final double ESCAPE_DANGER_SPEED = 2.2;
	public static final double MELEE_ATTACK_SPEED = 1.4;

	private static final TrackedData<Integer> VARIANT = DataTracker.registerData(
			RabbitEntity.class,
			TrackedDataHandlerRegistry.INTEGER
	);
	private static final Identifier KILLER_BUNNY = Identifier.ofVanilla("killer_bunny");
	private static final Identifier KILLER_BUNNY_ATTACK_DAMAGE_MODIFIER_ID = Identifier.ofVanilla("evil");
	private static final int MIN_JUMP_TICKS = 3;
	private static final int MAX_JUMP_TICKS = 5;
	private static final int FLEE_RADIUS = 8;
	private static final int JUMP_COOLDOWN_TICKS = 40;
	private static final int JUMP_DURATION = 10;
	private static final double ATTACK_RANGE_SQUARED = 16.0;
	private static final int CARROT_TICKS_RESET = 0;
	private static final double WATER_SPEED_MULTIPLIER = 1.5;
	private static final int SLOW_JUMP_COOLDOWN = 10;
	private static final int FAST_JUMP_COOLDOWN = 1;

	private int jumpTicks;
	private int jumpDuration;
	private boolean lastOnGround;
	private int ticksUntilJump;
	int moreCarrotTicks = CARROT_TICKS_RESET;

	public RabbitEntity(EntityType<? extends RabbitEntity> entityType, World world) {
		super(entityType, world);
		jumpControl = new RabbitJumpControl(this);
		moveControl = new RabbitMoveControl(this);
		setSpeed(0.0);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new SwimGoal(this));
		goalSelector.add(1, new PowderSnowJumpGoal(this, getEntityWorld()));
		goalSelector.add(1, new EscapeDangerGoal(this, ESCAPE_DANGER_SPEED));
		goalSelector.add(2, new AnimalMateGoal(this, RUN_SPEED));
		goalSelector.add(3, new TemptGoal(this, 1.0, stack -> stack.isIn(ItemTags.RABBIT_FOOD), false));
		goalSelector.add(4, new FleeGoal<>(this, PlayerEntity.class, 8.0F, ESCAPE_DANGER_SPEED, ESCAPE_DANGER_SPEED));
		goalSelector.add(4, new FleeGoal<>(this, WolfEntity.class, 10.0F, ESCAPE_DANGER_SPEED, ESCAPE_DANGER_SPEED));
		goalSelector.add(4, new FleeGoal<>(this, HostileEntity.class, 4.0F, ESCAPE_DANGER_SPEED, ESCAPE_DANGER_SPEED));
		goalSelector.add(5, new EatCarrotCropGoal(this));
		goalSelector.add(6, new WanderAroundFarGoal(this, WALK_SPEED));
		goalSelector.add(11, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
	}

	@Override
	protected float getJumpVelocity() {
		float velocity = 0.3F;
		if (moveControl.getSpeed() <= WALK_SPEED) {
			velocity = 0.2F;
		}

		Path path = navigation.getCurrentPath();
		if (path != null && !path.isFinished()) {
			Vec3d nodePos = path.getNodePosition(this);
			if (nodePos.y > getY() + 0.5) {
				velocity = 0.5F;
			}
		}

		if (horizontalCollision || jumping && moveControl.getTargetY() > getY() + 0.5) {
			velocity = 0.5F;
		}

		return super.getJumpVelocity(velocity / 0.42F);
	}

	@Override
	public void jump() {
		super.jump();
		double speed = moveControl.getSpeed();
		if (speed > 0.0) {
			double horizontalSpeedSq = getVelocity().horizontalLengthSquared();
			if (horizontalSpeedSq < 0.01) {
				updateVelocity(0.1F, new Vec3d(0.0, 0.0, 1.0));
			}
		}

		if (!getEntityWorld().isClient()) {
			getEntityWorld().sendEntityStatus(this, (byte) 1);
		}
	}

	public float getJumpProgress(float tickProgress) {
		return jumpDuration == 0 ? 0.0F : (jumpTicks + tickProgress) / jumpDuration;
	}

	public void setSpeed(double speed) {
		getNavigation().setSpeed(speed);
		moveControl.moveTo(
				moveControl.getTargetX(),
				moveControl.getTargetY(),
				moveControl.getTargetZ(),
				speed
		);
	}

	@Override
	public void setJumping(boolean jumping) {
		super.setJumping(jumping);
		if (jumping) {
			playSound(
					getJumpSound(),
					getSoundVolume(),
					((random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F) * (float) RUN_SPEED
			);
		}
	}

	/**
	 * Запускает анимацию прыжка: устанавливает флаг прыжка и длительность анимации.
	 */
	public void startJump() {
		setJumping(true);
		jumpDuration = JUMP_DURATION;
		jumpTicks = 0;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(VARIANT, Variant.DEFAULT.index);
	}

	@Override
	public void mobTick(ServerWorld world) {
		if (ticksUntilJump > 0) {
			ticksUntilJump--;
		}

		if (moreCarrotTicks > 0) {
			moreCarrotTicks = moreCarrotTicks - random.nextInt(3);
			if (moreCarrotTicks < 0) {
				moreCarrotTicks = CARROT_TICKS_RESET;
			}
		}

		if (isOnGround()) {
			if (!lastOnGround) {
				setJumping(false);
				scheduleJump();
			}

			if (getVariant() == Variant.EVIL && ticksUntilJump == 0) {
				LivingEntity target = getTarget();
				if (target != null && squaredDistanceTo(target) < ATTACK_RANGE_SQUARED) {
					lookTowards(target.getX(), target.getZ());
					moveControl.moveTo(target.getX(), target.getY(), target.getZ(), moveControl.getSpeed());
					startJump();
					lastOnGround = true;
				}
			}

			RabbitJumpControl rabbitJumpControl = (RabbitJumpControl) jumpControl;
			if (rabbitJumpControl.isActive()) {
				if (!rabbitJumpControl.canJump()) {
					enableJump();
				}
			}
			else if (moveControl.isMoving() && ticksUntilJump == 0) {
				Path path = navigation.getCurrentPath();
				Vec3d target = new Vec3d(
						moveControl.getTargetX(),
						moveControl.getTargetY(),
						moveControl.getTargetZ()
				);
				if (path != null && !path.isFinished()) {
					target = path.getNodePosition(this);
				}

				lookTowards(target.x, target.z);
				startJump();
			}
		}

		lastOnGround = isOnGround();
	}

	@Override
	public boolean shouldSpawnSprintingParticles() {
		return false;
	}

	private void lookTowards(double x, double z) {
		setYaw((float) (MathHelper.atan2(z - getZ(), x - getX()) * 180.0F / (float) Math.PI) - 90.0F);
	}

	private void enableJump() {
		((RabbitJumpControl) jumpControl).setCanJump(true);
	}

	private void disableJump() {
		((RabbitJumpControl) jumpControl).setCanJump(false);
	}

	private void doScheduleJump() {
		ticksUntilJump = moveControl.getSpeed() < ESCAPE_DANGER_SPEED ? SLOW_JUMP_COOLDOWN : FAST_JUMP_COOLDOWN;
	}

	private void scheduleJump() {
		doScheduleJump();
		disableJump();
	}

	@Override
	public void tickMovement() {
		super.tickMovement();
		if (jumpTicks != jumpDuration) {
			jumpTicks++;
		}
		else if (jumpDuration != 0) {
			jumpTicks = 0;
			jumpDuration = 0;
			setJumping(false);
		}
	}

	public static DefaultAttributeContainer.Builder createRabbitAttributes() {
		return AnimalEntity.createAnimalAttributes()
				.add(EntityAttributes.MAX_HEALTH, 3.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.3F)
				.add(EntityAttributes.ATTACK_DAMAGE, 3.0);
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.put("RabbitType", Variant.INDEX_CODEC, getVariant());
		view.putInt("MoreCarrotTicks", moreCarrotTicks);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		setVariant(view.<Variant>read("RabbitType", Variant.INDEX_CODEC).orElse(Variant.DEFAULT));
		moreCarrotTicks = view.getInt("MoreCarrotTicks", 0);
	}

	protected SoundEvent getJumpSound() {
		return SoundEvents.ENTITY_RABBIT_JUMP;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_RABBIT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_RABBIT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_RABBIT_DEATH;
	}

	@Override
	public void playAttackSound() {
		if (getVariant() == Variant.EVIL) {
			playSound(
					SoundEvents.ENTITY_RABBIT_ATTACK,
					1.0F,
					(random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F
			);
		}
	}

	@Override
	public SoundCategory getSoundCategory() {
		return getVariant() == Variant.EVIL ? SoundCategory.HOSTILE : SoundCategory.NEUTRAL;
	}

	/**
	 * Создаёт детёныша кролика при размножении.
	 * С вероятностью 1/20 выбирает случайный вариант из биома, иначе наследует от одного из родителей.
	 *
	 * @param serverWorld мир сервера
	 * @param passiveEntity второй родитель
	 * @return новый кролик или {@code null} если создание не удалось
	 */
	@Override
	public @Nullable RabbitEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
		RabbitEntity child = EntityType.RABBIT.create(serverWorld, SpawnReason.BREEDING);
		if (child == null) {
			return null;
		}

		Variant variant = getVariantFromPos(serverWorld, getBlockPos());
		if (random.nextInt(20) != 0) {
			variant = (passiveEntity instanceof RabbitEntity otherRabbit && random.nextBoolean())
					? otherRabbit.getVariant()
					: getVariant();
		}

		child.setVariant(variant);
		return child;
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.isIn(ItemTags.RABBIT_FOOD);
	}

	public Variant getVariant() {
		return Variant.byIndex(dataTracker.get(VARIANT));
	}

	/**
	 * Устанавливает вариант кролика. Для {@link Variant#EVIL} добавляет броню, цели атаки и модификатор урона.
	 */
	private void setVariant(Variant variant) {
		if (variant == Variant.EVIL) {
			getAttributeInstance(EntityAttributes.ARMOR).setBaseValue(8.0);
			goalSelector.add(4, new MeleeAttackGoal(this, MELEE_ATTACK_SPEED, true));
			targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
			targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
			targetSelector.add(2, new ActiveTargetGoal<>(this, WolfEntity.class, true));
			getAttributeInstance(EntityAttributes.ATTACK_DAMAGE)
					.updateModifier(new EntityAttributeModifier(
							KILLER_BUNNY_ATTACK_DAMAGE_MODIFIER_ID,
							5.0,
							EntityAttributeModifier.Operation.ADD_VALUE
					));
			if (!hasCustomName()) {
				setCustomName(Text.translatable(Util.createTranslationKey("entity", KILLER_BUNNY)));
			}
		}
		else {
			getAttributeInstance(EntityAttributes.ATTACK_DAMAGE)
					.removeModifier(KILLER_BUNNY_ATTACK_DAMAGE_MODIFIER_ID);
		}

		dataTracker.set(VARIANT, variant.index);
	}

	@Override
	public <T> @Nullable T get(ComponentType<? extends T> type) {
		return type == DataComponentTypes.RABBIT_VARIANT
				? castComponentValue((ComponentType<T>) type, getVariant())
				: super.get(type);
	}

	@Override
	protected void copyComponentsFrom(ComponentsAccess from) {
		copyComponentFrom(from, DataComponentTypes.RABBIT_VARIANT);
		super.copyComponentsFrom(from);
	}

	@Override
	protected <T> boolean setApplicableComponent(ComponentType<T> type, T value) {
		if (type == DataComponentTypes.RABBIT_VARIANT) {
			setVariant(castComponentValue(DataComponentTypes.RABBIT_VARIANT, value));
			return true;
		}

		return super.setApplicableComponent(type, value);
	}

	/**
	 * Определяет вариант кролика при спавне на основе биома.
	 * Снежные биомы → белый/белый с пятнами, пустынные → золотой, остальные → коричневый/соляной/чёрный.
	 */
	@Override
	public @Nullable EntityData initialize(
			ServerWorldAccess world,
			LocalDifficulty difficulty,
			SpawnReason spawnReason,
			@Nullable EntityData entityData
	) {
		Variant variant = getVariantFromPos(world, getBlockPos());
		if (entityData instanceof RabbitData rabbitData) {
			variant = rabbitData.variant;
		}
		else {
			entityData = new RabbitData(variant);
		}

		setVariant(variant);
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	private static Variant getVariantFromPos(WorldAccess world, BlockPos pos) {
		RegistryEntry<Biome> biome = world.getBiome(pos);
		int roll = world.getRandom().nextInt(100);

		if (biome.isIn(BiomeTags.SPAWNS_WHITE_RABBITS)) {
			return roll < 80 ? Variant.WHITE : Variant.WHITE_SPLOTCHED;
		}

		if (biome.isIn(BiomeTags.SPAWNS_GOLD_RABBITS)) {
			return Variant.GOLD;
		}

		return roll < 50 ? Variant.BROWN : (roll < 90 ? Variant.SALT : Variant.BLACK);
	}

	public static boolean canSpawn(
			EntityType<RabbitEntity> entity,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return world.getBlockState(pos.down()).isIn(BlockTags.RABBITS_SPAWNABLE_ON)
				&& isLightLevelValidForNaturalSpawn(world, pos);
	}

	boolean wantsCarrots() {
		return moreCarrotTicks <= 0;
	}

	@Override
	public void handleStatus(byte status) {
		if (status == 1) {
			spawnSprintingParticles();
			jumpDuration = JUMP_DURATION;
			jumpTicks = 0;
		}
		else {
			super.handleStatus(status);
		}
	}

	@Override
	public Vec3d getLeashOffset() {
		return new Vec3d(0.0, WALK_SPEED * getStandingEyeHeight(), getWidth() * 0.4F);
	}

	/**
	 * Цель ИИ: поиск и поедание моркови на грядках.
	 * Уменьшает возраст моркови на 1 или уничтожает её, если она ещё не выросла.
	 */
	static class EatCarrotCropGoal extends MoveToTargetPosGoal {

		private final RabbitEntity rabbit;
		private boolean wantsCarrots;
		private boolean hasTarget;

		public EatCarrotCropGoal(RabbitEntity rabbit) {
			super(rabbit, 0.7F, 16);
			this.rabbit = rabbit;
		}

		@Override
		public boolean canStart() {
			if (cooldown <= 0) {
				if (!getServerWorld(rabbit).getGameRules().getValue(GameRules.DO_MOB_GRIEFING)) {
					return false;
				}

				hasTarget = false;
				wantsCarrots = rabbit.wantsCarrots();
			}

			return super.canStart();
		}

		@Override
		public boolean shouldContinue() {
			return hasTarget && super.shouldContinue();
		}

		@Override
		public void tick() {
			super.tick();
			rabbit.getLookControl().lookAt(
					targetPos.getX() + 0.5,
					targetPos.getY() + 1,
					targetPos.getZ() + 0.5,
					10.0F,
					rabbit.getMaxLookPitchChange()
			);

			if (!hasReached()) {
				return;
			}

			World world = rabbit.getEntityWorld();
			BlockPos blockPos = targetPos.up();
			BlockState blockState = world.getBlockState(blockPos);
			Block block = blockState.getBlock();

			if (hasTarget && block instanceof CarrotsBlock) {
				int age = blockState.get(CarrotsBlock.AGE);
				if (age == 0) {
					world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 2);
					world.breakBlock(blockPos, true, rabbit);
				}
				else {
					world.setBlockState(blockPos, blockState.with(CarrotsBlock.AGE, age - 1), 2);
					world.emitGameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Emitter.of(rabbit));
					world.syncWorldEvent(2001, blockPos, Block.getRawIdFromState(blockState));
				}

				rabbit.moreCarrotTicks = JUMP_COOLDOWN_TICKS;
			}

			hasTarget = false;
			cooldown = SLOW_JUMP_COOLDOWN;
		}

		@Override
		protected boolean isTargetPos(WorldView world, BlockPos pos) {
			BlockState blockState = world.getBlockState(pos);
			if (!blockState.isOf(Blocks.FARMLAND) || !wantsCarrots || hasTarget) {
				return false;
			}

			BlockState above = world.getBlockState(pos.up());
			if (above.getBlock() instanceof CarrotsBlock carrotsBlock && carrotsBlock.isMature(above)) {
				hasTarget = true;
				return true;
			}

			return false;
		}
	}

	/**
	 * Цель ИИ: побег от опасности с обновлением скорости кролика.
	 */
	static class EscapeDangerGoal extends net.minecraft.entity.ai.goal.EscapeDangerGoal {

		private final RabbitEntity rabbit;

		public EscapeDangerGoal(RabbitEntity rabbit, double speed) {
			super(rabbit, speed);
			this.rabbit = rabbit;
		}

		@Override
		public void tick() {
			super.tick();
			rabbit.setSpeed(speed);
		}
	}

	/**
	 * Цель ИИ: бегство от определённого типа существ.
	 * Неактивна для варианта {@link Variant#EVIL}.
	 */
	static class FleeGoal<T extends LivingEntity> extends FleeEntityGoal<T> {

		private final RabbitEntity rabbit;

		public FleeGoal(
				RabbitEntity rabbit,
				Class<T> fleeFromType,
				float distance,
				double slowSpeed,
				double fastSpeed
		) {
			super(rabbit, fleeFromType, distance, slowSpeed, fastSpeed);
			this.rabbit = rabbit;
		}

		@Override
		public boolean canStart() {
			return rabbit.getVariant() != Variant.EVIL && super.canStart();
		}
	}

	/**
	 * Данные спавна кролика с фиксированным вариантом для всего помёта.
	 */
	public static class RabbitData extends PassiveEntity.PassiveData {

		public final Variant variant;

		public RabbitData(Variant variant) {
			super(1.0F);
			this.variant = variant;
		}
	}

	/**
	 * Контроллер прыжков кролика.
	 * Делегирует запуск прыжка в {@link RabbitEntity#startJump()} при активации.
	 */
	public static class RabbitJumpControl extends JumpControl {

		private final RabbitEntity rabbit;
		private boolean canJump;

		public RabbitJumpControl(RabbitEntity rabbit) {
			super(rabbit);
			this.rabbit = rabbit;
		}

		public boolean isActive() {
			return active;
		}

		public boolean canJump() {
			return canJump;
		}

		public void setCanJump(boolean canJump) {
			this.canJump = canJump;
		}

		@Override
		public void tick() {
			if (!active) {
				return;
			}

			rabbit.startJump();
			active = false;
		}
	}

	/**
	 * Контроллер движения кролика.
	 * Обнуляет скорость при приземлении и применяет ускорение в воде.
	 */
	static class RabbitMoveControl extends MoveControl {

		private final RabbitEntity rabbit;
		private double rabbitSpeed;

		public RabbitMoveControl(RabbitEntity owner) {
			super(owner);
			rabbit = owner;
		}

		@Override
		public void tick() {
			if (rabbit.isOnGround()
					&& !rabbit.jumping
					&& !((RabbitJumpControl) rabbit.jumpControl).isActive()
			) {
				rabbit.setSpeed(0.0);
			}
			else if (isMoving() || state == MoveControl.State.JUMPING) {
				rabbit.setSpeed(rabbitSpeed);
			}

			super.tick();
		}

		@Override
		public void moveTo(double x, double y, double z, double speed) {
			if (rabbit.isTouchingWater()) {
				speed = WATER_SPEED_MULTIPLIER;
			}

			super.moveTo(x, y, z, speed);
			if (speed > 0.0) {
				rabbitSpeed = speed;
			}
		}
	}

	/**
	 * Варианты окраски кролика. Индекс используется для сериализации в NBT (устаревший формат).
	 */
	public enum Variant implements StringIdentifiable {
		BROWN(0, "brown"),
		WHITE(1, "white"),
		BLACK(2, "black"),
		WHITE_SPLOTCHED(3, "white_splotched"),
		GOLD(4, "gold"),
		SALT(5, "salt"),
		EVIL(99, "evil");

		public static final Variant DEFAULT = BROWN;
		private static final IntFunction<Variant> INDEX_MAPPER = ValueLists.createIndexToValueFunction(
				Variant::getIndex,
				values(),
				DEFAULT
		);
		public static final Codec<Variant> CODEC = StringIdentifiable.createCodec(Variant::values);
		@Deprecated
		public static final Codec<Variant> INDEX_CODEC = Codec.INT.xmap(
				INDEX_MAPPER::apply,
				Variant::getIndex
		);
		public static final PacketCodec<ByteBuf, Variant> PACKET_CODEC = PacketCodecs.indexed(
				INDEX_MAPPER,
				Variant::getIndex
		);

		final int index;
		private final String id;

		Variant(int index, String id) {
			this.index = index;
			this.id = id;
		}

		@Override
		public String asString() {
			return id;
		}

		public int getIndex() {
			return index;
		}

		public static Variant byIndex(int index) {
			return INDEX_MAPPER.apply(index);
		}
	}
}
