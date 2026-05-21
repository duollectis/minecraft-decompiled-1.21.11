package net.minecraft.world.entity;

import java.util.UUID;

/**
 * {@code UniquelyIdentifiable}.
 */
public interface UniquelyIdentifiable {

	UUID getUuid();

	boolean isRemoved();
}
