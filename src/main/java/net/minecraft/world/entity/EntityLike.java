package net.minecraft.world.entity;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.stream.Stream;

/**
 * {@code EntityLike}.
 */
public interface EntityLike extends UniquelyIdentifiable {

	int getId();

	BlockPos getBlockPos();

	Box getBoundingBox();

	void setChangeListener(EntityChangeListener changeListener);

	Stream<? extends EntityLike> streamSelfAndPassengers();

	Stream<? extends EntityLike> streamPassengersAndSelf();

	void setRemoved(Entity.RemovalReason reason);

	boolean shouldSave();

	boolean isPlayer();
}
