package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

/**
 * {@code UpdateAttackTargetTask}.
 */
public class UpdateAttackTargetTask {

	/**
	 * Create.
	 *
	 * @param targetGetter target getter
	 *
	 * @return Task — результат операции
	 */
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
							                  else {
								                  Optional<? extends LivingEntity>
										                  optional =
										                  targetGetter.get(world, (E) entity);
								                  if (optional.isEmpty()) {
									                  return false;
								                  }
								                  else {
									                  LivingEntity livingEntity = optional.get();
									                  if (!entity.canTarget(livingEntity)) {
										                  return false;
									                  }
									                  else {
										                  attackTarget.remember(livingEntity);
										                  cantReachWalkTargetSince.forget();
										                  return true;
									                  }
								                  }
							                  }
						                  }
				                  )
		);
	}

	@FunctionalInterface
	/**
	 * {@code StartCondition}.
	 */
	public interface StartCondition<E> {

		boolean test(ServerWorld world, E entity);
	}

	@FunctionalInterface
	/**
	 * {@code TargetGetter}.
	 */
	public interface TargetGetter<E> {

		Optional<? extends LivingEntity> get(ServerWorld world, E entity);
	}
}
