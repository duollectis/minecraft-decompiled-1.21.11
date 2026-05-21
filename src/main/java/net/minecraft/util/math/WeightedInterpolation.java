package net.minecraft.util.math;

/**
 * {@code WeightedInterpolation}.
 */
public class WeightedInterpolation {

	private static final int FIRST_SEGMENT_OFFSET = 2;
	private static final int NUM_SEGMENTS = 6;
	private static final double[] ENDPOINT_WEIGHTS = new double[]{0.0, 1.0, 4.0, 6.0, 4.0, 1.0, 0.0};

	public static <V> void interpolate(
			Vec3d pos,
			WeightedInterpolation.PositionalFunction<V> f,
			WeightedInterpolation.Accumulator<V> accum
	) {
		pos = pos.subtract(0.5, 0.5, 0.5);
		int i = MathHelper.floor(pos.getX());
		int j = MathHelper.floor(pos.getY());
		int k = MathHelper.floor(pos.getZ());
		double d = pos.getX() - i;
		double e = pos.getY() - j;
		double g = pos.getZ() - k;

		for (int l = 0; l < 6; l++) {
			double h = MathHelper.lerp(g, ENDPOINT_WEIGHTS[l + 1], ENDPOINT_WEIGHTS[l]);
			int m = k - 2 + l;

			for (int n = 0; n < 6; n++) {
				double o = MathHelper.lerp(d, ENDPOINT_WEIGHTS[n + 1], ENDPOINT_WEIGHTS[n]);
				int p = i - 2 + n;

				for (int q = 0; q < 6; q++) {
					double r = MathHelper.lerp(e, ENDPOINT_WEIGHTS[q + 1], ENDPOINT_WEIGHTS[q]);
					int s = j - 2 + q;
					double t = o * r * h;
					V object = f.get(p, s, m);
					accum.accumulate(t, object);
				}
			}
		}
	}

	@FunctionalInterface
	/**
	 * {@code Accumulator}.
	 */
	public interface Accumulator<V> {

		void accumulate(double weight, V value);
	}

	@FunctionalInterface
	/**
	 * {@code PositionalFunction}.
	 */
	public interface PositionalFunction<V> {

		V get(int x, int y, int z);
	}
}
