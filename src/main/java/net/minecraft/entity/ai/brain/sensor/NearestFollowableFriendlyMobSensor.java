package net.minecraft.entity.ai.brain.sensor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.registry.tag.EntityTypeTags;

import java.util.Optional;

/**
 * Сенсор поиска ближайшего взрослого дружественного моба, за которым можно следовать.
 * Фильтрует по тегу {@code FOLLOWABLE_FRIENDLY_MOBS} вместо точного совпадения типа.
 */
public class NearestFollowableFriendlyMobSensor extends NearestVisibleAdultSensor {

	@Override
	protected void find(LivingEntity entity, LivingTargetCache targetCache) {
		Optional<LivingEntity> nearest = targetCache.findFirst(
				mob -> mob.getType().isIn(EntityTypeTags.FOLLOWABLE_FRIENDLY_MOBS) && !mob.isBaby()
		);
		entity.getBrain().remember(MemoryModuleType.NEAREST_VISIBLE_ADULT, nearest);
	}
}
