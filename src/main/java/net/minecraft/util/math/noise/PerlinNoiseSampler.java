package net.minecraft.util.math.noise;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.noise.NoiseHelper;

/**
 * Одна октава классического шума Перлина (1985). Инициализируется случайной
 * таблицей перестановок и случайным смещением origin для уникальности каждого экземпляра.
 * Поддерживает как стандартную выборку, так и выборку с производными (градиентами).
 */
public final class PerlinNoiseSampler {

	private static final float EPSILON = 1.0E-7F;

	private final byte[] permutation;
	public final double originX;
	public final double originY;
	public final double originZ;

	public PerlinNoiseSampler(Random random) {
		originX = random.nextDouble() * 256.0;
		originY = random.nextDouble() * 256.0;
		originZ = random.nextDouble() * 256.0;
		permutation = new byte[256];

		for (int i = 0; i < 256; i++) {
			permutation[i] = (byte) i;
		}

		for (int i = 0; i < 256; i++) {
			int j = random.nextInt(256 - i);
			byte temp = permutation[i];
			permutation[i] = permutation[i + j];
			permutation[i + j] = temp;
		}
	}

	public double sample(double x, double y, double z) {
		return sample(x, y, z, 0.0, 0.0);
	}

	@Deprecated
	public double sample(double x, double y, double z, double yScale, double yMax) {
		double shiftedX = x + originX;
		double shiftedY = y + originY;
		double shiftedZ = z + originZ;
		int sectionX = MathHelper.floor(shiftedX);
		int sectionY = MathHelper.floor(shiftedY);
		int sectionZ = MathHelper.floor(shiftedZ);
		double localX = shiftedX - sectionX;
		double localY = shiftedY - sectionY;
		double localZ = shiftedZ - sectionZ;

		double fadeY;

		if (yScale != 0.0) {
			double clampedY = (yMax >= 0.0 && yMax < localY) ? yMax : localY;
			fadeY = MathHelper.floor(clampedY / yScale + EPSILON) * yScale;
		} else {
			fadeY = 0.0;
		}

		return sample(sectionX, sectionY, sectionZ, localX, localY - fadeY, localZ, localY);
	}

	/**
	 * Вычисляет шум Перлина вместе с частными производными по x, y, z.
	 * Производные накапливаются в массиве {@code ds} (индексы 0, 1, 2).
	 * Используется для вычисления нормалей поверхности и деформации домена.
	 *
	 * @param x координата X
	 * @param y координата Y
	 * @param z координата Z
	 * @param ds массив для накопления производных [dx, dy, dz]
	 * @return значение шума в точке
	 */
	public double sampleDerivative(double x, double y, double z, double[] ds) {
		double shiftedX = x + originX;
		double shiftedY = y + originY;
		double shiftedZ = z + originZ;
		int sectionX = MathHelper.floor(shiftedX);
		int sectionY = MathHelper.floor(shiftedY);
		int sectionZ = MathHelper.floor(shiftedZ);
		double localX = shiftedX - sectionX;
		double localY = shiftedY - sectionY;
		double localZ = shiftedZ - sectionZ;

		return sampleDerivative(sectionX, sectionY, sectionZ, localX, localY, localZ, ds);
	}

	private static double grad(int hash, double x, double y, double z) {
		return SimplexNoiseSampler.dot(SimplexNoiseSampler.GRADIENTS[hash & 15], x, y, z);
	}

	private int map(int input) {
		return permutation[input & 0xFF] & 0xFF;
	}

	private double sample(
		int sectionX,
		int sectionY,
		int sectionZ,
		double localX,
		double localY,
		double localZ,
		double fadeLocalY
	) {
		int h00 = map(sectionX);
		int h10 = map(sectionX + 1);
		int h000 = map(h00 + sectionY);
		int h001 = map(h00 + sectionY + 1);
		int h100 = map(h10 + sectionY);
		int h101 = map(h10 + sectionY + 1);

		double g000 = grad(map(h000 + sectionZ), localX, localY, localZ);
		double g100 = grad(map(h100 + sectionZ), localX - 1.0, localY, localZ);
		double g010 = grad(map(h001 + sectionZ), localX, localY - 1.0, localZ);
		double g110 = grad(map(h101 + sectionZ), localX - 1.0, localY - 1.0, localZ);
		double g001 = grad(map(h000 + sectionZ + 1), localX, localY, localZ - 1.0);
		double g101 = grad(map(h100 + sectionZ + 1), localX - 1.0, localY, localZ - 1.0);
		double g011 = grad(map(h001 + sectionZ + 1), localX, localY - 1.0, localZ - 1.0);
		double g111 = grad(map(h101 + sectionZ + 1), localX - 1.0, localY - 1.0, localZ - 1.0);

		double fadeX = MathHelper.perlinFade(localX);
		double fadeY = MathHelper.perlinFade(fadeLocalY);
		double fadeZ = MathHelper.perlinFade(localZ);

		return MathHelper.lerp3(fadeX, fadeY, fadeZ, g000, g100, g010, g110, g001, g101, g011, g111);
	}

