package net.minecraft.world.gen.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.biome.source.util.VanillaBiomeParameters;
import net.minecraft.world.gen.densityfunction.DensityFunctions;
import net.minecraft.world.gen.noise.NoiseRouter;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.VanillaSurfaceRules;

import java.util.List;

/**
 * Полная конфигурация генератора чанков на основе шума.
 * Содержит форму генерации, блоки по умолчанию, роутер шума,
 * правила поверхности и флаги включения аквиферов/рудных жил.
 */
public record ChunkGeneratorSettings(
	GenerationShapeConfig generationShapeConfig,
	BlockState defaultBlock,
	BlockState defaultFluid,
	NoiseRouter noiseRouter,
	MaterialRules.MaterialRule surfaceRule,
	List<MultiNoiseUtil.NoiseHypercube> spawnTarget,
	int seaLevel,
	@Deprecated boolean mobGenerationDisabled,
	boolean aquifers,
	boolean oreVeins,
	boolean usesLegacyRandom
) {

	public static final Codec<ChunkGeneratorSettings> CODEC = RecordCodecBuilder.create(
		instance -> instance
			.group(
				GenerationShapeConfig.CODEC
					.fieldOf("noise")
					.forGetter(ChunkGeneratorSettings::generationShapeConfig),
				BlockState.CODEC
					.fieldOf("default_block")
					.forGetter(ChunkGeneratorSettings::defaultBlock),
				BlockState.CODEC
					.fieldOf("default_fluid")
					.forGetter(ChunkGeneratorSettings::defaultFluid),
				NoiseRouter.CODEC
					.fieldOf("noise_router")
					.forGetter(ChunkGeneratorSettings::noiseRouter),
				MaterialRules.MaterialRule.CODEC
					.fieldOf("surface_rule")
					.forGetter(ChunkGeneratorSettings::surfaceRule),
				MultiNoiseUtil.NoiseHypercube.CODEC
					.listOf()
					.fieldOf("spawn_target")
					.forGetter(ChunkGeneratorSettings::spawnTarget),
				Codec.INT
					.fieldOf("sea_level")
					.forGetter(ChunkGeneratorSettings::seaLevel),
				Codec.BOOL
					.fieldOf("disable_mob_generation")
					.forGetter(ChunkGeneratorSettings::mobGenerationDisabled),
				Codec.BOOL
					.fieldOf("aquifers_enabled")
					.forGetter(ChunkGeneratorSettings::hasAquifers),
				Codec.BOOL
					.fieldOf("ore_veins_enabled")
					.forGetter(ChunkGeneratorSettings::oreVeins),
				Codec.BOOL
					.fieldOf("legacy_random_source")
					.forGetter(ChunkGeneratorSettings::usesLegacyRandom)
			)
			.apply(instance, ChunkGeneratorSettings::new)
	);

	public static final Codec<RegistryEntry<ChunkGeneratorSettings>> REGISTRY_CODEC =
		RegistryElementCodec.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, CODEC);

	public static final RegistryKey<ChunkGeneratorSettings> OVERWORLD =
		RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.ofVanilla("overworld"));
	public static final RegistryKey<ChunkGeneratorSettings> LARGE_BIOMES =
		RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.ofVanilla("large_biomes"));
	public static final RegistryKey<ChunkGeneratorSettings> AMPLIFIED =
		RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.ofVanilla("amplified"));
	public static final RegistryKey<ChunkGeneratorSettings> NETHER =
		RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.ofVanilla("nether"));
	public static final RegistryKey<ChunkGeneratorSettings> END =
		RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.ofVanilla("end"));
	public static final RegistryKey<ChunkGeneratorSettings> CAVES =
		RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.ofVanilla("caves"));
	public static final RegistryKey<ChunkGeneratorSettings> FLOATING_ISLANDS =
		RegistryKey.of(RegistryKeys.CHUNK_GENERATOR_SETTINGS, Identifier.ofVanilla("floating_islands"));

	public boolean hasAquifers() {
		return aquifers && !SharedConstants.DISABLE_AQUIFERS;
	}

	public boolean oreVeins() {
		return oreVeins && !SharedConstants.DISABLE_ORE_VEINS;
	}

	public ChunkRandom.RandomProvider getRandomProvider() {
		return usesLegacyRandom ? ChunkRandom.RandomProvider.LEGACY : ChunkRandom.RandomProvider.XOROSHIRO;
	}

	/**
	 * Регистрирует все ванильные пресеты настроек генератора чанков.
	 */
	public static void bootstrap(Registerable<ChunkGeneratorSettings> registerable) {
		registerable.register(OVERWORLD, createSurfaceSettings(registerable, false, false));
		registerable.register(LARGE_BIOMES, createSurfaceSettings(registerable, false, true));
		registerable.register(AMPLIFIED, createSurfaceSettings(registerable, true, false));
		registerable.register(NETHER, createNetherSettings(registerable));
		registerable.register(END, createEndSettings(registerable));
		registerable.register(CAVES, createCavesSettings(registerable));
		registerable.register(FLOATING_ISLANDS, createFloatingIslandsSettings(registerable));
	}

	/**
	 * Создаёт заглушку настроек для отсутствующих записей реестра.
	 * Используется как fallback при загрузке повреждённых данных.
	 */
	public static ChunkGeneratorSettings createMissingSettings() {
		return new ChunkGeneratorSettings(
			GenerationShapeConfig.SURFACE,
			Blocks.STONE.getDefaultState(),
			Blocks.AIR.getDefaultState(),
			DensityFunctions.createMissingNoiseRouter(),
			VanillaSurfaceRules.getAirRule(),
			List.of(),
			63,
			true,
			false,
			false,
			false
		);
	}

	private static ChunkGeneratorSettings createEndSettings(Registerable<?> registerable) {
		return new ChunkGeneratorSettings(
			GenerationShapeConfig.END,
			Blocks.END_STONE.getDefaultState(),
			Blocks.AIR.getDefaultState(),
			DensityFunctions.createEndNoiseRouter(registerable.getRegistryLookup(RegistryKeys.DENSITY_FUNCTION)),
			VanillaSurfaceRules.getEndStoneRule(),
			List.of(),
			0,
			true,
			false,
			false,
			true
		);
	}

	private static ChunkGeneratorSettings createNetherSettings(Registerable<?> registerable) {
		return new ChunkGeneratorSettings(
			GenerationShapeConfig.NETHER,
			Blocks.NETHERRACK.getDefaultState(),
			Blocks.LAVA.getDefaultState(),
			DensityFunctions.createNetherNoiseRouter(
				registerable.getRegistryLookup(RegistryKeys.DENSITY_FUNCTION),
				registerable.getRegistryLookup(RegistryKeys.NOISE_PARAMETERS)
			),
			VanillaSurfaceRules.createNetherSurfaceRule(),
			List.of(),
			32,
			false,
			false,
			false,
			true
		);
	}

	private static ChunkGeneratorSettings createSurfaceSettings(
		Registerable<?> registerable,
		boolean amplified,
		boolean largeBiomes
	) {
		return new ChunkGeneratorSettings(
			GenerationShapeConfig.SURFACE,
			Blocks.STONE.getDefaultState(),
			Blocks.WATER.getDefaultState(),
			DensityFunctions.createSurfaceNoiseRouter(
				registerable.getRegistryLookup(RegistryKeys.DENSITY_FUNCTION),
				registerable.getRegistryLookup(RegistryKeys.NOISE_PARAMETERS),
				largeBiomes,
				amplified
			),
			VanillaSurfaceRules.createOverworldSurfaceRule(),
			new VanillaBiomeParameters().getSpawnSuitabilityNoises(),
			63,
			false,
			true,
			true,
			false
		);
	}

	private static ChunkGeneratorSettings createCavesSettings(Registerable<?> registerable) {
		return new ChunkGeneratorSettings(
			GenerationShapeConfig.CAVES,
			Blocks.STONE.getDefaultState(),
			Blocks.WATER.getDefaultState(),
			DensityFunctions.createCavesNoiseRouter(
				registerable.getRegistryLookup(RegistryKeys.DENSITY_FUNCTION),
				registerable.getRegistryLookup(RegistryKeys.NOISE_PARAMETERS)
			),
			VanillaSurfaceRules.createDefaultRule(false, true, true),
			List.of(),
			32,
			false,
			false,
			false,
			true
		);
	}

	private static ChunkGeneratorSettings createFloatingIslandsSettings(Registerable<?> registerable) {
		return new ChunkGeneratorSettings(
			GenerationShapeConfig.FLOATING_ISLANDS,
			Blocks.STONE.getDefaultState(),
			Blocks.WATER.getDefaultState(),
			DensityFunctions.createFloatingIslandsNoiseRouter(
				registerable.getRegistryLookup(RegistryKeys.DENSITY_FUNCTION),
				registerable.getRegistryLookup(RegistryKeys.NOISE_PARAMETERS)
			),
			VanillaSurfaceRules.createDefaultRule(false, false, false),
			List.of(),
			-64,
			false,
			false,
			false,
			true
		);
	}
}
