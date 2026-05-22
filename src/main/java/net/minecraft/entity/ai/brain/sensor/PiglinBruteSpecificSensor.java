package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сенсор специфической логики пиглин-брута.
 * Ищет ближайшего видимого врага (иссушитель или скелет-иссушитель) и
 * собирает список всех взрослых пиглинов поблизости.
 */
public class PiglinBruteSpecificSensor extends Sensor<LivingEntity> {

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.of(
				MemoryModuleType.VISIBLE_MOBS,
				MemoryModuleType.NEAREST_VISIBLE_NEMESIS,
				MemoryModuleType.NEARBY_ADULT_PIGLINS
		);
	}

	@Override
	protected void sense(ServerWorld world, LivingEntity entity) {
		Brain<?> brain = entity.getBrain();
		LivingTargetCache visibleMobs = brain
				.getOptionalRegisteredMemory(MemoryModuleType.VISIBLE_MOBS)
				.orElse(LivingTargetCache.empty());

		Optional<MobEntity> nearestNemesis = visibleMobs
				.findFirst(mob -> mob instanceof WitherSkeletonEntity || mob instanceof WitherEntity)
				.map(MobEntity.class::cast);

		List<AbstractPiglinEntity> nearbyPiglins = new ArrayList<>();

		for (LivingEntity mob : brain.getOptionalRegisteredMemory(MemoryModuleType.MOBS).orElse(ImmutableList.of())) {
			if (mob instanceof AbstractPiglinEntity piglin && piglin.isAdult()) {
				nearbyPiglins.add(piglin);
			}
		}

		brain.remember(MemoryModuleType.NEAREST_VISIBLE_NEMESIS, nearestNemesis);
		brain.remember(MemoryModuleType.NEARBY_ADULT_PIGLINS, nearbyPiglins);
	}
}
