package net.minecraft.data.loottable.vanilla;

import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.data.loottable.BlockLootTableGenerator;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.*;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.entry.*;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.CopyComponentsLootFunction;
import net.minecraft.loot.function.LimitCountLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.operator.BoundedIntUnaryOperator;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.resource.featuretoggle.FeatureFlags;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code VanillaBlockLootTableGenerator}.
 */
public class VanillaBlockLootTableGenerator extends BlockLootTableGenerator {

	private static final float[]
			JUNGLE_SAPLING_DROP_CHANCE =
			new float[]{0.025F, 0.027777778F, 0.03125F, 0.041666668F, 0.1F};
	private static final Set<Item> EXPLOSION_IMMUNE = Stream.of(
			Blocks.DRAGON_EGG,
			Blocks.BEACON,
			Blocks.CONDUIT,
			Blocks.SKELETON_SKULL,
			Blocks.WITHER_SKELETON_SKULL,
			Blocks.PLAYER_HEAD,
			Blocks.ZOMBIE_HEAD,
			Blocks.CREEPER_HEAD,
			Blocks.DRAGON_HEAD,
			Blocks.PIGLIN_HEAD,
			Blocks.SHULKER_BOX,
			Blocks.BLACK_SHULKER_BOX,
			Blocks.BLUE_SHULKER_BOX,
			Blocks.BROWN_SHULKER_BOX,
			Blocks.CYAN_SHULKER_BOX,
			Blocks.GRAY_SHULKER_BOX,
			Blocks.GREEN_SHULKER_BOX,
			Blocks.LIGHT_BLUE_SHULKER_BOX,
			Blocks.LIGHT_GRAY_SHULKER_BOX,
			Blocks.LIME_SHULKER_BOX,
			Blocks.MAGENTA_SHULKER_BOX,
			Blocks.ORANGE_SHULKER_BOX,
			Blocks.PINK_SHULKER_BOX,
			Blocks.PURPLE_SHULKER_BOX,
			Blocks.RED_SHULKER_BOX,
			Blocks.WHITE_SHULKER_BOX,
			Blocks.YELLOW_SHULKER_BOX
	                                                        )
	                                                        .map(ItemConvertible::asItem)
	                                                        .collect(Collectors.toSet());

	public VanillaBlockLootTableGenerator(RegistryWrapper.WrapperLookup registries) {
		super(EXPLOSION_IMMUNE, FeatureFlags.FEATURE_MANAGER.getFeatureSet(), registries);
	}