	private double sampleDerivative(
		int sectionX,
		int sectionY,
		int sectionZ,
		double localX,
		double localY,
		double localZ,
		double[] ds
	) {
		int h00 = map(sectionX);
		int h10 = map(sectionX + 1);
		int h000 = map(h00 + sectionY);
		int h001 = map(h00 + sectionY + 1);
		int h100 = map(h10 + sectionY);
		int h101 = map(h10 + sectionY + 1);
		int p000 = map(h000 + sectionZ);
		int p100 = map(h100 + sectionZ);
		int p010 = map(h001 + sectionZ);
		int p110 = map(h101 + sectionZ);
		int p001 = map(h000 + sectionZ + 1);
		int p101 = map(h100 + sectionZ + 1);
		int p011 = map(h001 + sectionZ + 1);
		int p111 = map(h101 + sectionZ + 1);

		int[] g000 = SimplexNoiseSampler.GRADIENTS[p000 & 15];
		int[] g100 = SimplexNoiseSampler.GRADIENTS[p100 & 15];
		int[] g010 = SimplexNoiseSampler.GRADIENTS[p010 & 15];
		int[] g110 = SimplexNoiseSampler.GRADIENTS[p110 & 15];
		int[] g001 = SimplexNoiseSampler.GRADIENTS[p001 & 15];
		int[] g101 = SimplexNoiseSampler.GRADIENTS[p101 & 15];
		int[] g011 = SimplexNoiseSampler.GRADIENTS[p011 & 15];
		int[] g111 = SimplexNoiseSampler.GRADIENTS[p111 & 15];

		double v000 = SimplexNoiseSampler.dot(g000, localX, localY, localZ);
		double v100 = SimplexNoiseSampler.dot(g100, localX - 1.0, localY, localZ);
		double v010 = SimplexNoiseSampler.dot(g010, localX, localY - 1.0, localZ);
		double v110 = SimplexNoiseSampler.dot(g110, localX - 1.0, localY - 1.0, localZ);
		double v001 = SimplexNoiseSampler.dot(g001, localX, localY, localZ - 1.0);
		double v101 = SimplexNoiseSampler.dot(g101, localX - 1.0, localY, localZ - 1.0);
		double v011 = SimplexNoiseSampler.dot(g011, localX, localY - 1.0, localZ - 1.0);
		double v111 = SimplexNoiseSampler.dot(g111, localX - 1.0, localY - 1.0, localZ - 1.0);

		double fadeX = MathHelper.perlinFade(localX);
		double fadeY = MathHelper.perlinFade(localY);
		double fadeZ = MathHelper.perlinFade(localZ);

		double gradX = MathHelper.lerp3(fadeX, fadeY, fadeZ, g000[0], g100[0], g010[0], g110[0], g001[0], g101[0], g011[0], g111[0]);
		double gradY = MathHelper.lerp3(fadeX, fadeY, fadeZ, g000[1], g100[1], g010[1], g110[1], g001[1], g101[1], g011[1], g111[1]);
		double gradZ = MathHelper.lerp3(fadeX, fadeY, fadeZ, g000[2], g100[2], g010[2], g110[2], g001[2], g101[2], g011[2], g111[2]);

		double dX = MathHelper.lerp2(fadeY, fadeZ, v100 - v000, v110 - v010, v101 - v001, v111 - v011);
		double dY = MathHelper.lerp2(fadeZ, fadeX, v010 - v000, v011 - v001, v110 - v100, v111 - v101);
		double dZ = MathHelper.lerp2(fadeX, fadeY, v001 - v000, v101 - v100, v011 - v010, v111 - v110);

		double derivX = gradX + MathHelper.perlinFadeDerivative(localX) * dX;
		double derivY = gradY + MathHelper.perlinFadeDerivative(localY) * dY;
		double derivZ = gradZ + MathHelper.perlinFadeDerivative(localZ) * dZ;

		ds[0] += derivX;
		ds[1] += derivY;
		ds[2] += derivZ;

		return MathHelper.lerp3(fadeX, fadeY, fadeZ, v000, v100, v010, v110, v001, v101, v011, v111);
	}

	@VisibleForTesting
	public void addDebugInfo(StringBuilder info) {
		NoiseHelper.appendDebugInfo(info, originX, originY, originZ, permutation);
	}
}
