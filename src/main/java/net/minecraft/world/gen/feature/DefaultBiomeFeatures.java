package net.minecraft.world.gen.feature;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.carver.ConfiguredCarvers;

/**
 * {@code DefaultBiomeFeatures}.
 */
public class DefaultBiomeFeatures {

	/**
	 * Добавляет land carvers.
	 *
	 * @param builder builder
	 */
	public static void addLandCarvers(GenerationSettings.LookupBackedBuilder builder) {
		builder.carver(ConfiguredCarvers.CAVE);
		builder.carver(ConfiguredCarvers.CAVE_EXTRA_UNDERGROUND);
		builder.carver(ConfiguredCarvers.CANYON);
		builder.feature(GenerationStep.Feature.LAKES, MiscPlacedFeatures.LAKE_LAVA_UNDERGROUND);
		builder.feature(GenerationStep.Feature.LAKES, MiscPlacedFeatures.LAKE_LAVA_SURFACE);
	}

	/**
	 * Добавляет dungeons.
	 *
	 * @param builder builder
	 */
	public static void addDungeons(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_STRUCTURES, UndergroundPlacedFeatures.MONSTER_ROOM);
		builder.feature(GenerationStep.Feature.UNDERGROUND_STRUCTURES, UndergroundPlacedFeatures.MONSTER_ROOM_DEEP);
	}

	/**
	 * Добавляет mineables.
	 *
	 * @param builder builder
	 */
	public static void addMineables(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_DIRT);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_GRAVEL);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_GRANITE_UPPER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_GRANITE_LOWER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_DIORITE_UPPER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_DIORITE_LOWER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_ANDESITE_UPPER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_ANDESITE_LOWER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_TUFF);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, UndergroundPlacedFeatures.GLOW_LICHEN);
	}

	/**
	 * Добавляет dripstone.
	 *
	 * @param builder builder
	 */
	public static void addDripstone(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.LOCAL_MODIFICATIONS, UndergroundPlacedFeatures.LARGE_DRIPSTONE);
		builder.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, UndergroundPlacedFeatures.DRIPSTONE_CLUSTER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, UndergroundPlacedFeatures.POINTED_DRIPSTONE);
	}

	/**
	 * Добавляет sculk.
	 *
	 * @param builder builder
	 */
	public static void addSculk(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, UndergroundPlacedFeatures.SCULK_VEIN);
		builder.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, UndergroundPlacedFeatures.SCULK_PATCH_DEEP_DARK);
	}

	/**
	 * Добавляет default ores.
	 *
	 * @param builder builder
	 */
	public static void addDefaultOres(GenerationSettings.LookupBackedBuilder builder) {
		addDefaultOres(builder, false);
	}

	/**
	 * Добавляет default ores.
	 *
	 * @param builder builder
	 * @param largeCopperOreBlob large copper ore blob
	 */
	public static void addDefaultOres(GenerationSettings.LookupBackedBuilder builder, boolean largeCopperOreBlob) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_COAL_UPPER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_COAL_LOWER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_IRON_UPPER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_IRON_MIDDLE);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_IRON_SMALL);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_GOLD);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_GOLD_LOWER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_REDSTONE);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_REDSTONE_LOWER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_DIAMOND);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_DIAMOND_MEDIUM);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_DIAMOND_LARGE);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_DIAMOND_BURIED);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_LAPIS);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_LAPIS_BURIED);
		builder.feature(
				GenerationStep.Feature.UNDERGROUND_ORES,
				largeCopperOreBlob ? OrePlacedFeatures.ORE_COPPER_LARGE : OrePlacedFeatures.ORE_COPPER
		);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, UndergroundPlacedFeatures.UNDERWATER_MAGMA);
	}

	/**
	 * Добавляет extra gold ore.
	 *
	 * @param builder builder
	 */
	public static void addExtraGoldOre(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_GOLD_EXTRA);
	}

	/**
	 * Добавляет emerald ore.
	 *
	 * @param builder builder
	 */
	public static void addEmeraldOre(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_EMERALD);
	}

	/**
	 * Добавляет infested stone.
	 *
	 * @param builder builder
	 */
	public static void addInfestedStone(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_INFESTED);
	}

	/**
	 * Добавляет default disks.
	 *
	 * @param builder builder
	 */
	public static void addDefaultDisks(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, MiscPlacedFeatures.DISK_SAND);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, MiscPlacedFeatures.DISK_CLAY);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, MiscPlacedFeatures.DISK_GRAVEL);
	}

	/**
	 * Добавляет clay disk.
	 *
	 * @param builder builder
	 */
	public static void addClayDisk(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, MiscPlacedFeatures.DISK_CLAY);
	}

	/**
	 * Добавляет grass and clay disks.
	 *
	 * @param builder builder
	 */
	public static void addGrassAndClayDisks(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, MiscPlacedFeatures.DISK_GRASS);
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, MiscPlacedFeatures.DISK_CLAY);
	}

	/**
	 * Добавляет mossy rocks.
	 *
	 * @param builder builder
	 */
	public static void addMossyRocks(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.LOCAL_MODIFICATIONS, MiscPlacedFeatures.FOREST_ROCK);
	}

	/**
	 * Добавляет large ferns.
	 *
	 * @param builder builder
	 */
	public static void addLargeFerns(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_LARGE_FERN);
	}

	/**
	 * Добавляет bushes.
	 *
	 * @param builder builder
	 */
	public static void addBushes(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_BUSH);
	}

	/**
	 * Добавляет sweet berry bushes snowy.
	 *
	 * @param builder builder
	 */
	public static void addSweetBerryBushesSnowy(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_BERRY_RARE);
	}

	/**
	 * Добавляет sweet berry bushes.
	 *
	 * @param builder builder
	 */
	public static void addSweetBerryBushes(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_BERRY_COMMON);
	}

	/**
	 * Добавляет bamboo.
	 *
	 * @param builder builder
	 */
	public static void addBamboo(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.BAMBOO_LIGHT);
	}

	/**
	 * Добавляет bamboo jungle trees.
	 *
	 * @param builder builder
	 */
	public static void addBambooJungleTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.BAMBOO);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.BAMBOO_VEGETATION);
	}

	/**
	 * Добавляет taiga trees.
	 *
	 * @param builder builder
	 */
	public static void addTaigaTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_TAIGA);
	}

	/**
	 * Добавляет grove trees.
	 *
	 * @param builder builder
	 */
	public static void addGroveTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_GROVE);
	}

	/**
	 * Добавляет water biome oak trees.
	 *
	 * @param builder builder
	 */
	public static void addWaterBiomeOakTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_WATER);
	}

	/**
	 * Добавляет birch trees.
	 *
	 * @param builder builder
	 */
	public static void addBirchTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_BIRCH);
	}

	/**
	 * Добавляет forest trees.
	 *
	 * @param builder builder
	 */
	public static void addForestTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_BIRCH_AND_OAK);
	}

	/**
	 * Добавляет tall birch trees.
	 *
	 * @param builder builder
	 */
	public static void addTallBirchTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.BIRCH_TALL);
	}

	/**
	 * Добавляет birch forest wildflowers.
	 *
	 * @param builder builder
	 */
	public static void addBirchForestWildflowers(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.WILDFLOWERS_BIRCH_FOREST);
	}

	/**
	 * Добавляет savanna trees.
	 *
	 * @param builder builder
	 */
	public static void addSavannaTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_SAVANNA);
	}

	/**
	 * Добавляет extra savanna trees.
	 *
	 * @param builder builder
	 */
	public static void addExtraSavannaTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_WINDSWEPT_SAVANNA);
	}

	/**
	 * Добавляет lush caves decoration.
	 *
	 * @param builder builder
	 */
	public static void addLushCavesDecoration(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(
				GenerationStep.Feature.VEGETAL_DECORATION,
				UndergroundPlacedFeatures.LUSH_CAVES_CEILING_VEGETATION
		);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, UndergroundPlacedFeatures.CAVE_VINES);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, UndergroundPlacedFeatures.LUSH_CAVES_CLAY);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, UndergroundPlacedFeatures.LUSH_CAVES_VEGETATION);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, UndergroundPlacedFeatures.ROOTED_AZALEA_TREE);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, UndergroundPlacedFeatures.SPORE_BLOSSOM);
		builder.feature(
				GenerationStep.Feature.VEGETAL_DECORATION,
				UndergroundPlacedFeatures.CLASSIC_VINES_CAVE_FEATURE
		);
	}

	/**
	 * Добавляет clay ore.
	 *
	 * @param builder builder
	 */
	public static void addClayOre(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_ORES, OrePlacedFeatures.ORE_CLAY);
	}

	/**
	 * Добавляет windswept hills trees.
	 *
	 * @param builder builder
	 */
	public static void addWindsweptHillsTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_WINDSWEPT_HILLS);
	}

	/**
	 * Добавляет windswept forest trees.
	 *
	 * @param builder builder
	 */
	public static void addWindsweptForestTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_WINDSWEPT_FOREST);
	}

	/**
	 * Добавляет jungle trees.
	 *
	 * @param builder builder
	 */
	public static void addJungleTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_JUNGLE);
	}

	/**
	 * Добавляет sparse jungle trees.
	 *
	 * @param builder builder
	 */
	public static void addSparseJungleTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_SPARSE_JUNGLE);
	}

	/**
	 * Добавляет badlands plateau trees.
	 *
	 * @param builder builder
	 */
	public static void addBadlandsPlateauTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_BADLANDS);
	}

	/**
	 * Добавляет snowy spruce trees.
	 *
	 * @param builder builder
	 */
	public static void addSnowySpruceTrees(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_SNOWY);
	}

	/**
	 * Добавляет jungle grass.
	 *
	 * @param builder builder
	 */
	public static void addJungleGrass(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_JUNGLE);
	}

	/**
	 * Добавляет savanna tall grass.
	 *
	 * @param builder builder
	 */
	public static void addSavannaTallGrass(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_TALL_GRASS);
	}

	/**
	 * Добавляет windswept savanna grass.
	 *
	 * @param builder builder
	 */
	public static void addWindsweptSavannaGrass(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_NORMAL);
	}

	/**
	 * Добавляет savanna grass.
	 *
	 * @param builder builder
	 */
	public static void addSavannaGrass(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_SAVANNA);
	}

	/**
	 * Добавляет badlands grass.
	 *
	 * @param builder builder
	 */
	public static void addBadlandsGrass(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_BADLANDS);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_DRY_GRASS_BADLANDS);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_DEAD_BUSH_BADLANDS);
	}

	/**
	 * Добавляет forest flowers.
	 *
	 * @param builder builder
	 */
	public static void addForestFlowers(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.FOREST_FLOWERS);
	}

	/**
	 * Добавляет forest grass.
	 *
	 * @param builder builder
	 */
	public static void addForestGrass(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_FOREST);
	}

	/**
	 * Добавляет swamp features.
	 *
	 * @param builder builder
	 */
	public static void addSwampFeatures(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_SWAMP);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.FLOWER_SWAMP);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_NORMAL);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_DEAD_BUSH);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_WATERLILY);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.BROWN_MUSHROOM_SWAMP);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.RED_MUSHROOM_SWAMP);
	}

	/**
	 * Добавляет mangrove swamp features.
	 *
	 * @param builder builder
	 */
	public static void addMangroveSwampFeatures(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_MANGROVE);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_NORMAL);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_DEAD_BUSH);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_WATERLILY);
	}

	/**
	 * Добавляет mushroom fields features.
	 *
	 * @param builder builder
	 */
	public static void addMushroomFieldsFeatures(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.MUSHROOM_ISLAND_VEGETATION);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.BROWN_MUSHROOM_TAIGA);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.RED_MUSHROOM_TAIGA);
	}

	/**
	 * Добавляет plains features.
	 *
	 * @param builder builder
	 */
	public static void addPlainsFeatures(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_PLAINS);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.FLOWER_PLAIN);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_PLAIN);
	}

	/**
	 * Добавляет desert dry vegetation.
	 *
	 * @param builder builder
	 */
	public static void addDesertDryVegetation(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_DRY_GRASS_DESERT);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_DEAD_BUSH_2);
	}

	/**
	 * Добавляет giant taiga grass.
	 *
	 * @param builder builder
	 */
	public static void addGiantTaigaGrass(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_TAIGA);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_DEAD_BUSH);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.BROWN_MUSHROOM_OLD_GROWTH);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.RED_MUSHROOM_OLD_GROWTH);
	}

	/**
	 * Добавляет default flowers.
	 *
	 * @param builder builder
	 */
	public static void addDefaultFlowers(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.FLOWER_DEFAULT);
	}

	/**
	 * Добавляет cherry grove features.
	 *
	 * @param builder builder
	 */
	public static void addCherryGroveFeatures(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_PLAIN);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.FLOWER_CHERRY);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_CHERRY);
	}

	/**
	 * Добавляет meadow flowers.
	 *
	 * @param builder builder
	 */
	public static void addMeadowFlowers(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_MEADOW);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.FLOWER_MEADOW);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.TREES_MEADOW);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.WILDFLOWERS_MEADOW);
	}

	/**
	 * Добавляет extra default flowers.
	 *
	 * @param builder builder
	 */
	public static void addExtraDefaultFlowers(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.FLOWER_WARM);
	}

	/**
	 * Добавляет default grass.
	 *
	 * @param builder builder
	 */
	public static void addDefaultGrass(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_BADLANDS);
	}

	/**
	 * Добавляет taiga grass.
	 *
	 * @param builder builder
	 */
	public static void addTaigaGrass(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_GRASS_TAIGA_2);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.BROWN_MUSHROOM_TAIGA);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.RED_MUSHROOM_TAIGA);
	}

	/**
	 * Добавляет plains tall grass.
	 *
	 * @param builder builder
	 */
	public static void addPlainsTallGrass(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_TALL_GRASS_2);
	}

	/**
	 * Добавляет default mushrooms.
	 *
	 * @param builder builder
	 */
	public static void addDefaultMushrooms(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.BROWN_MUSHROOM_NORMAL);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.RED_MUSHROOM_NORMAL);
	}

	/**
	 * Добавляет default vegetation.
	 *
	 * @param builder builder
	 * @param includeNearWater include near water
	 */
	public static void addDefaultVegetation(GenerationSettings.LookupBackedBuilder builder, boolean includeNearWater) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_PUMPKIN);
		if (includeNearWater) {
			addDefaultVegetationNearWater(builder);
		}
	}

	/**
	 * Добавляет default vegetation near water.
	 *
	 * @param builder builder
	 */
	public static void addDefaultVegetationNearWater(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_SUGAR_CANE);
		builder.feature(
				GenerationStep.Feature.VEGETAL_DECORATION,
				VegetationPlacedFeatures.PATCH_FIREFLY_BUSH_NEAR_WATER
		);
	}

	/**
	 * Добавляет leaf litter.
	 *
	 * @param builder builder
	 */
	public static void addLeafLitter(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_LEAF_LITTER);
	}

	/**
	 * Добавляет badlands vegetation.
	 *
	 * @param builder builder
	 */
	public static void addBadlandsVegetation(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_SUGAR_CANE_BADLANDS);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_PUMPKIN);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_CACTUS_DECORATED);
		builder.feature(
				GenerationStep.Feature.VEGETAL_DECORATION,
				VegetationPlacedFeatures.PATCH_FIREFLY_BUSH_NEAR_WATER
		);
	}

	/**
	 * Добавляет melons.
	 *
	 * @param builder builder
	 */
	public static void addMelons(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_MELON);
	}

	/**
	 * Добавляет sparse melons.
	 *
	 * @param builder builder
	 */
	public static void addSparseMelons(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_MELON_SPARSE);
	}

	/**
	 * Добавляет vines.
	 *
	 * @param builder builder
	 */
	public static void addVines(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.VINES);
	}

	/**
	 * Добавляет desert vegetation.
	 *
	 * @param builder builder
	 */
	public static void addDesertVegetation(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_SUGAR_CANE_DESERT);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_PUMPKIN);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_CACTUS_DESERT);
	}

	/**
	 * Добавляет swamp vegetation.
	 *
	 * @param builder builder
	 */
	public static void addSwampVegetation(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_SUGAR_CANE_SWAMP);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_PUMPKIN);
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, VegetationPlacedFeatures.PATCH_FIREFLY_BUSH);
		builder.feature(
				GenerationStep.Feature.VEGETAL_DECORATION,
				VegetationPlacedFeatures.PATCH_FIREFLY_BUSH_NEAR_WATER_SWAMP
		);
	}

	/**
	 * Добавляет mangrove swamp aquatic features.
	 *
	 * @param builder builder
	 */
	public static void addMangroveSwampAquaticFeatures(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, OceanPlacedFeatures.SEAGRASS_SWAMP);
		builder.feature(
				GenerationStep.Feature.VEGETAL_DECORATION,
				VegetationPlacedFeatures.PATCH_FIREFLY_BUSH_NEAR_WATER
		);
	}

	/**
	 * Добавляет desert features.
	 *
	 * @param builder builder
	 */
	public static void addDesertFeatures(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.SURFACE_STRUCTURES, MiscPlacedFeatures.DESERT_WELL);
	}

	/**
	 * Добавляет fossils.
	 *
	 * @param builder builder
	 */
	public static void addFossils(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_STRUCTURES, UndergroundPlacedFeatures.FOSSIL_UPPER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_STRUCTURES, UndergroundPlacedFeatures.FOSSIL_LOWER);
	}

	/**
	 * Добавляет kelp.
	 *
	 * @param builder builder
	 */
	public static void addKelp(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, OceanPlacedFeatures.KELP_COLD);
	}

	/**
	 * Добавляет less kelp.
	 *
	 * @param builder builder
	 */
	public static void addLessKelp(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.VEGETAL_DECORATION, OceanPlacedFeatures.KELP_WARM);
	}

	/**
	 * Добавляет springs.
	 *
	 * @param builder builder
	 */
	public static void addSprings(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.FLUID_SPRINGS, MiscPlacedFeatures.SPRING_WATER);
		builder.feature(GenerationStep.Feature.FLUID_SPRINGS, MiscPlacedFeatures.SPRING_LAVA);
	}

	/**
	 * Добавляет frozen lava spring.
	 *
	 * @param builder builder
	 */
	public static void addFrozenLavaSpring(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.FLUID_SPRINGS, MiscPlacedFeatures.SPRING_LAVA_FROZEN);
	}

	/**
	 * Добавляет icebergs.
	 *
	 * @param builder builder
	 */
	public static void addIcebergs(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.LOCAL_MODIFICATIONS, MiscPlacedFeatures.ICEBERG_PACKED);
		builder.feature(GenerationStep.Feature.LOCAL_MODIFICATIONS, MiscPlacedFeatures.ICEBERG_BLUE);
	}

	/**
	 * Добавляет blue ice.
	 *
	 * @param builder builder
	 */
	public static void addBlueIce(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.SURFACE_STRUCTURES, MiscPlacedFeatures.BLUE_ICE);
	}

	/**
	 * Добавляет frozen top layer.
	 *
	 * @param builder builder
	 */
	public static void addFrozenTopLayer(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.TOP_LAYER_MODIFICATION, MiscPlacedFeatures.FREEZE_TOP_LAYER);
	}

	/**
	 * Добавляет nether mineables.
	 *
	 * @param builder builder
	 */
	public static void addNetherMineables(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_GRAVEL_NETHER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_BLACKSTONE);
		builder.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_GOLD_NETHER);
		builder.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_QUARTZ_NETHER);
		addAncientDebris(builder);
	}

	/**
	 * Добавляет ancient debris.
	 *
	 * @param builder builder
	 */
	public static void addAncientDebris(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_ANCIENT_DEBRIS_LARGE);
		builder.feature(GenerationStep.Feature.UNDERGROUND_DECORATION, OrePlacedFeatures.ORE_DEBRIS_SMALL);
	}

	/**
	 * Добавляет amethyst geodes.
	 *
	 * @param builder builder
	 */
	public static void addAmethystGeodes(GenerationSettings.LookupBackedBuilder builder) {
		builder.feature(GenerationStep.Feature.LOCAL_MODIFICATIONS, UndergroundPlacedFeatures.AMETHYST_GEODE);
	}

	/**
	 * Добавляет farm animals.
	 *
	 * @param builder builder
	 */
	public static void addFarmAnimals(SpawnSettings.Builder builder) {
		builder.spawn(SpawnGroup.CREATURE, 12, new SpawnSettings.SpawnEntry(EntityType.SHEEP, 4, 4));
		builder.spawn(SpawnGroup.CREATURE, 10, new SpawnSettings.SpawnEntry(EntityType.PIG, 4, 4));
		builder.spawn(SpawnGroup.CREATURE, 10, new SpawnSettings.SpawnEntry(EntityType.CHICKEN, 4, 4));
		builder.spawn(SpawnGroup.CREATURE, 8, new SpawnSettings.SpawnEntry(EntityType.COW, 4, 4));
	}

	/**
	 * Добавляет cave mobs.
	 *
	 * @param builder builder
	 */
	public static void addCaveMobs(SpawnSettings.Builder builder) {
		builder.spawn(SpawnGroup.AMBIENT, 10, new SpawnSettings.SpawnEntry(EntityType.BAT, 8, 8));
		builder.spawn(
				SpawnGroup.UNDERGROUND_WATER_CREATURE,
				10,
				new SpawnSettings.SpawnEntry(EntityType.GLOW_SQUID, 4, 6)
		);
	}

	/**
	 * Добавляет cave and monsters.
	 *
	 * @param builder builder
	 */
	public static void addCaveAndMonsters(SpawnSettings.Builder builder) {
		addCaveAndMonsters(builder, 100);
	}

	/**
	 * Добавляет cave and monsters.
	 *
	 * @param builder builder
	 * @param skeletonWeight skeleton weight
	 */
	public static void addCaveAndMonsters(SpawnSettings.Builder builder, int skeletonWeight) {
		addCaveMobs(builder);
		addMonsters(builder, 95, 5, 0, skeletonWeight, false);
	}

	/**
	 * Добавляет cave and monsters and zombie horse.
	 *
	 * @param builder builder
	 */
	public static void addCaveAndMonstersAndZombieHorse(SpawnSettings.Builder builder) {
		addCaveMobs(builder);
		addMonsters(builder, 90, 5, 5, 100, false);
	}

	/**
	 * Добавляет swamp mobs.
	 *
	 * @param builder builder
	 * @param skeletonWeight skeleton weight
	 */
	public static void addSwampMobs(SpawnSettings.Builder builder, int skeletonWeight) {
		addCaveAndMonsters(builder, skeletonWeight);
		builder.spawn(SpawnGroup.MONSTER, 1, new SpawnSettings.SpawnEntry(EntityType.SLIME, 1, 1));
		builder.spawn(SpawnGroup.MONSTER, 30, new SpawnSettings.SpawnEntry(EntityType.BOGGED, 4, 4));
		builder.spawn(SpawnGroup.CREATURE, 10, new SpawnSettings.SpawnEntry(EntityType.FROG, 2, 5));
	}

	public static void addOceanMobs(
			SpawnSettings.Builder builder,
			int squidWeight,
			int squidMaxGroupSize,
			int codWeight
	) {
		builder.spawn(
				SpawnGroup.WATER_CREATURE,
				squidWeight,
				new SpawnSettings.SpawnEntry(EntityType.SQUID, 1, squidMaxGroupSize)
		);
		builder.spawn(SpawnGroup.WATER_AMBIENT, codWeight, new SpawnSettings.SpawnEntry(EntityType.COD, 3, 6));
		addCaveAndMonsters(builder);
		builder.spawn(SpawnGroup.MONSTER, 5, new SpawnSettings.SpawnEntry(EntityType.DROWNED, 1, 1));
	}

	/**
	 * Добавляет warm ocean mobs.
	 *
	 * @param builder builder
	 * @param squidWeight squid weight
	 * @param squidMinGroupSize squid min group size
	 */
	public static void addWarmOceanMobs(SpawnSettings.Builder builder, int squidWeight, int squidMinGroupSize) {
		builder.spawn(
				SpawnGroup.WATER_CREATURE,
				squidWeight,
				new SpawnSettings.SpawnEntry(EntityType.SQUID, squidMinGroupSize, 4)
		);
		builder.spawn(SpawnGroup.WATER_AMBIENT, 25, new SpawnSettings.SpawnEntry(EntityType.TROPICAL_FISH, 8, 8));
		builder.spawn(SpawnGroup.WATER_CREATURE, 2, new SpawnSettings.SpawnEntry(EntityType.DOLPHIN, 1, 2));
		builder.spawn(SpawnGroup.MONSTER, 5, new SpawnSettings.SpawnEntry(EntityType.DROWNED, 1, 1));
		addCaveAndMonsters(builder);
	}

	/**
	 * Добавляет plains mobs.
	 *
	 * @param builder builder
	 */
	public static void addPlainsMobs(SpawnSettings.Builder builder) {
		addFarmAnimals(builder);
		builder.spawn(SpawnGroup.CREATURE, 5, new SpawnSettings.SpawnEntry(EntityType.HORSE, 2, 6));
		builder.spawn(SpawnGroup.CREATURE, 1, new SpawnSettings.SpawnEntry(EntityType.DONKEY, 1, 3));
		addCaveAndMonstersAndZombieHorse(builder);
	}

	/**
	 * Добавляет snowy mobs.
	 *
	 * @param builder builder
	 * @param zombieHorse zombie horse
	 */
	public static void addSnowyMobs(SpawnSettings.Builder builder, boolean zombieHorse) {
		builder.spawn(SpawnGroup.CREATURE, 10, new SpawnSettings.SpawnEntry(EntityType.RABBIT, 2, 3));
		builder.spawn(SpawnGroup.CREATURE, 1, new SpawnSettings.SpawnEntry(EntityType.POLAR_BEAR, 1, 2));
		addCaveMobs(builder);
		addMonsters(builder, zombieHorse ? 90 : 95, 5, zombieHorse ? 5 : 0, 20, false);
		builder.spawn(SpawnGroup.MONSTER, 80, new SpawnSettings.SpawnEntry(EntityType.STRAY, 4, 4));
	}

	/**
	 * Добавляет desert mobs.
	 *
	 * @param builder builder
	 */
	public static void addDesertMobs(SpawnSettings.Builder builder) {
		builder.spawn(SpawnGroup.CREATURE, 12, new SpawnSettings.SpawnEntry(EntityType.RABBIT, 2, 3));
		builder.spawn(SpawnGroup.CREATURE, 1, new SpawnSettings.SpawnEntry(EntityType.CAMEL, 1, 1));
		addCaveMobs(builder);
		addMonsters(builder, 19, 1, 0, 50, false);
		builder.spawn(SpawnGroup.MONSTER, 80, new SpawnSettings.SpawnEntry(EntityType.HUSK, 4, 4));
		builder.spawn(SpawnGroup.MONSTER, 50, new SpawnSettings.SpawnEntry(EntityType.PARCHED, 4, 4));
	}

	/**
	 * Добавляет dripstone cave mobs.
	 *
	 * @param builder builder
	 */
	public static void addDripstoneCaveMobs(SpawnSettings.Builder builder) {
		addCaveMobs(builder);
		int i = 95;
		addMonsters(builder, 95, 5, 0, 100, false);
		builder.spawn(SpawnGroup.MONSTER, 95, new SpawnSettings.SpawnEntry(EntityType.DROWNED, 4, 4));
	}

	public static void addMonsters(
			SpawnSettings.Builder builder,
			int zombieWeight,
			int zombieVillagerWeight,
			int zombieHorseWeight,
			int skeletonWeight,
			boolean drowned
	) {
		builder.spawn(SpawnGroup.MONSTER, 100, new SpawnSettings.SpawnEntry(EntityType.SPIDER, 4, 4));
		builder.spawn(
				SpawnGroup.MONSTER,
				zombieWeight,
				new SpawnSettings.SpawnEntry(drowned ? EntityType.DROWNED : EntityType.ZOMBIE, 4, 4)
		);
		builder.spawn(
				SpawnGroup.MONSTER,
				zombieVillagerWeight,
				new SpawnSettings.SpawnEntry(EntityType.ZOMBIE_VILLAGER, 1, 1)
		);
		if (zombieHorseWeight > 0) {
			builder.spawn(
					SpawnGroup.MONSTER,
					zombieHorseWeight,
					new SpawnSettings.SpawnEntry(EntityType.ZOMBIE_HORSE, 1, 1)
			);
		}

		builder.spawn(SpawnGroup.MONSTER, skeletonWeight, new SpawnSettings.SpawnEntry(EntityType.SKELETON, 4, 4));
		builder.spawn(SpawnGroup.MONSTER, 100, new SpawnSettings.SpawnEntry(EntityType.CREEPER, 4, 4));
		builder.spawn(SpawnGroup.MONSTER, 100, new SpawnSettings.SpawnEntry(EntityType.SLIME, 4, 4));
		builder.spawn(SpawnGroup.MONSTER, 10, new SpawnSettings.SpawnEntry(EntityType.ENDERMAN, 1, 4));
		builder.spawn(SpawnGroup.MONSTER, 5, new SpawnSettings.SpawnEntry(EntityType.WITCH, 1, 1));
	}

	/**
	 * Добавляет mushroom mobs.
	 *
	 * @param builder builder
	 */
	public static void addMushroomMobs(SpawnSettings.Builder builder) {
		builder.spawn(SpawnGroup.CREATURE, 8, new SpawnSettings.SpawnEntry(EntityType.MOOSHROOM, 4, 8));
		addCaveMobs(builder);
	}

	/**
	 * Добавляет jungle mobs.
	 *
	 * @param builder builder
	 */
	public static void addJungleMobs(SpawnSettings.Builder builder) {
		addFarmAnimals(builder);
		builder.spawn(SpawnGroup.CREATURE, 10, new SpawnSettings.SpawnEntry(EntityType.CHICKEN, 4, 4));
		addCaveAndMonsters(builder);
	}

	/**
	 * Добавляет end mobs.
	 *
	 * @param builder builder
	 */
	public static void addEndMobs(SpawnSettings.Builder builder) {
		builder.spawn(SpawnGroup.MONSTER, 10, new SpawnSettings.SpawnEntry(EntityType.ENDERMAN, 4, 4));
	}
}
