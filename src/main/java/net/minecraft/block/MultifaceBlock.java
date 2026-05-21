package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * {@code MultifaceBlock}.
 */
public class MultifaceBlock extends Block implements Waterloggable {

	public static final MapCodec<MultifaceBlock> CODEC = createCodec(MultifaceBlock::new);
	public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	private static final Map<Direction, BooleanProperty> FACING_PROPERTIES = ConnectingBlock.FACING_PROPERTIES;
	protected static final Direction[] DIRECTIONS = Direction.values();
	private final Function<BlockState, VoxelShape> shapeFunction;
	private final boolean hasAllHorizontalDirections;
	private final boolean canMirrorX;
	private final boolean canMirrorZ;

	@Override
	protected MapCodec<? extends MultifaceBlock> getCodec() {
		return CODEC;
	}

	public MultifaceBlock(AbstractBlock.Settings settings) {
		super(settings);
		this.setDefaultState(withAllDirections(this.stateManager));
		this.shapeFunction = this.createShapeFunction();
		this.hasAllHorizontalDirections = Direction.Type.HORIZONTAL.stream().allMatch(this::canHaveDirection);
		this.canMirrorX =
				Direction.Type.HORIZONTAL.stream().filter(Direction.Axis.X).filter(this::canHaveDirection).count() % 2L
						== 0L;
		this.canMirrorZ =
				Direction.Type.HORIZONTAL.stream().filter(Direction.Axis.Z).filter(this::canHaveDirection).count() % 2L
						== 0L;
	}

	private Function<BlockState, VoxelShape> createShapeFunction() {
		Map<Direction, VoxelShape> map = VoxelShapes.createFacingShapeMap(Block.createCuboidZShape(16.0, 0.0, 1.0));
		return this.createShapeFunction(
				state -> {
					VoxelShape voxelShape = VoxelShapes.empty();

					for (Direction direction : DIRECTIONS) {
						if (hasDirection(state, direction)) {
							voxelShape = VoxelShapes.union(voxelShape, map.get(direction));
						}
					}

					return voxelShape.isEmpty() ? VoxelShapes.fullCube() : voxelShape;
				}, WATERLOGGED
		);
	}

	/**
	 * Collect directions.
	 *
	 * @param state state
	 *
	 * @return Set — результат операции
	 */
	public static Set<Direction> collectDirections(BlockState state) {
		if (!(state.getBlock() instanceof MultifaceBlock)) {
			return Set.of();
		}
		else {
			Set<Direction> set = EnumSet.noneOf(Direction.class);

			for (Direction direction : Direction.values()) {
				if (hasDirection(state, direction)) {
					set.add(direction);
				}
			}

			return set;
		}
	}

	/**
	 * Flag to directions.
	 *
	 * @param flag flag
	 *
	 * @return Set — результат операции
	 */
	public static Set<Direction> flagToDirections(byte flag) {
		Set<Direction> set = EnumSet.noneOf(Direction.class);

		for (Direction direction : Direction.values()) {
			if ((flag & (byte) (1 << direction.ordinal())) > 0) {
				set.add(direction);
			}
		}

		return set;
	}

	/**
	 * Directions to flag.
	 *
	 * @param directions directions
	 *
	 * @return byte — результат операции
	 */
	public static byte directionsToFlag(Collection<Direction> directions) {
		byte b = 0;

		for (Direction direction : directions) {
			b = (byte) (b | 1 << direction.ordinal());
		}

		return b;
	}

