package net.minecraft.entity.projectile;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Uuids;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Снаряд шалкера — самонаводящийся снаряд, движущийся по осям координат.
 * <p>
 * Снаряд перемещается строго вдоль одной из осей X/Y/Z за раз, периодически
 * меняя направление через {@link #changeTargetDirection}, чтобы обойти препятствия
 * и приблизиться к цели. При попадании накладывает эффект левитации на {@link LivingEntity}.
 */
public class ShulkerBulletEntity extends ProjectileEntity {

	private static final double MOVEMENT_SPEED = 0.15;
	private static final double VELOCITY_CORRECTION_FACTOR = 0.2;
	private static final double VELOCITY_ACCELERATION_FACTOR = 1.025;
	private static final double VELOCITY_MAX = 1.0;
	private static final float ENTITY_HIT_DAMAGE = 4.0F;
	private static final int LEVITATION_DURATION_TICKS = 200;
	private static final int STEP_COUNT_BASE = 10;
	private static final int STEP_COUNT_RANDOM_RANGE = 5;
	private static final int STEP_COUNT_RANDOM_MULTIPLIER = 10;
	private static final int RANDOM_DIRECTION_ATTEMPTS = 5;
	private static final double DEFAULT_TARGET_HEIGHT_FACTOR = 0.5;
	private static final double EXPLOSION_PARTICLE_SPREAD = 0.2;
	private static final int EXPLOSION_PARTICLE_COUNT = 2;
	private static final int CRIT_PARTICLE_COUNT = 15;
	private static final double CRIT_PARTICLE_SPREAD = 0.2;
	private static final double TARGET_PROXIMITY_THRESHOLD = 2.0;
	private static final float RENDER_DISTANCE_SQUARED = 16384.0F;

	private @Nullable LazyEntityReference<Entity> target;
	private @Nullable Direction direction;
	private int stepCount;
	private double targetX;
	private double targetY;
	private double targetZ;

	public ShulkerBulletEntity(EntityType<? extends ShulkerBulletEntity> entityType, World world) {
		super(entityType, world);
		noClip = true;
	}

	public ShulkerBulletEntity(World world, LivingEntity owner, Entity target, Direction.Axis axis) {
		this(EntityType.SHULKER_BULLET, world);
		setOwner(owner);
		Vec3d center = owner.getBoundingBox().getCenter();
		refreshPositionAndAngles(center.x, center.y, center.z, getYaw(), getPitch());
		this.target = LazyEntityReference.of(target);
		direction = Direction.UP;
		changeTargetDirection(axis, target);
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.HOSTILE;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		if (target != null) {
			view.put("Target", Uuids.INT_STREAM_CODEC, target.getUuid());
		}

		view.putNullable("Dir", Direction.INDEX_CODEC, direction);
		view.putInt("Steps", stepCount);
		view.putDouble("TXD", targetX);
		view.putDouble("TYD", targetY);
		view.putDouble("TZD", targetZ);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		stepCount = view.getInt("Steps", 0);
		targetX = view.getDouble("TXD", 0.0);
		targetY = view.getDouble("TYD", 0.0);
		targetZ = view.getDouble("TZD", 0.0);
		direction = view.<Direction>read("Dir", Direction.INDEX_CODEC).orElse(null);
		target = LazyEntityReference.fromData(view, "Target");
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
	}

	/**
	 * Пересчитывает вектор движения снаряда к цели, выбирая допустимое направление
	 * вдоль одной из осей координат. Если цель недостижима напрямую — выбирается
	 * случайное свободное направление. Шаг движения сбрасывается на случайное значение.
	 *
	 * @param axis   ось, которую нельзя использовать для следующего шага (уже использованная)
	 * @param target цель, к которой движется снаряд; {@code null} — движение вниз
	 */
	private void changeTargetDirection(Direction.@Nullable Axis axis, @Nullable Entity target) {
		double targetHeightFactor = DEFAULT_TARGET_HEIGHT_FACTOR;
		BlockPos targetBlock;

		if (target == null) {
			targetBlock = getBlockPos().down();
		} else {
			targetHeightFactor = target.getHeight() * DEFAULT_TARGET_HEIGHT_FACTOR;
			targetBlock = BlockPos.ofFloored(target.getX(), target.getY() + targetHeightFactor, target.getZ());
		}

		double destX = targetBlock.getX() + DEFAULT_TARGET_HEIGHT_FACTOR;
		double destY = targetBlock.getY() + targetHeightFactor;
		double destZ = targetBlock.getZ() + DEFAULT_TARGET_HEIGHT_FACTOR;
		Direction chosenDirection = null;

		if (!targetBlock.isWithinDistance(getEntityPos(), TARGET_PROXIMITY_THRESHOLD)) {
			BlockPos currentBlock = getBlockPos();
			List<Direction> candidates = Lists.newArrayList();

			if (axis != Direction.Axis.X) {
				if (currentBlock.getX() < targetBlock.getX() && getEntityWorld().isAir(currentBlock.east())) {
					candidates.add(Direction.EAST);
				} else if (currentBlock.getX() > targetBlock.getX() && getEntityWorld().isAir(currentBlock.west())) {
					candidates.add(Direction.WEST);
				}
			}

			if (axis != Direction.Axis.Y) {
				if (currentBlock.getY() < targetBlock.getY() && getEntityWorld().isAir(currentBlock.up())) {
					candidates.add(Direction.UP);
				} else if (currentBlock.getY() > targetBlock.getY() && getEntityWorld().isAir(currentBlock.down())) {
					candidates.add(Direction.DOWN);
				}
			}

			if (axis != Direction.Axis.Z) {
				if (currentBlock.getZ() < targetBlock.getZ() && getEntityWorld().isAir(currentBlock.south())) {
					candidates.add(Direction.SOUTH);
				} else if (currentBlock.getZ() > targetBlock.getZ() && getEntityWorld().isAir(currentBlock.north())) {
					candidates.add(Direction.NORTH);
				}
			}

			chosenDirection = Direction.random(random);

			if (candidates.isEmpty()) {
				for (int attempt = RANDOM_DIRECTION_ATTEMPTS;
						!getEntityWorld().isAir(currentBlock.offset(chosenDirection)) && attempt > 0;
						attempt--) {
					chosenDirection = Direction.random(random);
				}
			} else {
				chosenDirection = candidates.get(random.nextInt(candidates.size()));
			}

			destX = getX() + chosenDirection.getOffsetX();
			destY = getY() + chosenDirection.getOffsetY();
			destZ = getZ() + chosenDirection.getOffsetZ();
		}

		direction = chosenDirection;

		double deltaX = destX - getX();
		double deltaY = destY - getY();
		double deltaZ = destZ - getZ();
		double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

		if (length == 0.0) {
			targetX = 0.0;
			targetY = 0.0;
			targetZ = 0.0;
		} else {
			targetX = deltaX / length * MOVEMENT_SPEED;
			targetY = deltaY / length * MOVEMENT_SPEED;
			targetZ = deltaZ / length * MOVEMENT_SPEED;
		}

		velocityDirty = true;
		stepCount = STEP_COUNT_BASE + random.nextInt(STEP_COUNT_RANDOM_RANGE) * STEP_COUNT_RANDOM_MULTIPLIER;
	}

	@Override
	public void checkDespawn() {
		if (getEntityWorld().getDifficulty() == Difficulty.PEACEFUL) {
			discard();
		}
	}

	@Override
	protected double getGravity() {
		return 0.04;
	}

	@Override
	public void tick() {
		super.tick();

		Entity trackedTarget = !getEntityWorld().isClient()
				? LazyEntityReference.getEntity(target, getEntityWorld())
				: null;
		HitResult hitResult = null;

		if (!getEntityWorld().isClient()) {
			if (trackedTarget == null) {
				target = null;
			}

			boolean targetInvalid = trackedTarget == null
					|| !trackedTarget.isAlive()
					|| trackedTarget instanceof PlayerEntity && trackedTarget.isSpectator();

			if (targetInvalid) {
				applyGravity();
			} else {
				targetX = MathHelper.clamp(targetX * VELOCITY_ACCELERATION_FACTOR, -VELOCITY_MAX, VELOCITY_MAX);
				targetY = MathHelper.clamp(targetY * VELOCITY_ACCELERATION_FACTOR, -VELOCITY_MAX, VELOCITY_MAX);
				targetZ = MathHelper.clamp(targetZ * VELOCITY_ACCELERATION_FACTOR, -VELOCITY_MAX, VELOCITY_MAX);

				Vec3d velocity = getVelocity();
				setVelocity(velocity.add(
						(targetX - velocity.x) * VELOCITY_CORRECTION_FACTOR,
						(targetY - velocity.y) * VELOCITY_CORRECTION_FACTOR,
						(targetZ - velocity.z) * VELOCITY_CORRECTION_FACTOR
				));
			}

			hitResult = ProjectileUtil.getCollision(this, this::canHit);
		}

		Vec3d velocity = getVelocity();
		setPosition(getEntityPos().add(velocity));
		tickBlockCollision();

		if (portalManager != null && portalManager.isInPortal()) {
			tickPortalTeleportation();
		}

		if (hitResult != null && isAlive() && hitResult.getType() != HitResult.Type.MISS) {
			hitOrDeflect(hitResult);
		}

		ProjectileUtil.setRotationFromVelocity(this, 0.5F);

		if (getEntityWorld().isClient()) {
			getEntityWorld()
					.addParticleClient(
							ParticleTypes.END_ROD,
							getX() - velocity.x,
							getY() - velocity.y + MOVEMENT_SPEED,
							getZ() - velocity.z,
							0.0,
							0.0,
							0.0
					);
		} else if (trackedTarget != null) {
			if (stepCount > 0) {
				stepCount--;
				if (stepCount == 0) {
					changeTargetDirection(direction == null ? null : direction.getAxis(), trackedTarget);
				}
			}

			if (direction != null) {
				BlockPos currentBlock = getBlockPos();
				Direction.Axis currentAxis = direction.getAxis();

				if (getEntityWorld().isTopSolid(currentBlock.offset(direction), this)) {
					changeTargetDirection(currentAxis, trackedTarget);
				} else {
					BlockPos targetBlock = trackedTarget.getBlockPos();
					boolean axisAligned = currentAxis == Direction.Axis.X && currentBlock.getX() == targetBlock.getX()
							|| currentAxis == Direction.Axis.Z && currentBlock.getZ() == targetBlock.getZ()
							|| currentAxis == Direction.Axis.Y && currentBlock.getY() == targetBlock.getY();

					if (axisAligned) {
						changeTargetDirection(currentAxis, trackedTarget);
					}
				}
			}
		}
	}

	@Override
	protected boolean shouldTickBlockCollision() {
		return !isRemoved();
	}

	@Override
	protected boolean canHit(Entity entity) {
		return super.canHit(entity) && !entity.noClip;
	}

	@Override
	public boolean isOnFire() {
		return false;
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < RENDER_DISTANCE_SQUARED;
	}

	@Override
	public float getBrightnessAtEyes() {
		return 1.0F;
	}

	@Override
	protected void onEntityHit(EntityHitResult entityHitResult) {
		super.onEntityHit(entityHitResult);
		Entity target = entityHitResult.getEntity();
		Entity ownerEntity = getOwner();
		LivingEntity livingOwner = ownerEntity instanceof LivingEntity living ? living : null;
		DamageSource damageSource = getDamageSources().mobProjectile(this, livingOwner);
		boolean damaged = target.sidedDamage(damageSource, ENTITY_HIT_DAMAGE);

		if (damaged) {
			if (getEntityWorld() instanceof ServerWorld serverWorld) {
				EnchantmentHelper.onTargetDamaged(serverWorld, target, damageSource);
			}

			if (target instanceof LivingEntity livingTarget) {
				livingTarget.addStatusEffect(
						new StatusEffectInstance(StatusEffects.LEVITATION, LEVITATION_DURATION_TICKS),
						MoreObjects.firstNonNull(ownerEntity, this)
				);
			}
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		super.onBlockHit(blockHitResult);
		((ServerWorld) getEntityWorld()).spawnParticles(
				ParticleTypes.EXPLOSION,
				getX(),
				getY(),
				getZ(),
				EXPLOSION_PARTICLE_COUNT,
				EXPLOSION_PARTICLE_SPREAD,
				EXPLOSION_PARTICLE_SPREAD,
				EXPLOSION_PARTICLE_SPREAD,
				0.0
		);
		playSound(SoundEvents.ENTITY_SHULKER_BULLET_HIT, 1.0F, 1.0F);
	}

	private void destroy() {
		discard();
		getEntityWorld().emitGameEvent(GameEvent.ENTITY_DAMAGE, getEntityPos(), GameEvent.Emitter.of(this));
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		super.onCollision(hitResult);
		destroy();
	}

	@Override
	public boolean canHit() {
		return true;
	}

	@Override
	public boolean clientDamage(DamageSource source) {
		return true;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		playSound(SoundEvents.ENTITY_SHULKER_BULLET_HURT, 1.0F, 1.0F);
		world.spawnParticles(
				ParticleTypes.CRIT,
				getX(),
				getY(),
				getZ(),
				CRIT_PARTICLE_COUNT,
				CRIT_PARTICLE_SPREAD,
				CRIT_PARTICLE_SPREAD,
				CRIT_PARTICLE_SPREAD,
				0.0
		);
		destroy();
		return true;
	}

	@Override
	public void onSpawnPacket(EntitySpawnS2CPacket packet) {
		super.onSpawnPacket(packet);
		setVelocity(packet.getVelocity());
	}
}
