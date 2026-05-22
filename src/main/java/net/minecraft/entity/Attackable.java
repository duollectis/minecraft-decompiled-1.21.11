package net.minecraft.entity;

import org.jspecify.annotations.Nullable;

/**
 * Маркерный интерфейс для сущностей, которые могут быть атакованы
 * и отслеживают своего последнего атакующего.
 */
public interface Attackable {

	@Nullable LivingEntity getLastAttacker();
}
