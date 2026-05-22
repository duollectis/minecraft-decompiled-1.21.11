package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Фабричный класс задачи мозга, ищущей ближайшую сущность заданного типа в кэше видимых мобов
 * и устанавливающей её как цель взгляда и ходьбы.
 */
public class FindEntityTask {

	public static <T extends LivingEntity> Task<LivingEntity> create(
			EntityType<? extends T> type,
			int maxDistance,
			MemoryModuleType<T> targetModule,
			float speed,
			int completionRange
	) {
		return create(type, maxDistance, entity -> true, entity -> true, targetModule, speed, completionRange);
	}

	public static <E extends LivingEntity, T extends LivingEntity> Task<E> create(
			EntityType<? extends T> type,
			int maxDistance,
			Predicate<E> entityPredicate,
			Predicate<T> targetPredicate,
			MemoryModuleType<T> targetModule,
			float speed,
			int completionRange
	) {
		int maxDistanceSq = maxDistance * maxDistance;
		Predicate<LivingEntity> typedPredicate = e -> type.equals(e.getType()) && targetPredicate.test((T) e);

		return TaskTriggerer.task(
				context -> context.group(
						context.queryMemoryOptional(targetModule),
						context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
						context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET),
						context.queryMemoryValue(MemoryModuleType.VISIBLE_MOBS)
				).apply(
						context,
						(targetValue, lookTarget, walkTarget, visibleMobs) -> (world, entity, time) -> {
							LivingTargetCache cache = context.getValue(visibleMobs);

							if (!entityPredicate.test((E) entity) || !cache.anyMatch(typedPredicate)) {
								return false;
							}

							cache.findFirst(
									target -> target.squaredDistanceTo(entity) <= maxDistanceSq
											&& typedPredicate.test(target)
							).ifPresent(target -> {
								targetValue.remember((T) target);
								lookTarget.remember(new EntityLookTarget(target, true));
								walkTarget.remember(new WalkTarget(new EntityLookTarget(target, false), speed, completionRange));
							});

							return true;
						}
				)
		);
	}
}
