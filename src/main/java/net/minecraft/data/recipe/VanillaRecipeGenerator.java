package net.minecraft.data.recipe;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.advancement.criterion.TickCriterion;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SuspiciousStewIngredient;
import net.minecraft.data.DataOutput;
import net.minecraft.item.*;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.item.equipment.trim.ArmorTrimPatterns;
import net.minecraft.predicate.NumberRange;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Генератор всех ванильных рецептов крафта, плавки, копчения, взрывной печи и кузнечного стола.
 * <p>
 * Наследует базовую логику из {@link RecipeGenerator} и переопределяет метод {@code generate()},
 * в котором регистрируются тысячи рецептов для всех ванильных предметов и блоков.
 * Используется системой датагенерации Minecraft для автоматической генерации JSON-файлов рецептов.
 */
public class VanillaRecipeGenerator extends RecipeGenerator {

	private static final ImmutableList<ItemConvertible>
			COAL_ORES =
			ImmutableList.of(Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE);
	private static final ImmutableList<ItemConvertible>
			IRON_ORES =
			ImmutableList.of(Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.RAW_IRON);
	private static final ImmutableList<ItemConvertible>
			COPPER_ORES =
			ImmutableList.of(Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER);
	private static final ImmutableList<ItemConvertible> GOLD_ORES = ImmutableList.of(
			Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.NETHER_GOLD_ORE, Items.RAW_GOLD
	);
	private static final ImmutableList<ItemConvertible>
			DIAMOND_ORES =
			ImmutableList.of(Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE);
	private static final ImmutableList<ItemConvertible>
			LAPIS_ORES =
			ImmutableList.of(Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE);
	private static final ImmutableList<ItemConvertible>
			REDSTONE_ORES =
			ImmutableList.of(Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE);
	private static final ImmutableList<ItemConvertible>
			EMERALD_ORES =
			ImmutableList.of(Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE);

	VanillaRecipeGenerator(RegistryWrapper.WrapperLookup wrapperLookup, RecipeExporter recipeExporter) {
		super(wrapperLookup, recipeExporter);
	}

