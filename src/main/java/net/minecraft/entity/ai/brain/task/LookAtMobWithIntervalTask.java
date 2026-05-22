package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Устаревший фабричный класс задачи мозга, периодически направляющей взгляд на ближайшего видимого моба.
 * Используйте {@link LookAtMobTask} вместо этого класса.
 */
@Deprecated
public class LookAtMobWithIntervalTask {

	public static Task<LivingEntity> follow(float maxDistance, UniformIntProvider interval) {
		return follow(maxDistance, interval, entity -> true);
	}

	public static Task<LivingEntity> follow(EntityType<?> type, float maxDistance, UniformIntProvider interval) {
		return follow(maxDistance, interval, entity -> type.equals(entity.getType()));
	}

	private static Task<LivingEntity> follow(
			float maxDistance,
			UniformIntProvider interval,
			Predicate<LivingEntity> predicate
	) {
		float maxDistanceSq = maxDistance * maxDistance;
		Interval intervalTracker = new Interval(interval);
		return TaskTriggerer.task(
				context -> context
						.group(
								context.queryMemoryAbsent(MemoryModuleType.LOOK_TARGET),
								context.queryMemoryValue(MemoryModuleType.VISIBLE_MOBS)
						)
						.apply(
								context,
								(lookTarget, visibleMobs) -> (world, entity, time) -> {
									Optional<LivingEntity> found = context.<LivingTargetCache>getValue(visibleMobs)
									                                      .findFirst(predicate.and(
											                                      other -> other.squaredDistanceTo(entity) <= maxDistanceSq
									                                      ));

									if (found.isEmpty()) {
										return false;
									}

									if (!intervalTracker.shouldRun(world.random)) {
										return false;
									}

									lookTarget.remember(new EntityLookTarget(found.get(), true));
									return true;
								}
						)
		);
	}

	public static final class Interval {

		private final UniformIntProvider interval;
		private int remainingTicks;

		public Interval(UniformIntProvider interval) {
			if (interval.getMin() <= 1) {
				throw new IllegalArgumentException();
			}

			this.interval = interval;
		}

		public boolean shouldRun(Random random) {
			if (remainingTicks == 0) {
				remainingTicks = interval.get(random) - 1;
				return false;
			}

			return --remainingTicks == 0;
		}
	}
}
