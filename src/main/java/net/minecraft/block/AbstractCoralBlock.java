package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.FluidTags;
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
 * {@code AbstractCoralBlock}.
 */
public abstract class AbstractCoralBlock extends Block implements Waterloggable {

	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private static final VoxelShape SHAPE = Block.createColumnShape(12.0, 0.0, 4.0);

	protected AbstractCoralBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager.getDefaultState().with(WATERLOGGED, true));
	}

	@Override
	protected abstract MapCodec<? extends AbstractCoralBlock> getCodec();

	protected void checkLivingConditions(
			BlockState state,
			BlockView world,
			ScheduledTickView tickView,
			Random random,
			BlockPos pos
	) {
		if (!isInWater(state, world, pos)) {
			tickView.scheduleBlockTick(pos, this, 60 + random.nextInt(40));
		}
	}

	protected static boolean isInWater(BlockState state, BlockView world, BlockPos pos) {
		if (state.get(WATERLOGGED)) {
			return true;
		}
		else {
			for (Direction direction : Direction.values()) {
				if (world.getFluidState(pos.offset(direction)).isIn(FluidTags.WATER)) {
					return true;
				}
			}

			return false;
		}
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		return this.getDefaultState().with(WATERLOGGED, fluidState.isIn(FluidTags.WATER) && fluidState.getLevel() == 8);
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
		if (state.get(WATERLOGGED)) {
			tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}

		return direction == Direction.DOWN && !this.canPlaceAt(state, world, pos)
		       ? Blocks.AIR.getDefaultState()
		       : super.getStateForNeighborUpdate(
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
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		BlockPos blockPos = pos.down();
		return world.getBlockState(blockPos).isSideSolidFullSquare(world, blockPos, Direction.UP);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(WATERLOGGED);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}
}
