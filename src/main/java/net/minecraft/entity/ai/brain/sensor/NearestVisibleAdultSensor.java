package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;
import java.util.Set;

/**
 * {@code NearestVisibleAdultSensor}.
 */
public class NearestVisibleAdultSensor extends Sensor<LivingEntity> {

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.VISIBLE_MOBS);
	}

	@Override
	protected void sense(ServerWorld world, LivingEntity entity) {
		entity
				.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS)
				.ifPresent(targetCache -> this.find(entity, targetCache));
	}

	/**
	 * Find.
	 *
	 * @param entity entity
	 * @param targetCache target cache
	 */
	protected void find(LivingEntity entity, LivingTargetCache targetCache) {
		Optional<LivingEntity>
				optional =
				targetCache.findFirst(target -> target.getType() == entity.getType() && !target.isBaby());
		entity.getBrain().remember(MemoryModuleType.NEAREST_VISIBLE_ADULT, optional);
	}
}
