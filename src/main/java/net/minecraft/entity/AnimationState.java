package net.minecraft.entity;

import java.util.function.Consumer;

/**
 * {@code AnimationState}.
 */
public class AnimationState {

	private static final int STOPPED = Integer.MIN_VALUE;
	private int startTick = Integer.MIN_VALUE;

	/**
	 * Start.
	 *
	 * @param tick tick
	 */
	public void start(int tick) {
		this.startTick = tick;
	}

	/**
	 * Запускает if not running.
	 *
	 * @param tick tick
	 */
	public void startIfNotRunning(int tick) {
		if (!this.isRunning()) {
			this.start(tick);
		}
	}

	public void setRunning(boolean running, int tick) {
		if (running) {
			this.startIfNotRunning(tick);
		}
		else {
			this.stop();
		}
	}

	/**
	 * Stop.
	 */
	public void stop() {
		this.startTick = Integer.MIN_VALUE;
	}

	/**
	 * Run.
	 *
	 * @param consumer consumer
	 */
	public void run(Consumer<AnimationState> consumer) {
		if (this.isRunning()) {
			consumer.accept(this);
		}
	}

	/**
	 * Skip.
	 *
	 * @param ticks ticks
	 * @param speedMultiplier speed multiplier
	 */
	public void skip(int ticks, float speedMultiplier) {
		if (this.isRunning()) {
			this.startTick -= (int) (ticks * speedMultiplier);
		}
	}

	public long getTimeInMilliseconds(float age) {
		float f = age - this.startTick;
		return (long) (f * 50.0F);
	}

	public boolean isRunning() {
		return this.startTick != Integer.MIN_VALUE;
	}

	/**
	 * Создаёт копию from.
	 *
	 * @param state state
	 */
	public void copyFrom(AnimationState state) {
		this.startTick = state.startTick;
	}
}
