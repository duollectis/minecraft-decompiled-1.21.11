package net.minecraft.world.biome.source.util;

import net.minecraft.util.function.ToFloatFunction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Spline;
import net.minecraft.world.gen.densityfunction.DensityFunctions;

/**
 * Создаёт сплайны для параметров рельефа ванильного Верхнего мира:
 * смещение (offset), фактор (factor) и зазубренность (jaggedness).
 * Все сплайны зависят от континентальности, эрозии и складок хребтов.
 */
public class VanillaTerrainParametersCreator {

	private static final float EROSION_THRESHOLD_1 = -0.51F;
	private static final float EROSION_THRESHOLD_2 = -0.4F;
	private static final float EROSION_THRESHOLD_3 = 0.1F;
	private static final float EROSION_THRESHOLD_4 = -0.15F;

	// Константы для вычисления смещения рельефа
	private static final float OFFSET_SCALE_FACTOR = 1.17F;
	private static final float OFFSET_COMPRESS_FACTOR = 0.46082947F;
	private static final float OFFSET_MIN_CLAMP = -0.2222F;

	private static final ToFloatFunction<Float> IDENTITY = ToFloatFunction.IDENTITY;
	private static final ToFloatFunction<Float> OFFSET_AMPLIFIER =
		ToFloatFunction.fromFloat(value -> value < 0.0F ? value : value * 2.0F);
	private static final ToFloatFunction<Float> FACTOR_AMPLIFIER =
		ToFloatFunction.fromFloat(value -> 1.25F - 6.25F / (value + 5.0F));
	private static final ToFloatFunction<Float> JAGGEDNESS_AMPLIFIER =
		ToFloatFunction.fromFloat(value -> value * 2.0F);

	/**
	 * Создаёт сплайн смещения рельефа по континентальности.
	 * Смещение определяет базовую высоту поверхности биома.
	 */
	public static <C, I extends ToFloatFunction<C>> Spline<C, I> createOffsetSpline(
		I continents,
		I erosion,
		I ridgesFolded,
		boolean amplified
	) {
		ToFloatFunction<Float> amplifier = amplified ? OFFSET_AMPLIFIER : IDENTITY;
		Spline<C, I> coastSpline = createContinentalOffsetSpline(
			erosion, ridgesFolded, EROSION_THRESHOLD_4, 0.0F, 0.0F, EROSION_THRESHOLD_3, 0.0F, -0.03F,
			false, false, amplifier
		);
		Spline<C, I> nearCoastSpline = createContinentalOffsetSpline(
			erosion, ridgesFolded, -0.1F, 0.03F, EROSION_THRESHOLD_3, EROSION_THRESHOLD_3, 0.01F, -0.03F,
			false, false, amplifier
		);
		Spline<C, I> inlandSpline = createContinentalOffsetSpline(
			erosion, ridgesFolded, -0.1F, 0.03F, EROSION_THRESHOLD_3, 0.7F, 0.01F, -0.03F,
			true, true, amplifier
		);
		Spline<C, I> farInlandSpline = createContinentalOffsetSpline(
			erosion, ridgesFolded, -0.05F, 0.03F, EROSION_THRESHOLD_3, 1.0F, 0.01F, 0.01F,
			true, true, amplifier
		);

		return Spline.<C, I>builder(continents, amplifier)
			.add(-1.1F, 0.044F)
			.add(-1.02F, OFFSET_MIN_CLAMP)
			.add(EROSION_THRESHOLD_1, OFFSET_MIN_CLAMP)
			.add(-0.44F, -0.12F)
			.add(-0.18F, -0.12F)
			.add(-0.16F, coastSpline)
			.add(EROSION_THRESHOLD_4, coastSpline)
			.add(-0.1F, nearCoastSpline)
			.add(0.25F, inlandSpline)
			.add(1.0F, farInlandSpline)
			.build();
	}

