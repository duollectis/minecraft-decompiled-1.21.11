package net.minecraft.entity.mob;

import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.control.BodyControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Фантом — летающий ночной моб, атакующий игроков, которые не спали более 3 дней.
 * Боится кошек. Кружит над целью, затем пикирует. Получает урон от солнечного света.
 */
public class PhantomEntity extends MobEntity implements Monster {

	public static final float WING_FLAP_ANGULAR_VELOCITY = 7.448451F;
	public static final int WING_FLAP_TICKS = MathHelper.ceil(24.166098F);
	private static final int CAT_CHECK_INTERVAL = 20;
	private static final TrackedData<Integer>
			SIZE =
			DataTracker.registerData(PhantomEntity.class, TrackedDataHandlerRegistry.INTEGER);
	Vec3d targetPosition = Vec3d.ZERO;
	@Nullable BlockPos circlingCenter;
	PhantomEntity.PhantomMovementType movementType = PhantomEntity.PhantomMovementType.CIRCLE;

	public PhantomEntity(EntityType<? extends PhantomEntity> entityType, World world) {
		super(entityType, world);
		experiencePoints = 5;
		moveControl = new PhantomEntity.PhantomMoveControl(this);
		lookControl = new PhantomEntity.PhantomLookControl(this);
	}

	@Override
	public boolean isFlappingWings() {
		return (getWingFlapTickOffset() + age) % WING_FLAP_TICKS == 0;
	}

