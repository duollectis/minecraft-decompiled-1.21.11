package net.minecraft.util.math.noise;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.RandomSplitter;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Октавный шум Перлина: суммирует несколько октав {@link PerlinNoiseSampler}
 * с нарастающей частотой (лакунарность) и убывающей амплитудой (персистентность).
 * Поддерживает два режима инициализации: современный (xoroshiro) и устаревший (legacy).
 */
public class OctavePerlinNoiseSampler {

	private static final int MAX_OCTAVE_COUNT = 33554432;

	private final @Nullable PerlinNoiseSampler[] octaveSamplers;
	private final int firstOctave;
	private final DoubleList amplitudes;
	private final double persistence;
	private final double lacunarity;
	private final double maxValue;

	@Deprecated
	public static OctavePerlinNoiseSampler createLegacy(Random random, IntStream octaves) {
		return new OctavePerlinNoiseSampler(
			random,
			calculateAmplitudes(new IntRBTreeSet(octaves.boxed().collect(ImmutableList.toImmutableList()))),
			false
		);
	}

	@Deprecated
	public static OctavePerlinNoiseSampler createLegacy(Random random, int offset, DoubleList amplitudes) {
		return new OctavePerlinNoiseSampler(random, Pair.of(offset, amplitudes), false);
	}

	public static OctavePerlinNoiseSampler create(Random random, IntStream octaves) {
		return create(random, octaves.boxed().collect(ImmutableList.toImmutableList()));
	}

	public static OctavePerlinNoiseSampler create(Random random, List<Integer> octaves) {
		return new OctavePerlinNoiseSampler(random, calculateAmplitudes(new IntRBTreeSet(octaves)), true);
	}

	public static OctavePerlinNoiseSampler create(
		Random random,
		int offset,
		double firstAmplitude,
		double... amplitudes
	) {
		DoubleArrayList list = new DoubleArrayList(amplitudes);
		list.add(0, firstAmplitude);

		return new OctavePerlinNoiseSampler(random, Pair.of(offset, list), true);
	}

	public static OctavePerlinNoiseSampler create(Random random, int offset, DoubleList amplitudes) {
		return new OctavePerlinNoiseSampler(random, Pair.of(offset, amplitudes), true);
	}

	private static Pair<Integer, DoubleList> calculateAmplitudes(IntSortedSet octaves) {
		if (octaves.isEmpty()) {
			throw new IllegalArgumentException("Need some octaves!");
		}

		int negFirst = -octaves.firstInt();
		int last = octaves.lastInt();
		int count = negFirst + last + 1;

		if (count < 1) {
			throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
		}

		DoubleList amplitudeList = new DoubleArrayList(new double[count]);
		IntBidirectionalIterator iterator = octaves.iterator();

		while (iterator.hasNext()) {
			amplitudeList.set(iterator.nextInt() + negFirst, 1.0);
		}

		return Pair.of(-negFirst, amplitudeList);
	}

	protected OctavePerlinNoiseSampler(
		Random random,
		Pair<Integer, DoubleList> firstOctaveAndAmplitudes,
		boolean xoroshiro
	) {
		firstOctave = (Integer) firstOctaveAndAmplitudes.getFirst();
		amplitudes = (DoubleList) firstOctaveAndAmplitudes.getSecond();

		int octaveCount = amplitudes.size();
		int zeroOctaveIndex = -firstOctave;
		octaveSamplers = new PerlinNoiseSampler[octaveCount];

		if (xoroshiro) {
			RandomSplitter splitter = random.nextSplitter();

			for (int k = 0; k < octaveCount; k++) {
				if (amplitudes.getDouble(k) != 0.0) {
					int octaveId = firstOctave + k;
					octaveSamplers[k] = new PerlinNoiseSampler(splitter.split("octave_" + octaveId));
				}
			}
		} else {
			PerlinNoiseSampler zeroSampler = new PerlinNoiseSampler(random);

			if (zeroOctaveIndex >= 0 && zeroOctaveIndex < octaveCount) {
				if (amplitudes.getDouble(zeroOctaveIndex) != 0.0) {
					octaveSamplers[zeroOctaveIndex] = zeroSampler;
				}
			}

			for (int k = zeroOctaveIndex - 1; k >= 0; k--) {
				if (k < octaveCount) {
					double amp = amplitudes.getDouble(k);

					if (amp != 0.0) {
						octaveSamplers[k] = new PerlinNoiseSampler(random);
					} else {
						skipCalls(random);
					}
				} else {
					skipCalls(random);
				}
			}

			if (Arrays.stream(octaveSamplers).filter(Objects::nonNull).count()
				!= amplitudes.stream().filter(amp -> amp != 0.0).count()
			) {
				throw new IllegalStateException(
					"Failed to create correct number of noise levels for given non-zero amplitudes"
				);
			}

			if (zeroOctaveIndex < octaveCount - 1) {
				throw new IllegalArgumentException("Positive octaves are temporarily disabled");
			}
		}

		lacunarity = Math.pow(2.0, -zeroOctaveIndex);
		persistence = Math.pow(2.0, octaveCount - 1) / (Math.pow(2.0, octaveCount) - 1.0);
		maxValue = getTotalAmplitude(2.0);
	}

