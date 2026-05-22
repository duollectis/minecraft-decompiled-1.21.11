package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import java.util.Map;

/**
 * Блок цепи. Ориентируется по оси размещения, поддерживает заполнение водой.
 * Форма зависит от оси: узкий столбик 3×3 пикселя в центре.
 */
public class ChainBlock extends PillarBlock implements Waterloggable {

	public static final MapCodec<ChainBlock> CODEC = createCodec(ChainBlock::new);
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private static final Map<Direction.Axis, VoxelShape>
			SHAPES_BY_AXIS =
			VoxelShapes.createAxisShapeMap(Block.createCuboidShape(3.0, 3.0, 16.0));

	@Override
	public MapCodec<? extends ChainBlock> getCodec() {
		return CODEC;
	}

	public ChainBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(WATERLOGGED, false).with(AXIS, Direction.Axis.Y));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_AXIS.get(state.get(AXIS));
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		boolean bl = fluidState.getFluid() == Fluids.WATER;
		return super.getPlacementState(ctx).with(WATERLOGGED, bl);
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
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED).add(AXIS);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}
}
