package net.minecraft.world.gen.noise;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.function.Function;

/**
 * Маршрутизатор шумов мирогенерации — хранит все функции плотности,
 * используемые при генерации рельефа, биомов, жил и уровней жидкостей.
 */
public record NoiseRouter(
	DensityFunction barrierNoise,
	DensityFunction fluidLevelFloodednessNoise,
	DensityFunction fluidLevelSpreadNoise,
	DensityFunction lavaNoise,
	DensityFunction temperature,
	DensityFunction vegetation,
	DensityFunction continents,
	DensityFunction erosion,
	DensityFunction depth,
	DensityFunction ridges,
	DensityFunction preliminarySurfaceLevel,
	DensityFunction finalDensity,
	DensityFunction veinToggle,
	DensityFunction veinRidged,
	DensityFunction veinGap
) {

	public static final Codec<NoiseRouter> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
			field("barrier", NoiseRouter::barrierNoise),
			field("fluid_level_floodedness", NoiseRouter::fluidLevelFloodednessNoise),
			field("fluid_level_spread", NoiseRouter::fluidLevelSpreadNoise),
			field("lava", NoiseRouter::lavaNoise),
			field("temperature", NoiseRouter::temperature),
			field("vegetation", NoiseRouter::vegetation),
			field("continents", NoiseRouter::continents),
			field("erosion", NoiseRouter::erosion),
			field("depth", NoiseRouter::depth),
			field("ridges", NoiseRouter::ridges),
			field("preliminary_surface_level", NoiseRouter::preliminarySurfaceLevel),
			field("final_density", NoiseRouter::finalDensity),
			field("vein_toggle", NoiseRouter::veinToggle),
			field("vein_ridged", NoiseRouter::veinRidged),
			field("vein_gap", NoiseRouter::veinGap)
		)
		.apply(instance, NoiseRouter::new)
	);

	private static RecordCodecBuilder<NoiseRouter, DensityFunction> field(
		String name,
		Function<NoiseRouter, DensityFunction> getter
	) {
		return DensityFunction.FUNCTION_CODEC.fieldOf(name).forGetter(getter);
	}

	/**
	 * Применяет {@link DensityFunction.DensityFunctionVisitor} ко всем функциям плотности
	 * этого маршрутизатора и возвращает новый экземпляр с преобразованными функциями.
	 */
	public NoiseRouter apply(DensityFunction.DensityFunctionVisitor visitor) {
		return new NoiseRouter(
			barrierNoise.apply(visitor),
			fluidLevelFloodednessNoise.apply(visitor),
			fluidLevelSpreadNoise.apply(visitor),
			lavaNoise.apply(visitor),
			temperature.apply(visitor),
			vegetation.apply(visitor),
			continents.apply(visitor),
			erosion.apply(visitor),
			depth.apply(visitor),
			ridges.apply(visitor),
			preliminarySurfaceLevel.apply(visitor),
			finalDensity.apply(visitor),
			veinToggle.apply(visitor),
			veinRidged.apply(visitor),
			veinGap.apply(visitor)
		);
	}
}
