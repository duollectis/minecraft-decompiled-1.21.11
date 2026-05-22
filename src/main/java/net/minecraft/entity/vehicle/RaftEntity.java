package net.minecraft.entity.vehicle;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import java.util.function.Supplier;

/**
 * Плот из бамбука. Пассажир крепится выше, чем в обычной лодке — на 8/9 высоты корпуса.
 */
public class RaftEntity extends AbstractBoatEntity {

	/** Коэффициент высоты крепления пассажира (8/9 ≈ 0.8888889). */
	private static final float PASSENGER_ATTACHMENT_HEIGHT_FACTOR = 0.8888889F;

	public RaftEntity(EntityType<? extends RaftEntity> entityType, World world, Supplier<Item> supplier) {
		super(entityType, world, supplier);
	}

	@Override
	protected double getPassengerAttachmentY(EntityDimensions dimensions) {
		return dimensions.height() * PASSENGER_ATTACHMENT_HEIGHT_FACTOR;
	}
}
