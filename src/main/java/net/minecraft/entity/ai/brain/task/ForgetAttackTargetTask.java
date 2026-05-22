package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

/**
 * Фабричный класс задачи мозга, сбрасывающей цель атаки при выполнении условий забывания.
 * Поддерживает настраиваемые условия и колбэк при сбросе цели.
 */
public class ForgetAttackTargetTask {

	private static final int REMEMBER_TIME = 200;

	public static <E extends MobEntity> Task<E> create(ForgetAttackTargetTask.ForgetCallback<E> callback) {
		return create((world, target) -> false, callback, true);
	}

	public static <E extends MobEntity> Task<E> create(ForgetAttackTargetTask.AlternativeCondition condition) {
		return create(condition, (world, entity, target) -> {}, true);
	}

	public static <E extends MobEntity> Task<E> create() {
		return create((world, target) -> false, (world, entity, target) -> {}, true);
	}

	public static <E extends MobEntity> Task<E> create(
			ForgetAttackTargetTask.AlternativeCondition condition,
			ForgetAttackTargetTask.ForgetCallback<E> callback,
			boolean shouldForgetIfTargetUnreachable
	) {
		return TaskTriggerer.task(
				context -> context.group(
						context.queryMemoryValue(MemoryModuleType.ATTACK_TARGET),
						context.queryMemoryOptional(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
				).apply(
						context,
						(attackTarget, cantReachWalkTargetSince) -> (world, entity, time) -> {
							LivingEntity target = context.getValue(attackTarget);
							boolean targetReachable = !shouldForgetIfTargetUnreachable
									|| !cannotReachTarget(entity, context.getOptionalValue(cantReachWalkTargetSince));
							boolean shouldKeep = entity.canTarget(target)
									&& targetReachable
									&& target.isAlive()
									&& target.getEntityWorld() == entity.getEntityWorld()
									&& !condition.test(world, target);

							if (shouldKeep) {
								return true;
							}

							callback.accept(world, (E) entity, target);
							attackTarget.forget();
							return true;
						}
				)
		);
	}

	private static boolean cannotReachTarget(LivingEntity entity, Optional<Long> lastReachTime) {
		return lastReachTime.isPresent()
				&& entity.getEntityWorld().getTime() - lastReachTime.get() > REMEMBER_TIME;
	}

	@FunctionalInterface
	public interface AlternativeCondition {

		boolean test(ServerWorld world, LivingEntity target);
	}

	@FunctionalInterface
	public interface ForgetCallback<E> {

		void accept(ServerWorld world, E entity, LivingEntity target);
	}
}
