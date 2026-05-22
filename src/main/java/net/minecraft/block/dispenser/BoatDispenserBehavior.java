package net.minecraft.block.dispenser;

import net.minecraft.block.DispenserBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Поведение диспенсера для лодок: спавнит лодку на воде перед диспенсером.
 * Если перед диспенсером нет воды — делегирует стандартному выбросу предмета.
 */
public class BoatDispenserBehavior extends ItemDispenserBehavior {

	/** Смещение Y при спавне лодки на поверхности воды. */
	private static final double WATER_SURFACE_Y_OFFSET = 1.0;

	/** Смещение Y при спавне лодки над водой (блок воздуха над водой). */
	private static final double ABOVE_WATER_Y_OFFSET = 0.0;

	/** Вертикальное смещение позиции спавна от центра диспенсера. */
	private static final float SPAWN_Y_OFFSET = 1.125F;

	/** Код мирового события «диспенсер сработал» (звук). */
	private static final int DISPENSE_SOUND_EVENT = 1000;

	private final ItemDispenserBehavior fallbackBehavior = new ItemDispenserBehavior();
	private final EntityType<? extends AbstractBoatEntity> boatType;

	public BoatDispenserBehavior(EntityType<? extends AbstractBoatEntity> boatType) {
		this.boatType = boatType;
	}

	@Override
	public ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
		Direction direction = pointer.state().get(DispenserBlock.FACING);
		ServerWorld serverWorld = pointer.world();
		Vec3d center = pointer.centerPos();
		double halfWidth = 0.5625 + boatType.getWidth() / 2.0;
		double spawnX = center.getX() + direction.getOffsetX() * halfWidth;
		double spawnY = center.getY() + direction.getOffsetY() * SPAWN_Y_OFFSET;
		double spawnZ = center.getZ() + direction.getOffsetZ() * halfWidth;
		BlockPos targetPos = pointer.pos().offset(direction);

		double yOffset;
		if (serverWorld.getFluidState(targetPos).isIn(FluidTags.WATER)) {
			yOffset = WATER_SURFACE_Y_OFFSET;
		} else {
			if (serverWorld.getBlockState(targetPos).isAir() == false
					|| serverWorld.getFluidState(targetPos.down()).isIn(FluidTags.WATER) == false) {
				return fallbackBehavior.dispense(pointer, stack);
			}

			yOffset = ABOVE_WATER_Y_OFFSET;
		}

		AbstractBoatEntity boat = boatType.create(serverWorld, SpawnReason.DISPENSER);
		if (boat == null) {
			return stack;
		}

		boat.initPosition(spawnX, spawnY + yOffset, spawnZ);
		EntityType.<AbstractBoatEntity>copier(serverWorld, stack, null).accept(boat);
		boat.setYaw(direction.getPositiveHorizontalDegrees());
		serverWorld.spawnEntity(boat);
		stack.decrement(1);
		return stack;
	}

	@Override
	protected void playSound(BlockPointer pointer) {
		pointer.world().syncWorldEvent(DISPENSE_SOUND_EVENT, pointer.pos(), 0);
	}
}
