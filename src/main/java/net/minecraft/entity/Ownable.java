package net.minecraft.entity;

import org.jspecify.annotations.Nullable;

/**
 * {@code Ownable}.
 */
public interface Ownable {

	@Nullable Entity getOwner();
}