	protected double getMaxValue() {
		return maxValue;
	}

	private static void skipCalls(Random random) {
		random.skip(262);
	}

	public double sample(double x, double y, double z) {
		return sample(x, y, z, 0.0, 0.0, false);
	}

	@Deprecated
	public double sample(double x, double y, double z, double yScale, double yMax, boolean useOrigin) {
		double result = 0.0;
		double freq = lacunarity;
		double amp = persistence;

		for (int i = 0; i < octaveSamplers.length; i++) {
			PerlinNoiseSampler sampler = octaveSamplers[i];

			if (sampler != null) {
				double value = sampler.sample(
					maintainPrecision(x * freq),
					useOrigin ? -sampler.originY : maintainPrecision(y * freq),
					maintainPrecision(z * freq),
					yScale * freq,
					yMax * freq
				);
				result += amplitudes.getDouble(i) * value * amp;
			}

			freq *= 2.0;
			amp /= 2.0;
		}

		return result;
	}

	public double getMaxValueForScale(double scale) {
		return getTotalAmplitude(scale + 2.0);
	}

	private double getTotalAmplitude(double scale) {
		double total = 0.0;
		double amp = persistence;

		for (int i = 0; i < octaveSamplers.length; i++) {
			if (octaveSamplers[i] != null) {
				total += amplitudes.getDouble(i) * scale * amp;
			}

			amp /= 2.0;
		}

		return total;
	}

	public @Nullable PerlinNoiseSampler getOctave(int octave) {
		return octaveSamplers[octaveSamplers.length - 1 - octave];
	}

	/**
	 * Устраняет накопление ошибок с плавающей точкой при больших координатах,
	 * приводя значение в диапазон [-MAX_OCTAVE_COUNT/2, MAX_OCTAVE_COUNT/2].
	 *
	 * @param value входное значение координаты
	 * @return значение с восстановленной точностью
	 */
	public static double maintainPrecision(double value) {
		return value - MathHelper.lfloor(value / 3.3554432E7 + 0.5) * 3.3554432E7;
	}

	protected int getFirstOctave() {
		return firstOctave;
	}

	protected DoubleList getAmplitudes() {
		return amplitudes;
	}

	@VisibleForTesting
	public void addDebugInfo(StringBuilder info) {
		List<String> ampStrings = amplitudes
			.stream()
			.map(amp -> String.format(Locale.ROOT, "%.2f", amp))
			.toList();

		info.append("PerlinNoise{")
			.append("first octave: ")
			.append(firstOctave)
			.append(", amplitudes: ")
			.append(ampStrings)
			.append(", noise levels: [");

		for (int i = 0; i < octaveSamplers.length; i++) {
			info.append(i).append(": ");
			PerlinNoiseSampler sampler = octaveSamplers[i];

			if (sampler == null) {
				info.append("null");
			} else {
				sampler.addDebugInfo(info);
			}

			info.append(", ");
		}

		info.append("]}");
	}
}
