package net.minecraft.world.gen;

import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.*;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.feature.PlacedFeature;

import java.util.Map;
import java.util.Optional;

/**
 * Реестр предустановленных конфигураций мира (пресетов).
 * Каждый пресет определяет набор измерений и их генераторы чанков.
 */
public class WorldPresets {

	public static final RegistryKey<WorldPreset> DEFAULT = of("normal");
	public static final RegistryKey<WorldPreset> FLAT = of("flat");
	public static final RegistryKey<WorldPreset> LARGE_BIOMES = of("large_biomes");
	public static final RegistryKey<WorldPreset> AMPLIFIED = of("amplified");
	public static final RegistryKey<WorldPreset> SINGLE_BIOME_SURFACE = of("single_biome_surface");
	public static final RegistryKey<WorldPreset> DEBUG_ALL_BLOCK_STATES = of("debug_all_block_states");

	public static void bootstrap(Registerable<WorldPreset> presetRegisterable) {
		new Registrar(presetRegisterable).bootstrap();
	}

	private static RegistryKey<WorldPreset> of(String id) {
		return RegistryKey.of(RegistryKeys.WORLD_PRESET, Identifier.ofVanilla(id));
	}

	/**
	 * Определяет ключ пресета по типу генератора чанков Верхнего мира.
	 * Возвращает {@link Optional#empty()} для нестандартных генераторов.
	 */
	public static Optional<RegistryKey<WorldPreset>> getWorldPreset(DimensionOptionsRegistryHolder registry) {
		return registry.getOrEmpty(DimensionOptions.OVERWORLD).flatMap(overworld -> switch (overworld.chunkGenerator()) {
			case FlatChunkGenerator ignored -> Optional.of(FLAT);
			case DebugChunkGenerator ignored -> Optional.of(DEBUG_ALL_BLOCK_STATES);
			case NoiseChunkGenerator ignored -> Optional.of(DEFAULT);
			default -> Optional.empty();
		});
	}

	public static DimensionOptionsRegistryHolder createDemoOptions(RegistryWrapper.WrapperLookup registries) {
		return registries
			.getOrThrow(RegistryKeys.WORLD_PRESET)
			.getOrThrow(DEFAULT)
			.value()
			.createDimensionsRegistryHolder();
	}

	public static DimensionOptions getDefaultOverworldOptions(RegistryWrapper.WrapperLookup registries) {
		return registries
			.getOrThrow(RegistryKeys.WORLD_PRESET)
			.getOrThrow(DEFAULT)
			.value()
			.getOverworld()
			.orElseThrow();
	}

	public static DimensionOptionsRegistryHolder createTestOptions(RegistryWrapper.WrapperLookup registries) {
		return registries
			.getOrThrow(RegistryKeys.WORLD_PRESET)
			.getOrThrow(FLAT)
			.value()
			.createDimensionsRegistryHolder();
	}

	/**
	 * Внутренний регистратор пресетов мира.
	 * Создаёт и регистрирует все ванильные пресеты при бутстрапе реестра.
	 */
	static class Registrar {

		private final Registerable<WorldPreset> presetRegisterable;
		private final RegistryEntryLookup<ChunkGeneratorSettings> chunkGeneratorSettingsLookup;
		private final RegistryEntryLookup<Biome> biomeLookup;
		private final RegistryEntryLookup<PlacedFeature> featureLookup;
		private final RegistryEntryLookup<StructureSet> structureSetLookup;
		private final RegistryEntryLookup<MultiNoiseBiomeSourceParameterList> multiNoisePresetLookup;
		private final RegistryEntry<DimensionType> overworldDimensionType;
		private final DimensionOptions netherDimensionOptions;
		private final DimensionOptions endDimensionOptions;

