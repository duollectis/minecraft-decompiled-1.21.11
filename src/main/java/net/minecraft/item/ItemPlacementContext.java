package net.minecraft.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * Контекст размещения предмета-блока в мире.
 * <p>Расширяет {@link ItemUsageContext}, добавляя логику определения позиции размещения:
 * либо на месте существующего блока (если он заменяем), либо на соседней позиции.</p>
 */
public class ItemPlacementContext extends ItemUsageContext {

	private final BlockPos placementPos;
	protected boolean canReplaceExisting = true;

	public ItemPlacementContext(PlayerEntity player, Hand hand, ItemStack stack, BlockHitResult hitResult) {
		this(player.getEntityWorld(), player, hand, stack, hitResult);
	}

	public ItemPlacementContext(ItemUsageContext context) {
		this(context.getWorld(), context.getPlayer(), context.getHand(), context.getStack(), context.getHitResult());
	}

	public ItemPlacementContext(
			World world,
			@Nullable PlayerEntity player,
			Hand hand,
			ItemStack stack,
			BlockHitResult hitResult
	) {
		super(world, player, hand, stack, hitResult);
		placementPos = hitResult.getBlockPos().offset(hitResult.getSide());
		canReplaceExisting = world.getBlockState(hitResult.getBlockPos()).canReplace(this);
	}

	/**
	 * Создаёт контекст размещения со смещённой позицией и стороной.
	 * <p>Используется для размещения блоков рядом с уже существующими
	 * (например, при размещении через диспенсер).</p>
	 *
	 * @param context исходный контекст
	 * @param pos     новая позиция блока
	 * @param side    сторона, с которой происходит размещение
	 * @return новый контекст с обновлённой позицией
	 */
	public static ItemPlacementContext offset(ItemPlacementContext context, BlockPos pos, Direction side) {
		return new ItemPlacementContext(
				context.getWorld(),
				context.getPlayer(),
				context.getHand(),
				context.getStack(),
				new BlockHitResult(
						new Vec3d(
								pos.getX() + 0.5 + side.getOffsetX() * 0.5,
								pos.getY() + 0.5 + side.getOffsetY() * 0.5,
								pos.getZ() + 0.5 + side.getOffsetZ() * 0.5
						),
						side,
						pos,
						false
				)
		);
	}

	@Override
	public BlockPos getBlockPos() {
		return canReplaceExisting ? super.getBlockPos() : placementPos;
	}

	/**
	 * Проверяет, можно ли разместить блок на текущей позиции.
	 * <p>Возвращает {@code true} если целевой блок заменяем или позиция рядом с ним свободна.</p>
	 *
	 * @return {@code true} если размещение допустимо
	 */
	public boolean canPlace() {
		return canReplaceExisting || getWorld().getBlockState(getBlockPos()).canReplace(this);
	}

	public boolean canReplaceExisting() {
		return canReplaceExisting;
	}

	public Direction getPlayerLookDirection() {
		return Direction.getEntityFacingOrder(getPlayer())[0];
	}

	public Direction getVerticalPlayerLookDirection() {
		return Direction.getLookDirectionForAxis(getPlayer(), Direction.Axis.Y);
	}

	/**
	 * Возвращает приоритетный порядок направлений для размещения блока.
	 * <p>Если блок размещается не на заменяемом блоке, то направление от стороны попадания
	 * ставится первым в списке, чтобы блок ориентировался правильно.</p>
	 *
	 * @return массив направлений в порядке приоритета
	 */
	public Direction[] getPlacementDirections() {
		Direction[] directions = Direction.getEntityFacingOrder(getPlayer());
		if (canReplaceExisting) {
			return directions;
		}

		Direction hitSide = getSide();
		int insertIndex = 0;

		while (insertIndex < directions.length && directions[insertIndex] != hitSide.getOpposite()) {
			insertIndex++;
		}

		if (insertIndex > 0) {
			System.arraycopy(directions, 0, directions, 1, insertIndex);
			directions[0] = hitSide.getOpposite();
		}

		return directions;
	}
}
