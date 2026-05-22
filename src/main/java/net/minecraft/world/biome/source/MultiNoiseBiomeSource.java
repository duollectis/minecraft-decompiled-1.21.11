package net.minecraft.world.biome.source;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.biome.source.util.VanillaBiomeParameters;
import net.minecraft.world.gen.densityfunction.DensityFunctions;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Источник биомов на основе многомерного шума (температура, влажность, континентальность,
 * эрозия, глубина, странность). Поддерживает два режима: кастомный (прямой список записей)
 * и пресетный (ссылка на {@link MultiNoiseBiomeSourceParameterList} из реестра).
 */
public class MultiNoiseBiomeSource extends BiomeSource {

	private static final MapCodec<RegistryEntry<Biome>> BIOME_CODEC = Biome.REGISTRY_CODEC.fieldOf("biome");

	public static final MapCodec<MultiNoiseUtil.Entries<RegistryEntry<Biome>>> CUSTOM_CODEC =
		MultiNoiseUtil.Entries.createCodec(BIOME_CODEC).fieldOf("biomes");

	private static final MapCodec<RegistryEntry<MultiNoiseBiomeSourceParameterList>> PRESET_CODEC =
		MultiNoiseBiomeSourceParameterList.REGISTRY_CODEC
			.fieldOf("preset")
			.withLifecycle(Lifecycle.stable());

	public static final MapCodec<MultiNoiseBiomeSource> CODEC = Codec.mapEither(CUSTOM_CODEC, PRESET_CODEC)
		.xmap(
			MultiNoiseBiomeSource::new,
			source -> source.biomeEntries
		);

	private final Either<MultiNoiseUtil.Entries<RegistryEntry<Biome>>, RegistryEntry<MultiNoiseBiomeSourceParameterList>>
		biomeEntries;

	private MultiNoiseBiomeSource(
		Either<MultiNoiseUtil.Entries<RegistryEntry<Biome>>, RegistryEntry<MultiNoiseBiomeSourceParameterList>> biomeEntries
	) {
		this.biomeEntries = biomeEntries;
	}

	public static MultiNoiseBiomeSource create(MultiNoiseUtil.Entries<RegistryEntry<Biome>> biomeEntries) {
		return new MultiNoiseBiomeSource(Either.left(biomeEntries));
	}

	public static MultiNoiseBiomeSource create(RegistryEntry<MultiNoiseBiomeSourceParameterList> parameterList) {
		return new MultiNoiseBiomeSource(Either.right(parameterList));
	}

	private MultiNoiseUtil.Entries<RegistryEntry<Biome>> getBiomeEntries() {
		return biomeEntries.map(
			entries -> entries,
			parameterListEntry -> parameterListEntry.value().getEntries()
		);
	}

	@Override
	protected Stream<RegistryEntry<Biome>> biomeStream() {
		return getBiomeEntries().getEntries().stream().map(Pair::getSecond);
	}

	@Override
	protected MapCodec<? extends BiomeSource> getCodec() {
		return CODEC;
	}

	/**
	 * Проверяет, соответствует ли этот источник биомов заданному пресету из реестра.
	 * Используется для определения типа генерации мира в UI и командах.
	 */
	public boolean matchesInstance(RegistryKey<MultiNoiseBiomeSourceParameterList> parameterList) {
		Optional<RegistryEntry<MultiNoiseBiomeSourceParameterList>> preset = biomeEntries.right();
		return preset.isPresent() && preset.get().matchesKey(parameterList);
	}

	@Override
	public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
		return getBiomeAtPoint(noise.sample(x, y, z));
	}

	@Debug
	public RegistryEntry<Biome> getBiomeAtPoint(MultiNoiseUtil.NoiseValuePoint point) {
		return getBiomeEntries().get(point);
	}

	@Override
	public void addDebugInfo(List<String> info, BlockPos pos, MultiNoiseUtil.MultiNoiseSampler noiseSampler) {
		int biomeX = BiomeCoords.fromBlock(pos.getX());
		int biomeY = BiomeCoords.fromBlock(pos.getY());
		int biomeZ = BiomeCoords.fromBlock(pos.getZ());
		MultiNoiseUtil.NoiseValuePoint noisePoint = noiseSampler.sample(biomeX, biomeY, biomeZ);
		float continentalness = MultiNoiseUtil.toFloat(noisePoint.continentalnessNoise());
		float erosion = MultiNoiseUtil.toFloat(noisePoint.erosionNoise());
		float temperature = MultiNoiseUtil.toFloat(noisePoint.temperatureNoise());
		float humidity = MultiNoiseUtil.toFloat(noisePoint.humidityNoise());
		float weirdness = MultiNoiseUtil.toFloat(noisePoint.weirdnessNoise());
		double peaksValleys = DensityFunctions.getPeaksValleysNoise(weirdness);
		VanillaBiomeParameters biomeParams = new VanillaBiomeParameters();
		info.add(
			"Biome builder PV: "
				+ VanillaBiomeParameters.getPeaksValleysDescription(peaksValleys)
				+ " C: "
				+ biomeParams.getContinentalnessDescription(continentalness)
				+ " E: "
				+ biomeParams.getErosionDescription(erosion)
				+ " T: "
				+ biomeParams.getTemperatureDescription(temperature)
				+ " H: "
				+ biomeParams.getHumidityDescription(humidity)
		);
	}
}
