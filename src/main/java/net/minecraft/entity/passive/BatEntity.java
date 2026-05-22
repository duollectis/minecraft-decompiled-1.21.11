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
 * Летучая мышь — пассивное летающее существо, обитающее в пещерах.
 * Днём висит на потолке (roosting), ночью летает в поисках места для отдыха.
 * Убегает от игроков, приближающихся на расстояние менее 4 блоков.
 */
public class BatEntity extends AmbientEntity {

	public static final float BAT_WIDTH = 0.5F;
	public static final float BAT_TRACKING_RANGE = 10.0F;

	private static final TrackedData<Byte> BAT_FLAGS = DataTracker.registerData(
		BatEntity.class, TrackedDataHandlerRegistry.BYTE
	);
	private static final int ROOSTING_FLAG = 1;
	private static final TargetPredicate CLOSE_PLAYER_PREDICATE = TargetPredicate
		.createNonAttackable()
		.setBaseMaxDistance(4.0);

	public final AnimationState flyingAnimationState = new AnimationState();
	public final AnimationState roostingAnimationState = new AnimationState();
	private @Nullable BlockPos hangingPosition;

	public BatEntity(EntityType<? extends BatEntity> entityType, World world) {
		super(entityType, world);
		if (!world.isClient()) {
			setRoosting(true);
		}
	}

	@Override
	public boolean isFlappingWings() {
		return !isRoosting() && age % 10 == 0;
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
		return isRoosting() && random.nextInt(4) != 0 ? null : SoundEvents.ENTITY_BAT_AMBIENT;
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
		return (dataTracker.get(BAT_FLAGS) & ROOSTING_FLAG) != 0;
	}

	public void setRoosting(boolean roosting) {
		byte flags = dataTracker.get(BAT_FLAGS);
		dataTracker.set(BAT_FLAGS, roosting ? (byte) (flags | ROOSTING_FLAG) : (byte) (flags & ~ROOSTING_FLAG));
	}

	@Override
	public void tick() {
		super.tick();
		if (isRoosting()) {
			setVelocity(Vec3d.ZERO);
			setPos(getX(), MathHelper.floor(getY()) + 1.0 - getHeight(), getZ());
		} else {
			setVelocity(getVelocity().multiply(1.0, 0.6, 1.0));
		}

		updateAnimations();
	}

	/**
	 * Основная логика ИИ летучей мыши на сервере.
	 * В режиме roosting: случайно поворачивает голову и улетает при приближении игрока.
	 * В режиме полёта: навигирует к случайной точке подвешивания, пытается сесть на потолок.
	 */
	@Override
	protected void mobTick(ServerWorld world) {
		super.mobTick(world);
		BlockPos pos = getBlockPos();
		BlockPos posAbove = pos.up();

		if (isRoosting()) {
			boolean silent = isSilent();
			if (world.getBlockState(posAbove).isSolidBlock(world, pos)) {
				if (random.nextInt(200) == 0) {
					headYaw = random.nextInt(360);
				}

				if (world.getClosestPlayer(CLOSE_PLAYER_PREDICATE, this) != null) {
					setRoosting(false);
					if (!silent) {
						world.syncWorldEvent(null, 1025, pos, 0);
					}
				}
			} else {
				setRoosting(false);
				if (!silent) {
					world.syncWorldEvent(null, 1025, pos, 0);
				}
			}
		} else {
			if (hangingPosition != null
				&& (!world.isAir(hangingPosition) || hangingPosition.getY() <= world.getBottomY())
			) {
				hangingPosition = null;
			}

			if (hangingPosition == null
				|| random.nextInt(30) == 0
				|| hangingPosition.isWithinDistance(getEntityPos(), 2.0)
			) {
				hangingPosition = BlockPos.ofFloored(
					getX() + random.nextInt(7) - random.nextInt(7),
					getY() + random.nextInt(6) - 2.0,
					getZ() + random.nextInt(7) - random.nextInt(7)
				);
			}

			double dx = hangingPosition.getX() + 0.5 - getX();
			double dy = hangingPosition.getY() + 0.1 - getY();
			double dz = hangingPosition.getZ() + 0.5 - getZ();
			Vec3d velocity = getVelocity();
			Vec3d newVelocity = velocity.add(
				(Math.signum(dx) * 0.5 - velocity.x) * 0.1F,
				(Math.signum(dy) * 0.7F - velocity.y) * 0.1F,
				(Math.signum(dz) * 0.5 - velocity.z) * 0.1F
			);
			setVelocity(newVelocity);

			float targetYaw = (float) (MathHelper.atan2(newVelocity.z, newVelocity.x) * 180.0F / (float) Math.PI) - 90.0F;
			float yawDelta = MathHelper.wrapDegrees(targetYaw - getYaw());
			forwardSpeed = 0.5F;
			setYaw(getYaw() + yawDelta);

			if (random.nextInt(100) == 0 && world.getBlockState(posAbove).isSolidBlock(world, posAbove)) {
				setRoosting(true);
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
		if (isInvulnerableTo(world, source)) {
			return false;
		}

		if (isRoosting()) {
			setRoosting(false);
		}

		return super.damage(world, source, amount);
	}

	@Override
	protected void readCustomData(ReadView view) {
		super.readCustomData(view);
		dataTracker.set(BAT_FLAGS, view.getByte("BatFlags", (byte) 0));
	}

	@Override
	protected void writeCustomData(WriteView view) {
		super.writeCustomData(view);
		view.putByte("BatFlags", dataTracker.get(BAT_FLAGS));
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

		if (random.nextBoolean()) {
			return false;
		}

		if (world.getLightLevel(pos) > random.nextInt(4)) {
			return false;
		}

		if (!world.getBlockState(pos.down()).isIn(BlockTags.BATS_SPAWNABLE_ON)) {
			return false;
		}

		return canMobSpawn(type, world, spawnReason, pos, random);
	}

	private void updateAnimations() {
		if (isRoosting()) {
			flyingAnimationState.stop();
			roostingAnimationState.startIfNotRunning(age);
		} else {
			roostingAnimationState.stop();
			flyingAnimationState.startIfNotRunning(age);
		}
	}
}
