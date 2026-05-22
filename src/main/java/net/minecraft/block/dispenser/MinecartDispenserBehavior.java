package net.minecraft.block.dispenser;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Поведение диспенсера для вагонеток: спавнит вагонетку на рельсах перед диспенсером.
 * Если перед диспенсером нет рельсов — делегирует стандартному выбросу предмета.
 */
public class MinecartDispenserBehavior extends ItemDispenserBehavior {

	/** Горизонтальное смещение позиции спавна от центра диспенсера. */
	private static final double SPAWN_HORIZONTAL_OFFSET = 1.125;

	/** Смещение Y для вагонетки на восходящих рельсах. */
	private static final double ASCENDING_RAIL_Y_OFFSET = 0.6;

	/** Смещение Y для вагонетки на горизонтальных рельсах. */
	private static final double FLAT_RAIL_Y_OFFSET = 0.1;

	/** Смещение Y для вагонетки на восходящих рельсах снизу (блок воздуха над рельсами). */
	private static final double ASCENDING_BELOW_Y_OFFSET = -0.4;

	/** Смещение Y для вагонетки на горизонтальных рельсах снизу. */
	private static final double FLAT_BELOW_Y_OFFSET = -0.9;

	/** Код мирового события «диспенсер сработал» (звук). */
	private static final int DISPENSE_SOUND_EVENT = 1000;

	private final ItemDispenserBehavior fallbackBehavior = new ItemDispenserBehavior();
	private final EntityType<? extends AbstractMinecartEntity> minecartEntityType;

	public MinecartDispenserBehavior(EntityType<? extends AbstractMinecartEntity> minecartEntityType) {
		this.minecartEntityType = minecartEntityType;
	}

	@Override
	public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
		Direction direction = pointer.state().get(DispenserBlock.FACING);
		ServerWorld serverWorld = pointer.world();
		Vec3d center = pointer.centerPos();
		double spawnX = center.getX() + direction.getOffsetX() * SPAWN_HORIZONTAL_OFFSET;
		double spawnY = Math.floor(center.getY()) + direction.getOffsetY();
		double spawnZ = center.getZ() + direction.getOffsetZ() * SPAWN_HORIZONTAL_OFFSET;
		BlockPos targetPos = pointer.pos().offset(direction);
		BlockState targetState = serverWorld.getBlockState(targetPos);

		double yOffset;
		if (targetState.isIn(BlockTags.RAILS)) {
			yOffset = getRailShape(targetState).isAscending() ? ASCENDING_RAIL_Y_OFFSET : FLAT_RAIL_Y_OFFSET;
		} else {
			if (targetState.isAir() == false) {
				return fallbackBehavior.dispense(pointer, stack);
			}

			BlockState belowState = serverWorld.getBlockState(targetPos.down());
			if (belowState.isIn(BlockTags.RAILS) == false) {
				return fallbackBehavior.dispense(pointer, stack);
			}

			yOffset = (direction != Direction.DOWN && getRailShape(belowState).isAscending())
					? ASCENDING_BELOW_Y_OFFSET
					: FLAT_BELOW_Y_OFFSET;
		}

		AbstractMinecartEntity minecart = AbstractMinecartEntity.create(
				serverWorld,
				spawnX,
				spawnY + yOffset,
				spawnZ,
				minecartEntityType,
				SpawnReason.DISPENSER,
				stack,
				null
		);
		if (minecart == null) {
			return stack;
		}

		serverWorld.spawnEntity(minecart);
		stack.decrement(1);
		return stack;
	}

	private static RailShape getRailShape(BlockState state) {
		return state.getBlock() instanceof AbstractRailBlock railBlock
				? state.get(railBlock.getShapeProperty())
				: RailShape.NORTH_SOUTH;
	}

	@Override
	protected void playSound(BlockPointer pointer) {
		pointer.world().syncWorldEvent(DISPENSE_SOUND_EVENT, pointer.pos(), 0);
	}
}
