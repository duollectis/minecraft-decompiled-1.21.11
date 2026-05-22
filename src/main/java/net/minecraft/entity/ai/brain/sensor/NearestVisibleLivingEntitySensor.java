package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;
import java.util.Set;

/**
 * Абстрактный сенсор поиска ближайшей видимой живой сущности по заданному критерию.
 * Подклассы реализуют {@link #matches} для фильтрации целей и {@link #getOutputMemoryModule}
 * для указания модуля памяти, в который записывается результат.
 */
public abstract class NearestVisibleLivingEntitySensor extends Sensor<LivingEntity> {

	protected abstract boolean matches(ServerWorld world, LivingEntity entity, LivingEntity target);

	protected abstract MemoryModuleType<LivingEntity> getOutputMemoryModule();

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(getOutputMemoryModule());
	}

	@Override
	protected void sense(ServerWorld world, LivingEntity entity) {
		entity.getBrain().remember(getOutputMemoryModule(), getNearestVisibleLivingEntity(world, entity));
	}

	private Optional<LivingEntity> getNearestVisibleLivingEntity(ServerWorld world, LivingEntity entity) {
		return getVisibleLivingEntities(entity)
				.flatMap(cache -> cache.findFirst(target -> matches(world, entity, target)));
	}

	protected Optional<LivingTargetCache> getVisibleLivingEntities(LivingEntity entity) {
		return entity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS);
	}
}
