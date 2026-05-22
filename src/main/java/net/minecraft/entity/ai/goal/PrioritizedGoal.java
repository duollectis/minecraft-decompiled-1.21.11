package net.minecraft.entity.ai.goal;

import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/** Обёртка над {@link Goal}, добавляющая приоритет и флаг активности для {@link GoalSelector}. */
public class PrioritizedGoal extends Goal {

	private final Goal goal;
	private final int priority;
	private boolean running;

	public PrioritizedGoal(int priority, Goal goal) {
		this.priority = priority;
		this.goal = goal;
	}

	public boolean canBeReplacedBy(PrioritizedGoal other) {
		return canStop() && other.getPriority() < priority;
	}

	@Override
	public boolean canStart() {
		return goal.canStart();
	}

	@Override
	public boolean shouldContinue() {
		return goal.shouldContinue();
	}

	@Override
	public boolean canStop() {
		return goal.canStop();
	}

	@Override
	public void start() {
		if (!running) {
			running = true;
			goal.start();
		}
	}

	@Override
	public void stop() {
		if (running) {
			running = false;
			goal.stop();
		}
	}

	@Override
	public boolean shouldRunEveryTick() {
		return goal.shouldRunEveryTick();
	}

	@Override
	protected int getTickCount(int ticks) {
		return goal.getTickCount(ticks);
	}

	@Override
	public void tick() {
		goal.tick();
	}

	@Override
	public void setControls(EnumSet<Goal.Control> controls) {
		goal.setControls(controls);
	}

	@Override
	public EnumSet<Goal.Control> getControls() {
		return goal.getControls();
	}

	public boolean isRunning() {
		return running;
	}

	public int getPriority() {
		return priority;
	}

	public Goal getGoal() {
		return goal;
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}

		return o != null && getClass() == o.getClass() && goal.equals(((PrioritizedGoal) o).goal);
	}

	@Override
	public int hashCode() {
		return goal.hashCode();
	}
}
