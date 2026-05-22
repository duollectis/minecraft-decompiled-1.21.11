package net.minecraft.entity.projectile;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.OminousItemSpawnerEntity;
import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Базовый класс для снарядов, которые остаются в мире после попадания (стрелы, трезубцы).
 * <p>
 * Управляет физикой полёта, застреванием в блоках, пробиванием сущностей,
 * подбором игроком и логикой критического удара.
 */
public abstract class PersistentProjectileEntity extends ProjectileEntity {

	private static final double DEFAULT_DAMAGE = 2.0;
	private static final int SHAKE_TICKS_ON_HIT = 7;
	private static final float DRAG_IN_WATER = 0.6F;
	private static final float DRAG_IN_AIR = 0.99F;
	private static final int MAX_LIFE_TICKS = 1200;
	private static final int PIERCE_INITIAL_CAPACITY = 5;
	private static final float CRITICAL_PARTICLE_STEP = 4.0F;
	private static final float BLOCK_EMBED_OFFSET = 0.05F;
	private static final float FALL_VELOCITY_FACTOR = 0.2F;
	private static final float SPACE_EMPTY_CHECK_EXPAND = 0.06F;
	private static final double SLOW_VELOCITY_THRESHOLD = 1.0E-7;
	private static final float FIRE_DURATION_ON_HIT = 5.0F;
	private static final float SOUND_PITCH_BASE = 0.9F;
	private static final float SOUND_PITCH_RANGE = 0.2F;
	private static final int CRITICAL_FLAG = 1;
	private static final int NO_CLIP_FLAG = 2;

