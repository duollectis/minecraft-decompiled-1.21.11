package net.minecraft.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.enums.WallShape;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Блок стены, автоматически соединяющийся с соседними блоками стен, заборов
 * и сплошными поверхностями. Поддерживает три формы соединения: NONE, LOW, TALL.
 * Реализует {@link Waterloggable} — может быть заполнен водой.
 */
public class WallBlock extends Block implements Waterloggable {

	public static final MapCodec<WallBlock> CODEC = createCodec(WallBlock::new);
	public static final BooleanProperty UP = Properties.UP;
	public static final EnumProperty<WallShape> EAST_WALL_SHAPE = Properties.EAST_WALL_SHAPE;
	public static final EnumProperty<WallShape> NORTH_WALL_SHAPE = Properties.NORTH_WALL_SHAPE;
	public static final EnumProperty<WallShape> SOUTH_WALL_SHAPE = Properties.SOUTH_WALL_SHAPE;
	public static final EnumProperty<WallShape> WEST_WALL_SHAPE = Properties.WEST_WALL_SHAPE;
	public static final Map<Direction, EnumProperty<WallShape>>
			WALL_SHAPE_PROPERTIES_BY_DIRECTION =
			ImmutableMap.copyOf(
					Maps.newEnumMap(
							Map.of(
									Direction.NORTH,
									NORTH_WALL_SHAPE,
									Direction.EAST,
									EAST_WALL_SHAPE,
									Direction.SOUTH,
									SOUTH_WALL_SHAPE,
									Direction.WEST,
									WEST_WALL_SHAPE
							)
					)
			);
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private final Function<BlockState, VoxelShape> outlineShapeFunction;
	private final Function<BlockState, VoxelShape> collisionShapeFunction;
	private static final VoxelShape POST_SHAPE_FOR_TALL_TEST = Block.createColumnShape(2.0, 0.0, 16.0);
	private static final Map<Direction, VoxelShape>
			WALL_SHAPES_FOR_TALL_TEST_BY_DIRECTION =
			VoxelShapes.createHorizontalFacingShapeMap(
					Block.createCuboidZShape(2.0, 16.0, 0.0, 9.0)
			);

	@Override
	public MapCodec<WallBlock> getCodec() {
		return CODEC;
	}

