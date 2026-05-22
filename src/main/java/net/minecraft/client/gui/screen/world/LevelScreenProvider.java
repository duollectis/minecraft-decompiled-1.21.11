package net.minecraft.client.gui.screen.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureSet;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.feature.PlacedFeature;

import java.util.Map;
import java.util.Optional;

/**
 * Провайдер экрана настройки генератора мира для конкретного пресета.
 * Маппинг пресетов на соответствующие экраны настройки (плоский мир, один биом и т.д.).
 */
@Environment(EnvType.CLIENT)
public interface LevelScreenProvider {

	Map<Optional<RegistryKey<WorldPreset>>, LevelScreenProvider> WORLD_PRESET_TO_SCREEN_PROVIDER = Map.of(
			Optional.of(WorldPresets.FLAT),
			(parent, generatorOptionsHolder) -> {
				ChunkGenerator chunkGenerator = generatorOptionsHolder.selectedDimensions().getChunkGenerator();
				DynamicRegistryManager registryManager = generatorOptionsHolder.getCombinedRegistryManager();
				RegistryEntryLookup<Biome> biomeLookup = registryManager.getOrThrow(RegistryKeys.BIOME);
				RegistryEntryLookup<StructureSet> structureLookup = registryManager.getOrThrow(RegistryKeys.STRUCTURE_SET);
				RegistryEntryLookup<PlacedFeature> featureLookup = registryManager.getOrThrow(RegistryKeys.PLACED_FEATURE);

				return new CustomizeFlatLevelScreen(
						parent,
						config -> parent.getWorldCreator().applyModifier(createModifier(config)),
						chunkGenerator instanceof FlatChunkGenerator flatGenerator
								? flatGenerator.getConfig()
								: FlatChunkGeneratorConfig.getDefaultConfig(biomeLookup, structureLookup, featureLookup)
				);
			},
			Optional.of(WorldPresets.SINGLE_BIOME_SURFACE),
			(parent, generatorOptionsHolder) -> new CustomizeBuffetLevelScreen(
					parent,
					generatorOptionsHolder,
					biomeEntry -> parent.getWorldCreator().applyModifier(createModifier(biomeEntry))
			)
	);

	Screen createEditScreen(CreateWorldScreen parent, GeneratorOptionsHolder generatorOptionsHolder);

	static GeneratorOptionsHolder.RegistryAwareModifier createModifier(FlatChunkGeneratorConfig config) {
		return (dynamicRegistryManager, dimensionsRegistryHolder) -> {
			ChunkGenerator chunkGenerator = new FlatChunkGenerator(config);
			return dimensionsRegistryHolder.with(dynamicRegistryManager, chunkGenerator);
		};
	}

	private static GeneratorOptionsHolder.RegistryAwareModifier createModifier(RegistryEntry<Biome> biomeEntry) {
		return (dynamicRegistryManager, dimensionsRegistryHolder) -> {
			Registry<ChunkGeneratorSettings> settingsRegistry = dynamicRegistryManager.getOrThrow(RegistryKeys.CHUNK_GENERATOR_SETTINGS);
			RegistryEntry<ChunkGeneratorSettings> overworldSettings = settingsRegistry.getOrThrow(ChunkGeneratorSettings.OVERWORLD);
			BiomeSource biomeSource = new FixedBiomeSource(biomeEntry);
			ChunkGenerator chunkGenerator = new NoiseChunkGenerator(biomeSource, overworldSettings);
			return dimensionsRegistryHolder.with(dynamicRegistryManager, chunkGenerator);
		};
	}
}
