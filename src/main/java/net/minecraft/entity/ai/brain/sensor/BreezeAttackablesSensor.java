package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.BreezeEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Сенсор поиска ближайшей атакуемой цели для Бриза.
 * Расширяет базовый сенсор живых существ, добавляя поиск ближайшей атакуемой цели
 * среди не-творческих и не-зрительских игроков.
 */
public class BreezeAttackablesSensor extends NearestLivingEntitiesSensor<BreezeEntity> {

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.copyOf(Iterables.concat(
				super.getOutputMemoryModules(),
				List.of(MemoryModuleType.NEAREST_ATTACKABLE)
		));
	}

	@Override
	protected void sense(ServerWorld world, BreezeEntity entity) {
		super.sense(world, entity);
		entity.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.MOBS)
				.stream()
				.flatMap(Collection::stream)
				.filter(EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR)
				.filter(target -> Sensor.testAttackableTargetPredicate(world, entity, target))
				.findFirst()
				.ifPresentOrElse(
						target -> entity.getBrain().remember(MemoryModuleType.NEAREST_ATTACKABLE, target),
						() -> entity.getBrain().forget(MemoryModuleType.NEAREST_ATTACKABLE)
				);
	}
}
