package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;

import java.util.Set;

/**
 * Сенсор, отслеживающий последний источник урона сущности.
 * Записывает в память {@code HURT_BY} и {@code HURT_BY_ENTITY} данные об атакующем,
 * а также очищает память об атакующем, если тот мёртв или находится в другом измерении.
 */
public class HurtBySensor extends Sensor<LivingEntity> {

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY);
	}

	@Override
	protected void sense(ServerWorld world, LivingEntity entity) {
		Brain<?> brain = entity.getBrain();
		DamageSource damageSource = entity.getRecentDamageSource();

		if (damageSource == null) {
			brain.forget(MemoryModuleType.HURT_BY);
		} else {
			brain.remember(MemoryModuleType.HURT_BY, damageSource);

			if (damageSource.getAttacker() instanceof LivingEntity attacker) {
				brain.remember(MemoryModuleType.HURT_BY_ENTITY, attacker);
			}
		}

		brain.getOptionalRegisteredMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(hurtByEntity -> {
			if (hurtByEntity.isAlive() && hurtByEntity.getEntityWorld() == world) {
				return;
			}

			brain.forget(MemoryModuleType.HURT_BY_ENTITY);
		});
	}
}
