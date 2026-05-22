package net.minecraft.entity.ai.goal;

import net.minecraft.entity.mob.MobEntity;

/**
 * Расширение {@link DoorInteractGoal}: открывает дверь на фиксированное время
 * ({@link #OPEN_DURATION_TICKS} тиков), после чего закрывает её.
 * Параметр {@code delayedClose} управляет тем, нужно ли вообще закрывать дверь.
 */
public class LongDoorInteractGoal extends DoorInteractGoal {

	private static final int OPEN_DURATION_TICKS = 20;

	private final boolean delayedClose;
	private int ticksLeft;

	public LongDoorInteractGoal(MobEntity mob, boolean delayedClose) {
		super(mob);
		this.mob = mob;
		this.delayedClose = delayedClose;
	}

	@Override
	public boolean shouldContinue() {
		return delayedClose && ticksLeft > 0 && super.shouldContinue();
	}

	@Override
	public void start() {
		ticksLeft = OPEN_DURATION_TICKS;
		setDoorOpen(true);
	}

	@Override
	public void stop() {
		setDoorOpen(false);
	}

	@Override
	public void tick() {
		ticksLeft--;
		super.tick();
	}
}
