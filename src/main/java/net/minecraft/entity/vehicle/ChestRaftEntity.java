package net.minecraft.entity.vehicle;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import java.util.function.Supplier;

/**
 * Плот из бамбука со встроенным сундуком. Пассажир крепится на высоте 8/9 от высоты корпуса.
 */
public class ChestRaftEntity extends AbstractChestBoatEntity {

	/** Коэффициент высоты крепления пассажира (8/9 ≈ 0.8888889). */
	private static final float PASSENGER_ATTACHMENT_HEIGHT_FACTOR = 0.8888889F;

	public ChestRaftEntity(EntityType<? extends ChestRaftEntity> entityType, World world, Supplier<Item> supplier) {
		super(entityType, world, supplier);
	}

	@Override
	protected double getPassengerAttachmentY(EntityDimensions dimensions) {
		return dimensions.height() * PASSENGER_ATTACHMENT_HEIGHT_FACTOR;
	}
}
