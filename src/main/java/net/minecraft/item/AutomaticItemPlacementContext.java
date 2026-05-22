package net.minecraft.item;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Контекст автоматического размещения блока без участия игрока.
 * <p>Используется диспенсерами и другими механизмами автоматического размещения.
 * Переопределяет методы, зависящие от игрока, возвращая фиксированные значения.</p>
 */
public class AutomaticItemPlacementContext extends ItemPlacementContext {

	private final Direction facing;

	public AutomaticItemPlacementContext(World world, BlockPos pos, Direction facing, ItemStack stack, Direction side) {
		super(world, null, Hand.MAIN_HAND, stack, new BlockHitResult(Vec3d.ofBottomCenter(pos), side, pos, false));
		this.facing = facing;
	}

	@Override
	public BlockPos getBlockPos() {
		return getHitResult().getBlockPos();
	}

	@Override
	public boolean canPlace() {
		return getWorld().getBlockState(getHitResult().getBlockPos()).canReplace(this);
	}

	@Override
	public boolean canReplaceExisting() {
		return canPlace();
	}

	@Override
	public Direction getPlayerLookDirection() {
		return Direction.DOWN;
	}

	/**
	 * Возвращает порядок направлений для размещения блока в зависимости от стороны диспенсера.
	 * <p>Первым всегда идёт DOWN, затем направление диспенсера, затем остальные стороны.
	 * Это обеспечивает корректную ориентацию блоков при автоматическом размещении.</p>
	 *
	 * @return массив направлений в порядке приоритета
	 */
	@Override
	public Direction[] getPlacementDirections() {
		return switch (facing) {
			case UP -> new Direction[]{
					Direction.DOWN, Direction.UP,
					Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
			};
			case NORTH -> new Direction[]{
					Direction.DOWN, Direction.NORTH,
					Direction.EAST, Direction.WEST, Direction.UP, Direction.SOUTH
			};
			case SOUTH -> new Direction[]{
					Direction.DOWN, Direction.SOUTH,
					Direction.EAST, Direction.WEST, Direction.UP, Direction.NORTH
			};
			case WEST -> new Direction[]{
					Direction.DOWN, Direction.WEST,
					Direction.SOUTH, Direction.UP, Direction.NORTH, Direction.EAST
			};
			case EAST -> new Direction[]{
					Direction.DOWN, Direction.EAST,
					Direction.SOUTH, Direction.UP, Direction.NORTH, Direction.WEST
			};
			default -> new Direction[]{
					Direction.DOWN, Direction.NORTH,
					Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP
			};
		};
	}

	@Override
	public Direction getHorizontalPlayerFacing() {
		return facing.getAxis() == Direction.Axis.Y ? Direction.NORTH : facing;
	}

	@Override
	public boolean shouldCancelInteraction() {
		return false;
	}

	@Override
	public float getPlayerYaw() {
		return facing.getHorizontalQuarterTurns() * 90;
	}
}