	@Override
	public void generate() {
		exporter.addRootAdvancement();
		generateFamilies(FeatureSet.of(FeatureFlags.VANILLA));
		offerPlanksRecipe2(Blocks.ACACIA_PLANKS, ItemTags.ACACIA_LOGS, 4);
		offerPlanksRecipe(Blocks.BIRCH_PLANKS, ItemTags.BIRCH_LOGS, 4);
		offerPlanksRecipe(Blocks.CRIMSON_PLANKS, ItemTags.CRIMSON_STEMS, 4);
		offerPlanksRecipe2(Blocks.DARK_OAK_PLANKS, ItemTags.DARK_OAK_LOGS, 4);
		offerPlanksRecipe2(Blocks.PALE_OAK_PLANKS, ItemTags.PALE_OAK_LOGS, 4);
		offerPlanksRecipe(Blocks.JUNGLE_PLANKS, ItemTags.JUNGLE_LOGS, 4);
		offerPlanksRecipe(Blocks.OAK_PLANKS, ItemTags.OAK_LOGS, 4);
		offerPlanksRecipe(Blocks.SPRUCE_PLANKS, ItemTags.SPRUCE_LOGS, 4);
		offerPlanksRecipe(Blocks.WARPED_PLANKS, ItemTags.WARPED_STEMS, 4);
		offerPlanksRecipe(Blocks.MANGROVE_PLANKS, ItemTags.MANGROVE_LOGS, 4);
		offerBarkBlockRecipe(Blocks.ACACIA_WOOD, Blocks.ACACIA_LOG);
		offerBarkBlockRecipe(Blocks.BIRCH_WOOD, Blocks.BIRCH_LOG);
		offerBarkBlockRecipe(Blocks.DARK_OAK_WOOD, Blocks.DARK_OAK_LOG);
		offerBarkBlockRecipe(Blocks.PALE_OAK_WOOD, Blocks.PALE_OAK_LOG);
		offerBarkBlockRecipe(Blocks.JUNGLE_WOOD, Blocks.JUNGLE_LOG);
		offerBarkBlockRecipe(Blocks.OAK_WOOD, Blocks.OAK_LOG);
		offerBarkBlockRecipe(Blocks.SPRUCE_WOOD, Blocks.SPRUCE_LOG);
		offerBarkBlockRecipe(Blocks.CRIMSON_HYPHAE, Blocks.CRIMSON_STEM);
		offerBarkBlockRecipe(Blocks.WARPED_HYPHAE, Blocks.WARPED_STEM);
		offerBarkBlockRecipe(Blocks.MANGROVE_WOOD, Blocks.MANGROVE_LOG);
		offerBarkBlockRecipe(Blocks.STRIPPED_ACACIA_WOOD, Blocks.STRIPPED_ACACIA_LOG);
		offerBarkBlockRecipe(Blocks.STRIPPED_BIRCH_WOOD, Blocks.STRIPPED_BIRCH_LOG);
		offerBarkBlockRecipe(Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_LOG);
		offerBarkBlockRecipe(Blocks.STRIPPED_PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_LOG);
		offerBarkBlockRecipe(Blocks.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_LOG);
		offerBarkBlockRecipe(Blocks.STRIPPED_OAK_WOOD, Blocks.STRIPPED_OAK_LOG);
		offerBarkBlockRecipe(Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_LOG);
		offerBarkBlockRecipe(Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_STEM);
		offerBarkBlockRecipe(Blocks.STRIPPED_WARPED_HYPHAE, Blocks.STRIPPED_WARPED_STEM);
		offerBarkBlockRecipe(Blocks.STRIPPED_MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_LOG);
		offerBoatRecipe(Items.ACACIA_BOAT, Blocks.ACACIA_PLANKS);
		offerBoatRecipe(Items.BIRCH_BOAT, Blocks.BIRCH_PLANKS);
		offerBoatRecipe(Items.DARK_OAK_BOAT, Blocks.DARK_OAK_PLANKS);
		offerBoatRecipe(Items.PALE_OAK_BOAT, Blocks.PALE_OAK_PLANKS);
		offerBoatRecipe(Items.JUNGLE_BOAT, Blocks.JUNGLE_PLANKS);
		offerBoatRecipe(Items.OAK_BOAT, Blocks.OAK_PLANKS);
		offerBoatRecipe(Items.SPRUCE_BOAT, Blocks.SPRUCE_PLANKS);
		offerBoatRecipe(Items.MANGROVE_BOAT, Blocks.MANGROVE_PLANKS);
		offerShelfRecipe(Blocks.ACACIA_SHELF, Items.STRIPPED_ACACIA_LOG);
		offerShelfRecipe(Blocks.BAMBOO_SHELF, Items.STRIPPED_BAMBOO_BLOCK);
		offerShelfRecipe(Blocks.BIRCH_SHELF, Items.STRIPPED_BIRCH_LOG);
		offerShelfRecipe(Blocks.CHERRY_SHELF, Items.STRIPPED_CHERRY_LOG);
		offerShelfRecipe(Blocks.CRIMSON_SHELF, Items.STRIPPED_CRIMSON_STEM);
		offerShelfRecipe(Blocks.DARK_OAK_SHELF, Items.STRIPPED_DARK_OAK_LOG);
		offerShelfRecipe(Blocks.JUNGLE_SHELF, Items.STRIPPED_JUNGLE_LOG);
		offerShelfRecipe(Blocks.MANGROVE_SHELF, Items.STRIPPED_MANGROVE_LOG);
		offerShelfRecipe(Blocks.OAK_SHELF, Items.STRIPPED_OAK_LOG);
		offerShelfRecipe(Blocks.PALE_OAK_SHELF, Items.STRIPPED_PALE_OAK_LOG);
		offerShelfRecipe(Blocks.SPRUCE_SHELF, Items.STRIPPED_SPRUCE_LOG);
		offerShelfRecipe(Blocks.WARPED_SHELF, Items.STRIPPED_WARPED_STEM);
		List<Item> list = List.of(
				Items.BLACK_DYE,
				Items.BLUE_DYE,
				Items.BROWN_DYE,
				Items.CYAN_DYE,
				Items.GRAY_DYE,
				Items.GREEN_DYE,
				Items.LIGHT_BLUE_DYE,
				Items.LIGHT_GRAY_DYE,
				Items.LIME_DYE,
				Items.MAGENTA_DYE,
				Items.ORANGE_DYE,
				Items.PINK_DYE,
				Items.PURPLE_DYE,
				Items.RED_DYE,
				Items.YELLOW_DYE,
				Items.WHITE_DYE
		);
		List<Item> list2 = List.of(
				Items.BLACK_WOOL,
				Items.BLUE_WOOL,
				Items.BROWN_WOOL,
				Items.CYAN_WOOL,
				Items.GRAY_WOOL,
				Items.GREEN_WOOL,
				Items.LIGHT_BLUE_WOOL,
				Items.LIGHT_GRAY_WOOL,
				Items.LIME_WOOL,
				Items.MAGENTA_WOOL,
				Items.ORANGE_WOOL,
				Items.PINK_WOOL,
				Items.PURPLE_WOOL,
				Items.RED_WOOL,
				Items.YELLOW_WOOL,
				Items.WHITE_WOOL
		);
		List<Item> list3 = List.of(
				Items.BLACK_BED,
				Items.BLUE_BED,
				Items.BROWN_BED,
				Items.CYAN_BED,
				Items.GRAY_BED,
				Items.GREEN_BED,
				Items.LIGHT_BLUE_BED,
				Items.LIGHT_GRAY_BED,
				Items.LIME_BED,
				Items.MAGENTA_BED,
				Items.ORANGE_BED,
				Items.PINK_BED,
				Items.PURPLE_BED,
				Items.RED_BED,
				Items.YELLOW_BED,
				Items.WHITE_BED
		);
		List<Item> list4 = List.of(
				Items.BLACK_CARPET,
				Items.BLUE_CARPET,
				Items.BROWN_CARPET,
				Items.CYAN_CARPET,
				Items.GRAY_CARPET,
				Items.GREEN_CARPET,
				Items.LIGHT_BLUE_CARPET,
				Items.LIGHT_GRAY_CARPET,
				Items.LIME_CARPET,
				Items.MAGENTA_CARPET,
				Items.ORANGE_CARPET,
				Items.PINK_CARPET,
				Items.PURPLE_CARPET,
				Items.RED_CARPET,
				Items.YELLOW_CARPET,
				Items.WHITE_CARPET
		);
		List<Item> list5 = List.of(
				Items.BLACK_HARNESS,
				Items.BLUE_HARNESS,
				Items.BROWN_HARNESS,
				Items.CYAN_HARNESS,
				Items.GRAY_HARNESS,
				Items.GREEN_HARNESS,
				Items.LIGHT_BLUE_HARNESS,
				Items.LIGHT_GRAY_HARNESS,
				Items.LIME_HARNESS,
				Items.MAGENTA_HARNESS,
				Items.ORANGE_HARNESS,
				Items.PINK_HARNESS,
				Items.PURPLE_HARNESS,
				Items.RED_HARNESS,
				Items.YELLOW_HARNESS,
				Items.WHITE_HARNESS
		);
		offerDyeableRecipes(list, list2, "wool", RecipeCategory.BUILDING_BLOCKS);
		offerDyeableRecipes(list, list3, "bed_dye", RecipeCategory.DECORATIONS);
		offerDyeableRecipes(list, list4, "carpet_dye", RecipeCategory.DECORATIONS);
		offerDyeableRecipes(list, list5, "harness_dye", RecipeCategory.COMBAT);
		offerCarpetRecipe(Blocks.BLACK_CARPET, Blocks.BLACK_WOOL);
		offerBedRecipe(Items.BLACK_BED, Blocks.BLACK_WOOL);
		offerBannerRecipe(Items.BLACK_BANNER, Blocks.BLACK_WOOL);
		offerCarpetRecipe(Blocks.BLUE_CARPET, Blocks.BLUE_WOOL);
		offerBedRecipe(Items.BLUE_BED, Blocks.BLUE_WOOL);
		offerBannerRecipe(Items.BLUE_BANNER, Blocks.BLUE_WOOL);
		offerCarpetRecipe(Blocks.BROWN_CARPET, Blocks.BROWN_WOOL);
		offerBedRecipe(Items.BROWN_BED, Blocks.BROWN_WOOL);
		offerBannerRecipe(Items.BROWN_BANNER, Blocks.BROWN_WOOL);
		offerCarpetRecipe(Blocks.CYAN_CARPET, Blocks.CYAN_WOOL);
		offerBedRecipe(Items.CYAN_BED, Blocks.CYAN_WOOL);
		offerBannerRecipe(Items.CYAN_BANNER, Blocks.CYAN_WOOL);
		offerCarpetRecipe(Blocks.GRAY_CARPET, Blocks.GRAY_WOOL);
		offerBedRecipe(Items.GRAY_BED, Blocks.GRAY_WOOL);
		offerBannerRecipe(Items.GRAY_BANNER, Blocks.GRAY_WOOL);
		offerCarpetRecipe(Blocks.GREEN_CARPET, Blocks.GREEN_WOOL);
		offerBedRecipe(Items.GREEN_BED, Blocks.GREEN_WOOL);
		offerBannerRecipe(Items.GREEN_BANNER, Blocks.GREEN_WOOL);
		offerCarpetRecipe(Blocks.LIGHT_BLUE_CARPET, Blocks.LIGHT_BLUE_WOOL);
		offerBedRecipe(Items.LIGHT_BLUE_BED, Blocks.LIGHT_BLUE_WOOL);
		offerBannerRecipe(Items.LIGHT_BLUE_BANNER, Blocks.LIGHT_BLUE_WOOL);
		offerCarpetRecipe(Blocks.LIGHT_GRAY_CARPET, Blocks.LIGHT_GRAY_WOOL);
		offerBedRecipe(Items.LIGHT_GRAY_BED, Blocks.LIGHT_GRAY_WOOL);
		offerBannerRecipe(Items.LIGHT_GRAY_BANNER, Blocks.LIGHT_GRAY_WOOL);
		offerCarpetRecipe(Blocks.LIME_CARPET, Blocks.LIME_WOOL);
		offerBedRecipe(Items.LIME_BED, Blocks.LIME_WOOL);
		offerBannerRecipe(Items.LIME_BANNER, Blocks.LIME_WOOL);
		offerCarpetRecipe(Blocks.MAGENTA_CARPET, Blocks.MAGENTA_WOOL);
		offerBedRecipe(Items.MAGENTA_BED, Blocks.MAGENTA_WOOL);
		offerBannerRecipe(Items.MAGENTA_BANNER, Blocks.MAGENTA_WOOL);
		offerCarpetRecipe(Blocks.ORANGE_CARPET, Blocks.ORANGE_WOOL);
		offerBedRecipe(Items.ORANGE_BED, Blocks.ORANGE_WOOL);
		offerBannerRecipe(Items.ORANGE_BANNER, Blocks.ORANGE_WOOL);
		offerCarpetRecipe(Blocks.PINK_CARPET, Blocks.PINK_WOOL);
		offerBedRecipe(Items.PINK_BED, Blocks.PINK_WOOL);
		offerBannerRecipe(Items.PINK_BANNER, Blocks.PINK_WOOL);
		offerCarpetRecipe(Blocks.PURPLE_CARPET, Blocks.PURPLE_WOOL);
		offerBedRecipe(Items.PURPLE_BED, Blocks.PURPLE_WOOL);
		offerBannerRecipe(Items.PURPLE_BANNER, Blocks.PURPLE_WOOL);
		offerCarpetRecipe(Blocks.RED_CARPET, Blocks.RED_WOOL);
		offerBedRecipe(Items.RED_BED, Blocks.RED_WOOL);
		offerBannerRecipe(Items.RED_BANNER, Blocks.RED_WOOL);
		offerCarpetRecipe(Blocks.WHITE_CARPET, Blocks.WHITE_WOOL);
		offerBedRecipe(Items.WHITE_BED, Blocks.WHITE_WOOL);
		offerBannerRecipe(Items.WHITE_BANNER, Blocks.WHITE_WOOL);
		offerCarpetRecipe(Blocks.YELLOW_CARPET, Blocks.YELLOW_WOOL);
		offerBedRecipe(Items.YELLOW_BED, Blocks.YELLOW_WOOL);
		offerBannerRecipe(Items.YELLOW_BANNER, Blocks.YELLOW_WOOL);
		offerCarpetRecipe(Blocks.MOSS_CARPET, Blocks.MOSS_BLOCK);
		offerCarpetRecipe(Blocks.PALE_MOSS_CARPET, Blocks.PALE_MOSS_BLOCK);
		offerHarness(Items.WHITE_HARNESS, Blocks.WHITE_WOOL);
		offerHarness(Items.ORANGE_HARNESS, Blocks.ORANGE_WOOL);
		offerHarness(Items.MAGENTA_HARNESS, Blocks.MAGENTA_WOOL);
		offerHarness(Items.LIGHT_BLUE_HARNESS, Blocks.LIGHT_BLUE_WOOL);
		offerHarness(Items.YELLOW_HARNESS, Blocks.YELLOW_WOOL);
		offerHarness(Items.LIME_HARNESS, Blocks.LIME_WOOL);
		offerHarness(Items.PINK_HARNESS, Blocks.PINK_WOOL);
		offerHarness(Items.GRAY_HARNESS, Blocks.GRAY_WOOL);
		offerHarness(Items.LIGHT_GRAY_HARNESS, Blocks.LIGHT_GRAY_WOOL);
		offerHarness(Items.CYAN_HARNESS, Blocks.CYAN_WOOL);
		offerHarness(Items.PURPLE_HARNESS, Blocks.PURPLE_WOOL);
		offerHarness(Items.BLUE_HARNESS, Blocks.BLUE_WOOL);
		offerHarness(Items.BROWN_HARNESS, Blocks.BROWN_WOOL);
		offerHarness(Items.GREEN_HARNESS, Blocks.GREEN_WOOL);
		offerHarness(Items.RED_HARNESS, Blocks.RED_WOOL);
		offerHarness(Items.BLACK_HARNESS, Blocks.BLACK_WOOL);
		offerStainedGlassDyeingRecipe(Blocks.BLACK_STAINED_GLASS, Items.BLACK_DYE);
		offerStainedGlassPaneRecipe(Blocks.BLACK_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.BLACK_STAINED_GLASS_PANE, Items.BLACK_DYE);
		offerStainedGlassDyeingRecipe(Blocks.BLUE_STAINED_GLASS, Items.BLUE_DYE);
		offerStainedGlassPaneRecipe(Blocks.BLUE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.BLUE_STAINED_GLASS_PANE, Items.BLUE_DYE);
		offerStainedGlassDyeingRecipe(Blocks.BROWN_STAINED_GLASS, Items.BROWN_DYE);
		offerStainedGlassPaneRecipe(Blocks.BROWN_STAINED_GLASS_PANE, Blocks.BROWN_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.BROWN_STAINED_GLASS_PANE, Items.BROWN_DYE);
		offerStainedGlassDyeingRecipe(Blocks.CYAN_STAINED_GLASS, Items.CYAN_DYE);
		offerStainedGlassPaneRecipe(Blocks.CYAN_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.CYAN_STAINED_GLASS_PANE, Items.CYAN_DYE);
		offerStainedGlassDyeingRecipe(Blocks.GRAY_STAINED_GLASS, Items.GRAY_DYE);
		offerStainedGlassPaneRecipe(Blocks.GRAY_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.GRAY_STAINED_GLASS_PANE, Items.GRAY_DYE);
		offerStainedGlassDyeingRecipe(Blocks.GREEN_STAINED_GLASS, Items.GREEN_DYE);
		offerStainedGlassPaneRecipe(Blocks.GREEN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.GREEN_STAINED_GLASS_PANE, Items.GREEN_DYE);
		offerStainedGlassDyeingRecipe(Blocks.LIGHT_BLUE_STAINED_GLASS, Items.LIGHT_BLUE_DYE);
		offerStainedGlassPaneRecipe(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Items.LIGHT_BLUE_DYE);
		offerStainedGlassDyeingRecipe(Blocks.LIGHT_GRAY_STAINED_GLASS, Items.LIGHT_GRAY_DYE);
		offerStainedGlassPaneRecipe(Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Items.LIGHT_GRAY_DYE);
		offerStainedGlassDyeingRecipe(Blocks.LIME_STAINED_GLASS, Items.LIME_DYE);
		offerStainedGlassPaneRecipe(Blocks.LIME_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.LIME_STAINED_GLASS_PANE, Items.LIME_DYE);
		offerStainedGlassDyeingRecipe(Blocks.MAGENTA_STAINED_GLASS, Items.MAGENTA_DYE);
		offerStainedGlassPaneRecipe(Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.MAGENTA_STAINED_GLASS_PANE, Items.MAGENTA_DYE);
		offerStainedGlassDyeingRecipe(Blocks.ORANGE_STAINED_GLASS, Items.ORANGE_DYE);
		offerStainedGlassPaneRecipe(Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.ORANGE_STAINED_GLASS_PANE, Items.ORANGE_DYE);
		offerStainedGlassDyeingRecipe(Blocks.PINK_STAINED_GLASS, Items.PINK_DYE);
		offerStainedGlassPaneRecipe(Blocks.PINK_STAINED_GLASS_PANE, Blocks.PINK_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.PINK_STAINED_GLASS_PANE, Items.PINK_DYE);
		offerStainedGlassDyeingRecipe(Blocks.PURPLE_STAINED_GLASS, Items.PURPLE_DYE);
		offerStainedGlassPaneRecipe(Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.PURPLE_STAINED_GLASS_PANE, Items.PURPLE_DYE);
		offerStainedGlassDyeingRecipe(Blocks.RED_STAINED_GLASS, Items.RED_DYE);
		offerStainedGlassPaneRecipe(Blocks.RED_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.RED_STAINED_GLASS_PANE, Items.RED_DYE);
		offerStainedGlassDyeingRecipe(Blocks.WHITE_STAINED_GLASS, Items.WHITE_DYE);
		offerStainedGlassPaneRecipe(Blocks.WHITE_STAINED_GLASS_PANE, Blocks.WHITE_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.WHITE_STAINED_GLASS_PANE, Items.WHITE_DYE);
		offerStainedGlassDyeingRecipe(Blocks.YELLOW_STAINED_GLASS, Items.YELLOW_DYE);
		offerStainedGlassPaneRecipe(Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS);
		offerStainedGlassPaneDyeingRecipe(Blocks.YELLOW_STAINED_GLASS_PANE, Items.YELLOW_DYE);
		offerTerracottaDyeingRecipe(Blocks.BLACK_TERRACOTTA, Items.BLACK_DYE);
		offerTerracottaDyeingRecipe(Blocks.BLUE_TERRACOTTA, Items.BLUE_DYE);
		offerTerracottaDyeingRecipe(Blocks.BROWN_TERRACOTTA, Items.BROWN_DYE);
		offerTerracottaDyeingRecipe(Blocks.CYAN_TERRACOTTA, Items.CYAN_DYE);
		offerTerracottaDyeingRecipe(Blocks.GRAY_TERRACOTTA, Items.GRAY_DYE);
		offerTerracottaDyeingRecipe(Blocks.GREEN_TERRACOTTA, Items.GREEN_DYE);
		offerTerracottaDyeingRecipe(Blocks.LIGHT_BLUE_TERRACOTTA, Items.LIGHT_BLUE_DYE);
		offerTerracottaDyeingRecipe(Blocks.LIGHT_GRAY_TERRACOTTA, Items.LIGHT_GRAY_DYE);
		offerTerracottaDyeingRecipe(Blocks.LIME_TERRACOTTA, Items.LIME_DYE);
		offerTerracottaDyeingRecipe(Blocks.MAGENTA_TERRACOTTA, Items.MAGENTA_DYE);
		offerTerracottaDyeingRecipe(Blocks.ORANGE_TERRACOTTA, Items.ORANGE_DYE);
		offerTerracottaDyeingRecipe(Blocks.PINK_TERRACOTTA, Items.PINK_DYE);
		offerTerracottaDyeingRecipe(Blocks.PURPLE_TERRACOTTA, Items.PURPLE_DYE);
		offerTerracottaDyeingRecipe(Blocks.RED_TERRACOTTA, Items.RED_DYE);
		offerTerracottaDyeingRecipe(Blocks.WHITE_TERRACOTTA, Items.WHITE_DYE);
		offerTerracottaDyeingRecipe(Blocks.YELLOW_TERRACOTTA, Items.YELLOW_DYE);
		offerConcretePowderDyeingRecipe(Blocks.BLACK_CONCRETE_POWDER, Items.BLACK_DYE);
		offerConcretePowderDyeingRecipe(Blocks.BLUE_CONCRETE_POWDER, Items.BLUE_DYE);
		offerConcretePowderDyeingRecipe(Blocks.BROWN_CONCRETE_POWDER, Items.BROWN_DYE);
		offerConcretePowderDyeingRecipe(Blocks.CYAN_CONCRETE_POWDER, Items.CYAN_DYE);
		offerConcretePowderDyeingRecipe(Blocks.GRAY_CONCRETE_POWDER, Items.GRAY_DYE);
		offerConcretePowderDyeingRecipe(Blocks.GREEN_CONCRETE_POWDER, Items.GREEN_DYE);
		offerConcretePowderDyeingRecipe(Blocks.LIGHT_BLUE_CONCRETE_POWDER, Items.LIGHT_BLUE_DYE);
		offerConcretePowderDyeingRecipe(Blocks.LIGHT_GRAY_CONCRETE_POWDER, Items.LIGHT_GRAY_DYE);
		offerConcretePowderDyeingRecipe(Blocks.LIME_CONCRETE_POWDER, Items.LIME_DYE);
		offerConcretePowderDyeingRecipe(Blocks.MAGENTA_CONCRETE_POWDER, Items.MAGENTA_DYE);
		offerConcretePowderDyeingRecipe(Blocks.ORANGE_CONCRETE_POWDER, Items.ORANGE_DYE);
		offerConcretePowderDyeingRecipe(Blocks.PINK_CONCRETE_POWDER, Items.PINK_DYE);
		offerConcretePowderDyeingRecipe(Blocks.PURPLE_CONCRETE_POWDER, Items.PURPLE_DYE);
		offerConcretePowderDyeingRecipe(Blocks.RED_CONCRETE_POWDER, Items.RED_DYE);
		offerConcretePowderDyeingRecipe(Blocks.WHITE_CONCRETE_POWDER, Items.WHITE_DYE);
		offerConcretePowderDyeingRecipe(Blocks.YELLOW_CONCRETE_POWDER, Items.YELLOW_DYE);
		offerDriedGhast(Blocks.DRIED_GHAST);
		createShaped(RecipeCategory.DECORATIONS, Items.CANDLE)
		    .input('S', Items.STRING)
		    .input('H', Items.HONEYCOMB)
		    .pattern("S")
		    .pattern("H")
		    .criterion("has_string", conditionsFromItem(Items.STRING))
		    .criterion("has_honeycomb", conditionsFromItem(Items.HONEYCOMB))
		    .offerTo(exporter);
		offerCandleDyeingRecipe(Blocks.BLACK_CANDLE, Items.BLACK_DYE);
		offerCandleDyeingRecipe(Blocks.BLUE_CANDLE, Items.BLUE_DYE);
		offerCandleDyeingRecipe(Blocks.BROWN_CANDLE, Items.BROWN_DYE);
		offerCandleDyeingRecipe(Blocks.CYAN_CANDLE, Items.CYAN_DYE);
		offerCandleDyeingRecipe(Blocks.GRAY_CANDLE, Items.GRAY_DYE);
		offerCandleDyeingRecipe(Blocks.GREEN_CANDLE, Items.GREEN_DYE);
		offerCandleDyeingRecipe(Blocks.LIGHT_BLUE_CANDLE, Items.LIGHT_BLUE_DYE);
		offerCandleDyeingRecipe(Blocks.LIGHT_GRAY_CANDLE, Items.LIGHT_GRAY_DYE);
		offerCandleDyeingRecipe(Blocks.LIME_CANDLE, Items.LIME_DYE);
		offerCandleDyeingRecipe(Blocks.MAGENTA_CANDLE, Items.MAGENTA_DYE);
		offerCandleDyeingRecipe(Blocks.ORANGE_CANDLE, Items.ORANGE_DYE);
		offerCandleDyeingRecipe(Blocks.PINK_CANDLE, Items.PINK_DYE);
		offerCandleDyeingRecipe(Blocks.PURPLE_CANDLE, Items.PURPLE_DYE);
		offerCandleDyeingRecipe(Blocks.RED_CANDLE, Items.RED_DYE);
		offerCandleDyeingRecipe(Blocks.WHITE_CANDLE, Items.WHITE_DYE);
		offerCandleDyeingRecipe(Blocks.YELLOW_CANDLE, Items.YELLOW_DYE);
		createShapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.PACKED_MUD, 1)
		    .input(Blocks.MUD)
		    .input(Items.WHEAT)
		    .criterion("has_mud", conditionsFromItem(Blocks.MUD))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.MUD_BRICKS, 4)
		    .input('#', Blocks.PACKED_MUD)
		    .pattern("##")
		    .pattern("##")
		    .criterion("has_packed_mud", conditionsFromItem(Blocks.PACKED_MUD))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.MUDDY_MANGROVE_ROOTS, 1)
		    .input(Blocks.MUD)
		    .input(Items.MANGROVE_ROOTS)
		    .criterion("has_mangrove_roots", conditionsFromItem(Blocks.MANGROVE_ROOTS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TRANSPORTATION, Blocks.ACTIVATOR_RAIL, 6)
		    .input('#', Blocks.REDSTONE_TORCH)
		    .input('S', Items.STICK)
		    .input('X', Items.IRON_INGOT)
		    .pattern("XSX")
		    .pattern("X#X")
		    .pattern("XSX")
		    .criterion("has_rail", conditionsFromItem(Blocks.RAIL))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.ANDESITE, 2)
		    .input(Blocks.DIORITE)
		    .input(Blocks.COBBLESTONE)
		    .criterion("has_stone", conditionsFromItem(Blocks.DIORITE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.ANVIL)
		    .input('I', Blocks.IRON_BLOCK)
		    .input('i', Items.IRON_INGOT)
		    .pattern("III")
		    .pattern(" i ")
		    .pattern("iii")
		    .criterion("has_iron_block", conditionsFromItem(Blocks.IRON_BLOCK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Items.ARMOR_STAND)
		    .input('/', Items.STICK)
		    .input('_', Blocks.SMOOTH_STONE_SLAB)
		    .pattern("///")
		    .pattern(" / ")
		    .pattern("/_/")
		    .criterion("has_stone_slab", conditionsFromItem(Blocks.SMOOTH_STONE_SLAB))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.ARROW, 4)
		    .input('#', Items.STICK)
		    .input('X', Items.FLINT)
		    .input('Y', Items.FEATHER)
		    .pattern("X")
		    .pattern("#")
		    .pattern("Y")
		    .criterion("has_feather", conditionsFromItem(Items.FEATHER))
		    .criterion("has_flint", conditionsFromItem(Items.FLINT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.BARREL, 1)
		    .input('P', ItemTags.PLANKS)
		    .input('S', ItemTags.WOODEN_SLABS)
		    .pattern("PSP")
		    .pattern("P P")
		    .pattern("PSP")
		    .criterion("has_planks", conditionsFromTag(ItemTags.PLANKS))
		    .criterion("has_wood_slab", conditionsFromTag(ItemTags.WOODEN_SLABS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.MISC, Blocks.BEACON)
		    .input('S', Items.NETHER_STAR)
		    .input('G', Blocks.GLASS)
		    .input('O', Blocks.OBSIDIAN)
		    .pattern("GGG")
		    .pattern("GSG")
		    .pattern("OOO")
		    .criterion("has_nether_star", conditionsFromItem(Items.NETHER_STAR))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.BEEHIVE)
		    .input('P', ItemTags.PLANKS)
		    .input('H', Items.HONEYCOMB)
		    .pattern("PPP")
		    .pattern("HHH")
		    .pattern("PPP")
		    .criterion("has_honeycomb", conditionsFromItem(Items.HONEYCOMB))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.FOOD, Items.BEETROOT_SOUP)
		    .input(Items.BOWL)
		    .input(Items.BEETROOT, 6)
		    .criterion("has_beetroot", conditionsFromItem(Items.BEETROOT))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.BLACK_DYE)
		    .input(Items.INK_SAC)
		    .group("black_dye")
		    .criterion("has_ink_sac", conditionsFromItem(Items.INK_SAC))
		    .offerTo(exporter);
		offerSingleOutputShapelessRecipe(Items.BLACK_DYE, Blocks.WITHER_ROSE, "black_dye");
		createShapeless(RecipeCategory.BREWING, Items.BLAZE_POWDER, 2)
		    .input(Items.BLAZE_ROD)
		    .criterion("has_blaze_rod", conditionsFromItem(Items.BLAZE_ROD))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.BLUE_DYE)
		    .input(Items.LAPIS_LAZULI)
		    .group("blue_dye")
		    .criterion("has_lapis_lazuli", conditionsFromItem(Items.LAPIS_LAZULI))
		    .offerTo(exporter);
		offerSingleOutputShapelessRecipe(Items.BLUE_DYE, Blocks.CORNFLOWER, "blue_dye");
		offerCompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.BLUE_ICE, Blocks.PACKED_ICE);
		createShapeless(RecipeCategory.MISC, Items.BONE_MEAL, 3)
		    .input(Items.BONE)
		    .group("bonemeal")
		    .criterion("has_bone", conditionsFromItem(Items.BONE))
		    .offerTo(exporter);
		offerReversibleCompactingRecipesWithReverseRecipeGroup(
				RecipeCategory.MISC,
				Items.BONE_MEAL,
				RecipeCategory.BUILDING_BLOCKS,
				Items.BONE_BLOCK,
				"bone_meal_from_bone_block",
				"bonemeal"
		);
		createShapeless(RecipeCategory.MISC, Items.BOOK)
		    .input(Items.PAPER, 3)
		    .input(Items.LEATHER)
		    .criterion("has_paper", conditionsFromItem(Items.PAPER))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.BOOKSHELF)
		    .input('#', ItemTags.PLANKS)
		    .input('X', Items.BOOK)
		    .pattern("###")
		    .pattern("XXX")
		    .pattern("###")
		    .criterion("has_book", conditionsFromItem(Items.BOOK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.BOW)
		    .input('#', Items.STICK)
		    .input('X', Items.STRING)
		    .pattern(" #X")
		    .pattern("# X")
		    .pattern(" #X")
		    .criterion("has_string", conditionsFromItem(Items.STRING))
		    .offerTo(exporter);
		createShaped(RecipeCategory.MISC, Items.BOWL, 4)
		    .input('#', ItemTags.PLANKS)
		    .pattern("# #")
		    .pattern(" # ")
		    .criterion("has_brown_mushroom", conditionsFromItem(Blocks.BROWN_MUSHROOM))
		    .criterion("has_red_mushroom", conditionsFromItem(Blocks.RED_MUSHROOM))
		    .criterion("has_mushroom_stew", conditionsFromItem(Items.MUSHROOM_STEW))
		    .offerTo(exporter);
		createShaped(RecipeCategory.FOOD, Items.BREAD)
		    .input('#', Items.WHEAT)
		    .pattern("###")
		    .criterion("has_wheat", conditionsFromItem(Items.WHEAT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BREWING, Blocks.BREWING_STAND)
		    .input('B', Items.BLAZE_ROD)
		    .input('#', ItemTags.STONE_CRAFTING_MATERIALS)
		    .pattern(" B ")
		    .pattern("###")
		    .criterion("has_blaze_rod", conditionsFromItem(Items.BLAZE_ROD))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.BRICKS)
		    .input('#', Items.BRICK)
		    .pattern("##")
		    .pattern("##")
		    .criterion("has_brick", conditionsFromItem(Items.BRICK))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.BROWN_DYE)
		    .input(Items.COCOA_BEANS)
		    .group("brown_dye")
		    .criterion("has_cocoa_beans", conditionsFromItem(Items.COCOA_BEANS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.MISC, Items.BUCKET)
		    .input('#', Items.IRON_INGOT)
		    .pattern("# #")
		    .pattern(" # ")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.FOOD, Blocks.CAKE)
		    .input('A', Items.MILK_BUCKET)
		    .input('B', Items.SUGAR)
		    .input('C', Items.WHEAT)
		    .input('E', ItemTags.EGGS)
		    .pattern("AAA")
		    .pattern("BEB")
		    .pattern("CCC")
		    .criterion("has_egg", conditionsFromTag(ItemTags.EGGS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.CAMPFIRE)
		    .input('L', ItemTags.LOGS)
		    .input('S', Items.STICK)
		    .input('C', ItemTags.COALS)
		    .pattern(" S ")
		    .pattern("SCS")
		    .pattern("LLL")
		    .criterion("has_stick", conditionsFromItem(Items.STICK))
		    .criterion("has_coal", conditionsFromTag(ItemTags.COALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TRANSPORTATION, Items.CARROT_ON_A_STICK)
		    .input('#', Items.FISHING_ROD)
		    .input('X', Items.CARROT)
		    .pattern("# ")
		    .pattern(" X")
		    .criterion("has_carrot", conditionsFromItem(Items.CARROT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TRANSPORTATION, Items.WARPED_FUNGUS_ON_A_STICK)
		    .input('#', Items.FISHING_ROD)
		    .input('X', Items.WARPED_FUNGUS)
		    .pattern("# ")
		    .pattern(" X")
		    .criterion("has_warped_fungus", conditionsFromItem(Items.WARPED_FUNGUS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BREWING, Blocks.CAULDRON)
		    .input('#', Items.IRON_INGOT)
		    .pattern("# #")
		    .pattern("# #")
		    .pattern("###")
		    .criterion("has_water_bucket", conditionsFromItem(Items.WATER_BUCKET))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.COMPOSTER)
		    .input('#', ItemTags.WOODEN_SLABS)
		    .pattern("# #")
		    .pattern("# #")
		    .pattern("###")
		    .criterion("has_wood_slab", conditionsFromTag(ItemTags.WOODEN_SLABS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.CHEST)
		    .input('#', ItemTags.PLANKS)
		    .pattern("###")
		    .pattern("# #")
		    .pattern("###")
		    .criterion(
				    "has_lots_of_items",
				    Criteria.INVENTORY_CHANGED
						    .create(
								    new InventoryChangedCriterion.Conditions(
										    Optional.empty(),
										    new InventoryChangedCriterion.Conditions.Slots(
												    NumberRange.IntRange.atLeast(10),
												    NumberRange.IntRange.ANY,
												    NumberRange.IntRange.ANY
										    ),
										    List.of()
								    )
						    )
		    )
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.COPPER_CHEST)
		    .input('#', Items.COPPER_INGOT)
		    .input('X', Items.CHEST)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .criterion("has_copper_chest", conditionsFromItem(Items.COPPER_CHEST))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.TRANSPORTATION, Items.CHEST_MINECART)
		    .input(Blocks.CHEST)
		    .input(Items.MINECART)
		    .criterion("has_minecart", conditionsFromItem(Items.MINECART))
		    .offerTo(exporter);
		offerChestBoatRecipe(Items.ACACIA_CHEST_BOAT, Items.ACACIA_BOAT);
		offerChestBoatRecipe(Items.BIRCH_CHEST_BOAT, Items.BIRCH_BOAT);
		offerChestBoatRecipe(Items.DARK_OAK_CHEST_BOAT, Items.DARK_OAK_BOAT);
		offerChestBoatRecipe(Items.PALE_OAK_CHEST_BOAT, Items.PALE_OAK_BOAT);
		offerChestBoatRecipe(Items.JUNGLE_CHEST_BOAT, Items.JUNGLE_BOAT);
		offerChestBoatRecipe(Items.OAK_CHEST_BOAT, Items.OAK_BOAT);
		offerChestBoatRecipe(Items.SPRUCE_CHEST_BOAT, Items.SPRUCE_BOAT);
		offerChestBoatRecipe(Items.MANGROVE_CHEST_BOAT, Items.MANGROVE_BOAT);
		this
				.createChiseledBlockRecipe(
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.CHISELED_QUARTZ_BLOCK,
						Ingredient.ofItem(Blocks.QUARTZ_SLAB)
				)
				.criterion("has_chiseled_quartz_block", conditionsFromItem(Blocks.CHISELED_QUARTZ_BLOCK))
				.criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK))
				.criterion("has_quartz_pillar", conditionsFromItem(Blocks.QUARTZ_PILLAR))
				.offerTo(exporter);
		this
				.createChiseledBlockRecipe(
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.CHISELED_STONE_BRICKS,
						Ingredient.ofItem(Blocks.STONE_BRICK_SLAB)
				)
				.criterion("has_tag", conditionsFromTag(ItemTags.STONE_BRICKS))
				.offerTo(exporter);
		offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CLAY, Items.CLAY_BALL);
		createShaped(RecipeCategory.TOOLS, Items.CLOCK)
		    .input('#', Items.GOLD_INGOT)
		    .input('X', Items.REDSTONE)
		    .pattern(" # ")
		    .pattern("#X#")
		    .pattern(" # ")
		    .criterion("has_redstone", conditionsFromItem(Items.REDSTONE))
		    .offerTo(exporter);
		offerReversibleCompactingRecipes(
				RecipeCategory.MISC,
				Items.COAL,
				RecipeCategory.BUILDING_BLOCKS,
				Items.COAL_BLOCK
		);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.COARSE_DIRT, 4)
		    .input('D', Blocks.DIRT)
		    .input('G', Blocks.GRAVEL)
		    .pattern("DG")
		    .pattern("GD")
		    .criterion("has_gravel", conditionsFromItem(Blocks.GRAVEL))
		    .offerTo(exporter);
		createShaped(RecipeCategory.REDSTONE, Blocks.COMPARATOR)
		    .input('#', Blocks.REDSTONE_TORCH)
		    .input('X', Items.QUARTZ)
		    .input('I', Blocks.STONE)
		    .pattern(" # ")
		    .pattern("#X#")
		    .pattern("III")
		    .criterion("has_quartz", conditionsFromItem(Items.QUARTZ))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.COMPASS)
		    .input('#', Items.IRON_INGOT)
		    .input('X', Items.REDSTONE)
		    .pattern(" # ")
		    .pattern("#X#")
		    .pattern(" # ")
		    .criterion("has_redstone", conditionsFromItem(Items.REDSTONE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.FOOD, Items.COOKIE, 8)
		    .input('#', Items.WHEAT)
		    .input('X', Items.COCOA_BEANS)
		    .pattern("#X#")
		    .criterion("has_cocoa", conditionsFromItem(Items.COCOA_BEANS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.CRAFTING_TABLE)
		    .input('#', ItemTags.PLANKS)
		    .pattern("##")
		    .pattern("##")
		    .criterion("unlock_right_away", TickCriterion.Conditions.createTick())
		    .showNotification(false)
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.CROSSBOW)
		    .input('~', Items.STRING)
		    .input('#', Items.STICK)
		    .input('&', Items.IRON_INGOT)
		    .input('$', Blocks.TRIPWIRE_HOOK)
		    .pattern("#&#")
		    .pattern("~$~")
		    .pattern(" # ")
		    .criterion("has_string", conditionsFromItem(Items.STRING))
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .criterion("has_tripwire_hook", conditionsFromItem(Blocks.TRIPWIRE_HOOK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.LOOM)
		    .input('#', ItemTags.PLANKS)
		    .input('@', Items.STRING)
		    .pattern("@@")
		    .pattern("##")
		    .criterion("has_string", conditionsFromItem(Items.STRING))
		    .offerTo(exporter);
		this
				.createChiseledBlockRecipe(
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.CHISELED_RED_SANDSTONE,
						Ingredient.ofItem(Blocks.RED_SANDSTONE_SLAB)
				)
				.criterion("has_red_sandstone", conditionsFromItem(Blocks.RED_SANDSTONE))
				.criterion("has_chiseled_red_sandstone", conditionsFromItem(Blocks.CHISELED_RED_SANDSTONE))
				.criterion("has_cut_red_sandstone", conditionsFromItem(Blocks.CUT_RED_SANDSTONE))
				.offerTo(exporter);
		offerChiseledBlockRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_SANDSTONE, Blocks.SANDSTONE_SLAB);
		offerReversibleCompactingRecipesWithReverseRecipeGroup(
				RecipeCategory.MISC,
				Items.COPPER_INGOT,
				RecipeCategory.BUILDING_BLOCKS,
				Items.COPPER_BLOCK,
				getRecipeName(Items.COPPER_INGOT),
				getItemPath(Items.COPPER_INGOT)
		);
		createShapeless(RecipeCategory.MISC, Items.COPPER_INGOT, 9)
		    .input(Blocks.WAXED_COPPER_BLOCK)
		    .group(getItemPath(Items.COPPER_INGOT))
		    .criterion(hasItem(Blocks.WAXED_COPPER_BLOCK), conditionsFromItem(Blocks.WAXED_COPPER_BLOCK))
		    .offerTo(exporter, convertBetween(Items.COPPER_INGOT, Blocks.WAXED_COPPER_BLOCK));
		offerWaxingRecipes(FeatureSet.of(FeatureFlags.VANILLA));
		createShapeless(RecipeCategory.MISC, Items.CYAN_DYE, 2)
		    .input(Items.BLUE_DYE)
		    .input(Items.GREEN_DYE)
		    .group("cyan_dye")
		    .criterion("has_green_dye", conditionsFromItem(Items.GREEN_DYE))
		    .criterion("has_blue_dye", conditionsFromItem(Items.BLUE_DYE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.DARK_PRISMARINE)
		    .input('S', Items.PRISMARINE_SHARD)
		    .input('I', Items.BLACK_DYE)
		    .pattern("SSS")
		    .pattern("SIS")
		    .pattern("SSS")
		    .criterion("has_prismarine_shard", conditionsFromItem(Items.PRISMARINE_SHARD))
		    .offerTo(exporter);
		createShaped(RecipeCategory.REDSTONE, Blocks.DAYLIGHT_DETECTOR)
		    .input('Q', Items.QUARTZ)
		    .input('G', Blocks.GLASS)
		    .input('W', ItemTags.WOODEN_SLABS)
		    .pattern("GGG")
		    .pattern("QQQ")
		    .pattern("WWW")
		    .criterion("has_quartz", conditionsFromItem(Items.QUARTZ))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.DEEPSLATE_BRICKS, 4)
		    .input('S', Blocks.POLISHED_DEEPSLATE)
		    .pattern("SS")
		    .pattern("SS")
		    .criterion("has_polished_deepslate", conditionsFromItem(Blocks.POLISHED_DEEPSLATE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.DEEPSLATE_TILES, 4)
		    .input('S', Blocks.DEEPSLATE_BRICKS)
		    .pattern("SS")
		    .pattern("SS")
		    .criterion("has_deepslate_bricks", conditionsFromItem(Blocks.DEEPSLATE_BRICKS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TRANSPORTATION, Blocks.DETECTOR_RAIL, 6)
		    .input('R', Items.REDSTONE)
		    .input('#', Blocks.STONE_PRESSURE_PLATE)
		    .input('X', Items.IRON_INGOT)
		    .pattern("X X")
		    .pattern("X#X")
		    .pattern("XRX")
		    .criterion("has_rail", conditionsFromItem(Blocks.RAIL))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.DIAMOND_AXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.DIAMOND_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern("X#")
		    .pattern(" #")
		    .criterion("has_diamond", conditionsFromTag(ItemTags.DIAMOND_TOOL_MATERIALS))
		    .offerTo(exporter);
		offerReversibleCompactingRecipes(
				RecipeCategory.MISC,
				Items.DIAMOND,
				RecipeCategory.BUILDING_BLOCKS,
				Items.DIAMOND_BLOCK
		);
		createShaped(RecipeCategory.COMBAT, Items.DIAMOND_BOOTS)
		    .input('X', Items.DIAMOND)
		    .pattern("X X")
		    .pattern("X X")
		    .criterion("has_diamond", conditionsFromItem(Items.DIAMOND))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.DIAMOND_CHESTPLATE)
		    .input('X', Items.DIAMOND)
		    .pattern("X X")
		    .pattern("XXX")
		    .pattern("XXX")
		    .criterion("has_diamond", conditionsFromItem(Items.DIAMOND))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.DIAMOND_HELMET)
		    .input('X', Items.DIAMOND)
		    .pattern("XXX")
		    .pattern("X X")
		    .criterion("has_diamond", conditionsFromItem(Items.DIAMOND))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.DIAMOND_HOE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.DIAMOND_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern(" #")
		    .pattern(" #")
		    .criterion("has_diamond", conditionsFromTag(ItemTags.DIAMOND_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.DIAMOND_LEGGINGS)
		    .input('X', Items.DIAMOND)
		    .pattern("XXX")
		    .pattern("X X")
		    .pattern("X X")
		    .criterion("has_diamond", conditionsFromItem(Items.DIAMOND))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.DIAMOND_PICKAXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.DIAMOND_TOOL_MATERIALS)
		    .pattern("XXX")
		    .pattern(" # ")
		    .pattern(" # ")
		    .criterion("has_diamond", conditionsFromTag(ItemTags.DIAMOND_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.DIAMOND_SHOVEL)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.DIAMOND_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("#")
		    .pattern("#")
		    .criterion("has_diamond", conditionsFromTag(ItemTags.DIAMOND_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.DIAMOND_SWORD)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.DIAMOND_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("X")
		    .pattern("#")
		    .criterion("has_diamond", conditionsFromTag(ItemTags.DIAMOND_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.DIAMOND_SPEAR)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.DIAMOND_TOOL_MATERIALS)
		    .pattern("  X")
		    .pattern(" # ")
		    .pattern("#  ")
		    .criterion("has_diamond", conditionsFromTag(ItemTags.DIAMOND_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.DIORITE, 2)
		    .input('Q', Items.QUARTZ)
		    .input('C', Blocks.COBBLESTONE)
		    .pattern("CQ")
		    .pattern("QC")
		    .criterion("has_quartz", conditionsFromItem(Items.QUARTZ))
		    .offerTo(exporter);
		createShaped(RecipeCategory.REDSTONE, Blocks.DISPENSER)
		    .input('R', Items.REDSTONE)
		    .input('#', Blocks.COBBLESTONE)
		    .input('X', Items.BOW)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("#R#")
		    .criterion("has_bow", conditionsFromItem(Items.BOW))
		    .offerTo(exporter);
		offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.DRIPSTONE_BLOCK, Items.POINTED_DRIPSTONE);
		createShaped(RecipeCategory.REDSTONE, Blocks.DROPPER)
		    .input('R', Items.REDSTONE)
		    .input('#', Blocks.COBBLESTONE)
		    .pattern("###")
		    .pattern("# #")
		    .pattern("#R#")
		    .criterion("has_redstone", conditionsFromItem(Items.REDSTONE))
		    .offerTo(exporter);
		offerReversibleCompactingRecipes(
				RecipeCategory.MISC,
				Items.EMERALD,
				RecipeCategory.BUILDING_BLOCKS,
				Items.EMERALD_BLOCK
		);
		createShaped(RecipeCategory.DECORATIONS, Blocks.ENCHANTING_TABLE)
		    .input('B', Items.BOOK)
		    .input('#', Blocks.OBSIDIAN)
		    .input('D', Items.DIAMOND)
		    .pattern(" B ")
		    .pattern("D#D")
		    .pattern("###")
		    .criterion("has_obsidian", conditionsFromItem(Blocks.OBSIDIAN))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.ENDER_CHEST)
		    .input('#', Blocks.OBSIDIAN)
		    .input('E', Items.ENDER_EYE)
		    .pattern("###")
		    .pattern("#E#")
		    .pattern("###")
		    .criterion("has_ender_eye", conditionsFromItem(Items.ENDER_EYE))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.ENDER_EYE)
		    .input(Items.ENDER_PEARL)
		    .input(Items.BLAZE_POWDER)
		    .criterion("has_blaze_powder", conditionsFromItem(Items.BLAZE_POWDER))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.END_STONE_BRICKS, 4)
		    .input('#', Blocks.END_STONE)
		    .pattern("##")
		    .pattern("##")
		    .criterion("has_end_stone", conditionsFromItem(Blocks.END_STONE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Items.END_CRYSTAL)
		    .input('T', Items.GHAST_TEAR)
		    .input('E', Items.ENDER_EYE)
		    .input('G', Blocks.GLASS)
		    .pattern("GGG")
		    .pattern("GEG")
		    .pattern("GTG")
		    .criterion("has_ender_eye", conditionsFromItem(Items.ENDER_EYE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.END_ROD, 4)
		    .input('#', Items.POPPED_CHORUS_FRUIT)
		    .input('/', Items.BLAZE_ROD)
		    .pattern("/")
		    .pattern("#")
		    .criterion("has_chorus_fruit_popped", conditionsFromItem(Items.POPPED_CHORUS_FRUIT))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.BREWING, Items.FERMENTED_SPIDER_EYE)
		    .input(Items.SPIDER_EYE)
		    .input(Blocks.BROWN_MUSHROOM)
		    .input(Items.SUGAR)
		    .criterion("has_spider_eye", conditionsFromItem(Items.SPIDER_EYE))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.FIRE_CHARGE, 3)
		    .input(Items.GUNPOWDER)
		    .input(Items.BLAZE_POWDER)
		    .input(Ingredient.ofItems(Items.COAL, Items.CHARCOAL))
		    .criterion("has_blaze_powder", conditionsFromItem(Items.BLAZE_POWDER))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.FIREWORK_ROCKET, 3)
		    .input(Items.GUNPOWDER)
		    .input(Items.PAPER)
		    .criterion("has_gunpowder", conditionsFromItem(Items.GUNPOWDER))
		    .offerTo(exporter, "firework_rocket_simple");
		createShaped(RecipeCategory.TOOLS, Items.FISHING_ROD)
		    .input('#', Items.STICK)
		    .input('X', Items.STRING)
		    .pattern("  #")
		    .pattern(" #X")
		    .pattern("# X")
		    .criterion("has_string", conditionsFromItem(Items.STRING))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.TOOLS, Items.FLINT_AND_STEEL)
		    .input(Items.IRON_INGOT)
		    .input(Items.FLINT)
		    .criterion("has_flint", conditionsFromItem(Items.FLINT))
		    .criterion("has_obsidian", conditionsFromItem(Blocks.OBSIDIAN))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.FLOWER_POT)
		    .input('#', Items.BRICK)
		    .pattern("# #")
		    .pattern(" # ")
		    .criterion("has_brick", conditionsFromItem(Items.BRICK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.FURNACE)
		    .input('#', ItemTags.STONE_CRAFTING_MATERIALS)
		    .pattern("###")
		    .pattern("# #")
		    .pattern("###")
		    .criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_CRAFTING_MATERIALS))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.TRANSPORTATION, Items.FURNACE_MINECART)
		    .input(Blocks.FURNACE)
		    .input(Items.MINECART)
		    .criterion("has_minecart", conditionsFromItem(Items.MINECART))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BREWING, Items.GLASS_BOTTLE, 3)
		    .input('#', Blocks.GLASS)
		    .pattern("# #")
		    .pattern(" # ")
		    .criterion("has_glass", conditionsFromItem(Blocks.GLASS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.GLASS_PANE, 16)
		    .input('#', Blocks.GLASS)
		    .pattern("###")
		    .pattern("###")
		    .criterion("has_glass", conditionsFromItem(Blocks.GLASS))
		    .offerTo(exporter);
		offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.GLOWSTONE, Items.GLOWSTONE_DUST);
		createShapeless(RecipeCategory.DECORATIONS, Items.GLOW_ITEM_FRAME)
		    .input(Items.ITEM_FRAME)
		    .input(Items.GLOW_INK_SAC)
		    .criterion("has_item_frame", conditionsFromItem(Items.ITEM_FRAME))
		    .criterion("has_glow_ink_sac", conditionsFromItem(Items.GLOW_INK_SAC))
		    .offerTo(exporter);
		createShaped(RecipeCategory.FOOD, Items.GOLDEN_APPLE)
		    .input('#', Items.GOLD_INGOT)
		    .input('X', Items.APPLE)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.GOLDEN_AXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.GOLD_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern("X#")
		    .pattern(" #")
		    .criterion("has_gold_ingot", conditionsFromTag(ItemTags.GOLD_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.GOLDEN_BOOTS)
		    .input('X', Items.GOLD_INGOT)
		    .pattern("X X")
		    .pattern("X X")
		    .criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BREWING, Items.GOLDEN_CARROT)
		    .input('#', Items.GOLD_NUGGET)
		    .input('X', Items.CARROT)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .criterion("has_gold_nugget", conditionsFromItem(Items.GOLD_NUGGET))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.GOLDEN_CHESTPLATE)
		    .input('X', Items.GOLD_INGOT)
		    .pattern("X X")
		    .pattern("XXX")
		    .pattern("XXX")
		    .criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.GOLDEN_HELMET)
		    .input('X', Items.GOLD_INGOT)
		    .pattern("XXX")
		    .pattern("X X")
		    .criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.GOLDEN_HOE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.GOLD_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern(" #")
		    .pattern(" #")
		    .criterion("has_gold_ingot", conditionsFromTag(ItemTags.GOLD_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.GOLDEN_LEGGINGS)
		    .input('X', Items.GOLD_INGOT)
		    .pattern("XXX")
		    .pattern("X X")
		    .pattern("X X")
		    .criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.GOLDEN_PICKAXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.GOLD_TOOL_MATERIALS)
		    .pattern("XXX")
		    .pattern(" # ")
		    .pattern(" # ")
		    .criterion("has_gold_ingot", conditionsFromTag(ItemTags.GOLD_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TRANSPORTATION, Blocks.POWERED_RAIL, 6)
		    .input('R', Items.REDSTONE)
		    .input('#', Items.STICK)
		    .input('X', Items.GOLD_INGOT)
		    .pattern("X X")
		    .pattern("X#X")
		    .pattern("XRX")
		    .criterion("has_rail", conditionsFromItem(Blocks.RAIL))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.GOLDEN_SHOVEL)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.GOLD_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("#")
		    .pattern("#")
		    .criterion("has_gold_ingot", conditionsFromTag(ItemTags.GOLD_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.GOLDEN_SWORD)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.GOLD_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("X")
		    .pattern("#")
		    .criterion("has_gold_ingot", conditionsFromTag(ItemTags.GOLD_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.GOLDEN_SPEAR)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.GOLD_TOOL_MATERIALS)
		    .pattern("  X")
		    .pattern(" # ")
		    .pattern("#  ")
		    .criterion("has_gold_ingot", conditionsFromTag(ItemTags.GOLD_TOOL_MATERIALS))
		    .offerTo(exporter);
		offerReversibleCompactingRecipesWithReverseRecipeGroup(
				RecipeCategory.MISC,
				Items.GOLD_INGOT,
				RecipeCategory.BUILDING_BLOCKS,
				Items.GOLD_BLOCK,
				"gold_ingot_from_gold_block",
				"gold_ingot"
		);
		offerReversibleCompactingRecipesWithCompactingRecipeGroup(
				RecipeCategory.MISC,
				Items.GOLD_NUGGET,
				RecipeCategory.MISC,
				Items.GOLD_INGOT,
				"gold_ingot_from_nuggets",
				"gold_ingot"
		);
		createShapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.GRANITE)
		    .input(Blocks.DIORITE)
		    .input(Items.QUARTZ)
		    .criterion("has_quartz", conditionsFromItem(Items.QUARTZ))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.GRAY_DYE, 2)
		    .input(Items.BLACK_DYE)
		    .input(Items.WHITE_DYE)
		    .group("gray_dye")
		    .criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE))
		    .criterion("has_black_dye", conditionsFromItem(Items.BLACK_DYE))
		    .offerTo(exporter);
		offerCompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.HAY_BLOCK, Items.WHEAT);
		offerPressurePlateRecipe(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.IRON_INGOT);
		createShapeless(RecipeCategory.FOOD, Items.HONEY_BOTTLE, 4)
		    .input(Items.HONEY_BLOCK)
		    .input(Items.GLASS_BOTTLE, 4)
		    .criterion("has_honey_block", conditionsFromItem(Blocks.HONEY_BLOCK))
		    .offerTo(exporter);
		offer2x2CompactingRecipe(RecipeCategory.REDSTONE, Blocks.HONEY_BLOCK, Items.HONEY_BOTTLE);
		offer2x2CompactingRecipe(RecipeCategory.DECORATIONS, Blocks.HONEYCOMB_BLOCK, Items.HONEYCOMB);
		createShaped(RecipeCategory.REDSTONE, Blocks.HOPPER)
		    .input('C', Blocks.CHEST)
		    .input('I', Items.IRON_INGOT)
		    .pattern("I I")
		    .pattern("ICI")
		    .pattern(" I ")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.TRANSPORTATION, Items.HOPPER_MINECART)
		    .input(Blocks.HOPPER)
		    .input(Items.MINECART)
		    .criterion("has_minecart", conditionsFromItem(Items.MINECART))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.IRON_AXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.IRON_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern("X#")
		    .pattern(" #")
		    .criterion("has_iron_ingot", conditionsFromTag(ItemTags.IRON_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.IRON_BARS, 16)
		    .input('#', Items.IRON_INGOT)
		    .pattern("###")
		    .pattern("###")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.COPPER_BARS.unaffected(), 16)
		    .input('#', Items.COPPER_INGOT)
		    .pattern("###")
		    .pattern("###")
		    .criterion("has_copper_ingot", conditionsFromItem(Items.COPPER_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.IRON_BOOTS)
		    .input('X', Items.IRON_INGOT)
		    .pattern("X X")
		    .pattern("X X")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.IRON_CHESTPLATE)
		    .input('X', Items.IRON_INGOT)
		    .pattern("X X")
		    .pattern("XXX")
		    .pattern("XXX")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createDoorRecipe(Blocks.IRON_DOOR, Ingredient.ofItem(Items.IRON_INGOT))
		    .criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.IRON_HELMET)
		    .input('X', Items.IRON_INGOT)
		    .pattern("XXX")
		    .pattern("X X")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.IRON_HOE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.IRON_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern(" #")
		    .pattern(" #")
		    .criterion("has_iron_ingot", conditionsFromTag(ItemTags.IRON_TOOL_MATERIALS))
		    .offerTo(exporter);
		offerReversibleCompactingRecipesWithReverseRecipeGroup(
				RecipeCategory.MISC,
				Items.IRON_INGOT,
				RecipeCategory.BUILDING_BLOCKS,
				Items.IRON_BLOCK,
				"iron_ingot_from_iron_block",
				"iron_ingot"
		);
		offerReversibleCompactingRecipesWithCompactingRecipeGroup(
				RecipeCategory.MISC,
				Items.IRON_NUGGET,
				RecipeCategory.MISC,
				Items.IRON_INGOT,
				"iron_ingot_from_nuggets",
				"iron_ingot"
		);
		createShaped(RecipeCategory.COMBAT, Items.IRON_LEGGINGS)
		    .input('X', Items.IRON_INGOT)
		    .pattern("XXX")
		    .pattern("X X")
		    .pattern("X X")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.IRON_PICKAXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.IRON_TOOL_MATERIALS)
		    .pattern("XXX")
		    .pattern(" # ")
		    .pattern(" # ")
		    .criterion("has_iron_ingot", conditionsFromTag(ItemTags.IRON_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.IRON_SHOVEL)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.IRON_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("#")
		    .pattern("#")
		    .criterion("has_iron_ingot", conditionsFromTag(ItemTags.IRON_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.IRON_SWORD)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.IRON_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("X")
		    .pattern("#")
		    .criterion("has_iron_ingot", conditionsFromTag(ItemTags.IRON_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.IRON_SPEAR)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.IRON_TOOL_MATERIALS)
		    .pattern("  X")
		    .pattern(" # ")
		    .pattern("#  ")
		    .criterion("has_iron_ingot", conditionsFromTag(ItemTags.IRON_TOOL_MATERIALS))
		    .offerTo(exporter);
		offer2x2CompactingRecipe(RecipeCategory.REDSTONE, Blocks.IRON_TRAPDOOR, Items.IRON_INGOT);
		createShaped(RecipeCategory.DECORATIONS, Items.ITEM_FRAME)
		    .input('#', Items.STICK)
		    .input('X', Items.LEATHER)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .criterion("has_leather", conditionsFromItem(Items.LEATHER))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.JUKEBOX)
		    .input('#', ItemTags.PLANKS)
		    .input('X', Items.DIAMOND)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .criterion("has_diamond", conditionsFromItem(Items.DIAMOND))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.LADDER, 3)
		    .input('#', Items.STICK)
		    .pattern("# #")
		    .pattern("###")
		    .pattern("# #")
		    .criterion("has_stick", conditionsFromItem(Items.STICK))
		    .offerTo(exporter);
		offerReversibleCompactingRecipes(
				RecipeCategory.MISC,
				Items.LAPIS_LAZULI,
				RecipeCategory.BUILDING_BLOCKS,
				Items.LAPIS_BLOCK
		);
		createShaped(RecipeCategory.TOOLS, Items.LEAD, 2)
		    .input('~', Items.STRING)
		    .pattern("~~ ")
		    .pattern("~~ ")
		    .pattern("  ~")
		    .criterion("has_string", conditionsFromItem(Items.STRING))
		    .offerTo(exporter);
		offer2x2CompactingRecipe(RecipeCategory.MISC, Items.LEATHER, Items.RABBIT_HIDE);
		createShaped(RecipeCategory.COMBAT, Items.LEATHER_BOOTS)
		    .input('X', Items.LEATHER)
		    .pattern("X X")
		    .pattern("X X")
		    .criterion("has_leather", conditionsFromItem(Items.LEATHER))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.LEATHER_CHESTPLATE)
		    .input('X', Items.LEATHER)
		    .pattern("X X")
		    .pattern("XXX")
		    .pattern("XXX")
		    .criterion("has_leather", conditionsFromItem(Items.LEATHER))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.LEATHER_HELMET)
		    .input('X', Items.LEATHER)
		    .pattern("XXX")
		    .pattern("X X")
		    .criterion("has_leather", conditionsFromItem(Items.LEATHER))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.LEATHER_LEGGINGS)
		    .input('X', Items.LEATHER)
		    .pattern("XXX")
		    .pattern("X X")
		    .pattern("X X")
		    .criterion("has_leather", conditionsFromItem(Items.LEATHER))
		    .offerTo(exporter);
		createShaped(RecipeCategory.MISC, Items.LEATHER_HORSE_ARMOR)
		    .input('X', Items.LEATHER)
		    .pattern("X X")
		    .pattern("XXX")
		    .pattern("X X")
		    .criterion("has_leather", conditionsFromItem(Items.LEATHER))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.SADDLE)
		    .input('X', Items.LEATHER)
		    .input('#', Items.IRON_INGOT)
		    .pattern(" X ")
		    .pattern("X#X")
		    .criterion("has_leather", conditionsFromItem(Items.LEATHER))
		    .offerTo(exporter);
		offerReversibleCompactingRecipesWithCompactingRecipeGroup(
				RecipeCategory.MISC,
				Items.COPPER_NUGGET,
				RecipeCategory.MISC,
				Items.COPPER_INGOT,
				"copper_ingot_from_nuggets",
				"copper_ingot"
		);
		createShaped(RecipeCategory.TOOLS, Items.COPPER_AXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.COPPER_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern("X#")
		    .pattern(" #")
		    .criterion("has_copper_ingot", conditionsFromTag(ItemTags.COPPER_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.COPPER_HOE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.COPPER_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern(" #")
		    .pattern(" #")
		    .criterion("has_copper_ingot", conditionsFromTag(ItemTags.COPPER_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.COPPER_PICKAXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.COPPER_TOOL_MATERIALS)
		    .pattern("XXX")
		    .pattern(" # ")
		    .pattern(" # ")
		    .criterion("has_copper_ingot", conditionsFromTag(ItemTags.COPPER_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.COPPER_SHOVEL)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.COPPER_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("#")
		    .pattern("#")
		    .criterion("has_copper_ingot", conditionsFromTag(ItemTags.COPPER_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.COPPER_SWORD)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.COPPER_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("X")
		    .pattern("#")
		    .criterion("has_copper_ingot", conditionsFromTag(ItemTags.COPPER_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.COPPER_SPEAR)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.COPPER_TOOL_MATERIALS)
		    .pattern("  X")
		    .pattern(" # ")
		    .pattern("#  ")
		    .criterion("has_copper_ingot", conditionsFromTag(ItemTags.COPPER_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.COPPER_BOOTS)
		    .input('X', Items.COPPER_INGOT)
		    .pattern("X X")
		    .pattern("X X")
		    .criterion("has_copper_ingot", conditionsFromItem(Items.COPPER_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.COPPER_CHESTPLATE)
		    .input('X', Items.COPPER_INGOT)
		    .pattern("X X")
		    .pattern("XXX")
		    .pattern("XXX")
		    .criterion("has_copper_ingot", conditionsFromItem(Items.COPPER_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.COPPER_HELMET)
		    .input('X', Items.COPPER_INGOT)
		    .pattern("XXX")
		    .pattern("X X")
		    .criterion("has_copper_ingot", conditionsFromItem(Items.COPPER_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.COPPER_LEGGINGS)
		    .input('X', Items.COPPER_INGOT)
		    .pattern("XXX")
		    .pattern("X X")
		    .pattern("X X")
		    .criterion("has_copper_ingot", conditionsFromItem(Items.COPPER_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.REDSTONE, Blocks.LECTERN)
		    .input('S', ItemTags.WOODEN_SLABS)
		    .input('B', Blocks.BOOKSHELF)
		    .pattern("SSS")
		    .pattern(" B ")
		    .pattern(" S ")
		    .criterion("has_book", conditionsFromItem(Items.BOOK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.REDSTONE, Blocks.LEVER)
		    .input('#', Blocks.COBBLESTONE)
		    .input('X', Items.STICK)
		    .pattern("X")
		    .pattern("#")
		    .criterion("has_cobblestone", conditionsFromItem(Blocks.COBBLESTONE))
		    .offerTo(exporter);
		offerSingleOutputShapelessRecipe(Items.LIGHT_BLUE_DYE, Blocks.BLUE_ORCHID, "light_blue_dye");
		createShapeless(RecipeCategory.MISC, Items.LIGHT_BLUE_DYE, 2)
		    .input(Items.BLUE_DYE)
		    .input(Items.WHITE_DYE)
		    .group("light_blue_dye")
		    .criterion("has_blue_dye", conditionsFromItem(Items.BLUE_DYE))
		    .criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE))
		    .offerTo(exporter, "light_blue_dye_from_blue_white_dye");
		offerSingleOutputShapelessRecipe(Items.LIGHT_GRAY_DYE, Blocks.AZURE_BLUET, "light_gray_dye");
		createShapeless(RecipeCategory.MISC, Items.LIGHT_GRAY_DYE, 2)
		    .input(Items.GRAY_DYE)
		    .input(Items.WHITE_DYE)
		    .group("light_gray_dye")
		    .criterion("has_gray_dye", conditionsFromItem(Items.GRAY_DYE))
		    .criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE))
		    .offerTo(exporter, "light_gray_dye_from_gray_white_dye");
		createShapeless(RecipeCategory.MISC, Items.LIGHT_GRAY_DYE, 3)
		    .input(Items.BLACK_DYE)
		    .input(Items.WHITE_DYE, 2)
		    .group("light_gray_dye")
		    .criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE))
		    .criterion("has_black_dye", conditionsFromItem(Items.BLACK_DYE))
		    .offerTo(exporter, "light_gray_dye_from_black_white_dye");
		offerSingleOutputShapelessRecipe(Items.LIGHT_GRAY_DYE, Blocks.OXEYE_DAISY, "light_gray_dye");
		offerSingleOutputShapelessRecipe(Items.LIGHT_GRAY_DYE, Blocks.WHITE_TULIP, "light_gray_dye");
		offerPressurePlateRecipe(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.GOLD_INGOT);
		createShaped(RecipeCategory.REDSTONE, Blocks.LIGHTNING_ROD)
		    .input('#', Items.COPPER_INGOT)
		    .pattern("#")
		    .pattern("#")
		    .pattern("#")
		    .criterion("has_copper_ingot", conditionsFromItem(Items.COPPER_INGOT))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.LIME_DYE, 2)
		    .input(Items.GREEN_DYE)
		    .input(Items.WHITE_DYE)
		    .criterion("has_green_dye", conditionsFromItem(Items.GREEN_DYE))
		    .criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.JACK_O_LANTERN)
		    .input('A', Blocks.CARVED_PUMPKIN)
		    .input('B', Blocks.TORCH)
		    .pattern("A")
		    .pattern("B")
		    .criterion("has_carved_pumpkin", conditionsFromItem(Blocks.CARVED_PUMPKIN))
		    .offerTo(exporter);
		offerSingleOutputShapelessRecipe(Items.MAGENTA_DYE, Blocks.ALLIUM, "magenta_dye");
		createShapeless(RecipeCategory.MISC, Items.MAGENTA_DYE, 4)
		    .input(Items.BLUE_DYE)
		    .input(Items.RED_DYE, 2)
		    .input(Items.WHITE_DYE)
		    .group("magenta_dye")
		    .criterion("has_blue_dye", conditionsFromItem(Items.BLUE_DYE))
		    .criterion("has_rose_red", conditionsFromItem(Items.RED_DYE))
		    .criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE))
		    .offerTo(exporter, "magenta_dye_from_blue_red_white_dye");
		createShapeless(RecipeCategory.MISC, Items.MAGENTA_DYE, 3)
		    .input(Items.BLUE_DYE)
		    .input(Items.RED_DYE)
		    .input(Items.PINK_DYE)
		    .group("magenta_dye")
		    .criterion("has_pink_dye", conditionsFromItem(Items.PINK_DYE))
		    .criterion("has_blue_dye", conditionsFromItem(Items.BLUE_DYE))
		    .criterion("has_red_dye", conditionsFromItem(Items.RED_DYE))
		    .offerTo(exporter, "magenta_dye_from_blue_red_pink");
		offerShapelessRecipe(Items.MAGENTA_DYE, Blocks.LILAC, "magenta_dye", 2);
		createShapeless(RecipeCategory.MISC, Items.MAGENTA_DYE, 2)
		    .input(Items.PURPLE_DYE)
		    .input(Items.PINK_DYE)
		    .group("magenta_dye")
		    .criterion("has_pink_dye", conditionsFromItem(Items.PINK_DYE))
		    .criterion("has_purple_dye", conditionsFromItem(Items.PURPLE_DYE))
		    .offerTo(exporter, "magenta_dye_from_purple_and_pink");
		offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.MAGMA_BLOCK, Items.MAGMA_CREAM);
		createShapeless(RecipeCategory.BREWING, Items.MAGMA_CREAM)
		    .input(Items.BLAZE_POWDER)
		    .input(Items.SLIME_BALL)
		    .criterion("has_blaze_powder", conditionsFromItem(Items.BLAZE_POWDER))
		    .offerTo(exporter);
		createShaped(RecipeCategory.MISC, Items.MAP)
		    .input('#', Items.PAPER)
		    .input('X', Items.COMPASS)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .criterion("has_compass", conditionsFromItem(Items.COMPASS))
		    .offerTo(exporter);
		offerCompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.MELON, Items.MELON_SLICE, "has_melon");
		createShapeless(RecipeCategory.MISC, Items.MELON_SEEDS)
		    .input(Items.MELON_SLICE)
		    .criterion("has_melon", conditionsFromItem(Items.MELON_SLICE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TRANSPORTATION, Items.MINECART)
		    .input('#', Items.IRON_INGOT)
		    .pattern("# #")
		    .pattern("###")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.MOSSY_COBBLESTONE)
		    .input(Blocks.COBBLESTONE)
		    .input(Blocks.VINE)
		    .group("mossy_cobblestone")
		    .criterion("has_vine", conditionsFromItem(Blocks.VINE))
		    .offerTo(exporter, convertBetween(Blocks.MOSSY_COBBLESTONE, Blocks.VINE));
		createShapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.MOSSY_STONE_BRICKS)
		    .input(Blocks.STONE_BRICKS)
		    .input(Blocks.VINE)
		    .group("mossy_stone_bricks")
		    .criterion("has_vine", conditionsFromItem(Blocks.VINE))
		    .offerTo(exporter, convertBetween(Blocks.MOSSY_STONE_BRICKS, Blocks.VINE));
		createShapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.MOSSY_COBBLESTONE)
		    .input(Blocks.COBBLESTONE)
		    .input(Blocks.MOSS_BLOCK)
		    .group("mossy_cobblestone")
		    .criterion("has_moss_block", conditionsFromItem(Blocks.MOSS_BLOCK))
		    .offerTo(exporter, convertBetween(Blocks.MOSSY_COBBLESTONE, Blocks.MOSS_BLOCK));
		createShapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.MOSSY_STONE_BRICKS)
		    .input(Blocks.STONE_BRICKS)
		    .input(Blocks.MOSS_BLOCK)
		    .group("mossy_stone_bricks")
		    .criterion("has_moss_block", conditionsFromItem(Blocks.MOSS_BLOCK))
		    .offerTo(exporter, convertBetween(Blocks.MOSSY_STONE_BRICKS, Blocks.MOSS_BLOCK));
		createShapeless(RecipeCategory.FOOD, Items.MUSHROOM_STEW)
		    .input(Blocks.BROWN_MUSHROOM)
		    .input(Blocks.RED_MUSHROOM)
		    .input(Items.BOWL)
		    .criterion("has_mushroom_stew", conditionsFromItem(Items.MUSHROOM_STEW))
		    .criterion("has_bowl", conditionsFromItem(Items.BOWL))
		    .criterion("has_brown_mushroom", conditionsFromItem(Blocks.BROWN_MUSHROOM))
		    .criterion("has_red_mushroom", conditionsFromItem(Blocks.RED_MUSHROOM))
		    .offerTo(exporter);
		Registries.ITEM.stream().forEach(item -> {
			SuspiciousStewIngredient suspiciousStewIngredient = SuspiciousStewIngredient.of(item);
			if (suspiciousStewIngredient != null) {
				offerSuspiciousStewRecipe(item, suspiciousStewIngredient);
			}
		});
		offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.NETHER_BRICKS, Items.NETHER_BRICK);
		offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.RESIN_BRICKS, Items.RESIN_BRICK);
		offerReversibleCompactingRecipes(
				RecipeCategory.MISC,
				Items.RESIN_CLUMP,
				RecipeCategory.BUILDING_BLOCKS,
				Items.RESIN_BLOCK
		);
		createShaped(RecipeCategory.MISC, Blocks.CREAKING_HEART)
		    .input('R', Items.RESIN_BLOCK)
		    .input('L', Blocks.PALE_OAK_LOG)
		    .pattern(" L ")
		    .pattern(" R ")
		    .pattern(" L ")
		    .criterion("has_resin_block", conditionsFromItem(Items.RESIN_BLOCK))
		    .offerTo(exporter);
		offerCompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.NETHER_WART_BLOCK, Items.NETHER_WART);
		createShaped(RecipeCategory.REDSTONE, Blocks.NOTE_BLOCK)
		    .input('#', ItemTags.PLANKS)
		    .input('X', Items.REDSTONE)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .criterion("has_redstone", conditionsFromItem(Items.REDSTONE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.REDSTONE, Blocks.OBSERVER)
		    .input('Q', Items.QUARTZ)
		    .input('R', Items.REDSTONE)
		    .input('#', Blocks.COBBLESTONE)
		    .pattern("###")
		    .pattern("RRQ")
		    .pattern("###")
		    .criterion("has_quartz", conditionsFromItem(Items.QUARTZ))
		    .offerTo(exporter);
		offerSingleOutputShapelessRecipe(Items.ORANGE_DYE, Blocks.ORANGE_TULIP, "orange_dye");
		createShapeless(RecipeCategory.MISC, Items.ORANGE_DYE, 2)
		    .input(Items.RED_DYE)
		    .input(Items.YELLOW_DYE)
		    .group("orange_dye")
		    .criterion("has_red_dye", conditionsFromItem(Items.RED_DYE))
		    .criterion("has_yellow_dye", conditionsFromItem(Items.YELLOW_DYE))
		    .offerTo(exporter, "orange_dye_from_red_yellow");
		createShaped(RecipeCategory.DECORATIONS, Items.PAINTING)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.WOOL)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .criterion("has_wool", conditionsFromTag(ItemTags.WOOL))
		    .offerTo(exporter);
		createShaped(RecipeCategory.MISC, Items.PAPER, 3)
		    .input('#', Blocks.SUGAR_CANE)
		    .pattern("###")
		    .criterion("has_reeds", conditionsFromItem(Blocks.SUGAR_CANE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.QUARTZ_PILLAR, 2)
		    .input('#', Blocks.QUARTZ_BLOCK)
		    .pattern("#")
		    .pattern("#")
		    .criterion("has_chiseled_quartz_block", conditionsFromItem(Blocks.CHISELED_QUARTZ_BLOCK))
		    .criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK))
		    .criterion("has_quartz_pillar", conditionsFromItem(Blocks.QUARTZ_PILLAR))
		    .offerTo(exporter);
		offerCompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.PACKED_ICE, Blocks.ICE);
		offerShapelessRecipe(Items.PINK_DYE, Blocks.PEONY, "pink_dye", 2);
		offerSingleOutputShapelessRecipe(Items.PINK_DYE, Blocks.PINK_TULIP, "pink_dye");
		offerSingleOutputShapelessRecipe(Items.PINK_DYE, Blocks.CACTUS_FLOWER, "pink_dye");
		createShapeless(RecipeCategory.MISC, Items.PINK_DYE, 2)
		    .input(Items.RED_DYE)
		    .input(Items.WHITE_DYE)
		    .group("pink_dye")
		    .criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE))
		    .criterion("has_red_dye", conditionsFromItem(Items.RED_DYE))
		    .offerTo(exporter, "pink_dye_from_red_white_dye");
		createShaped(RecipeCategory.REDSTONE, Blocks.PISTON)
		    .input('R', Items.REDSTONE)
		    .input('#', Blocks.COBBLESTONE)
		    .input('T', ItemTags.PLANKS)
		    .input('X', Items.IRON_INGOT)
		    .pattern("TTT")
		    .pattern("#X#")
		    .pattern("#R#")
		    .criterion("has_redstone", conditionsFromItem(Items.REDSTONE))
		    .offerTo(exporter);
		offerPolishedStoneRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_BASALT, Blocks.BASALT);
		offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.PRISMARINE, Items.PRISMARINE_SHARD);
		offerCompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.PRISMARINE_BRICKS, Items.PRISMARINE_SHARD);
		createShapeless(RecipeCategory.FOOD, Items.PUMPKIN_PIE)
		    .input(Blocks.PUMPKIN)
		    .input(Items.SUGAR)
		    .input(ItemTags.EGGS)
		    .criterion("has_carved_pumpkin", conditionsFromItem(Blocks.CARVED_PUMPKIN))
		    .criterion("has_pumpkin", conditionsFromItem(Blocks.PUMPKIN))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.PUMPKIN_SEEDS, 4)
		    .input(Blocks.PUMPKIN)
		    .criterion("has_pumpkin", conditionsFromItem(Blocks.PUMPKIN))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.PURPLE_DYE, 2)
		    .input(Items.BLUE_DYE)
		    .input(Items.RED_DYE)
		    .criterion("has_blue_dye", conditionsFromItem(Items.BLUE_DYE))
		    .criterion("has_red_dye", conditionsFromItem(Items.RED_DYE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.SHULKER_BOX)
		    .input('#', Blocks.CHEST)
		    .input('-', Items.SHULKER_SHELL)
		    .pattern("-")
		    .pattern("#")
		    .pattern("-")
		    .criterion("has_shulker_shell", conditionsFromItem(Items.SHULKER_SHELL))
		    .offerTo(exporter);
		generateDyedShulkerBoxes();
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.PURPUR_BLOCK, 4)
		    .input('F', Items.POPPED_CHORUS_FRUIT)
		    .pattern("FF")
		    .pattern("FF")
		    .criterion("has_chorus_fruit_popped", conditionsFromItem(Items.POPPED_CHORUS_FRUIT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.PURPUR_PILLAR)
		    .input('#', Blocks.PURPUR_SLAB)
		    .pattern("#")
		    .pattern("#")
		    .criterion("has_purpur_block", conditionsFromItem(Blocks.PURPUR_BLOCK))
		    .offerTo(exporter);
		this
				.createSlabRecipe(
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.PURPUR_SLAB,
						Ingredient.ofItems(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR)
				)
				.criterion("has_purpur_block", conditionsFromItem(Blocks.PURPUR_BLOCK))
				.offerTo(exporter);
		createStairsRecipe(Blocks.PURPUR_STAIRS, Ingredient.ofItems(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR))
		    .criterion("has_purpur_block", conditionsFromItem(Blocks.PURPUR_BLOCK))
		    .offerTo(exporter);
		offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.QUARTZ_BLOCK, Items.QUARTZ);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.QUARTZ_BRICKS, 4)
		    .input('#', Blocks.QUARTZ_BLOCK)
		    .pattern("##")
		    .pattern("##")
		    .criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK))
		    .offerTo(exporter);
		createSlabRecipe(
				    RecipeCategory.BUILDING_BLOCKS,
				    Blocks.QUARTZ_SLAB,
				    Ingredient.ofItems(Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR)
		    )
		    .criterion("has_chiseled_quartz_block", conditionsFromItem(Blocks.CHISELED_QUARTZ_BLOCK))
		    .criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK))
		    .criterion("has_quartz_pillar", conditionsFromItem(Blocks.QUARTZ_PILLAR))
		    .offerTo(exporter);
		this
				.createStairsRecipe(
						Blocks.QUARTZ_STAIRS,
						Ingredient.ofItems(Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR)
				)
				.criterion("has_chiseled_quartz_block", conditionsFromItem(Blocks.CHISELED_QUARTZ_BLOCK))
				.criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK))
				.criterion("has_quartz_pillar", conditionsFromItem(Blocks.QUARTZ_PILLAR))
				.offerTo(exporter);
		createShapeless(RecipeCategory.FOOD, Items.RABBIT_STEW)
		    .input(Items.BAKED_POTATO)
		    .input(Items.COOKED_RABBIT)
		    .input(Items.BOWL)
		    .input(Items.CARROT)
		    .input(Blocks.BROWN_MUSHROOM)
		    .group("rabbit_stew")
		    .criterion("has_cooked_rabbit", conditionsFromItem(Items.COOKED_RABBIT))
		    .offerTo(exporter, convertBetween(Items.RABBIT_STEW, Items.BROWN_MUSHROOM));
		createShapeless(RecipeCategory.FOOD, Items.RABBIT_STEW)
		    .input(Items.BAKED_POTATO)
		    .input(Items.COOKED_RABBIT)
		    .input(Items.BOWL)
		    .input(Items.CARROT)
		    .input(Blocks.RED_MUSHROOM)
		    .group("rabbit_stew")
		    .criterion("has_cooked_rabbit", conditionsFromItem(Items.COOKED_RABBIT))
		    .offerTo(exporter, convertBetween(Items.RABBIT_STEW, Items.RED_MUSHROOM));
		createShaped(RecipeCategory.TRANSPORTATION, Blocks.RAIL, 16)
		    .input('#', Items.STICK)
		    .input('X', Items.IRON_INGOT)
		    .pattern("X X")
		    .pattern("X#X")
		    .pattern("X X")
		    .criterion("has_minecart", conditionsFromItem(Items.MINECART))
		    .offerTo(exporter);
		offerReversibleCompactingRecipes(
				RecipeCategory.REDSTONE,
				Items.REDSTONE,
				RecipeCategory.REDSTONE,
				Items.REDSTONE_BLOCK
		);
		createShaped(RecipeCategory.REDSTONE, Blocks.REDSTONE_LAMP)
		    .input('R', Items.REDSTONE)
		    .input('G', Blocks.GLOWSTONE)
		    .pattern(" R ")
		    .pattern("RGR")
		    .pattern(" R ")
		    .criterion("has_glowstone", conditionsFromItem(Blocks.GLOWSTONE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.REDSTONE, Blocks.REDSTONE_TORCH)
		    .input('#', Items.STICK)
		    .input('X', Items.REDSTONE)
		    .pattern("X")
		    .pattern("#")
		    .criterion("has_redstone", conditionsFromItem(Items.REDSTONE))
		    .offerTo(exporter);
		offerSingleOutputShapelessRecipe(Items.RED_DYE, Items.BEETROOT, "red_dye");
		offerSingleOutputShapelessRecipe(Items.RED_DYE, Blocks.POPPY, "red_dye");
		offerShapelessRecipe(Items.RED_DYE, Blocks.ROSE_BUSH, "red_dye", 2);
		offerSingleOutputShapelessRecipe(Items.ORANGE_DYE, Blocks.OPEN_EYEBLOSSOM, "orange_dye");
		offerSingleOutputShapelessRecipe(Items.GRAY_DYE, Blocks.CLOSED_EYEBLOSSOM, "gray_dye");
		createShapeless(RecipeCategory.MISC, Items.RED_DYE)
		    .input(Blocks.RED_TULIP)
		    .group("red_dye")
		    .criterion("has_red_flower", conditionsFromItem(Blocks.RED_TULIP))
		    .offerTo(exporter, "red_dye_from_tulip");
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.RED_NETHER_BRICKS)
		    .input('W', Items.NETHER_WART)
		    .input('N', Items.NETHER_BRICK)
		    .pattern("NW")
		    .pattern("WN")
		    .criterion("has_nether_wart", conditionsFromItem(Items.NETHER_WART))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.RED_SANDSTONE)
		    .input('#', Blocks.RED_SAND)
		    .pattern("##")
		    .pattern("##")
		    .criterion("has_sand", conditionsFromItem(Blocks.RED_SAND))
		    .offerTo(exporter);
		this
				.createSlabRecipe(
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.RED_SANDSTONE_SLAB,
						Ingredient.ofItems(Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE)
				)
				.criterion("has_red_sandstone", conditionsFromItem(Blocks.RED_SANDSTONE))
				.criterion("has_chiseled_red_sandstone", conditionsFromItem(Blocks.CHISELED_RED_SANDSTONE))
				.offerTo(exporter);
		this
				.createStairsRecipe(
						Blocks.RED_SANDSTONE_STAIRS,
						Ingredient.ofItems(
								Blocks.RED_SANDSTONE,
								Blocks.CHISELED_RED_SANDSTONE,
								Blocks.CUT_RED_SANDSTONE
						)
				)
				.criterion("has_red_sandstone", conditionsFromItem(Blocks.RED_SANDSTONE))
				.criterion("has_chiseled_red_sandstone", conditionsFromItem(Blocks.CHISELED_RED_SANDSTONE))
				.criterion("has_cut_red_sandstone", conditionsFromItem(Blocks.CUT_RED_SANDSTONE))
				.offerTo(exporter);
		createShaped(RecipeCategory.REDSTONE, Blocks.REPEATER)
		    .input('#', Blocks.REDSTONE_TORCH)
		    .input('X', Items.REDSTONE)
		    .input('I', Blocks.STONE)
		    .pattern("#X#")
		    .pattern("III")
		    .criterion("has_redstone_torch", conditionsFromItem(Blocks.REDSTONE_TORCH))
		    .offerTo(exporter);
		offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.SANDSTONE, Blocks.SAND);
		this
				.createSlabRecipe(
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.SANDSTONE_SLAB,
						Ingredient.ofItems(Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE)
				)
				.criterion("has_sandstone", conditionsFromItem(Blocks.SANDSTONE))
				.criterion("has_chiseled_sandstone", conditionsFromItem(Blocks.CHISELED_SANDSTONE))
				.offerTo(exporter);
		this
				.createStairsRecipe(
						Blocks.SANDSTONE_STAIRS,
						Ingredient.ofItems(Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE)
				)
				.criterion("has_sandstone", conditionsFromItem(Blocks.SANDSTONE))
				.criterion("has_chiseled_sandstone", conditionsFromItem(Blocks.CHISELED_SANDSTONE))
				.criterion("has_cut_sandstone", conditionsFromItem(Blocks.CUT_SANDSTONE))
				.offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.SEA_LANTERN)
		    .input('S', Items.PRISMARINE_SHARD)
		    .input('C', Items.PRISMARINE_CRYSTALS)
		    .pattern("SCS")
		    .pattern("CCC")
		    .pattern("SCS")
		    .criterion("has_prismarine_crystals", conditionsFromItem(Items.PRISMARINE_CRYSTALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.SHEARS)
		    .input('#', Items.IRON_INGOT)
		    .pattern(" #")
		    .pattern("# ")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.SHIELD)
		    .input('W', ItemTags.WOODEN_TOOL_MATERIALS)
		    .input('o', Items.IRON_INGOT)
		    .pattern("WoW")
		    .pattern("WWW")
		    .pattern(" W ")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		offerReversibleCompactingRecipes(
				RecipeCategory.MISC,
				Items.SLIME_BALL,
				RecipeCategory.REDSTONE,
				Items.SLIME_BLOCK
		);
		offerCutCopperRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_RED_SANDSTONE, Blocks.RED_SANDSTONE);
		offerCutCopperRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE);
		offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.SNOW_BLOCK, Items.SNOWBALL);
		createShaped(RecipeCategory.DECORATIONS, Blocks.SNOW, 6)
		    .input('#', Blocks.SNOW_BLOCK)
		    .pattern("###")
		    .criterion("has_snowball", conditionsFromItem(Items.SNOWBALL))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.SOUL_CAMPFIRE)
		    .input('L', ItemTags.LOGS)
		    .input('S', Items.STICK)
		    .input('#', ItemTags.SOUL_FIRE_BASE_BLOCKS)
		    .pattern(" S ")
		    .pattern("S#S")
		    .pattern("LLL")
		    .criterion("has_soul_sand", conditionsFromTag(ItemTags.SOUL_FIRE_BASE_BLOCKS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BREWING, Items.GLISTERING_MELON_SLICE)
		    .input('#', Items.GOLD_NUGGET)
		    .input('X', Items.MELON_SLICE)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .criterion("has_melon", conditionsFromItem(Items.MELON_SLICE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.SPECTRAL_ARROW, 2)
		    .input('#', Items.GLOWSTONE_DUST)
		    .input('X', Items.ARROW)
		    .pattern(" # ")
		    .pattern("#X#")
		    .pattern(" # ")
		    .criterion("has_glowstone_dust", conditionsFromItem(Items.GLOWSTONE_DUST))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.SPYGLASS)
		    .input('#', Items.AMETHYST_SHARD)
		    .input('X', Items.COPPER_INGOT)
		    .pattern(" # ")
		    .pattern(" X ")
		    .pattern(" X ")
		    .criterion("has_amethyst_shard", conditionsFromItem(Items.AMETHYST_SHARD))
		    .offerTo(exporter);
		createShaped(RecipeCategory.MISC, Items.STICK, 4)
		    .input('#', ItemTags.PLANKS)
		    .pattern("#")
		    .pattern("#")
		    .group("sticks")
		    .criterion("has_planks", conditionsFromTag(ItemTags.PLANKS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.MISC, Items.STICK, 1)
		    .input('#', Blocks.BAMBOO)
		    .pattern("#")
		    .pattern("#")
		    .group("sticks")
		    .criterion("has_bamboo", conditionsFromItem(Blocks.BAMBOO))
		    .offerTo(exporter, "stick_from_bamboo_item");
		createShaped(RecipeCategory.REDSTONE, Blocks.STICKY_PISTON)
		    .input('P', Blocks.PISTON)
		    .input('S', Items.SLIME_BALL)
		    .pattern("S")
		    .pattern("P")
		    .criterion("has_slime_ball", conditionsFromItem(Items.SLIME_BALL))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.STONE_BRICKS, 4)
		    .input('#', Blocks.STONE)
		    .pattern("##")
		    .pattern("##")
		    .criterion("has_stone", conditionsFromItem(Blocks.STONE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.STONE_AXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.STONE_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern("X#")
		    .pattern(" #")
		    .criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_TOOL_MATERIALS))
		    .offerTo(exporter);
		this
				.createSlabRecipe(
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.STONE_BRICK_SLAB,
						Ingredient.ofItem(Blocks.STONE_BRICKS)
				)
				.criterion("has_stone_bricks", conditionsFromTag(ItemTags.STONE_BRICKS))
				.offerTo(exporter);
		createStairsRecipe(Blocks.STONE_BRICK_STAIRS, Ingredient.ofItem(Blocks.STONE_BRICKS))
		    .criterion("has_stone_bricks", conditionsFromTag(ItemTags.STONE_BRICKS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.STONE_HOE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.STONE_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern(" #")
		    .pattern(" #")
		    .criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.STONE_PICKAXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.STONE_TOOL_MATERIALS)
		    .pattern("XXX")
		    .pattern(" # ")
		    .pattern(" # ")
		    .criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.STONE_SHOVEL)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.STONE_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("#")
		    .pattern("#")
		    .criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_TOOL_MATERIALS))
		    .offerTo(exporter);
		offerSlabRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_STONE_SLAB, Blocks.SMOOTH_STONE);
		createShaped(RecipeCategory.COMBAT, Items.STONE_SWORD)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.STONE_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("X")
		    .pattern("#")
		    .criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.STONE_SPEAR)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.STONE_TOOL_MATERIALS)
		    .pattern("  X")
		    .pattern(" # ")
		    .pattern("#  ")
		    .criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_TOOL_MATERIALS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.WHITE_WOOL)
		    .input('#', Items.STRING)
		    .pattern("##")
		    .pattern("##")
		    .criterion("has_string", conditionsFromItem(Items.STRING))
		    .offerTo(exporter, convertBetween(Blocks.WHITE_WOOL, Items.STRING));
		offerSingleOutputShapelessRecipe(Items.SUGAR, Blocks.SUGAR_CANE, "sugar");
		createShapeless(RecipeCategory.MISC, Items.SUGAR, 3)
		    .input(Items.HONEY_BOTTLE)
		    .group("sugar")
		    .criterion("has_honey_bottle", conditionsFromItem(Items.HONEY_BOTTLE))
		    .offerTo(exporter, convertBetween(Items.SUGAR, Items.HONEY_BOTTLE));
		createShaped(RecipeCategory.REDSTONE, Blocks.TARGET)
		    .input('H', Items.HAY_BLOCK)
		    .input('R', Items.REDSTONE)
		    .pattern(" R ")
		    .pattern("RHR")
		    .pattern(" R ")
		    .criterion("has_redstone", conditionsFromItem(Items.REDSTONE))
		    .criterion("has_hay_block", conditionsFromItem(Blocks.HAY_BLOCK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.REDSTONE, Blocks.TNT)
		    .input('#', Ingredient.ofItems(Blocks.SAND, Blocks.RED_SAND))
		    .input('X', Items.GUNPOWDER)
		    .pattern("X#X")
		    .pattern("#X#")
		    .pattern("X#X")
		    .criterion("has_gunpowder", conditionsFromItem(Items.GUNPOWDER))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.TRANSPORTATION, Items.TNT_MINECART)
		    .input(Blocks.TNT)
		    .input(Items.MINECART)
		    .criterion("has_minecart", conditionsFromItem(Items.MINECART))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.TORCH, 4)
		    .input('#', Items.STICK)
		    .input('X', Ingredient.ofItems(Items.COAL, Items.CHARCOAL))
		    .pattern("X")
		    .pattern("#")
		    .criterion("has_stone_pickaxe", conditionsFromItem(Items.STONE_PICKAXE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.SOUL_TORCH, 4)
		    .input('X', Ingredient.ofItems(Items.COAL, Items.CHARCOAL))
		    .input('#', Items.STICK)
		    .input('S', ItemTags.SOUL_FIRE_BASE_BLOCKS)
		    .pattern("X")
		    .pattern("#")
		    .pattern("S")
		    .criterion("has_soul_sand", conditionsFromTag(ItemTags.SOUL_FIRE_BASE_BLOCKS))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.COPPER_TORCH, 4)
		    .input('X', Ingredient.ofItems(Items.COAL, Items.CHARCOAL))
		    .input('#', Items.STICK)
		    .input('C', Items.COPPER_NUGGET)
		    .pattern("C")
		    .pattern("X")
		    .pattern("#")
		    .criterion("has_copper_nugget", conditionsFromItem(Items.COPPER_NUGGET))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.LANTERN)
		    .input('#', Items.TORCH)
		    .input('X', Items.IRON_NUGGET)
		    .pattern("XXX")
		    .pattern("X#X")
		    .pattern("XXX")
		    .criterion("has_iron_nugget", conditionsFromItem(Items.IRON_NUGGET))
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.SOUL_LANTERN)
		    .input('#', Items.SOUL_TORCH)
		    .input('X', Items.IRON_NUGGET)
		    .pattern("XXX")
		    .pattern("X#X")
		    .pattern("XXX")
		    .criterion("has_soul_torch", conditionsFromItem(Items.SOUL_TORCH))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.COPPER_LANTERNS.unaffected())
		    .input('#', Items.COPPER_TORCH)
		    .input('X', Items.COPPER_NUGGET)
		    .pattern("XXX")
		    .pattern("X#X")
		    .pattern("XXX")
		    .criterion("has_copper_torch", conditionsFromItem(Items.COPPER_TORCH))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.REDSTONE, Blocks.TRAPPED_CHEST)
		    .input(Blocks.CHEST)
		    .input(Blocks.TRIPWIRE_HOOK)
		    .criterion("has_tripwire_hook", conditionsFromItem(Blocks.TRIPWIRE_HOOK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.REDSTONE, Blocks.TRIPWIRE_HOOK, 2)
		    .input('#', ItemTags.PLANKS)
		    .input('S', Items.STICK)
		    .input('I', Items.IRON_INGOT)
		    .pattern("I")
		    .pattern("S")
		    .pattern("#")
		    .criterion("has_string", conditionsFromItem(Items.STRING))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.TURTLE_HELMET)
		    .input('X', Items.TURTLE_SCUTE)
		    .pattern("XXX")
		    .pattern("X X")
		    .criterion("has_turtle_scute", conditionsFromItem(Items.TURTLE_SCUTE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.WOLF_ARMOR)
		    .input('X', Items.ARMADILLO_SCUTE)
		    .pattern("X  ")
		    .pattern("XXX")
		    .pattern("X X")
		    .criterion("has_armadillo_scute", conditionsFromItem(Items.ARMADILLO_SCUTE))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.WHEAT, 9)
		    .input(Blocks.HAY_BLOCK)
		    .criterion("has_hay_block", conditionsFromItem(Blocks.HAY_BLOCK))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.WHITE_DYE)
		    .input(Items.BONE_MEAL)
		    .group("white_dye")
		    .criterion("has_bone_meal", conditionsFromItem(Items.BONE_MEAL))
		    .offerTo(exporter);
		offerSingleOutputShapelessRecipe(Items.WHITE_DYE, Blocks.LILY_OF_THE_VALLEY, "white_dye");
		createShaped(RecipeCategory.TOOLS, Items.WOODEN_AXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.WOODEN_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern("X#")
		    .pattern(" #")
		    .criterion("has_stick", conditionsFromItem(Items.STICK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.WOODEN_HOE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.WOODEN_TOOL_MATERIALS)
		    .pattern("XX")
		    .pattern(" #")
		    .pattern(" #")
		    .criterion("has_stick", conditionsFromItem(Items.STICK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.WOODEN_PICKAXE)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.WOODEN_TOOL_MATERIALS)
		    .pattern("XXX")
		    .pattern(" # ")
		    .pattern(" # ")
		    .criterion("has_stick", conditionsFromItem(Items.STICK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.TOOLS, Items.WOODEN_SHOVEL)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.WOODEN_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("#")
		    .pattern("#")
		    .criterion("has_stick", conditionsFromItem(Items.STICK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.WOODEN_SWORD)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.WOODEN_TOOL_MATERIALS)
		    .pattern("X")
		    .pattern("X")
		    .pattern("#")
		    .criterion("has_stick", conditionsFromItem(Items.STICK))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.WOODEN_SPEAR)
		    .input('#', Items.STICK)
		    .input('X', ItemTags.WOODEN_TOOL_MATERIALS)
		    .pattern("  X")
		    .pattern(" # ")
		    .pattern("#  ")
		    .criterion("has_stick", conditionsFromItem(Items.STICK))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.WRITABLE_BOOK)
		    .input(Items.BOOK)
		    .input(Items.INK_SAC)
		    .input(Items.FEATHER)
		    .criterion("has_book", conditionsFromItem(Items.BOOK))
		    .offerTo(exporter);
		offerSingleOutputShapelessRecipe(Items.YELLOW_DYE, Blocks.DANDELION, "yellow_dye");
		offerShapelessRecipe(Items.YELLOW_DYE, Blocks.SUNFLOWER, "yellow_dye", 2);
		offerSingleOutputShapelessRecipe(Items.YELLOW_DYE, Blocks.WILDFLOWERS, "yellow_dye");
		offerReversibleCompactingRecipes(
				RecipeCategory.FOOD,
				Items.DRIED_KELP,
				RecipeCategory.BUILDING_BLOCKS,
				Items.DRIED_KELP_BLOCK
		);
		createShaped(RecipeCategory.MISC, Blocks.CONDUIT)
		    .input('#', Items.NAUTILUS_SHELL)
		    .input('X', Items.HEART_OF_THE_SEA)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .criterion("has_nautilus_core", conditionsFromItem(Items.HEART_OF_THE_SEA))
		    .criterion("has_nautilus_shell", conditionsFromItem(Items.NAUTILUS_SHELL))
		    .offerTo(exporter);
		offerWallRecipe(RecipeCategory.DECORATIONS, Blocks.RED_SANDSTONE_WALL, Blocks.RED_SANDSTONE);
		offerWallRecipe(RecipeCategory.DECORATIONS, Blocks.STONE_BRICK_WALL, Blocks.STONE_BRICKS);
		offerWallRecipe(RecipeCategory.DECORATIONS, Blocks.SANDSTONE_WALL, Blocks.SANDSTONE);
		createShapeless(RecipeCategory.MISC, Items.FIELD_MASONED_BANNER_PATTERN)
		    .input(Items.PAPER)
		    .input(Blocks.BRICKS)
		    .criterion("has_bricks", conditionsFromItem(Blocks.BRICKS))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.BORDURE_INDENTED_BANNER_PATTERN)
		    .input(Items.PAPER)
		    .input(Blocks.VINE)
		    .criterion("has_vines", conditionsFromItem(Blocks.VINE))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.CREEPER_BANNER_PATTERN)
		    .input(Items.PAPER)
		    .input(Items.CREEPER_HEAD)
		    .criterion("has_creeper_head", conditionsFromItem(Items.CREEPER_HEAD))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.SKULL_BANNER_PATTERN)
		    .input(Items.PAPER)
		    .input(Items.WITHER_SKELETON_SKULL)
		    .criterion("has_wither_skeleton_skull", conditionsFromItem(Items.WITHER_SKELETON_SKULL))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.FLOWER_BANNER_PATTERN)
		    .input(Items.PAPER)
		    .input(Blocks.OXEYE_DAISY)
		    .criterion("has_oxeye_daisy", conditionsFromItem(Blocks.OXEYE_DAISY))
		    .offerTo(exporter);
		createShapeless(RecipeCategory.MISC, Items.MOJANG_BANNER_PATTERN)
		    .input(Items.PAPER)
		    .input(Items.ENCHANTED_GOLDEN_APPLE)
		    .criterion("has_enchanted_golden_apple", conditionsFromItem(Items.ENCHANTED_GOLDEN_APPLE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.SCAFFOLDING, 6)
		    .input('~', Items.STRING)
		    .input('I', Blocks.BAMBOO)
		    .pattern("I~I")
		    .pattern("I I")
		    .pattern("I I")
		    .criterion("has_bamboo", conditionsFromItem(Blocks.BAMBOO))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.GRINDSTONE)
		    .input('I', Items.STICK)
		    .input('-', Blocks.STONE_SLAB)
		    .input('#', ItemTags.PLANKS)
		    .pattern("I-I")
		    .pattern("# #")
		    .criterion("has_stone_slab", conditionsFromItem(Blocks.STONE_SLAB))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.BLAST_FURNACE)
		    .input('#', Blocks.SMOOTH_STONE)
		    .input('X', Blocks.FURNACE)
		    .input('I', Items.IRON_INGOT)
		    .pattern("III")
		    .pattern("IXI")
		    .pattern("###")
		    .criterion("has_smooth_stone", conditionsFromItem(Blocks.SMOOTH_STONE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.SMOKER)
		    .input('#', ItemTags.LOGS)
		    .input('X', Blocks.FURNACE)
		    .pattern(" # ")
		    .pattern("#X#")
		    .pattern(" # ")
		    .criterion("has_furnace", conditionsFromItem(Blocks.FURNACE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.CARTOGRAPHY_TABLE)
		    .input('#', ItemTags.PLANKS)
		    .input('@', Items.PAPER)
		    .pattern("@@")
		    .pattern("##")
		    .pattern("##")
		    .criterion("has_paper", conditionsFromItem(Items.PAPER))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.SMITHING_TABLE)
		    .input('#', ItemTags.PLANKS)
		    .input('@', Items.IRON_INGOT)
		    .pattern("@@")
		    .pattern("##")
		    .pattern("##")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.FLETCHING_TABLE)
		    .input('#', ItemTags.PLANKS)
		    .input('@', Items.FLINT)
		    .pattern("@@")
		    .pattern("##")
		    .pattern("##")
		    .criterion("has_flint", conditionsFromItem(Items.FLINT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.STONECUTTER)
		    .input('I', Items.IRON_INGOT)
		    .input('#', Blocks.STONE)
		    .pattern(" I ")
		    .pattern("###")
		    .criterion("has_stone", conditionsFromItem(Blocks.STONE))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.LODESTONE)
		    .input('S', Items.CHISELED_STONE_BRICKS)
		    .input('#', Items.IRON_INGOT)
		    .pattern("SSS")
		    .pattern("S#S")
		    .pattern("SSS")
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .criterion("has_lodestone", conditionsFromItem(Items.LODESTONE))
		    .offerTo(exporter);
		offerReversibleCompactingRecipesWithReverseRecipeGroup(
				RecipeCategory.MISC,
				Items.NETHERITE_INGOT,
				RecipeCategory.BUILDING_BLOCKS,
				Items.NETHERITE_BLOCK,
				"netherite_ingot_from_netherite_block",
				"netherite_ingot"
		);
		createShapeless(RecipeCategory.MISC, Items.NETHERITE_INGOT)
		    .input(Items.NETHERITE_SCRAP, 4)
		    .input(Items.GOLD_INGOT, 4)
		    .group("netherite_ingot")
		    .criterion("has_netherite_scrap", conditionsFromItem(Items.NETHERITE_SCRAP))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.RESPAWN_ANCHOR)
		    .input('O', Blocks.CRYING_OBSIDIAN)
		    .input('G', Blocks.GLOWSTONE)
		    .pattern("OOO")
		    .pattern("GGG")
		    .pattern("OOO")
		    .criterion("has_obsidian", conditionsFromItem(Blocks.CRYING_OBSIDIAN))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.IRON_CHAIN)
		    .input('I', Items.IRON_INGOT)
		    .input('N', Items.IRON_NUGGET)
		    .pattern("N")
		    .pattern("I")
		    .pattern("N")
		    .criterion("has_iron_nugget", conditionsFromItem(Items.IRON_NUGGET))
		    .criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Blocks.COPPER_CHAINS.unaffected())
		    .input('I', Items.COPPER_INGOT)
		    .input('N', Items.COPPER_NUGGET)
		    .pattern("N")
		    .pattern("I")
		    .pattern("N")
		    .criterion("has_copper_nugget", conditionsFromItem(Items.COPPER_NUGGET))
		    .criterion("has_copper_ingot", conditionsFromItem(Items.COPPER_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.TINTED_GLASS, 2)
		    .input('G', Blocks.GLASS)
		    .input('S', Items.AMETHYST_SHARD)
		    .pattern(" S ")
		    .pattern("SGS")
		    .pattern(" S ")
		    .criterion("has_amethyst_shard", conditionsFromItem(Items.AMETHYST_SHARD))
		    .offerTo(exporter);
		offer2x2CompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.AMETHYST_BLOCK, Items.AMETHYST_SHARD);
		createShaped(RecipeCategory.TOOLS, Items.RECOVERY_COMPASS)
		    .input('C', Items.COMPASS)
		    .input('S', Items.ECHO_SHARD)
		    .pattern("SSS")
		    .pattern("SCS")
		    .pattern("SSS")
		    .criterion("has_echo_shard", conditionsFromItem(Items.ECHO_SHARD))
		    .offerTo(exporter);
		createShaped(RecipeCategory.REDSTONE, Items.CALIBRATED_SCULK_SENSOR)
		    .input('#', Items.AMETHYST_SHARD)
		    .input('X', Items.SCULK_SENSOR)
		    .pattern(" # ")
		    .pattern("#X#")
		    .criterion("has_amethyst_shard", conditionsFromItem(Items.AMETHYST_SHARD))
		    .offerTo(exporter);
		offerCompactingRecipe(RecipeCategory.MISC, Items.MUSIC_DISC_5, Items.DISC_FRAGMENT_5);
		ComplexRecipeJsonBuilder.create(ArmorDyeRecipe::new).offerTo(exporter, "armor_dye");
		ComplexRecipeJsonBuilder.create(BannerDuplicateRecipe::new).offerTo(exporter, "banner_duplicate");
		ComplexRecipeJsonBuilder.create(BookCloningRecipe::new).offerTo(exporter, "book_cloning");
		ComplexRecipeJsonBuilder.create(FireworkRocketRecipe::new).offerTo(exporter, "firework_rocket");
		ComplexRecipeJsonBuilder.create(FireworkStarRecipe::new).offerTo(exporter, "firework_star");
		ComplexRecipeJsonBuilder.create(FireworkStarFadeRecipe::new).offerTo(exporter, "firework_star_fade");
		ComplexRecipeJsonBuilder.create(MapCloningRecipe::new).offerTo(exporter, "map_cloning");
		ComplexRecipeJsonBuilder.create(MapExtendingRecipe::new).offerTo(exporter, "map_extending");
		ComplexRecipeJsonBuilder.create(RepairItemRecipe::new).offerTo(exporter, "repair_item");
		ComplexRecipeJsonBuilder.create(ShieldDecorationRecipe::new).offerTo(exporter, "shield_decoration");
		ComplexRecipeJsonBuilder.create(TippedArrowRecipe::new).offerTo(exporter, "tipped_arrow");
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(Items.POTATO), RecipeCategory.FOOD, Items.BAKED_POTATO, 0.35F, 200)
				.criterion("has_potato", conditionsFromItem(Items.POTATO))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(Items.CLAY_BALL), RecipeCategory.MISC, Items.BRICK, 0.3F, 200)
				.criterion("has_clay_ball", conditionsFromItem(Items.CLAY_BALL))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						ingredientFromTag(ItemTags.LOGS_THAT_BURN),
						RecipeCategory.MISC,
						Items.CHARCOAL,
						0.15F,
						200
				)
				.criterion("has_log", conditionsFromTag(ItemTags.LOGS_THAT_BURN))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Items.CHORUS_FRUIT),
						RecipeCategory.MISC,
						Items.POPPED_CHORUS_FRUIT,
						0.1F,
						200
				)
				.criterion("has_chorus_fruit", conditionsFromItem(Items.CHORUS_FRUIT))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(Items.BEEF), RecipeCategory.FOOD, Items.COOKED_BEEF, 0.35F, 200)
				.criterion("has_beef", conditionsFromItem(Items.BEEF))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(Items.CHICKEN), RecipeCategory.FOOD, Items.COOKED_CHICKEN, 0.35F, 200)
				.criterion("has_chicken", conditionsFromItem(Items.CHICKEN))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(Items.COD), RecipeCategory.FOOD, Items.COOKED_COD, 0.35F, 200)
				.criterion("has_cod", conditionsFromItem(Items.COD))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(Blocks.KELP), RecipeCategory.FOOD, Items.DRIED_KELP, 0.1F, 200)
				.criterion("has_kelp", conditionsFromItem(Blocks.KELP))
				.offerTo(exporter, getSmeltingItemPath(Items.DRIED_KELP));
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(Items.SALMON), RecipeCategory.FOOD, Items.COOKED_SALMON, 0.35F, 200)
				.criterion("has_salmon", conditionsFromItem(Items.SALMON))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(Items.MUTTON), RecipeCategory.FOOD, Items.COOKED_MUTTON, 0.35F, 200)
				.criterion("has_mutton", conditionsFromItem(Items.MUTTON))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Items.PORKCHOP),
						RecipeCategory.FOOD,
						Items.COOKED_PORKCHOP,
						0.35F,
						200
				)
				.criterion("has_porkchop", conditionsFromItem(Items.PORKCHOP))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(Items.RABBIT), RecipeCategory.FOOD, Items.COOKED_RABBIT, 0.35F, 200)
				.criterion("has_rabbit", conditionsFromItem(Items.RABBIT))
				.offerTo(exporter);
		offerSmelting(COAL_ORES, RecipeCategory.MISC, Items.COAL, 0.1F, 200, "coal");
		offerSmelting(IRON_ORES, RecipeCategory.MISC, Items.IRON_INGOT, 0.7F, 200, "iron_ingot");
		offerSmelting(COPPER_ORES, RecipeCategory.MISC, Items.COPPER_INGOT, 0.7F, 200, "copper_ingot");
		offerSmelting(GOLD_ORES, RecipeCategory.MISC, Items.GOLD_INGOT, 1.0F, 200, "gold_ingot");
		offerSmelting(DIAMOND_ORES, RecipeCategory.MISC, Items.DIAMOND, 1.0F, 200, "diamond");
		offerSmelting(LAPIS_ORES, RecipeCategory.MISC, Items.LAPIS_LAZULI, 0.2F, 200, "lapis_lazuli");
		offerSmelting(REDSTONE_ORES, RecipeCategory.REDSTONE, Items.REDSTONE, 0.7F, 200, "redstone");
		offerSmelting(EMERALD_ORES, RecipeCategory.MISC, Items.EMERALD, 1.0F, 200, "emerald");
		offerReversibleCompactingRecipes(
				RecipeCategory.MISC,
				Items.RAW_IRON,
				RecipeCategory.BUILDING_BLOCKS,
				Items.RAW_IRON_BLOCK
		);
		offerReversibleCompactingRecipes(
				RecipeCategory.MISC,
				Items.RAW_COPPER,
				RecipeCategory.BUILDING_BLOCKS,
				Items.RAW_COPPER_BLOCK
		);
		offerReversibleCompactingRecipes(
				RecipeCategory.MISC,
				Items.RAW_GOLD,
				RecipeCategory.BUILDING_BLOCKS,
				Items.RAW_GOLD_BLOCK
		);
		CookingRecipeJsonBuilder.createSmelting(
				                        ingredientFromTag(ItemTags.SMELTS_TO_GLASS),
				                        RecipeCategory.BUILDING_BLOCKS,
				                        Blocks.GLASS.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_smelts_to_glass", conditionsFromTag(ItemTags.SMELTS_TO_GLASS))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(Blocks.SEA_PICKLE), RecipeCategory.MISC, Items.LIME_DYE, 0.1F, 200)
				.criterion("has_sea_pickle", conditionsFromItem(Blocks.SEA_PICKLE))
				.offerTo(exporter, getSmeltingItemPath(Items.LIME_DYE));
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.CACTUS.asItem()),
						RecipeCategory.MISC,
						Items.GREEN_DYE,
						1.0F,
						200
				)
				.criterion("has_cactus", conditionsFromItem(Blocks.CACTUS))
				.offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItems(
						                        Items.GOLDEN_PICKAXE,
						                        Items.GOLDEN_SHOVEL,
						                        Items.GOLDEN_AXE,
						                        Items.GOLDEN_HOE,
						                        Items.GOLDEN_SWORD,
						                        Items.GOLDEN_SPEAR,
						                        Items.GOLDEN_HELMET,
						                        Items.GOLDEN_CHESTPLATE,
						                        Items.GOLDEN_LEGGINGS,
						                        Items.GOLDEN_BOOTS,
						                        Items.GOLDEN_HORSE_ARMOR,
						                        Items.GOLDEN_NAUTILUS_ARMOR
				                        ),
				                        RecipeCategory.MISC,
				                        Items.GOLD_NUGGET,
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_golden_pickaxe", conditionsFromItem(Items.GOLDEN_PICKAXE))
		                        .criterion("has_golden_shovel", conditionsFromItem(Items.GOLDEN_SHOVEL))
		                        .criterion("has_golden_axe", conditionsFromItem(Items.GOLDEN_AXE))
		                        .criterion("has_golden_hoe", conditionsFromItem(Items.GOLDEN_HOE))
		                        .criterion("has_golden_sword", conditionsFromItem(Items.GOLDEN_SWORD))
		                        .criterion("has_golden_spear", conditionsFromItem(Items.GOLDEN_SPEAR))
		                        .criterion("has_golden_helmet", conditionsFromItem(Items.GOLDEN_HELMET))
		                        .criterion("has_golden_chestplate", conditionsFromItem(Items.GOLDEN_CHESTPLATE))
		                        .criterion("has_golden_leggings", conditionsFromItem(Items.GOLDEN_LEGGINGS))
		                        .criterion("has_golden_boots", conditionsFromItem(Items.GOLDEN_BOOTS))
		                        .criterion("has_golden_horse_armor", conditionsFromItem(Items.GOLDEN_HORSE_ARMOR))
		                        .criterion(
				                        "has_golden_nautilus_armor",
				                        conditionsFromItem(Items.GOLDEN_NAUTILUS_ARMOR)
		                        )
		                        .offerTo(exporter, getSmeltingItemPath(Items.GOLD_NUGGET));
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItems(
						                        Items.COPPER_PICKAXE,
						                        Items.COPPER_SHOVEL,
						                        Items.COPPER_AXE,
						                        Items.COPPER_HOE,
						                        Items.COPPER_SWORD,
						                        Items.COPPER_SPEAR,
						                        Items.COPPER_HELMET,
						                        Items.COPPER_CHESTPLATE,
						                        Items.COPPER_LEGGINGS,
						                        Items.COPPER_BOOTS,
						                        Items.COPPER_HORSE_ARMOR,
						                        Items.COPPER_NAUTILUS_ARMOR
				                        ),
				                        RecipeCategory.MISC,
				                        Items.COPPER_NUGGET,
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_copper_pickaxe", conditionsFromItem(Items.COPPER_PICKAXE))
		                        .criterion("has_copper_shovel", conditionsFromItem(Items.COPPER_SHOVEL))
		                        .criterion("has_copper_axe", conditionsFromItem(Items.COPPER_AXE))
		                        .criterion("has_copper_hoe", conditionsFromItem(Items.COPPER_HOE))
		                        .criterion("has_copper_sword", conditionsFromItem(Items.COPPER_SWORD))
		                        .criterion("has_copper_spear", conditionsFromItem(Items.COPPER_SPEAR))
		                        .criterion("has_copper_helmet", conditionsFromItem(Items.COPPER_HELMET))
		                        .criterion("has_copper_chestplate", conditionsFromItem(Items.COPPER_CHESTPLATE))
		                        .criterion("has_copper_leggings", conditionsFromItem(Items.COPPER_LEGGINGS))
		                        .criterion("has_copper_boots", conditionsFromItem(Items.COPPER_BOOTS))
		                        .criterion("has_copper_horse_armor", conditionsFromItem(Items.COPPER_HORSE_ARMOR))
		                        .criterion(
				                        "has_copper_nautilus_armor",
				                        conditionsFromItem(Items.COPPER_NAUTILUS_ARMOR)
		                        )
		                        .offerTo(exporter, getSmeltingItemPath(Items.COPPER_NUGGET));
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItems(
						                        Items.IRON_PICKAXE,
						                        Items.IRON_SHOVEL,
						                        Items.IRON_AXE,
						                        Items.IRON_HOE,
						                        Items.IRON_SWORD,
						                        Items.IRON_SPEAR,
						                        Items.IRON_HELMET,
						                        Items.IRON_CHESTPLATE,
						                        Items.IRON_LEGGINGS,
						                        Items.IRON_BOOTS,
						                        Items.IRON_HORSE_ARMOR,
						                        Items.CHAINMAIL_HELMET,
						                        Items.CHAINMAIL_CHESTPLATE,
						                        Items.CHAINMAIL_LEGGINGS,
						                        Items.CHAINMAIL_BOOTS,
						                        Items.IRON_NAUTILUS_ARMOR
				                        ),
				                        RecipeCategory.MISC,
				                        Items.IRON_NUGGET,
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_iron_pickaxe", conditionsFromItem(Items.IRON_PICKAXE))
		                        .criterion("has_iron_shovel", conditionsFromItem(Items.IRON_SHOVEL))
		                        .criterion("has_iron_axe", conditionsFromItem(Items.IRON_AXE))
		                        .criterion("has_iron_hoe", conditionsFromItem(Items.IRON_HOE))
		                        .criterion("has_iron_sword", conditionsFromItem(Items.IRON_SWORD))
		                        .criterion("has_iron_spear", conditionsFromItem(Items.IRON_SPEAR))
		                        .criterion("has_iron_helmet", conditionsFromItem(Items.IRON_HELMET))
		                        .criterion("has_iron_chestplate", conditionsFromItem(Items.IRON_CHESTPLATE))
		                        .criterion("has_iron_leggings", conditionsFromItem(Items.IRON_LEGGINGS))
		                        .criterion("has_iron_boots", conditionsFromItem(Items.IRON_BOOTS))
		                        .criterion("has_iron_horse_armor", conditionsFromItem(Items.IRON_HORSE_ARMOR))
		                        .criterion("has_chainmail_helmet", conditionsFromItem(Items.CHAINMAIL_HELMET))
		                        .criterion(
				                        "has_chainmail_chestplate",
				                        conditionsFromItem(Items.CHAINMAIL_CHESTPLATE)
		                        )
		                        .criterion("has_chainmail_leggings", conditionsFromItem(Items.CHAINMAIL_LEGGINGS))
		                        .criterion("has_chainmail_boots", conditionsFromItem(Items.CHAINMAIL_BOOTS))
		                        .criterion(
				                        "has_iron_nautilus_armor",
				                        conditionsFromItem(Items.IRON_NAUTILUS_ARMOR)
		                        )
		                        .offerTo(exporter, getSmeltingItemPath(Items.IRON_NUGGET));
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.CLAY),
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.TERRACOTTA.asItem(),
						0.35F,
						200
				)
				.criterion("has_clay_block", conditionsFromItem(Blocks.CLAY))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.NETHERRACK),
						RecipeCategory.MISC,
						Items.NETHER_BRICK,
						0.1F,
						200
				)
				.criterion("has_netherrack", conditionsFromItem(Blocks.NETHERRACK))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(Items.RESIN_CLUMP), RecipeCategory.MISC, Items.RESIN_BRICK, 0.1F, 200)
				.criterion("has_resin_clump", conditionsFromItem(Blocks.RESIN_CLUMP))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.NETHER_QUARTZ_ORE),
						RecipeCategory.MISC,
						Items.QUARTZ,
						0.2F,
						200
				)
				.criterion("has_nether_quartz_ore", conditionsFromItem(Blocks.NETHER_QUARTZ_ORE))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.WET_SPONGE),
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.SPONGE.asItem(),
						0.15F,
						200
				)
				.criterion("has_wet_sponge", conditionsFromItem(Blocks.WET_SPONGE))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.COBBLESTONE),
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.STONE.asItem(),
						0.1F,
						200
				)
				.criterion("has_cobblestone", conditionsFromItem(Blocks.COBBLESTONE))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.STONE),
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.SMOOTH_STONE.asItem(),
						0.1F,
						200
				)
				.criterion("has_stone", conditionsFromItem(Blocks.STONE))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.SANDSTONE),
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.SMOOTH_SANDSTONE.asItem(),
						0.1F,
						200
				)
				.criterion("has_sandstone", conditionsFromItem(Blocks.SANDSTONE))
				.offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.RED_SANDSTONE),
				                        RecipeCategory.BUILDING_BLOCKS,
				                        Blocks.SMOOTH_RED_SANDSTONE.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_red_sandstone", conditionsFromItem(Blocks.RED_SANDSTONE))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.QUARTZ_BLOCK),
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.SMOOTH_QUARTZ.asItem(),
						0.1F,
						200
				)
				.criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK))
				.offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.STONE_BRICKS),
				                        RecipeCategory.BUILDING_BLOCKS,
				                        Blocks.CRACKED_STONE_BRICKS.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_stone_bricks", conditionsFromItem(Blocks.STONE_BRICKS))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.BLACK_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.BLACK_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_black_terracotta", conditionsFromItem(Blocks.BLACK_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.BLUE_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.BLUE_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_blue_terracotta", conditionsFromItem(Blocks.BLUE_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.BROWN_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.BROWN_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_brown_terracotta", conditionsFromItem(Blocks.BROWN_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.CYAN_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.CYAN_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_cyan_terracotta", conditionsFromItem(Blocks.CYAN_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.GRAY_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.GRAY_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_gray_terracotta", conditionsFromItem(Blocks.GRAY_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.GREEN_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.GREEN_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_green_terracotta", conditionsFromItem(Blocks.GREEN_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.LIGHT_BLUE_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion(
				                        "has_light_blue_terracotta",
				                        conditionsFromItem(Blocks.LIGHT_BLUE_TERRACOTTA)
		                        )
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.LIGHT_GRAY_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion(
				                        "has_light_gray_terracotta",
				                        conditionsFromItem(Blocks.LIGHT_GRAY_TERRACOTTA)
		                        )
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.LIME_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.LIME_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_lime_terracotta", conditionsFromItem(Blocks.LIME_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.MAGENTA_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.MAGENTA_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_magenta_terracotta", conditionsFromItem(Blocks.MAGENTA_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.ORANGE_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.ORANGE_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_orange_terracotta", conditionsFromItem(Blocks.ORANGE_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.PINK_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.PINK_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_pink_terracotta", conditionsFromItem(Blocks.PINK_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.PURPLE_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.PURPLE_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_purple_terracotta", conditionsFromItem(Blocks.PURPLE_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.RED_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.RED_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_red_terracotta", conditionsFromItem(Blocks.RED_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.WHITE_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.WHITE_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_white_terracotta", conditionsFromItem(Blocks.WHITE_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder.createSmelting(
				                        Ingredient.ofItem(Blocks.YELLOW_TERRACOTTA),
				                        RecipeCategory.DECORATIONS,
				                        Blocks.YELLOW_GLAZED_TERRACOTTA.asItem(),
				                        0.1F,
				                        200
		                        )
		                        .criterion("has_yellow_terracotta", conditionsFromItem(Blocks.YELLOW_TERRACOTTA))
		                        .offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.ANCIENT_DEBRIS),
						RecipeCategory.MISC,
						Items.NETHERITE_SCRAP,
						2.0F,
						200
				)
				.criterion("has_ancient_debris", conditionsFromItem(Blocks.ANCIENT_DEBRIS))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.BASALT),
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.SMOOTH_BASALT,
						0.1F,
						200
				)
				.criterion("has_basalt", conditionsFromItem(Blocks.BASALT))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						Ingredient.ofItem(Blocks.COBBLED_DEEPSLATE),
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.DEEPSLATE,
						0.1F,
						200
				)
				.criterion("has_cobbled_deepslate", conditionsFromItem(Blocks.COBBLED_DEEPSLATE))
				.offerTo(exporter);
		CookingRecipeJsonBuilder
				.createSmelting(
						ingredientFromTag(ItemTags.LEAVES),
						RecipeCategory.MISC,
						Blocks.LEAF_LITTER,
						0.1F,
						200
				)
				.criterion("has_leaves", conditionsFromTag(ItemTags.LEAVES))
				.offerTo(exporter);
		offerBlasting(COAL_ORES, RecipeCategory.MISC, Items.COAL, 0.1F, 100, "coal");
		offerBlasting(IRON_ORES, RecipeCategory.MISC, Items.IRON_INGOT, 0.7F, 100, "iron_ingot");
		offerBlasting(COPPER_ORES, RecipeCategory.MISC, Items.COPPER_INGOT, 0.7F, 100, "copper_ingot");
		offerBlasting(GOLD_ORES, RecipeCategory.MISC, Items.GOLD_INGOT, 1.0F, 100, "gold_ingot");
		offerBlasting(DIAMOND_ORES, RecipeCategory.MISC, Items.DIAMOND, 1.0F, 100, "diamond");
		offerBlasting(LAPIS_ORES, RecipeCategory.MISC, Items.LAPIS_LAZULI, 0.2F, 100, "lapis_lazuli");
		offerBlasting(REDSTONE_ORES, RecipeCategory.REDSTONE, Items.REDSTONE, 0.7F, 100, "redstone");
		offerBlasting(EMERALD_ORES, RecipeCategory.MISC, Items.EMERALD, 1.0F, 100, "emerald");
		CookingRecipeJsonBuilder
				.createBlasting(
						Ingredient.ofItem(Blocks.NETHER_QUARTZ_ORE),
						RecipeCategory.MISC,
						Items.QUARTZ,
						0.2F,
						100
				)
				.criterion("has_nether_quartz_ore", conditionsFromItem(Blocks.NETHER_QUARTZ_ORE))
				.offerTo(exporter, getBlastingItemPath(Items.QUARTZ));
		CookingRecipeJsonBuilder.createBlasting(
				                        Ingredient.ofItems(
						                        Items.GOLDEN_PICKAXE,
						                        Items.GOLDEN_SHOVEL,
						                        Items.GOLDEN_AXE,
						                        Items.GOLDEN_HOE,
						                        Items.GOLDEN_SWORD,
						                        Items.GOLDEN_SPEAR,
						                        Items.GOLDEN_HELMET,
						                        Items.GOLDEN_CHESTPLATE,
						                        Items.GOLDEN_LEGGINGS,
						                        Items.GOLDEN_BOOTS,
						                        Items.GOLDEN_HORSE_ARMOR,
						                        Items.GOLDEN_NAUTILUS_ARMOR
				                        ),
				                        RecipeCategory.MISC,
				                        Items.GOLD_NUGGET,
				                        0.1F,
				                        100
		                        )
		                        .criterion("has_golden_pickaxe", conditionsFromItem(Items.GOLDEN_PICKAXE))
		                        .criterion("has_golden_shovel", conditionsFromItem(Items.GOLDEN_SHOVEL))
		                        .criterion("has_golden_axe", conditionsFromItem(Items.GOLDEN_AXE))
		                        .criterion("has_golden_hoe", conditionsFromItem(Items.GOLDEN_HOE))
		                        .criterion("has_golden_sword", conditionsFromItem(Items.GOLDEN_SWORD))
		                        .criterion("has_golden_spear", conditionsFromItem(Items.GOLDEN_SPEAR))
		                        .criterion("has_golden_helmet", conditionsFromItem(Items.GOLDEN_HELMET))
		                        .criterion("has_golden_chestplate", conditionsFromItem(Items.GOLDEN_CHESTPLATE))
		                        .criterion("has_golden_leggings", conditionsFromItem(Items.GOLDEN_LEGGINGS))
		                        .criterion("has_golden_boots", conditionsFromItem(Items.GOLDEN_BOOTS))
		                        .criterion("has_golden_horse_armor", conditionsFromItem(Items.GOLDEN_HORSE_ARMOR))
		                        .criterion(
				                        "has_golden_nautilus_armor",
				                        conditionsFromItem(Items.GOLDEN_NAUTILUS_ARMOR)
		                        )
		                        .offerTo(exporter, getBlastingItemPath(Items.GOLD_NUGGET));
		CookingRecipeJsonBuilder.createBlasting(
				                        Ingredient.ofItems(
						                        Items.COPPER_PICKAXE,
						                        Items.COPPER_SHOVEL,
						                        Items.COPPER_AXE,
						                        Items.COPPER_HOE,
						                        Items.COPPER_SWORD,
						                        Items.COPPER_SPEAR,
						                        Items.COPPER_HELMET,
						                        Items.COPPER_CHESTPLATE,
						                        Items.COPPER_LEGGINGS,
						                        Items.COPPER_BOOTS,
						                        Items.COPPER_HORSE_ARMOR,
						                        Items.COPPER_NAUTILUS_ARMOR
				                        ),
				                        RecipeCategory.MISC,
				                        Items.COPPER_NUGGET,
				                        0.1F,
				                        100
		                        )
		                        .criterion("has_copper_pickaxe", conditionsFromItem(Items.COPPER_PICKAXE))
		                        .criterion("has_copper_shovel", conditionsFromItem(Items.COPPER_SHOVEL))
		                        .criterion("has_copper_axe", conditionsFromItem(Items.COPPER_AXE))
		                        .criterion("has_copper_hoe", conditionsFromItem(Items.COPPER_HOE))
		                        .criterion("has_copper_sword", conditionsFromItem(Items.COPPER_SWORD))
		                        .criterion("has_copper_spear", conditionsFromItem(Items.COPPER_SPEAR))
		                        .criterion("has_copper_helmet", conditionsFromItem(Items.COPPER_HELMET))
		                        .criterion("has_copper_chestplate", conditionsFromItem(Items.COPPER_CHESTPLATE))
		                        .criterion("has_copper_leggings", conditionsFromItem(Items.COPPER_LEGGINGS))
		                        .criterion("has_copper_boots", conditionsFromItem(Items.COPPER_BOOTS))
		                        .criterion("has_copper_horse_armor", conditionsFromItem(Items.COPPER_HORSE_ARMOR))
		                        .criterion(
				                        "has_copper_nautilus_armor",
				                        conditionsFromItem(Items.COPPER_NAUTILUS_ARMOR)
		                        )
		                        .offerTo(exporter, getBlastingItemPath(Items.COPPER_NUGGET));
		CookingRecipeJsonBuilder.createBlasting(
				                        Ingredient.ofItems(
						                        Items.IRON_PICKAXE,
						                        Items.IRON_SHOVEL,
						                        Items.IRON_AXE,
						                        Items.IRON_HOE,
						                        Items.IRON_SWORD,
						                        Items.IRON_SPEAR,
						                        Items.IRON_HELMET,
						                        Items.IRON_CHESTPLATE,
						                        Items.IRON_LEGGINGS,
						                        Items.IRON_BOOTS,
						                        Items.IRON_HORSE_ARMOR,
						                        Items.IRON_NAUTILUS_ARMOR,
						                        Items.CHAINMAIL_HELMET,
						                        Items.CHAINMAIL_CHESTPLATE,
						                        Items.CHAINMAIL_LEGGINGS,
						                        Items.CHAINMAIL_BOOTS
				                        ),
				                        RecipeCategory.MISC,
				                        Items.IRON_NUGGET,
				                        0.1F,
				                        100
		                        )
		                        .criterion("has_iron_pickaxe", conditionsFromItem(Items.IRON_PICKAXE))
		                        .criterion("has_iron_shovel", conditionsFromItem(Items.IRON_SHOVEL))
		                        .criterion("has_iron_axe", conditionsFromItem(Items.IRON_AXE))
		                        .criterion("has_iron_hoe", conditionsFromItem(Items.IRON_HOE))
		                        .criterion("has_iron_sword", conditionsFromItem(Items.IRON_SWORD))
		                        .criterion("has_iron_spear", conditionsFromItem(Items.IRON_SPEAR))
		                        .criterion("has_iron_helmet", conditionsFromItem(Items.IRON_HELMET))
		                        .criterion("has_iron_chestplate", conditionsFromItem(Items.IRON_CHESTPLATE))
		                        .criterion("has_iron_leggings", conditionsFromItem(Items.IRON_LEGGINGS))
		                        .criterion("has_iron_boots", conditionsFromItem(Items.IRON_BOOTS))
		                        .criterion("has_iron_horse_armor", conditionsFromItem(Items.IRON_HORSE_ARMOR))
		                        .criterion("has_chainmail_helmet", conditionsFromItem(Items.CHAINMAIL_HELMET))
		                        .criterion(
				                        "has_chainmail_chestplate",
				                        conditionsFromItem(Items.CHAINMAIL_CHESTPLATE)
		                        )
		                        .criterion("has_chainmail_leggings", conditionsFromItem(Items.CHAINMAIL_LEGGINGS))
		                        .criterion("has_chainmail_boots", conditionsFromItem(Items.CHAINMAIL_BOOTS))
		                        .criterion(
				                        "has_iron_nautilus_armor",
				                        conditionsFromItem(Items.IRON_NAUTILUS_ARMOR)
		                        )
		                        .offerTo(exporter, getBlastingItemPath(Items.IRON_NUGGET));
		CookingRecipeJsonBuilder
				.createBlasting(
						Ingredient.ofItem(Blocks.ANCIENT_DEBRIS),
						RecipeCategory.MISC,
						Items.NETHERITE_SCRAP,
						2.0F,
						100
				)
				.criterion("has_ancient_debris", conditionsFromItem(Blocks.ANCIENT_DEBRIS))
				.offerTo(exporter, getBlastingItemPath(Items.NETHERITE_SCRAP));
		generateCookingRecipes("smoking", RecipeSerializer.SMOKING, SmokingRecipe::new, 100);
		generateCookingRecipes(
				"campfire_cooking",
				RecipeSerializer.CAMPFIRE_COOKING,
				CampfireCookingRecipe::new,
				600
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.STONE_SLAB, Blocks.STONE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.STONE_STAIRS, Blocks.STONE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.STONE_BRICKS, Blocks.STONE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.STONE_BRICK_SLAB, Blocks.STONE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.STONE_BRICK_STAIRS, Blocks.STONE);
		StonecuttingRecipeJsonBuilder
				.createStonecutting(
						Ingredient.ofItem(Blocks.STONE),
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.CHISELED_STONE_BRICKS
				)
				.criterion("has_stone", conditionsFromItem(Blocks.STONE))
				.offerTo(exporter, "chiseled_stone_bricks_stone_from_stonecutting");
		StonecuttingRecipeJsonBuilder
				.createStonecutting(
						Ingredient.ofItem(Blocks.STONE),
						RecipeCategory.DECORATIONS,
						Blocks.STONE_BRICK_WALL
				)
				.criterion("has_stone", conditionsFromItem(Blocks.STONE))
				.offerTo(exporter, "stone_brick_walls_from_stone_stonecutting");
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.SANDSTONE_SLAB, Blocks.SANDSTONE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_SANDSTONE_SLAB, Blocks.SANDSTONE, 2);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.CUT_SANDSTONE_SLAB,
				Blocks.CUT_SANDSTONE,
				2
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.SANDSTONE_STAIRS, Blocks.SANDSTONE);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.SANDSTONE_WALL, Blocks.SANDSTONE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_SANDSTONE, Blocks.SANDSTONE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_RED_SANDSTONE, Blocks.RED_SANDSTONE);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.RED_SANDSTONE_SLAB,
				Blocks.RED_SANDSTONE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.CUT_RED_SANDSTONE_SLAB,
				Blocks.RED_SANDSTONE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.CUT_RED_SANDSTONE_SLAB,
				Blocks.CUT_RED_SANDSTONE,
				2
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.RED_SANDSTONE_STAIRS, Blocks.RED_SANDSTONE);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.RED_SANDSTONE_WALL, Blocks.RED_SANDSTONE);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.CHISELED_RED_SANDSTONE,
				Blocks.RED_SANDSTONE
		);
		StonecuttingRecipeJsonBuilder
				.createStonecutting(
						Ingredient.ofItem(Blocks.QUARTZ_BLOCK),
						RecipeCategory.BUILDING_BLOCKS,
						Blocks.QUARTZ_SLAB,
						2
				)
				.criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK))
				.offerTo(exporter, "quartz_slab_from_stonecutting");
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.QUARTZ_STAIRS, Blocks.QUARTZ_BLOCK);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BLOCK);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.QUARTZ_BRICKS, Blocks.QUARTZ_BLOCK);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.COBBLESTONE_STAIRS, Blocks.COBBLESTONE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.COBBLESTONE_SLAB, Blocks.COBBLESTONE, 2);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.COBBLESTONE_WALL, Blocks.COBBLESTONE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.STONE_BRICK_SLAB, Blocks.STONE_BRICKS, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS);
		StonecuttingRecipeJsonBuilder
				.createStonecutting(
						Ingredient.ofItem(Blocks.STONE_BRICKS),
						RecipeCategory.DECORATIONS,
						Blocks.STONE_BRICK_WALL
				)
				.criterion("has_stone_bricks", conditionsFromItem(Blocks.STONE_BRICKS))
				.offerTo(exporter, "stone_brick_wall_from_stone_bricks_stonecutting");
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.BRICK_SLAB, Blocks.BRICKS, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.BRICK_STAIRS, Blocks.BRICKS);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.BRICK_WALL, Blocks.BRICKS);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.MUD_BRICK_SLAB, Blocks.MUD_BRICKS, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.MUD_BRICK_STAIRS, Blocks.MUD_BRICKS);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.MUD_BRICK_WALL, Blocks.MUD_BRICKS);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.NETHER_BRICK_SLAB, Blocks.NETHER_BRICKS, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_BRICKS);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.NETHER_BRICK_WALL, Blocks.NETHER_BRICKS);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.CHISELED_NETHER_BRICKS,
				Blocks.NETHER_BRICKS
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.RESIN_BRICK_SLAB, Blocks.RESIN_BRICKS, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.RESIN_BRICK_STAIRS, Blocks.RESIN_BRICKS);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.RESIN_BRICK_WALL, Blocks.RESIN_BRICKS);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_RESIN_BRICKS, Blocks.RESIN_BRICKS);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.RED_NETHER_BRICK_SLAB,
				Blocks.RED_NETHER_BRICKS,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.RED_NETHER_BRICK_STAIRS,
				Blocks.RED_NETHER_BRICKS
		);
		offerStonecuttingRecipe(
				RecipeCategory.DECORATIONS,
				Blocks.RED_NETHER_BRICK_WALL,
				Blocks.RED_NETHER_BRICKS
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.PURPUR_SLAB, Blocks.PURPUR_BLOCK, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.PURPUR_STAIRS, Blocks.PURPUR_BLOCK);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.PURPUR_PILLAR, Blocks.PURPUR_BLOCK);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.PRISMARINE_SLAB, Blocks.PRISMARINE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.PRISMARINE_STAIRS, Blocks.PRISMARINE);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.PRISMARINE_WALL, Blocks.PRISMARINE);
		StonecuttingRecipeJsonBuilder.createStonecutting(
				                             Ingredient.ofItem(Blocks.PRISMARINE_BRICKS),
				                             RecipeCategory.BUILDING_BLOCKS,
				                             Blocks.PRISMARINE_BRICK_SLAB,
				                             2
		                             )
		                             .criterion(
				                             "has_prismarine_brick",
				                             conditionsFromItem(Blocks.PRISMARINE_BRICKS)
		                             )
		                             .offerTo(exporter, "prismarine_brick_slab_from_prismarine_stonecutting");
		StonecuttingRecipeJsonBuilder.createStonecutting(
				                             Ingredient.ofItem(Blocks.PRISMARINE_BRICKS),
				                             RecipeCategory.BUILDING_BLOCKS,
				                             Blocks.PRISMARINE_BRICK_STAIRS
		                             )
		                             .criterion(
				                             "has_prismarine_brick",
				                             conditionsFromItem(Blocks.PRISMARINE_BRICKS)
		                             )
		                             .offerTo(exporter, "prismarine_brick_stairs_from_prismarine_stonecutting");
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DARK_PRISMARINE_SLAB,
				Blocks.DARK_PRISMARINE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DARK_PRISMARINE_STAIRS,
				Blocks.DARK_PRISMARINE
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.ANDESITE_SLAB, Blocks.ANDESITE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.ANDESITE_STAIRS, Blocks.ANDESITE);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.ANDESITE_WALL, Blocks.ANDESITE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_ANDESITE, Blocks.ANDESITE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_ANDESITE_SLAB, Blocks.ANDESITE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.ANDESITE);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_ANDESITE_SLAB,
				Blocks.POLISHED_ANDESITE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_ANDESITE_STAIRS,
				Blocks.POLISHED_ANDESITE
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_BASALT, Blocks.BASALT);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.GRANITE_SLAB, Blocks.GRANITE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.GRANITE_STAIRS, Blocks.GRANITE);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.GRANITE_WALL, Blocks.GRANITE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_GRANITE, Blocks.GRANITE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_GRANITE_SLAB, Blocks.GRANITE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_GRANITE_STAIRS, Blocks.GRANITE);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_GRANITE_SLAB,
				Blocks.POLISHED_GRANITE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_GRANITE_STAIRS,
				Blocks.POLISHED_GRANITE
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.DIORITE_SLAB, Blocks.DIORITE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.DIORITE_STAIRS, Blocks.DIORITE);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.DIORITE_WALL, Blocks.DIORITE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_DIORITE, Blocks.DIORITE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_DIORITE_SLAB, Blocks.DIORITE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_DIORITE_STAIRS, Blocks.DIORITE);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_DIORITE_SLAB,
				Blocks.POLISHED_DIORITE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_DIORITE_STAIRS,
				Blocks.POLISHED_DIORITE
		);
		StonecuttingRecipeJsonBuilder.createStonecutting(
				                             Ingredient.ofItem(Blocks.MOSSY_STONE_BRICKS),
				                             RecipeCategory.BUILDING_BLOCKS,
				                             Blocks.MOSSY_STONE_BRICK_SLAB,
				                             2
		                             )
		                             .criterion(
				                             "has_mossy_stone_bricks",
				                             conditionsFromItem(Blocks.MOSSY_STONE_BRICKS)
		                             )
		                             .offerTo(
				                             exporter,
				                             "mossy_stone_brick_slab_from_mossy_stone_brick_stonecutting"
		                             );
		StonecuttingRecipeJsonBuilder.createStonecutting(
				                             Ingredient.ofItem(Blocks.MOSSY_STONE_BRICKS),
				                             RecipeCategory.BUILDING_BLOCKS,
				                             Blocks.MOSSY_STONE_BRICK_STAIRS
		                             )
		                             .criterion(
				                             "has_mossy_stone_bricks",
				                             conditionsFromItem(Blocks.MOSSY_STONE_BRICKS)
		                             )
		                             .offerTo(
				                             exporter,
				                             "mossy_stone_brick_stairs_from_mossy_stone_brick_stonecutting"
		                             );
		StonecuttingRecipeJsonBuilder
				.createStonecutting(
						Ingredient.ofItem(Blocks.MOSSY_STONE_BRICKS),
						RecipeCategory.DECORATIONS,
						Blocks.MOSSY_STONE_BRICK_WALL
				)
				.criterion("has_mossy_stone_bricks", conditionsFromItem(Blocks.MOSSY_STONE_BRICKS))
				.offerTo(exporter, "mossy_stone_brick_wall_from_mossy_stone_brick_stonecutting");
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.MOSSY_COBBLESTONE_SLAB,
				Blocks.MOSSY_COBBLESTONE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.MOSSY_COBBLESTONE_STAIRS,
				Blocks.MOSSY_COBBLESTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.DECORATIONS,
				Blocks.MOSSY_COBBLESTONE_WALL,
				Blocks.MOSSY_COBBLESTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.SMOOTH_SANDSTONE_SLAB,
				Blocks.SMOOTH_SANDSTONE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.SMOOTH_SANDSTONE_STAIRS,
				Blocks.SMOOTH_SANDSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.SMOOTH_RED_SANDSTONE_SLAB,
				Blocks.SMOOTH_RED_SANDSTONE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.SMOOTH_RED_SANDSTONE_STAIRS,
				Blocks.SMOOTH_RED_SANDSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.SMOOTH_QUARTZ_SLAB,
				Blocks.SMOOTH_QUARTZ,
				2
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_QUARTZ_STAIRS, Blocks.SMOOTH_QUARTZ);
		StonecuttingRecipeJsonBuilder.createStonecutting(
				                             Ingredient.ofItem(Blocks.END_STONE_BRICKS),
				                             RecipeCategory.BUILDING_BLOCKS,
				                             Blocks.END_STONE_BRICK_SLAB,
				                             2
		                             )
		                             .criterion("has_end_stone_brick", conditionsFromItem(Blocks.END_STONE_BRICKS))
		                             .offerTo(exporter, "end_stone_brick_slab_from_end_stone_brick_stonecutting");
		StonecuttingRecipeJsonBuilder.createStonecutting(
				                             Ingredient.ofItem(Blocks.END_STONE_BRICKS),
				                             RecipeCategory.BUILDING_BLOCKS,
				                             Blocks.END_STONE_BRICK_STAIRS
		                             )
		                             .criterion("has_end_stone_brick", conditionsFromItem(Blocks.END_STONE_BRICKS))
		                             .offerTo(
				                             exporter,
				                             "end_stone_brick_stairs_from_end_stone_brick_stonecutting"
		                             );
		StonecuttingRecipeJsonBuilder
				.createStonecutting(
						Ingredient.ofItem(Blocks.END_STONE_BRICKS),
						RecipeCategory.DECORATIONS,
						Blocks.END_STONE_BRICK_WALL
				)
				.criterion("has_end_stone_brick", conditionsFromItem(Blocks.END_STONE_BRICKS))
				.offerTo(exporter, "end_stone_brick_wall_from_end_stone_brick_stonecutting");
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.END_STONE_BRICKS, Blocks.END_STONE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.END_STONE_BRICK_SLAB, Blocks.END_STONE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.END_STONE_BRICK_STAIRS, Blocks.END_STONE);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.END_STONE_BRICK_WALL, Blocks.END_STONE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_STONE_SLAB, Blocks.SMOOTH_STONE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.BLACKSTONE_SLAB, Blocks.BLACKSTONE, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.BLACKSTONE_STAIRS, Blocks.BLACKSTONE);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.BLACKSTONE_WALL, Blocks.BLACKSTONE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_BLACKSTONE, Blocks.BLACKSTONE);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.POLISHED_BLACKSTONE_WALL, Blocks.BLACKSTONE);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_SLAB,
				Blocks.BLACKSTONE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_STAIRS,
				Blocks.BLACKSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.CHISELED_POLISHED_BLACKSTONE,
				Blocks.BLACKSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_BRICKS,
				Blocks.BLACKSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_BRICK_SLAB,
				Blocks.BLACKSTONE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
				Blocks.BLACKSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.DECORATIONS,
				Blocks.POLISHED_BLACKSTONE_BRICK_WALL,
				Blocks.BLACKSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_SLAB,
				Blocks.POLISHED_BLACKSTONE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_STAIRS,
				Blocks.POLISHED_BLACKSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_BRICKS,
				Blocks.POLISHED_BLACKSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.DECORATIONS,
				Blocks.POLISHED_BLACKSTONE_WALL,
				Blocks.POLISHED_BLACKSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_BRICK_SLAB,
				Blocks.POLISHED_BLACKSTONE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
				Blocks.POLISHED_BLACKSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.DECORATIONS,
				Blocks.POLISHED_BLACKSTONE_BRICK_WALL,
				Blocks.POLISHED_BLACKSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.CHISELED_POLISHED_BLACKSTONE,
				Blocks.POLISHED_BLACKSTONE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_BRICK_SLAB,
				Blocks.POLISHED_BLACKSTONE_BRICKS,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
				Blocks.POLISHED_BLACKSTONE_BRICKS
		);
		offerStonecuttingRecipe(
				RecipeCategory.DECORATIONS,
				Blocks.POLISHED_BLACKSTONE_BRICK_WALL,
				Blocks.POLISHED_BLACKSTONE_BRICKS
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_COPPER_SLAB, Blocks.CUT_COPPER, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_COPPER_STAIRS, Blocks.CUT_COPPER);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.EXPOSED_CUT_COPPER_SLAB,
				Blocks.EXPOSED_CUT_COPPER,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.EXPOSED_CUT_COPPER_STAIRS,
				Blocks.EXPOSED_CUT_COPPER
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WEATHERED_CUT_COPPER_SLAB,
				Blocks.WEATHERED_CUT_COPPER,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WEATHERED_CUT_COPPER_STAIRS,
				Blocks.WEATHERED_CUT_COPPER
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.OXIDIZED_CUT_COPPER_SLAB,
				Blocks.OXIDIZED_CUT_COPPER,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.OXIDIZED_CUT_COPPER_STAIRS,
				Blocks.OXIDIZED_CUT_COPPER
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_CUT_COPPER_SLAB,
				Blocks.WAXED_CUT_COPPER,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_CUT_COPPER_STAIRS,
				Blocks.WAXED_CUT_COPPER
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB,
				Blocks.WAXED_EXPOSED_CUT_COPPER,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS,
				Blocks.WAXED_EXPOSED_CUT_COPPER
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB,
				Blocks.WAXED_WEATHERED_CUT_COPPER,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS,
				Blocks.WAXED_WEATHERED_CUT_COPPER
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB,
				Blocks.WAXED_OXIDIZED_CUT_COPPER,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS,
				Blocks.WAXED_OXIDIZED_CUT_COPPER
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_COPPER, Blocks.COPPER_BLOCK, 4);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_COPPER_STAIRS, Blocks.COPPER_BLOCK, 4);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_COPPER_SLAB, Blocks.COPPER_BLOCK, 8);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.EXPOSED_CUT_COPPER,
				Blocks.EXPOSED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.EXPOSED_CUT_COPPER_STAIRS,
				Blocks.EXPOSED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.EXPOSED_CUT_COPPER_SLAB,
				Blocks.EXPOSED_COPPER,
				8
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WEATHERED_CUT_COPPER,
				Blocks.WEATHERED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WEATHERED_CUT_COPPER_STAIRS,
				Blocks.WEATHERED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WEATHERED_CUT_COPPER_SLAB,
				Blocks.WEATHERED_COPPER,
				8
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.OXIDIZED_CUT_COPPER,
				Blocks.OXIDIZED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.OXIDIZED_CUT_COPPER_STAIRS,
				Blocks.OXIDIZED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.OXIDIZED_CUT_COPPER_SLAB,
				Blocks.OXIDIZED_COPPER,
				8
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_CUT_COPPER,
				Blocks.WAXED_COPPER_BLOCK,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_CUT_COPPER_STAIRS,
				Blocks.WAXED_COPPER_BLOCK,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_CUT_COPPER_SLAB,
				Blocks.WAXED_COPPER_BLOCK,
				8
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_EXPOSED_CUT_COPPER,
				Blocks.WAXED_EXPOSED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS,
				Blocks.WAXED_EXPOSED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB,
				Blocks.WAXED_EXPOSED_COPPER,
				8
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_WEATHERED_CUT_COPPER,
				Blocks.WAXED_WEATHERED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS,
				Blocks.WAXED_WEATHERED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB,
				Blocks.WAXED_WEATHERED_COPPER,
				8
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_OXIDIZED_CUT_COPPER,
				Blocks.WAXED_OXIDIZED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS,
				Blocks.WAXED_OXIDIZED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB,
				Blocks.WAXED_OXIDIZED_COPPER,
				8
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.COBBLED_DEEPSLATE_SLAB,
				Blocks.COBBLED_DEEPSLATE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.COBBLED_DEEPSLATE_STAIRS,
				Blocks.COBBLED_DEEPSLATE
		);
		offerStonecuttingRecipe(
				RecipeCategory.DECORATIONS,
				Blocks.COBBLED_DEEPSLATE_WALL,
				Blocks.COBBLED_DEEPSLATE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.CHISELED_DEEPSLATE,
				Blocks.COBBLED_DEEPSLATE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_DEEPSLATE,
				Blocks.COBBLED_DEEPSLATE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_DEEPSLATE_SLAB,
				Blocks.COBBLED_DEEPSLATE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_DEEPSLATE_STAIRS,
				Blocks.COBBLED_DEEPSLATE
		);
		offerStonecuttingRecipe(
				RecipeCategory.DECORATIONS,
				Blocks.POLISHED_DEEPSLATE_WALL,
				Blocks.COBBLED_DEEPSLATE
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.DEEPSLATE_BRICKS, Blocks.COBBLED_DEEPSLATE);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_BRICK_SLAB,
				Blocks.COBBLED_DEEPSLATE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_BRICK_STAIRS,
				Blocks.COBBLED_DEEPSLATE
		);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.DEEPSLATE_BRICK_WALL, Blocks.COBBLED_DEEPSLATE);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.DEEPSLATE_TILES, Blocks.COBBLED_DEEPSLATE);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_TILE_SLAB,
				Blocks.COBBLED_DEEPSLATE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_TILE_STAIRS,
				Blocks.COBBLED_DEEPSLATE
		);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.DEEPSLATE_TILE_WALL, Blocks.COBBLED_DEEPSLATE);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_DEEPSLATE_SLAB,
				Blocks.POLISHED_DEEPSLATE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_DEEPSLATE_STAIRS,
				Blocks.POLISHED_DEEPSLATE
		);
		offerStonecuttingRecipe(
				RecipeCategory.DECORATIONS,
				Blocks.POLISHED_DEEPSLATE_WALL,
				Blocks.POLISHED_DEEPSLATE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_BRICKS,
				Blocks.POLISHED_DEEPSLATE
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_BRICK_SLAB,
				Blocks.POLISHED_DEEPSLATE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_BRICK_STAIRS,
				Blocks.POLISHED_DEEPSLATE
		);
		offerStonecuttingRecipe(
				RecipeCategory.DECORATIONS,
				Blocks.DEEPSLATE_BRICK_WALL,
				Blocks.POLISHED_DEEPSLATE
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_TILE_SLAB,
				Blocks.POLISHED_DEEPSLATE,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_TILE_STAIRS,
				Blocks.POLISHED_DEEPSLATE
		);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.DEEPSLATE_TILE_WALL, Blocks.POLISHED_DEEPSLATE);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_BRICK_SLAB,
				Blocks.DEEPSLATE_BRICKS,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_BRICK_STAIRS,
				Blocks.DEEPSLATE_BRICKS
		);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.DEEPSLATE_BRICK_WALL, Blocks.DEEPSLATE_BRICKS);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_TILE_SLAB,
				Blocks.DEEPSLATE_BRICKS,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_TILE_STAIRS,
				Blocks.DEEPSLATE_BRICKS
		);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.DEEPSLATE_TILE_WALL, Blocks.DEEPSLATE_BRICKS);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_TILE_SLAB,
				Blocks.DEEPSLATE_TILES,
				2
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.DEEPSLATE_TILE_STAIRS,
				Blocks.DEEPSLATE_TILES
		);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.DEEPSLATE_TILE_WALL, Blocks.DEEPSLATE_TILES);
		streamSmithingTemplates().forEach(template -> offerSmithingTrimRecipe(
				template.template(),
				template.patternId(),
				template.recipeId()
		));
		offerNetheriteUpgradeRecipe(Items.DIAMOND_CHESTPLATE, RecipeCategory.COMBAT, Items.NETHERITE_CHESTPLATE);
		offerNetheriteUpgradeRecipe(Items.DIAMOND_LEGGINGS, RecipeCategory.COMBAT, Items.NETHERITE_LEGGINGS);
		offerNetheriteUpgradeRecipe(Items.DIAMOND_HELMET, RecipeCategory.COMBAT, Items.NETHERITE_HELMET);
		offerNetheriteUpgradeRecipe(Items.DIAMOND_BOOTS, RecipeCategory.COMBAT, Items.NETHERITE_BOOTS);
		offerNetheriteUpgradeRecipe(
				Items.DIAMOND_NAUTILUS_ARMOR,
				RecipeCategory.COMBAT,
				Items.NETHERITE_NAUTILUS_ARMOR
		);
		offerNetheriteUpgradeRecipe(Items.DIAMOND_HORSE_ARMOR, RecipeCategory.COMBAT, Items.NETHERITE_HORSE_ARMOR);
		offerNetheriteUpgradeRecipe(Items.DIAMOND_SWORD, RecipeCategory.COMBAT, Items.NETHERITE_SWORD);
		offerNetheriteUpgradeRecipe(Items.DIAMOND_SPEAR, RecipeCategory.COMBAT, Items.NETHERITE_SPEAR);
		offerNetheriteUpgradeRecipe(Items.DIAMOND_AXE, RecipeCategory.TOOLS, Items.NETHERITE_AXE);
		offerNetheriteUpgradeRecipe(Items.DIAMOND_PICKAXE, RecipeCategory.TOOLS, Items.NETHERITE_PICKAXE);
		offerNetheriteUpgradeRecipe(Items.DIAMOND_HOE, RecipeCategory.TOOLS, Items.NETHERITE_HOE);
		offerNetheriteUpgradeRecipe(Items.DIAMOND_SHOVEL, RecipeCategory.TOOLS, Items.NETHERITE_SHOVEL);
		offerSmithingTemplateCopyingRecipe(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.NETHERRACK);
		offerSmithingTemplateCopyingRecipe(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, Items.COBBLESTONE);
		offerSmithingTemplateCopyingRecipe(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SANDSTONE);
		offerSmithingTemplateCopyingRecipe(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.COBBLESTONE);
		offerSmithingTemplateCopyingRecipe(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.MOSSY_COBBLESTONE);
		offerSmithingTemplateCopyingRecipe(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.COBBLED_DEEPSLATE);
		offerSmithingTemplateCopyingRecipe(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.END_STONE);
		offerSmithingTemplateCopyingRecipe(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, Items.COBBLESTONE);
		offerSmithingTemplateCopyingRecipe(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.PRISMARINE);
		offerSmithingTemplateCopyingRecipe(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, Items.BLACKSTONE);
		offerSmithingTemplateCopyingRecipe(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, Items.NETHERRACK);
		offerSmithingTemplateCopyingRecipe(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.PURPUR_BLOCK);
		offerSmithingTemplateCopyingRecipe(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.COBBLED_DEEPSLATE);
		offerSmithingTemplateCopyingRecipe(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TERRACOTTA);
		offerSmithingTemplateCopyingRecipe(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TERRACOTTA);
		offerSmithingTemplateCopyingRecipe(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TERRACOTTA);
		offerSmithingTemplateCopyingRecipe(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TERRACOTTA);
		offerSmithingTemplateCopyingRecipe(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, Items.BREEZE_ROD);
		offerSmithingTemplateCopyingRecipe(
				Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
				Ingredient.ofItems(Items.COPPER_BLOCK, Items.WAXED_COPPER_BLOCK)
		);
		offerCompactingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.BAMBOO_BLOCK, Items.BAMBOO);
		offerPlanksRecipe(Blocks.BAMBOO_PLANKS, ItemTags.BAMBOO_BLOCKS, 2);
		offerMosaicRecipe(RecipeCategory.DECORATIONS, Blocks.BAMBOO_MOSAIC, Blocks.BAMBOO_SLAB);
		offerBoatRecipe(Items.BAMBOO_RAFT, Blocks.BAMBOO_PLANKS);
		offerChestBoatRecipe(Items.BAMBOO_CHEST_RAFT, Items.BAMBOO_RAFT);
		offerHangingSignRecipe(Items.OAK_HANGING_SIGN, Blocks.STRIPPED_OAK_LOG);
		offerHangingSignRecipe(Items.SPRUCE_HANGING_SIGN, Blocks.STRIPPED_SPRUCE_LOG);
		offerHangingSignRecipe(Items.BIRCH_HANGING_SIGN, Blocks.STRIPPED_BIRCH_LOG);
		offerHangingSignRecipe(Items.JUNGLE_HANGING_SIGN, Blocks.STRIPPED_JUNGLE_LOG);
		offerHangingSignRecipe(Items.ACACIA_HANGING_SIGN, Blocks.STRIPPED_ACACIA_LOG);
		offerHangingSignRecipe(Items.CHERRY_HANGING_SIGN, Blocks.STRIPPED_CHERRY_LOG);
		offerHangingSignRecipe(Items.DARK_OAK_HANGING_SIGN, Blocks.STRIPPED_DARK_OAK_LOG);
		offerHangingSignRecipe(Items.PALE_OAK_HANGING_SIGN, Blocks.STRIPPED_PALE_OAK_LOG);
		offerHangingSignRecipe(Items.MANGROVE_HANGING_SIGN, Blocks.STRIPPED_MANGROVE_LOG);
		offerHangingSignRecipe(Items.BAMBOO_HANGING_SIGN, Items.STRIPPED_BAMBOO_BLOCK);
		offerHangingSignRecipe(Items.CRIMSON_HANGING_SIGN, Blocks.STRIPPED_CRIMSON_STEM);
		offerHangingSignRecipe(Items.WARPED_HANGING_SIGN, Blocks.STRIPPED_WARPED_STEM);
		createShaped(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_BOOKSHELF)
		    .input('#', ItemTags.PLANKS)
		    .input('X', ItemTags.WOODEN_SLABS)
		    .pattern("###")
		    .pattern("XXX")
		    .pattern("###")
		    .criterion("has_book", conditionsFromItem(Items.BOOK))
		    .offerTo(exporter);
		offerSingleOutputShapelessRecipe(Items.ORANGE_DYE, Blocks.TORCHFLOWER, "orange_dye");
		offerShapelessRecipe(Items.CYAN_DYE, Blocks.PITCHER_PLANT, "cyan_dye", 2);
		offerPlanksRecipe2(Blocks.CHERRY_PLANKS, ItemTags.CHERRY_LOGS, 4);
		offerBarkBlockRecipe(Blocks.CHERRY_WOOD, Blocks.CHERRY_LOG);
		offerBarkBlockRecipe(Blocks.STRIPPED_CHERRY_WOOD, Blocks.STRIPPED_CHERRY_LOG);
		offerBoatRecipe(Items.CHERRY_BOAT, Blocks.CHERRY_PLANKS);
		offerChestBoatRecipe(Items.CHERRY_CHEST_BOAT, Items.CHERRY_BOAT);
		offerShapelessRecipe(Items.PINK_DYE, Items.PINK_PETALS, "pink_dye", 1);
		createShaped(RecipeCategory.TOOLS, Items.BRUSH)
		    .input('X', Items.FEATHER)
		    .input('#', Items.COPPER_INGOT)
		    .input('I', Items.STICK)
		    .pattern("X")
		    .pattern("#")
		    .pattern("I")
		    .criterion("has_copper_ingot", conditionsFromItem(Items.COPPER_INGOT))
		    .offerTo(exporter);
		createShaped(RecipeCategory.DECORATIONS, Items.DECORATED_POT)
		    .input('#', Items.BRICK)
		    .pattern(" # ")
		    .pattern("# #")
		    .pattern(" # ")
		    .criterion("has_brick", conditionsFromTag(ItemTags.DECORATED_POT_INGREDIENTS))
		    .offerTo(exporter, "decorated_pot_simple");
		ComplexRecipeJsonBuilder.create(CraftingDecoratedPotRecipe::new).offerTo(exporter, "decorated_pot");
		createShaped(RecipeCategory.REDSTONE, Blocks.CRAFTER)
		    .input('#', Items.IRON_INGOT)
		    .input('C', Items.CRAFTING_TABLE)
		    .input('R', Items.REDSTONE)
		    .input('D', Items.DROPPER)
		    .pattern("###")
		    .pattern("#C#")
		    .pattern("RDR")
		    .criterion("has_dropper", conditionsFromItem(Items.DROPPER))
		    .offerTo(exporter);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.TUFF_SLAB, Blocks.TUFF, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.TUFF_STAIRS, Blocks.TUFF);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.TUFF_WALL, Blocks.TUFF);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_TUFF, Blocks.TUFF);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_TUFF, Blocks.TUFF);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_TUFF_SLAB, Blocks.TUFF, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_TUFF_STAIRS, Blocks.TUFF);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.POLISHED_TUFF_WALL, Blocks.TUFF);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.TUFF_BRICKS, Blocks.TUFF);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.TUFF_BRICK_SLAB, Blocks.TUFF, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.TUFF_BRICK_STAIRS, Blocks.TUFF);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.TUFF_BRICK_WALL, Blocks.TUFF);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_TUFF_BRICKS, Blocks.TUFF);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.POLISHED_TUFF_SLAB,
				Blocks.POLISHED_TUFF,
				2
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_TUFF_STAIRS, Blocks.POLISHED_TUFF);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.POLISHED_TUFF_WALL, Blocks.POLISHED_TUFF);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.TUFF_BRICKS, Blocks.POLISHED_TUFF);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.TUFF_BRICK_SLAB, Blocks.POLISHED_TUFF, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.TUFF_BRICK_STAIRS, Blocks.POLISHED_TUFF);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.TUFF_BRICK_WALL, Blocks.POLISHED_TUFF);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_TUFF_BRICKS, Blocks.POLISHED_TUFF);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.TUFF_BRICK_SLAB, Blocks.TUFF_BRICKS, 2);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.TUFF_BRICK_STAIRS, Blocks.TUFF_BRICKS);
		offerStonecuttingRecipe(RecipeCategory.DECORATIONS, Blocks.TUFF_BRICK_WALL, Blocks.TUFF_BRICKS);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_TUFF_BRICKS, Blocks.TUFF_BRICKS);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_COPPER, Blocks.COPPER_BLOCK, 4);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.EXPOSED_CHISELED_COPPER,
				Blocks.EXPOSED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WEATHERED_CHISELED_COPPER,
				Blocks.WEATHERED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.OXIDIZED_CHISELED_COPPER,
				Blocks.OXIDIZED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_CHISELED_COPPER,
				Blocks.WAXED_COPPER_BLOCK,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_EXPOSED_CHISELED_COPPER,
				Blocks.WAXED_EXPOSED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_WEATHERED_CHISELED_COPPER,
				Blocks.WAXED_WEATHERED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_OXIDIZED_CHISELED_COPPER,
				Blocks.WAXED_OXIDIZED_COPPER,
				4
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_COPPER, Blocks.CUT_COPPER, 1);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.EXPOSED_CHISELED_COPPER,
				Blocks.EXPOSED_CUT_COPPER,
				1
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WEATHERED_CHISELED_COPPER,
				Blocks.WEATHERED_CUT_COPPER,
				1
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.OXIDIZED_CHISELED_COPPER,
				Blocks.OXIDIZED_CUT_COPPER,
				1
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_CHISELED_COPPER,
				Blocks.WAXED_CUT_COPPER,
				1
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_EXPOSED_CHISELED_COPPER,
				Blocks.WAXED_EXPOSED_CUT_COPPER,
				1
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_WEATHERED_CHISELED_COPPER,
				Blocks.WAXED_WEATHERED_CUT_COPPER,
				1
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_OXIDIZED_CHISELED_COPPER,
				Blocks.WAXED_OXIDIZED_CUT_COPPER,
				1
		);
		offerGrateRecipe(Blocks.COPPER_GRATE, Blocks.COPPER_BLOCK);
		offerGrateRecipe(Blocks.EXPOSED_COPPER_GRATE, Blocks.EXPOSED_COPPER);
		offerGrateRecipe(Blocks.WEATHERED_COPPER_GRATE, Blocks.WEATHERED_COPPER);
		offerGrateRecipe(Blocks.OXIDIZED_COPPER_GRATE, Blocks.OXIDIZED_COPPER);
		offerGrateRecipe(Blocks.WAXED_COPPER_GRATE, Blocks.WAXED_COPPER_BLOCK);
		offerGrateRecipe(Blocks.WAXED_EXPOSED_COPPER_GRATE, Blocks.WAXED_EXPOSED_COPPER);
		offerGrateRecipe(Blocks.WAXED_WEATHERED_COPPER_GRATE, Blocks.WAXED_WEATHERED_COPPER);
		offerGrateRecipe(Blocks.WAXED_OXIDIZED_COPPER_GRATE, Blocks.WAXED_OXIDIZED_COPPER);
		offerBulbRecipe(Blocks.COPPER_BULB, Blocks.COPPER_BLOCK);
		offerBulbRecipe(Blocks.EXPOSED_COPPER_BULB, Blocks.EXPOSED_COPPER);
		offerBulbRecipe(Blocks.WEATHERED_COPPER_BULB, Blocks.WEATHERED_COPPER);
		offerBulbRecipe(Blocks.OXIDIZED_COPPER_BULB, Blocks.OXIDIZED_COPPER);
		offerBulbRecipe(Blocks.WAXED_COPPER_BULB, Blocks.WAXED_COPPER_BLOCK);
		offerBulbRecipe(Blocks.WAXED_EXPOSED_COPPER_BULB, Blocks.WAXED_EXPOSED_COPPER);
		offerBulbRecipe(Blocks.WAXED_WEATHERED_COPPER_BULB, Blocks.WAXED_WEATHERED_COPPER);
		offerBulbRecipe(Blocks.WAXED_OXIDIZED_COPPER_BULB, Blocks.WAXED_OXIDIZED_COPPER);
		offerWaxedChiseledCopperRecipe(Blocks.WAXED_CHISELED_COPPER, Blocks.WAXED_CUT_COPPER_SLAB);
		offerWaxedChiseledCopperRecipe(Blocks.WAXED_EXPOSED_CHISELED_COPPER, Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB);
		offerWaxedChiseledCopperRecipe(
				Blocks.WAXED_WEATHERED_CHISELED_COPPER,
				Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB
		);
		offerWaxedChiseledCopperRecipe(
				Blocks.WAXED_OXIDIZED_CHISELED_COPPER,
				Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB
		);
		offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, Blocks.COPPER_GRATE, Blocks.COPPER_BLOCK, 4);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.EXPOSED_COPPER_GRATE,
				Blocks.EXPOSED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WEATHERED_COPPER_GRATE,
				Blocks.WEATHERED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.OXIDIZED_COPPER_GRATE,
				Blocks.OXIDIZED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_COPPER_GRATE,
				Blocks.WAXED_COPPER_BLOCK,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_EXPOSED_COPPER_GRATE,
				Blocks.WAXED_EXPOSED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_WEATHERED_COPPER_GRATE,
				Blocks.WAXED_WEATHERED_COPPER,
				4
		);
		offerStonecuttingRecipe(
				RecipeCategory.BUILDING_BLOCKS,
				Blocks.WAXED_OXIDIZED_COPPER_GRATE,
				Blocks.WAXED_OXIDIZED_COPPER,
				4
		);
		createShapeless(RecipeCategory.MISC, Items.WIND_CHARGE, 4)
		    .input(Items.BREEZE_ROD)
		    .criterion("has_breeze_rod", conditionsFromItem(Items.BREEZE_ROD))
		    .offerTo(exporter);
		createShaped(RecipeCategory.COMBAT, Items.MACE, 1)
		    .input('I', Items.BREEZE_ROD)
		    .input('#', Blocks.HEAVY_CORE)
		    .pattern(" # ")
		    .pattern(" I ")
		    .criterion("has_breeze_rod", conditionsFromItem(Items.BREEZE_ROD))
		    .criterion("has_heavy_core", conditionsFromItem(Blocks.HEAVY_CORE))
		    .offerTo(exporter);
		createDoorRecipe(Blocks.COPPER_DOOR, Ingredient.ofItem(Items.COPPER_INGOT))
		    .criterion(hasItem(Items.COPPER_INGOT), conditionsFromItem(Items.COPPER_INGOT))
		    .offerTo(exporter);
		offer2x2CompactingRecipe(RecipeCategory.REDSTONE, Blocks.COPPER_TRAPDOOR, Items.COPPER_INGOT);
		createShaped(RecipeCategory.TOOLS, Items.BUNDLE)
		    .input('-', Items.STRING)
		    .input('#', Items.LEATHER)
		    .pattern("-")
		    .pattern("#")
		    .criterion("has_string", conditionsFromItem(Items.STRING))
		    .offerTo(exporter);
		generateDyedBundles();
	}

	public static Stream<VanillaRecipeGenerator.SmithingTemplate> streamSmithingTemplates() {
		return Stream.of(
				             Pair.of(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.BOLT),
				             Pair.of(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.COAST),
				             Pair.of(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.DUNE),
				             Pair.of(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.EYE),
				             Pair.of(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.FLOW),
				             Pair.of(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.HOST),
				             Pair.of(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.RAISER),
				             Pair.of(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.RIB),
				             Pair.of(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.SENTRY),
				             Pair.of(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.SHAPER),
				             Pair.of(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.SILENCE),
				             Pair.of(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.SNOUT),
				             Pair.of(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.SPIRE),
				             Pair.of(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.TIDE),
				             Pair.of(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.VEX),
				             Pair.of(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.WARD),
				             Pair.of(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.WAYFINDER),
				             Pair.of(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.WILD)
		             )
		             .map(pair -> {
			             Item item = (Item) pair.getFirst();
			             RegistryKey<ArmorTrimPattern> registryKey = (RegistryKey<ArmorTrimPattern>) pair.getSecond();
			             RegistryKey<Recipe<?>>
					             registryKey2 =
					             RegistryKey.of(
							             RegistryKeys.RECIPE,
							             Identifier.ofVanilla(getItemPath(item) + "_smithing_trim")
					             );
			             return new VanillaRecipeGenerator.SmithingTemplate(item, registryKey, registryKey2);
		             });
	}

	private void generateDyedShulkerBoxes() {
		Ingredient ingredient = ingredientFromTag(ItemTags.SHULKER_BOXES);

		for (DyeColor dyeColor : DyeColor.values()) {
			TransmuteRecipeJsonBuilder.create(
					                          RecipeCategory.DECORATIONS,
					                          ingredient,
					                          Ingredient.ofItem(DyeItem.byColor(dyeColor)),
					                          ShulkerBoxBlock.get(dyeColor).asItem()
			                          )
			                          .group("shulker_box_dye")
			                          .criterion("has_shulker_box", conditionsFromTag(ItemTags.SHULKER_BOXES))
			                          .offerTo(exporter);
		}
	}

	private void generateDyedBundles() {
		Ingredient ingredient = ingredientFromTag(ItemTags.BUNDLES);

		for (DyeColor dyeColor : DyeColor.values()) {
			DyeItem dyeItem = DyeItem.byColor(dyeColor);
			TransmuteRecipeJsonBuilder
					.create(
							RecipeCategory.TOOLS,
							ingredient,
							Ingredient.ofItem(dyeItem),
							BundleItem.getBundle(dyeColor)
					)
					.group("bundle_dye")
					.criterion(hasItem(dyeItem), conditionsFromItem(dyeItem))
					.offerTo(exporter);
		}
	}

	/**
	 * Точка входа генератора рецептов ванильного Minecraft для data-генерации.
	 * Регистрируется как {@link net.minecraft.data.DataProvider} и делегирует
	 * создание всех ванильных рецептов экземпляру {@link VanillaRecipeGenerator}.
	 */
	public static class Provider extends RecipeGenerator.RecipeProvider {

		public Provider(DataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
			super(dataOutput, completableFuture);
		}

		@Override
		protected RecipeGenerator getRecipeGenerator(
				RegistryWrapper.WrapperLookup registries,
				RecipeExporter exporter
		) {
			return new VanillaRecipeGenerator(registries, exporter);
		}

		@Override
		public String getName() {
			return "Vanilla Recipes";
		}
	}

	/**
	 * {@code SmithingTemplate}.
	 */
	public record SmithingTemplate(
			Item template,
			RegistryKey<ArmorTrimPattern> patternId,
			RegistryKey<Recipe<?>> recipeId
	) {
	}
}
