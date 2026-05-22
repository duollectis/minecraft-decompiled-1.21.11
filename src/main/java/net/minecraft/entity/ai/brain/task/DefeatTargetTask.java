package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.world.rule.GameRules;

import java.util.function.BiPredicate;

/**
 * Фабричный класс задачи мозга, обрабатывающей победу над целью атаки.
 * Запускает празднование, запоминает позицию победы и сбрасывает цель атаки.
 */
public class DefeatTargetTask {

	public static Task<LivingEntity> create(
			int celebrationDuration,
			BiPredicate<LivingEntity, LivingEntity> predicate
	) {
		return TaskTriggerer.task(
				context -> context.group(
						context.queryMemoryValue(MemoryModuleType.ATTACK_TARGET),
						context.queryMemoryOptional(MemoryModuleType.ANGRY_AT),
						context.queryMemoryAbsent(MemoryModuleType.CELEBRATE_LOCATION),
						context.queryMemoryOptional(MemoryModuleType.DANCING)
				).apply(
						context,
						(attackTarget, angryAt, celebrateLocation, dancing) -> (world, entity, time) -> {
							LivingEntity defeated = context.getValue(attackTarget);
							if (!defeated.isDead()) {
								return false;
							}

							if (predicate.test(entity, defeated)) {
								dancing.remember(true, celebrationDuration);
							}

							celebrateLocation.remember(defeated.getBlockPos(), celebrationDuration);
							if (defeated.getType() != EntityType.PLAYER
									|| world.getGameRules().getValue(GameRules.FORGIVE_DEAD_PLAYERS)) {
								attackTarget.forget();
								angryAt.forget();
							}

							return true;
						}
				)
		);
	}
}
