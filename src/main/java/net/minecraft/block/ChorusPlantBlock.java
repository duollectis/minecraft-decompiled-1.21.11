package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * Стебель хоруса — многосвязный блок, формирующий тело хорусного растения в Крае.
 * Соединяется со всеми соседними стеблями, цветками хоруса и блоками камня Края снизу.
 * Разрушается, если теряет опору (нет соединения снизу или с боковым стеблем на камне Края).
 */
public class ChorusPlantBlock extends ConnectingBlock {

	public static final MapCodec<ChorusPlantBlock> CODEC = createCodec(ChorusPlantBlock::new);

	@Override
	public MapCodec<ChorusPlantBlock> getCodec() {
		return CODEC;
	}

	public ChorusPlantBlock(AbstractBlock.Settings settings) {
		super(10.0F, settings);
		setDefaultState(
				stateManager
						.getDefaultState()
						.with(NORTH, false)
						.with(EAST, false)
						.with(SOUTH, false)
						.with(WEST, false)
						.with(UP, false)
						.with(DOWN, false)
		);
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return withConnectionProperties(ctx.getWorld(), ctx.getBlockPos(), getDefaultState());
	}

	/**
	 * Вычисляет состояние блока с учётом всех соседних соединений.
	 * Соединяется вниз также с блоком камня Края, что является корневым условием роста хоруса.
	 *
	 * @param world мир для чтения соседних состояний
	 * @param pos   позиция данного блока
	 * @param state базовое состояние, к которому применяются соединения
	 * @return состояние с выставленными флагами направлений
	 */
	public static BlockState withConnectionProperties(BlockView world, BlockPos pos, BlockState state) {
		BlockState below = world.getBlockState(pos.down());
		BlockState above = world.getBlockState(pos.up());
		BlockState northState = world.getBlockState(pos.north());
		BlockState eastState = world.getBlockState(pos.east());
		BlockState southState = world.getBlockState(pos.south());
		BlockState westState = world.getBlockState(pos.west());
		Block self = state.getBlock();

		return state
				.withIfExists(
						DOWN,
						below.isOf(self) || below.isOf(Blocks.CHORUS_FLOWER) || below.isOf(Blocks.END_STONE)
				)
				.withIfExists(UP, above.isOf(self) || above.isOf(Blocks.CHORUS_FLOWER))
				.withIfExists(NORTH, northState.isOf(self) || northState.isOf(Blocks.CHORUS_FLOWER))
				.withIfExists(EAST, eastState.isOf(self) || eastState.isOf(Blocks.CHORUS_FLOWER))
				.withIfExists(SOUTH, southState.isOf(self) || southState.isOf(Blocks.CHORUS_FLOWER))
				.withIfExists(WEST, westState.isOf(self) || westState.isOf(Blocks.CHORUS_FLOWER));
	}

	@Override
	protected BlockState getStateForNeighborUpdate(
			BlockState state,
			WorldView world,
			ScheduledTickView tickView,
			BlockPos pos,
			Direction direction,
			BlockPos neighborPos,
			BlockState neighborState,
			Random random
	) {
		if (!state.canPlaceAt(world, pos)) {
			tickView.scheduleBlockTick(pos, this, 1);

			return super.getStateForNeighborUpdate(
					state,
					world,
					tickView,
					pos,
					direction,
					neighborPos,
					neighborState,
					random
			);
		}

		boolean connects = neighborState.isOf(this)
				|| neighborState.isOf(Blocks.CHORUS_FLOWER)
				|| direction == Direction.DOWN && neighborState.isOf(Blocks.END_STONE);

		return state.with(FACING_PROPERTIES.get(direction), connects);
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (!state.canPlaceAt(world, pos)) {
			world.breakBlock(pos, true);
		}
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockState below = world.getBlockState(pos.down());
		boolean hasVerticalNeighbors = !world.getBlockState(pos.up()).isAir() && !below.isAir();

		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos neighborPos = pos.offset(direction);
			BlockState neighborState = world.getBlockState(neighborPos);

			if (neighborState.isOf(this)) {
				if (hasVerticalNeighbors) {
					return false;
				}

				BlockState neighborBelow = world.getBlockState(neighborPos.down());

				if (neighborBelow.isOf(this) || neighborBelow.isOf(Blocks.END_STONE)) {
					return true;
				}
			}
		}

		return below.isOf(this) || below.isOf(Blocks.END_STONE);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}
