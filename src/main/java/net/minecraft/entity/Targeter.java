package net.minecraft.entity;

import org.jspecify.annotations.Nullable;

/**
 * {@code Targeter}.
 */
public interface Targeter {

	@Nullable LivingEntity getTarget();
}
