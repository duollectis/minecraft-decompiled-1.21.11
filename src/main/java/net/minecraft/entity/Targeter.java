package net.minecraft.entity;

import org.jspecify.annotations.Nullable;

public interface Targeter {
   @Nullable LivingEntity getTarget();
}
