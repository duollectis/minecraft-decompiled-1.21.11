package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * Цель прыжка на цель: моб прыгает в направлении цели, если находится
 * на земле и цель в диапазоне от 2 до 4 блоков.
 */
public class PounceAtTargetGoal extends Goal {

	private static final double MIN_POUNCE_DISTANCE_SQ = 4.0;
	private static final double MAX_POUNCE_DISTANCE_SQ = 16.0;
	private static final int POUNCE_CHANCE_TICKS = 5;
	private static final double HORIZONTAL_IMPULSE = 0.4;
	private static final double VELOCITY_CARRY = 0.2;
	private static final double MIN_DIRECTION_LENGTH_SQ = 1.0E-7;

	private final MobEntity mob;
	private LivingEntity target;
	private final float velocity;

	public PounceAtTargetGoal(MobEntity mob, float velocity) {
		this.mob = mob;
		this.velocity = velocity;
		setControls(EnumSet.of(Goal.Control.JUMP, Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (mob.hasControllingPassenger()) {
			return false;
		}

		target = mob.getTarget();
		if (target == null) {
			return false;
		}

		double distSq = mob.squaredDistanceTo(target);
		if (distSq < MIN_POUNCE_DISTANCE_SQ || distSq > MAX_POUNCE_DISTANCE_SQ) {
			return false;
		}

		return mob.isOnGround() && mob.getRandom().nextInt(toGoalTicks(POUNCE_CHANCE_TICKS)) == 0;
	}

	@Override
	public boolean shouldContinue() {
		return !mob.isOnGround();
	}

	@Override
	public void start() {
		Vec3d currentVelocity = mob.getVelocity();
		Vec3d direction = new Vec3d(target.getX() - mob.getX(), 0.0, target.getZ() - mob.getZ());
		if (direction.lengthSquared() > MIN_DIRECTION_LENGTH_SQ) {
			direction = direction.normalize().multiply(HORIZONTAL_IMPULSE).add(currentVelocity.multiply(VELOCITY_CARRY));
		}

		mob.setVelocity(direction.x, velocity, direction.z);
	}
}