	/**
	 * Создаёт сплайн фактора рельефа по континентальности.
	 * Фактор определяет крутизну/масштаб вертикальных изменений рельефа.
	 */
	public static <C, I extends ToFloatFunction<C>> Spline<C, I> createFactorSpline(
		I continents,
		I erosion,
		I ridges,
		I ridgesFolded,
		boolean amplified
	) {
		ToFloatFunction<Float> amplifier = amplified ? FACTOR_AMPLIFIER : IDENTITY;
		return Spline.<C, I>builder(continents, IDENTITY)
			.add(-0.19F, 3.95F)
			.add(EROSION_THRESHOLD_4, createContinentalOffsetSplineInner(erosion, ridges, ridgesFolded, 6.25F, true, IDENTITY))
			.add(-0.1F, createContinentalOffsetSplineInner(erosion, ridges, ridgesFolded, 5.47F, true, amplifier))
			.add(0.03F, createContinentalOffsetSplineInner(erosion, ridges, ridgesFolded, 5.08F, true, amplifier))
			.add(0.06F, createContinentalOffsetSplineInner(erosion, ridges, ridgesFolded, 4.69F, false, amplifier))
			.build();
	}

	/**
	 * Создаёт сплайн зазубренности рельефа по континентальности.
	 * Зазубренность определяет остроту горных пиков.
	 */
	public static <C, I extends ToFloatFunction<C>> Spline<C, I> createJaggednessSpline(
		I continents,
		I erosion,
		I ridges,
		I ridgesFolded,
		boolean amplified
	) {
		ToFloatFunction<Float> amplifier = amplified ? JAGGEDNESS_AMPLIFIER : IDENTITY;
		return Spline.<C, I>builder(continents, amplifier)
			.add(-0.11F, 0.0F)
			.add(0.03F, createErosionRidgesSpline(erosion, ridges, ridgesFolded, 1.0F, 0.5F, 0.0F, 0.0F, amplifier))
			.add(0.65F, createErosionRidgesSpline(erosion, ridges, ridgesFolded, 1.0F, 1.0F, 1.0F, 0.0F, amplifier))
			.build();
	}

	private static <C, I extends ToFloatFunction<C>> Spline<C, I> createErosionRidgesSpline(
		I erosion,
		I ridges,
		I ridgesFolded,
		float highRidgeScale,
		float midRidgeScale,
		float highJaggedness,
		float midJaggedness,
		ToFloatFunction<Float> amplifier
	) {
		float erosionThreshold = -0.5775F;
		Spline<C, I> highSpline = createRidgesFoldedSpline(ridges, ridgesFolded, highRidgeScale, highJaggedness, amplifier);
		Spline<C, I> midSpline = createRidgesFoldedSpline(ridges, ridgesFolded, midRidgeScale, midJaggedness, amplifier);
		return Spline.<C, I>builder(erosion, amplifier)
			.add(-1.0F, highSpline)
			.add(-0.78F, midSpline)
			.add(erosionThreshold, midSpline)
			.add(-0.375F, 0.0F)
			.build();
	}

	private static <C, I extends ToFloatFunction<C>> Spline<C, I> createRidgesFoldedSpline(
		I ridges,
		I ridgesFolded,
		float highScale,
		float midScale,
		ToFloatFunction<Float> amplifier
	) {
		float peakNoise = DensityFunctions.getPeaksValleysNoise(0.4F);
		float valleyNoise = DensityFunctions.getPeaksValleysNoise(0.56666666F);
		float midNoise = (peakNoise + valleyNoise) / 2.0F;
		Spline.Builder<C, I> builder = Spline.builder(ridgesFolded, amplifier);
		builder.add(peakNoise, 0.0F);

		if (midScale > 0.0F) {
			builder.add(midNoise, createRidgesSpline(ridges, midScale, amplifier));
		} else {
			builder.add(midNoise, 0.0F);
		}

		if (highScale > 0.0F) {
			builder.add(1.0F, createRidgesSpline(ridges, highScale, amplifier));
		} else {
			builder.add(1.0F, 0.0F);
		}

		return builder.build();
	}

