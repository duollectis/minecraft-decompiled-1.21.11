package net.minecraft.entity.vehicle;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import java.util.function.Supplier;

/**
 * Лодка со встроенным сундуком. Пассажир крепится на высоте 1/3 от высоты корпуса.
 */
public class ChestBoatEntity extends AbstractChestBoatEntity {

	public ChestBoatEntity(EntityType<? extends ChestBoatEntity> entityType, World world, Supplier<Item> supplier) {
		super(entityType, world, supplier);
	}

	@Override
	protected double getPassengerAttachmentY(EntityDimensions dimensions) {
		return dimensions.height() / 3.0F;
	}
}
