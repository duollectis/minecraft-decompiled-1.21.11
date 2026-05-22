package net.minecraft.util.math;

/**
 * Сглаживает входящий поток значений, ограничивая скорость изменения через адаптивную задержку движения.
 * Используется для плавной интерполяции позиций и углов в клиентской логике.
 */
public class Smoother {

	private double actualSum;
	private double smoothedSum;
	private double movementLatency;

	/**
	 * Вычисляет сглаженное приращение для текущего кадра.
	 * Алгоритм ограничивает скорость изменения через экспоненциальное сглаживание задержки,
	 * предотвращая резкие скачки при внезапных изменениях входного значения.
	 *
	 * @param original  исходное приращение за текущий тик
	 * @param smoother  коэффициент сглаживания (0..1), чем меньше — тем плавнее
	 * @return сглаженное приращение, готовое к применению
	 */
	public double smooth(double original, double smoother) {
		actualSum += original;
		double delta = actualSum - smoothedSum;
		double latency = MathHelper.lerp(0.5, movementLatency, delta);
		double sign = Math.signum(delta);

		if (sign * delta > sign * movementLatency) {
			delta = latency;
		}

		movementLatency = latency;
		smoothedSum += delta * smoother;

		return delta * smoother;
	}

	public void clear() {
		actualSum = 0.0;
		smoothedSum = 0.0;
		movementLatency = 0.0;
	}
}
