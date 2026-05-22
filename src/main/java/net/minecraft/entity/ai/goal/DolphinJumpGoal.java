package net.minecraft.entity.ai.goal;

import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Цель прыжка дельфина: проверяет наличие непрерывного водного коридора
 * с воздухом сверху по направлению движения, затем выполняет прыжок с разворотом.
 */
public class DolphinJumpGoal extends DiveJumpingGoal {

	private static final int[] OFFSET_MULTIPLIERS = new int[]{0, 1, 4, 5, 6, 7};
	private static final float JUMP_HORIZONTAL_IMPULSE = 0.6F;
	private static final float JUMP_VERTICAL_IMPULSE = 0.7F;
	private static final float PITCH_LERP_SPEED = 0.2F;
	private static final float MIN_VELOCITY_SQ = 0.03F;
	private static final float PITCH_LEVEL_THRESHOLD = 10.0F;
	private static final double MIN_SPEED = 1.0E-5;

	private final DolphinEntity dolphin;
	private final int chance;
	private boolean inWater;

	public DolphinJumpGoal(DolphinEntity dolphin, int chance) {
		this.dolphin = dolphin;
		this.chance = toGoalTicks(chance);
	}

	@Override
	public boolean canStart() {
		if (dolphin.getRandom().nextInt(chance) != 0) {
			return false;
		}

		Direction direction = dolphin.getMovementDirection();
		int offsetX = direction.getOffsetX();
		int offsetZ = direction.getOffsetZ();
		BlockPos pos = dolphin.getBlockPos();

		for (int multiplier : OFFSET_MULTIPLIERS) {
			if (!isWater(pos, offsetX, offsetZ, multiplier) || !isAirAbove(pos, offsetX, offsetZ, multiplier)) {
				return false;
			}
		}

		return true;
	}

	private boolean isWater(BlockPos pos, int offsetX, int offsetZ, int multiplier) {
		BlockPos target = pos.add(offsetX * multiplier, 0, offsetZ * multiplier);
		return dolphin.getEntityWorld().getFluidState(target).isIn(FluidTags.WATER)
			&& !dolphin.getEntityWorld().getBlockState(target).blocksMovement();
	}

	private boolean isAirAbove(BlockPos pos, int offsetX, int offsetZ, int multiplier) {
		return dolphin.getEntityWorld()
			.getBlockState(pos.add(offsetX * multiplier, 1, offsetZ * multiplier))
			.isAir()
			&& dolphin.getEntityWorld()
			.getBlockState(pos.add(offsetX * multiplier, 2, offsetZ * multiplier))
			.isAir();
	}

	@Override
	public boolean shouldContinue() {
		double velocityY = dolphin.getVelocity().y;
		boolean slowingDown = velocityY * velocityY < MIN_VELOCITY_SQ
			&& dolphin.getPitch() != 0.0F
			&& Math.abs(dolphin.getPitch()) < PITCH_LEVEL_THRESHOLD
			&& dolphin.isTouchingWater();

		return !slowingDown && !dolphin.isOnGround();
	}

	@Override
	public boolean canStop() {
		return false;
	}

	@Override
	public void start() {
		Direction direction = dolphin.getMovementDirection();
		dolphin.setVelocity(
			dolphin.getVelocity().add(
				direction.getOffsetX() * JUMP_HORIZONTAL_IMPULSE,
				JUMP_VERTICAL_IMPULSE,
				direction.getOffsetZ() * JUMP_HORIZONTAL_IMPULSE
			)
		);
		dolphin.getNavigation().stop();
	}

	@Override
	public void stop() {
		dolphin.setPitch(0.0F);
	}

	@Override
	public void tick() {
		boolean wasInWater = inWater;

		if (!wasInWater) {
			FluidState fluidState = dolphin.getEntityWorld().getFluidState(dolphin.getBlockPos());
			inWater = fluidState.isIn(FluidTags.WATER);
		}

		if (inWater && !wasInWater) {
			dolphin.playSound(SoundEvents.ENTITY_DOLPHIN_JUMP, 1.0F, 1.0F);
		}

		Vec3d velocity = dolphin.getVelocity();

		if (velocity.y * velocity.y < MIN_VELOCITY_SQ && dolphin.getPitch() != 0.0F) {
			dolphin.setPitch(MathHelper.lerpAngleDegrees(PITCH_LERP_SPEED, dolphin.getPitch(), 0.0F));
		} else if (velocity.length() > MIN_SPEED) {
			double horizontalLen = velocity.horizontalLength();
			double pitchAngle = Math.atan2(-velocity.y, horizontalLen) * 180.0F / (float) Math.PI;
			dolphin.setPitch((float) pitchAngle);
		}
	}
}
