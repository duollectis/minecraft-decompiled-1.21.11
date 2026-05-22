package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * Блок лианы, прикрепляющийся к вертикальным поверхностям и растущий вниз.
 * Поддерживает пять направлений (UP, N, E, S, W). При случайном тике
 * распространяется в соседние позиции, если плотность лиан в радиусе 4 блоков
 * не превышает {@link #MAX_VINES_IN_AREA}.
 */
public class VineBlock extends Block {

	public static final MapCodec<VineBlock> CODEC = createCodec(VineBlock::new);
	public static final BooleanProperty UP = ConnectingBlock.UP;
	public static final BooleanProperty NORTH = ConnectingBlock.NORTH;
	public static final BooleanProperty EAST = ConnectingBlock.EAST;
	public static final BooleanProperty SOUTH = ConnectingBlock.SOUTH;
	public static final BooleanProperty WEST = ConnectingBlock.WEST;
	public static final Map<Direction, BooleanProperty> FACING_PROPERTIES = ConnectingBlock.FACING_PROPERTIES
			.entrySet()
			.stream()
			.filter(entry -> entry.getKey() != Direction.DOWN)
			.collect(Util.toMap());
	private final Function<BlockState, VoxelShape> shapeFunction;

	@Override
	public MapCodec<VineBlock> getCodec() {
		return CODEC;
	}

	public VineBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(this.stateManager
				.getDefaultState()
				.with(UP, false)
				.with(NORTH, false)
				.with(EAST, false)
				.with(SOUTH, false)
				.with(WEST, false));
		this.shapeFunction = this.createShapeFunction();
	}

	private Function<BlockState, VoxelShape> createShapeFunction() {
		Map<Direction, VoxelShape> map = VoxelShapes.createFacingShapeMap(Block.createCuboidZShape(16.0, 0.0, 1.0));
		return this.createShapeFunction(state -> {
			VoxelShape voxelShape = VoxelShapes.empty();

			for (Entry<Direction, BooleanProperty> entry : FACING_PROPERTIES.entrySet()) {
				if (state.get(entry.getValue())) {
					voxelShape = VoxelShapes.union(voxelShape, map.get(entry.getKey()));
				}
			}

			return voxelShape.isEmpty() ? VoxelShapes.fullCube() : voxelShape;
		});
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return this.shapeFunction.apply(state);
	}

	@Override
	protected boolean isTransparent(BlockState state) {
		return true;
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return this.hasAdjacentBlocks(this.getPlacementShape(state, world, pos));
	}

	private boolean hasAdjacentBlocks(BlockState state) {
		return this.getAdjacentBlockCount(state) > 0;
	}

	private int getAdjacentBlockCount(BlockState state) {
		int i = 0;

		for (BooleanProperty booleanProperty : FACING_PROPERTIES.values()) {
			if (state.get(booleanProperty)) {
				i++;
			}
		}

		return i;
	}

	private boolean shouldHaveSide(BlockView world, BlockPos pos, Direction side) {
		if (side == Direction.DOWN) {
			return false;
		}
		else {
			BlockPos blockPos = pos.offset(side);
			if (shouldConnectTo(world, blockPos, side)) {
				return true;
			}
			else if (side.getAxis() == Direction.Axis.Y) {
				return false;
			}
			else {
				BooleanProperty booleanProperty = FACING_PROPERTIES.get(side);
				BlockState blockState = world.getBlockState(pos.up());
				return blockState.isOf(this) && blockState.get(booleanProperty);
			}
		}
	}

	public static boolean shouldConnectTo(BlockView world, BlockPos pos, Direction direction) {
		return MultifaceBlock.canGrowOn(world, direction, pos, world.getBlockState(pos));
	}

	private BlockState getPlacementShape(BlockState state, BlockView world, BlockPos pos) {
		BlockPos blockPos = pos.up();
		if (state.get(UP)) {
			state = state.with(UP, shouldConnectTo(world, blockPos, Direction.DOWN));
		}

		BlockState blockState = null;

		for (Direction direction : Direction.Type.HORIZONTAL) {
			BooleanProperty booleanProperty = getFacingProperty(direction);
			if (state.get(booleanProperty)) {
				boolean bl = this.shouldHaveSide(world, pos, direction);
				if (!bl) {
					if (blockState == null) {
						blockState = world.getBlockState(blockPos);
					}

					bl = blockState.isOf(this) && blockState.get(booleanProperty);
				}

				state = state.with(booleanProperty, bl);
			}
		}

		return state;
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
			BlockState blockState = this.getPlacementShape(state, world, pos);
			return !this.hasAdjacentBlocks(blockState) ? Blocks.AIR.getDefaultState() : blockState;
		}
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (world.getGameRules().getValue(GameRules.SPREAD_VINES)) {
			if (random.nextInt(4) == 0) {
				Direction direction = Direction.random(random);
				BlockPos blockPos = pos.up();
				if (direction.getAxis().isHorizontal() && !state.get(getFacingProperty(direction))) {
					if (this.canGrowAt(world, pos)) {
						BlockPos blockPos2 = pos.offset(direction);
						BlockState blockState = world.getBlockState(blockPos2);
						if (blockState.isAir()) {
							Direction direction2 = direction.rotateYClockwise();
							Direction direction3 = direction.rotateYCounterclockwise();
							boolean hasClockwiseSide = state.get(getFacingProperty(direction2));
							boolean hasCounterClockwiseSide = state.get(getFacingProperty(direction3));
							BlockPos blockPos3 = blockPos2.offset(direction2);
							BlockPos blockPos4 = blockPos2.offset(direction3);
							if (hasClockwiseSide && shouldConnectTo(world, blockPos3, direction2)) {
								world.setBlockState(
										blockPos2,
										this.getDefaultState().with(getFacingProperty(direction2), true),
										2
								);
							}
							else if (hasCounterClockwiseSide && shouldConnectTo(world, blockPos4, direction3)) {
								world.setBlockState(
										blockPos2,
										this.getDefaultState().with(getFacingProperty(direction3), true),
										2
								);
							}
							else {
								Direction direction4 = direction.getOpposite();
								if (hasClockwiseSide && world.isAir(blockPos3) && shouldConnectTo(
										world,
										pos.offset(direction2),
										direction4
								)) {
									world.setBlockState(
											blockPos3,
											this.getDefaultState().with(getFacingProperty(direction4), true),
											2
									);
								}
								else if (hasCounterClockwiseSide && world.isAir(blockPos4) && shouldConnectTo(
										world,
										pos.offset(direction3),
										direction4
								)) {
									world.setBlockState(
											blockPos4,
											this.getDefaultState().with(getFacingProperty(direction4), true),
											2
									);
								}
								else if (random.nextFloat() < 0.05 && shouldConnectTo(
										world,
										blockPos2.up(),
										Direction.UP
								)) {
									world.setBlockState(blockPos2, this.getDefaultState().with(UP, true), 2);
								}
							}
						}
						else if (shouldConnectTo(world, blockPos2, direction)) {
							world.setBlockState(pos, state.with(getFacingProperty(direction), true), 2);
						}
					}
				}
				else {
					if (direction == Direction.UP && pos.getY() < world.getTopYInclusive()) {
						if (this.shouldHaveSide(world, pos, direction)) {
							world.setBlockState(pos, state.with(UP, true), 2);
							return;
						}

						if (world.isAir(blockPos)) {
							if (!this.canGrowAt(world, pos)) {
								return;
							}

							BlockState blockState2 = state;

							for (Direction direction2 : Direction.Type.HORIZONTAL) {
								if (random.nextBoolean() || !shouldConnectTo(
										world,
										blockPos.offset(direction2),
										direction2
								)) {
									blockState2 = blockState2.with(getFacingProperty(direction2), false);
								}
							}

							if (this.hasHorizontalSide(blockState2)) {
								world.setBlockState(blockPos, blockState2, 2);
							}

							return;
						}
					}

					if (pos.getY() > world.getBottomY()) {
						BlockPos blockPos2 = pos.down();
						BlockState blockState = world.getBlockState(blockPos2);
						if (blockState.isAir() || blockState.isOf(this)) {
							BlockState blockState3 = blockState.isAir() ? this.getDefaultState() : blockState;
							BlockState blockState4 = this.getGrownState(state, blockState3, random);
							if (blockState3 != blockState4 && this.hasHorizontalSide(blockState4)) {
								world.setBlockState(blockPos2, blockState4, 2);
							}
						}
					}
				}
			}
		}
	}

	private BlockState getGrownState(BlockState above, BlockState state, Random random) {
		for (Direction direction : Direction.Type.HORIZONTAL) {
			if (random.nextBoolean()) {
				BooleanProperty booleanProperty = getFacingProperty(direction);
				if (above.get(booleanProperty)) {
					state = state.with(booleanProperty, true);
				}
			}
		}

		return state;
	}

	private boolean hasHorizontalSide(BlockState state) {
		return state.get(NORTH) || state.get(EAST) || state.get(SOUTH) || state.get(WEST);
	}

	private static final int GROWTH_RADIUS = 4;
	private static final int MAX_VINES_IN_AREA = 5;

	private boolean canGrowAt(BlockView world, BlockPos pos) {
		int vineCount = MAX_VINES_IN_AREA;

		for (BlockPos blockPos : BlockPos.iterate(
				pos.getX() - GROWTH_RADIUS,
				pos.getY() - 1,
				pos.getZ() - GROWTH_RADIUS,
				pos.getX() + GROWTH_RADIUS,
				pos.getY() + 1,
				pos.getZ() + GROWTH_RADIUS
		)) {
			if (world.getBlockState(blockPos).isOf(this)) {
				if (--vineCount <= 0) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	protected boolean canReplace(BlockState state, ItemPlacementContext context) {
		BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());
		return blockState.isOf(this) ? this.getAdjacentBlockCount(blockState) < FACING_PROPERTIES.size()
		                             : super.canReplace(state, context);
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos());
		boolean isVine = blockState.isOf(this);
		BlockState blockState2 = isVine ? blockState : this.getDefaultState();

		for (Direction direction : ctx.getPlacementDirections()) {
			if (direction != Direction.DOWN) {
				BooleanProperty booleanProperty = getFacingProperty(direction);
				boolean alreadyHasSide = isVine && blockState.get(booleanProperty);
				if (!alreadyHasSide && this.shouldHaveSide(ctx.getWorld(), ctx.getBlockPos(), direction)) {
					return blockState2.with(booleanProperty, true);
				}
			}
		}

		return isVine ? blockState2 : null;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(UP, NORTH, EAST, SOUTH, WEST);
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		switch (rotation) {
			case CLOCKWISE_180:
				return state
						.with(NORTH, state.get(SOUTH))
						.with(EAST, state.get(WEST))
						.with(SOUTH, state.get(NORTH))
						.with(WEST, state.get(EAST));
			case COUNTERCLOCKWISE_90:
				return state
						.with(NORTH, state.get(EAST))
						.with(EAST, state.get(SOUTH))
						.with(SOUTH, state.get(WEST))
						.with(WEST, state.get(NORTH));
			case CLOCKWISE_90:
				return state
						.with(NORTH, state.get(WEST))
						.with(EAST, state.get(NORTH))
						.with(SOUTH, state.get(EAST))
						.with(WEST, state.get(SOUTH));
			default:
				return state;
		}
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		switch (mirror) {
			case LEFT_RIGHT:
				return state.with(NORTH, state.get(SOUTH)).with(SOUTH, state.get(NORTH));
			case FRONT_BACK:
				return state.with(EAST, state.get(WEST)).with(WEST, state.get(EAST));
			default:
				return super.mirror(state, mirror);
		}
	}

	public static BooleanProperty getFacingProperty(Direction direction) {
		return FACING_PROPERTIES.get(direction);
	}
}
