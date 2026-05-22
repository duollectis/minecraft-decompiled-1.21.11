package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * Блок корней мангрового дерева — полупрозрачный столб с поддержкой водозаполнения.
 * Скрывает боковые грани при соединении с соседними корнями по вертикальной оси.
 */
public class MangroveRootsBlock extends Block implements Waterloggable {

	public static final MapCodec<MangroveRootsBlock> CODEC = createCodec(MangroveRootsBlock::new);
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

	@Override
	public MapCodec<MangroveRootsBlock> getCodec() {
		return CODEC;
	}

	public MangroveRootsBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(WATERLOGGED, false));
	}

	@Override
	protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
		return stateFrom.isOf(Blocks.MANGROVE_ROOTS) && direction.getAxis() == Direction.Axis.Y;
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		boolean waterlogged = fluidState.getFluid() == Fluids.WATER;
		return super.getPlacementState(ctx).with(WATERLOGGED, waterlogged);
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
		if (state.get(WATERLOGGED)) {
			tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}

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

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED);
	}
}
