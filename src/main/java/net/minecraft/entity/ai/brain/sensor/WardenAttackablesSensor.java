package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Сенсор поиска ближайшей атакуемой цели для Хранителя.
 * Приоритет отдаётся игрокам — если игрок не найден, выбирается любая другая цель,
 * прошедшая проверку {@code isValidTarget}.
 */
public class WardenAttackablesSensor extends NearestLivingEntitiesSensor<WardenEntity> {

	@Override
	public Set<MemoryModuleType<?>> getOutputMemoryModules() {
		return ImmutableSet.copyOf(Iterables.concat(
				super.getOutputMemoryModules(),
				List.of(MemoryModuleType.NEAREST_ATTACKABLE)
		));
	}

	@Override
	protected void sense(ServerWorld world, WardenEntity entity) {
		super.sense(world, entity);
		findNearestTarget(entity, target -> target.getType() == EntityType.PLAYER)
				.or(() -> findNearestTarget(entity, target -> target.getType() != EntityType.PLAYER))
				.ifPresentOrElse(
						target -> entity.getBrain().remember(MemoryModuleType.NEAREST_ATTACKABLE, target),
						() -> entity.getBrain().forget(MemoryModuleType.NEAREST_ATTACKABLE)
				);
	}

	private static Optional<LivingEntity> findNearestTarget(WardenEntity warden, Predicate<LivingEntity> filter) {
		return warden.getBrain()
				.getOptionalRegisteredMemory(MemoryModuleType.MOBS)
				.stream()
				.flatMap(Collection::stream)
				.filter(warden::isValidTarget)
				.filter(filter)
				.findFirst();
	}
}