	@Override
	protected BodyControl createBodyControl() {
		return new PhantomEntity.PhantomBodyControl(this);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new PhantomEntity.StartAttackGoal());
		goalSelector.add(2, new PhantomEntity.SwoopMovementGoal());
		goalSelector.add(3, new PhantomEntity.CircleMovementGoal());
		targetSelector.add(1, new PhantomEntity.FindTargetGoal());
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(SIZE, 0);
	}

	public void setPhantomSize(int size) {
		dataTracker.set(SIZE, MathHelper.clamp(size, 0, 64));
	}

	private void onSizeChanged() {
		calculateDimensions();
		getAttributeInstance(EntityAttributes.ATTACK_DAMAGE).setBaseValue(6 + getPhantomSize());
	}

	public int getPhantomSize() {
		return dataTracker.get(SIZE);
	}

	@Override
	public void onTrackedDataSet(TrackedData<?> data) {
		if (SIZE.equals(data)) {
			onSizeChanged();
		}

		super.onTrackedDataSet(data);
	}

	public int getWingFlapTickOffset() {
		return getId() * 3;
	}

	@Override
	public void tick() {
		super.tick();

		if (!getEntityWorld().isClient()) {
			return;
		}

		float wingCos = MathHelper.cos(
			(getWingFlapTickOffset() + age) * WING_FLAP_ANGULAR_VELOCITY * (float) (Math.PI / 180.0) + (float) Math.PI
		);
		float wingCosNext = MathHelper.cos(
			(getWingFlapTickOffset() + age + 1) * WING_FLAP_ANGULAR_VELOCITY * (float) (Math.PI / 180.0) + (float) Math.PI
		);

		if (wingCos > 0.0F && wingCosNext <= 0.0F) {
			getEntityWorld().playSoundClient(
				getX(),
				getY(),
				getZ(),
				SoundEvents.ENTITY_PHANTOM_FLAP,
				getSoundCategory(),
				0.95F + random.nextFloat() * 0.05F,
				0.95F + random.nextFloat() * 0.05F,
				false
			);
		}

		float wingSpan = getWidth() * 1.48F;
		float wingOffsetX = MathHelper.cos(getYaw() * (float) (Math.PI / 180.0)) * wingSpan;
		float wingOffsetZ = MathHelper.sin(getYaw() * (float) (Math.PI / 180.0)) * wingSpan;
		float wingOffsetY = (0.3F + wingCos * 0.45F) * getHeight() * 2.5F;

		getEntityWorld().addParticleClient(
			ParticleTypes.MYCELIUM,
			getX() + wingOffsetX,
			getY() + wingOffsetY,
			getZ() + wingOffsetZ,
			0.0,
			0.0,
			0.0
		);
		getEntityWorld().addParticleClient(
			ParticleTypes.MYCELIUM,
			getX() - wingOffsetX,
			getY() + wingOffsetY,
			getZ() - wingOffsetZ,
			0.0,
			0.0,
			0.0
		);
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
		travelFlying(movementInput, 0.2F);
	}

	@Override
	public EntityData initialize(
		ServerWorldAccess world,
		LocalDifficulty difficulty,
		SpawnReason spawnReason,
		@Nullable EntityData entityData
	) {
		circlingCenter = getBlockPos().up(5);
		setPhantomSize(0);
		return super.initialize(world, difficulty, spawnReason, entityData);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		circlingCenter = view.<BlockPos>read("anchor_pos", BlockPos.CODEC).orElse(null);
		setPhantomSize(view.getInt("size", 0));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putNullable("anchor_pos", BlockPos.CODEC, circlingCenter);
		view.putInt("size", getPhantomSize());
	}

	@Override
	public boolean shouldRender(double distance) {
		return true;
	}

	@Override
	public SoundCategory getSoundCategory() {
		return SoundCategory.HOSTILE;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENTITY_PHANTOM_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_PHANTOM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_PHANTOM_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 1.0F;
	}

	@Override
	public boolean canTarget(EntityType<?> type) {
		return true;
	}

	@Override
	public EntityDimensions getBaseDimensions(EntityPose pose) {
		int size = getPhantomSize();
		EntityDimensions dimensions = super.getBaseDimensions(pose);

		return dimensions.scaled(1.0F + 0.15F * size);
	}

	boolean testTargetPredicate(ServerWorld world, LivingEntity target, TargetPredicate predicate) {
		return predicate.test(world, this, target);
	}

	class CircleMovementGoal extends PhantomEntity.MovementGoal {

		private float angle;
		private float radius;
		private float yOffset;
		private float circlingDirection;

		@Override
		public boolean canStart() {
			return PhantomEntity.this.getTarget() == null
					|| PhantomEntity.this.movementType == PhantomEntity.PhantomMovementType.CIRCLE;
		}

		@Override
		public void start() {
			this.radius = 5.0F + PhantomEntity.this.random.nextFloat() * 10.0F;
			this.yOffset = -4.0F + PhantomEntity.this.random.nextFloat() * 9.0F;
			this.circlingDirection = PhantomEntity.this.random.nextBoolean() ? 1.0F : -1.0F;
			this.adjustDirection();
		}

		@Override
		public void tick() {
			if (PhantomEntity.this.random.nextInt(this.getTickCount(350)) == 0) {
				this.yOffset = -4.0F + PhantomEntity.this.random.nextFloat() * 9.0F;
			}

			if (PhantomEntity.this.random.nextInt(this.getTickCount(250)) == 0) {
				this.radius++;
				if (this.radius > 15.0F) {
					this.radius = 5.0F;
					this.circlingDirection = -this.circlingDirection;
				}
			}

			if (PhantomEntity.this.random.nextInt(this.getTickCount(450)) == 0) {
				this.angle = PhantomEntity.this.random.nextFloat() * 2.0F * (float) Math.PI;
				this.adjustDirection();
			}

			if (this.isNearTarget()) {
				this.adjustDirection();
			}

			if (PhantomEntity.this.targetPosition.y < PhantomEntity.this.getY()
					&& !PhantomEntity.this.getEntityWorld().isAir(PhantomEntity.this.getBlockPos().down(1))) {
				this.yOffset = Math.max(1.0F, this.yOffset);
				this.adjustDirection();
			}

			if (PhantomEntity.this.targetPosition.y > PhantomEntity.this.getY()
					&& !PhantomEntity.this.getEntityWorld().isAir(PhantomEntity.this.getBlockPos().up(1))) {
				this.yOffset = Math.min(-1.0F, this.yOffset);
				this.adjustDirection();
			}
		}

		private void adjustDirection() {
			if (PhantomEntity.this.circlingCenter == null) {
				PhantomEntity.this.circlingCenter = PhantomEntity.this.getBlockPos();
			}

			this.angle = this.angle + this.circlingDirection * 15.0F * (float) (Math.PI / 180.0);
			PhantomEntity.this.targetPosition = Vec3d.of(PhantomEntity.this.circlingCenter)
			                                         .add(
					                                         this.radius * MathHelper.cos(this.angle),
					                                         -4.0F + this.yOffset,
					                                         this.radius * MathHelper.sin(this.angle)
			                                         );
		}
	}

	class FindTargetGoal extends Goal {

		private static final double PLAYER_DETECTION_RANGE = 64.0;
		private static final TargetPredicate PLAYERS_IN_RANGE_PREDICATE = TargetPredicate.createAttackable().setBaseMaxDistance(PLAYER_DETECTION_RANGE);
		private int delay = toGoalTicks(CAT_CHECK_INTERVAL);

		@Override
		public boolean canStart() {
			if (delay > 0) {
				delay--;
				return false;
			}

			delay = toGoalTicks(60);
			ServerWorld serverWorld = castToServerWorld(PhantomEntity.this.getEntityWorld());
			List<PlayerEntity> players = serverWorld.getPlayers(
				PLAYERS_IN_RANGE_PREDICATE,
				PhantomEntity.this,
				PhantomEntity.this.getBoundingBox().expand(16.0, 64.0, 16.0)
			);

			if (players.isEmpty()) {
				return false;
			}

			players.sort(Comparator.comparing(Entity::getY).reversed());

			for (PlayerEntity player : players) {
				if (PhantomEntity.this.testTargetPredicate(serverWorld, player, TargetPredicate.DEFAULT)) {
					PhantomEntity.this.setTarget(player);
					return true;
				}
			}

			return false;
		}

		@Override
		public boolean shouldContinue() {
			LivingEntity target = PhantomEntity.this.getTarget();

			if (target == null) {
				return false;
			}

			return PhantomEntity.this.testTargetPredicate(
				castToServerWorld(PhantomEntity.this.getEntityWorld()),
				target,
				TargetPredicate.DEFAULT
			);
		}
	}

	abstract class MovementGoal extends Goal {

		public MovementGoal() {
			setControls(EnumSet.of(Goal.Control.MOVE));
		}

		protected boolean isNearTarget() {
			return PhantomEntity.this.targetPosition.squaredDistanceTo(
				PhantomEntity.this.getX(),
				PhantomEntity.this.getY(),
				PhantomEntity.this.getZ()
			) < 4.0;
		}
	}

	class PhantomBodyControl extends BodyControl {

		public PhantomBodyControl(final MobEntity entity) {
			super(entity);
		}

		@Override
		public void tick() {
			PhantomEntity.this.headYaw = PhantomEntity.this.bodyYaw;
			PhantomEntity.this.bodyYaw = PhantomEntity.this.getYaw();
		}
	}

	static class PhantomLookControl extends LookControl {

		public PhantomLookControl(MobEntity mobEntity) {
			super(mobEntity);
		}

		@Override
		public void tick() {
		}
	}

	class PhantomMoveControl extends MoveControl {

		private float targetSpeed = 0.1F;

		public PhantomMoveControl(final MobEntity owner) {
			super(owner);
		}

		@Override
		public void tick() {
			if (PhantomEntity.this.horizontalCollision) {
				PhantomEntity.this.setYaw(PhantomEntity.this.getYaw() + 180.0F);
				targetSpeed = 0.1F;
			}

			double dx = PhantomEntity.this.targetPosition.x - PhantomEntity.this.getX();
			double dy = PhantomEntity.this.targetPosition.y - PhantomEntity.this.getY();
			double dz = PhantomEntity.this.targetPosition.z - PhantomEntity.this.getZ();
			double horizDist = Math.sqrt(dx * dx + dz * dz);

			if (Math.abs(horizDist) > 1.0E-5F) {
				double vertScale = 1.0 - Math.abs(dy * 0.7F) / horizDist;
				dx *= vertScale;
				dz *= vertScale;
				horizDist = Math.sqrt(dx * dx + dz * dz);
				double totalDist = Math.sqrt(dx * dx + dz * dz + dy * dy);
				float prevYaw = PhantomEntity.this.getYaw();
				float targetAngle = (float) MathHelper.atan2(dz, dx);
				float wrappedYaw = MathHelper.wrapDegrees(PhantomEntity.this.getYaw() + 90.0F);
				float wrappedTarget = MathHelper.wrapDegrees(targetAngle * (180.0F / (float) Math.PI));
				PhantomEntity.this.setYaw(MathHelper.stepUnwrappedAngleTowards(wrappedYaw, wrappedTarget, 4.0F) - 90.0F);
				PhantomEntity.this.bodyYaw = PhantomEntity.this.getYaw();

				if (MathHelper.angleBetween(prevYaw, PhantomEntity.this.getYaw()) < 3.0F) {
					targetSpeed = MathHelper.stepTowards(targetSpeed, 1.8F, 0.005F * (1.8F / targetSpeed));
				} else {
					targetSpeed = MathHelper.stepTowards(targetSpeed, 0.2F, 0.025F);
				}

				float pitch = (float) (-(MathHelper.atan2(-dy, horizDist) * 180.0F / (float) Math.PI));
				PhantomEntity.this.setPitch(pitch);
				float yawOffset = PhantomEntity.this.getYaw() + 90.0F;
				double velX = targetSpeed * MathHelper.cos(yawOffset * (float) (Math.PI / 180.0)) * Math.abs(dx / totalDist);
				double velZ = targetSpeed * MathHelper.sin(yawOffset * (float) (Math.PI / 180.0)) * Math.abs(dz / totalDist);
				double velY = targetSpeed * MathHelper.sin(pitch * (float) (Math.PI / 180.0)) * Math.abs(dy / totalDist);
				Vec3d velocity = PhantomEntity.this.getVelocity();
				PhantomEntity.this.setVelocity(velocity.add(new Vec3d(velX, velY, velZ).subtract(velocity).multiply(0.2)));
			}
		}
	}

	enum PhantomMovementType {
		CIRCLE,
		SWOOP
	}

	class StartAttackGoal extends Goal {

		private int cooldown;

		@Override
		public boolean canStart() {
			LivingEntity target = PhantomEntity.this.getTarget();

			if (target == null) {
				return false;
			}

			return PhantomEntity.this.testTargetPredicate(
				castToServerWorld(PhantomEntity.this.getEntityWorld()),
				target,
				TargetPredicate.DEFAULT
			);
		}

		@Override
		public void start() {
			cooldown = getTickCount(10);
			PhantomEntity.this.movementType = PhantomEntity.PhantomMovementType.CIRCLE;
			startSwoop();
		}

		@Override
		public void stop() {
			if (PhantomEntity.this.circlingCenter == null) {
				return;
			}

			PhantomEntity.this.circlingCenter = PhantomEntity.this.getEntityWorld()
				.getTopPosition(Heightmap.Type.MOTION_BLOCKING, PhantomEntity.this.circlingCenter)
				.up(10 + PhantomEntity.this.random.nextInt(CAT_CHECK_INTERVAL));
		}

		@Override
		public void tick() {
			if (PhantomEntity.this.movementType != PhantomEntity.PhantomMovementType.CIRCLE) {
				return;
			}

			cooldown--;

			if (cooldown <= 0) {
				PhantomEntity.this.movementType = PhantomEntity.PhantomMovementType.SWOOP;
				startSwoop();
				cooldown = getTickCount((8 + PhantomEntity.this.random.nextInt(4)) * CAT_CHECK_INTERVAL);
				PhantomEntity.this.playSound(
					SoundEvents.ENTITY_PHANTOM_SWOOP,
					10.0F,
					0.95F + PhantomEntity.this.random.nextFloat() * 0.1F
				);
			}
		}

		private void startSwoop() {
			if (PhantomEntity.this.circlingCenter == null) {
				return;
			}

			PhantomEntity.this.circlingCenter = PhantomEntity.this.getTarget()
				.getBlockPos()
				.up(CAT_CHECK_INTERVAL + PhantomEntity.this.random.nextInt(CAT_CHECK_INTERVAL));

			if (PhantomEntity.this.circlingCenter.getY() < PhantomEntity.this.getEntityWorld().getSeaLevel()) {
				PhantomEntity.this.circlingCenter = new BlockPos(
					PhantomEntity.this.circlingCenter.getX(),
					PhantomEntity.this.getEntityWorld().getSeaLevel() + 1,
					PhantomEntity.this.circlingCenter.getZ()
				);
			}
		}
	}

	class SwoopMovementGoal extends PhantomEntity.MovementGoal {

		private boolean catsNearby;
		private int nextCatCheckAge;

		@Override
		public boolean canStart() {
			return PhantomEntity.this.getTarget() != null
				&& PhantomEntity.this.movementType == PhantomEntity.PhantomMovementType.SWOOP;
		}

		@Override
		public boolean shouldContinue() {
			LivingEntity target = PhantomEntity.this.getTarget();

			if (target == null) {
				return false;
			}

			if (!target.isAlive()) {
				return false;
			}

			if (target instanceof PlayerEntity player && (target.isSpectator() || player.isCreative())) {
				return false;
			}

			if (!canStart()) {
				return false;
			}

			if (PhantomEntity.this.age > nextCatCheckAge) {
				nextCatCheckAge = PhantomEntity.this.age + CAT_CHECK_INTERVAL;
				List<CatEntity> cats = PhantomEntity.this.getEntityWorld()
					.getEntitiesByClass(
						CatEntity.class,
						PhantomEntity.this.getBoundingBox().expand(16.0),
						EntityPredicates.VALID_ENTITY
					);

				for (CatEntity cat : cats) {
					cat.hiss();
				}

				catsNearby = !cats.isEmpty();
			}

			return !catsNearby;
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
			PhantomEntity.this.setTarget(null);
			PhantomEntity.this.movementType = PhantomEntity.PhantomMovementType.CIRCLE;
		}

		@Override
		public void tick() {
			LivingEntity target = PhantomEntity.this.getTarget();

			if (target == null) {
				return;
			}

			PhantomEntity.this.targetPosition = new Vec3d(target.getX(), target.getBodyY(0.5), target.getZ());

			if (PhantomEntity.this.getBoundingBox().expand(0.2F).intersects(target.getBoundingBox())) {
				PhantomEntity.this.tryAttack(castToServerWorld(PhantomEntity.this.getEntityWorld()), target);
				PhantomEntity.this.movementType = PhantomEntity.PhantomMovementType.CIRCLE;

				if (!PhantomEntity.this.isSilent()) {
					PhantomEntity.this.getEntityWorld().syncWorldEvent(1039, PhantomEntity.this.getBlockPos(), 0);
				}
			} else if (PhantomEntity.this.horizontalCollision || PhantomEntity.this.hurtTime > 0) {
				PhantomEntity.this.movementType = PhantomEntity.PhantomMovementType.CIRCLE;
			}
		}
	}
}
