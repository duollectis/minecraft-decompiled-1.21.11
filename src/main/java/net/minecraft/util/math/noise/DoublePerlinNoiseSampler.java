package net.minecraft.util.math.noise;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

import java.util.List;

/**
 * Двойной шум Перлина: суммирует два октавных шума Перлина с небольшим
 * смещением домена второго сэмплера ({@link #DOMAIN_SCALE}), что устраняет
 * артефакты оси и делает шум более изотропным.
 */
public class DoublePerlinNoiseSampler {

	private static final double DOMAIN_SCALE = 1.0181268882175227;
	private static final double AMPLITUDE_FACTOR = 0.3333333333333333;

	private final double amplitude;
	private final OctavePerlinNoiseSampler firstSampler;
	private final OctavePerlinNoiseSampler secondSampler;
	private final double maxValue;
	private final NoiseParameters parameters;

	@Deprecated
	public static DoublePerlinNoiseSampler createLegacy(Random random, NoiseParameters parameters) {
		return new DoublePerlinNoiseSampler(random, parameters, false);
	}

	public static DoublePerlinNoiseSampler create(Random random, int offset, double... octaves) {
		return create(random, new NoiseParameters(offset, new DoubleArrayList(octaves)));
	}

	public static DoublePerlinNoiseSampler create(Random random, NoiseParameters parameters) {
		return new DoublePerlinNoiseSampler(random, parameters, true);
	}

	private DoublePerlinNoiseSampler(Random random, NoiseParameters parameters, boolean modern) {
		int firstOctave = parameters.firstOctave;
		DoubleList amplitudes = parameters.amplitudes;
		this.parameters = parameters;

		if (modern) {
			firstSampler = OctavePerlinNoiseSampler.create(random, firstOctave, amplitudes);
			secondSampler = OctavePerlinNoiseSampler.create(random, firstOctave, amplitudes);
		} else {
			firstSampler = OctavePerlinNoiseSampler.createLegacy(random, firstOctave, amplitudes);
			secondSampler = OctavePerlinNoiseSampler.createLegacy(random, firstOctave, amplitudes);
		}

		int minIndex = Integer.MAX_VALUE;
		int maxIndex = Integer.MIN_VALUE;
		DoubleListIterator iterator = amplitudes.iterator();

		while (iterator.hasNext()) {
			int index = iterator.nextIndex();
			double value = iterator.nextDouble();

			if (value != 0.0) {
				minIndex = Math.min(minIndex, index);
				maxIndex = Math.max(maxIndex, index);
			}
		}

		amplitude = 0.16666666666666666 / createAmplitude(maxIndex - minIndex);
		maxValue = (firstSampler.getMaxValue() + secondSampler.getMaxValue()) * amplitude;
	}

	public double getMaxValue() {
		return maxValue;
	}

	private static double createAmplitude(int octaves) {
		return 0.1 * (1.0 + 1.0 / (octaves + 1));
	}

	public double sample(double x, double y, double z) {
		double scaledX = x * DOMAIN_SCALE;
		double scaledY = y * DOMAIN_SCALE;
		double scaledZ = z * DOMAIN_SCALE;

		return (firstSampler.sample(x, y, z) + secondSampler.sample(scaledX, scaledY, scaledZ)) * amplitude;
	}

	public NoiseParameters copy() {
		return parameters;
	}

	@VisibleForTesting
	public void addDebugInfo(StringBuilder info) {
		info.append("NormalNoise {");
		info.append("first: ");
		firstSampler.addDebugInfo(info);
		info.append(", second: ");
		secondSampler.addDebugInfo(info);
		info.append("}");
	}

	public record NoiseParameters(int firstOctave, DoubleList amplitudes) {

		public static final Codec<NoiseParameters> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.INT.fieldOf("firstOctave").forGetter(NoiseParameters::firstOctave),
				Codec.DOUBLE.listOf().fieldOf("amplitudes").forGetter(NoiseParameters::amplitudes)
			).apply(instance, NoiseParameters::new)
		);

		public static final Codec<RegistryEntry<NoiseParameters>> REGISTRY_ENTRY_CODEC =
			RegistryElementCodec.of(RegistryKeys.NOISE_PARAMETERS, CODEC);

		public NoiseParameters(int firstOctave, List<Double> amplitudes) {
			this(firstOctave, new DoubleArrayList(amplitudes));
		}

		public NoiseParameters(int firstOctave, double firstAmplitude, double... amplitudes) {
			this(
				firstOctave,
				Util.make(
					new DoubleArrayList(amplitudes),
					list -> list.add(0, firstAmplitude)
				)
			);
		}
	}
}
