package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Базовый сенсор обнаружения ближайших живых существ в радиусе атрибута {@code FOLLOW_RANGE}.
 * Записывает в память {@code MOBS} полный список и {@code VISIBLE_MOBS} — кэш видимости.
 */
public class NearestLivingEntitiesSensor<T extends LivingEntity> extends Sensor<T> {

	@Override
	protected void sense(ServerWorld world, T entity) {
		double followRange = entity.getAttributeValue(EntityAttributes.FOLLOW_RANGE);
		Box box = entity.getBoundingBox().expand(followRange, followRange, followRange);
		List<LivingEntity> entities = world.getEntitiesByClass(
				LivingEntity.class,
				box,
				e -> e != entity && e.isAlive()
		);
		entities.sort(Comparator.comparingDouble(entity::squaredDistanceTo));

		Brain<?> brain = entity.getBrain();
		brain.remember(MemoryModuleType.MOBS, entities);
		brain.remember(MemoryModuleType.VISIBLE_MOBS, new LivingTargetCache(world, entity, entities));
	}

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(MemoryModuleType.MOBS, MemoryModuleType.VISIBLE_MOBS);
	}
}
