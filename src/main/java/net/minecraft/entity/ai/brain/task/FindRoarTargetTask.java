package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.WardenEntity;

import java.util.Optional;
import java.util.function.Function;

/**
 * Фабричный класс задачи мозга, ищущей цель для рёва Вардена.
 * Устанавливает найденную сущность в память {@code ROAR_TARGET}, если она является допустимой целью.
 */
public class FindRoarTargetTask {

	public static <E extends WardenEntity> Task<E> create(Function<E, Optional<? extends LivingEntity>> targetFinder) {
		return TaskTriggerer.task(
				context -> context.group(
						context.queryMemoryAbsent(MemoryModuleType.ROAR_TARGET),
						context.queryMemoryAbsent(MemoryModuleType.ATTACK_TARGET),
						context.queryMemoryOptional(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
				).apply(
						context,
						(roarTarget, attackTarget, cantReachWalkTargetSince) -> (world, entity, time) -> {
							Optional<? extends LivingEntity> found = targetFinder.apply((E) entity);

							if (found.filter(entity::isValidTarget).isEmpty()) {
								return false;
							}

							roarTarget.remember(found.get());
							cantReachWalkTargetSince.forget();
							return true;
						}
				)
		);
	}
}
