package net.minecraft.entity.vehicle;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import java.util.function.Supplier;

/**
 * {@code RaftEntity}.
 */
public class RaftEntity extends AbstractBoatEntity {

	public RaftEntity(EntityType<? extends RaftEntity> entityType, World world, Supplier<Item> supplier) {
		super(entityType, world, supplier);
	}

	@Override
	protected double getPassengerAttachmentY(EntityDimensions dimensions) {
		return dimensions.height() * 0.8888889F;
	}
}
