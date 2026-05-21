package net.minecraft.entity;

import org.jspecify.annotations.Nullable;

/**
 * {@code Attackable}.
 */
public interface Attackable {

	@Nullable LivingEntity getLastAttacker();
}
