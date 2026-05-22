package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

/**
 * Фабричный класс задачи мозга, устанавливающей цель атаки из внешнего поставщика.
 * Поддерживает опциональное условие запуска; при успехе сбрасывает таймер недостижимости цели.
 */
public class UpdateAttackTargetTask {

	public static <E extends MobEntity> Task<E> create(UpdateAttackTargetTask.TargetGetter<E> targetGetter) {
		return create((world, entity) -> true, targetGetter);
	}

	public static <E extends MobEntity> Task<E> create(
			UpdateAttackTargetTask.StartCondition<E> condition,
			UpdateAttackTargetTask.TargetGetter<E> targetGetter
	) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryAbsent(MemoryModuleType.ATTACK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
				                  )
				                  .apply(
						                  context,
						                  (attackTarget, cantReachWalkTargetSince) -> (world, entity, time) -> {
							                  if (!condition.test(world, (E) entity)) {
								                  return false;
							                  }

							                  Optional<? extends LivingEntity> targetOpt = targetGetter.get(world, (E) entity);

							                  if (targetOpt.isEmpty()) {
								                  return false;
							                  }

							                  LivingEntity newTarget = targetOpt.get();

							                  if (!entity.canTarget(newTarget)) {
								                  return false;
							                  }

							                  attackTarget.remember(newTarget);
							                  cantReachWalkTargetSince.forget();
							                  return true;
						                  }
				                  )
		);
	}

	@FunctionalInterface
	public interface StartCondition<E> {

		boolean test(ServerWorld world, E entity);
	}

	@FunctionalInterface
	public interface TargetGetter<E> {

		Optional<? extends LivingEntity> get(ServerWorld world, E entity);
	}
}
