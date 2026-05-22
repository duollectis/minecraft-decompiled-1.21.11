package net.minecraft.entity.ai.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * Цель блуждания вблизи текущей цели атаки.
 * Моб выбирает случайную позицию в направлении цели и идёт к ней,
 * пока цель жива и находится в пределах {@code maxDistance}.
 */
public class WanderNearTargetGoal extends Goal {

	private static final int WANDER_HORIZONTAL_RANGE = 16;
	private static final int WANDER_VERTICAL_RANGE = 7;
	private static final float HALF_PI = (float) (Math.PI / 2);

	private final PathAwareEntity mob;
	private final double speed;
	private final float maxDistance;
	private @Nullable LivingEntity target;
	private double targetX;
	private double targetY;
	private double targetZ;

	public WanderNearTargetGoal(PathAwareEntity mob, double speed, float maxDistance) {
		this.mob = mob;
		this.speed = speed;
		this.maxDistance = maxDistance;
		setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		target = mob.getTarget();

		if (target == null) {
			return false;
		}

		if (target.squaredDistanceTo(mob) > maxDistance * maxDistance) {
			return false;
		}

		Vec3d wanderPos = NoPenaltyTargeting.findTo(
				mob,
				WANDER_HORIZONTAL_RANGE,
				WANDER_VERTICAL_RANGE,
				target.getEntityPos(),
				HALF_PI
		);

		if (wanderPos == null) {
			return false;
		}

		targetX = wanderPos.x;
		targetY = wanderPos.y;
		targetZ = wanderPos.z;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return !mob.getNavigation().isIdle()
				&& target.isAlive()
				&& target.squaredDistanceTo(mob) < maxDistance * maxDistance;
	}

	@Override
	public void stop() {
		target = null;
	}

	@Override
	public void start() {
		mob.getNavigation().startMovingTo(targetX, targetY, targetZ, speed);
	}
}