	public WallBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(
				this.stateManager
						.getDefaultState()
						.with(UP, true)
						.with(NORTH_WALL_SHAPE, WallShape.NONE)
						.with(EAST_WALL_SHAPE, WallShape.NONE)
						.with(SOUTH_WALL_SHAPE, WallShape.NONE)
						.with(WEST_WALL_SHAPE, WallShape.NONE)
						.with(WATERLOGGED, false)
		);
		this.outlineShapeFunction = this.createShapeFunction(16.0F, 14.0F);
		this.collisionShapeFunction = this.createShapeFunction(24.0F, 24.0F);
	}

	private Function<BlockState, VoxelShape> createShapeFunction(float tallHeight, float lowHeight) {
		VoxelShape voxelShape = Block.createColumnShape(8.0, 0.0, tallHeight);
		Map<Direction, VoxelShape>
				map =
				VoxelShapes.createHorizontalFacingShapeMap(Block.createCuboidZShape(6.0, 0.0, lowHeight, 0.0, 11.0));
		Map<Direction, VoxelShape>
				map2 =
				VoxelShapes.createHorizontalFacingShapeMap(Block.createCuboidZShape(6.0, 0.0, tallHeight, 0.0, 11.0));
		return this.createShapeFunction(
				state -> {
					VoxelShape voxelShape2 = state.get(UP) ? voxelShape : VoxelShapes.empty();

					for (Entry<Direction, EnumProperty<WallShape>> entry : WALL_SHAPE_PROPERTIES_BY_DIRECTION.entrySet()) {
						voxelShape2 = VoxelShapes.union(
								voxelShape2, switch ((WallShape) state.get(entry.getValue())) {
									case NONE -> VoxelShapes.empty();
									case LOW -> (VoxelShape) map.get(entry.getKey());
									case TALL -> (VoxelShape) map2.get(entry.getKey());
								}
						);
					}

					return voxelShape2;
				}, WATERLOGGED
		);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return this.outlineShapeFunction.apply(state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return this.collisionShapeFunction.apply(state);
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	private boolean shouldConnectTo(BlockState state, boolean faceFullSquare, Direction side) {
		Block block = state.getBlock();
		boolean bl = block instanceof FenceGateBlock && FenceGateBlock.canWallConnect(state, side);
		return state.isIn(BlockTags.WALLS) || !cannotConnect(state) && faceFullSquare || block instanceof PaneBlock
				|| bl;
	}

	@Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		WorldView worldView = ctx.getWorld();
		BlockPos blockPos = ctx.getBlockPos();
		FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
		BlockPos blockPos2 = blockPos.north();
		BlockPos blockPos3 = blockPos.east();
		BlockPos blockPos4 = blockPos.south();
		BlockPos blockPos5 = blockPos.west();
		BlockPos blockPos6 = blockPos.up();
		BlockState blockState = worldView.getBlockState(blockPos2);
		BlockState blockState2 = worldView.getBlockState(blockPos3);
		BlockState blockState3 = worldView.getBlockState(blockPos4);
		BlockState blockState4 = worldView.getBlockState(blockPos5);
		BlockState blockState5 = worldView.getBlockState(blockPos6);
		boolean connectNorth = this.shouldConnectTo(
				blockState,
				blockState.isSideSolidFullSquare(worldView, blockPos2, Direction.SOUTH),
				Direction.SOUTH
		);
		boolean connectEast = this.shouldConnectTo(
				blockState2,
				blockState2.isSideSolidFullSquare(worldView, blockPos3, Direction.WEST),
				Direction.WEST
		);
		boolean connectSouth = this.shouldConnectTo(
				blockState3,
				blockState3.isSideSolidFullSquare(worldView, blockPos4, Direction.NORTH),
				Direction.NORTH
		);
		boolean connectWest = this.shouldConnectTo(
				blockState4,
				blockState4.isSideSolidFullSquare(worldView, blockPos5, Direction.EAST),
				Direction.EAST
		);
		BlockState blockState6 = this.getDefaultState().with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
		return this.getStateWith(worldView, blockState6, blockPos6, blockState5, connectNorth, connectEast, connectSouth, connectWest);
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

		if (direction == Direction.DOWN) {
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
		else {
			return direction == Direction.UP
			       ? this.getStateAt(world, state, neighborPos, neighborState)
			       : this.getStateWithNeighbor(world, pos, state, neighborPos, neighborState, direction);
		}
	}

	private static boolean isConnected(BlockState state, Property<WallShape> property) {
		return state.get(property) != WallShape.NONE;
	}

	private static boolean shouldUseTallShape(VoxelShape aboveShape, VoxelShape tallShape) {
		return !VoxelShapes.matchesAnywhere(tallShape, aboveShape, BooleanBiFunction.ONLY_FIRST);
	}

	private BlockState getStateAt(WorldView world, BlockState state, BlockPos pos, BlockState aboveState) {
		boolean bl = isConnected(state, NORTH_WALL_SHAPE);
		boolean bl2 = isConnected(state, EAST_WALL_SHAPE);
		boolean bl3 = isConnected(state, SOUTH_WALL_SHAPE);
		boolean bl4 = isConnected(state, WEST_WALL_SHAPE);
		return this.getStateWith(world, state, pos, aboveState, bl, bl2, bl3, bl4);
	}

	private BlockState getStateWithNeighbor(
			WorldView world,
			BlockPos pos,
			BlockState state,
			BlockPos neighborPos,
			BlockState neighborState,
			Direction direction
	) {
		Direction direction2 = direction.getOpposite();
		boolean bl = direction == Direction.NORTH
		             ? this.shouldConnectTo(
				neighborState,
				neighborState.isSideSolidFullSquare(world, neighborPos, direction2),
				direction2
		)
		             : isConnected(state, NORTH_WALL_SHAPE);
		boolean bl2 = direction == Direction.EAST
		              ? this.shouldConnectTo(
				neighborState,
				neighborState.isSideSolidFullSquare(world, neighborPos, direction2),
				direction2
		)
		              : isConnected(state, EAST_WALL_SHAPE);
		boolean bl3 = direction == Direction.SOUTH
		              ? this.shouldConnectTo(
				neighborState,
				neighborState.isSideSolidFullSquare(world, neighborPos, direction2),
				direction2
		)
		              : isConnected(state, SOUTH_WALL_SHAPE);
		boolean bl4 = direction == Direction.WEST
		              ? this.shouldConnectTo(
				neighborState,
				neighborState.isSideSolidFullSquare(world, neighborPos, direction2),
				direction2
		)
		              : isConnected(state, WEST_WALL_SHAPE);
		BlockPos blockPos = pos.up();
		BlockState blockState = world.getBlockState(blockPos);
		return this.getStateWith(world, state, blockPos, blockState, bl, bl2, bl3, bl4);
	}

	private BlockState getStateWith(
			WorldView world,
			BlockState state,
			BlockPos pos,
			BlockState aboveState,
			boolean north,
			boolean east,
			boolean south,
			boolean west
	) {
		VoxelShape voxelShape = aboveState.getCollisionShape(world, pos).getFace(Direction.DOWN);
		BlockState blockState = this.getStateWith(state, north, east, south, west, voxelShape);
		return blockState.with(UP, this.shouldHavePost(blockState, aboveState, voxelShape));
	}

	private boolean shouldHavePost(BlockState state, BlockState aboveState, VoxelShape aboveShape) {
		boolean bl = aboveState.getBlock() instanceof WallBlock && aboveState.get(UP);
		if (bl) {
			return true;
		}
		else {
			WallShape wallShape = state.get(NORTH_WALL_SHAPE);
			WallShape wallShape2 = state.get(SOUTH_WALL_SHAPE);
			WallShape wallShape3 = state.get(EAST_WALL_SHAPE);
			WallShape wallShape4 = state.get(WEST_WALL_SHAPE);
			boolean bl2 = wallShape2 == WallShape.NONE;
			boolean bl3 = wallShape4 == WallShape.NONE;
			boolean bl4 = wallShape3 == WallShape.NONE;
			boolean bl5 = wallShape == WallShape.NONE;
			boolean bl6 = bl5 && bl2 && bl3 && bl4 || bl5 != bl2 || bl3 != bl4;
			if (bl6) {
				return true;
			}
			else {
				boolean
						bl7 =
						wallShape == WallShape.TALL && wallShape2 == WallShape.TALL
								|| wallShape3 == WallShape.TALL && wallShape4 == WallShape.TALL;
				return bl7 ? false : aboveState.isIn(BlockTags.WALL_POST_OVERRIDE) || shouldUseTallShape(
						aboveShape,
						POST_SHAPE_FOR_TALL_TEST
				);
			}
		}
	}

	private BlockState getStateWith(
			BlockState state,
			boolean north,
			boolean east,
			boolean south,
			boolean west,
			VoxelShape aboveShape
	) {
		return state
				.with(
						NORTH_WALL_SHAPE,
						this.getWallShape(
								north,
								aboveShape,
								WALL_SHAPES_FOR_TALL_TEST_BY_DIRECTION.get(Direction.NORTH)
						)
				)
				.with(
						EAST_WALL_SHAPE,
						this.getWallShape(east, aboveShape, WALL_SHAPES_FOR_TALL_TEST_BY_DIRECTION.get(Direction.EAST))
				)
				.with(
						SOUTH_WALL_SHAPE,
						this.getWallShape(
								south,
								aboveShape,
								WALL_SHAPES_FOR_TALL_TEST_BY_DIRECTION.get(Direction.SOUTH)
						)
				)
				.with(
						WEST_WALL_SHAPE,
						this.getWallShape(west, aboveShape, WALL_SHAPES_FOR_TALL_TEST_BY_DIRECTION.get(Direction.WEST))
				);
	}

	private WallShape getWallShape(boolean connected, VoxelShape aboveShape, VoxelShape tallShape) {
		if (connected) {
			return shouldUseTallShape(aboveShape, tallShape) ? WallShape.TALL : WallShape.LOW;
		}
		else {
			return WallShape.NONE;
		}
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	protected boolean isTransparent(BlockState state) {
		return !state.get(WATERLOGGED);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(UP, NORTH_WALL_SHAPE, EAST_WALL_SHAPE, WEST_WALL_SHAPE, SOUTH_WALL_SHAPE, WATERLOGGED);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		switch (rotation) {
			case CLOCKWISE_180:
				return state.with(NORTH_WALL_SHAPE, state.get(SOUTH_WALL_SHAPE))
				            .with(EAST_WALL_SHAPE, state.get(WEST_WALL_SHAPE))
				            .with(SOUTH_WALL_SHAPE, state.get(NORTH_WALL_SHAPE))
				            .with(WEST_WALL_SHAPE, state.get(EAST_WALL_SHAPE));
			case COUNTERCLOCKWISE_90:
				return state.with(NORTH_WALL_SHAPE, state.get(EAST_WALL_SHAPE))
				            .with(EAST_WALL_SHAPE, state.get(SOUTH_WALL_SHAPE))
				            .with(SOUTH_WALL_SHAPE, state.get(WEST_WALL_SHAPE))
				            .with(WEST_WALL_SHAPE, state.get(NORTH_WALL_SHAPE));
			case CLOCKWISE_90:
				return state.with(NORTH_WALL_SHAPE, state.get(WEST_WALL_SHAPE))
				            .with(EAST_WALL_SHAPE, state.get(NORTH_WALL_SHAPE))
				            .with(SOUTH_WALL_SHAPE, state.get(EAST_WALL_SHAPE))
				            .with(WEST_WALL_SHAPE, state.get(SOUTH_WALL_SHAPE));
			default:
				return state;
		}
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		switch (mirror) {
			case LEFT_RIGHT:
				return state
						.with(NORTH_WALL_SHAPE, state.get(SOUTH_WALL_SHAPE))
						.with(SOUTH_WALL_SHAPE, state.get(NORTH_WALL_SHAPE));
			case FRONT_BACK:
				return state
						.with(EAST_WALL_SHAPE, state.get(WEST_WALL_SHAPE))
						.with(WEST_WALL_SHAPE, state.get(EAST_WALL_SHAPE));
			default:
				return super.mirror(state, mirror);
		}
	}
}
