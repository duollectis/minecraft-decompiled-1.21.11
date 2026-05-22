package net.minecraft.util.math.noise;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Октавный симплекс-шум: суммирует несколько октав {@link SimplexNoiseSampler}
 * с нарастающей частотой и убывающей амплитудой. Используется для генерации
 * двумерных карт (например, температуры и влажности биомов в старых версиях).
 */
public class OctaveSimplexNoiseSampler {

	private final @Nullable SimplexNoiseSampler[] octaveSamplers;
	private final double persistence;
	private final double lacunarity;

	public OctaveSimplexNoiseSampler(Random random, List<Integer> octaves) {
		this(random, new IntRBTreeSet(octaves));
	}

	private OctaveSimplexNoiseSampler(Random random, IntSortedSet octaves) {
		if (octaves.isEmpty()) {
			throw new IllegalArgumentException("Need some octaves!");
		}

		int negFirst = -octaves.firstInt();
		int last = octaves.lastInt();
		int count = negFirst + last + 1;

		if (count < 1) {
			throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
		}

		SimplexNoiseSampler zeroSampler = new SimplexNoiseSampler(random);
		octaveSamplers = new SimplexNoiseSampler[count];

		if (last >= 0 && last < count && octaves.contains(0)) {
			octaveSamplers[last] = zeroSampler;
		}

		for (int k = last + 1; k < count; k++) {
			if (k >= 0 && octaves.contains(last - k)) {
				octaveSamplers[k] = new SimplexNoiseSampler(random);
			} else {
				random.skip(262);
			}
		}

		if (last > 0) {
			// Семя для нижних октав вычисляется из значения нулевого сэмплера в его origin-точке
			long seed = (long) (zeroSampler.sample(
				zeroSampler.originX,
				zeroSampler.originY,
				zeroSampler.originZ
			) * 9.223372E18F);
			Random lowerRandom = new ChunkRandom(new CheckedRandom(seed));

			for (int k = last - 1; k >= 0; k--) {
				if (k < count && octaves.contains(last - k)) {
					octaveSamplers[k] = new SimplexNoiseSampler(lowerRandom);
				} else {
					lowerRandom.skip(262);
				}
			}
		}

		lacunarity = Math.pow(2.0, last);
		persistence = 1.0 / (Math.pow(2.0, count) - 1.0);
	}

	public double sample(double x, double y, boolean useOrigin) {
		double result = 0.0;
		double freq = lacunarity;
		double amp = persistence;

		for (SimplexNoiseSampler sampler : octaveSamplers) {
			if (sampler != null) {
				result += sampler.sample(
					x * freq + (useOrigin ? sampler.originX : 0.0),
					y * freq + (useOrigin ? sampler.originY : 0.0)
				) * amp;
			}

			freq /= 2.0;
			amp *= 2.0;
		}

		return result;
	}
}
