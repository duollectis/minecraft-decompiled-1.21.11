package net.minecraft.entity;

import org.jspecify.annotations.Nullable;

/**
 * Маркерный интерфейс для сущностей, способных выбирать цель атаки.
 */
public interface Targeter {

	@Nullable LivingEntity getTarget();
}
