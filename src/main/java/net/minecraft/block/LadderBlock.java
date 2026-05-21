package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * {@code LadderBlock}.
 */
public class LadderBlock extends Block implements Waterloggable {

	public static final MapCodec<LadderBlock> CODEC = createCodec(LadderBlock::new);
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	public static final Map<Direction, VoxelShape>
			SHAPES_BY_DIRECTION =
			VoxelShapes.createHorizontalFacingShapeMap(Block.createCuboidZShape(16.0, 13.0, 16.0));

	@Override
	public MapCodec<LadderBlock> getCodec() {
		return CODEC;
	}

	public LadderBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager
				.getDefaultState()
				.with(FACING, Direction.NORTH)
				.with(WATERLOGGED, false));
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPES_BY_DIRECTION.get(state.get(FACING));
	}

	private boolean canPlaceOn(BlockView world, BlockPos pos, Direction side) {
		BlockState blockState = world.getBlockState(pos);
		return blockState.isSideSolidFullSquare(world, pos, side);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		Direction direction = state.get(FACING);
		return this.canPlaceOn(world, pos.offset(direction.getOpposite()), direction);
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
		if (direction.getOpposite() == state.get(FACING) && !state.canPlaceAt(world, pos)) {
			return Blocks.AIR.getDefaultState();
		}
		else {
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

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		if (!ctx.canReplaceExisting()) {
			BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(ctx.getSide().getOpposite()));
			if (blockState.isOf(this) && blockState.get(FACING) == ctx.getSide()) {
				return null;
			}
		}

		BlockState blockState = this.getDefaultState();
		WorldView worldView = ctx.getWorld();
		BlockPos blockPos = ctx.getBlockPos();
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());

		for (Direction direction : ctx.getPlacementDirections()) {
			if (direction.getAxis().isHorizontal()) {
				blockState = blockState.with(FACING, direction.getOpposite());
				if (blockState.canPlaceAt(worldView, blockPos)) {
					return blockState.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
				}
			}
		}

		return null;
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}
}
