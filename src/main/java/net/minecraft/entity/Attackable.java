package net.minecraft.entity;

import org.jspecify.annotations.Nullable;

public interface Attackable {
   @Nullable LivingEntity getLastAttacker();
}
