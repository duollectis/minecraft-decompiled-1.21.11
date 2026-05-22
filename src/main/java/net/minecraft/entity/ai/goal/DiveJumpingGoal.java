package net.minecraft.entity.ai.goal;

import java.util.EnumSet;

/** Базовая цель прыжка с нырком: захватывает управление движением и прыжком. */
public abstract class DiveJumpingGoal extends Goal {

	public DiveJumpingGoal() {
		this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP));
	}
}
