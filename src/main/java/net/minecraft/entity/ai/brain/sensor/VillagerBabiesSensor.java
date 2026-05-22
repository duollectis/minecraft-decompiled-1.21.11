package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Set;

/**
 * Сенсор обнаружения видимых детёнышей жителей деревни.
 * Фильтрует кэш видимых мобов по типу {@code VILLAGER} и признаку {@code isBaby}.
 */
public class VillagerBabiesSensor extends Sensor<LivingEntity> {

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES);
	}

	@Override
	protected void sense(ServerWorld world, LivingEntity entity) {
		entity.getBrain().remember(MemoryModuleType.VISIBLE_VILLAGER_BABIES, getVisibleVillagerBabies(entity));
	}

	private List<LivingEntity> getVisibleVillagerBabies(LivingEntity entity) {
		return ImmutableList.copyOf(getVisibleMobs(entity).iterate(this::isVillagerBaby));
	}

	private boolean isVillagerBaby(LivingEntity entity) {
		return entity.getType() == EntityType.VILLAGER && entity.isBaby();
	}

	private LivingTargetCache getVisibleMobs(LivingEntity entity) {
		return entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS)
				.orElse(LivingTargetCache.empty());
	}
}
