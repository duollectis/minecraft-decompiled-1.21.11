package net.minecraft.util.math.noise;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Трёхмерный интерполированный шум Перлина, используемый для генерации рельефа.
 * Комбинирует три октавных сэмплера: нижний, верхний и интерполяционный.
 * Интерполяционный шум определяет, какой из двух основных сэмплеров доминирует
 * в каждой точке пространства, создавая плавные переходы между разными типами рельефа.
 */
public class InterpolatedNoiseSampler implements DensityFunction.Base {

	private static final double PERLIN_SCALE = 684.412;
	private static final Codec<Double> SCALE_AND_FACTOR_RANGE = Codec.doubleRange(0.001, 1000.0);
	private static final MapCodec<InterpolatedNoiseSampler> MAP_CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			SCALE_AND_FACTOR_RANGE.fieldOf("xz_scale").forGetter(s -> s.xzScale),
			SCALE_AND_FACTOR_RANGE.fieldOf("y_scale").forGetter(s -> s.yScale),
			SCALE_AND_FACTOR_RANGE.fieldOf("xz_factor").forGetter(s -> s.xzFactor),
			SCALE_AND_FACTOR_RANGE.fieldOf("y_factor").forGetter(s -> s.yFactor),
			Codec.doubleRange(1.0, 8.0).fieldOf("smear_scale_multiplier").forGetter(s -> s.smearScaleMultiplier)
		).apply(instance, InterpolatedNoiseSampler::createBase3dNoiseFunction)
	);

	public static final CodecHolder<InterpolatedNoiseSampler> CODEC = CodecHolder.of(MAP_CODEC);

	private final OctavePerlinNoiseSampler lowerInterpolatedNoise;
	private final OctavePerlinNoiseSampler upperInterpolatedNoise;
	private final OctavePerlinNoiseSampler interpolationNoise;
	private final double scaledXzScale;
	private final double scaledYScale;
	private final double xzFactor;
	private final double yFactor;
	private final double smearScaleMultiplier;
	private final double maxValue;
	private final double xzScale;
	private final double yScale;

	public static InterpolatedNoiseSampler createBase3dNoiseFunction(
		double xzScale,
		double yScale,
		double xzFactor,
		double yFactor,
		double smearScaleMultiplier
	) {
		return new InterpolatedNoiseSampler(
			new Xoroshiro128PlusPlusRandom(0L),
			xzScale,
			yScale,
			xzFactor,
			yFactor,
			smearScaleMultiplier
		);
	}

	private InterpolatedNoiseSampler(
		OctavePerlinNoiseSampler lowerInterpolatedNoise,
		OctavePerlinNoiseSampler upperInterpolatedNoise,
		OctavePerlinNoiseSampler interpolationNoise,
		double xzScale,
		double yScale,
		double xzFactor,
		double yFactor,
		double smearScaleMultiplier
	) {
		this.lowerInterpolatedNoise = lowerInterpolatedNoise;
		this.upperInterpolatedNoise = upperInterpolatedNoise;
		this.interpolationNoise = interpolationNoise;
		this.xzScale = xzScale;
		this.yScale = yScale;
		this.xzFactor = xzFactor;
		this.yFactor = yFactor;
		this.smearScaleMultiplier = smearScaleMultiplier;
		this.scaledXzScale = PERLIN_SCALE * xzScale;
		this.scaledYScale = PERLIN_SCALE * yScale;
		this.maxValue = lowerInterpolatedNoise.getMaxValueForScale(scaledYScale);
	}

	@VisibleForTesting
	public InterpolatedNoiseSampler(
		Random random,
		double xzScale,
		double yScale,
		double xzFactor,
		double yFactor,
		double smearScaleMultiplier
	) {
		this(
			OctavePerlinNoiseSampler.createLegacy(random, IntStream.rangeClosed(-15, 0)),
			OctavePerlinNoiseSampler.createLegacy(random, IntStream.rangeClosed(-15, 0)),
			OctavePerlinNoiseSampler.createLegacy(random, IntStream.rangeClosed(-7, 0)),
			xzScale,
			yScale,
			xzFactor,
			yFactor,
			smearScaleMultiplier
		);
	}

	/**
	 * Создаёт копию сэмплера с новым источником случайности, сохраняя все параметры масштаба.
	 * Используется при инициализации чанков для получения уникального шума на каждый чанк.
	 *
	 * @param random новый источник случайности
	 * @return новый экземпляр с теми же параметрами, но другим шумом
	 */
	public InterpolatedNoiseSampler copyWithRandom(Random random) {
		return new InterpolatedNoiseSampler(random, xzScale, yScale, xzFactor, yFactor, smearScaleMultiplier);
	}

	@Override
	public double sample(DensityFunction.NoisePos pos) {
		double scaledX = pos.blockX() * scaledXzScale;
		double scaledY = pos.blockY() * scaledYScale;
		double scaledZ = pos.blockZ() * scaledXzScale;
		double interpX = scaledX / xzFactor;
		double interpY = scaledY / yFactor;
		double interpZ = scaledZ / xzFactor;
		double smearY = scaledYScale * smearScaleMultiplier;
		double smearYScaled = smearY / yFactor;

		double interpNoise = 0.0;
		double octaveScale = 1.0;

		for (int octave = 0; octave < 8; octave++) {
			PerlinNoiseSampler sampler = interpolationNoise.getOctave(octave);

			if (sampler != null) {
				interpNoise += sampler.sample(
					OctavePerlinNoiseSampler.maintainPrecision(interpX * octaveScale),
					OctavePerlinNoiseSampler.maintainPrecision(interpY * octaveScale),
					OctavePerlinNoiseSampler.maintainPrecision(interpZ * octaveScale),
					smearYScaled * octaveScale,
					interpY * octaveScale
				) / octaveScale;
			}

			octaveScale /= 2.0;
		}

		double blendFactor = (interpNoise / 10.0 + 1.0) / 2.0;
		boolean useUpperOnly = blendFactor >= 1.0;
		boolean useLowerOnly = blendFactor <= 0.0;
		octaveScale = 1.0;

		double lowerNoise = 0.0;
		double upperNoise = 0.0;

		for (int octave = 0; octave < 16; octave++) {
			double precX = OctavePerlinNoiseSampler.maintainPrecision(scaledX * octaveScale);
			double precY = OctavePerlinNoiseSampler.maintainPrecision(scaledY * octaveScale);
			double precZ = OctavePerlinNoiseSampler.maintainPrecision(scaledZ * octaveScale);
			double smearStep = smearY * octaveScale;

			if (!useUpperOnly) {
				PerlinNoiseSampler lowerSampler = lowerInterpolatedNoise.getOctave(octave);

				if (lowerSampler != null) {
					lowerNoise += lowerSampler.sample(precX, precY, precZ, smearStep, scaledY * octaveScale) / octaveScale;
				}
			}

			if (!useLowerOnly) {
				PerlinNoiseSampler upperSampler = upperInterpolatedNoise.getOctave(octave);

				if (upperSampler != null) {
					upperNoise += upperSampler.sample(precX, precY, precZ, smearStep, scaledY * octaveScale) / octaveScale;
				}
			}

			octaveScale /= 2.0;
		}

		return MathHelper.clampedLerp(blendFactor, lowerNoise / 512.0, upperNoise / 512.0) / 128.0;
	}

	@Override
	public double minValue() {
		return -maxValue();
	}

	@Override
	public double maxValue() {
		return maxValue;
	}

	@VisibleForTesting
	public void addDebugInfo(StringBuilder info) {
		info.append("BlendedNoise{minLimitNoise=");
		lowerInterpolatedNoise.addDebugInfo(info);
		info.append(", maxLimitNoise=");
		upperInterpolatedNoise.addDebugInfo(info);
		info.append(", mainNoise=");
		interpolationNoise.addDebugInfo(info);
		info.append(
			String.format(
				Locale.ROOT,
				", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=4, cellHeight=8",
				684.412,
				684.412,
				8.555150000000001,
				4.277575000000001
			)
		).append('}');
	}

	@Override
	public CodecHolder<? extends DensityFunction> getCodecHolder() {
		return CODEC;
	}
}
