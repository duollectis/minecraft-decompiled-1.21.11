package net.minecraft.world.biome;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Точка входа для регистрации всех ванильных биомов в реестре.
 * Делегирует создание биомов специализированным фабрикам:
 * {@link OverworldBiomeCreator}, {@link TheNetherBiomeCreator}, {@link TheEndBiomeCreator}.
 */
public abstract class BuiltinBiomes {

	public static void bootstrap(Registerable<Biome> biomeRegisterable) {
		RegistryEntryLookup<PlacedFeature> featureLookup =
			biomeRegisterable.getRegistryLookup(RegistryKeys.PLACED_FEATURE);
		RegistryEntryLookup<ConfiguredCarver<?>> carverLookup =
			biomeRegisterable.getRegistryLookup(RegistryKeys.CONFIGURED_CARVER);

		biomeRegisterable.register(
			BiomeKeys.THE_VOID,
			OverworldBiomeCreator.createTheVoid(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.PLAINS,
			OverworldBiomeCreator.createPlains(featureLookup, carverLookup, false, false, false)
		);
		biomeRegisterable.register(
			BiomeKeys.SUNFLOWER_PLAINS,
			OverworldBiomeCreator.createPlains(featureLookup, carverLookup, true, false, false)
		);
		biomeRegisterable.register(
			BiomeKeys.SNOWY_PLAINS,
			OverworldBiomeCreator.createPlains(featureLookup, carverLookup, false, true, false)
		);
		biomeRegisterable.register(
			BiomeKeys.ICE_SPIKES,
			OverworldBiomeCreator.createPlains(featureLookup, carverLookup, false, true, true)
		);
		biomeRegisterable.register(
			BiomeKeys.DESERT,
			OverworldBiomeCreator.createDesert(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.SWAMP,
			OverworldBiomeCreator.createSwamp(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.MANGROVE_SWAMP,
			OverworldBiomeCreator.createMangroveSwamp(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.FOREST,
			OverworldBiomeCreator.createNormalForest(featureLookup, carverLookup, false, false, false)
		);
		biomeRegisterable.register(
			BiomeKeys.FLOWER_FOREST,
			OverworldBiomeCreator.createNormalForest(featureLookup, carverLookup, false, false, true)
		);
		biomeRegisterable.register(
			BiomeKeys.BIRCH_FOREST,
			OverworldBiomeCreator.createNormalForest(featureLookup, carverLookup, true, false, false)
		);
		biomeRegisterable.register(
			BiomeKeys.DARK_FOREST,
			OverworldBiomeCreator.createDenseForest(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.PALE_GARDEN,
			OverworldBiomeCreator.createDenseForest(featureLookup, carverLookup, true)
		);
		biomeRegisterable.register(
			BiomeKeys.OLD_GROWTH_BIRCH_FOREST,
			OverworldBiomeCreator.createNormalForest(featureLookup, carverLookup, true, true, false)
		);
		biomeRegisterable.register(
			BiomeKeys.OLD_GROWTH_PINE_TAIGA,
			OverworldBiomeCreator.createOldGrowthTaiga(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA,
			OverworldBiomeCreator.createOldGrowthTaiga(featureLookup, carverLookup, true)
		);
		biomeRegisterable.register(
			BiomeKeys.TAIGA,
			OverworldBiomeCreator.createTaiga(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.SNOWY_TAIGA,
			OverworldBiomeCreator.createTaiga(featureLookup, carverLookup, true)
		);
		biomeRegisterable.register(
			BiomeKeys.SAVANNA,
			OverworldBiomeCreator.createSavanna(featureLookup, carverLookup, false, false)
		);
		biomeRegisterable.register(
			BiomeKeys.SAVANNA_PLATEAU,
			OverworldBiomeCreator.createSavanna(featureLookup, carverLookup, false, true)
		);
		biomeRegisterable.register(
			BiomeKeys.WINDSWEPT_HILLS,
			OverworldBiomeCreator.createWindsweptHills(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.WINDSWEPT_GRAVELLY_HILLS,
			OverworldBiomeCreator.createWindsweptHills(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.WINDSWEPT_FOREST,
			OverworldBiomeCreator.createWindsweptHills(featureLookup, carverLookup, true)
		);
		biomeRegisterable.register(
			BiomeKeys.WINDSWEPT_SAVANNA,
			OverworldBiomeCreator.createSavanna(featureLookup, carverLookup, true, false)
		);
		biomeRegisterable.register(
			BiomeKeys.JUNGLE,
			OverworldBiomeCreator.createJungle(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.SPARSE_JUNGLE,
			OverworldBiomeCreator.createSparseJungle(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.BAMBOO_JUNGLE,
			OverworldBiomeCreator.createNormalBambooJungle(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.BADLANDS,
			OverworldBiomeCreator.createBadlands(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.ERODED_BADLANDS,
			OverworldBiomeCreator.createBadlands(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.WOODED_BADLANDS,
			OverworldBiomeCreator.createBadlands(featureLookup, carverLookup, true)
		);
		biomeRegisterable.register(
			BiomeKeys.MEADOW,
			OverworldBiomeCreator.createMeadow(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.CHERRY_GROVE,
			OverworldBiomeCreator.createMeadow(featureLookup, carverLookup, true)
		);
		biomeRegisterable.register(
			BiomeKeys.GROVE,
			OverworldBiomeCreator.createGrove(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.SNOWY_SLOPES,
			OverworldBiomeCreator.createSnowySlopes(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.FROZEN_PEAKS,
			OverworldBiomeCreator.createFrozenPeaks(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.JAGGED_PEAKS,
			OverworldBiomeCreator.createJaggedPeaks(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.STONY_PEAKS,
			OverworldBiomeCreator.createStonyPeaks(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.RIVER,
			OverworldBiomeCreator.createRiver(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.FROZEN_RIVER,
			OverworldBiomeCreator.createRiver(featureLookup, carverLookup, true)
		);
		biomeRegisterable.register(
			BiomeKeys.BEACH,
			OverworldBiomeCreator.createBeach(featureLookup, carverLookup, false, false)
		);
		biomeRegisterable.register(
			BiomeKeys.SNOWY_BEACH,
			OverworldBiomeCreator.createBeach(featureLookup, carverLookup, true, false)
		);
		biomeRegisterable.register(
			BiomeKeys.STONY_SHORE,
			OverworldBiomeCreator.createBeach(featureLookup, carverLookup, false, true)
		);
		biomeRegisterable.register(
			BiomeKeys.WARM_OCEAN,
			OverworldBiomeCreator.createWarmOcean(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.LUKEWARM_OCEAN,
			OverworldBiomeCreator.createLukewarmOcean(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.DEEP_LUKEWARM_OCEAN,
			OverworldBiomeCreator.createLukewarmOcean(featureLookup, carverLookup, true)
		);
		biomeRegisterable.register(
			BiomeKeys.OCEAN,
			OverworldBiomeCreator.createNormalOcean(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.DEEP_OCEAN,
			OverworldBiomeCreator.createNormalOcean(featureLookup, carverLookup, true)
		);
		biomeRegisterable.register(
			BiomeKeys.COLD_OCEAN,
			OverworldBiomeCreator.createColdOcean(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.DEEP_COLD_OCEAN,
			OverworldBiomeCreator.createColdOcean(featureLookup, carverLookup, true)
		);
		biomeRegisterable.register(
			BiomeKeys.FROZEN_OCEAN,
			OverworldBiomeCreator.createFrozenOcean(featureLookup, carverLookup, false)
		);
		biomeRegisterable.register(
			BiomeKeys.DEEP_FROZEN_OCEAN,
			OverworldBiomeCreator.createFrozenOcean(featureLookup, carverLookup, true)
		);
		biomeRegisterable.register(
			BiomeKeys.MUSHROOM_FIELDS,
			OverworldBiomeCreator.createMushroomFields(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.DRIPSTONE_CAVES,
			OverworldBiomeCreator.createDripstoneCaves(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.LUSH_CAVES,
			OverworldBiomeCreator.createLushCaves(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.DEEP_DARK,
			OverworldBiomeCreator.createDeepDark(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.NETHER_WASTES,
			TheNetherBiomeCreator.createNetherWastes(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.WARPED_FOREST,
			TheNetherBiomeCreator.createWarpedForest(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.CRIMSON_FOREST,
			TheNetherBiomeCreator.createCrimsonForest(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.SOUL_SAND_VALLEY,
			TheNetherBiomeCreator.createSoulSandValley(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.BASALT_DELTAS,
			TheNetherBiomeCreator.createBasaltDeltas(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.THE_END,
			TheEndBiomeCreator.createTheEnd(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.END_HIGHLANDS,
			TheEndBiomeCreator.createEndHighlands(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.END_MIDLANDS,
			TheEndBiomeCreator.createEndMidlands(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.SMALL_END_ISLANDS,
			TheEndBiomeCreator.createSmallEndIslands(featureLookup, carverLookup)
		);
		biomeRegisterable.register(
			BiomeKeys.END_BARRENS,
			TheEndBiomeCreator.createEndBarrens(featureLookup, carverLookup)
		);
	}
}
