package net.minecraft.data.recipe;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.EnterBlockCriterion;
import net.minecraft.advancement.criterion.ImpossibleCriterion;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SuspiciousStewIngredient;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.data.family.BlockFamilies;
import net.minecraft.data.family.BlockFamily;
import net.minecraft.item.*;
import net.minecraft.item.equipment.trim.ArmorTrimPattern;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Базовый генератор рецептов крафта для системы датагенерации Minecraft.
 * Предоставляет утилитарные методы для создания рецептов всех типов (shaped, shapeless,
 * smelting, smithing и т.д.) и автоматической генерации рецептов для блочных семейств.
 */
public abstract class RecipeGenerator {

	protected final RegistryWrapper.WrapperLookup registries;
	private final RegistryEntryLookup<Item> itemLookup;
	protected final RecipeExporter exporter;
	private static final Map<BlockFamily.Variant, RecipeGenerator.BlockFamilyRecipeFactory> VARIANT_FACTORIES =
			ImmutableMap.<BlockFamily.Variant, RecipeGenerator.BlockFamilyRecipeFactory>builder()
			            .put(BlockFamily.Variant.BUTTON,
					            (gen, out, in) -> gen.createButtonRecipe(out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.CHISELED,
					            (gen, out, in) -> gen.createChiseledBlockRecipe(RecipeCategory.BUILDING_BLOCKS, out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.CUT,
					            (gen, out, in) -> gen.createCutCopperRecipe(RecipeCategory.BUILDING_BLOCKS, out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.DOOR,
					            (gen, out, in) -> gen.createDoorRecipe(out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.CUSTOM_FENCE,
					            (gen, out, in) -> gen.createFenceRecipe(out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.FENCE,
					            (gen, out, in) -> gen.createFenceRecipe(out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.CUSTOM_FENCE_GATE,
					            (gen, out, in) -> gen.createFenceGateRecipe(out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.FENCE_GATE,
					            (gen, out, in) -> gen.createFenceGateRecipe(out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.SIGN,
					            (gen, out, in) -> gen.createSignRecipe(out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.SLAB,
					            (gen, out, in) -> gen.createSlabRecipe(RecipeCategory.BUILDING_BLOCKS, out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.STAIRS,
					            (gen, out, in) -> gen.createStairsRecipe(out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.PRESSURE_PLATE,
					            (gen, out, in) -> gen.createPressurePlateRecipe(RecipeCategory.REDSTONE, out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.POLISHED,
					            (gen, out, in) -> gen.createCondensingRecipe(RecipeCategory.BUILDING_BLOCKS, out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.TRAPDOOR,
					            (gen, out, in) -> gen.createTrapdoorRecipe(out, Ingredient.ofItem(in)))
			            .put(BlockFamily.Variant.WALL,
					            (gen, out, in) -> gen.getWallRecipe(RecipeCategory.DECORATIONS, out, Ingredient.ofItem(in)))
			            .build();

	protected RecipeGenerator(RegistryWrapper.WrapperLookup registries, RecipeExporter exporter) {
		this.registries = registries;
		this.itemLookup = registries.getOrThrow(RegistryKeys.ITEM);
		this.exporter = exporter;
	}

	public abstract void generate();

	public void generateFamilies(FeatureSet enabledFeatures) {
		BlockFamilies
				.getFamilies()
				.filter(BlockFamily::shouldGenerateRecipes)
				.forEach(family -> generateFamily(family, enabledFeatures));
	}

	public void offerSingleOutputShapelessRecipe(
			ItemConvertible output,
			ItemConvertible input,
			@Nullable String group
	) {
		offerShapelessRecipe(output, input, group, 1);
	}

	public void offerShapelessRecipe(
			ItemConvertible output,
			ItemConvertible input,
			@Nullable String group,
			int outputCount
	) {
		createShapeless(RecipeCategory.MISC, output, outputCount)
		    .input(input)
		    .group(group)
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .offerTo(exporter, convertBetween(output, input));
	}

	public void offerSmelting(
			List<ItemConvertible> inputs,
			RecipeCategory category,
			ItemConvertible output,
			float experience,
			int cookingTime,
			String group
	) {
		offerMultipleOptions(
				RecipeSerializer.SMELTING,
				SmeltingRecipe::new,
				inputs,
				category,
				output,
				experience,
				cookingTime,
				group,
				"_from_smelting"
		);
	}

	public void offerBlasting(
			List<ItemConvertible> inputs,
			RecipeCategory category,
			ItemConvertible output,
			float experience,
			int cookingTime,
			String group
	) {
		offerMultipleOptions(
				RecipeSerializer.BLASTING,
				BlastingRecipe::new,
				inputs,
				category,
				output,
				experience,
				cookingTime,
				group,
				"_from_blasting"
		);
	}

	public final <T extends AbstractCookingRecipe> void offerMultipleOptions(
			RecipeSerializer<T> serializer,
			AbstractCookingRecipe.RecipeFactory<T> recipeFactory,
			List<ItemConvertible> inputs,
			RecipeCategory category,
			ItemConvertible output,
			float experience,
			int cookingTime,
			String group,
			String suffix
	) {
		for (ItemConvertible itemConvertible : inputs) {
			CookingRecipeJsonBuilder
					.create(
							Ingredient.ofItem(itemConvertible),
							category,
							output,
							experience,
							cookingTime,
							serializer,
							recipeFactory
					)
					.group(group)
					.criterion(hasItem(itemConvertible), conditionsFromItem(itemConvertible))
					.offerTo(exporter, getItemPath(output) + suffix + "_" + getItemPath(itemConvertible));
		}
	}

	public void offerNetheriteUpgradeRecipe(Item input, RecipeCategory category, Item result) {
		SmithingTransformRecipeJsonBuilder.create(
				                                  Ingredient.ofItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
				                                  Ingredient.ofItem(input),
				                                  ingredientFromTag(ItemTags.NETHERITE_TOOL_MATERIALS),
				                                  category,
				                                  result
		                                  )
		                                  .criterion(
				                                  "has_netherite_ingot",
				                                  conditionsFromTag(ItemTags.NETHERITE_TOOL_MATERIALS)
		                                  )
		                                  .offerTo(exporter, getItemPath(result) + "_smithing");
	}

	public void offerSmithingTrimRecipe(
			Item input,
			RegistryKey<ArmorTrimPattern> pattern,
			RegistryKey<Recipe<?>> recipeKey
	) {
		RegistryEntry.Reference<ArmorTrimPattern>
				reference =
				registries.getOrThrow(RegistryKeys.TRIM_PATTERN).getOrThrow(pattern);
		SmithingTrimRecipeJsonBuilder.create(
				                             Ingredient.ofItem(input),
				                             ingredientFromTag(ItemTags.TRIMMABLE_ARMOR),
				                             ingredientFromTag(ItemTags.TRIM_MATERIALS),
				                             reference,
				                             RecipeCategory.MISC
		                             )
		                             .criterion("has_smithing_trim_template", conditionsFromItem(input))
		                             .offerTo(exporter, recipeKey);
	}

	public void offer2x2CompactingRecipe(RecipeCategory category, ItemConvertible output, ItemConvertible input) {
		createShaped(category, output, 1)
		    .input('#', input)
		    .pattern("##")
		    .pattern("##")
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public void offerCompactingRecipe(
			RecipeCategory category,
			ItemConvertible output,
			ItemConvertible input,
			String criterionName
	) {
		this
				.createShapeless(category, output)
				.input(input, 9)
				.criterion(criterionName, conditionsFromItem(input))
				.offerTo(exporter);
	}

	public void offerCompactingRecipe(RecipeCategory category, ItemConvertible output, ItemConvertible input) {
		offerCompactingRecipe(category, output, input, hasItem(input));
	}

	public void offerPlanksRecipe2(ItemConvertible output, TagKey<Item> logTag, int count) {
		createShapeless(RecipeCategory.BUILDING_BLOCKS, output, count)
		    .input(logTag)
		    .group("planks")
		    .criterion("has_log", conditionsFromTag(logTag))
		    .offerTo(exporter);
	}

	public void offerPlanksRecipe(ItemConvertible output, TagKey<Item> logTag, int count) {
		createShapeless(RecipeCategory.BUILDING_BLOCKS, output, count)
		    .input(logTag)
		    .group("planks")
		    .criterion("has_logs", conditionsFromTag(logTag))
		    .offerTo(exporter);
	}

	public void offerBarkBlockRecipe(ItemConvertible output, ItemConvertible input) {
		createShaped(RecipeCategory.BUILDING_BLOCKS, output, 3)
		    .input('#', input)
		    .pattern("##")
		    .pattern("##")
		    .group("bark")
		    .criterion("has_log", conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public void offerBoatRecipe(ItemConvertible output, ItemConvertible input) {
		createShaped(RecipeCategory.TRANSPORTATION, output)
		    .input('#', input)
		    .pattern("# #")
		    .pattern("###")
		    .group("boat")
		    .criterion("in_water", requireEnteringFluid(Blocks.WATER))
		    .offerTo(exporter);
	}

	public void offerChestBoatRecipe(ItemConvertible output, ItemConvertible input) {
		createShapeless(RecipeCategory.TRANSPORTATION, output)
		    .input(Blocks.CHEST)
		    .input(input)
		    .group("chest_boat")
		    .criterion("has_boat", conditionsFromTag(ItemTags.BOATS))
		    .offerTo(exporter);
	}

	public final CraftingRecipeJsonBuilder createButtonRecipe(ItemConvertible output, Ingredient input) {
		return createShapeless(RecipeCategory.REDSTONE, output).input(input);
	}

	public CraftingRecipeJsonBuilder createDoorRecipe(ItemConvertible output, Ingredient input) {
		return this
				.createShaped(RecipeCategory.REDSTONE, output, 3)
				.input('#', input)
				.pattern("##")
				.pattern("##")
				.pattern("##");
	}

	public final CraftingRecipeJsonBuilder createFenceRecipe(ItemConvertible output, Ingredient input) {
		int i = output == Blocks.NETHER_BRICK_FENCE ? 6 : 3;
		Item item = output == Blocks.NETHER_BRICK_FENCE ? Items.NETHER_BRICK : Items.STICK;
		return this
				.createShaped(RecipeCategory.DECORATIONS, output, i)
				.input('W', input)
				.input('#', item)
				.pattern("W#W")
				.pattern("W#W");
	}

	public final CraftingRecipeJsonBuilder createFenceGateRecipe(ItemConvertible output, Ingredient input) {
		return this
				.createShaped(RecipeCategory.REDSTONE, output)
				.input('#', Items.STICK)
				.input('W', input)
				.pattern("#W#")
				.pattern("#W#");
	}

	public void offerPressurePlateRecipe(ItemConvertible output, ItemConvertible input) {
		createPressurePlateRecipe(RecipeCategory.REDSTONE, output, Ingredient.ofItem(input))
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public final CraftingRecipeJsonBuilder createPressurePlateRecipe(
			RecipeCategory category,
			ItemConvertible output,
			Ingredient input
	) {
		return createShaped(category, output).input('#', input).pattern("##");
	}

	public void offerSlabRecipe(RecipeCategory category, ItemConvertible output, ItemConvertible input) {
		this
				.createSlabRecipe(category, output, Ingredient.ofItem(input))
				.criterion(hasItem(input), conditionsFromItem(input))
				.offerTo(exporter);
	}

	public void offerShelfRecipe(ItemConvertible output, ItemConvertible input) {
		createShaped(RecipeCategory.DECORATIONS, output, 6)
		    .input('#', input)
		    .pattern("###")
		    .pattern("   ")
		    .pattern("###")
		    .group("shelf")
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public CraftingRecipeJsonBuilder createSlabRecipe(
			RecipeCategory category,
			ItemConvertible output,
			Ingredient input
	) {
		return createShaped(category, output, 6).input('#', input).pattern("###");
	}

	public CraftingRecipeJsonBuilder createStairsRecipe(ItemConvertible output, Ingredient input) {
		return this
				.createShaped(RecipeCategory.BUILDING_BLOCKS, output, 4)
				.input('#', input)
				.pattern("#  ")
				.pattern("## ")
				.pattern("###");
	}

	public CraftingRecipeJsonBuilder createTrapdoorRecipe(ItemConvertible output, Ingredient input) {
		return createShaped(RecipeCategory.REDSTONE, output, 2).input('#', input).pattern("###").pattern("###");
	}

	public final CraftingRecipeJsonBuilder createSignRecipe(ItemConvertible output, Ingredient input) {
		return createShaped(RecipeCategory.DECORATIONS, output, 3)
		           .group("sign")
		           .input('#', input)
		           .input('X', Items.STICK)
		           .pattern("###")
		           .pattern("###")
		           .pattern(" X ");
	}

	public void offerHangingSignRecipe(ItemConvertible output, ItemConvertible input) {
		createShaped(RecipeCategory.DECORATIONS, output, 6)
		    .group("hanging_sign")
		    .input('#', input)
		    .input('X', Items.IRON_CHAIN)
		    .pattern("X X")
		    .pattern("###")
		    .pattern("###")
		    .criterion("has_stripped_logs", conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public void offerDyeableRecipes(List<Item> dyes, List<Item> dyeables, String group, RecipeCategory category) {
		offerDyeablesRecipes(dyes, dyeables, null, group, category);
	}

	public void offerDyeablesRecipes(
			List<Item> dyes,
			List<Item> dyeables,
			@Nullable Item undyed,
			String group,
			RecipeCategory category
	) {
		for (int i = 0; i < dyes.size(); i++) {
			Item item = dyes.get(i);
			Item item2 = dyeables.get(i);
			Stream<Item> stream = dyeables.stream().filter(itemx -> !itemx.equals(item2));
			if (undyed != null) {
				stream = Stream.concat(stream, Stream.of(undyed));
			}

			createShapeless(category, item2)
			    .input(item)
			    .input(Ingredient.ofItems(stream))
			    .group(group)
			    .criterion("has_needed_dye", conditionsFromItem(item))
			    .offerTo(exporter, "dye_" + getItemPath(item2));
		}
	}

	public void offerCarpetRecipe(ItemConvertible output, ItemConvertible input) {
		createShaped(RecipeCategory.DECORATIONS, output, 3)
		    .input('#', input)
		    .pattern("##")
		    .group("carpet")
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public void offerBedRecipe(ItemConvertible output, ItemConvertible inputWool) {
		createShaped(RecipeCategory.DECORATIONS, output)
		    .input('#', inputWool)
		    .input('X', ItemTags.PLANKS)
		    .pattern("###")
		    .pattern("XXX")
		    .group("bed")
		    .criterion(hasItem(inputWool), conditionsFromItem(inputWool))
		    .offerTo(exporter);
	}

	public void offerBannerRecipe(ItemConvertible output, ItemConvertible inputWool) {
		createShaped(RecipeCategory.DECORATIONS, output)
		    .input('#', inputWool)
		    .input('|', Items.STICK)
		    .pattern("###")
		    .pattern("###")
		    .pattern(" | ")
		    .group("banner")
		    .criterion(hasItem(inputWool), conditionsFromItem(inputWool))
		    .offerTo(exporter);
	}

	public void offerStainedGlassDyeingRecipe(ItemConvertible output, ItemConvertible input) {
		createShaped(RecipeCategory.BUILDING_BLOCKS, output, 8)
		    .input('#', Blocks.GLASS)
		    .input('X', input)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .group("stained_glass")
		    .criterion("has_glass", conditionsFromItem(Blocks.GLASS))
		    .offerTo(exporter);
	}

	public void offerDriedGhast(ItemConvertible output) {
		createShaped(RecipeCategory.BUILDING_BLOCKS, output, 1)
		    .input('#', Items.GHAST_TEAR)
		    .input('X', Items.SOUL_SAND)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .group("dry_ghast")
		    .criterion(hasItem(Items.GHAST_TEAR), conditionsFromItem(Items.GHAST_TEAR))
		    .offerTo(exporter);
	}

	public void offerHarness(ItemConvertible output, ItemConvertible wool) {
		createShaped(RecipeCategory.COMBAT, output)
		    .input('#', wool)
		    .input('G', Items.GLASS)
		    .input('L', Items.LEATHER)
		    .pattern("LLL")
		    .pattern("G#G")
		    .group("harness")
		    .criterion("has_dried_ghast", conditionsFromItem(Blocks.DRIED_GHAST))
		    .offerTo(exporter);
	}

	public void offerStainedGlassPaneRecipe(ItemConvertible output, ItemConvertible input) {
		createShaped(RecipeCategory.DECORATIONS, output, 16)
		    .input('#', input)
		    .pattern("###")
		    .pattern("###")
		    .group("stained_glass_pane")
		    .criterion("has_glass", conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public void offerStainedGlassPaneDyeingRecipe(ItemConvertible output, ItemConvertible inputDye) {
		createShaped(RecipeCategory.DECORATIONS, output, 8)
		    .input('#', Blocks.GLASS_PANE)
		    .input('$', inputDye)
		    .pattern("###")
		    .pattern("#$#")
		    .pattern("###")
		    .group("stained_glass_pane")
		    .criterion("has_glass_pane", conditionsFromItem(Blocks.GLASS_PANE))
		    .criterion(hasItem(inputDye), conditionsFromItem(inputDye))
		    .offerTo(exporter, convertBetween(output, Blocks.GLASS_PANE));
	}

	public void offerTerracottaDyeingRecipe(ItemConvertible output, ItemConvertible input) {
		createShaped(RecipeCategory.BUILDING_BLOCKS, output, 8)
		    .input('#', Blocks.TERRACOTTA)
		    .input('X', input)
		    .pattern("###")
		    .pattern("#X#")
		    .pattern("###")
		    .group("stained_terracotta")
		    .criterion("has_terracotta", conditionsFromItem(Blocks.TERRACOTTA))
		    .offerTo(exporter);
	}

	public void offerConcretePowderDyeingRecipe(ItemConvertible output, ItemConvertible input) {
		createShapeless(RecipeCategory.BUILDING_BLOCKS, output, 8)
		    .input(input)
		    .input(Blocks.SAND, 4)
		    .input(Blocks.GRAVEL, 4)
		    .group("concrete_powder")
		    .criterion("has_sand", conditionsFromItem(Blocks.SAND))
		    .criterion("has_gravel", conditionsFromItem(Blocks.GRAVEL))
		    .offerTo(exporter);
	}

	public void offerCandleDyeingRecipe(ItemConvertible output, ItemConvertible input) {
		createShapeless(RecipeCategory.DECORATIONS, output)
		    .input(Blocks.CANDLE)
		    .input(input)
		    .group("dyed_candle")
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public void offerWallRecipe(RecipeCategory category, ItemConvertible output, ItemConvertible input) {
		this
				.getWallRecipe(category, output, Ingredient.ofItem(input))
				.criterion(hasItem(input), conditionsFromItem(input))
				.offerTo(exporter);
	}

	public final CraftingRecipeJsonBuilder getWallRecipe(
			RecipeCategory category,
			ItemConvertible output,
			Ingredient input
	) {
		return createShaped(category, output, 6).input('#', input).pattern("###").pattern("###");
	}

	public void offerPolishedStoneRecipe(RecipeCategory category, ItemConvertible output, ItemConvertible input) {
		this
				.createCondensingRecipe(category, output, Ingredient.ofItem(input))
				.criterion(hasItem(input), conditionsFromItem(input))
				.offerTo(exporter);
	}

	public final CraftingRecipeJsonBuilder createCondensingRecipe(
			RecipeCategory category,
			ItemConvertible output,
			Ingredient input
	) {
		return createShaped(category, output, 4).input('S', input).pattern("SS").pattern("SS");
	}

	public void offerCutCopperRecipe(RecipeCategory category, ItemConvertible output, ItemConvertible input) {
		this
				.createCutCopperRecipe(category, output, Ingredient.ofItem(input))
				.criterion(hasItem(input), conditionsFromItem(input))
				.offerTo(exporter);
	}

	public final ShapedRecipeJsonBuilder createCutCopperRecipe(
			RecipeCategory category,
			ItemConvertible output,
			Ingredient input
	) {
		return createShaped(category, output, 4).input('#', input).pattern("##").pattern("##");
	}

	public void offerChiseledBlockRecipe(RecipeCategory category, ItemConvertible output, ItemConvertible input) {
		createChiseledBlockRecipe(category, output, Ingredient.ofItem(input))
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public void offerMosaicRecipe(RecipeCategory category, ItemConvertible output, ItemConvertible input) {
		createShaped(category, output)
		    .input('#', input)
		    .pattern("#")
		    .pattern("#")
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public ShapedRecipeJsonBuilder createChiseledBlockRecipe(
			RecipeCategory category,
			ItemConvertible output,
			Ingredient input
	) {
		return createShaped(category, output).input('#', input).pattern("#").pattern("#");
	}

	public void offerStonecuttingRecipe(RecipeCategory category, ItemConvertible output, ItemConvertible input) {
		offerStonecuttingRecipe(category, output, input, 1);
	}

	public void offerStonecuttingRecipe(
			RecipeCategory category,
			ItemConvertible output,
			ItemConvertible input,
			int count
	) {
		StonecuttingRecipeJsonBuilder.createStonecutting(Ingredient.ofItem(input), category, output, count)
		                             .criterion(hasItem(input), conditionsFromItem(input))
		                             .offerTo(exporter, convertBetween(output, input) + "_stonecutting");
	}

	public final void offerCrackingRecipe(ItemConvertible output, ItemConvertible input) {
		CookingRecipeJsonBuilder
				.createSmelting(Ingredient.ofItem(input), RecipeCategory.BUILDING_BLOCKS, output, 0.1F, 200)
				.criterion(hasItem(input), conditionsFromItem(input))
				.offerTo(exporter);
	}

	public void offerReversibleCompactingRecipes(
			RecipeCategory reverseCategory,
			ItemConvertible baseItem,
			RecipeCategory compactingCategory,
			ItemConvertible compactItem
	) {
		offerReversibleCompactingRecipes(
				reverseCategory,
				baseItem,
				compactingCategory,
				compactItem,
				getRecipeName(compactItem),
				null,
				getRecipeName(baseItem),
				null
		);
	}

	public void offerReversibleCompactingRecipesWithCompactingRecipeGroup(
			RecipeCategory reverseCategory,
			ItemConvertible baseItem,
			RecipeCategory compactingCategory,
			ItemConvertible compactItem,
			String compactingId,
			String compactingGroup
	) {
		offerReversibleCompactingRecipes(
				reverseCategory,
				baseItem,
				compactingCategory,
				compactItem,
				compactingId,
				compactingGroup,
				getRecipeName(baseItem),
				null
		);
	}

	public void offerReversibleCompactingRecipesWithReverseRecipeGroup(
			RecipeCategory reverseCategory,
			ItemConvertible baseItem,
			RecipeCategory compactingCategory,
			ItemConvertible compactItem,
			String reverseId,
			String reverseGroup
	) {
		offerReversibleCompactingRecipes(
				reverseCategory,
				baseItem,
				compactingCategory,
				compactItem,
				getRecipeName(compactItem),
				null,
				reverseId,
				reverseGroup
		);
	}

	public final void offerReversibleCompactingRecipes(
			RecipeCategory reverseCategory,
			ItemConvertible baseItem,
			RecipeCategory compactingCategory,
			ItemConvertible compactItem,
			String compactingId,
			@Nullable String compactingGroup,
			String reverseId,
			@Nullable String reverseGroup
	) {
		createShapeless(reverseCategory, baseItem, 9)
		    .input(compactItem)
		    .group(reverseGroup)
		    .criterion(hasItem(compactItem), conditionsFromItem(compactItem))
		    .offerTo(exporter, RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(reverseId)));
		createShaped(compactingCategory, compactItem)
		    .input('#', baseItem)
		    .pattern("###")
		    .pattern("###")
		    .pattern("###")
		    .group(compactingGroup)
		    .criterion(hasItem(baseItem), conditionsFromItem(baseItem))
		    .offerTo(exporter, RegistryKey.of(RegistryKeys.RECIPE, Identifier.of(compactingId)));
	}

	public void offerSmithingTemplateCopyingRecipe(ItemConvertible template, ItemConvertible resource) {
		createShaped(RecipeCategory.MISC, template, 2)
		    .input('#', Items.DIAMOND)
		    .input('C', resource)
		    .input('S', template)
		    .pattern("#S#")
		    .pattern("#C#")
		    .pattern("###")
		    .criterion(hasItem(template), conditionsFromItem(template))
		    .offerTo(exporter);
	}

	public void offerSmithingTemplateCopyingRecipe(ItemConvertible template, Ingredient resource) {
		createShaped(RecipeCategory.MISC, template, 2)
		    .input('#', Items.DIAMOND)
		    .input('C', resource)
		    .input('S', template)
		    .pattern("#S#")
		    .pattern("#C#")
		    .pattern("###")
		    .criterion(hasItem(template), conditionsFromItem(template))
		    .offerTo(exporter);
	}

	public <T extends AbstractCookingRecipe> void generateCookingRecipes(
			String cooker,
			RecipeSerializer<T> serializer,
			AbstractCookingRecipe.RecipeFactory<T> recipeFactory,
			int cookingTime
	) {
		offerFoodCookingRecipe(
				cooker,
				serializer,
				recipeFactory,
				cookingTime,
				Items.BEEF,
				Items.COOKED_BEEF,
				0.35F
		);
		offerFoodCookingRecipe(
				cooker,
				serializer,
				recipeFactory,
				cookingTime,
				Items.CHICKEN,
				Items.COOKED_CHICKEN,
				0.35F
		);
		offerFoodCookingRecipe(cooker, serializer, recipeFactory, cookingTime, Items.COD, Items.COOKED_COD, 0.35F);
		offerFoodCookingRecipe(cooker, serializer, recipeFactory, cookingTime, Items.KELP, Items.DRIED_KELP, 0.1F);
		offerFoodCookingRecipe(
				cooker,
				serializer,
				recipeFactory,
				cookingTime,
				Items.SALMON,
				Items.COOKED_SALMON,
				0.35F
		);
		offerFoodCookingRecipe(
				cooker,
				serializer,
				recipeFactory,
				cookingTime,
				Items.MUTTON,
				Items.COOKED_MUTTON,
				0.35F
		);
		offerFoodCookingRecipe(
				cooker,
				serializer,
				recipeFactory,
				cookingTime,
				Items.PORKCHOP,
				Items.COOKED_PORKCHOP,
				0.35F
		);
		offerFoodCookingRecipe(
				cooker,
				serializer,
				recipeFactory,
				cookingTime,
				Items.POTATO,
				Items.BAKED_POTATO,
				0.35F
		);
		offerFoodCookingRecipe(
				cooker,
				serializer,
				recipeFactory,
				cookingTime,
				Items.RABBIT,
				Items.COOKED_RABBIT,
				0.35F
		);
	}

	public final <T extends AbstractCookingRecipe> void offerFoodCookingRecipe(
			String cooker,
			RecipeSerializer<T> serializer,
			AbstractCookingRecipe.RecipeFactory<T> recipeFactory,
			int cookingTime,
			ItemConvertible input,
			ItemConvertible output,
			float experience
	) {
		CookingRecipeJsonBuilder
				.create(
						Ingredient.ofItem(input),
						RecipeCategory.FOOD,
						output,
						experience,
						cookingTime,
						serializer,
						recipeFactory
				)
				.criterion(hasItem(input), conditionsFromItem(input))
				.offerTo(exporter, getItemPath(output) + "_from_" + cooker);
	}

	public void offerWaxingRecipes(FeatureSet enabledFeatures) {
		HoneycombItem.UNWAXED_TO_WAXED_BLOCKS
				.get()
				.forEach(
						(unwaxed, waxed) -> {
							if (waxed.getRequiredFeatures().isSubsetOf(enabledFeatures)) {
								Pair<RecipeCategory, String>
										pair =
										(Pair<RecipeCategory, String>) HoneycombItem.WAXED_RECIPE_GROUPS
												.getOrDefault(
														waxed,
														Pair.of(RecipeCategory.BUILDING_BLOCKS, getItemPath(waxed))
												);
								RecipeCategory recipeCategory = (RecipeCategory) pair.getFirst();
								String string = (String) pair.getSecond();
								createShapeless(recipeCategory, waxed)
								    .input(unwaxed)
								    .input(Items.HONEYCOMB)
								    .group(string)
								    .criterion(hasItem(unwaxed), conditionsFromItem(unwaxed))
								    .offerTo(exporter, convertBetween(waxed, Items.HONEYCOMB));
							}
						}
				);
	}

	public void offerGrateRecipe(Block output, Block input) {
		createShaped(RecipeCategory.BUILDING_BLOCKS, output, 4)
		    .input('M', input)
		    .pattern(" M ")
		    .pattern("M M")
		    .pattern(" M ")
		    .group(getItemPath(output))
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public void offerBulbRecipe(Block output, Block input) {
		createShaped(RecipeCategory.REDSTONE, output, 4)
		    .input('C', input)
		    .input('R', Items.REDSTONE)
		    .input('B', Items.BLAZE_ROD)
		    .pattern(" C ")
		    .pattern("CBC")
		    .pattern(" R ")
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .group(getItemPath(output))
		    .offerTo(exporter);
	}

	public void offerWaxedChiseledCopperRecipe(Block output, Block input) {
		createShaped(RecipeCategory.BUILDING_BLOCKS, output)
		    .input('M', input)
		    .pattern(" M ")
		    .pattern(" M ")
		    .group(getItemPath(output))
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .offerTo(exporter);
	}

	public void offerSuspiciousStewRecipe(Item input, SuspiciousStewIngredient stewIngredient) {
		ItemStack itemStack = new ItemStack(
				Items.SUSPICIOUS_STEW.getRegistryEntry(),
				1,
				ComponentChanges
						.builder()
						.add(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, stewIngredient.getStewEffects())
						.build()
		);
		createShapeless(RecipeCategory.FOOD, itemStack)
		    .input(Items.BOWL)
		    .input(Items.BROWN_MUSHROOM)
		    .input(Items.RED_MUSHROOM)
		    .input(input)
		    .group("suspicious_stew")
		    .criterion(hasItem(input), conditionsFromItem(input))
		    .offerTo(exporter, getItemPath(itemStack.getItem()) + "_from_" + getItemPath(input));
	}

	public void generateFamily(BlockFamily family, FeatureSet enabledFeatures) {
		family.getVariants().forEach((variant, block) -> {
			if (block.getRequiredFeatures().isSubsetOf(enabledFeatures) == false) {
				return;
			}

			BlockFamilyRecipeFactory recipeFactory = VARIANT_FACTORIES.get(variant);
			ItemConvertible recipeInput = getVariantRecipeInput(family, variant);

			if (recipeFactory != null) {
				CraftingRecipeJsonBuilder recipeBuilder = recipeFactory.create(this, block, recipeInput);

				family.getGroup().ifPresent(group -> recipeBuilder.group(
						group + (variant == BlockFamily.Variant.CUT ? "" : "_" + variant.getName())
				));

				recipeBuilder.criterion(
						family.getUnlockCriterionName().orElseGet(() -> hasItem(recipeInput)),
						conditionsFromItem(recipeInput)
				);
				recipeBuilder.offerTo(exporter);
			}

			if (variant == BlockFamily.Variant.CRACKED) {
				offerCrackingRecipe(block, recipeInput);
			}
		});
	}

	public final Block getVariantRecipeInput(BlockFamily family, BlockFamily.Variant variant) {
		if (variant == BlockFamily.Variant.CHISELED) {
			if (family.getVariants().containsKey(BlockFamily.Variant.SLAB) == false) {
				throw new IllegalStateException("Slab is not defined for the family.");
			}

			return family.getVariant(BlockFamily.Variant.SLAB);
		}

		return family.getBaseBlock();
	}

	public static AdvancementCriterion<EnterBlockCriterion.Conditions> requireEnteringFluid(Block block) {
		return Criteria.ENTER_BLOCK.create(new EnterBlockCriterion.Conditions(
				Optional.empty(),
				Optional.of(block.getRegistryEntry()),
				Optional.empty()
		));
	}

	public final AdvancementCriterion<InventoryChangedCriterion.Conditions> conditionsFromItem(
			NumberRange.IntRange count,
			ItemConvertible item
	) {
		return conditionsFromPredicates(ItemPredicate.Builder.create().items(itemLookup, item).count(count));
	}

	public AdvancementCriterion<InventoryChangedCriterion.Conditions> conditionsFromItem(ItemConvertible item) {
		return conditionsFromPredicates(ItemPredicate.Builder.create().items(itemLookup, item));
	}

	public AdvancementCriterion<InventoryChangedCriterion.Conditions> conditionsFromTag(TagKey<Item> tag) {
		return conditionsFromPredicates(ItemPredicate.Builder.create().tag(itemLookup, tag));
	}

	public static AdvancementCriterion<InventoryChangedCriterion.Conditions> conditionsFromPredicates(ItemPredicate.Builder... predicates) {
		return conditionsFromItemPredicates(Arrays
				.stream(predicates)
				.map(ItemPredicate.Builder::build)
				.toArray(ItemPredicate[]::new));
	}

	public static AdvancementCriterion<InventoryChangedCriterion.Conditions> conditionsFromItemPredicates(ItemPredicate... predicates) {
		return Criteria.INVENTORY_CHANGED
				.create(new InventoryChangedCriterion.Conditions(
						Optional.empty(),
						InventoryChangedCriterion.Conditions.Slots.ANY,
						List.of(predicates)
				));
	}

	public static String hasItem(ItemConvertible item) {
		return "has_" + getItemPath(item);
	}

	public static String getItemPath(ItemConvertible item) {
		return Registries.ITEM.getId(item.asItem()).getPath();
	}

	public static String getRecipeName(ItemConvertible item) {
		return getItemPath(item);
	}

	public static String convertBetween(ItemConvertible to, ItemConvertible from) {
		return getItemPath(to) + "_from_" + getItemPath(from);
	}

	public static String getSmeltingItemPath(ItemConvertible item) {
		return getItemPath(item) + "_from_smelting";
	}

	public static String getBlastingItemPath(ItemConvertible item) {
		return getItemPath(item) + "_from_blasting";
	}

	public Ingredient ingredientFromTag(TagKey<Item> tag) {
		return Ingredient.ofTag(itemLookup.getOrThrow(tag));
	}

	public ShapedRecipeJsonBuilder createShaped(RecipeCategory category, ItemConvertible output) {
		return ShapedRecipeJsonBuilder.create(itemLookup, category, output);
	}

	public ShapedRecipeJsonBuilder createShaped(RecipeCategory category, ItemConvertible output, int count) {
		return ShapedRecipeJsonBuilder.create(itemLookup, category, output, count);
	}

	public ShapelessRecipeJsonBuilder createShapeless(RecipeCategory category, ItemStack output) {
		return ShapelessRecipeJsonBuilder.create(itemLookup, category, output);
	}

	public ShapelessRecipeJsonBuilder createShapeless(RecipeCategory category, ItemConvertible output) {
		return ShapelessRecipeJsonBuilder.create(itemLookup, category, output);
	}

	public ShapelessRecipeJsonBuilder createShapeless(RecipeCategory category, ItemConvertible output, int count) {
		return ShapelessRecipeJsonBuilder.create(itemLookup, category, output, count);
	}

	/**
	 * Фабрика рецептов для вариантов блочного семейства.
	 * Принимает генератор, выходной блок и входной блок, возвращает построитель рецепта.
	 */
	@FunctionalInterface
	interface BlockFamilyRecipeFactory {

		CraftingRecipeJsonBuilder create(RecipeGenerator generator, ItemConvertible output, ItemConvertible input);
	}

	/**
	 * Базовый {@link DataProvider} для генерации рецептов.
	 * Управляет жизненным циклом асинхронной записи рецептов и их достижений на диск.
	 */
	public abstract static class RecipeProvider implements DataProvider {

		private final DataOutput output;
		private final CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture;

		protected RecipeProvider(DataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
			this.output = output;
			this.registriesFuture = registriesFuture;
		}

		@Override
		public CompletableFuture<?> run(DataWriter writer) {
			return registriesFuture
					.thenCompose(
							registries -> {
								final DataOutput.PathResolver recipePathResolver = output.getResolver(RegistryKeys.RECIPE);
								final DataOutput.PathResolver advancementPathResolver = output.getResolver(RegistryKeys.ADVANCEMENT);
								final Set<RegistryKey<Recipe<?>>> registeredKeys = Sets.newHashSet();
								final List<CompletableFuture<?>> futures = new ArrayList<>();

								RecipeExporter recipeExporter = new RecipeExporter() {
									@Override
									public void accept(
											RegistryKey<Recipe<?>> key,
											Recipe<?> recipe,
											@Nullable AdvancementEntry advancement
									) {
										if (registeredKeys.add(key) == false) {
											throw new IllegalStateException("Duplicate recipe " + key.getValue());
										}

										addRecipe(key, recipe);

										if (advancement != null) {
											addRecipeAdvancement(advancement);
										}
									}

									@Override
									public Advancement.Builder getAdvancementBuilder() {
										return Advancement.Builder
												.createUntelemetered()
												.parent(CraftingRecipeJsonBuilder.ROOT);
									}

									@Override
									public void addRootAdvancement() {
										AdvancementEntry advancementEntry = Advancement.Builder
												.createUntelemetered()
												.criterion(
														"impossible",
														Criteria.IMPOSSIBLE.create(new ImpossibleCriterion.Conditions())
												)
												.build(CraftingRecipeJsonBuilder.ROOT);

										addRecipeAdvancement(advancementEntry);
									}

									private void addRecipe(RegistryKey<Recipe<?>> key, Recipe<?> recipe) {
										futures.add(DataProvider.writeCodecToPath(
												writer,
												registries,
												Recipe.CODEC,
												recipe,
												recipePathResolver.resolveJson(key.getValue())
										));
									}

									private void addRecipeAdvancement(AdvancementEntry advancementEntry) {
										futures.add(DataProvider.writeCodecToPath(
												writer,
												registries,
												Advancement.CODEC,
												advancementEntry.value(),
												advancementPathResolver.resolveJson(advancementEntry.id())
										));
									}
								};

								getRecipeGenerator(registries, recipeExporter).generate();

								return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
							}
					);
		}

		protected abstract RecipeGenerator getRecipeGenerator(
				RegistryWrapper.WrapperLookup registries,
				RecipeExporter exporter
		);
	}
}
