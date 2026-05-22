package net.minecraft.block.cauldron;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Функциональный интерфейс поведения котла при взаимодействии с предметом.
 * Каждый тип котла ({@link #EMPTY_CAULDRON_BEHAVIOR}, {@link #WATER_CAULDRON_BEHAVIOR},
 * {@link #LAVA_CAULDRON_BEHAVIOR}, {@link #POWDER_SNOW_CAULDRON_BEHAVIOR}) хранит
 * свою карту {@code Item → CauldronBehavior}, которая определяет, что происходит
 * при использовании конкретного предмета на котле.
 */
public interface CauldronBehavior {

	Map<String, CauldronBehavior.CauldronBehaviorMap> BEHAVIOR_MAPS = new Object2ObjectArrayMap();

	Codec<CauldronBehavior.CauldronBehaviorMap>
			CODEC =
			Codec.stringResolver(CauldronBehavior.CauldronBehaviorMap::name, BEHAVIOR_MAPS::get);

	CauldronBehavior.CauldronBehaviorMap EMPTY_CAULDRON_BEHAVIOR = createMap("empty");

	CauldronBehavior.CauldronBehaviorMap WATER_CAULDRON_BEHAVIOR = createMap("water");

	CauldronBehavior.CauldronBehaviorMap LAVA_CAULDRON_BEHAVIOR = createMap("lava");

	CauldronBehavior.CauldronBehaviorMap POWDER_SNOW_CAULDRON_BEHAVIOR = createMap("powder_snow");

	static CauldronBehavior.CauldronBehaviorMap createMap(String name) {
		Object2ObjectOpenHashMap<Item, CauldronBehavior> behaviorMap = new Object2ObjectOpenHashMap();
		behaviorMap.defaultReturnValue(
				(state, world, pos, player, hand, stack) -> ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION
		);
		CauldronBehavior.CauldronBehaviorMap map = new CauldronBehavior.CauldronBehaviorMap(name, behaviorMap);
		BEHAVIOR_MAPS.put(name, map);
		return map;
	}

	ActionResult interact(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack);

	static void registerBehavior() {
		Map<Item, CauldronBehavior> emptyMap = EMPTY_CAULDRON_BEHAVIOR.map();
		registerBucketBehavior(emptyMap);
		emptyMap.put(
				Items.POTION, (state, world, pos, player, hand, stack) -> {
					PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
					if (potionContents == null || potionContents.matches(Potions.WATER) == false) {
						return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
					}

					if (!world.isClient()) {
						Item item = stack.getItem();
						player.setStackInHand(
								hand,
								ItemUsage.exchangeStack(stack, player, new ItemStack(Items.GLASS_BOTTLE))
						);
						player.incrementStat(Stats.USE_CAULDRON);
						player.incrementStat(Stats.USED.getOrCreateStat(item));
						world.setBlockState(pos, Blocks.WATER_CAULDRON.getDefaultState());
						world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
						world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
					}

					return ActionResult.SUCCESS;
				}
		);
		Map<Item, CauldronBehavior> waterMap = WATER_CAULDRON_BEHAVIOR.map();
		registerBucketBehavior(waterMap);
		waterMap.put(
				Items.BUCKET,
				(state, world, pos, player, hand, stack) -> emptyCauldron(
						state,
						world,
						pos,
						player,
						hand,
						stack,
						new ItemStack(Items.WATER_BUCKET),
						cauldronState -> cauldronState.get(LeveledCauldronBlock.LEVEL) == 3,
						SoundEvents.ITEM_BUCKET_FILL
				)
		);
		waterMap.put(
				Items.GLASS_BOTTLE, (state, world, pos, player, hand, stack) -> {
					if (!world.isClient()) {
						Item item = stack.getItem();
						player.setStackInHand(
								hand,
								ItemUsage.exchangeStack(
										stack,
										player,
										PotionContentsComponent.createStack(Items.POTION, Potions.WATER)
								)
						);
						player.incrementStat(Stats.USE_CAULDRON);
						player.incrementStat(Stats.USED.getOrCreateStat(item));
						LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
						world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
						world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
					}

					return ActionResult.SUCCESS;
				}
		);
		waterMap.put(
				Items.POTION, (state, world, pos, player, hand, stack) -> {
					if (state.get(LeveledCauldronBlock.LEVEL) == 3) {
						return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
					}

					PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
					if (potionContents == null || potionContents.matches(Potions.WATER) == false) {
						return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
					}

					if (!world.isClient()) {
						player.setStackInHand(
								hand,
								ItemUsage.exchangeStack(stack, player, new ItemStack(Items.GLASS_BOTTLE))
						);
						player.incrementStat(Stats.USE_CAULDRON);
						player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
						world.setBlockState(pos, state.cycle(LeveledCauldronBlock.LEVEL));
						world.playSound(
								null,
								pos,
								SoundEvents.ITEM_BOTTLE_EMPTY,
								SoundCategory.BLOCKS,
								1.0F,
								1.0F
						);
						world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
					}

					return ActionResult.SUCCESS;
				}
		);
		waterMap.put(Items.LEATHER_BOOTS, CauldronBehavior::cleanArmor);
		waterMap.put(Items.LEATHER_LEGGINGS, CauldronBehavior::cleanArmor);
		waterMap.put(Items.LEATHER_CHESTPLATE, CauldronBehavior::cleanArmor);
		waterMap.put(Items.LEATHER_HELMET, CauldronBehavior::cleanArmor);
		waterMap.put(Items.LEATHER_HORSE_ARMOR, CauldronBehavior::cleanArmor);
		waterMap.put(Items.WOLF_ARMOR, CauldronBehavior::cleanArmor);
		waterMap.put(Items.WHITE_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.GRAY_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.BLACK_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.BLUE_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.BROWN_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.CYAN_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.GREEN_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.LIGHT_BLUE_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.LIGHT_GRAY_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.LIME_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.MAGENTA_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.ORANGE_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.PINK_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.PURPLE_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.RED_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.YELLOW_BANNER, CauldronBehavior::cleanBanner);
		waterMap.put(Items.WHITE_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.GRAY_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.BLACK_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.BLUE_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.BROWN_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.CYAN_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.GREEN_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.LIGHT_BLUE_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.LIGHT_GRAY_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.LIME_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.MAGENTA_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.ORANGE_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.PINK_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.PURPLE_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.RED_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		waterMap.put(Items.YELLOW_SHULKER_BOX, CauldronBehavior::cleanShulkerBox);
		Map<Item, CauldronBehavior> lavaMap = LAVA_CAULDRON_BEHAVIOR.map();
		lavaMap.put(
				Items.BUCKET,
				(state, world, pos, player, hand, stack) -> emptyCauldron(
						state,
						world,
						pos,
						player,
						hand,
						stack,
						new ItemStack(Items.LAVA_BUCKET),
						cauldronState -> true,
						SoundEvents.ITEM_BUCKET_FILL_LAVA
				)
		);
		registerBucketBehavior(lavaMap);
		Map<Item, CauldronBehavior> snowMap = POWDER_SNOW_CAULDRON_BEHAVIOR.map();
		snowMap.put(
				Items.BUCKET,
				(state, world, pos, player, hand, stack) -> emptyCauldron(
						state,
						world,
						pos,
						player,
						hand,
						stack,
						new ItemStack(Items.POWDER_SNOW_BUCKET),
						cauldronState -> cauldronState.get(LeveledCauldronBlock.LEVEL) == 3,
						SoundEvents.ITEM_BUCKET_FILL_POWDER_SNOW
				)
		);
		registerBucketBehavior(snowMap);
	}

	static void registerBucketBehavior(Map<Item, CauldronBehavior> behavior) {
		behavior.put(Items.LAVA_BUCKET, CauldronBehavior::tryFillWithLava);
		behavior.put(Items.WATER_BUCKET, CauldronBehavior::tryFillWithWater);
		behavior.put(Items.POWDER_SNOW_BUCKET, CauldronBehavior::tryFillWithPowderSnow);
	}

	static ActionResult emptyCauldron(
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			ItemStack stack,
			ItemStack output,
			Predicate<BlockState> fullPredicate,
			SoundEvent soundEvent
	) {
		if (fullPredicate.test(state) == false) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		if (!world.isClient()) {
			Item item = stack.getItem();
			player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, output));
			player.incrementStat(Stats.USE_CAULDRON);
			player.incrementStat(Stats.USED.getOrCreateStat(item));
			world.setBlockState(pos, Blocks.CAULDRON.getDefaultState());
			world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.emitGameEvent(null, GameEvent.FLUID_PICKUP, pos);
		}

		return ActionResult.SUCCESS;
	}

	static ActionResult fillCauldron(
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			ItemStack stack,
			BlockState state,
			SoundEvent soundEvent
	) {
		if (!world.isClient()) {
			Item item = stack.getItem();
			player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, new ItemStack(Items.BUCKET)));
			player.incrementStat(Stats.FILL_CAULDRON);
			player.incrementStat(Stats.USED.getOrCreateStat(item));
			world.setBlockState(pos, state);
			world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
			world.emitGameEvent(null, GameEvent.FLUID_PLACE, pos);
		}

		return ActionResult.SUCCESS;
	}

	private static ActionResult tryFillWithWater(
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			ItemStack stack
	) {
		return fillCauldron(
				world,
				pos,
				player,
				hand,
				stack,
				Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3),
				SoundEvents.ITEM_BUCKET_EMPTY
		);
	}

	private static ActionResult tryFillWithLava(
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			ItemStack stack
	) {
		return (ActionResult) (isUnderwater(world, pos)
		                       ? ActionResult.CONSUME
		                       : fillCauldron(
				                       world,
				                       pos,
				                       player,
				                       hand,
				                       stack,
				                       Blocks.LAVA_CAULDRON.getDefaultState(),
				                       SoundEvents.ITEM_BUCKET_EMPTY_LAVA
		                       )
		);
	}

	private static ActionResult tryFillWithPowderSnow(
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			ItemStack stack
	) {
		return (ActionResult) (isUnderwater(world, pos)
		                       ? ActionResult.CONSUME
		                       : fillCauldron(
				                       world,
				                       pos,
				                       player,
				                       hand,
				                       stack,
				                       Blocks.POWDER_SNOW_CAULDRON
				                       .getDefaultState()
				                       .with(LeveledCauldronBlock.LEVEL, 3),
				                       SoundEvents.ITEM_BUCKET_EMPTY_POWDER_SNOW
		                       )
		);
	}

	private static ActionResult cleanShulkerBox(
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			ItemStack stack
	) {
		Block block = Block.getBlockFromItem(stack.getItem());
		if (block instanceof ShulkerBoxBlock == false) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		if (!world.isClient()) {
			ItemStack cleaned = stack.copyComponentsToNewStack(Blocks.SHULKER_BOX, 1);
			player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, cleaned, false));
			player.incrementStat(Stats.CLEAN_SHULKER_BOX);
			LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
		}

		return ActionResult.SUCCESS;
	}

	private static ActionResult cleanBanner(
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			ItemStack stack
	) {
		BannerPatternsComponent patterns = stack.getOrDefault(
				DataComponentTypes.BANNER_PATTERNS,
				BannerPatternsComponent.DEFAULT
		);
		if (patterns.layers().isEmpty()) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		if (!world.isClient()) {
			ItemStack cleaned = stack.copyWithCount(1);
			cleaned.set(DataComponentTypes.BANNER_PATTERNS, patterns.withoutTopLayer());
			player.setStackInHand(hand, ItemUsage.exchangeStack(stack, player, cleaned, false));
			player.incrementStat(Stats.CLEAN_BANNER);
			LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
		}

		return ActionResult.SUCCESS;
	}

	private static ActionResult cleanArmor(
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			ItemStack stack
	) {
		if (stack.isIn(ItemTags.DYEABLE) == false) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		if (stack.contains(DataComponentTypes.DYED_COLOR) == false) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		if (!world.isClient()) {
			stack.remove(DataComponentTypes.DYED_COLOR);
			player.incrementStat(Stats.CLEAN_ARMOR);
			LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
		}

		return ActionResult.SUCCESS;
	}

	private static boolean isUnderwater(World world, BlockPos pos) {
		FluidState fluidState = world.getFluidState(pos.up());
		return fluidState.isIn(FluidTags.WATER);
	}

	/**
	 * Именованная карта поведений котла: связывает предмет с его {@link CauldronBehavior}.
	 * Имя используется для сериализации через {@link #CODEC}.
	 */
	public record CauldronBehaviorMap(String name, Map<Item, CauldronBehavior> map) {
	}
}
