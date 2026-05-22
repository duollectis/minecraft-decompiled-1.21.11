package net.minecraft.util;

/**
 * Счётчик перезарядки с настраиваемым шагом и порогом.
 * <p>
 * Каждый тик значение уменьшается на 1. При вызове {@link #increment()}
 * значение увеличивается на заданный шаг. Действие доступно, пока
 * текущее значение не достигло порога.
 */
public class Cooldown {

	private final int increment;
	private final int threshold;
	private int current;

	public Cooldown(int increment, int threshold) {
		this.increment = increment;
		this.threshold = threshold;
	}

	/** Увеличивает счётчик перезарядки на заданный шаг. */
	public void increment() {
		current += increment;
	}

	/** Уменьшает счётчик перезарядки на 1 за тик, не опускаясь ниже нуля. */
	public void tick() {
		if (current > 0) {
			current--;
		}
	}

	/** @return {@code true} если действие доступно (счётчик не достиг порога) */
	public boolean canUse() {
		return current < threshold;
	}
}
