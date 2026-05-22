package net.minecraft.entity.mob;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.BooleanSupplier;

/**
 * Гаст — летающий моб Нижнего мира, стреляющий огненными шарами.
 * Атакует игроков в радиусе 64 блоков по вертикали ±4 блока.
 * Отражённый игроком огненный шар наносит 1000 урона (мгновенная смерть).
 */
public class GhastEntity extends MobEntity implements Monster {

	private static final TrackedData<Boolean>
			SHOOTING =
			DataTracker.registerData(GhastEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private static final byte DEFAULT_FIREBALL_STRENGTH = 1;
	private int fireballStrength = 1;

	public GhastEntity(EntityType<? extends GhastEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 5;
		moveControl = new GhastEntity.GhastMoveControl(this, false, () -> false);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(5, new GhastEntity.FlyRandomlyGoal(this));
		goalSelector.add(7, new GhastEntity.LookAtTargetGoal(this));
		goalSelector.add(7, new GhastEntity.ShootFireballGoal(this));
		targetSelector.add(
			1,
			new ActiveTargetGoal<>(
				this,
				PlayerEntity.class,
				10,
				true,
				false,
				(entity, world) -> Math.abs(entity.getY() - getY()) <= 4.0
			)
		);
	}

	public boolean isShooting() {
		return dataTracker.get(SHOOTING);
	}

	public void setShooting(boolean shooting) {
		dataTracker.set(SHOOTING, shooting);
	}

	public int getFireballStrength() {
		return fireballStrength;
	}

	private static boolean isFireballFromPlayer(DamageSource damageSource) {
		return damageSource.getSource() instanceof FireballEntity && damageSource.getAttacker() instanceof PlayerEntity;
	}

	@Override
	public boolean isInvulnerableTo(ServerWorld world, DamageSource source) {
		return isInvulnerable() && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)
			|| !isFireballFromPlayer(source) && super.isInvulnerableTo(world, source);
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
	}

	@Override
	public boolean isClimbing() {
		return false;
	}

	@Override
	public void travel(Vec3d movementInput) {
		travelFlying(movementInput, 0.02F);
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (isFireballFromPlayer(source)) {
			super.damage(world, source, 1000.0F);
			return true;
		}

		return isInvulnerableTo(world, source) ? false : super.damage(world, source, amount);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(SHOOTING, false);
	}

	public static DefaultAttributeContainer.Builder createGhastAttributes() {
		return MobEntity.createMobAttributes()
		                .add(EntityAttributes.MAX_HEALTH, 10.0)
		                .add(EntityAttributes.FOLLOW_RANGE, 100.0)
		                .add(EntityAttributes.CAMERA_DISTANCE, 8.0)
		                .add(EntityAttributes.FLYING_SPEED, 0.06);
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.HOSTILE;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_GHAST_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_GHAST_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_GHAST_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 5.0F;
	}

	public static boolean canSpawn(
			EntityType<GhastEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		return world.getDifficulty() != Difficulty.PEACEFUL && random.nextInt(20) == 0 && canMobSpawn(
				type,
				world,
				spawnReason,
				pos,
				random
		);
	}

	@Override
	public int getLimitPerChunk() {
		return 1;
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putByte("ExplosionPower", (byte) fireballStrength);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		fireballStrength = view.getByte("ExplosionPower", (byte) DEFAULT_FIREBALL_STRENGTH);
	}

	@Override
	public boolean hasQuadLeashAttachmentPoints() {
		return true;
	}

	@Override
	public double getElasticLeashDistance() {
		return 10.0;
	}

	@Override
	public double getLeashSnappingDistance() {
		return 16.0;
	}

	/**
	 * Поворачивает гаста в сторону цели или по вектору скорости, если цели нет.
	 * Используется как в основном классе, так и в {@link LookAtTargetGoal}.
	 */
	public static void updateYaw(MobEntity ghast) {
		LivingEntity target = ghast.getTarget();

		if (target == null) {
			Vec3d velocity = ghast.getVelocity();
			ghast.setYaw(-((float) MathHelper.atan2(velocity.x, velocity.z)) * (180.0F / (float) Math.PI));
			ghast.bodyYaw = ghast.getYaw();
			return;
		}

		if (target.squaredDistanceTo(ghast) < 4096.0) {
			double dx = target.getX() - ghast.getX();
			double dz = target.getZ() - ghast.getZ();
			ghast.setYaw(-((float) MathHelper.atan2(dx, dz)) * (180.0F / (float) Math.PI));
			ghast.bodyYaw = ghast.getYaw();
		}
	}

	public static class FlyRandomlyGoal extends Goal {