	private static final TrackedData<Byte> PROJECTILE_FLAGS =
		DataTracker.registerData(PersistentProjectileEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final TrackedData<Byte> PIERCE_LEVEL =
		DataTracker.registerData(PersistentProjectileEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final TrackedData<Boolean> IN_GROUND =
		DataTracker.registerData(PersistentProjectileEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

	private @Nullable BlockState inBlockState;
	protected int inGroundTime;
	public PickupPermission pickupType = PickupPermission.DISALLOWED;
	public int shake;
	private int life;
	private double damage = DEFAULT_DAMAGE;
	private SoundEvent sound = getHitSound();
	private @Nullable IntOpenHashSet piercedEntities;
	private @Nullable List<Entity> piercingKilledEntities;
	private ItemStack stack = getDefaultItemStack();
	private @Nullable ItemStack weapon;

	protected PersistentProjectileEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
		super(entityType, world);
	}

	/**
	 * Создаёт снаряд в заданной позиции с привязкой к стеку предмета и оружию.
	 * Если оружие задано — считывает уровень пробивания из зачарований.
	 *
	 * @param type   тип сущности
	 * @param x      позиция X
	 * @param y      позиция Y
	 * @param z      позиция Z
	 * @param world  мир
	 * @param stack  стек предмета-снаряда
	 * @param weapon стек оружия, из которого выпущен снаряд (может быть null)
	 */
	protected PersistentProjectileEntity(
		EntityType<? extends PersistentProjectileEntity> type,
		double x,
		double y,
		double z,
		World world,
		ItemStack stack,
		@Nullable ItemStack weapon
	) {
		this(type, world);
		this.stack = stack.copy();
		copyComponentsFrom(stack);
		Unit intangibleTag = stack.remove(DataComponentTypes.INTANGIBLE_PROJECTILE);
		if (intangibleTag != null) {
			pickupType = PickupPermission.CREATIVE_ONLY;
		}

		setPosition(x, y, z);
		if (weapon != null && world instanceof ServerWorld serverWorld) {
			if (weapon.isEmpty()) {
				throw new IllegalArgumentException("Invalid weapon firing an arrow");
			}

			this.weapon = weapon.copy();
			int pierceLevel = EnchantmentHelper.getProjectilePiercing(serverWorld, weapon, this.stack);
			if (pierceLevel > 0) {
				setPierceLevel((byte) pierceLevel);
			}
		}
	}

	protected PersistentProjectileEntity(
		EntityType<? extends PersistentProjectileEntity> type,
		LivingEntity owner,
		World world,
		ItemStack stack,
		@Nullable ItemStack shotFrom
	) {
		this(type, owner.getX(), owner.getEyeY() - 0.1F, owner.getZ(), world, stack, shotFrom);
		setOwner(owner);
	}

	public void setSound(SoundEvent sound) {
		this.sound = sound;
	}

	@Override
	public boolean shouldRender(double distance) {
		double sideLen = getBoundingBox().getAverageSideLength() * 10.0;
		if (Double.isNaN(sideLen)) {
			sideLen = 1.0;
		}

		sideLen *= 64.0 * getRenderDistanceMultiplier();
		return distance < sideLen * sideLen;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(PROJECTILE_FLAGS, (byte) 0);
		builder.add(PIERCE_LEVEL, (byte) 0);
		builder.add(IN_GROUND, false);
	}

	@Override
	public void setVelocity(double x, double y, double z, float power, float uncertainty) {
		super.setVelocity(x, y, z, power, uncertainty);
		life = 0;
	}

	@Override
	public void setVelocityClient(Vec3d clientVelocity) {
		super.setVelocityClient(clientVelocity);
		life = 0;
		if (isInGround() && clientVelocity.lengthSquared() > 0.0) {
			setInGround(false);
		}
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		super.onTrackedDataSet(data);
		if (!firstUpdate && shake <= 0 && data.equals(IN_GROUND) && isInGround()) {
			shake = SHAKE_TICKS_ON_HIT;
		}
	}

	@Override
	public void tick() {
		boolean hasCollision = !isNoClip();
		Vec3d velocity = getVelocity();
		BlockPos blockPos = getBlockPos();
		BlockState blockState = getEntityWorld().getBlockState(blockPos);

		// Если снаряд оказался внутри блока — останавливаем его
		if (!blockState.isAir() && hasCollision) {
			VoxelShape shape = blockState.getCollisionShape(getEntityWorld(), blockPos);
			if (!shape.isEmpty()) {
				Vec3d pos = getEntityPos();
				for (Box box : shape.getBoundingBoxes()) {
					if (box.offset(blockPos).contains(pos)) {
						setVelocity(Vec3d.ZERO);
						setInGround(true);
						break;
					}
				}
			}
		}

		if (shake > 0) {
			shake--;
		}

		if (isTouchingWaterOrRain()) {
			extinguish();
		}

		if (isInGround() && hasCollision) {
			if (!getEntityWorld().isClient()) {
				if (inBlockState != blockState && shouldFall()) {
					fall();
				} else {
					age();
				}
			}

			inGroundTime++;
			if (isAlive()) {
				tickBlockCollision();
			}

			if (!getEntityWorld().isClient()) {
				setOnFire(getFireTicks() > 0);
			}
		} else {
			inGroundTime = 0;
			Vec3d pos = getEntityPos();

			if (isTouchingWater()) {
				applyDrag(getDragInWater());
				spawnBubbleParticles(pos);
			}

			if (isCritical()) {
				for (int step = 0; step < CRITICAL_PARTICLE_STEP; step++) {
					getEntityWorld().addParticleClient(
						ParticleTypes.CRIT,
						pos.x + velocity.x * step / CRITICAL_PARTICLE_STEP,
						pos.y + velocity.y * step / CRITICAL_PARTICLE_STEP,
						pos.z + velocity.z * step / CRITICAL_PARTICLE_STEP,
						-velocity.x,
						-velocity.y + 0.2,
						-velocity.z
					);
				}
			}

			float yawAngle = hasCollision
				? (float) (MathHelper.atan2(velocity.x, velocity.z) * 180.0F / (float) Math.PI)
				: (float) (MathHelper.atan2(-velocity.x, -velocity.z) * 180.0F / (float) Math.PI);
			float pitchAngle = (float) (MathHelper.atan2(velocity.y, velocity.horizontalLength()) * 180.0F / (float) Math.PI);
			setPitch(updateRotation(getPitch(), pitchAngle));
			setYaw(updateRotation(getYaw(), yawAngle));
			tickLeftOwner();

			if (hasCollision) {
				BlockHitResult blockHit = getEntityWorld().getCollisionsIncludingWorldBorder(
					new RaycastContext(
						pos,
						pos.add(velocity),
						RaycastContext.ShapeType.COLLIDER,
						RaycastContext.FluidHandling.NONE,
						this
					)
				);
				applyCollision(blockHit);
			} else {
				setPosition(pos.add(velocity));
				tickBlockCollision();
			}

			if (!isTouchingWater()) {
				applyDrag(DRAG_IN_AIR);
			}

			if (hasCollision && !isInGround()) {
				applyGravity();
			}

			super.tick();
		}
	}

	/**
	 * Обрабатывает столкновения снаряда с блоком и сущностями на пути.
	 * Для пробивающих снарядов продолжает движение через сущности до исчерпания уровня пробивания.
	 *
	 * @param blockHit результат трассировки луча по блокам
	 */
	private void applyCollision(BlockHitResult blockHit) {
		while (isAlive()) {
			Vec3d pos = getEntityPos();
			List<EntityHitResult> entityHits = collectPiercingCollisions(pos, blockHit.getPos())
				.stream()
				.sorted(Comparator.comparingDouble(hit -> pos.squaredDistanceTo(hit.getEntity().getEntityPos())))
				.collect(java.util.stream.Collectors.toList());

			Vec3d targetPos = Objects.requireNonNullElse(
				entityHits.isEmpty() ? null : entityHits.getFirst(),
				blockHit
			).getPos();
			setPosition(targetPos);
			tickBlockCollision(pos, targetPos);

			if (portalManager != null && portalManager.isInPortal()) {
				tickPortalTeleportation();
			}

			if (entityHits.isEmpty()) {
				if (isAlive() && blockHit.getType() != HitResult.Type.MISS) {
					hitOrDeflect(blockHit);
					velocityDirty = true;
				}

				break;
			}

			if (isAlive() && !noClip) {
				ProjectileDeflection deflection = hitOrDeflect(entityHits);
				velocityDirty = true;
				if (getPierceLevel() > 0 && deflection == ProjectileDeflection.NONE) {
					continue;
				}
			}

			break;
		}
	}

	private ProjectileDeflection hitOrDeflect(Collection<EntityHitResult> hitResults) {
		for (EntityHitResult entityHit : hitResults) {
			ProjectileDeflection deflection = hitOrDeflect(entityHit);
			if (!isAlive() || deflection != ProjectileDeflection.NONE) {
				return deflection;
			}
		}

		return ProjectileDeflection.NONE;
	}

	private void applyDrag(float drag) {
		setVelocity(getVelocity().multiply(drag));
	}

	private void spawnBubbleParticles(Vec3d pos) {
		Vec3d velocity = getVelocity();
		float bubbleOffset = 0.25F;
		for (int i = 0; i < 4; i++) {
			getEntityWorld().addParticleClient(
				ParticleTypes.BUBBLE,
				pos.x - velocity.x * bubbleOffset,
				pos.y - velocity.y * bubbleOffset,
				pos.z - velocity.z * bubbleOffset,
				velocity.x,
				velocity.y,
				velocity.z
			);
		}
	}

	@Override
	protected double getGravity() {
		return 0.05;
	}

	/**
	 * Проверяет, должен ли снаряд выпасть из блока (блок исчез или снаряд в пустом пространстве).
	 */
	private boolean shouldFall() {
		return isInGround() && getEntityWorld().isSpaceEmpty(
			new Box(getEntityPos(), getEntityPos()).expand(SPACE_EMPTY_CHECK_EXPAND)
		);
	}

	/**
	 * Выбивает снаряд из блока с небольшим случайным импульсом.
	 */
	private void fall() {
		setInGround(false);
		Vec3d velocity = getVelocity();
		setVelocity(velocity.multiply(
			random.nextFloat() * FALL_VELOCITY_FACTOR,
			random.nextFloat() * FALL_VELOCITY_FACTOR,
			random.nextFloat() * FALL_VELOCITY_FACTOR
		));
		life = 0;
	}

	protected boolean isInGround() {
		return dataTracker.get(IN_GROUND);
	}

	protected void setInGround(boolean inGround) {
		dataTracker.set(IN_GROUND, inGround);
	}

	@Override
	public boolean isPushedByFluids() {
		return !isInGround();
	}

	@Override
	public void move(MovementType type, Vec3d movement) {
		super.move(type, movement);
		if (type != MovementType.SELF && shouldFall()) {
			fall();
		}
	}

	/**
	 * Увеличивает счётчик времени жизни снаряда в блоке.
	 * По истечении {@value #MAX_LIFE_TICKS} тиков снаряд удаляется.
	 */
	protected void age() {
		life++;
		if (life >= MAX_LIFE_TICKS) {
			discard();
		}
	}

	private void clearPiercingStatus() {
		if (piercingKilledEntities != null) {
			piercingKilledEntities.clear();
		}

		if (piercedEntities != null) {
			piercedEntities.clear();
		}
	}

	@Override
	public void onBroken(Item item) {
		weapon = null;
	}

	@Override
	public void onBubbleColumnSurfaceCollision(boolean drag, BlockPos pos) {
		if (!isInGround()) {
			super.onBubbleColumnSurfaceCollision(drag, pos);
		}
	}

	@Override
	public void onBubbleColumnCollision(boolean drag) {
		if (!isInGround()) {
			super.onBubbleColumnCollision(drag);
		}
	}

	@Override
	public void addVelocity(double deltaX, double deltaY, double deltaZ) {
		if (!isInGround()) {
			super.addVelocity(deltaX, deltaY, deltaZ);
		}
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		Entity target = entityHitResult.getEntity();
		float speed = (float) getVelocity().length();
		double baseDamage = damage;
		Entity ownerEntity = getOwner();
		DamageSource damageSource = getDamageSources().arrow(this, ownerEntity != null ? ownerEntity : this);

		if (getWeaponStack() != null && getEntityWorld() instanceof ServerWorld serverWorld) {
			baseDamage = EnchantmentHelper.getDamage(serverWorld, getWeaponStack(), target, damageSource, (float) baseDamage);
		}

		int rawDamage = MathHelper.ceil(MathHelper.clamp(speed * baseDamage, 0.0, 2.147483647E9));

		if (getPierceLevel() > 0) {
			if (piercedEntities == null) {
				piercedEntities = new IntOpenHashSet(PIERCE_INITIAL_CAPACITY);
			}

			if (piercingKilledEntities == null) {
				piercingKilledEntities = Lists.newArrayListWithCapacity(PIERCE_INITIAL_CAPACITY);
			}

			if (piercedEntities.size() >= getPierceLevel() + 1) {
				discard();
				return;
			}

			piercedEntities.add(target.getId());
		}

		if (isCritical()) {
			long critBonus = random.nextInt(rawDamage / 2 + 2);
			rawDamage = (int) Math.min(critBonus + rawDamage, 2147483647L);
		}

		if (ownerEntity instanceof LivingEntity livingOwner) {
			livingOwner.onAttacking(target);
		}

		boolean isEnderman = target.getType() == EntityType.ENDERMAN;
		int prevFireTicks = target.getFireTicks();
		if (isOnFire() && !isEnderman) {
			target.setOnFireFor(FIRE_DURATION_ON_HIT);
		}

		if (target.sidedDamage(damageSource, rawDamage)) {
			if (isEnderman) {
				return;
			}

			if (target instanceof LivingEntity livingTarget) {
				if (!getEntityWorld().isClient() && getPierceLevel() <= 0) {
					livingTarget.setStuckArrowCount(livingTarget.getStuckArrowCount() + 1);
				}

				knockback(livingTarget, damageSource);

				if (getEntityWorld() instanceof ServerWorld serverWorld) {
					EnchantmentHelper.onTargetDamaged(serverWorld, livingTarget, damageSource, getWeaponStack());
				}

				onHit(livingTarget);

				if (livingTarget instanceof PlayerEntity
					&& ownerEntity instanceof ServerPlayerEntity serverOwner
					&& !isSilent()
					&& livingTarget != serverOwner
				) {
					serverOwner.networkHandler.sendPacket(
						new GameStateChangeS2CPacket(GameStateChangeS2CPacket.PROJECTILE_HIT_PLAYER, 0.0F)
					);
				}

				if (!target.isAlive() && piercingKilledEntities != null) {
					piercingKilledEntities.add(livingTarget);
				}

				if (!getEntityWorld().isClient() && ownerEntity instanceof ServerPlayerEntity serverOwner) {
					if (piercingKilledEntities != null) {
						Criteria.KILLED_BY_ARROW.trigger(serverOwner, piercingKilledEntities, weapon);
					} else if (!target.isAlive()) {
						Criteria.KILLED_BY_ARROW.trigger(serverOwner, List.of(target), weapon);
					}
				}
			}

			playSound(sound, 1.0F, SOUND_PITCH_BASE + random.nextFloat() * SOUND_PITCH_RANGE);
			if (getPierceLevel() <= 0) {
				discard();
			}
		} else {
			target.setFireTicks(prevFireTicks);
			deflect(ProjectileDeflection.SIMPLE, target, owner, false);
			setVelocity(getVelocity().multiply(0.2));
			if (getEntityWorld() instanceof ServerWorld serverWorld
				&& getVelocity().lengthSquared() < SLOW_VELOCITY_THRESHOLD
			) {
				if (pickupType == PickupPermission.ALLOWED) {
					dropStack(serverWorld, asItemStack(), 0.1F);
				}

				discard();
			}
		}
	}

	/**
	 * Применяет отбрасывание к цели на основе зачарования «Отдача» на оружии.
	 * Учитывает сопротивление отбрасыванию цели.
	 *
	 * @param target цель
	 * @param source источник урона
	 */
	protected void knockback(LivingEntity target, DamageSource source) {
		double knockbackStrength = weapon != null && getEntityWorld() instanceof ServerWorld serverWorld
			? EnchantmentHelper.modifyKnockback(serverWorld, weapon, target, source, 0.0F)
			: 0.0F;
		if (knockbackStrength <= 0.0) {
			return;
		}

		double resistanceFactor = Math.max(0.0, 1.0 - target.getAttributeValue(EntityAttributes.KNOCKBACK_RESISTANCE));
		Vec3d knockbackVec = getVelocity()
			.multiply(1.0, 0.0, 1.0)
			.normalize()
			.multiply(knockbackStrength * DRAG_IN_WATER * resistanceFactor);
		if (knockbackVec.lengthSquared() > 0.0) {
			target.addVelocity(knockbackVec.x, 0.1, knockbackVec.z);
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		inBlockState = getEntityWorld().getBlockState(blockHitResult.getBlockPos());
		super.onBlockHit(blockHitResult);
		ItemStack weaponStack = getWeaponStack();
		if (getEntityWorld() instanceof ServerWorld serverWorld && weaponStack != null) {
			onBlockHitEnchantmentEffects(serverWorld, blockHitResult, weaponStack);
		}

		Vec3d velocity = getVelocity();
		Vec3d embedOffset = new Vec3d(
			Math.signum(velocity.x),
			Math.signum(velocity.y),
			Math.signum(velocity.z)
		).multiply(BLOCK_EMBED_OFFSET);
		setPosition(getEntityPos().subtract(embedOffset));
		setVelocity(Vec3d.ZERO);
		playSound(getSound(), 1.0F, SOUND_PITCH_BASE + random.nextFloat() * SOUND_PITCH_RANGE);
		setInGround(true);
		shake = SHAKE_TICKS_ON_HIT;
		setCritical(false);
		setPierceLevel((byte) 0);
		setSound(SoundEvents.ENTITY_ARROW_HIT);
		clearPiercingStatus();
	}

	/**
	 * Применяет эффекты зачарований при попадании снаряда в блок.
	 * Вызывается только на сервере.
	 *
	 * @param world       серверный мир
	 * @param blockHit    результат попадания в блок
	 * @param weaponStack стек оружия
	 */
	protected void onBlockHitEnchantmentEffects(
		ServerWorld world,
		BlockHitResult blockHit,
		ItemStack weaponStack
	) {
		Vec3d hitPos = blockHit.getBlockPos().clampToWithin(blockHit.getPos());
		EnchantmentHelper.onHitBlock(
			world,
			weaponStack,
			getOwner() instanceof LivingEntity livingOwner ? livingOwner : null,
			this,
			null,
			hitPos,
			world.getBlockState(blockHit.getBlockPos()),
			item -> weapon = null
		);
	}

	@Override
	public @Nullable ItemStack getWeaponStack() {
		return weapon;
	}

	protected SoundEvent getHitSound() {
		return SoundEvents.ENTITY_ARROW_HIT;
	}

	protected final SoundEvent getSound() {
		return sound;
	}

	protected void onHit(LivingEntity target) {
	}

	protected @Nullable EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
		return ProjectileUtil.getEntityCollision(
			getEntityWorld(),
			this,
			currentPosition,
			nextPosition,
			getBoundingBox().stretch(getVelocity()).expand(1.0),
			this::canHit
		);
	}

	/**
	 * Собирает все сущности, которые снаряд пронизывает на пути от {@code from} до {@code to}.
	 * Используется для пробивающих снарядов.
	 *
	 * @param from начальная точка
	 * @param to   конечная точка
	 * @return коллекция результатов попаданий
	 */
	protected Collection<EntityHitResult> collectPiercingCollisions(Vec3d from, Vec3d to) {
		return ProjectileUtil.collectPiercingCollisions(
			getEntityWorld(),
			this,
			from,
			to,
			getBoundingBox().stretch(getVelocity()).expand(1.0),
			this::canHit,
			false
		);
	}

	@Override
	protected boolean canHit(Entity entity) {
		if (entity instanceof PlayerEntity targetPlayer && getOwner() instanceof PlayerEntity ownerPlayer
			&& !ownerPlayer.shouldDamagePlayer(targetPlayer)
		) {
			return false;
		}

		return super.canHit(entity)
			&& (piercedEntities == null || !piercedEntities.contains(entity.getId()));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putShort("life", (short) life);
		view.putNullable("inBlockState", BlockState.CODEC, inBlockState);
		view.putByte("shake", (byte) shake);
		view.putBoolean("inGround", isInGround());
		view.put("pickup", PickupPermission.CODEC, pickupType);
		view.putDouble("damage", damage);
		view.putBoolean("crit", isCritical());
		view.putByte("PierceLevel", getPierceLevel());
		view.put("SoundEvent", Registries.SOUND_EVENT.getCodec(), sound);
		view.put("item", ItemStack.CODEC, stack);
		view.putNullable("weapon", ItemStack.CODEC, weapon);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		life = view.getShort("life", (short) 0);
		inBlockState = view.<BlockState>read("inBlockState", BlockState.CODEC).orElse(null);
		shake = view.getByte("shake", (byte) 0) & 255;
		setInGround(view.getBoolean("inGround", false));
		damage = view.getDouble("damage", DEFAULT_DAMAGE);
		pickupType = view.<PickupPermission>read("pickup", PickupPermission.CODEC)
			.orElse(PickupPermission.DISALLOWED);
		setCritical(view.getBoolean("crit", false));
		setPierceLevel(view.getByte("PierceLevel", (byte) 0));
		sound = view.<SoundEvent>read("SoundEvent", Registries.SOUND_EVENT.getCodec()).orElse(getHitSound());
		setStack(view.<ItemStack>read("item", ItemStack.CODEC).orElse(getDefaultItemStack()));
		weapon = view.<ItemStack>read("weapon", ItemStack.CODEC).orElse(null);
	}

	/**
		* Обновляет тип разрешения подбора при смене владельца.
		* Если владелец — игрок и подбор был запрещён, разрешает его.
		* Если владелец — {@link OminousItemSpawnerEntity}, запрещает подбор.
		*/
	@Override
	public void setOwner(@Nullable Entity owner) {
		super.setOwner(owner);
		pickupType = switch (owner) {
			case PlayerEntity ignored when pickupType == PickupPermission.DISALLOWED ->
				PickupPermission.ALLOWED;
			case OminousItemSpawnerEntity ignored ->
				PickupPermission.DISALLOWED;
			case null, default -> pickupType;
		};
	}

	@Override
	public void onPlayerCollision(PlayerEntity player) {
		if (!getEntityWorld().isClient() && (isInGround() || isNoClip()) && shake <= 0) {
			if (tryPickup(player)) {
				player.sendPickup(this, 1);
				discard();
			}
		}
	}

	/**
		* Пытается добавить снаряд в инвентарь игрока согласно правилам подбора.
		*
		* @param player игрок, подбирающий снаряд
		* @return true если снаряд успешно подобран
		*/
	protected boolean tryPickup(PlayerEntity player) {
		return switch (pickupType) {
			case DISALLOWED -> false;
			case ALLOWED -> player.getInventory().insertStack(asItemStack());
			case CREATIVE_ONLY -> player.isInCreativeMode();
		};
	}

	/**
		* Возвращает копию стека предмета этого снаряда для подбора или дропа.
		*/
	protected ItemStack asItemStack() {
		return stack.copy();
	}

	protected abstract ItemStack getDefaultItemStack();

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.NONE;
	}

	public ItemStack getItemStack() {
		return stack;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	@Override
	public boolean isAttackable() {
		return getType().isIn(EntityTypeTags.REDIRECTABLE_PROJECTILE);
	}

	public void setCritical(boolean critical) {
		setProjectileFlag(CRITICAL_FLAG, critical);
	}

	private void setPierceLevel(byte level) {
		dataTracker.set(PIERCE_LEVEL, level);
	}

	private void setProjectileFlag(int index, boolean flag) {
		byte flags = dataTracker.get(PROJECTILE_FLAGS);
		dataTracker.set(PROJECTILE_FLAGS, flag
			? (byte) (flags | index)
			: (byte) (flags & ~index)
		);
	}

	protected void setStack(ItemStack stack) {
		this.stack = stack.isEmpty() ? getDefaultItemStack() : stack;
	}

	public boolean isCritical() {
		return (dataTracker.get(PROJECTILE_FLAGS) & CRITICAL_FLAG) != 0;
	}

	public byte getPierceLevel() {
		return dataTracker.get(PIERCE_LEVEL);
	}

	/**
		* Применяет модификатор урона с учётом сложности мира.
		* Итоговый урон = {@code damageModifier * 2 + случайная добавка по сложности}.
		*
		* @param damageModifier базовый множитель урона (обычно зависит от силы натяжения лука)
		*/
	public void applyDamageModifier(float damageModifier) {
		setDamage(damageModifier * 2.0F + random.nextTriangular(
			getEntityWorld().getDifficulty().getId() * 0.11,
			0.57425
		));
	}

	protected float getDragInWater() {
		return DRAG_IN_WATER;
	}

	public void setNoClip(boolean noClip) {
		this.noClip = noClip;
		setProjectileFlag(NO_CLIP_FLAG, noClip);
	}

	/**
		* Проверяет режим прохождения сквозь блоки.
		* На сервере использует поле {@code noClip}, на клиенте — синхронизированный флаг.
		*/
	public boolean isNoClip() {
		return !getEntityWorld().isClient()
			? noClip
			: (dataTracker.get(PROJECTILE_FLAGS) & NO_CLIP_FLAG) != 0;
	}

	@Override
	public boolean canHit() {
		return super.canHit() && !isInGround();
	}

	@Override
	public @Nullable StackReference getStackReference(int slot) {
		return slot == 0 ? StackReference.of(this::getItemStack, this::setStack) : super.getStackReference(slot);
	}

	@Override
	protected boolean deflectsAgainstWorldBorder() {
		return true;
	}

	/**
		* Разрешения на подбор снаряда игроком.
		*/
	public enum PickupPermission {
		DISALLOWED,
		ALLOWED,
		CREATIVE_ONLY;

		public static final Codec<PickupPermission> CODEC = Codec.BYTE.xmap(
			PickupPermission::fromOrdinal,
			permission -> (byte) permission.ordinal()
		);

		public static PickupPermission fromOrdinal(int ordinal) {
			if (ordinal < 0 || ordinal > values().length) {
				ordinal = 0;
			}

			return values()[ordinal];
		}
	}
}
