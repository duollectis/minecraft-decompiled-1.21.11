package net.minecraft.entity.ai.goal;

import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * Цель, направляющая моба к его позиционной цели ({@code positionTarget})
 * без штрафных зон. Активируется, когда моб находится вне целевого радиуса.
 */
public class GoToWalkTargetGoal extends Goal {

	private static final int SEARCH_HORIZONTAL_RANGE = 16;
	private static final int SEARCH_VERTICAL_RANGE = 7;

	private final PathAwareEntity mob;
	private double targetX;
	private double targetY;
	private double targetZ;
	private final double speed;

	public GoToWalkTargetGoal(PathAwareEntity mob, double speed) {
		this.mob = mob;
		this.speed = speed;
		this.setControls(EnumSet.of(Goal.Control.MOVE));
	}

	@Override
	public boolean canStart() {
		if (mob.isInPositionTargetRange()) {
			return false;
		}

		Vec3d target = NoPenaltyTargeting.findTo(
			mob,
			SEARCH_HORIZONTAL_RANGE,
			SEARCH_VERTICAL_RANGE,
			Vec3d.ofBottomCenter(mob.getPositionTarget()),
			(float) (Math.PI / 2)
		);

		if (target == null) {
			return false;
		}

		targetX = target.x;
		targetY = target.y;
		targetZ = target.z;
		return true;
	}

	@Override
	public boolean shouldContinue() {
		return !mob.getNavigation().isIdle();
	}

	@Override
	public void start() {
		mob.getNavigation().startMovingTo(targetX, targetY, targetZ, speed);
	}
}