	@Override
	public void generate() {
		RegistryWrapper.Impl<Enchantment> impl = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
		RegistryWrapper.Impl<Item> impl2 = registries.getOrThrow(RegistryKeys.ITEM);
		addDrop(Blocks.GRANITE);
		addDrop(Blocks.POLISHED_GRANITE);
		addDrop(Blocks.DIORITE);
		addDrop(Blocks.POLISHED_DIORITE);
		addDrop(Blocks.ANDESITE);
		addDrop(Blocks.POLISHED_ANDESITE);
		addDrop(Blocks.DIRT);
		addDrop(Blocks.COARSE_DIRT);
		addDrop(Blocks.COBBLESTONE);
		addDrop(Blocks.OAK_PLANKS);
		addDrop(Blocks.SPRUCE_PLANKS);
		addDrop(Blocks.BIRCH_PLANKS);
		addDrop(Blocks.JUNGLE_PLANKS);
		addDrop(Blocks.ACACIA_PLANKS);
		addDrop(Blocks.DARK_OAK_PLANKS);
		addDrop(Blocks.PALE_OAK_PLANKS);
		addDrop(Blocks.MANGROVE_PLANKS);
		addDrop(Blocks.CHERRY_PLANKS);
		addDrop(Blocks.BAMBOO_PLANKS);
		addDrop(Blocks.BAMBOO_MOSAIC);
		addDrop(Blocks.DECORATED_POT, this::decoratedPotDrops);
		addDrop(Blocks.OAK_SAPLING);
		addDrop(Blocks.SPRUCE_SAPLING);
		addDrop(Blocks.BIRCH_SAPLING);
		addDrop(Blocks.JUNGLE_SAPLING);
		addDrop(Blocks.ACACIA_SAPLING);
		addDrop(Blocks.DARK_OAK_SAPLING);
		addDrop(Blocks.PALE_OAK_SAPLING);
		addDrop(Blocks.CHERRY_SAPLING);
		addDrop(Blocks.SAND);
		addDrop(Blocks.SUSPICIOUS_SAND, dropsNothing());
		addDrop(Blocks.SUSPICIOUS_GRAVEL, dropsNothing());
		addDrop(Blocks.RED_SAND);
		addDrop(Blocks.OAK_LOG);
		addDrop(Blocks.SPRUCE_LOG);
		addDrop(Blocks.BIRCH_LOG);
		addDrop(Blocks.JUNGLE_LOG);
		addDrop(Blocks.ACACIA_LOG);
		addDrop(Blocks.DARK_OAK_LOG);
		addDrop(Blocks.PALE_OAK_LOG);
		addDrop(Blocks.CHERRY_LOG);
		addDrop(Blocks.BAMBOO_BLOCK);
		addDrop(Blocks.STRIPPED_OAK_LOG);
		addDrop(Blocks.STRIPPED_SPRUCE_LOG);
		addDrop(Blocks.STRIPPED_BIRCH_LOG);
		addDrop(Blocks.STRIPPED_JUNGLE_LOG);
		addDrop(Blocks.STRIPPED_ACACIA_LOG);
		addDrop(Blocks.STRIPPED_DARK_OAK_LOG);
		addDrop(Blocks.STRIPPED_PALE_OAK_LOG);
		addDrop(Blocks.STRIPPED_MANGROVE_LOG);
		addDrop(Blocks.STRIPPED_CHERRY_LOG);
		addDrop(Blocks.STRIPPED_BAMBOO_BLOCK);
		addDrop(Blocks.STRIPPED_WARPED_STEM);
		addDrop(Blocks.STRIPPED_CRIMSON_STEM);
		addDrop(Blocks.OAK_WOOD);
		addDrop(Blocks.SPRUCE_WOOD);
		addDrop(Blocks.BIRCH_WOOD);
		addDrop(Blocks.JUNGLE_WOOD);
		addDrop(Blocks.ACACIA_WOOD);
		addDrop(Blocks.DARK_OAK_WOOD);
		addDrop(Blocks.PALE_OAK_WOOD);
		addDrop(Blocks.MANGROVE_WOOD);
		addDrop(Blocks.CHERRY_WOOD);
		addDrop(Blocks.STRIPPED_OAK_WOOD);
		addDrop(Blocks.STRIPPED_SPRUCE_WOOD);
		addDrop(Blocks.STRIPPED_BIRCH_WOOD);
		addDrop(Blocks.STRIPPED_JUNGLE_WOOD);
		addDrop(Blocks.STRIPPED_ACACIA_WOOD);
		addDrop(Blocks.STRIPPED_DARK_OAK_WOOD);
		addDrop(Blocks.STRIPPED_PALE_OAK_WOOD);
		addDrop(Blocks.STRIPPED_MANGROVE_WOOD);
		addDrop(Blocks.STRIPPED_CHERRY_WOOD);
		addDrop(Blocks.STRIPPED_CRIMSON_HYPHAE);
		addDrop(Blocks.STRIPPED_WARPED_HYPHAE);
		addDrop(Blocks.SPONGE);
		addDrop(Blocks.WET_SPONGE);
		addDrop(Blocks.LAPIS_BLOCK);
		addDrop(Blocks.RESIN_BLOCK);
		addDrop(Blocks.SANDSTONE);
		addDrop(Blocks.CHISELED_SANDSTONE);
		addDrop(Blocks.CUT_SANDSTONE);
		addDrop(Blocks.NOTE_BLOCK);
		addDrop(Blocks.POWERED_RAIL);
		addDrop(Blocks.DETECTOR_RAIL);
		addDrop(Blocks.STICKY_PISTON);
		addDrop(Blocks.PISTON);
		addDrop(Blocks.WHITE_WOOL);
		addDrop(Blocks.ORANGE_WOOL);
		addDrop(Blocks.MAGENTA_WOOL);
		addDrop(Blocks.LIGHT_BLUE_WOOL);
		addDrop(Blocks.YELLOW_WOOL);
		addDrop(Blocks.LIME_WOOL);
		addDrop(Blocks.PINK_WOOL);
		addDrop(Blocks.GRAY_WOOL);
		addDrop(Blocks.LIGHT_GRAY_WOOL);
		addDrop(Blocks.CYAN_WOOL);
		addDrop(Blocks.PURPLE_WOOL);
		addDrop(Blocks.BLUE_WOOL);
		addDrop(Blocks.BROWN_WOOL);
		addDrop(Blocks.GREEN_WOOL);
		addDrop(Blocks.RED_WOOL);
		addDrop(Blocks.BLACK_WOOL);
		addDrop(Blocks.DANDELION);
		addDrop(Blocks.OPEN_EYEBLOSSOM);
		addDrop(Blocks.CLOSED_EYEBLOSSOM);
		addDrop(Blocks.POPPY);
		addDrop(Blocks.TORCHFLOWER);
		addDrop(Blocks.BLUE_ORCHID);
		addDrop(Blocks.ALLIUM);
		addDrop(Blocks.AZURE_BLUET);
		addDrop(Blocks.RED_TULIP);
		addDrop(Blocks.ORANGE_TULIP);
		addDrop(Blocks.WHITE_TULIP);
		addDrop(Blocks.PINK_TULIP);
		addDrop(Blocks.OXEYE_DAISY);
		addDrop(Blocks.CORNFLOWER);
		addDrop(Blocks.WITHER_ROSE);
		addDrop(Blocks.LILY_OF_THE_VALLEY);
		addDrop(Blocks.BROWN_MUSHROOM);
		addDrop(Blocks.RED_MUSHROOM);
		addDrop(Blocks.GOLD_BLOCK);
		addDrop(Blocks.IRON_BLOCK);
		addDrop(Blocks.BRICKS);
		addDrop(Blocks.MOSSY_COBBLESTONE);
		addDrop(Blocks.OBSIDIAN);
		addDrop(Blocks.CRYING_OBSIDIAN);
		addDrop(Blocks.TORCH);
		addDrop(Blocks.OAK_STAIRS);
		addDrop(Blocks.MANGROVE_STAIRS);
		addDrop(Blocks.BAMBOO_STAIRS);
		addDrop(Blocks.BAMBOO_MOSAIC_STAIRS);
		addDrop(Blocks.REDSTONE_WIRE);
		addDrop(Blocks.DIAMOND_BLOCK);
		addDrop(Blocks.CRAFTING_TABLE);
		addDrop(Blocks.OAK_SIGN);
		addDrop(Blocks.SPRUCE_SIGN);
		addDrop(Blocks.BIRCH_SIGN);
		addDrop(Blocks.ACACIA_SIGN);
		addDrop(Blocks.JUNGLE_SIGN);
		addDrop(Blocks.DARK_OAK_SIGN);
		addDrop(Blocks.PALE_OAK_SIGN);
		addDrop(Blocks.MANGROVE_SIGN);
		addDrop(Blocks.CHERRY_SIGN);
		addDrop(Blocks.BAMBOO_SIGN);
		addDrop(Blocks.OAK_HANGING_SIGN);
		addDrop(Blocks.SPRUCE_HANGING_SIGN);
		addDrop(Blocks.BIRCH_HANGING_SIGN);
		addDrop(Blocks.ACACIA_HANGING_SIGN);
		addDrop(Blocks.CHERRY_HANGING_SIGN);
		addDrop(Blocks.JUNGLE_HANGING_SIGN);
		addDrop(Blocks.DARK_OAK_HANGING_SIGN);
		addDrop(Blocks.PALE_OAK_HANGING_SIGN);
		addDrop(Blocks.MANGROVE_HANGING_SIGN);
		addDrop(Blocks.CRIMSON_HANGING_SIGN);
		addDrop(Blocks.WARPED_HANGING_SIGN);
		addDrop(Blocks.BAMBOO_HANGING_SIGN);
		addDrop(Blocks.LADDER);
		addDrop(Blocks.RAIL);
		addDrop(Blocks.COBBLESTONE_STAIRS);
		addDrop(Blocks.LEVER);
		addDrop(Blocks.STONE_PRESSURE_PLATE);
		addDrop(Blocks.OAK_PRESSURE_PLATE);
		addDrop(Blocks.SPRUCE_PRESSURE_PLATE);
		addDrop(Blocks.BIRCH_PRESSURE_PLATE);
		addDrop(Blocks.JUNGLE_PRESSURE_PLATE);
		addDrop(Blocks.ACACIA_PRESSURE_PLATE);
		addDrop(Blocks.DARK_OAK_PRESSURE_PLATE);
		addDrop(Blocks.PALE_OAK_PRESSURE_PLATE);
		addDrop(Blocks.MANGROVE_PRESSURE_PLATE);
		addDrop(Blocks.CHERRY_PRESSURE_PLATE);
		addDrop(Blocks.BAMBOO_PRESSURE_PLATE);
		addDrop(Blocks.REDSTONE_TORCH);
		addDrop(Blocks.STONE_BUTTON);
		addDrop(Blocks.CACTUS);
		addDrop(Blocks.SUGAR_CANE);
		addDrop(Blocks.JUKEBOX);
		addDrop(Blocks.OAK_FENCE);
		addDrop(Blocks.MANGROVE_FENCE);
		addDrop(Blocks.BAMBOO_FENCE);
		addDrop(Blocks.PUMPKIN);
		addDrop(Blocks.NETHERRACK);
		addDrop(Blocks.SOUL_SAND);
		addDrop(Blocks.SOUL_SOIL);
		addDrop(Blocks.BASALT);
		addDrop(Blocks.POLISHED_BASALT);
		addDrop(Blocks.SMOOTH_BASALT);
		addDrop(Blocks.SOUL_TORCH);
		addDrop(Blocks.COPPER_TORCH);
		addDrop(Blocks.CARVED_PUMPKIN);
		addDrop(Blocks.JACK_O_LANTERN);
		addDrop(Blocks.REPEATER);
		addDrop(Blocks.OAK_TRAPDOOR);
		addDrop(Blocks.SPRUCE_TRAPDOOR);
		addDrop(Blocks.BIRCH_TRAPDOOR);
		addDrop(Blocks.JUNGLE_TRAPDOOR);
		addDrop(Blocks.ACACIA_TRAPDOOR);
		addDrop(Blocks.DARK_OAK_TRAPDOOR);
		addDrop(Blocks.PALE_OAK_TRAPDOOR);
		addDrop(Blocks.MANGROVE_TRAPDOOR);
		addDrop(Blocks.CHERRY_TRAPDOOR);
		addDrop(Blocks.BAMBOO_TRAPDOOR);
		addDrop(Blocks.COPPER_TRAPDOOR);
		addDrop(Blocks.EXPOSED_COPPER_TRAPDOOR);
		addDrop(Blocks.WEATHERED_COPPER_TRAPDOOR);
		addDrop(Blocks.OXIDIZED_COPPER_TRAPDOOR);
		addDrop(Blocks.WAXED_COPPER_TRAPDOOR);
		addDrop(Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR);
		addDrop(Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR);
		addDrop(Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR);
		addDrop(Blocks.STONE_BRICKS);
		addDrop(Blocks.MOSSY_STONE_BRICKS);
		addDrop(Blocks.CRACKED_STONE_BRICKS);
		addDrop(Blocks.CHISELED_STONE_BRICKS);
		addDrop(Blocks.IRON_BARS);
		Blocks.COPPER_BARS.forEach(block -> addDrop(block));
		addDrop(Blocks.OAK_FENCE_GATE);
		addDrop(Blocks.MANGROVE_FENCE_GATE);
		addDrop(Blocks.BAMBOO_FENCE_GATE);
		addDrop(Blocks.BRICK_STAIRS);
		addDrop(Blocks.STONE_BRICK_STAIRS);
		addDrop(Blocks.LILY_PAD);
		addDrop(Blocks.RESIN_BRICKS);
		addDrop(Blocks.RESIN_BRICK_WALL);
		addDrop(Blocks.RESIN_BRICK_STAIRS);
		addDrop(Blocks.CHISELED_RESIN_BRICKS);
		addDrop(Blocks.NETHER_BRICKS);
		addDrop(Blocks.NETHER_BRICK_FENCE);
		addDrop(Blocks.NETHER_BRICK_STAIRS);
		addDrop(Blocks.CAULDRON);
		addDrop(Blocks.END_STONE);
		addDrop(Blocks.REDSTONE_LAMP);
		addDrop(Blocks.SANDSTONE_STAIRS);
		addDrop(Blocks.TRIPWIRE_HOOK);
		addDrop(Blocks.EMERALD_BLOCK);
		addDrop(Blocks.SPRUCE_STAIRS);
		addDrop(Blocks.BIRCH_STAIRS);
		addDrop(Blocks.JUNGLE_STAIRS);
		addDrop(Blocks.COBBLESTONE_WALL);
		addDrop(Blocks.MOSSY_COBBLESTONE_WALL);
		addDrop(Blocks.FLOWER_POT);
		addDrop(Blocks.OAK_BUTTON);
		addDrop(Blocks.SPRUCE_BUTTON);
		addDrop(Blocks.BIRCH_BUTTON);
		addDrop(Blocks.JUNGLE_BUTTON);
		addDrop(Blocks.ACACIA_BUTTON);
		addDrop(Blocks.DARK_OAK_BUTTON);
		addDrop(Blocks.PALE_OAK_BUTTON);
		addDrop(Blocks.MANGROVE_BUTTON);
		addDrop(Blocks.CHERRY_BUTTON);
		addDrop(Blocks.BAMBOO_BUTTON);
		addDrop(Blocks.ANVIL);
		addDrop(Blocks.CHIPPED_ANVIL);
		addDrop(Blocks.DAMAGED_ANVIL);
		addDrop(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
		addDrop(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
		addDrop(Blocks.COMPARATOR);
		addDrop(Blocks.DAYLIGHT_DETECTOR);
		addDrop(Blocks.REDSTONE_BLOCK);
		addDrop(Blocks.QUARTZ_BLOCK);
		addDrop(Blocks.CHISELED_QUARTZ_BLOCK);
		addDrop(Blocks.QUARTZ_PILLAR);
		addDrop(Blocks.QUARTZ_STAIRS);
		addDrop(Blocks.ACTIVATOR_RAIL);
		addDrop(Blocks.WHITE_TERRACOTTA);
		addDrop(Blocks.ORANGE_TERRACOTTA);
		addDrop(Blocks.MAGENTA_TERRACOTTA);
		addDrop(Blocks.LIGHT_BLUE_TERRACOTTA);
		addDrop(Blocks.YELLOW_TERRACOTTA);
		addDrop(Blocks.LIME_TERRACOTTA);
		addDrop(Blocks.PINK_TERRACOTTA);
		addDrop(Blocks.GRAY_TERRACOTTA);
		addDrop(Blocks.LIGHT_GRAY_TERRACOTTA);
		addDrop(Blocks.CYAN_TERRACOTTA);
		addDrop(Blocks.PURPLE_TERRACOTTA);
		addDrop(Blocks.BLUE_TERRACOTTA);
		addDrop(Blocks.BROWN_TERRACOTTA);
		addDrop(Blocks.GREEN_TERRACOTTA);
		addDrop(Blocks.RED_TERRACOTTA);
		addDrop(Blocks.BLACK_TERRACOTTA);
		addDrop(Blocks.ACACIA_STAIRS);
		addDrop(Blocks.DARK_OAK_STAIRS);
		addDrop(Blocks.PALE_OAK_STAIRS);
		addDrop(Blocks.CHERRY_STAIRS);
		addDrop(Blocks.SLIME_BLOCK);
		addDrop(Blocks.IRON_TRAPDOOR);
		addDrop(Blocks.PRISMARINE);
		addDrop(Blocks.PRISMARINE_BRICKS);
		addDrop(Blocks.DARK_PRISMARINE);
		addDrop(Blocks.PRISMARINE_STAIRS);
		addDrop(Blocks.PRISMARINE_BRICK_STAIRS);
		addDrop(Blocks.DARK_PRISMARINE_STAIRS);
		addDrop(Blocks.HAY_BLOCK);
		addDrop(Blocks.WHITE_CARPET);
		addDrop(Blocks.ORANGE_CARPET);
		addDrop(Blocks.MAGENTA_CARPET);
		addDrop(Blocks.LIGHT_BLUE_CARPET);
		addDrop(Blocks.YELLOW_CARPET);
		addDrop(Blocks.LIME_CARPET);
		addDrop(Blocks.PINK_CARPET);
		addDrop(Blocks.GRAY_CARPET);
		addDrop(Blocks.LIGHT_GRAY_CARPET);
		addDrop(Blocks.CYAN_CARPET);
		addDrop(Blocks.PURPLE_CARPET);
		addDrop(Blocks.BLUE_CARPET);
		addDrop(Blocks.BROWN_CARPET);
		addDrop(Blocks.GREEN_CARPET);
		addDrop(Blocks.RED_CARPET);
		addDrop(Blocks.BLACK_CARPET);
		addDrop(Blocks.TERRACOTTA);
		addDrop(Blocks.COAL_BLOCK);
		addDrop(Blocks.RED_SANDSTONE);
		addDrop(Blocks.CHISELED_RED_SANDSTONE);
		addDrop(Blocks.CUT_RED_SANDSTONE);
		addDrop(Blocks.RED_SANDSTONE_STAIRS);
		addDrop(Blocks.SMOOTH_STONE);
		addDrop(Blocks.SMOOTH_SANDSTONE);
		addDrop(Blocks.SMOOTH_QUARTZ);
		addDrop(Blocks.SMOOTH_RED_SANDSTONE);
		addDrop(Blocks.SPRUCE_FENCE_GATE);
		addDrop(Blocks.BIRCH_FENCE_GATE);
		addDrop(Blocks.JUNGLE_FENCE_GATE);
		addDrop(Blocks.ACACIA_FENCE_GATE);
		addDrop(Blocks.DARK_OAK_FENCE_GATE);
		addDrop(Blocks.PALE_OAK_FENCE_GATE);
		addDrop(Blocks.CHERRY_FENCE_GATE);
		addDrop(Blocks.SPRUCE_FENCE);
		addDrop(Blocks.BIRCH_FENCE);
		addDrop(Blocks.JUNGLE_FENCE);
		addDrop(Blocks.ACACIA_FENCE);
		addDrop(Blocks.DARK_OAK_FENCE);
		addDrop(Blocks.PALE_OAK_FENCE);
		addDrop(Blocks.CHERRY_FENCE);
		addDrop(Blocks.END_ROD);
		addDrop(Blocks.PURPUR_BLOCK);
		addDrop(Blocks.PURPUR_PILLAR);
		addDrop(Blocks.PURPUR_STAIRS);
		addDrop(Blocks.END_STONE_BRICKS);
		addDrop(Blocks.MAGMA_BLOCK);
		addDrop(Blocks.NETHER_WART_BLOCK);
		addDrop(Blocks.RED_NETHER_BRICKS);
		addDrop(Blocks.BONE_BLOCK);
		addDrop(Blocks.OBSERVER);
		addDrop(Blocks.TARGET);
		addDrop(Blocks.WHITE_GLAZED_TERRACOTTA);
		addDrop(Blocks.ORANGE_GLAZED_TERRACOTTA);
		addDrop(Blocks.MAGENTA_GLAZED_TERRACOTTA);
		addDrop(Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA);
		addDrop(Blocks.YELLOW_GLAZED_TERRACOTTA);
		addDrop(Blocks.LIME_GLAZED_TERRACOTTA);
		addDrop(Blocks.PINK_GLAZED_TERRACOTTA);
		addDrop(Blocks.GRAY_GLAZED_TERRACOTTA);
		addDrop(Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA);
		addDrop(Blocks.CYAN_GLAZED_TERRACOTTA);
		addDrop(Blocks.PURPLE_GLAZED_TERRACOTTA);
		addDrop(Blocks.BLUE_GLAZED_TERRACOTTA);
		addDrop(Blocks.BROWN_GLAZED_TERRACOTTA);
		addDrop(Blocks.GREEN_GLAZED_TERRACOTTA);
		addDrop(Blocks.RED_GLAZED_TERRACOTTA);
		addDrop(Blocks.BLACK_GLAZED_TERRACOTTA);
		addDrop(Blocks.WHITE_CONCRETE);
		addDrop(Blocks.ORANGE_CONCRETE);
		addDrop(Blocks.MAGENTA_CONCRETE);
		addDrop(Blocks.LIGHT_BLUE_CONCRETE);
		addDrop(Blocks.YELLOW_CONCRETE);
		addDrop(Blocks.LIME_CONCRETE);
		addDrop(Blocks.PINK_CONCRETE);
		addDrop(Blocks.GRAY_CONCRETE);
		addDrop(Blocks.LIGHT_GRAY_CONCRETE);
		addDrop(Blocks.CYAN_CONCRETE);
		addDrop(Blocks.PURPLE_CONCRETE);
		addDrop(Blocks.BLUE_CONCRETE);
		addDrop(Blocks.BROWN_CONCRETE);
		addDrop(Blocks.GREEN_CONCRETE);
		addDrop(Blocks.RED_CONCRETE);
		addDrop(Blocks.BLACK_CONCRETE);
		addDrop(Blocks.WHITE_CONCRETE_POWDER);
		addDrop(Blocks.ORANGE_CONCRETE_POWDER);
		addDrop(Blocks.MAGENTA_CONCRETE_POWDER);
		addDrop(Blocks.LIGHT_BLUE_CONCRETE_POWDER);
		addDrop(Blocks.YELLOW_CONCRETE_POWDER);
		addDrop(Blocks.LIME_CONCRETE_POWDER);
		addDrop(Blocks.PINK_CONCRETE_POWDER);
		addDrop(Blocks.GRAY_CONCRETE_POWDER);
		addDrop(Blocks.LIGHT_GRAY_CONCRETE_POWDER);
		addDrop(Blocks.CYAN_CONCRETE_POWDER);
		addDrop(Blocks.PURPLE_CONCRETE_POWDER);
		addDrop(Blocks.BLUE_CONCRETE_POWDER);
		addDrop(Blocks.BROWN_CONCRETE_POWDER);
		addDrop(Blocks.GREEN_CONCRETE_POWDER);
		addDrop(Blocks.RED_CONCRETE_POWDER);
		addDrop(Blocks.BLACK_CONCRETE_POWDER);
		addDrop(Blocks.KELP);
		addDrop(Blocks.DRIED_KELP_BLOCK);
		addDrop(Blocks.DEAD_TUBE_CORAL_BLOCK);
		addDrop(Blocks.DEAD_BRAIN_CORAL_BLOCK);
		addDrop(Blocks.DEAD_BUBBLE_CORAL_BLOCK);
		addDrop(Blocks.DEAD_FIRE_CORAL_BLOCK);
		addDrop(Blocks.DEAD_HORN_CORAL_BLOCK);
		addDrop(Blocks.CONDUIT);
		addDrop(Blocks.DRAGON_EGG);
		addDrop(Blocks.BAMBOO);
		addDrop(Blocks.POLISHED_GRANITE_STAIRS);
		addDrop(Blocks.SMOOTH_RED_SANDSTONE_STAIRS);
		addDrop(Blocks.MOSSY_STONE_BRICK_STAIRS);
		addDrop(Blocks.POLISHED_DIORITE_STAIRS);
		addDrop(Blocks.MOSSY_COBBLESTONE_STAIRS);
		addDrop(Blocks.END_STONE_BRICK_STAIRS);
		addDrop(Blocks.STONE_STAIRS);
		addDrop(Blocks.SMOOTH_SANDSTONE_STAIRS);
		addDrop(Blocks.SMOOTH_QUARTZ_STAIRS);
		addDrop(Blocks.GRANITE_STAIRS);
		addDrop(Blocks.ANDESITE_STAIRS);
		addDrop(Blocks.RED_NETHER_BRICK_STAIRS);
		addDrop(Blocks.POLISHED_ANDESITE_STAIRS);
		addDrop(Blocks.DIORITE_STAIRS);
		addDrop(Blocks.BRICK_WALL);
		addDrop(Blocks.PRISMARINE_WALL);
		addDrop(Blocks.RED_SANDSTONE_WALL);
		addDrop(Blocks.MOSSY_STONE_BRICK_WALL);
		addDrop(Blocks.GRANITE_WALL);
		addDrop(Blocks.STONE_BRICK_WALL);
		addDrop(Blocks.NETHER_BRICK_WALL);
		addDrop(Blocks.ANDESITE_WALL);
		addDrop(Blocks.RED_NETHER_BRICK_WALL);
		addDrop(Blocks.SANDSTONE_WALL);
		addDrop(Blocks.END_STONE_BRICK_WALL);
		addDrop(Blocks.DIORITE_WALL);
		addDrop(Blocks.MUD_BRICK_WALL);
		addDrop(Blocks.LOOM);
		addDrop(Blocks.SCAFFOLDING);
		addDrop(Blocks.HONEY_BLOCK);
		addDrop(Blocks.HONEYCOMB_BLOCK);
		addDrop(Blocks.RESPAWN_ANCHOR);
		addDrop(Blocks.LODESTONE);
		addDrop(Blocks.WARPED_STEM);
		addDrop(Blocks.WARPED_HYPHAE);
		addDrop(Blocks.WARPED_FUNGUS);
		addDrop(Blocks.WARPED_WART_BLOCK);
		addDrop(Blocks.CRIMSON_STEM);
		addDrop(Blocks.CRIMSON_HYPHAE);
		addDrop(Blocks.CRIMSON_FUNGUS);
		addDrop(Blocks.SHROOMLIGHT);
		addDrop(Blocks.CRIMSON_PLANKS);
		addDrop(Blocks.WARPED_PLANKS);
		addDrop(Blocks.WARPED_PRESSURE_PLATE);
		addDrop(Blocks.WARPED_FENCE);
		addDrop(Blocks.WARPED_TRAPDOOR);
		addDrop(Blocks.WARPED_FENCE_GATE);
		addDrop(Blocks.WARPED_STAIRS);
		addDrop(Blocks.WARPED_BUTTON);
		addDrop(Blocks.WARPED_SIGN);
		addDrop(Blocks.CRIMSON_PRESSURE_PLATE);
		addDrop(Blocks.CRIMSON_FENCE);
		addDrop(Blocks.CRIMSON_TRAPDOOR);
		addDrop(Blocks.CRIMSON_FENCE_GATE);
		addDrop(Blocks.CRIMSON_STAIRS);
		addDrop(Blocks.CRIMSON_BUTTON);
		addDrop(Blocks.CRIMSON_SIGN);
		addDrop(Blocks.NETHERITE_BLOCK);
		addDrop(Blocks.ANCIENT_DEBRIS);
		addDrop(Blocks.BLACKSTONE);
		addDrop(Blocks.POLISHED_BLACKSTONE_BRICKS);
		addDrop(Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS);
		addDrop(Blocks.BLACKSTONE_STAIRS);
		addDrop(Blocks.BLACKSTONE_WALL);
		addDrop(Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
		addDrop(Blocks.CHISELED_POLISHED_BLACKSTONE);
		addDrop(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
		addDrop(Blocks.POLISHED_BLACKSTONE);
		addDrop(Blocks.POLISHED_BLACKSTONE_STAIRS);
		addDrop(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE);
		addDrop(Blocks.POLISHED_BLACKSTONE_BUTTON);
		addDrop(Blocks.POLISHED_BLACKSTONE_WALL);
		addDrop(Blocks.CHISELED_NETHER_BRICKS);
		addDrop(Blocks.CRACKED_NETHER_BRICKS);
		addDrop(Blocks.QUARTZ_BRICKS);
		addDrop(Blocks.IRON_CHAIN);
		Blocks.COPPER_CHAINS.forEach(block -> addDrop(block));
		addDrop(Blocks.WARPED_ROOTS);
		addDrop(Blocks.CRIMSON_ROOTS);
		addDrop(Blocks.MUD_BRICKS);
		addDrop(Blocks.MUDDY_MANGROVE_ROOTS);
		addDrop(Blocks.MUD_BRICK_STAIRS);
		addDrop(Blocks.AMETHYST_BLOCK);
		addDrop(Blocks.CALCITE);
		addDrop(Blocks.TUFF);
		addDrop(Blocks.TINTED_GLASS);
		addDropWithSilkTouch(Blocks.SCULK_SENSOR);
		addDropWithSilkTouch(Blocks.CALIBRATED_SCULK_SENSOR);
		addDropWithSilkTouch(Blocks.SCULK);
		addDropWithSilkTouch(Blocks.SCULK_CATALYST);
		addDrop(Blocks.SCULK_VEIN, block -> multifaceGrowthDrops(block, createSilkTouchCondition()));
		addDropWithSilkTouch(Blocks.SCULK_SHRIEKER);
		addDropWithSilkTouch(Blocks.CHISELED_BOOKSHELF);
		addDrop(Blocks.COPPER_BLOCK);
		addDrop(Blocks.EXPOSED_COPPER);
		addDrop(Blocks.WEATHERED_COPPER);
		addDrop(Blocks.OXIDIZED_COPPER);
		addDrop(Blocks.CUT_COPPER);
		addDrop(Blocks.EXPOSED_CUT_COPPER);
		addDrop(Blocks.WEATHERED_CUT_COPPER);
		addDrop(Blocks.OXIDIZED_CUT_COPPER);
		addDrop(Blocks.WAXED_COPPER_BLOCK);
		addDrop(Blocks.WAXED_WEATHERED_COPPER);
		addDrop(Blocks.WAXED_EXPOSED_COPPER);
		addDrop(Blocks.WAXED_OXIDIZED_COPPER);
		addDrop(Blocks.WAXED_CUT_COPPER);
		addDrop(Blocks.WAXED_WEATHERED_CUT_COPPER);
		addDrop(Blocks.WAXED_EXPOSED_CUT_COPPER);
		addDrop(Blocks.WAXED_OXIDIZED_CUT_COPPER);
		addDrop(Blocks.WAXED_CUT_COPPER_STAIRS);
		addDrop(Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS);
		addDrop(Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS);
		addDrop(Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS);
		addDrop(Blocks.CUT_COPPER_STAIRS);
		addDrop(Blocks.EXPOSED_CUT_COPPER_STAIRS);
		addDrop(Blocks.WEATHERED_CUT_COPPER_STAIRS);
		addDrop(Blocks.OXIDIZED_CUT_COPPER_STAIRS);
		addDrop(Blocks.LIGHTNING_ROD);
		addDrop(Blocks.EXPOSED_LIGHTNING_ROD);
		addDrop(Blocks.WEATHERED_LIGHTNING_ROD);
		addDrop(Blocks.OXIDIZED_LIGHTNING_ROD);
		addDrop(Blocks.WAXED_LIGHTNING_ROD);
		addDrop(Blocks.WAXED_EXPOSED_LIGHTNING_ROD);
		addDrop(Blocks.WAXED_WEATHERED_LIGHTNING_ROD);
		addDrop(Blocks.WAXED_OXIDIZED_LIGHTNING_ROD);
		addDrop(Blocks.POINTED_DRIPSTONE);
		addDrop(Blocks.DRIPSTONE_BLOCK);
		addDrop(Blocks.SPORE_BLOSSOM);
		addDrop(Blocks.FLOWERING_AZALEA);
		addDrop(Blocks.AZALEA);
		addDrop(Blocks.MOSS_CARPET);
		addDrop(Blocks.PINK_PETALS, segmentedDrops(Blocks.PINK_PETALS));
		addDrop(Blocks.WILDFLOWERS, segmentedDrops(Blocks.WILDFLOWERS));
		addDrop(Blocks.LEAF_LITTER, segmentedDrops(Blocks.LEAF_LITTER));
		addDrop(Blocks.BIG_DRIPLEAF);
		addDrop(Blocks.MOSS_BLOCK);
		addDrop(Blocks.PALE_MOSS_CARPET, block -> paleMossCarpetDrops(block));
		addDrop(Blocks.PALE_HANGING_MOSS, block -> dropsWithSilkTouchOrShears(block));
		addDrop(Blocks.PALE_MOSS_BLOCK);
		addDrop(Blocks.ROOTED_DIRT);
		addDrop(Blocks.COBBLED_DEEPSLATE);
		addDrop(Blocks.COBBLED_DEEPSLATE_STAIRS);
		addDrop(Blocks.COBBLED_DEEPSLATE_WALL);
		addDrop(Blocks.POLISHED_DEEPSLATE);
		addDrop(Blocks.POLISHED_DEEPSLATE_STAIRS);
		addDrop(Blocks.POLISHED_DEEPSLATE_WALL);
		addDrop(Blocks.DEEPSLATE_TILES);
		addDrop(Blocks.DEEPSLATE_TILE_STAIRS);
		addDrop(Blocks.DEEPSLATE_TILE_WALL);
		addDrop(Blocks.DEEPSLATE_BRICKS);
		addDrop(Blocks.DEEPSLATE_BRICK_STAIRS);
		addDrop(Blocks.DEEPSLATE_BRICK_WALL);
		addDrop(Blocks.CHISELED_DEEPSLATE);
		addDrop(Blocks.CRACKED_DEEPSLATE_BRICKS);
		addDrop(Blocks.CRACKED_DEEPSLATE_TILES);
		addDrop(Blocks.RAW_IRON_BLOCK);
		addDrop(Blocks.RAW_COPPER_BLOCK);
		addDrop(Blocks.RAW_GOLD_BLOCK);
		addDrop(Blocks.OCHRE_FROGLIGHT);
		addDrop(Blocks.VERDANT_FROGLIGHT);
		addDrop(Blocks.PEARLESCENT_FROGLIGHT);
		addDrop(Blocks.MANGROVE_ROOTS);
		addDrop(Blocks.MANGROVE_LOG);
		addDrop(Blocks.MUD);
		addDrop(Blocks.PACKED_MUD);
		addDrop(Blocks.CRAFTER);
		addDrop(Blocks.CHISELED_TUFF);
		addDrop(Blocks.TUFF_STAIRS);
		addDrop(Blocks.TUFF_WALL);
		addDrop(Blocks.POLISHED_TUFF);
		addDrop(Blocks.POLISHED_TUFF_STAIRS);
		addDrop(Blocks.POLISHED_TUFF_WALL);
		addDrop(Blocks.TUFF_BRICKS);
		addDrop(Blocks.TUFF_BRICK_STAIRS);
		addDrop(Blocks.TUFF_BRICK_WALL);
		addDrop(Blocks.CHISELED_TUFF_BRICKS);
		addDrop(Blocks.TUFF_SLAB, block -> slabDrops(block));
		addDrop(Blocks.TUFF_BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.POLISHED_TUFF_SLAB, block -> slabDrops(block));
		addDrop(Blocks.CHISELED_COPPER);
		addDrop(Blocks.EXPOSED_CHISELED_COPPER);
		addDrop(Blocks.WEATHERED_CHISELED_COPPER);
		addDrop(Blocks.OXIDIZED_CHISELED_COPPER);
		addDrop(Blocks.WAXED_CHISELED_COPPER);
		addDrop(Blocks.WAXED_EXPOSED_CHISELED_COPPER);
		addDrop(Blocks.WAXED_WEATHERED_CHISELED_COPPER);
		addDrop(Blocks.WAXED_OXIDIZED_CHISELED_COPPER);
		addDrop(Blocks.COPPER_GRATE);
		addDrop(Blocks.EXPOSED_COPPER_GRATE);
		addDrop(Blocks.WEATHERED_COPPER_GRATE);
		addDrop(Blocks.OXIDIZED_COPPER_GRATE);
		addDrop(Blocks.WAXED_COPPER_GRATE);
		addDrop(Blocks.WAXED_EXPOSED_COPPER_GRATE);
		addDrop(Blocks.WAXED_WEATHERED_COPPER_GRATE);
		addDrop(Blocks.WAXED_OXIDIZED_COPPER_GRATE);
		addDrop(Blocks.COPPER_BULB);
		addDrop(Blocks.EXPOSED_COPPER_BULB);
		addDrop(Blocks.WEATHERED_COPPER_BULB);
		addDrop(Blocks.OXIDIZED_COPPER_BULB);
		addDrop(Blocks.WAXED_COPPER_BULB);
		addDrop(Blocks.WAXED_EXPOSED_COPPER_BULB);
		addDrop(Blocks.WAXED_WEATHERED_COPPER_BULB);
		addDrop(Blocks.WAXED_OXIDIZED_COPPER_BULB);
		addDrop(Blocks.COPPER_GOLEM_STATUE, block -> copperGolemStatueDrops(block));
		addDrop(Blocks.EXPOSED_COPPER_GOLEM_STATUE, block -> copperGolemStatueDrops(block));
		addDrop(Blocks.WEATHERED_COPPER_GOLEM_STATUE, block -> copperGolemStatueDrops(block));
		addDrop(Blocks.OXIDIZED_COPPER_GOLEM_STATUE, block -> copperGolemStatueDrops(block));
		addDrop(Blocks.WAXED_COPPER_GOLEM_STATUE, block -> copperGolemStatueDrops(block));
		addDrop(Blocks.WAXED_EXPOSED_COPPER_GOLEM_STATUE, block -> copperGolemStatueDrops(block));
		addDrop(Blocks.WAXED_WEATHERED_COPPER_GOLEM_STATUE, block -> copperGolemStatueDrops(block));
		addDrop(Blocks.WAXED_OXIDIZED_COPPER_GOLEM_STATUE, block -> copperGolemStatueDrops(block));
		addDrop(Blocks.HEAVY_CORE);
		addDrop(Blocks.FIREFLY_BUSH);
		addDrop(Blocks.CACTUS_FLOWER);
		addDrop(Blocks.FARMLAND, Blocks.DIRT);
		addDrop(Blocks.TRIPWIRE, Items.STRING);
		addDrop(Blocks.DIRT_PATH, Blocks.DIRT);
		addDrop(Blocks.KELP_PLANT, Blocks.KELP);
		addDrop(Blocks.BAMBOO_SAPLING, Blocks.BAMBOO);
		addDrop(Blocks.WATER_CAULDRON, Blocks.CAULDRON);
		addDrop(Blocks.LAVA_CAULDRON, Blocks.CAULDRON);
		addDrop(Blocks.POWDER_SNOW_CAULDRON, Blocks.CAULDRON);
		addDrop(Blocks.BIG_DRIPLEAF_STEM, Blocks.BIG_DRIPLEAF);
		addDrop(Blocks.STONE, block -> drops(block, Blocks.COBBLESTONE));
		addDrop(Blocks.DEEPSLATE, block -> drops(block, Blocks.COBBLED_DEEPSLATE));
		addDrop(Blocks.GRASS_BLOCK, block -> drops(block, Blocks.DIRT));
		addDrop(Blocks.PODZOL, block -> drops(block, Blocks.DIRT));
		addDrop(Blocks.MYCELIUM, block -> drops(block, Blocks.DIRT));
		addDrop(Blocks.TUBE_CORAL_BLOCK, block -> drops(block, Blocks.DEAD_TUBE_CORAL_BLOCK));
		addDrop(Blocks.BRAIN_CORAL_BLOCK, block -> drops(block, Blocks.DEAD_BRAIN_CORAL_BLOCK));
		addDrop(Blocks.BUBBLE_CORAL_BLOCK, block -> drops(block, Blocks.DEAD_BUBBLE_CORAL_BLOCK));
		addDrop(Blocks.FIRE_CORAL_BLOCK, block -> drops(block, Blocks.DEAD_FIRE_CORAL_BLOCK));
		addDrop(Blocks.HORN_CORAL_BLOCK, block -> drops(block, Blocks.DEAD_HORN_CORAL_BLOCK));
		addDrop(Blocks.CRIMSON_NYLIUM, block -> drops(block, Blocks.NETHERRACK));
		addDrop(Blocks.WARPED_NYLIUM, block -> drops(block, Blocks.NETHERRACK));
		addDrop(Blocks.BOOKSHELF, block -> drops(block, Items.BOOK, ConstantLootNumberProvider.create(3.0F)));
		addDrop(Blocks.CLAY, block -> drops(block, Items.CLAY_BALL, ConstantLootNumberProvider.create(4.0F)));
		addDrop(
				Blocks.ENDER_CHEST,
				block -> drops(block, Blocks.OBSIDIAN, ConstantLootNumberProvider.create(8.0F))
		);
		addDrop(
				Blocks.SNOW_BLOCK,
				block -> drops(block, Items.SNOWBALL, ConstantLootNumberProvider.create(4.0F))
		);
		addDrop(Blocks.CHORUS_PLANT, drops(Items.CHORUS_FRUIT, UniformLootNumberProvider.create(0.0F, 1.0F)));
		addPottedPlantDrops(Blocks.POTTED_OAK_SAPLING);
		addPottedPlantDrops(Blocks.POTTED_SPRUCE_SAPLING);
		addPottedPlantDrops(Blocks.POTTED_BIRCH_SAPLING);
		addPottedPlantDrops(Blocks.POTTED_JUNGLE_SAPLING);
		addPottedPlantDrops(Blocks.POTTED_ACACIA_SAPLING);
		addPottedPlantDrops(Blocks.POTTED_DARK_OAK_SAPLING);
		addPottedPlantDrops(Blocks.POTTED_PALE_OAK_SAPLING);
		addPottedPlantDrops(Blocks.POTTED_MANGROVE_PROPAGULE);
		addPottedPlantDrops(Blocks.POTTED_CHERRY_SAPLING);
		addPottedPlantDrops(Blocks.POTTED_FERN);
		addPottedPlantDrops(Blocks.POTTED_DANDELION);
		addPottedPlantDrops(Blocks.POTTED_POPPY);
		addPottedPlantDrops(Blocks.POTTED_OPEN_EYEBLOSSOM);
		addPottedPlantDrops(Blocks.POTTED_CLOSED_EYEBLOSSOM);
		addPottedPlantDrops(Blocks.POTTED_BLUE_ORCHID);
		addPottedPlantDrops(Blocks.POTTED_ALLIUM);
		addPottedPlantDrops(Blocks.POTTED_AZURE_BLUET);
		addPottedPlantDrops(Blocks.POTTED_RED_TULIP);
		addPottedPlantDrops(Blocks.POTTED_ORANGE_TULIP);
		addPottedPlantDrops(Blocks.POTTED_WHITE_TULIP);
		addPottedPlantDrops(Blocks.POTTED_PINK_TULIP);
		addPottedPlantDrops(Blocks.POTTED_OXEYE_DAISY);
		addPottedPlantDrops(Blocks.POTTED_CORNFLOWER);
		addPottedPlantDrops(Blocks.POTTED_LILY_OF_THE_VALLEY);
		addPottedPlantDrops(Blocks.POTTED_WITHER_ROSE);
		addPottedPlantDrops(Blocks.POTTED_RED_MUSHROOM);
		addPottedPlantDrops(Blocks.POTTED_BROWN_MUSHROOM);
		addPottedPlantDrops(Blocks.POTTED_DEAD_BUSH);
		addPottedPlantDrops(Blocks.POTTED_CACTUS);
		addPottedPlantDrops(Blocks.POTTED_BAMBOO);
		addPottedPlantDrops(Blocks.POTTED_CRIMSON_FUNGUS);
		addPottedPlantDrops(Blocks.POTTED_WARPED_FUNGUS);
		addPottedPlantDrops(Blocks.POTTED_CRIMSON_ROOTS);
		addPottedPlantDrops(Blocks.POTTED_WARPED_ROOTS);
		addPottedPlantDrops(Blocks.POTTED_AZALEA_BUSH);
		addPottedPlantDrops(Blocks.POTTED_FLOWERING_AZALEA_BUSH);
		addPottedPlantDrops(Blocks.POTTED_TORCHFLOWER);
		addDrop(Blocks.OAK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.PETRIFIED_OAK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.SPRUCE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.BIRCH_SLAB, block -> slabDrops(block));
		addDrop(Blocks.JUNGLE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.ACACIA_SLAB, block -> slabDrops(block));
		addDrop(Blocks.DARK_OAK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.PALE_OAK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.MANGROVE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.CHERRY_SLAB, block -> slabDrops(block));
		addDrop(Blocks.BAMBOO_SLAB, block -> slabDrops(block));
		addDrop(Blocks.BAMBOO_MOSAIC_SLAB, block -> slabDrops(block));
		addDrop(Blocks.BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.COBBLESTONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.DARK_PRISMARINE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.NETHER_BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.PRISMARINE_BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.PRISMARINE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.PURPUR_SLAB, block -> slabDrops(block));
		addDrop(Blocks.QUARTZ_SLAB, block -> slabDrops(block));
		addDrop(Blocks.RED_SANDSTONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.SANDSTONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.CUT_RED_SANDSTONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.CUT_SANDSTONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.STONE_BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.STONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.SMOOTH_STONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.POLISHED_GRANITE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.SMOOTH_RED_SANDSTONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.MOSSY_STONE_BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.POLISHED_DIORITE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.MOSSY_COBBLESTONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.END_STONE_BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.SMOOTH_SANDSTONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.SMOOTH_QUARTZ_SLAB, block -> slabDrops(block));
		addDrop(Blocks.GRANITE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.ANDESITE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.RED_NETHER_BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.POLISHED_ANDESITE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.DIORITE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.CRIMSON_SLAB, block -> slabDrops(block));
		addDrop(Blocks.WARPED_SLAB, block -> slabDrops(block));
		addDrop(Blocks.BLACKSTONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.POLISHED_BLACKSTONE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.OXIDIZED_CUT_COPPER_SLAB, block -> slabDrops(block));
		addDrop(Blocks.WEATHERED_CUT_COPPER_SLAB, block -> slabDrops(block));
		addDrop(Blocks.EXPOSED_CUT_COPPER_SLAB, block -> slabDrops(block));
		addDrop(Blocks.CUT_COPPER_SLAB, block -> slabDrops(block));
		addDrop(Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, block -> slabDrops(block));
		addDrop(Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, block -> slabDrops(block));
		addDrop(Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, block -> slabDrops(block));
		addDrop(Blocks.WAXED_CUT_COPPER_SLAB, block -> slabDrops(block));
		addDrop(Blocks.COBBLED_DEEPSLATE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.POLISHED_DEEPSLATE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.DEEPSLATE_TILE_SLAB, block -> slabDrops(block));
		addDrop(Blocks.DEEPSLATE_BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.MUD_BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.RESIN_BRICK_SLAB, block -> slabDrops(block));
		addDrop(Blocks.OAK_DOOR, block -> doorDrops(block));
		addDrop(Blocks.SPRUCE_DOOR, block -> doorDrops(block));
		addDrop(Blocks.BIRCH_DOOR, block -> doorDrops(block));
		addDrop(Blocks.JUNGLE_DOOR, block -> doorDrops(block));
		addDrop(Blocks.ACACIA_DOOR, block -> doorDrops(block));
		addDrop(Blocks.DARK_OAK_DOOR, block -> doorDrops(block));
		addDrop(Blocks.PALE_OAK_DOOR, block -> doorDrops(block));
		addDrop(Blocks.MANGROVE_DOOR, block -> doorDrops(block));
		addDrop(Blocks.CHERRY_DOOR, block -> doorDrops(block));
		addDrop(Blocks.BAMBOO_DOOR, block -> doorDrops(block));
		addDrop(Blocks.WARPED_DOOR, block -> doorDrops(block));
		addDrop(Blocks.CRIMSON_DOOR, block -> doorDrops(block));
		addDrop(Blocks.IRON_DOOR, block -> doorDrops(block));
		addDrop(Blocks.COPPER_DOOR, block -> doorDrops(block));
		addDrop(Blocks.EXPOSED_COPPER_DOOR, block -> doorDrops(block));
		addDrop(Blocks.WEATHERED_COPPER_DOOR, block -> doorDrops(block));
		addDrop(Blocks.OXIDIZED_COPPER_DOOR, block -> doorDrops(block));
		addDrop(Blocks.WAXED_COPPER_DOOR, block -> doorDrops(block));
		addDrop(Blocks.WAXED_EXPOSED_COPPER_DOOR, block -> doorDrops(block));
		addDrop(Blocks.WAXED_WEATHERED_COPPER_DOOR, block -> doorDrops(block));
		addDrop(Blocks.WAXED_OXIDIZED_COPPER_DOOR, block -> doorDrops(block));
		addDrop(Blocks.BLACK_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.BLUE_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.BROWN_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.CYAN_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.GRAY_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.GREEN_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.LIGHT_BLUE_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.LIGHT_GRAY_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.LIME_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.MAGENTA_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.PURPLE_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.ORANGE_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.PINK_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.RED_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.WHITE_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.YELLOW_BED, block -> dropsWithProperty(block, BedBlock.PART, BedPart.HEAD));
		addDrop(Blocks.LILAC, block -> dropsWithProperty(block, TallPlantBlock.HALF, DoubleBlockHalf.LOWER));
		addDrop(
				Blocks.SUNFLOWER,
				block -> dropsWithProperty(block, TallPlantBlock.HALF, DoubleBlockHalf.LOWER)
		);
		addDrop(Blocks.PEONY, block -> dropsWithProperty(block, TallPlantBlock.HALF, DoubleBlockHalf.LOWER));
		addDrop(
				Blocks.ROSE_BUSH,
				block -> dropsWithProperty(block, TallPlantBlock.HALF, DoubleBlockHalf.LOWER)
		);
		addDrop(
				Blocks.TNT,
				LootTable.builder()
				         .pool(
						         addSurvivesExplosionCondition(
								         Blocks.TNT,
								         LootPool.builder()
								                 .rolls(ConstantLootNumberProvider.create(1.0F))
								                 .with(
										                 ItemEntry.builder(Blocks.TNT)
										                          .conditionally(
												                          BlockStatePropertyLootCondition
														                          .builder(Blocks.TNT)
														                          .properties(StatePredicate.Builder
																                          .create()
																                          .exactMatch(
																		                          TntBlock.UNSTABLE,
																		                          false
																                          ))
										                          )
								                 )
						         )
				         )
		);
		addDrop(
				Blocks.COCOA,
				block -> LootTable.builder()
				                  .pool(
						                  LootPool.builder()
						                          .rolls(ConstantLootNumberProvider.create(1.0F))
						                          .with(
								                          (LootPoolEntry.Builder<?>) applyExplosionDecay(
										                          block,
										                          ItemEntry.builder(Items.COCOA_BEANS)
										                                   .apply(
												                                   SetCountLootFunction
														                                   .builder(
																                                   ConstantLootNumberProvider.create(
																		                                   3.0F))
														                                   .conditionally(
																                                   BlockStatePropertyLootCondition
																		                                   .builder(
																				                                   block)
																		                                   .properties(
																				                                   StatePredicate.Builder
																						                                   .create()
																						                                   .exactMatch(
																								                                   CocoaBlock.AGE,
																								                                   2
																						                                   ))
														                                   )
										                                   )
								                          )
						                          )
				                  )
		);
		addDrop(
				Blocks.SEA_PICKLE,
				block -> LootTable.builder()
				                  .pool(
						                  LootPool.builder()
						                          .rolls(ConstantLootNumberProvider.create(1.0F))
						                          .with(
								                          (LootPoolEntry.Builder<?>) applyExplosionDecay(
										                          Blocks.SEA_PICKLE,
										                          ItemEntry.builder(block)
										                                   .apply(
												                                   List.of(2, 3, 4),
												                                   pickles -> SetCountLootFunction
														                                   .builder(
																                                   ConstantLootNumberProvider.create(
																		                                   pickles.intValue()))
														                                   .conditionally(
																                                   BlockStatePropertyLootCondition
																		                                   .builder(
																				                                   block)
																		                                   .properties(
																				                                   StatePredicate.Builder
																						                                   .create()
																						                                   .exactMatch(
																								                                   SeaPickleBlock.PICKLES,
																								                                   pickles.intValue()
																						                                   ))
														                                   )
										                                   )
								                          )
						                          )
				                  )
		);
		addDrop(
				Blocks.COMPOSTER,
				block -> LootTable.builder()
				                  .pool(LootPool
						                  .builder()
						                  .with((LootPoolEntry.Builder<?>) applyExplosionDecay(
								                  block,
								                  ItemEntry.builder(Items.COMPOSTER)
						                  )))
				                  .pool(
						                  LootPool.builder()
						                          .with(ItemEntry.builder(Items.BONE_MEAL))
						                          .conditionally(BlockStatePropertyLootCondition
								                          .builder(block)
								                          .properties(StatePredicate.Builder
										                          .create()
										                          .exactMatch(ComposterBlock.LEVEL, 8)))
				                  )
		);
		addDrop(Blocks.CAVE_VINES, block -> glowBerryDrops(block));
		addDrop(Blocks.CAVE_VINES_PLANT, block -> glowBerryDrops(block));
		addDrop(Blocks.CANDLE, block -> candleDrops(block));
		addDrop(Blocks.WHITE_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.ORANGE_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.MAGENTA_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.LIGHT_BLUE_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.YELLOW_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.LIME_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.PINK_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.GRAY_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.LIGHT_GRAY_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.CYAN_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.PURPLE_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.BLUE_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.BROWN_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.GREEN_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.RED_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.BLACK_CANDLE, block -> candleDrops(block));
		addDrop(Blocks.BEACON, block -> nameableContainerDrops(block));
		addDrop(Blocks.BREWING_STAND, block -> nameableContainerDrops(block));
		addDrop(Blocks.CHEST, block -> nameableContainerDrops(block));
		addDrop(Blocks.COPPER_CHEST, block -> nameableContainerDrops(block));
		addDrop(Blocks.EXPOSED_COPPER_CHEST, block -> nameableContainerDrops(block));
		addDrop(Blocks.WEATHERED_COPPER_CHEST, block -> nameableContainerDrops(block));
		addDrop(Blocks.OXIDIZED_COPPER_CHEST, block -> nameableContainerDrops(block));
		addDrop(Blocks.WAXED_COPPER_CHEST, block -> nameableContainerDrops(block));
		addDrop(Blocks.WAXED_EXPOSED_COPPER_CHEST, block -> nameableContainerDrops(block));
		addDrop(Blocks.WAXED_WEATHERED_COPPER_CHEST, block -> nameableContainerDrops(block));
		addDrop(Blocks.WAXED_OXIDIZED_COPPER_CHEST, block -> nameableContainerDrops(block));
		addDrop(Blocks.DISPENSER, block -> nameableContainerDrops(block));
		addDrop(Blocks.DROPPER, block -> nameableContainerDrops(block));
		addDrop(Blocks.ENCHANTING_TABLE, block -> nameableContainerDrops(block));
		addDrop(Blocks.FURNACE, block -> nameableContainerDrops(block));
		addDrop(Blocks.HOPPER, block -> nameableContainerDrops(block));
		addDrop(Blocks.TRAPPED_CHEST, block -> nameableContainerDrops(block));
		addDrop(Blocks.SMOKER, block -> nameableContainerDrops(block));
		addDrop(Blocks.BLAST_FURNACE, block -> nameableContainerDrops(block));
		addDrop(Blocks.BARREL, block -> nameableContainerDrops(block));
		addDrop(Blocks.CARTOGRAPHY_TABLE);
		addDrop(Blocks.FLETCHING_TABLE);
		addDrop(Blocks.GRINDSTONE);
		addDrop(Blocks.LECTERN);
		addDrop(Blocks.SMITHING_TABLE);
		addDrop(Blocks.STONECUTTER);
		addDrop(Blocks.ACACIA_SHELF);
		addDrop(Blocks.BAMBOO_SHELF);
		addDrop(Blocks.BIRCH_SHELF);
		addDrop(Blocks.CHERRY_SHELF);
		addDrop(Blocks.CRIMSON_SHELF);
		addDrop(Blocks.DARK_OAK_SHELF);
		addDrop(Blocks.JUNGLE_SHELF);
		addDrop(Blocks.MANGROVE_SHELF);
		addDrop(Blocks.OAK_SHELF);
		addDrop(Blocks.PALE_OAK_SHELF);
		addDrop(Blocks.SPRUCE_SHELF);
		addDrop(Blocks.WARPED_SHELF);
		addDrop(Blocks.BELL, this::drops);
		addDrop(Blocks.LANTERN, this::drops);
		addDrop(Blocks.SOUL_LANTERN, this::drops);
		Blocks.COPPER_LANTERNS.forEach(block -> addDrop(block, this::drops));
		addDrop(Blocks.SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.BLACK_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.BLUE_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.BROWN_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.CYAN_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.GRAY_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.GREEN_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.LIGHT_BLUE_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.LIGHT_GRAY_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.LIME_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.MAGENTA_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.ORANGE_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.PINK_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.PURPLE_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.RED_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.WHITE_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.YELLOW_SHULKER_BOX, block -> shulkerBoxDrops(block));
		addDrop(Blocks.BLACK_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.BLUE_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.BROWN_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.CYAN_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.GRAY_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.GREEN_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.LIGHT_BLUE_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.LIGHT_GRAY_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.LIME_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.MAGENTA_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.ORANGE_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.PINK_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.PURPLE_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.RED_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.WHITE_BANNER, block -> bannerDrops(block));
		addDrop(Blocks.YELLOW_BANNER, block -> bannerDrops(block));
		addDrop(
				Blocks.PLAYER_HEAD,
				block -> LootTable.builder()
				                  .pool(
						                  addSurvivesExplosionCondition(
								                  block,
								                  LootPool.builder()
								                          .rolls(ConstantLootNumberProvider.create(1.0F))
								                          .with(
										                          ItemEntry.builder(block)
										                                   .apply(
												                                   CopyComponentsLootFunction
														                                   .blockEntity(
																                                   LootContextParameters.BLOCK_ENTITY)
														                                   .include(DataComponentTypes.PROFILE)
														                                   .include(DataComponentTypes.NOTE_BLOCK_SOUND)
														                                   .include(DataComponentTypes.CUSTOM_NAME)
										                                   )
								                          )
						                  )
				                  )
		);
		addDrop(Blocks.SKELETON_SKULL, this::skullDrops);
		addDrop(Blocks.WITHER_SKELETON_SKULL, this::skullDrops);
		addDrop(Blocks.ZOMBIE_HEAD, this::skullDrops);
		addDrop(Blocks.CREEPER_HEAD, this::skullDrops);
		addDrop(Blocks.PIGLIN_HEAD, this::skullDrops);
		addDrop(Blocks.DRAGON_HEAD, this::skullDrops);
		addDrop(Blocks.BEE_NEST, block -> beeNestDrops(block));
		addDrop(Blocks.BEEHIVE, block -> beehiveDrops(block));
		addDrop(Blocks.OAK_LEAVES, block -> oakLeavesDrops(block, Blocks.OAK_SAPLING, SAPLING_DROP_CHANCE));
		addDrop(
				Blocks.SPRUCE_LEAVES,
				block -> leavesDrops(block, Blocks.SPRUCE_SAPLING, SAPLING_DROP_CHANCE)
		);
		addDrop(Blocks.BIRCH_LEAVES, block -> leavesDrops(block, Blocks.BIRCH_SAPLING, SAPLING_DROP_CHANCE));
		addDrop(
				Blocks.JUNGLE_LEAVES,
				block -> leavesDrops(block, Blocks.JUNGLE_SAPLING, JUNGLE_SAPLING_DROP_CHANCE)
		);
		addDrop(
				Blocks.ACACIA_LEAVES,
				block -> leavesDrops(block, Blocks.ACACIA_SAPLING, SAPLING_DROP_CHANCE)
		);
		addDrop(
				Blocks.DARK_OAK_LEAVES,
				block -> oakLeavesDrops(block, Blocks.DARK_OAK_SAPLING, SAPLING_DROP_CHANCE)
		);
		addDrop(
				Blocks.PALE_OAK_LEAVES,
				block -> leavesDrops(block, Blocks.PALE_OAK_SAPLING, SAPLING_DROP_CHANCE)
		);
		addDrop(
				Blocks.CHERRY_LEAVES,
				block -> leavesDrops(block, Blocks.CHERRY_SAPLING, SAPLING_DROP_CHANCE)
		);
		addDrop(Blocks.AZALEA_LEAVES, block -> leavesDrops(block, Blocks.AZALEA, SAPLING_DROP_CHANCE));
		addDrop(
				Blocks.FLOWERING_AZALEA_LEAVES,
				block -> leavesDrops(block, Blocks.FLOWERING_AZALEA, SAPLING_DROP_CHANCE)
		);
		LootCondition.Builder builder = BlockStatePropertyLootCondition.builder(Blocks.BEETROOTS)
		                                                               .properties(StatePredicate.Builder
				                                                               .create()
				                                                               .exactMatch(BeetrootsBlock.AGE, 3));
		addDrop(Blocks.BEETROOTS, cropDrops(Blocks.BEETROOTS, Items.BEETROOT, Items.BEETROOT_SEEDS, builder));
		LootCondition.Builder builder2 = BlockStatePropertyLootCondition.builder(Blocks.WHEAT)
		                                                                .properties(StatePredicate.Builder
				                                                                .create()
				                                                                .exactMatch(CropBlock.AGE, 7));
		addDrop(Blocks.WHEAT, cropDrops(Blocks.WHEAT, Items.WHEAT, Items.WHEAT_SEEDS, builder2));
		LootCondition.Builder builder3 = BlockStatePropertyLootCondition.builder(Blocks.CARROTS)
		                                                                .properties(StatePredicate.Builder
				                                                                .create()
				                                                                .exactMatch(CarrotsBlock.AGE, 7));
		LootCondition.Builder builder4 = BlockStatePropertyLootCondition.builder(Blocks.MANGROVE_PROPAGULE)
		                                                                .properties(StatePredicate.Builder
				                                                                .create()
				                                                                .exactMatch(PropaguleBlock.AGE, 4));
		addDrop(
				Blocks.MANGROVE_PROPAGULE,
				applyExplosionDecay(
						Blocks.MANGROVE_PROPAGULE,
						LootTable
								.builder()
								.pool(LootPool
										.builder()
										.conditionally(builder4)
										.with(ItemEntry.builder(Items.MANGROVE_PROPAGULE)))
				)
		);
		addDrop(
				Blocks.TORCHFLOWER_CROP,
				applyExplosionDecay(
						Blocks.TORCHFLOWER_CROP,
						LootTable.builder().pool(LootPool.builder().with(ItemEntry.builder(Items.TORCHFLOWER_SEEDS)))
				)
		);
		addDrop(Blocks.SNIFFER_EGG);
		addDrop(Blocks.DRIED_GHAST);
		addDrop(Blocks.PITCHER_CROP, block -> this.pitcherCropDrops());
		addDrop(Blocks.PITCHER_PLANT);
		addDrop(
				Blocks.PITCHER_PLANT,
				applyExplosionDecay(
						Blocks.PITCHER_PLANT,
						LootTable.builder()
						         .pool(
								         LootPool.builder()
								                 .with(
										                 ItemEntry.builder(Items.PITCHER_PLANT)
										                          .conditionally(
												                          BlockStatePropertyLootCondition
														                          .builder(Blocks.PITCHER_PLANT)
														                          .properties(StatePredicate.Builder
																                          .create()
																                          .exactMatch(
																		                          TallPlantBlock.HALF,
																		                          DoubleBlockHalf.LOWER
																                          ))
										                          )
								                 )
						         )
				)
		);
		addDrop(
				Blocks.CARROTS,
				applyExplosionDecay(
						Blocks.CARROTS,
						LootTable.builder()
						         .pool(LootPool.builder().with(ItemEntry.builder(Items.CARROT)))
						         .pool(
								         LootPool.builder()
								                 .conditionally(builder3)
								                 .with(
										                 ItemEntry.builder(Items.CARROT)
										                          .apply(ApplyBonusLootFunction.binomialWithBonusCount(
												                          impl.getOrThrow(Enchantments.FORTUNE),
												                          0.5714286F,
												                          3
										                          ))
								                 )
						         )
				)
		);
		LootCondition.Builder builder5 = BlockStatePropertyLootCondition.builder(Blocks.POTATOES)
		                                                                .properties(StatePredicate.Builder
				                                                                .create()
				                                                                .exactMatch(PotatoesBlock.AGE, 7));
		addDrop(
				Blocks.POTATOES,
				applyExplosionDecay(
						Blocks.POTATOES,
						LootTable.builder()
						         .pool(LootPool.builder().with(ItemEntry.builder(Items.POTATO)))
						         .pool(
								         LootPool.builder()
								                 .conditionally(builder5)
								                 .with(
										                 ItemEntry.builder(Items.POTATO)
										                          .apply(ApplyBonusLootFunction.binomialWithBonusCount(
												                          impl.getOrThrow(Enchantments.FORTUNE),
												                          0.5714286F,
												                          3
										                          ))
								                 )
						         )
						         .pool(
								         LootPool.builder()
								                 .conditionally(builder5)
								                 .with(ItemEntry
										                 .builder(Items.POISONOUS_POTATO)
										                 .conditionally(RandomChanceLootCondition.builder(0.02F)))
						         )
				)
		);
		addDrop(
				Blocks.SWEET_BERRY_BUSH,
				block -> applyExplosionDecay(
						block,
						LootTable.builder()
						         .pool(
								         LootPool.builder()
								                 .conditionally(
										                 BlockStatePropertyLootCondition
												                 .builder(Blocks.SWEET_BERRY_BUSH)
												                 .properties(StatePredicate.Builder
														                 .create()
														                 .exactMatch(SweetBerryBushBlock.AGE, 3))
								                 )
								                 .with(ItemEntry.builder(Items.SWEET_BERRIES))
								                 .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(
										                 2.0F,
										                 3.0F
								                 )))
								                 .apply(ApplyBonusLootFunction.uniformBonusCount(impl.getOrThrow(
										                 Enchantments.FORTUNE)))
						         )
						         .pool(
								         LootPool.builder()
								                 .conditionally(
										                 BlockStatePropertyLootCondition
												                 .builder(Blocks.SWEET_BERRY_BUSH)
												                 .properties(StatePredicate.Builder
														                 .create()
														                 .exactMatch(SweetBerryBushBlock.AGE, 2))
								                 )
								                 .with(ItemEntry.builder(Items.SWEET_BERRIES))
								                 .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(
										                 1.0F,
										                 2.0F
								                 )))
								                 .apply(ApplyBonusLootFunction.uniformBonusCount(impl.getOrThrow(
										                 Enchantments.FORTUNE)))
						         )
				)
		);
		addDrop(Blocks.BROWN_MUSHROOM_BLOCK, block -> mushroomBlockDrops(block, Blocks.BROWN_MUSHROOM));
		addDrop(Blocks.RED_MUSHROOM_BLOCK, block -> mushroomBlockDrops(block, Blocks.RED_MUSHROOM));
		addDrop(Blocks.COAL_ORE, block -> oreDrops(block, Items.COAL));
		addDrop(Blocks.DEEPSLATE_COAL_ORE, block -> oreDrops(block, Items.COAL));
		addDrop(Blocks.EMERALD_ORE, block -> oreDrops(block, Items.EMERALD));
		addDrop(Blocks.DEEPSLATE_EMERALD_ORE, block -> oreDrops(block, Items.EMERALD));
		addDrop(Blocks.NETHER_QUARTZ_ORE, block -> oreDrops(block, Items.QUARTZ));
		addDrop(Blocks.DIAMOND_ORE, block -> oreDrops(block, Items.DIAMOND));
		addDrop(Blocks.DEEPSLATE_DIAMOND_ORE, block -> oreDrops(block, Items.DIAMOND));
		addDrop(Blocks.COPPER_ORE, block -> copperOreDrops(block));
		addDrop(Blocks.DEEPSLATE_COPPER_ORE, block -> copperOreDrops(block));
		addDrop(Blocks.IRON_ORE, block -> oreDrops(block, Items.RAW_IRON));
		addDrop(Blocks.DEEPSLATE_IRON_ORE, block -> oreDrops(block, Items.RAW_IRON));
		addDrop(Blocks.GOLD_ORE, block -> oreDrops(block, Items.RAW_GOLD));
		addDrop(Blocks.DEEPSLATE_GOLD_ORE, block -> oreDrops(block, Items.RAW_GOLD));
		addDrop(
				Blocks.NETHER_GOLD_ORE,
				block -> dropsWithSilkTouch(
						block,
						(LootPoolEntry.Builder<?>) applyExplosionDecay(
								block,
								ItemEntry.builder(Items.GOLD_NUGGET)
								         .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(
										         2.0F,
										         6.0F
								         )))
								         .apply(ApplyBonusLootFunction.oreDrops(impl.getOrThrow(Enchantments.FORTUNE)))
						)
				)
		);
		addDrop(Blocks.LAPIS_ORE, block -> lapisOreDrops(block));
		addDrop(Blocks.DEEPSLATE_LAPIS_ORE, block -> lapisOreDrops(block));
		addDrop(
				Blocks.COBWEB,
				block -> dropsWithSilkTouchOrShears(
						block,
						(LootPoolEntry.Builder<?>) addSurvivesExplosionCondition(
								block,
								ItemEntry.builder(Items.STRING)
						)
				)
		);
		addDrop(
				Blocks.DEAD_BUSH,
				block -> dropsWithShears(
						block,
						(LootPoolEntry.Builder<?>) applyExplosionDecay(
								block,
								ItemEntry
										.builder(Items.STICK)
										.apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(
												0.0F,
												2.0F
										)))
						)
				)
		);
		addDrop(Blocks.SHORT_DRY_GRASS, block -> dropsWithSilkTouchOrShears(block));
		addDrop(Blocks.TALL_DRY_GRASS, block -> dropsWithSilkTouchOrShears(block));
		addDrop(Blocks.BUSH, block -> dropsWithSilkTouchOrShears(block));
		addDrop(Blocks.NETHER_SPROUTS, block -> dropsWithShears(block));
		addDrop(Blocks.SEAGRASS, block -> dropsWithShears(block));
		addDrop(Blocks.VINE, block -> dropsWithShears(block));
		addDrop(Blocks.GLOW_LICHEN, block -> multifaceGrowthDrops(block, createWithShearsCondition()));
		addDrop(Blocks.RESIN_CLUMP, block -> multifaceGrowthDrops(block));
		addDrop(Blocks.HANGING_ROOTS, block -> dropsWithShears(block));
		addDrop(Blocks.SMALL_DRIPLEAF, block -> dropsWithShears(block));
		addDrop(Blocks.MANGROVE_LEAVES, block -> mangroveLeavesDrops(block));
		addDrop(Blocks.TALL_SEAGRASS, seagrassDrops(Blocks.SEAGRASS));
		addDrop(Blocks.LARGE_FERN, block -> tallPlantDrops(block, Blocks.FERN));
		addDrop(Blocks.TALL_GRASS, block -> tallPlantDrops(block, Blocks.SHORT_GRASS));
		addDrop(Blocks.MELON_STEM, block -> cropStemDrops(block, Items.MELON_SEEDS));
		addDrop(Blocks.ATTACHED_MELON_STEM, block -> attachedCropStemDrops(block, Items.MELON_SEEDS));
		addDrop(Blocks.PUMPKIN_STEM, block -> cropStemDrops(block, Items.PUMPKIN_SEEDS));
		addDrop(Blocks.ATTACHED_PUMPKIN_STEM, block -> attachedCropStemDrops(block, Items.PUMPKIN_SEEDS));
		addDrop(
				Blocks.CHORUS_FLOWER,
				block -> LootTable.builder()
				                  .pool(
						                  LootPool.builder()
						                          .rolls(ConstantLootNumberProvider.create(1.0F))
						                          .with(
								                          ((LeafEntry.Builder) addSurvivesExplosionCondition(
										                          block,
										                          ItemEntry.builder(block)
								                          )
								                          )
										                          .conditionally(EntityPropertiesLootCondition.create(
												                          LootContext.EntityReference.THIS))
						                          )
				                  )
		);
		addDrop(Blocks.FERN, block -> shortPlantDrops(block));
		addDrop(Blocks.SHORT_GRASS, block -> shortPlantDrops(block));
		addDrop(
				Blocks.GLOWSTONE,
				block -> dropsWithSilkTouch(
						block,
						(LootPoolEntry.Builder<?>) applyExplosionDecay(
								block,
								ItemEntry.builder(Items.GLOWSTONE_DUST)
								         .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(
										         2.0F,
										         4.0F
								         )))
								         .apply(ApplyBonusLootFunction.uniformBonusCount(impl.getOrThrow(Enchantments.FORTUNE)))
								         .apply(LimitCountLootFunction.builder(BoundedIntUnaryOperator.create(1, 4)))
						)
				)
		);
		addDrop(
				Blocks.MELON,
				block -> dropsWithSilkTouch(
						block,
						(LootPoolEntry.Builder<?>) applyExplosionDecay(
								block,
								ItemEntry.builder(Items.MELON_SLICE)
								         .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(
										         3.0F,
										         7.0F
								         )))
								         .apply(ApplyBonusLootFunction.uniformBonusCount(impl.getOrThrow(Enchantments.FORTUNE)))
								         .apply(LimitCountLootFunction.builder(BoundedIntUnaryOperator.createMax(9)))
						)
				)
		);
		addDrop(Blocks.REDSTONE_ORE, block -> redstoneOreDrops(block));
		addDrop(Blocks.DEEPSLATE_REDSTONE_ORE, block -> redstoneOreDrops(block));
		addDrop(
				Blocks.SEA_LANTERN,
				block -> dropsWithSilkTouch(
						block,
						(LootPoolEntry.Builder<?>) applyExplosionDecay(
								block,
								ItemEntry.builder(Items.PRISMARINE_CRYSTALS)
								         .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(
										         2.0F,
										         3.0F
								         )))
								         .apply(ApplyBonusLootFunction.uniformBonusCount(impl.getOrThrow(Enchantments.FORTUNE)))
								         .apply(LimitCountLootFunction.builder(BoundedIntUnaryOperator.create(1, 5)))
						)
				)
		);
		addDrop(
				Blocks.CREAKING_HEART,
				block -> dropsWithSilkTouch(
						block,
						(LootPoolEntry.Builder<?>) applyExplosionDecay(
								block,
								ItemEntry.builder(Items.RESIN_CLUMP)
								         .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(
										         1.0F,
										         3.0F
								         )))
								         .apply(ApplyBonusLootFunction.uniformBonusCount(impl.getOrThrow(Enchantments.FORTUNE)))
								         .apply(LimitCountLootFunction.builder(BoundedIntUnaryOperator.createMax(9)))
						)
				)
		);
		addDrop(
				Blocks.NETHER_WART,
				block -> LootTable.builder()
				                  .pool(
						                  applyExplosionDecay(
								                  block,
								                  LootPool.builder()
								                          .rolls(ConstantLootNumberProvider.create(1.0F))
								                          .with(
										                          ItemEntry.builder(Items.NETHER_WART)
										                                   .apply(
												                                   SetCountLootFunction
														                                   .builder(
																                                   UniformLootNumberProvider.create(
																		                                   2.0F,
																		                                   4.0F
																                                   ))
														                                   .conditionally(
																                                   BlockStatePropertyLootCondition
																		                                   .builder(
																				                                   block)
																		                                   .properties(
																				                                   StatePredicate.Builder
																						                                   .create()
																						                                   .exactMatch(
																								                                   NetherWartBlock.AGE,
																								                                   3
																						                                   ))
														                                   )
										                                   )
										                                   .apply(
												                                   ApplyBonusLootFunction
														                                   .uniformBonusCount(impl.getOrThrow(
																                                   Enchantments.FORTUNE))
														                                   .conditionally(
																                                   BlockStatePropertyLootCondition
																		                                   .builder(
																				                                   block)
																		                                   .properties(
																				                                   StatePredicate.Builder
																						                                   .create()
																						                                   .exactMatch(
																								                                   NetherWartBlock.AGE,
																								                                   3
																						                                   ))
														                                   )
										                                   )
								                          )
						                  )
				                  )
		);
		addDrop(
				Blocks.SNOW,
				block -> LootTable.builder()
				                  .pool(
						                  LootPool.builder()
						                          .conditionally(EntityPropertiesLootCondition.create(LootContext.EntityReference.THIS))
						                          .with(
								                          AlternativeEntry.builder(
										                          AlternativeEntry.builder(
												                                          SnowBlock.LAYERS.getValues(),
												                                          layers -> ItemEntry.builder(Items.SNOWBALL)
												                                                             .conditionally(
														                                                             BlockStatePropertyLootCondition
																                                                             .builder(
																		                                                             block)
																                                                             .properties(
																		                                                             StatePredicate.Builder
																				                                                             .create()
																				                                                             .exactMatch(
																						                                                             SnowBlock.LAYERS,
																						                                                             layers.intValue()
																				                                                             ))
												                                                             )
												                                                             .apply(SetCountLootFunction.builder(
														                                                             ConstantLootNumberProvider.create(
																                                                             layers.intValue())))
										                                          )
										                                          .conditionally(createWithoutSilkTouchCondition()),
										                          AlternativeEntry.builder(
												                          SnowBlock.LAYERS.getValues(),
												                          layers -> layers == 8
												                                    ? ItemEntry.builder(Blocks.SNOW_BLOCK)
												                                    : ItemEntry.builder(Blocks.SNOW)
												                                               .apply(SetCountLootFunction.builder(
														                                               ConstantLootNumberProvider.create(
																                                               layers.intValue())))
												                                               .conditionally(
														                                               BlockStatePropertyLootCondition
														                                               .builder(block)
														                                               .properties(
																                                               StatePredicate.Builder
																                                               .create()
																                                               .exactMatch(
																		                                               SnowBlock.LAYERS,
																		                                               layers.intValue()
																                                               ))
												                                               )
										                          )
								                          )
						                          )
				                  )
		);
		addDrop(
				Blocks.GRAVEL,
				block -> dropsWithSilkTouch(
						block,
						addSurvivesExplosionCondition(
								block,
								ItemEntry.builder(Items.FLINT)
								         .conditionally(TableBonusLootCondition.builder(
										         impl.getOrThrow(Enchantments.FORTUNE),
										         0.1F,
										         0.14285715F,
										         0.25F,
										         1.0F
								         ))
								         .alternatively(ItemEntry.builder(block))
						)
				)
		);
		addDrop(
				Blocks.CAMPFIRE,
				block -> dropsWithSilkTouch(
						block,
						(LootPoolEntry.Builder<?>) addSurvivesExplosionCondition(
								block,
								ItemEntry
										.builder(Items.CHARCOAL)
										.apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(2.0F)))
						)
				)
		);
		addDrop(
				Blocks.GILDED_BLACKSTONE,
				block -> dropsWithSilkTouch(
						block,
						addSurvivesExplosionCondition(
								block,
								ItemEntry.builder(Items.GOLD_NUGGET)
								         .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(
										         2.0F,
										         5.0F
								         )))
								         .conditionally(TableBonusLootCondition.builder(
										         impl.getOrThrow(Enchantments.FORTUNE),
										         0.1F,
										         0.14285715F,
										         0.25F,
										         1.0F
								         ))
								         .alternatively(ItemEntry.builder(block))
						)
				)
		);
		addDrop(
				Blocks.SOUL_CAMPFIRE,
				block -> dropsWithSilkTouch(
						block,
						(LootPoolEntry.Builder<?>) addSurvivesExplosionCondition(
								block,
								ItemEntry
										.builder(Items.SOUL_SOIL)
										.apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1.0F)))
						)
				)
		);
		addDrop(
				Blocks.AMETHYST_CLUSTER,
				block -> dropsWithSilkTouch(
						block,
						ItemEntry.builder(Items.AMETHYST_SHARD)
						         .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(4.0F)))
						         .apply(ApplyBonusLootFunction.oreDrops(impl.getOrThrow(Enchantments.FORTUNE)))
						         .conditionally(MatchToolLootCondition.builder(ItemPredicate.Builder
								         .create()
								         .tag(impl2, ItemTags.CLUSTER_MAX_HARVESTABLES)))
						         .alternatively(
								         (LootPoolEntry.Builder<?>) applyExplosionDecay(
										         block,
										         ItemEntry
												         .builder(Items.AMETHYST_SHARD)
												         .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(
														         2.0F)))
								         )
						         )
				)
		);
		addDropWithSilkTouch(Blocks.SMALL_AMETHYST_BUD);
		addDropWithSilkTouch(Blocks.MEDIUM_AMETHYST_BUD);
		addDropWithSilkTouch(Blocks.LARGE_AMETHYST_BUD);
		addDropWithSilkTouch(Blocks.GLASS);
		addDropWithSilkTouch(Blocks.WHITE_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.ORANGE_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.MAGENTA_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.LIGHT_BLUE_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.YELLOW_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.LIME_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.PINK_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.GRAY_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.LIGHT_GRAY_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.CYAN_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.PURPLE_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.BLUE_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.BROWN_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.GREEN_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.RED_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.BLACK_STAINED_GLASS);
		addDropWithSilkTouch(Blocks.GLASS_PANE);
		addDropWithSilkTouch(Blocks.WHITE_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.ORANGE_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.MAGENTA_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.YELLOW_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.LIME_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.PINK_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.GRAY_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.LIGHT_GRAY_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.CYAN_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.PURPLE_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.BLUE_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.BROWN_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.GREEN_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.RED_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.BLACK_STAINED_GLASS_PANE);
		addDropWithSilkTouch(Blocks.ICE);
		addDropWithSilkTouch(Blocks.PACKED_ICE);
		addDropWithSilkTouch(Blocks.BLUE_ICE);
		addDropWithSilkTouch(Blocks.TURTLE_EGG);
		addDropWithSilkTouch(Blocks.MUSHROOM_STEM);
		addDropWithSilkTouch(Blocks.DEAD_TUBE_CORAL);
		addDropWithSilkTouch(Blocks.DEAD_BRAIN_CORAL);
		addDropWithSilkTouch(Blocks.DEAD_BUBBLE_CORAL);
		addDropWithSilkTouch(Blocks.DEAD_FIRE_CORAL);
		addDropWithSilkTouch(Blocks.DEAD_HORN_CORAL);
		addDropWithSilkTouch(Blocks.TUBE_CORAL);
		addDropWithSilkTouch(Blocks.BRAIN_CORAL);
		addDropWithSilkTouch(Blocks.BUBBLE_CORAL);
		addDropWithSilkTouch(Blocks.FIRE_CORAL);
		addDropWithSilkTouch(Blocks.HORN_CORAL);
		addDropWithSilkTouch(Blocks.DEAD_TUBE_CORAL_FAN);
		addDropWithSilkTouch(Blocks.DEAD_BRAIN_CORAL_FAN);
		addDropWithSilkTouch(Blocks.DEAD_BUBBLE_CORAL_FAN);
		addDropWithSilkTouch(Blocks.DEAD_FIRE_CORAL_FAN);
		addDropWithSilkTouch(Blocks.DEAD_HORN_CORAL_FAN);
		addDropWithSilkTouch(Blocks.TUBE_CORAL_FAN);
		addDropWithSilkTouch(Blocks.BRAIN_CORAL_FAN);
		addDropWithSilkTouch(Blocks.BUBBLE_CORAL_FAN);
		addDropWithSilkTouch(Blocks.FIRE_CORAL_FAN);
		addDropWithSilkTouch(Blocks.HORN_CORAL_FAN);
		addDropWithSilkTouch(Blocks.INFESTED_STONE, Blocks.STONE);
		addDropWithSilkTouch(Blocks.INFESTED_COBBLESTONE, Blocks.COBBLESTONE);
		addDropWithSilkTouch(Blocks.INFESTED_STONE_BRICKS, Blocks.STONE_BRICKS);
		addDropWithSilkTouch(Blocks.INFESTED_MOSSY_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
		addDropWithSilkTouch(Blocks.INFESTED_CRACKED_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
		addDropWithSilkTouch(Blocks.INFESTED_CHISELED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS);
		addDropWithSilkTouch(Blocks.INFESTED_DEEPSLATE, Blocks.DEEPSLATE);
		addVinePlantDrop(Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT);
		addVinePlantDrop(Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT);
		addDrop(Blocks.CAKE, dropsNothing());
		addDrop(Blocks.CANDLE_CAKE, candleCakeDrops(Blocks.CANDLE));
		addDrop(Blocks.WHITE_CANDLE_CAKE, candleCakeDrops(Blocks.WHITE_CANDLE));
		addDrop(Blocks.ORANGE_CANDLE_CAKE, candleCakeDrops(Blocks.ORANGE_CANDLE));
		addDrop(Blocks.MAGENTA_CANDLE_CAKE, candleCakeDrops(Blocks.MAGENTA_CANDLE));
		addDrop(Blocks.LIGHT_BLUE_CANDLE_CAKE, candleCakeDrops(Blocks.LIGHT_BLUE_CANDLE));
		addDrop(Blocks.YELLOW_CANDLE_CAKE, candleCakeDrops(Blocks.YELLOW_CANDLE));
		addDrop(Blocks.LIME_CANDLE_CAKE, candleCakeDrops(Blocks.LIME_CANDLE));
		addDrop(Blocks.PINK_CANDLE_CAKE, candleCakeDrops(Blocks.PINK_CANDLE));
		addDrop(Blocks.GRAY_CANDLE_CAKE, candleCakeDrops(Blocks.GRAY_CANDLE));
		addDrop(Blocks.LIGHT_GRAY_CANDLE_CAKE, candleCakeDrops(Blocks.LIGHT_GRAY_CANDLE));
		addDrop(Blocks.CYAN_CANDLE_CAKE, candleCakeDrops(Blocks.CYAN_CANDLE));
		addDrop(Blocks.PURPLE_CANDLE_CAKE, candleCakeDrops(Blocks.PURPLE_CANDLE));
		addDrop(Blocks.BLUE_CANDLE_CAKE, candleCakeDrops(Blocks.BLUE_CANDLE));
		addDrop(Blocks.BROWN_CANDLE_CAKE, candleCakeDrops(Blocks.BROWN_CANDLE));
		addDrop(Blocks.GREEN_CANDLE_CAKE, candleCakeDrops(Blocks.GREEN_CANDLE));
		addDrop(Blocks.RED_CANDLE_CAKE, candleCakeDrops(Blocks.RED_CANDLE));
		addDrop(Blocks.BLACK_CANDLE_CAKE, candleCakeDrops(Blocks.BLACK_CANDLE));
		addDrop(Blocks.FROSTED_ICE, dropsNothing());
		addDrop(Blocks.SPAWNER, dropsNothing());
		addDrop(Blocks.TRIAL_SPAWNER, dropsNothing());
		addDrop(Blocks.VAULT, dropsNothing());
		addDrop(Blocks.FIRE, dropsNothing());
		addDrop(Blocks.SOUL_FIRE, dropsNothing());
		addDrop(Blocks.NETHER_PORTAL, dropsNothing());
		addDrop(Blocks.BUDDING_AMETHYST, dropsNothing());
		addDrop(Blocks.POWDER_SNOW, dropsNothing());
		addDrop(Blocks.FROGSPAWN, dropsNothing());
		addDrop(Blocks.REINFORCED_DEEPSLATE, dropsNothing());
		addDrop(Blocks.SUSPICIOUS_SAND, dropsNothing());
		addDrop(Blocks.SUSPICIOUS_GRAVEL, dropsNothing());
	}

	private LootTable.Builder decoratedPotDrops(Block block) {
		return LootTable.builder()
		                .pool(
				                LootPool.builder()
				                        .rolls(ConstantLootNumberProvider.create(1.0F))
				                        .with(
						                        DynamicEntry.builder(DecoratedPotBlock.SHERDS_DYNAMIC_DROP_ID)
						                                    .conditionally(
								                                    BlockStatePropertyLootCondition
										                                    .builder(block)
										                                    .properties(StatePredicate.Builder
												                                    .create()
												                                    .exactMatch(
														                                    DecoratedPotBlock.CRACKED,
														                                    true
												                                    ))
						                                    )
						                                    .alternatively(
								                                    ItemEntry.builder(block)
								                                             .apply(CopyComponentsLootFunction
										                                             .blockEntity(LootContextParameters.BLOCK_ENTITY)
										                                             .include(DataComponentTypes.POT_DECORATIONS))
						                                    )
				                        )
		                );
	}

	private LootTable.Builder pitcherCropDrops() {
		return applyExplosionDecay(
				Blocks.PITCHER_CROP,
				LootTable.builder()
				         .pool(
						         LootPool.builder()
						                 .with(
								                 AlternativeEntry.builder(
										                 PitcherCropBlock.AGE.getValues(),
										                 age -> {
											                 BlockStatePropertyLootCondition.Builder
													                 builder =
													                 BlockStatePropertyLootCondition
															                 .builder(Blocks.PITCHER_CROP)
															                 .properties(StatePredicate.Builder
																	                 .create()
																	                 .exactMatch(
																			                 TallPlantBlock.HALF,
																			                 DoubleBlockHalf.LOWER
																	                 ));
											                 BlockStatePropertyLootCondition.Builder
													                 builder2 =
													                 BlockStatePropertyLootCondition
															                 .builder(Blocks.PITCHER_CROP)
															                 .properties(StatePredicate.Builder
																	                 .create()
																	                 .exactMatch(
																			                 PitcherCropBlock.AGE,
																			                 age.intValue()
																	                 ));
											                 return age == 4
											                        ? ItemEntry.builder(Items.PITCHER_PLANT)
											                                   .conditionally(builder2)
											                                   .conditionally(builder)
											                                   .apply(SetCountLootFunction.builder(
													                                   ConstantLootNumberProvider.create(
															                                   1.0F)))
											                        : ItemEntry.builder(Items.PITCHER_POD)
											                                   .conditionally(builder2)
											                                   .conditionally(builder)
											                                   .apply(SetCountLootFunction.builder(
													                                   ConstantLootNumberProvider.create(
															                                   1.0F)));
										                 }
								                 )
						                 )
				         )
		);
	}

	private LootTable.Builder skullDrops(Block skull) {
		return LootTable.builder()
		                .pool(
				                addSurvivesExplosionCondition(
						                skull,
						                LootPool.builder()
						                        .rolls(ConstantLootNumberProvider.create(1.0F))
						                        .with(
								                        ItemEntry.builder(skull)
								                                 .apply(CopyComponentsLootFunction
										                                 .blockEntity(LootContextParameters.BLOCK_ENTITY)
										                                 .include(DataComponentTypes.CUSTOM_NAME))
						                        )
				                )
		                );
	}
}