		private static final int MAX_RANDOM_ATTEMPTS = 64;
		private final MobEntity ghast;
		private final int blockCheckDistance;

		public FlyRandomlyGoal(MobEntity ghast) {
			this(ghast, 0);
		}

		public FlyRandomlyGoal(MobEntity ghast, int blockCheckDistance) {
			this.ghast = ghast;
			this.blockCheckDistance = blockCheckDistance;
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		@Override
		public boolean canStart() {
			MoveControl moveControl = ghast.getMoveControl();

			if (!moveControl.isMoving()) {
				return true;
			}

			double dx = moveControl.getTargetX() - ghast.getX();
			double dy = moveControl.getTargetY() - ghast.getY();
			double dz = moveControl.getTargetZ() - ghast.getZ();
			double distSq = dx * dx + dy * dy + dz * dz;

			return distSq < 1.0 || distSq > 3600.0;
		}

		@Override
		public boolean shouldContinue() {
			return false;
		}

		@Override
		public void start() {
			Vec3d target = locateTarget(ghast, blockCheckDistance);
			ghast.getMoveControl().moveTo(target.getX(), target.getY(), target.getZ(), 1.0);
		}

		/**
		 * Ищет случайную точку полёта для гаста с учётом ограничений позиции и высоты рельефа.
		 * Если за {@code MAX_RANDOM_ATTEMPTS} попыток не найдена валидная точка — берётся последняя случайная.
		 */
		public static Vec3d locateTarget(MobEntity ghast, int blockCheckDistance) {
			World world = ghast.getEntityWorld();
			Random random = ghast.getRandom();
			Vec3d origin = ghast.getEntityPos();
			Vec3d candidate = null;

			for (int i = 0; i < MAX_RANDOM_ATTEMPTS; i++) {
				candidate = getTargetPos(ghast, origin, random);

				if (candidate != null && isTargetValid(world, candidate, blockCheckDistance)) {
					return candidate;
				}
			}

			if (candidate == null) {
				candidate = addRandom(origin, random);
			}

			BlockPos candidatePos = BlockPos.ofFloored(candidate);
			int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, candidatePos.getX(), candidatePos.getZ());

			if (topY < candidatePos.getY() && topY > world.getBottomY()) {
				candidate = new Vec3d(candidate.getX(), ghast.getY() - Math.abs(ghast.getY() - candidate.getY()), candidate.getZ());
			}

			return candidate;
		}

		private static boolean isTargetValid(World world, Vec3d pos, int blockCheckDistance) {
			if (blockCheckDistance <= 0) {
				return true;
			}

			BlockPos blockPos = BlockPos.ofFloored(pos);

			if (!world.getBlockState(blockPos).isAir()) {
				return false;
			}

			for (Direction direction : Direction.values()) {
				for (int i = 1; i < blockCheckDistance; i++) {
					BlockPos neighbor = blockPos.offset(direction, i);

					if (!world.getBlockState(neighbor).isAir()) {
						return true;
					}
				}
			}

			return false;
		}

		private static Vec3d addRandom(Vec3d pos, Random random) {
			double x = pos.getX() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
			double y = pos.getY() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;
			double z = pos.getZ() + (random.nextFloat() * 2.0F - 1.0F) * 16.0F;

			return new Vec3d(x, y, z);
		}

		private static @Nullable Vec3d getTargetPos(MobEntity ghast, Vec3d pos, Random random) {
			Vec3d candidate = addRandom(pos, random);

			return ghast.hasPositionTarget() && !ghast.isInPositionTargetRange(candidate) ? null : candidate;
		}
	}

	public static class GhastMoveControl extends MoveControl {

		private final MobEntity ghast;
		private int collisionCheckCooldown;
		private final boolean happy;
		private final BooleanSupplier shouldStayStill;

		public GhastMoveControl(MobEntity ghast, boolean happy, BooleanSupplier shouldStayStill) {
			super(ghast);
			this.ghast = ghast;
			this.happy = happy;
			this.shouldStayStill = shouldStayStill;
		}

		@Override
		public void tick() {
			if (shouldStayStill.getAsBoolean()) {
				state = MoveControl.State.WAIT;
				ghast.stopMovement();
			}

			if (state != MoveControl.State.MOVE_TO) {
				return;
			}

			if (--collisionCheckCooldown <= 0) {
				collisionCheckCooldown = collisionCheckCooldown + ghast.getRandom().nextInt(5) + 2;
				Vec3d movement = new Vec3d(
					targetX - ghast.getX(),
					targetY - ghast.getY(),
					targetZ - ghast.getZ()
				);

				if (willCollide(movement)) {
					ghast.setVelocity(
						ghast.getVelocity()
							.add(movement
								.normalize()
								.multiply(ghast.getAttributeValue(EntityAttributes.FLYING_SPEED) * 5.0 / 3.0))
					);
				} else {
					state = MoveControl.State.WAIT;
				}
			}
		}

