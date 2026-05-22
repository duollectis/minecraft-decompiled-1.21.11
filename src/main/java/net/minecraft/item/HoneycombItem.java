package net.minecraft.item;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Предмет «Соты». Покрывает медные блоки воском, предотвращая их окисление.
 * Также запечатывает таблички воском через интерфейс {@link SignChangingItem}.
 */
public class HoneycombItem extends Item implements SignChangingItem {

	/** Флаги обновления блока при нанесении воска. */
	private static final int WAX_UPDATE_FLAGS = 11;

	/** ID мирового события для визуального эффекта нанесения воска. */
	private static final int WAX_APPLY_EVENT_ID = 3003;

	/**
	 * Карта «невощёный медный блок → вощёный медный блок».
	 * Лениво инициализируется при первом обращении.
	 */
	public static final Supplier<BiMap<Block, Block>> UNWAXED_TO_WAXED_BLOCKS = Suppliers.memoize(
			() -> ImmutableBiMap.<Block, Block>builder()
					.put(Blocks.COPPER_BLOCK, Blocks.WAXED_COPPER_BLOCK)
					.put(Blocks.EXPOSED_COPPER, Blocks.WAXED_EXPOSED_COPPER)
					.put(Blocks.WEATHERED_COPPER, Blocks.WAXED_WEATHERED_COPPER)
					.put(Blocks.OXIDIZED_COPPER, Blocks.WAXED_OXIDIZED_COPPER)
					.put(Blocks.CUT_COPPER, Blocks.WAXED_CUT_COPPER)
					.put(Blocks.EXPOSED_CUT_COPPER, Blocks.WAXED_EXPOSED_CUT_COPPER)
					.put(Blocks.WEATHERED_CUT_COPPER, Blocks.WAXED_WEATHERED_CUT_COPPER)
					.put(Blocks.OXIDIZED_CUT_COPPER, Blocks.WAXED_OXIDIZED_CUT_COPPER)
					.put(Blocks.CUT_COPPER_SLAB, Blocks.WAXED_CUT_COPPER_SLAB)
					.put(Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB)
					.put(Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB)
					.put(Blocks.OXIDIZED_CUT_COPPER_SLAB, Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB)
					.put(Blocks.CUT_COPPER_STAIRS, Blocks.WAXED_CUT_COPPER_STAIRS)
					.put(Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS)
					.put(Blocks.WEATHERED_CUT_COPPER_STAIRS, Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS)
					.put(Blocks.OXIDIZED_CUT_COPPER_STAIRS, Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS)
					.put(Blocks.CHISELED_COPPER, Blocks.WAXED_CHISELED_COPPER)
					.put(Blocks.EXPOSED_CHISELED_COPPER, Blocks.WAXED_EXPOSED_CHISELED_COPPER)
					.put(Blocks.WEATHERED_CHISELED_COPPER, Blocks.WAXED_WEATHERED_CHISELED_COPPER)
					.put(Blocks.OXIDIZED_CHISELED_COPPER, Blocks.WAXED_OXIDIZED_CHISELED_COPPER)
					.put(Blocks.COPPER_DOOR, Blocks.WAXED_COPPER_DOOR)
					.put(Blocks.EXPOSED_COPPER_DOOR, Blocks.WAXED_EXPOSED_COPPER_DOOR)
					.put(Blocks.WEATHERED_COPPER_DOOR, Blocks.WAXED_WEATHERED_COPPER_DOOR)
					.put(Blocks.OXIDIZED_COPPER_DOOR, Blocks.WAXED_OXIDIZED_COPPER_DOOR)
					.put(Blocks.COPPER_TRAPDOOR, Blocks.WAXED_COPPER_TRAPDOOR)
					.put(Blocks.EXPOSED_COPPER_TRAPDOOR, Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR)
					.put(Blocks.WEATHERED_COPPER_TRAPDOOR, Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR)
					.put(Blocks.OXIDIZED_COPPER_TRAPDOOR, Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR)
					.putAll(Blocks.COPPER_BARS.getWaxingMap())
					.put(Blocks.COPPER_GRATE, Blocks.WAXED_COPPER_GRATE)
					.put(Blocks.EXPOSED_COPPER_GRATE, Blocks.WAXED_EXPOSED_COPPER_GRATE)
					.put(Blocks.WEATHERED_COPPER_GRATE, Blocks.WAXED_WEATHERED_COPPER_GRATE)
					.put(Blocks.OXIDIZED_COPPER_GRATE, Blocks.WAXED_OXIDIZED_COPPER_GRATE)
					.put(Blocks.COPPER_BULB, Blocks.WAXED_COPPER_BULB)
					.put(Blocks.EXPOSED_COPPER_BULB, Blocks.WAXED_EXPOSED_COPPER_BULB)
					.put(Blocks.WEATHERED_COPPER_BULB, Blocks.WAXED_WEATHERED_COPPER_BULB)
					.put(Blocks.OXIDIZED_COPPER_BULB, Blocks.WAXED_OXIDIZED_COPPER_BULB)
					.put(Blocks.COPPER_CHEST, Blocks.WAXED_COPPER_CHEST)
					.put(Blocks.EXPOSED_COPPER_CHEST, Blocks.WAXED_EXPOSED_COPPER_CHEST)
					.put(Blocks.WEATHERED_COPPER_CHEST, Blocks.WAXED_WEATHERED_COPPER_CHEST)
					.put(Blocks.OXIDIZED_COPPER_CHEST, Blocks.WAXED_OXIDIZED_COPPER_CHEST)
					.put(Blocks.COPPER_GOLEM_STATUE, Blocks.WAXED_COPPER_GOLEM_STATUE)
					.put(Blocks.EXPOSED_COPPER_GOLEM_STATUE, Blocks.WAXED_EXPOSED_COPPER_GOLEM_STATUE)
					.put(Blocks.WEATHERED_COPPER_GOLEM_STATUE, Blocks.WAXED_WEATHERED_COPPER_GOLEM_STATUE)
					.put(Blocks.OXIDIZED_COPPER_GOLEM_STATUE, Blocks.WAXED_OXIDIZED_COPPER_GOLEM_STATUE)
					.put(Blocks.LIGHTNING_ROD, Blocks.WAXED_LIGHTNING_ROD)
					.put(Blocks.EXPOSED_LIGHTNING_ROD, Blocks.WAXED_EXPOSED_LIGHTNING_ROD)
					.put(Blocks.WEATHERED_LIGHTNING_ROD, Blocks.WAXED_WEATHERED_LIGHTNING_ROD)
					.put(Blocks.OXIDIZED_LIGHTNING_ROD, Blocks.WAXED_OXIDIZED_LIGHTNING_ROD)
					.putAll(Blocks.COPPER_LANTERNS.getWaxingMap())
					.putAll(Blocks.COPPER_CHAINS.getWaxingMap())
					.build()
	);