	private static <C, I extends ToFloatFunction<C>> Spline<C, I> createRidgesSpline(
		I ridges,
		float scale,
		ToFloatFunction<Float> amplifier
	) {
		float highValue = 0.63F * scale;
		float lowValue = 0.3F * scale;
		return Spline.<C, I>builder(ridges, amplifier).add(-0.01F, highValue).add(0.01F, lowValue).build();
	}

	private static <C, I extends ToFloatFunction<C>> Spline<C, I> createContinentalOffsetSplineInner(
		I erosion,
		I ridges,
		I ridgesFolded,
		float maxFactor,
		boolean includeHighlands,
		ToFloatFunction<Float> amplifier
	) {
		Spline<C, I> ridgesSpline = Spline.<C, I>builder(ridges, amplifier)
			.add(-0.2F, 6.3F)
			.add(0.2F, maxFactor)
			.build();
		Spline.Builder<C, I> builder = Spline.<C, I>builder(erosion, amplifier)
			.add(-0.6F, ridgesSpline)
			.add(
				-0.5F,
				Spline.<C, I>builder(ridges, amplifier).add(-0.05F, 6.3F).add(0.05F, 2.67F).build()
			)
			.add(-0.35F, ridgesSpline)
			.add(-0.25F, ridgesSpline)
			.add(
				-0.1F,
				Spline.<C, I>builder(ridges, amplifier).add(-0.05F, 2.67F).add(0.05F, 6.3F).build()
			)
			.add(0.03F, ridgesSpline);

		if (includeHighlands) {
			Spline<C, I> highlandsRidgesSpline = Spline.<C, I>builder(ridges, amplifier)
				.add(0.0F, maxFactor)
				.add(EROSION_THRESHOLD_3, 0.625F)
				.build();
			Spline<C, I> highlandsSpline = Spline.<C, I>builder(ridgesFolded, amplifier)
				.add(-0.9F, maxFactor)
				.add(-0.69F, highlandsRidgesSpline)
				.build();
			builder.add(0.35F, maxFactor).add(0.45F, highlandsSpline).add(0.55F, highlandsSpline).add(0.62F, maxFactor);
		} else {
			Spline<C, I> lowlandsSpline = Spline.<C, I>builder(ridgesFolded, amplifier)
				.add(-0.7F, ridgesSpline)
				.add(EROSION_THRESHOLD_4, 1.37F)
				.build();
			Spline<C, I> coastalSpline = Spline.<C, I>builder(ridgesFolded, amplifier)
				.add(0.45F, ridgesSpline)
				.add(0.7F, 1.56F)
				.build();
			builder
				.add(0.05F, coastalSpline)
				.add(0.4F, coastalSpline)
				.add(0.45F, lowlandsSpline)
				.add(0.55F, lowlandsSpline)
				.add(0.58F, maxFactor);
		}

		return builder.build();
	}

	private static float computeSlope(float fromValue, float toValue, float fromPos, float toPos) {
		return (toValue - fromValue) / (toPos - fromPos);
	}

