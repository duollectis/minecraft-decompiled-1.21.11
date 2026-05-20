package net.minecraft.world.waypoint;

import net.minecraft.entity.Entity;

@FunctionalInterface
public interface EntityTickProgress {
   float getTickProgress(Entity entity);
}
