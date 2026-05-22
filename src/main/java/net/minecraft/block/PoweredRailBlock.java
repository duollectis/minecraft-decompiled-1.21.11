package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.enums.RailShape;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Рельс-ускоритель (золотой рельс) с поддержкой питания от редстоуна.
 * При получении сигнала ускоряет вагонетки; без сигнала — тормозит.
 * Питание распространяется по цепочке до 8 соседних рельсов того же типа.
 */
public class PoweredRailBlock extends AbstractRailBlock {

	public static final MapCodec<PoweredRailBlock> CODEC = createCodec(PoweredRailBlock::new);
	public static final EnumProperty<RailShape> SHAPE = Properties.STRAIGHT_RAIL_SHAPE;
	public static final BooleanProperty POWERED = Properties.POWERED;

	@Override
	public MapCodec<PoweredRailBlock> getCodec() {
		return CODEC;
	}

	public PoweredRailBlock(AbstractBlock.Settings settings) {
		super(true, settings);
		setDefaultState(stateManager
				.getDefaultState()
				.with(SHAPE, RailShape.NORTH_SOUTH)
				.with(POWERED, false)
				.with(WATERLOGGED, false));
	}

	/**
	 * Рекурсивно проверяет, получает ли рельс питание от соседних рельсов того же типа.
	 * Обходит цепочку в направлении {@code forward} на глубину до 8 блоков.
	 */
	protected boolean isPoweredByOtherRails(
			World world,
			BlockPos pos,
			BlockState state,
			boolean forward,
			int distance
	) {
		if (distance >= 8) {
			return false;
		}

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		boolean canCheckBelow = true;
		RailShape railShape = state.get(SHAPE);

		switch (railShape) {
			case NORTH_SOUTH:
				z += forward ? 1 : -1;
				break;
			case EAST_WEST:
				x += forward ? -1 : 1;
				break;
			case ASCENDING_EAST:
				if (forward) {
					x--;
				} else {
					x++;
					y++;
					canCheckBelow = false;
				}
				railShape = RailShape.EAST_WEST;
				break;
			case ASCENDING_WEST:
				if (forward) {
					x--;
					y++;
					canCheckBelow = false;
				} else {
					x++;
				}
				railShape = RailShape.EAST_WEST;
				break;
			case ASCENDING_NORTH:
				if (forward) {
					z++;
				} else {
					z--;
					y++;
					canCheckBelow = false;
				}
				railShape = RailShape.NORTH_SOUTH;
				break;
			case ASCENDING_SOUTH:
				if (forward) {
					z++;
					y++;
					canCheckBelow = false;
				} else {
					z--;
				}
				railShape = RailShape.NORTH_SOUTH;
				break;
			default:
				break;
		}

		return isPoweredByOtherRails(world, new BlockPos(x, y, z), forward, distance, railShape)
				|| canCheckBelow && isPoweredByOtherRails(world, new BlockPos(x, y - 1, z), forward, distance, railShape);
	}

	/**
	 * Проверяет, является ли рельс в позиции {@code pos} питающим звеном цепочки.
	 * Возвращает {@code false}, если форма рельса перпендикулярна направлению цепочки.
	 */
	protected boolean isPoweredByOtherRails(
			World world,
			BlockPos pos,
			boolean forward,
			int distance,
			RailShape shape
	) {
		BlockState neighborState = world.getBlockState(pos);
		if (neighborState.isOf(this) == false) {
			return false;
		}

		RailShape neighborShape = neighborState.get(SHAPE);
		if (shape == RailShape.EAST_WEST
				&& (neighborShape == RailShape.NORTH_SOUTH
				|| neighborShape == RailShape.ASCENDING_NORTH
				|| neighborShape == RailShape.ASCENDING_SOUTH)) {
			return false;
		}

		if (shape == RailShape.NORTH_SOUTH
				&& (neighborShape == RailShape.EAST_WEST
				|| neighborShape == RailShape.ASCENDING_EAST
				|| neighborShape == RailShape.ASCENDING_WEST)) {
			return false;
		}

		if (neighborState.get(POWERED) == false) {
			return false;
		}

		return world.isReceivingRedstonePower(pos)
				|| isPoweredByOtherRails(world, pos, neighborState, forward, distance + 1);
	}

	@Override
	protected void updateBlockState(BlockState state, World world, BlockPos pos, Block neighbor) {
		boolean wasPowered = state.get(POWERED);
		boolean nowPowered = world.isReceivingRedstonePower(pos)
				|| isPoweredByOtherRails(world, pos, state, true, 0)
				|| isPoweredByOtherRails(world, pos, state, false, 0);

		if (nowPowered != wasPowered) {
			world.setBlockState(pos, state.with(POWERED, nowPowered), 3);
			world.updateNeighbors(pos.down(), this);

			if (state.get(SHAPE).isAscending()) {
				world.updateNeighbors(pos.up(), this);
			}
		}
	}

	@Override
	public Property<RailShape> getShapeProperty() {
		return SHAPE;
	}

	@Override
	protected BlockState rotate(BlockState state, BlockRotation rotation) {
		return state.with(SHAPE, rotateShape(state.get(SHAPE), rotation));
	}

	@Override
	protected BlockState mirror(BlockState state, BlockMirror mirror) {
		return state.with(SHAPE, mirrorShape(state.get(SHAPE), mirror));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(SHAPE, POWERED, WATERLOGGED);
	}
}
