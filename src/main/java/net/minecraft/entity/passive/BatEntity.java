package net.minecraft.entity.passive;

import net.minecraft.block.BlockState;
import net.minecraft.entity.AnimationState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jspecify.annotations.Nullable;

/**
 * {@code BatEntity}.
 */
public class BatEntity extends AmbientEntity {

	public static final float BAT_WIDTH = 0.5F;
	public static final float BAT_TRACKING_RANGE = 10.0F;
	private static final TrackedData<Byte>
			BAT_FLAGS =
			DataTracker.registerData(BatEntity.class, TrackedDataHandlerRegistry.BYTE);
	private static final int ROOSTING_FLAG = 1;
	private static final TargetPredicate
			CLOSE_PLAYER_PREDICATE =
			TargetPredicate.createNonAttackable().setBaseMaxDistance(4.0);
	private static final byte DEFAULT_BAT_FLAGS = 0;
	public final AnimationState flyingAnimationState = new AnimationState();
	public final AnimationState roostingAnimationState = new AnimationState();
	private @Nullable BlockPos hangingPosition;

	public BatEntity(EntityType<? extends BatEntity> entityType, World world) {
		super(entityType, world);
		if (!world.isClient()) {
			this.setRoosting(true);
		}
	}

	@Override
	public boolean isFlappingWings() {
		return !this.isRoosting() && this.age % 10.0F == 0.0F;
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(BAT_FLAGS, (byte) 0);
	}

	@Override
	protected float getSoundVolume() {
		return 0.1F;
	}

	@Override
	public float getSoundPitch() {
		return super.getSoundPitch() * 0.95F;
	}

	@Override
	public @Nullable SoundEvent getAmbientSound() {
		return this.isRoosting() && this.random.nextInt(4) != 0 ? null : SoundEvents.ENTITY_BAT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENTITY_BAT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENTITY_BAT_DEATH;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void pushAway(Entity entity) {
	}

	@Override
	protected void tickCramming() {
	}

	public static DefaultAttributeContainer.Builder createBatAttributes() {
		return MobEntity.createMobAttributes().add(EntityAttributes.MAX_HEALTH, 6.0);
	}

	public boolean isRoosting() {
		return (this.dataTracker.get(BAT_FLAGS) & 1) != 0;
	}

	public void setRoosting(boolean roosting) {
		byte b = this.dataTracker.get(BAT_FLAGS);
		if (roosting) {
			this.dataTracker.set(BAT_FLAGS, (byte) (b | 1));
		}
		else {
			this.dataTracker.set(BAT_FLAGS, (byte) (b & -2));
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.isRoosting()) {
			this.setVelocity(Vec3d.ZERO);
			this.setPos(this.getX(), MathHelper.floor(this.getY()) + 1.0 - this.getHeight(), this.getZ());
		}
		else {
			this.setVelocity(this.getVelocity().multiply(1.0, 0.6, 1.0));
		}

		this.updateAnimations();
	}

	@Override
	protected void mobTick(ServerWorld world) {
		super.mobTick(world);
		BlockPos blockPos = this.getBlockPos();
		BlockPos blockPos2 = blockPos.up();
		if (this.isRoosting()) {
			boolean bl = this.isSilent();
			if (world.getBlockState(blockPos2).isSolidBlock(world, blockPos)) {
				if (this.random.nextInt(200) == 0) {
					this.headYaw = this.random.nextInt(360);
				}

				if (world.getClosestPlayer(CLOSE_PLAYER_PREDICATE, this) != null) {
					this.setRoosting(false);
					if (!bl) {
						world.syncWorldEvent(null, 1025, blockPos, 0);
					}
				}
			}
			else {
				this.setRoosting(false);
				if (!bl) {
					world.syncWorldEvent(null, 1025, blockPos, 0);
				}
			}
		}
		else {
			if (this.hangingPosition != null && (!world.isAir(this.hangingPosition)
					|| this.hangingPosition.getY() <= world.getBottomY()
			)) {
				this.hangingPosition = null;
			}

			if (this.hangingPosition == null || this.random.nextInt(30) == 0 || this.hangingPosition.isWithinDistance(
					this.getEntityPos(),
					2.0
			)) {
				this.hangingPosition = BlockPos.ofFloored(
						this.getX() + this.random.nextInt(7) - this.random.nextInt(7),
						this.getY() + this.random.nextInt(6) - 2.0,
						this.getZ() + this.random.nextInt(7) - this.random.nextInt(7)
				);
			}

			double d = this.hangingPosition.getX() + 0.5 - this.getX();
			double e = this.hangingPosition.getY() + 0.1 - this.getY();
			double f = this.hangingPosition.getZ() + 0.5 - this.getZ();
			Vec3d vec3d = this.getVelocity();
			Vec3d
					vec3d2 =
					vec3d.add(
							(Math.signum(d) * 0.5 - vec3d.x) * 0.1F,
							(Math.signum(e) * 0.7F - vec3d.y) * 0.1F,
							(Math.signum(f) * 0.5 - vec3d.z) * 0.1F
					);
			this.setVelocity(vec3d2);
			float g = (float) (MathHelper.atan2(vec3d2.z, vec3d2.x) * 180.0F / (float) Math.PI) - 90.0F;
			float h = MathHelper.wrapDegrees(g - this.getYaw());
			this.forwardSpeed = 0.5F;
			this.setYaw(this.getYaw() + h);
			if (this.random.nextInt(100) == 0 && world.getBlockState(blockPos2).isSolidBlock(world, blockPos2)) {
				this.setRoosting(true);
			}
		}
	}

	@Override
	protected Entity.MoveEffect getMoveEffect() {
		return Entity.MoveEffect.EVENTS;
	}

	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
	}

	@Override
	public boolean canAvoidTraps() {
		return true;
	}

	@Override
	public boolean damage(ServerWorld world, DamageSource source, float amount) {
		if (this.isInvulnerableTo(world, source)) {
			return false;
		}
		else {
			if (this.isRoosting()) {
				this.setRoosting(false);
			}

			return super.damage(world, source, amount);
		}
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		this.dataTracker.set(BAT_FLAGS, view.getByte("BatFlags", (byte) 0));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putByte("BatFlags", this.dataTracker.get(BAT_FLAGS));
	}

	public static boolean canSpawn(
			EntityType<BatEntity> type,
			WorldAccess world,
			SpawnReason spawnReason,
			BlockPos pos,
			Random random
	) {
		if (pos.getY() >= world.getTopPosition(Heightmap.Type.WORLD_SURFACE, pos).getY()) {
			return false;
		}
		else if (random.nextBoolean()) {
			return false;
		}
		else if (world.getLightLevel(pos) > random.nextInt(4)) {
			return false;
		}
		else {
			return !world.getBlockState(pos.down()).isIn(BlockTags.BATS_SPAWNABLE_ON) ? false : canMobSpawn(
					type,
					world,
					spawnReason,
					pos,
					random
			);
		}
	}

	private void updateAnimations() {
		if (this.isRoosting()) {
			this.flyingAnimationState.stop();
			this.roostingAnimationState.startIfNotRunning(this.age);
		}
		else {
			this.roostingAnimationState.stop();
			this.flyingAnimationState.startIfNotRunning(this.age);
		}
	}
}
