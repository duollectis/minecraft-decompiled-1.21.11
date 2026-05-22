package net.minecraft.world.biome;

import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.feature.DefaultBiomeFeatures;
import net.minecraft.world.gen.feature.EndPlacedFeatures;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Фабрика биомов измерения Края (The End).
 * Все биомы Края разделяют общую базу через {@link #createEndBiome}.
 */
public class TheEndBiomeCreator {

	private static final int END_WATER_COLOR = 4159204;
	private static final float END_TEMPERATURE = 0.5F;
	private static final float END_DOWNFALL = 0.5F;

	/**
	 * Создаёт базовый биом Края с общими параметрами для всех вариантов.
	 * Все биомы Края не имеют осадков и используют одинаковый цвет воды.
	 */
	public static Biome createEndBiome(GenerationSettings.LookupBackedBuilder generationBuilder) {
		SpawnSettings.Builder spawnBuilder = new SpawnSettings.Builder();
		DefaultBiomeFeatures.addEndMobs(spawnBuilder);

		return new Biome.Builder()
			.precipitation(false)
			.temperature(END_TEMPERATURE)
			.downfall(END_DOWNFALL)
			.effects(new BiomeEffects.Builder().waterColor(END_WATER_COLOR).build())
			.spawnSettings(spawnBuilder.build())
			.generationSettings(generationBuilder.build())
			.build();
	}

	public static Biome createEndBarrens(
		RegistryEntryLookup<PlacedFeature> featureLookup,
		RegistryEntryLookup<ConfiguredCarver<?>> carverLookup
	) {
		GenerationSettings.LookupBackedBuilder generationBuilder =
			new GenerationSettings.LookupBackedBuilder(featureLookup, carverLookup);

		return createEndBiome(generationBuilder);
	}

	public static Biome createTheEnd(
		RegistryEntryLookup<PlacedFeature> featureLookup,
		RegistryEntryLookup<ConfiguredCarver<?>> carverLookup
	) {
		GenerationSettings.LookupBackedBuilder generationBuilder =
			new GenerationSettings.LookupBackedBuilder(featureLookup, carverLookup)
				.feature(GenerationStep.Feature.SURFACE_STRUCTURES, EndPlacedFeatures.END_SPIKE)
				.feature(GenerationStep.Feature.TOP_LAYER_MODIFICATION, EndPlacedFeatures.END_PLATFORM);

		return createEndBiome(generationBuilder);
	}

	public static Biome createEndMidlands(
		RegistryEntryLookup<PlacedFeature> featureLookup,
		RegistryEntryLookup<ConfiguredCarver<?>> carverLookup
	) {
		GenerationSettings.LookupBackedBuilder generationBuilder =
			new GenerationSettings.LookupBackedBuilder(featureLookup, carverLookup);

		return createEndBiome(generationBuilder);
	}

	public static Biome createEndHighlands(
		RegistryEntryLookup<PlacedFeature> featureLookup,
		RegistryEntryLookup<ConfiguredCarver<?>> carverLookup
	) {
		GenerationSettings.LookupBackedBuilder generationBuilder =
			new GenerationSettings.LookupBackedBuilder(featureLookup, carverLookup)
				.feature(GenerationStep.Feature.SURFACE_STRUCTURES, EndPlacedFeatures.END_GATEWAY_RETURN)
				.feature(GenerationStep.Feature.VEGETAL_DECORATION, EndPlacedFeatures.CHORUS_PLANT);

		return createEndBiome(generationBuilder);
	}

	public static Biome createSmallEndIslands(
		RegistryEntryLookup<PlacedFeature> featureLookup,
		RegistryEntryLookup<ConfiguredCarver<?>> carverLookup
	) {
		GenerationSettings.LookupBackedBuilder generationBuilder =
			new GenerationSettings.LookupBackedBuilder(featureLookup, carverLookup)
				.feature(GenerationStep.Feature.RAW_GENERATION, EndPlacedFeatures.END_ISLAND_DECORATED);

		return createEndBiome(generationBuilder);
	}
}
