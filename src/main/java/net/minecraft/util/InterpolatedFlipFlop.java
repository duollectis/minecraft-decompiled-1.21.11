package net.minecraft.util;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.attribute.timeline.EasingType;

/**
 * Интерполированный переключатель состояния (вкл/выкл) с плавным переходом.
 * <p>
 * Хранит текущее и предыдущее значения счётчика в диапазоне {@code [0, frames]}.
 * При вызове {@link #tick(boolean)} счётчик плавно движется к целевому значению.
 * Метод {@link #getValue(float)} возвращает интерполированное значение в диапазоне {@code [0.0, 1.0]}
 * с применением функции сглаживания.
 */
public class InterpolatedFlipFlop {

	private final int frames;
	private final EasingType smoothingFunction;
	private int current;
	private int previous;

	public InterpolatedFlipFlop(int frames, EasingType smoothingFunction) {
		this.frames = frames;
		this.smoothingFunction = smoothingFunction;
	}

	public InterpolatedFlipFlop(int frames) {
		this(frames, EasingType.LINEAR);
	}

	/**
	 * Обновляет состояние переключателя за один тик.
	 * Если {@code active = true}, счётчик увеличивается до {@link #frames}.
	 * Если {@code active = false}, счётчик уменьшается до 0.
	 *
	 * @param active целевое состояние переключателя
	 */
	public void tick(boolean active) {
		previous = current;

		if (active) {
			if (current < frames) {
				current++;
			}
		} else if (current > 0) {
			current--;
		}
	}

	/**
	 * Возвращает интерполированное значение переключателя в диапазоне {@code [0.0, 1.0]}.
	 *
	 * @param tickProgress прогресс текущего тика в диапазоне {@code [0.0, 1.0]}
	 * @return сглаженное значение переключателя
	 */
	public float getValue(float tickProgress) {
		float normalized = MathHelper.lerp(tickProgress, (float) previous, (float) current) / frames;
		return smoothingFunction.apply(normalized);
	}
}
