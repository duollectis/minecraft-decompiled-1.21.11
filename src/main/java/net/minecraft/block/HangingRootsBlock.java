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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * Блок свисающих корней. Крепится к твёрдой поверхности снизу; поддерживает заполнение водой.
 */
public class HangingRootsBlock extends Block implements Waterloggable {

	public static final MapCodec<HangingRootsBlock> CODEC = createCodec(HangingRootsBlock::new);
	private static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private static final VoxelShape SHAPE = Block.createColumnShape(12.0, 10.0, 16.0);

	@Override
	public MapCodec<HangingRootsBlock> getCodec() {
		return CODEC;
	}

	public HangingRootsBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager.getDefaultState().with(WATERLOGGED, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState base = super.getPlacementState(ctx);
		if (base == null) {
			return null;
		}

		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		return base.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos above = pos.up();
		return world.getBlockState(above).isSideSolidFullSquare(world, above, Direction.DOWN);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
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
		if (direction == Direction.UP && canPlaceAt(state, world, pos) == false) {
			return Blocks.AIR.getDefaultState();
		}

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
}
