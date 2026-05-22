package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChiseledBookshelfBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;

/**
 * Блок резного книжного шкафа — хранит до 6 книг в отдельных слотах.
 * <p>
 * Каждый слот отображается отдельным булевым свойством состояния блока.
 * Поддерживает компараторный выход: сигнал равен номеру последнего
 * взаимодействовавшего слота + 1 (от 1 до 6).
 */
public class ChiseledBookshelfBlock extends BlockWithEntity implements InteractibleSlotContainer {

	public static final MapCodec<ChiseledBookshelfBlock> CODEC = createCodec(ChiseledBookshelfBlock::new);
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	public static final BooleanProperty SLOT_0_OCCUPIED = Properties.SLOT_0_OCCUPIED;
	public static final BooleanProperty SLOT_1_OCCUPIED = Properties.SLOT_1_OCCUPIED;
	public static final BooleanProperty SLOT_2_OCCUPIED = Properties.SLOT_2_OCCUPIED;
	public static final BooleanProperty SLOT_3_OCCUPIED = Properties.SLOT_3_OCCUPIED;
	public static final BooleanProperty SLOT_4_OCCUPIED = Properties.SLOT_4_OCCUPIED;
	public static final BooleanProperty SLOT_5_OCCUPIED = Properties.SLOT_5_OCCUPIED;
	private static final int MAX_BOOK_COUNT = 6;
	private static final int BOOK_HEIGHT = 3;
	public static final List<BooleanProperty> SLOT_OCCUPIED_PROPERTIES = List.of(
			SLOT_0_OCCUPIED, SLOT_1_OCCUPIED, SLOT_2_OCCUPIED, SLOT_3_OCCUPIED, SLOT_4_OCCUPIED, SLOT_5_OCCUPIED
	);

	@Override
	public MapCodec<ChiseledBookshelfBlock> getCodec() {
		return CODEC;
	}

	@Override
	public int getRows() {
		return 2;
	}

	@Override
	public int getColumns() {
		return 3;
	}

	public ChiseledBookshelfBlock(AbstractBlock.Settings settings) {
		super(settings);
		BlockState defaultState = stateManager.getDefaultState().with(FACING, Direction.NORTH);

		for (BooleanProperty slotProperty : SLOT_OCCUPIED_PROPERTIES) {
			defaultState = defaultState.with(slotProperty, false);
		}

		setDefaultState(defaultState);
	}

	@Override
	protected ActionResult onUseWithItem(
			ItemStack stack,
			BlockState state,
			World world,
			BlockPos pos,
			PlayerEntity player,
			Hand hand,
			BlockHitResult hit
	) {
		if (!(world.getBlockEntity(pos) instanceof ChiseledBookshelfBlockEntity bookshelf)) {
			return ActionResult.PASS;
		}

		if (!stack.isIn(ItemTags.BOOKSHELF_BOOKS)) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		OptionalInt hitSlot = getHitSlot(hit, state.get(FACING));
		if (hitSlot.isEmpty()) {
			return ActionResult.PASS;
		}

		if (state.get(SLOT_OCCUPIED_PROPERTIES.get(hitSlot.getAsInt()))) {
			return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
		}

		tryAddBook(world, pos, player, bookshelf, stack, hitSlot.getAsInt());
		return ActionResult.SUCCESS;
	}

	@Override
	protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
		if (!(world.getBlockEntity(pos) instanceof ChiseledBookshelfBlockEntity bookshelf)) {
			return ActionResult.PASS;
		}

		OptionalInt hitSlot = getHitSlot(hit, state.get(FACING));
		if (hitSlot.isEmpty()) {
			return ActionResult.PASS;
		}

		if (!state.get(SLOT_OCCUPIED_PROPERTIES.get(hitSlot.getAsInt()))) {
			return ActionResult.CONSUME;
		}

		tryRemoveBook(world, pos, player, bookshelf, hitSlot.getAsInt());
		return ActionResult.SUCCESS;
	}

	private static void tryAddBook(
			World world,
			BlockPos pos,
			PlayerEntity player,
			ChiseledBookshelfBlockEntity blockEntity,
			ItemStack stack,
			int slot
	) {
		if (!world.isClient()) {
			player.incrementStat(Stats.USED.getOrCreateStat(stack.getItem()));
			SoundEvent soundEvent = stack.isOf(Items.ENCHANTED_BOOK)
			                        ? SoundEvents.BLOCK_CHISELED_BOOKSHELF_INSERT_ENCHANTED
			                        : SoundEvents.BLOCK_CHISELED_BOOKSHELF_INSERT;
			blockEntity.setStack(slot, stack.splitUnlessCreative(1, player));
			world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}
	}

	private static void tryRemoveBook(
			World world,
			BlockPos pos,
			PlayerEntity player,
			ChiseledBookshelfBlockEntity blockEntity,
			int slot
	) {
		if (!world.isClient()) {
			ItemStack itemStack = blockEntity.removeStack(slot, 1);
			SoundEvent soundEvent = itemStack.isOf(Items.ENCHANTED_BOOK)
			                        ? SoundEvents.BLOCK_CHISELED_BOOKSHELF_PICKUP_ENCHANTED
			                        : SoundEvents.BLOCK_CHISELED_BOOKSHELF_PICKUP;
			world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 1.0F, 1.0F);
			if (!player.getInventory().insertStack(itemStack)) {
				player.dropItem(itemStack, false);
			}

			world.emitGameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
	}

	@Override
	public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new ChiseledBookshelfBlockEntity(pos, state);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		SLOT_OCCUPIED_PROPERTIES.forEach(property -> builder.add(property));
	}

	@Override
	protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
		ItemScatterer.onStateReplaced(state, world, pos);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}

	@Override
	protected boolean hasComparatorOutput(BlockState state) {
		return true;
	}

	@Override
	protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
		if (world.isClient()) {
			return 0;
		}

		return world.getBlockEntity(pos) instanceof ChiseledBookshelfBlockEntity bookshelf
				? bookshelf.getLastInteractedSlot() + 1
				: 0;
	}
}
