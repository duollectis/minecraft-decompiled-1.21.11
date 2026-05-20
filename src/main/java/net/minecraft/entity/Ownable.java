package net.minecraft.entity;

import org.jspecify.annotations.Nullable;

public interface Ownable {
   @Nullable Entity getOwner();
}