	private static <C, I extends ToFloatFunction<C>> Spline<C, I> createRidgesFoldedOffsetSpline(
		I ridgesFolded,
		float continentalness,
		boolean preferHighValues,
		ToFloatFunction<Float> amplifier
	) {
		Spline.Builder<C, I> builder = Spline.builder(ridgesFolded, amplifier);
		float ridgeThreshold = -0.7F;
		float startPos = -1.0F;
		float startValue = getOffsetValue(startPos, continentalness, ridgeThreshold);
		float endPos = 1.0F;
		float endValue = getOffsetValue(endPos, continentalness, ridgeThreshold);
		float jaggednessThreshold = computeJaggednessThreshold(continentalness);
		float jaggednessStart = -0.65F;

		if (jaggednessStart < jaggednessThreshold && jaggednessThreshold < endPos) {
			float jaggednessStartValue = getOffsetValue(jaggednessStart, continentalness, ridgeThreshold);
			float preJaggednessPos = -0.75F;
			float preJaggednessValue = getOffsetValue(preJaggednessPos, continentalness, ridgeThreshold);
			float slopeToPreJaggedness = computeSlope(startValue, preJaggednessValue, startPos, preJaggednessPos);
			builder.add(startPos, startValue, slopeToPreJaggedness);
			builder.add(preJaggednessPos, preJaggednessValue);
			builder.add(jaggednessStart, jaggednessStartValue);
			float jaggednessValue = getOffsetValue(jaggednessThreshold, continentalness, ridgeThreshold);
			float slopeFromJaggedness = computeSlope(jaggednessValue, endValue, jaggednessThreshold, endPos);
			float epsilon = 0.01F;
			builder.add(jaggednessThreshold - epsilon, jaggednessValue);
			builder.add(jaggednessThreshold, jaggednessValue, slopeFromJaggedness);
			builder.add(endPos, endValue, slopeFromJaggedness);
		} else {
			float slope = computeSlope(startValue, endValue, startPos, endPos);

			if (preferHighValues) {
				builder.add(startPos, Math.max(0.2F, startValue));
				builder.add(0.0F, MathHelper.lerp(0.5F, startValue, endValue), slope);
			} else {
				builder.add(startPos, startValue, slope);
			}

			builder.add(endPos, endValue, slope);
		}

		return builder.build();
	}

	/**
	 * Вычисляет значение смещения рельефа для заданной позиции на хребте.
	 * Формула основана на линейном преобразовании с зажимом снизу.
	 */
	private static float getOffsetValue(float ridgePos, float continentalness, float ridgeThreshold) {
		float scaledContinentalness = 1.0F - (1.0F - continentalness) * 0.5F;
		float halfInverseContinentalness = 0.5F * (1.0F - continentalness);
		float scaledRidgePos = (ridgePos + OFFSET_SCALE_FACTOR) * OFFSET_COMPRESS_FACTOR;
		float rawValue = scaledRidgePos * scaledContinentalness - halfInverseContinentalness;
		return ridgePos < ridgeThreshold ? Math.max(rawValue, OFFSET_MIN_CLAMP) : Math.max(rawValue, 0.0F);
	}

	/**
	 * Вычисляет порог зазубренности — позицию на хребте, где начинается зазубренный рельеф.
	 */
	private static float computeJaggednessThreshold(float continentalness) {
		float scaledContinentalness = 1.0F - (1.0F - continentalness) * 0.5F;
		float halfInverseContinentalness = 0.5F * (1.0F - continentalness);
		return halfInverseContinentalness / (OFFSET_COMPRESS_FACTOR * scaledContinentalness) - OFFSET_SCALE_FACTOR;
	}

