package net.minecraft.entity.ai.brain.task;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.LongJumpUtil;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Задача мозга бриза, управляющая прыжком к позиции за спиной цели.
 * Проходит фазы: вдох (INHALING) → прыжок (LONG_JUMPING) → приземление (STANDING).
 * После приземления активирует кулдаун прыжка и разрешает стрельбу.
 */
public class BreezeJumpTask extends MultiTickTask<BreezeEntity> {

	private static final int REQUIRED_SPACE_ABOVE = 4;
	private static final int JUMP_COOLDOWN_EXPIRY = 10;
	private static final int JUMP_COOLDOWN_EXPIRY_WHEN_HURT = 2;
	private static final int JUMP_INHALING_EXPIRY = Math.round(10.0F);
	private static final float MAX_JUMP_RANGE = 24.0F;
	private static final float MAX_JUMP_VELOCITY = 1.4F;
	private static final float FOLLOW_RANGE_MULTIPLIER_FOR_VELOCITY = 0.058333334F;
	private static final long SHOOT_AFTER_JUMP_EXPIRY = 100L;
	private static final double RAYCAST_VERTICAL_REACH = 10.0;
	private static final float MIN_ATTACK_DISTANCE = 4.0F;
	private static final int MAX_RUN_TIME = 200;
	private static final ObjectArrayList<Integer> POSSIBLE_JUMP_ANGLES =
			new ObjectArrayList<>(new Integer[]{40, 55, 60, 75, 80});

	@VisibleForTesting
	public BreezeJumpTask() {
		super(
				Map.of(
						MemoryModuleType.ATTACK_TARGET,
						MemoryModuleState.VALUE_PRESENT,
						MemoryModuleType.BREEZE_JUMP_COOLDOWN,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.BREEZE_JUMP_INHALING,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.BREEZE_JUMP_TARGET,
						MemoryModuleState.REGISTERED,
						MemoryModuleType.BREEZE_SHOOT,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.WALK_TARGET,
						MemoryModuleState.VALUE_ABSENT,
						MemoryModuleType.BREEZE_LEAVING_WATER,
						MemoryModuleState.REGISTERED
				),
				MAX_RUN_TIME
		);
	}

	public static boolean shouldJump(ServerWorld world, BreezeEntity breeze) {
		if (!breeze.isOnGround() && !breeze.isTouchingWater()) {
			return false;
		}

		if (StayAboveWaterTask.isUnderwater(breeze)) {
			return false;
		}

		if (breeze.getBrain().isMemoryInState(MemoryModuleType.BREEZE_JUMP_TARGET, MemoryModuleState.VALUE_PRESENT)) {
			return true;
		}

		LivingEntity target = breeze.getBrain().getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
		if (target == null) {
			return false;
		}

		if (isTargetOutOfRange(breeze, target)) {
			breeze.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
			return false;
		}

		if (isTargetTooClose(breeze, target)) {
			return false;
		}

		if (!hasRoomToJump(world, breeze)) {
			return false;
		}

		BlockPos jumpPos = getPosToJumpTo(breeze, BreezeMovementUtil.getRandomPosBehindTarget(target, breeze.getRandom()));
		if (jumpPos == null) {
			return false;
		}

		BlockState groundState = world.getBlockState(jumpPos.down());
		if (breeze.getType().isInvalidSpawn(groundState)) {
			return false;
		}

		if (!BreezeMovementUtil.canMoveTo(breeze, jumpPos.toCenterPos())
				&& !BreezeMovementUtil.canMoveTo(breeze, jumpPos.up(4).toCenterPos())) {
			return false;
		}

		breeze.getBrain().remember(MemoryModuleType.BREEZE_JUMP_TARGET, jumpPos);
		return true;
	}

	@Override
	protected boolean shouldRun(ServerWorld world, BreezeEntity entity) {
		return shouldJump(world, entity);
	}

	@Override
	protected boolean shouldKeepRunning(ServerWorld world, BreezeEntity entity, long time) {
		return entity.getPose() != EntityPose.STANDING
				&& !entity.getBrain().hasMemoryModule(MemoryModuleType.BREEZE_JUMP_COOLDOWN);
	}

