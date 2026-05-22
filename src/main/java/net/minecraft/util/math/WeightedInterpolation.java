package net.minecraft.util.math;

/**
 * Трёхмерная взвешенная интерполяция на основе B-сплайна 5-го порядка.
 * Использует 6×6×6 = 216 соседних точек с весами из биномиального ряда {0,1,4,6,4,1,0}.
 */
public class WeightedInterpolation {

	private static final int FIRST_SEGMENT_OFFSET = 2;
	private static final int NUM_SEGMENTS = 6;
	private static final double[] ENDPOINT_WEIGHTS = new double[]{0.0, 1.0, 4.0, 6.0, 4.0, 1.0, 0.0};

	/**
	 * Выполняет взвешенную интерполяцию значений в окрестности позиции {@code pos}.
	 * Для каждой из 216 соседних ячеек вычисляется вес и вызывается {@code accum.accumulate}.
	 *
	 * @param pos   позиция в мировом пространстве
	 * @param f     функция, возвращающая значение для целочисленных координат
	 * @param accum аккумулятор, собирающий взвешенные значения
	 */
	public static <V> void interpolate(
		Vec3d pos,
		WeightedInterpolation.PositionalFunction<V> f,
		WeightedInterpolation.Accumulator<V> accum
	) {
		pos = pos.subtract(0.5, 0.5, 0.5);
		int baseX = MathHelper.floor(pos.getX());
		int baseY = MathHelper.floor(pos.getY());
		int baseZ = MathHelper.floor(pos.getZ());
		double fracX = pos.getX() - baseX;
		double fracY = pos.getY() - baseY;
		double fracZ = pos.getZ() - baseZ;

		for (int dz = 0; dz < NUM_SEGMENTS; dz++) {
			double weightZ = MathHelper.lerp(fracZ, ENDPOINT_WEIGHTS[dz + 1], ENDPOINT_WEIGHTS[dz]);
			int worldZ = baseZ - FIRST_SEGMENT_OFFSET + dz;

			for (int dx = 0; dx < NUM_SEGMENTS; dx++) {
				double weightX = MathHelper.lerp(fracX, ENDPOINT_WEIGHTS[dx + 1], ENDPOINT_WEIGHTS[dx]);
				int worldX = baseX - FIRST_SEGMENT_OFFSET + dx;

				for (int dy = 0; dy < NUM_SEGMENTS; dy++) {
					double weightY = MathHelper.lerp(fracY, ENDPOINT_WEIGHTS[dy + 1], ENDPOINT_WEIGHTS[dy]);
					int worldY = baseY - FIRST_SEGMENT_OFFSET + dy;
					double totalWeight = weightX * weightY * weightZ;
					V value = f.get(worldX, worldY, worldZ);
					accum.accumulate(totalWeight, value);
				}
			}
		}
	}

	/**
	 * Аккумулятор взвешенных значений.
	 */
	@FunctionalInterface
	public interface Accumulator<V> {

		void accumulate(double weight, V value);
	}

	/**
	 * Функция, возвращающая значение по целочисленным координатам.
	 */
	@FunctionalInterface
	public interface PositionalFunction<V> {

		V get(int x, int y, int z);
	}
}