		private boolean willCollide(Vec3d movement) {
			Box box = ghast.getBoundingBox();
			Box expandedBox = box.offset(movement);

			if (happy) {
				for (BlockPos blockPos : BlockPos.iterate(expandedBox.expand(1.0))) {
					if (!canPassThrough(ghast.getEntityWorld(), null, null, blockPos, false, false)) {
						return false;
					}
				}
			}

			boolean inWater = ghast.isTouchingWater();
			boolean inLava = ghast.isInLava();
			Vec3d fromPos = ghast.getEntityPos();
			Vec3d toPos = fromPos.add(movement);

			return BlockView.collectCollisionsBetween(
				fromPos,
				toPos,
				expandedBox,
				(pos, version) -> box.contains(pos)
					? true
					: canPassThrough(ghast.getEntityWorld(), fromPos, toPos, pos, inWater, inLava)
			);
		}

		private boolean canPassThrough(
			BlockView world,
			@Nullable Vec3d oldPos,
			@Nullable Vec3d newPos,
			BlockPos blockPos,
			boolean waterAllowed,
			boolean lavaAllowed
		) {
			BlockState blockState = world.getBlockState(blockPos);

			if (blockState.isAir()) {
				return true;
			}

			boolean hasPositions = oldPos != null && newPos != null;
			boolean noCollision = hasPositions
				? !ghast.collides(
					oldPos,
					newPos,
					blockState.getCollisionShape(world, blockPos).offset(new Vec3d(blockPos)).getBoundingBoxes()
				)
				: blockState.getCollisionShape(world, blockPos).isEmpty();

			if (!happy) {
				return noCollision;
			}

			if (blockState.isIn(BlockTags.HAPPY_GHAST_AVOIDS)) {
				return false;
			}

			FluidState fluidState = world.getFluidState(blockPos);

			if (!fluidState.isEmpty() && (!hasPositions || ghast.collidesWithFluid(fluidState, blockPos, oldPos, newPos))) {
				if (fluidState.isIn(FluidTags.WATER)) {
					return waterAllowed;
				}

				if (fluidState.isIn(FluidTags.LAVA)) {
					return lavaAllowed;
				}
			}

			return noCollision;
		}
	}

	public static class LookAtTargetGoal extends Goal {

		private final MobEntity ghast;

		public LookAtTargetGoal(MobEntity ghast) {
			this.ghast = ghast;
			setControls(EnumSet.of(Goal.Control.LOOK));
		}

		@Override
		public boolean canStart() {
			return true;
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			GhastEntity.updateYaw(ghast);
		}
	}

	static class ShootFireballGoal extends Goal {

		private final GhastEntity ghast;
		public int cooldown;

		public ShootFireballGoal(GhastEntity ghast) {
			this.ghast = ghast;
		}

		@Override
		public boolean canStart() {
			return ghast.getTarget() != null;
		}

		@Override
		public void start() {
			cooldown = 0;
		}

		@Override
		public void stop() {
			ghast.setShooting(false);
		}

		@Override
		public boolean shouldRunEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = ghast.getTarget();

			if (target == null) {
				return;
			}

			if (target.squaredDistanceTo(ghast) < 4096.0 && ghast.canSee(target)) {
				World world = ghast.getEntityWorld();
				cooldown++;

				if (cooldown == 10 && !ghast.isSilent()) {
					world.syncWorldEvent(null, 1015, ghast.getBlockPos(), 0);
				}

				if (cooldown == 20) {
					Vec3d rotationVec = ghast.getRotationVec(1.0F);
					double dx = target.getX() - (ghast.getX() + rotationVec.x * 4.0);
					double dy = target.getBodyY(0.5) - (0.5 + ghast.getBodyY(0.5));
					double dz = target.getZ() - (ghast.getZ() + rotationVec.z * 4.0);

					if (!ghast.isSilent()) {
						world.syncWorldEvent(null, 1016, ghast.getBlockPos(), 0);
					}

					FireballEntity fireball = new FireballEntity(
						world,
						ghast,
						new Vec3d(dx, dy, dz).normalize(),
						ghast.getFireballStrength()
					);
					fireball.setPosition(
						ghast.getX() + rotationVec.x * 4.0,
						ghast.getBodyY(0.5) + 0.5,
						fireball.getZ() + rotationVec.z * 4.0
					);
					world.spawnEntity(fireball);
					cooldown = -40;
				}
			} else if (cooldown > 0) {
				cooldown--;
			}

			ghast.setShooting(cooldown > 10);
		}
	}
}