	@Override
	protected void run(ServerWorld world, BreezeEntity entity, long time) {
		if (entity.getBrain().isMemoryInState(MemoryModuleType.BREEZE_JUMP_INHALING, MemoryModuleState.VALUE_ABSENT)) {
			entity.getBrain().remember(MemoryModuleType.BREEZE_JUMP_INHALING, Unit.INSTANCE, JUMP_INHALING_EXPIRY);
		}

		entity.setPose(EntityPose.INHALING);
		world.playSoundFromEntity(null, entity, SoundEvents.ENTITY_BREEZE_CHARGE, SoundCategory.HOSTILE, 1.0F, 1.0F);
		entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
				.ifPresent(jumpTarget -> entity.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, jumpTarget.toCenterPos()));
	}

	@Override
	protected void keepRunning(ServerWorld world, BreezeEntity entity, long time) {
		boolean touchingWater = entity.isTouchingWater();
		if (!touchingWater
				&& entity.getBrain().isMemoryInState(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryModuleState.VALUE_PRESENT)) {
			entity.getBrain().forget(MemoryModuleType.BREEZE_LEAVING_WATER);
		}

		if (shouldStopInhalingPose(entity)) {
			Vec3d velocity = entity.getBrain()
					.getOptionalRegisteredMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
					.flatMap(jumpTarget -> getJumpingVelocity(entity, entity.getRandom(), Vec3d.ofBottomCenter(jumpTarget)))
					.orElse(null);
			if (velocity == null) {
				entity.setPose(EntityPose.STANDING);
				return;
			}

			if (touchingWater) {
				entity.getBrain().remember(MemoryModuleType.BREEZE_LEAVING_WATER, Unit.INSTANCE);
			}

			entity.playSound(SoundEvents.ENTITY_BREEZE_JUMP, 1.0F, 1.0F);
			entity.setPose(EntityPose.LONG_JUMPING);
			entity.setYaw(entity.bodyYaw);
			entity.setNoDrag(true);
			entity.setVelocity(velocity);
		} else if (shouldStopLongJumpingPose(entity)) {
			entity.playSound(SoundEvents.ENTITY_BREEZE_LAND, 1.0F, 1.0F);
			entity.setPose(EntityPose.STANDING);
			entity.setNoDrag(false);
			boolean wasHurt = entity.getBrain().hasMemoryModule(MemoryModuleType.HURT_BY);
			entity.getBrain().remember(
				MemoryModuleType.BREEZE_JUMP_COOLDOWN,
				Unit.INSTANCE,
				wasHurt ? JUMP_COOLDOWN_EXPIRY_WHEN_HURT : JUMP_COOLDOWN_EXPIRY
			);
			entity.getBrain().remember(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, SHOOT_AFTER_JUMP_EXPIRY);
		}
	}

	@Override
	protected void finishRunning(ServerWorld world, BreezeEntity entity, long time) {
		EntityPose pose = entity.getPose();
		if (pose == EntityPose.LONG_JUMPING || pose == EntityPose.INHALING) {
			entity.setPose(EntityPose.STANDING);
		}

		entity.getBrain().forget(MemoryModuleType.BREEZE_JUMP_TARGET);
		entity.getBrain().forget(MemoryModuleType.BREEZE_JUMP_INHALING);
		entity.getBrain().forget(MemoryModuleType.BREEZE_LEAVING_WATER);
	}

	private static boolean shouldStopInhalingPose(BreezeEntity breeze) {
		return breeze.getBrain().getOptionalRegisteredMemory(MemoryModuleType.BREEZE_JUMP_INHALING).isEmpty()
				&& breeze.getPose() == EntityPose.INHALING;
	}

	private static boolean shouldStopLongJumpingPose(BreezeEntity breeze) {
		boolean isLongJumping = breeze.getPose() == EntityPose.LONG_JUMPING;
		boolean onGround = breeze.isOnGround();
		boolean exitingWater = breeze.isTouchingWater()
				&& breeze.getBrain().isMemoryInState(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryModuleState.VALUE_ABSENT);
		return isLongJumping && (onGround || exitingWater);
	}

	private static @Nullable BlockPos getPosToJumpTo(LivingEntity breeze, Vec3d pos) {
		RaycastContext downContext = new RaycastContext(
				pos,
				pos.offset(Direction.DOWN, RAYCAST_VERTICAL_REACH),
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				breeze
		);
		HitResult downHit = breeze.getEntityWorld().raycast(downContext);

		if (downHit.getType() == HitResult.Type.BLOCK) {
			return BlockPos.ofFloored(downHit.getPos()).up();
		}

		RaycastContext upContext = new RaycastContext(
				pos,
				pos.offset(Direction.UP, RAYCAST_VERTICAL_REACH),
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				breeze
		);
		HitResult upHit = breeze.getEntityWorld().raycast(upContext);

		return upHit.getType() == HitResult.Type.BLOCK ? BlockPos.ofFloored(upHit.getPos()).up() : null;
	}

	private static boolean isTargetOutOfRange(BreezeEntity breeze, LivingEntity target) {
		return !target.isInRange(breeze, breeze.getAttributeValue(EntityAttributes.FOLLOW_RANGE));
	}

	private static boolean isTargetTooClose(BreezeEntity breeze, LivingEntity target) {
		return target.distanceTo(breeze) - MIN_ATTACK_DISTANCE <= 0.0F;
	}

	private static boolean hasRoomToJump(ServerWorld world, BreezeEntity breeze) {
		BlockPos pos = breeze.getBlockPos();
		if (world.getBlockState(pos).isOf(Blocks.HONEY_BLOCK)) {
			return false;
		}

		for (int height = 1; height <= REQUIRED_SPACE_ABOVE; height++) {
			BlockPos above = pos.offset(Direction.UP, height);
			if (!world.getBlockState(above).isAir() && !world.getFluidState(above).isIn(FluidTags.WATER)) {
				return false;
			}
		}

		return true;
	}

	private static Optional<Vec3d> getJumpingVelocity(BreezeEntity breeze, Random random, Vec3d jumpTarget) {
		for (int angle : Util.copyShuffled(POSSIBLE_JUMP_ANGLES, random)) {
			float maxVelocity = FOLLOW_RANGE_MULTIPLIER_FOR_VELOCITY * (float) breeze.getAttributeValue(EntityAttributes.FOLLOW_RANGE);
			Optional<Vec3d> velocity = LongJumpUtil.getJumpingVelocity(breeze, jumpTarget, maxVelocity, angle, false);
			if (velocity.isPresent()) {
				if (breeze.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
					double boostY = velocity.get().normalize().y * breeze.getJumpBoostVelocityModifier();
					return velocity.map(vec -> vec.add(0.0, boostY, 0.0));
				}

				return velocity;
			}
		}

		return Optional.empty();
	}
}
