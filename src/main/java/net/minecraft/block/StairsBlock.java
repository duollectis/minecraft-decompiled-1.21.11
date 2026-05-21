package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.StairShape;
import net.minecraft.entity.ai.pathing.NavigationType;
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
import net.minecraft.util.math.DirectionTransformation;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import java.util.Map;

/**
 * {@code StairsBlock}.
 */
public class StairsBlock extends Block implements Waterloggable {

	public static final MapCodec<StairsBlock> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance
					.group(
							BlockState.CODEC.fieldOf("base_state").forGetter(block -> block.baseBlockState),
							createSettingsCodec()
					)
					.apply(instance, StairsBlock::new)
	);
	public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
	public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;
	public static final EnumProperty<StairShape> SHAPE = Properties.STAIR_SHAPE;
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private static final VoxelShape OUTER_SHAPE = VoxelShapes.union(
			Block.createColumnShape(16.0, 0.0, 8.0), Block.createCuboidShape(0.0, 8.0, 0.0, 8.0, 16.0, 8.0)
	);
	private static final VoxelShape
			STRAIGHT_SHAPE =
			VoxelShapes.union(OUTER_SHAPE, VoxelShapes.transform(OUTER_SHAPE, DirectionTransformation.ROT_90_Z_POS));
	private static final VoxelShape
			INNER_SHAPE =
			VoxelShapes.union(
					STRAIGHT_SHAPE,
					VoxelShapes.transform(STRAIGHT_SHAPE, DirectionTransformation.ROT_90_Z_POS)
			);
	private static final Map<Direction, VoxelShape>
			OUTER_BOTTOM_SHAPES =
			VoxelShapes.createHorizontalFacingShapeMap(OUTER_SHAPE);
	private static final Map<Direction, VoxelShape>
			STRAIGHT_BOTTOM_SHAPES =
			VoxelShapes.createHorizontalFacingShapeMap(STRAIGHT_SHAPE);
	private static final Map<Direction, VoxelShape>
			INNER_BOTTOM_SHAPES =
			VoxelShapes.createHorizontalFacingShapeMap(INNER_SHAPE);
	private static final Map<Direction, VoxelShape>
			OUTER_TOP_SHAPES =
			VoxelShapes.createHorizontalFacingShapeMap(OUTER_SHAPE, DirectionTransformation.INVERT_Y);
	private static final Map<Direction, VoxelShape> STRAIGHT_TOP_SHAPES = VoxelShapes.createHorizontalFacingShapeMap(
			STRAIGHT_SHAPE, DirectionTransformation.INVERT_Y
	);
	private static final Map<Direction, VoxelShape>
			INNER_TOP_SHAPES =
			VoxelShapes.createHorizontalFacingShapeMap(INNER_SHAPE, DirectionTransformation.INVERT_Y);
	private final Block baseBlock;
	protected final BlockState baseBlockState;

	@Override
	public MapCodec<? extends StairsBlock> getCodec() {
		return CODEC;
	}

	public StairsBlock(BlockState baseBlockState, AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(
				this.stateManager
						.getDefaultState()
						.with(FACING, Direction.NORTH)
						.with(HALF, BlockHalf.BOTTOM)
						.with(SHAPE, StairShape.STRAIGHT)
						.with(WATERLOGGED, false)
		);
		this.baseBlock = baseBlockState.getBlock();
		this.baseBlockState = baseBlockState;
	}

	@Override
	protected boolean hasSidedTransparency(BlockState state) {
		return true;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		boolean bl = state.get(HALF) == BlockHalf.BOTTOM;
		Direction direction = state.get(FACING);

		Map var10000 = switch ((StairShape) state.get(SHAPE)) {
			case STRAIGHT -> bl ? STRAIGHT_BOTTOM_SHAPES : STRAIGHT_TOP_SHAPES;
			case OUTER_LEFT, OUTER_RIGHT -> bl ? OUTER_BOTTOM_SHAPES : OUTER_TOP_SHAPES;
			case INNER_RIGHT, INNER_LEFT -> bl ? INNER_BOTTOM_SHAPES : INNER_TOP_SHAPES;
		};

		return (VoxelShape) var10000.get(switch ((StairShape) state.get(SHAPE)) {
			case STRAIGHT, OUTER_LEFT, INNER_RIGHT -> direction;
			case INNER_LEFT -> direction.rotateYCounterclockwise();
			case OUTER_RIGHT -> direction.rotateYClockwise();
		});
	}

	@Override
	public float getBlastResistance() {
		return this.baseBlock.getBlastResistance();
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		Direction direction = ctx.getSide();
		BlockPos blockPos = ctx.getBlockPos();
		FluidState fluidState = ctx.getWorld().getFluidState(blockPos);
		BlockState blockState = this.getDefaultState()
		                            .with(FACING, ctx.getHorizontalPlayerFacing())
		                            .with(
				                            HALF,
				                            direction != Direction.DOWN && (direction == Direction.UP || !(
						                            ctx.getHitPos().y - blockPos.getY() > 0.5
				                            )
				                            ) ? BlockHalf.BOTTOM : BlockHalf.TOP
		                            )
		                            .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
		return blockState.with(SHAPE, getStairShape(blockState, ctx.getWorld(), blockPos));
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

		return direction.getAxis().isHorizontal()
		       ? state.with(SHAPE, getStairShape(state, world, pos))
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

	private static StairShape getStairShape(BlockState state, BlockView world, BlockPos pos) {
		Direction direction = state.get(FACING);
		BlockState blockState = world.getBlockState(pos.offset(direction));
		if (isStairs(blockState) && state.get(HALF) == blockState.get(HALF)) {
			Direction direction2 = blockState.get(FACING);
			if (direction2.getAxis() != state.get(FACING).getAxis() && isDifferentOrientation(
					state,
					world,
					pos,
					direction2.getOpposite()
			)) {
				if (direction2 == direction.rotateYCounterclockwise()) {
					return StairShape.OUTER_LEFT;
				}

				return StairShape.OUTER_RIGHT;
			}
		}

		BlockState blockState2 = world.getBlockState(pos.offset(direction.getOpposite()));
		if (isStairs(blockState2) && state.get(HALF) == blockState2.get(HALF)) {
			Direction direction3 = blockState2.get(FACING);
			if (direction3.getAxis() != state.get(FACING).getAxis() && isDifferentOrientation(
					state,
					world,
					pos,
					direction3
			)) {
				if (direction3 == direction.rotateYCounterclockwise()) {
					return StairShape.INNER_LEFT;
				}

				return StairShape.INNER_RIGHT;
			}
		}

		return StairShape.STRAIGHT;
	}

	private static boolean isDifferentOrientation(BlockState state, BlockView world, BlockPos pos, Direction dir) {
		BlockState blockState = world.getBlockState(pos.offset(dir));
		return !isStairs(blockState) || blockState.get(FACING) != state.get(FACING)
				|| blockState.get(HALF) != state.get(HALF);
	}

	public static boolean isStairs(BlockState state) {
		return state.getBlock() instanceof StairsBlock;
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		Direction direction = state.get(FACING);
		StairShape stairShape = state.get(SHAPE);
		switch (mirror) {
			case LEFT_RIGHT:
				if (direction.getAxis() == Direction.Axis.Z) {
					switch (stairShape) {
						case OUTER_LEFT:
							return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.OUTER_RIGHT);
						case INNER_RIGHT:
							return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.INNER_LEFT);
						case INNER_LEFT:
							return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.INNER_RIGHT);
						case OUTER_RIGHT:
							return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.OUTER_LEFT);
						default:
							return state.rotate(BlockRotation.CLOCKWISE_180);
					}
				}
				break;
			case FRONT_BACK:
				if (direction.getAxis() == Direction.Axis.X) {
					switch (stairShape) {
						case STRAIGHT:
							return state.rotate(BlockRotation.CLOCKWISE_180);
						case OUTER_LEFT:
							return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.OUTER_RIGHT);
						case INNER_RIGHT:
							return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.INNER_RIGHT);
						case INNER_LEFT:
							return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.INNER_LEFT);
						case OUTER_RIGHT:
							return state.rotate(BlockRotation.CLOCKWISE_180).with(SHAPE, StairShape.OUTER_LEFT);
					}
				}
		}

		return super.mirror(state, mirror);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF, SHAPE, WATERLOGGED);
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
