package net.minecraft.util;

/**
 * {@code Cooldown}.
 */
public class Cooldown {

	private final int increment;
	private final int threshold;
	private int current;

	public Cooldown(int increment, int threshold) {
		this.increment = increment;
		this.threshold = threshold;
	}

	/**
	 * Increment.
	 */
	public void increment() {
		this.current = this.current + this.increment;
	}

	/**
	 * Tick.
	 */
	public void tick() {
		if (this.current > 0) {
			this.current--;
		}
	}

	/**
	 * Проверяет возможность use.
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canUse() {
		return this.current < this.threshold;
	}
}