	/** Обратная карта «вощёный → невощёный», вычисляется из {@link #UNWAXED_TO_WAXED_BLOCKS}. */
	public static final Supplier<BiMap<Block, Block>> WAXED_TO_UNWAXED_BLOCKS =
			Suppliers.memoize(() -> UNWAXED_TO_WAXED_BLOCKS.get().inverse());

	private static final String WAXED_COPPER_DOOR_GROUP = "waxed_copper_door";
	private static final String WAXED_COPPER_TRAPDOOR_GROUP = "waxed_copper_trapdoor";
	private static final String WAXED_COPPER_GOLEM_STATUE_GROUP = "waxed_copper_golem_statue";
	private static final String WAXED_COPPER_CHEST_GROUP = "waxed_copper_chest";
	private static final String WAXED_LIGHTNING_ROD_GROUP = "waxed_lightning_rod";
	private static final String WAXED_COPPER_BAR_GROUP = "waxed_copper_bar";
	private static final String WAXED_COPPER_CHAIN_GROUP = "waxed_copper_chain";
	private static final String WAXED_COPPER_LANTERN_GROUP = "waxed_copper_lantern";
	private static final String WAXED_COPPER_BLOCK_GROUP = "waxed_copper_block";

	@SuppressWarnings("unchecked")
	public static final ImmutableMap<Block, Pair<RecipeCategory, String>> WAXED_RECIPE_GROUPS =
			(ImmutableMap<Block, Pair<RecipeCategory, String>>) (ImmutableMap<?, ?>) ImmutableMap
					.<Block, Pair<RecipeCategory, String>>builder()
					.put(Blocks.WAXED_COPPER_BULB, Pair.of(RecipeCategory.REDSTONE, "waxed_copper_bulb"))
					.put(Blocks.WAXED_WEATHERED_COPPER_BULB, Pair.of(RecipeCategory.REDSTONE, "waxed_weathered_copper_bulb"))
					.put(Blocks.WAXED_EXPOSED_COPPER_BULB, Pair.of(RecipeCategory.REDSTONE, "waxed_exposed_copper_bulb"))
					.put(Blocks.WAXED_OXIDIZED_COPPER_BULB, Pair.of(RecipeCategory.REDSTONE, "waxed_oxidized_copper_bulb"))
					.put(Blocks.WAXED_COPPER_DOOR, Pair.of(RecipeCategory.REDSTONE, WAXED_COPPER_DOOR_GROUP))
					.put(Blocks.WAXED_WEATHERED_COPPER_DOOR, Pair.of(RecipeCategory.REDSTONE, WAXED_COPPER_DOOR_GROUP))
					.put(Blocks.WAXED_EXPOSED_COPPER_DOOR, Pair.of(RecipeCategory.REDSTONE, WAXED_COPPER_DOOR_GROUP))
					.put(Blocks.WAXED_OXIDIZED_COPPER_DOOR, Pair.of(RecipeCategory.REDSTONE, WAXED_COPPER_DOOR_GROUP))
					.put(Blocks.WAXED_COPPER_TRAPDOOR, Pair.of(RecipeCategory.REDSTONE, WAXED_COPPER_TRAPDOOR_GROUP))
					.put(Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR, Pair.of(RecipeCategory.REDSTONE, WAXED_COPPER_TRAPDOOR_GROUP))
					.put(Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR, Pair.of(RecipeCategory.REDSTONE, WAXED_COPPER_TRAPDOOR_GROUP))
					.put(Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR, Pair.of(RecipeCategory.REDSTONE, WAXED_COPPER_TRAPDOOR_GROUP))
					.put(Blocks.WAXED_COPPER_GOLEM_STATUE, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_GOLEM_STATUE_GROUP))
					.put(Blocks.WAXED_WEATHERED_COPPER_GOLEM_STATUE, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_GOLEM_STATUE_GROUP))
					.put(Blocks.WAXED_EXPOSED_COPPER_GOLEM_STATUE, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_GOLEM_STATUE_GROUP))
					.put(Blocks.WAXED_OXIDIZED_COPPER_GOLEM_STATUE, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_GOLEM_STATUE_GROUP))
					.put(Blocks.WAXED_COPPER_CHEST, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_CHEST_GROUP))
					.put(Blocks.WAXED_WEATHERED_COPPER_CHEST, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_CHEST_GROUP))
					.put(Blocks.WAXED_EXPOSED_COPPER_CHEST, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_CHEST_GROUP))
					.put(Blocks.WAXED_OXIDIZED_COPPER_CHEST, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_CHEST_GROUP))
					.put(Blocks.WAXED_LIGHTNING_ROD, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_LIGHTNING_ROD_GROUP))
					.put(Blocks.WAXED_WEATHERED_LIGHTNING_ROD, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_LIGHTNING_ROD_GROUP))
					.put(Blocks.WAXED_EXPOSED_LIGHTNING_ROD, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_LIGHTNING_ROD_GROUP))
					.put(Blocks.WAXED_OXIDIZED_LIGHTNING_ROD, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_LIGHTNING_ROD_GROUP))
					.put(Blocks.COPPER_BARS.waxed(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_BAR_GROUP))
					.put(Blocks.COPPER_BARS.waxedWeathered(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_BAR_GROUP))
					.put(Blocks.COPPER_BARS.waxedExposed(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_BAR_GROUP))
					.put(Blocks.COPPER_BARS.waxedOxidized(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_BAR_GROUP))
					.put(Blocks.COPPER_CHAINS.waxed(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_CHAIN_GROUP))
					.put(Blocks.COPPER_CHAINS.waxedWeathered(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_CHAIN_GROUP))
					.put(Blocks.COPPER_CHAINS.waxedExposed(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_CHAIN_GROUP))
					.put(Blocks.COPPER_CHAINS.waxedOxidized(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_CHAIN_GROUP))
					.put(Blocks.COPPER_LANTERNS.waxed(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_LANTERN_GROUP))
					.put(Blocks.COPPER_LANTERNS.waxedWeathered(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_LANTERN_GROUP))
					.put(Blocks.COPPER_LANTERNS.waxedExposed(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_LANTERN_GROUP))
					.put(Blocks.COPPER_LANTERNS.waxedOxidized(), Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_LANTERN_GROUP))
					.put(Blocks.WAXED_COPPER_BLOCK, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_BLOCK_GROUP))
					.put(Blocks.WAXED_WEATHERED_COPPER, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_BLOCK_GROUP))
					.put(Blocks.WAXED_EXPOSED_COPPER, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_BLOCK_GROUP))
					.put(Blocks.WAXED_OXIDIZED_COPPER, Pair.of(RecipeCategory.BUILDING_BLOCKS, WAXED_COPPER_BLOCK_GROUP))
					.build();

	public HoneycombItem(Item.Settings settings) {
		super(settings);
	}

	/**
	 * Наносит воск на медный блок. Если блок — двойной сундук, обрабатывает оба блока.
	 * Триггерит критерий достижения {@code ITEM_USED_ON_BLOCK} для серверных игроков.
	 */
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState blockState = world.getBlockState(pos);

		return getWaxedState(blockState).map(waxedState -> {
			PlayerEntity player = context.getPlayer();
			ItemStack stack = context.getStack();

			if (player instanceof ServerPlayerEntity serverPlayer) {
				Criteria.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, stack);
			}

			stack.decrement(1);
			world.setBlockState(pos, waxedState, WAX_UPDATE_FLAGS);
			world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(player, waxedState));
			world.syncWorldEvent(player, WAX_APPLY_EVENT_ID, pos, 0);

			if (blockState.getBlock() instanceof ChestBlock
					&& blockState.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE
			) {
				BlockPos connectedPos = ChestBlock.getPosInFrontOf(pos, blockState);
				world.emitGameEvent(
						GameEvent.BLOCK_CHANGE,
						connectedPos,
						GameEvent.Emitter.of(player, world.getBlockState(connectedPos))
				);
				world.syncWorldEvent(player, WAX_APPLY_EVENT_ID, connectedPos, 0);
			}

			return (ActionResult) ActionResult.SUCCESS;
		}).orElse((ActionResult) ActionResult.PASS);
	}

	/**
	 * Возвращает вощёное состояние блока, если оно существует в таблице преобразований.
	 *
	 * @param state исходное состояние блока
	 * @return {@link Optional} с вощёным состоянием или пустой
	 */
	public static Optional<BlockState> getWaxedState(BlockState state) {
		return Optional.ofNullable((Block) UNWAXED_TO_WAXED_BLOCKS.get().get(state.getBlock()))
				.map(block -> block.getStateWithProperties(state));
	}

	@Override
	public boolean useOnSign(World world, SignBlockEntity signBlockEntity, boolean front, PlayerEntity player) {
		if (signBlockEntity.setWaxed(true)) {
			world.syncWorldEvent(null, WAX_APPLY_EVENT_ID, signBlockEntity.getPos(), 0);
			return true;
		}

		return false;
	}

	@Override
	public boolean canUseOnSignText(SignText signText, PlayerEntity player) {
		return true;
	}
}