		Registrar(Registerable<WorldPreset> presetRegisterable) {
			this.presetRegisterable = presetRegisterable;

			RegistryEntryLookup<DimensionType> dimensionTypeLookup =
				presetRegisterable.getRegistryLookup(RegistryKeys.DIMENSION_TYPE);

			chunkGeneratorSettingsLookup = presetRegisterable.getRegistryLookup(RegistryKeys.CHUNK_GENERATOR_SETTINGS);
			biomeLookup = presetRegisterable.getRegistryLookup(RegistryKeys.BIOME);
			featureLookup = presetRegisterable.getRegistryLookup(RegistryKeys.PLACED_FEATURE);
			structureSetLookup = presetRegisterable.getRegistryLookup(RegistryKeys.STRUCTURE_SET);
			multiNoisePresetLookup =
				presetRegisterable.getRegistryLookup(RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
			overworldDimensionType = dimensionTypeLookup.getOrThrow(DimensionTypes.OVERWORLD);

			RegistryEntry<DimensionType> netherType = dimensionTypeLookup.getOrThrow(DimensionTypes.THE_NETHER);
			RegistryEntry<ChunkGeneratorSettings> netherSettings =
				chunkGeneratorSettingsLookup.getOrThrow(ChunkGeneratorSettings.NETHER);
			RegistryEntry.Reference<MultiNoiseBiomeSourceParameterList> netherBiomes =
				multiNoisePresetLookup.getOrThrow(MultiNoiseBiomeSourceParameterLists.NETHER);
			netherDimensionOptions = new DimensionOptions(
				netherType,
				new NoiseChunkGenerator(MultiNoiseBiomeSource.create(netherBiomes), netherSettings)
			);

			RegistryEntry<DimensionType> endType = dimensionTypeLookup.getOrThrow(DimensionTypes.THE_END);
			RegistryEntry<ChunkGeneratorSettings> endSettings =
				chunkGeneratorSettingsLookup.getOrThrow(ChunkGeneratorSettings.END);
			endDimensionOptions = new DimensionOptions(
				endType,
				new NoiseChunkGenerator(TheEndBiomeSource.createVanilla(biomeLookup), endSettings)
			);
		}

		private DimensionOptions createOverworldOptions(ChunkGenerator chunkGenerator) {
			return new DimensionOptions(overworldDimensionType, chunkGenerator);
		}

		private DimensionOptions createOverworldOptions(
			BiomeSource biomeSource,
			RegistryEntry<ChunkGeneratorSettings> settings
		) {
			return createOverworldOptions(new NoiseChunkGenerator(biomeSource, settings));
		}

		private WorldPreset createPreset(DimensionOptions overworldOptions) {
			return new WorldPreset(Map.of(
				DimensionOptions.OVERWORLD, overworldOptions,
				DimensionOptions.NETHER, netherDimensionOptions,
				DimensionOptions.END, endDimensionOptions
			));
		}

		private void register(RegistryKey<WorldPreset> key, DimensionOptions overworldOptions) {
			presetRegisterable.register(key, createPreset(overworldOptions));
		}

		private void bootstrapNoiseBiomeVariants(BiomeSource biomeSource) {
			RegistryEntry<ChunkGeneratorSettings> overworldSettings =
				chunkGeneratorSettingsLookup.getOrThrow(ChunkGeneratorSettings.OVERWORLD);
			register(DEFAULT, createOverworldOptions(biomeSource, overworldSettings));

			RegistryEntry<ChunkGeneratorSettings> largeBiomesSettings =
				chunkGeneratorSettingsLookup.getOrThrow(ChunkGeneratorSettings.LARGE_BIOMES);
			register(LARGE_BIOMES, createOverworldOptions(biomeSource, largeBiomesSettings));

			RegistryEntry<ChunkGeneratorSettings> amplifiedSettings =
				chunkGeneratorSettingsLookup.getOrThrow(ChunkGeneratorSettings.AMPLIFIED);
			register(AMPLIFIED, createOverworldOptions(biomeSource, amplifiedSettings));
		}

		public void bootstrap() {
			RegistryEntry.Reference<MultiNoiseBiomeSourceParameterList> overworldBiomes =
				multiNoisePresetLookup.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD);
			bootstrapNoiseBiomeVariants(MultiNoiseBiomeSource.create(overworldBiomes));

			RegistryEntry<ChunkGeneratorSettings> overworldSettings =
				chunkGeneratorSettingsLookup.getOrThrow(ChunkGeneratorSettings.OVERWORLD);
			RegistryEntry.Reference<Biome> plains = biomeLookup.getOrThrow(BiomeKeys.PLAINS);

			register(SINGLE_BIOME_SURFACE, createOverworldOptions(new FixedBiomeSource(plains), overworldSettings));
			register(
				FLAT,
				createOverworldOptions(new FlatChunkGenerator(
					FlatChunkGeneratorConfig.getDefaultConfig(biomeLookup, structureSetLookup, featureLookup)
				))
			);
			register(DEBUG_ALL_BLOCK_STATES, createOverworldOptions(new DebugChunkGenerator(plains)));
		}
	}
}
