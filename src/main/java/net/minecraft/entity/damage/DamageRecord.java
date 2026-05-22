package net.minecraft.entity.damage;

import org.jspecify.annotations.Nullable;

/**
 * Запись об одном событии урона: источник, количество, место падения и дистанция падения.
 * Используется {@link DamageTracker} для формирования сообщения о смерти.
 */
public record DamageRecord(
	DamageSource damageSource,
	float damage,
	@Nullable FallLocation fallLocation,
	float fallDistance
) {
}
