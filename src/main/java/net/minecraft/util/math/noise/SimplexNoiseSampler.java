package net.minecraft.util.math.noise;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

/**
 * Симплекс-шум Кена Перлина (2001). Более эффективная альтернатива классическому
 * шуму Перлина: использует симплексную решётку вместо кубической, что даёт
 * меньше артефактов направленности и лучшую производительность в 3D.
 */
public class SimplexNoiseSampler {

	protected static final int[][] GRADIENTS = {
		{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
		{1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
		{0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
		{1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}
	};

	private static final double SQRT_3 = Math.sqrt(3.0);
	private static final double SKEW_FACTOR_2D = 0.5 * (SQRT_3 - 1.0);
	private static final double UNSKEW_FACTOR_2D = (3.0 - SQRT_3) / 6.0;
	// Коэффициенты скоса/обратного скоса для 3D симплексной решётки
	private static final double SKEW_FACTOR_3D = 1.0 / 3.0;
	private static final double UNSKEW_FACTOR_3D = 1.0 / 6.0;

	private final int[] permutation = new int[512];
	public final double originX;
	public final double originY;
	public final double originZ;

	public SimplexNoiseSampler(Random random) {
		originX = random.nextDouble() * 256.0;
		originY = random.nextDouble() * 256.0;
		originZ = random.nextDouble() * 256.0;

		int i = 0;
		while (i < 256) {
			permutation[i] = i++;
		}

		for (int ix = 0; ix < 256; ix++) {
			int j = random.nextInt(256 - ix);
			int temp = permutation[ix];
			permutation[ix] = permutation[j + ix];
			permutation[j + ix] = temp;
		}
	}

	private int map(int input) {
		return permutation[input & 0xFF];
	}

	protected static double dot(int[] gradient, double x, double y, double z) {
		return gradient[0] * x + gradient[1] * y + gradient[2] * z;
	}

	private double grad(int hash, double x, double y, double z, double distance) {
		double contribution = distance - x * x - y * y - z * z;

		if (contribution < 0.0) {
			return 0.0;
		}

		contribution *= contribution;

		return contribution * contribution * dot(GRADIENTS[hash], x, y, z);
	}

	public double sample(double x, double y) {
		double skew = (x + y) * SKEW_FACTOR_2D;
		int gridX = MathHelper.floor(x + skew);
		int gridY = MathHelper.floor(y + skew);
		double unskew = (gridX + gridY) * UNSKEW_FACTOR_2D;
		double cellX = gridX - unskew;
		double cellY = gridY - unskew;
		double localX = x - cellX;
		double localY = y - cellY;

		int stepX;
		int stepY;

		if (localX > localY) {
			stepX = 1;
			stepY = 0;
		} else {
			stepX = 0;
			stepY = 1;
		}

		double midX = localX - stepX + UNSKEW_FACTOR_2D;
		double midY = localY - stepY + UNSKEW_FACTOR_2D;
		double farX = localX - 1.0 + 2.0 * UNSKEW_FACTOR_2D;
		double farY = localY - 1.0 + 2.0 * UNSKEW_FACTOR_2D;

		int rx = gridX & 0xFF;
		int ry = gridY & 0xFF;
		int hash0 = map(rx + map(ry)) % 12;
		int hash1 = map(rx + stepX + map(ry + stepY)) % 12;
		int hash2 = map(rx + 1 + map(ry + 1)) % 12;

		double contrib0 = grad(hash0, localX, localY, 0.0, 0.5);
		double contrib1 = grad(hash1, midX, midY, 0.0, 0.5);
		double contrib2 = grad(hash2, farX, farY, 0.0, 0.5);

		return 70.0 * (contrib0 + contrib1 + contrib2);
	}

	public double sample(double x, double y, double z) {
		double skew = (x + y + z) * SKEW_FACTOR_3D;
		int gridX = MathHelper.floor(x + skew);
		int gridY = MathHelper.floor(y + skew);
		int gridZ = MathHelper.floor(z + skew);
		double unskew = (gridX + gridY + gridZ) * UNSKEW_FACTOR_3D;
		double cellX = gridX - unskew;
		double cellY = gridY - unskew;
		double cellZ = gridZ - unskew;
		double localX = x - cellX;
		double localY = y - cellY;
		double localZ = z - cellZ;

		// Определяем порядок обхода симплекса (6 возможных вариантов)
		int step1X;
		int step1Y;
		int step1Z;
		int step2X;
		int step2Y;
		int step2Z;

		if (localX >= localY) {
			if (localY >= localZ) {
				step1X = 1; step1Y = 0; step1Z = 0;
				step2X = 1; step2Y = 1; step2Z = 0;
			} else if (localX >= localZ) {
				step1X = 1; step1Y = 0; step1Z = 0;
				step2X = 1; step2Y = 0; step2Z = 1;
			} else {
				step1X = 0; step1Y = 0; step1Z = 1;
				step2X = 1; step2Y = 0; step2Z = 1;
			}
		} else if (localY < localZ) {
			step1X = 0; step1Y = 0; step1Z = 1;
			step2X = 0; step2Y = 1; step2Z = 1;
		} else if (localX < localZ) {
			step1X = 0; step1Y = 1; step1Z = 0;
			step2X = 0; step2Y = 1; step2Z = 1;
		} else {
			step1X = 0; step1Y = 1; step1Z = 0;
			step2X = 1; step2Y = 1; step2Z = 0;
		}

		double mid1X = localX - step1X + UNSKEW_FACTOR_3D;
		double mid1Y = localY - step1Y + UNSKEW_FACTOR_3D;
		double mid1Z = localZ - step1Z + UNSKEW_FACTOR_3D;
		double mid2X = localX - step2X + 2.0 * UNSKEW_FACTOR_3D;
		double mid2Y = localY - step2Y + 2.0 * UNSKEW_FACTOR_3D;
		double mid2Z = localZ - step2Z + 2.0 * UNSKEW_FACTOR_3D;
		double farX = localX - 1.0 + 3.0 * UNSKEW_FACTOR_3D;
		double farY = localY - 1.0 + 3.0 * UNSKEW_FACTOR_3D;
		double farZ = localZ - 1.0 + 3.0 * UNSKEW_FACTOR_3D;

		int rx = gridX & 0xFF;
		int ry = gridY & 0xFF;
		int rz = gridZ & 0xFF;
		int hash0 = map(rx + map(ry + map(rz))) % 12;
		int hash1 = map(rx + step1X + map(ry + step1Y + map(rz + step1Z))) % 12;
		int hash2 = map(rx + step2X + map(ry + step2Y + map(rz + step2Z))) % 12;
		int hash3 = map(rx + 1 + map(ry + 1 + map(rz + 1))) % 12;

		double contrib0 = grad(hash0, localX, localY, localZ, 0.6);
		double contrib1 = grad(hash1, mid1X, mid1Y, mid1Z, 0.6);
		double contrib2 = grad(hash2, mid2X, mid2Y, mid2Z, 0.6);
		double contrib3 = grad(hash3, farX, farY, farZ, 0.6);

		return 32.0 * (contrib0 + contrib1 + contrib2 + contrib3);
	}
}