	/**
	 * Создаёт сплайн смещения рельефа по эрозии для заданного уровня континентальности.
	 * Параметры {@code bl} и {@code bl2} управляют включением горных и прибрежных секций.
	 */
	public static <C, I extends ToFloatFunction<C>> Spline<C, I> createContinentalOffsetSpline(
		I erosion,
		I ridgesFolded,
		float continentalness,
		float nearCoastOffset,
		float coastOffset,
		float inlandOffset,
		float farInlandOffset,
		float deepInlandOffset,
		boolean includeMountains,
		boolean preferHighRidges,
		ToFloatFunction<Float> amplifier
	) {
		Spline<C, I> highRidgeSpline = createRidgesFoldedOffsetSpline(
			ridgesFolded, MathHelper.lerp(inlandOffset, 0.6F, 1.5F), preferHighRidges, amplifier
		);
		Spline<C, I> midRidgeSpline = createRidgesFoldedOffsetSpline(
			ridgesFolded, MathHelper.lerp(inlandOffset, 0.6F, 1.0F), preferHighRidges, amplifier
		);
		Spline<C, I> lowRidgeSpline = createRidgesFoldedOffsetSpline(
			ridgesFolded, inlandOffset, preferHighRidges, amplifier
		);
		Spline<C, I> coastalRidgeSpline = createRidgesOffsetSpline(
			ridgesFolded,
			continentalness - 0.15F,
			0.5F * inlandOffset,
			MathHelper.lerp(0.5F, 0.5F, 0.5F) * inlandOffset,
			0.5F * inlandOffset,
			0.6F * inlandOffset,
			0.5F,
			amplifier
		);
		Spline<C, I> nearCoastRidgeSpline = createRidgesOffsetSpline(
			ridgesFolded, continentalness, farInlandOffset * inlandOffset, nearCoastOffset * inlandOffset,
			0.5F * inlandOffset, 0.6F * inlandOffset, 0.5F, amplifier
		);
		Spline<C, I> shoreRidgeSpline = createRidgesOffsetSpline(
			ridgesFolded, continentalness, farInlandOffset, farInlandOffset, nearCoastOffset, coastOffset, 0.5F, amplifier
		);
		Spline<C, I> deepShoreRidgeSpline = createRidgesOffsetSpline(
			ridgesFolded, continentalness, farInlandOffset, farInlandOffset, nearCoastOffset, coastOffset, 0.5F, amplifier
		);
		Spline<C, I> oceanRidgeSpline = Spline.<C, I>builder(ridgesFolded, amplifier)
			.add(-1.0F, continentalness)
			.add(EROSION_THRESHOLD_2, shoreRidgeSpline)
			.add(0.0F, coastOffset + 0.07F)
			.build();
		Spline<C, I> deepOceanRidgeSpline = createRidgesOffsetSpline(
			ridgesFolded, -0.02F, deepInlandOffset, deepInlandOffset, nearCoastOffset, coastOffset, 0.0F, amplifier
		);
		Spline.Builder<C, I> builder = Spline.<C, I>builder(erosion, amplifier)
			.add(-0.85F, highRidgeSpline)
			.add(-0.7F, midRidgeSpline)
			.add(EROSION_THRESHOLD_2, lowRidgeSpline)
			.add(-0.35F, coastalRidgeSpline)
			.add(-0.1F, nearCoastRidgeSpline)
			.add(0.2F, shoreRidgeSpline);

		if (includeMountains) {
			builder
				.add(0.4F, deepShoreRidgeSpline)
				.add(0.45F, oceanRidgeSpline)
				.add(0.55F, oceanRidgeSpline)
				.add(0.58F, deepShoreRidgeSpline);
		}

		builder.add(0.7F, deepOceanRidgeSpline);
		return builder.build();
	}

	private static <C, I extends ToFloatFunction<C>> Spline<C, I> createRidgesOffsetSpline(
		I ridgesFolded,
		float baseOffset,
		float nearPeakOffset,
		float peakOffset,
		float valleyOffset,
		float deepValleyOffset,
		float minSlope,
		ToFloatFunction<Float> amplifier
	) {
		float slopeToNearPeak = Math.max(0.5F * (nearPeakOffset - baseOffset), minSlope);
		float slopeToPeak = 5.0F * (peakOffset - nearPeakOffset);
		return Spline.<C, I>builder(ridgesFolded, amplifier)
			.add(-1.0F, baseOffset, slopeToNearPeak)
			.add(EROSION_THRESHOLD_2, nearPeakOffset, Math.min(slopeToNearPeak, slopeToPeak))
			.add(0.0F, peakOffset, slopeToPeak)
			.add(0.4F, valleyOffset, 2.0F * (valleyOffset - peakOffset))
			.add(1.0F, deepValleyOffset, 0.7F * (deepValleyOffset - valleyOffset))
			.build();
	}
}