	/**
	 * Проверяет возможность have direction.
	 *
	 * @param direction direction
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	protected boolean canHaveDirection(Direction direction) {
		return true;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		for (Direction direction : DIRECTIONS) {
			if (this.canHaveDirection(direction)) {
				builder.add(getProperty(direction));
			}
		}

		builder.add(WATERLOGGED);
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

		if (!hasAnyDirection(state)) {
			return Blocks.AIR.getDefaultState();
		}
		else {
			return hasDirection(state, direction) && !canGrowOn(world, direction, neighborPos, neighborState)
			       ? disableDirection(state, getProperty(direction))
			       : state;
		}
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return this.shapeFunction.apply(state);
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		boolean bl = false;

		for (Direction direction : DIRECTIONS) {
			if (hasDirection(state, direction)) {
				if (!canGrowOn(world, pos, direction)) {
					return false;
				}

				bl = true;
			}
		}

		return bl;
	}

	@Override
	protected boolean canReplace(BlockState state, ItemPlacementContext context) {
		return !context.getStack().isOf(this.asItem()) || isNotFullBlock(state);
	}

	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		World world = ctx.getWorld();
		BlockPos blockPos = ctx.getBlockPos();
		BlockState blockState = world.getBlockState(blockPos);
		return Arrays.stream(ctx.getPlacementDirections())
		             .map(direction -> this.withDirection(blockState, world, blockPos, direction))
		             .filter(Objects::nonNull)
		             .findFirst()
		             .orElse(null);
	}

	/**
	 * Проверяет возможность grow with direction.
	 *
	 * @param world world
	 * @param state state
	 * @param pos pos
	 * @param direction direction
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public boolean canGrowWithDirection(BlockView world, BlockState state, BlockPos pos, Direction direction) {
		if (this.canHaveDirection(direction) && (!state.isOf(this) || !hasDirection(state, direction))) {
			BlockPos blockPos = pos.offset(direction);
			return canGrowOn(world, direction, blockPos, world.getBlockState(blockPos));
		}
		else {
			return false;
		}
	}

	/**
	 * With direction.
	 *
	 * @param state state
	 * @param world world
	 * @param pos pos
	 * @param direction direction
	 *
	 * @return @Nullable BlockState — результат операции
	 */
	public @Nullable BlockState withDirection(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		if (!this.canGrowWithDirection(world, state, pos, direction)) {
			return null;
		}
		else {
			BlockState blockState;
			if (state.isOf(this)) {
				blockState = state;
			}
			else if (state.getFluidState().isEqualAndStill(Fluids.WATER)) {
				blockState = this.getDefaultState().with(Properties.WATERLOGGED, true);
			}
			else {
				blockState = this.getDefaultState();
			}

			return blockState.with(getProperty(direction), true);
		}
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return !this.hasAllHorizontalDirections ? state : this.mirror(state, rotation::rotate);
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		if (mirror == BlockMirror.FRONT_BACK && !this.canMirrorX) {
			return state;
		}
		else {
			return mirror == BlockMirror.LEFT_RIGHT && !this.canMirrorZ ? state : this.mirror(state, mirror::apply);
		}
	}

	private BlockState mirror(BlockState state, Function<Direction, Direction> mirror) {
		BlockState blockState = state;

		for (Direction direction : DIRECTIONS) {
			if (this.canHaveDirection(direction)) {
				blockState = blockState.with(getProperty(mirror.apply(direction)), state.get(getProperty(direction)));
			}
		}

		return blockState;
	}

	public static boolean hasDirection(BlockState state, Direction direction) {
		BooleanProperty booleanProperty = getProperty(direction);
		return state.get(booleanProperty, false);
	}

	/**
	 * Проверяет возможность grow on.
	 *
	 * @param world world
	 * @param pos pos
	 * @param direction direction
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public static boolean canGrowOn(BlockView world, BlockPos pos, Direction direction) {
		BlockPos blockPos = pos.offset(direction);
		BlockState blockState = world.getBlockState(blockPos);
		return canGrowOn(world, direction, blockPos, blockState);
	}

	/**
	 * Проверяет возможность grow on.
	 *
	 * @param world world
	 * @param direction direction
	 * @param pos pos
	 * @param state state
	 *
	 * @return boolean — {@code true} если условие выполнено
	 */
	public static boolean canGrowOn(BlockView world, Direction direction, BlockPos pos, BlockState state) {
		return Block.isFaceFullSquare(state.getSidesShape(world, pos), direction.getOpposite())
				|| Block.isFaceFullSquare(state.getCollisionShape(world, pos), direction.getOpposite());
	}

	private static BlockState disableDirection(BlockState state, BooleanProperty direction) {
		BlockState blockState = state.with(direction, false);
		return hasAnyDirection(blockState) ? blockState : Blocks.AIR.getDefaultState();
	}

	public static BooleanProperty getProperty(Direction direction) {
		return FACING_PROPERTIES.get(direction);
	}

	private static BlockState withAllDirections(StateManager<Block, BlockState> stateManager) {
		BlockState blockState = stateManager.getDefaultState().with(WATERLOGGED, false);

		for (BooleanProperty booleanProperty : FACING_PROPERTIES.values()) {
			blockState = blockState.withIfExists(booleanProperty, false);
		}

		return blockState;
	}

	protected static boolean hasAnyDirection(BlockState state) {
		for (Direction direction : DIRECTIONS) {
			if (hasDirection(state, direction)) {
				return true;
			}
		}

		return false;
	}

	private static boolean isNotFullBlock(BlockState state) {
		for (Direction direction : DIRECTIONS) {
			if (!hasDirection(state, direction)) {
				return true;
			}
		}

		return false;
	}
}
