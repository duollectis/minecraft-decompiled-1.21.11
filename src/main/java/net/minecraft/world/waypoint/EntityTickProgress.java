package net.minecraft.world.waypoint;

import net.minecraft.entity.Entity;

@FunctionalInterface
/**
 * {@code EntityTickProgress}.
 */
public interface EntityTickProgress {

	float getTickProgress(Entity entity);
}
