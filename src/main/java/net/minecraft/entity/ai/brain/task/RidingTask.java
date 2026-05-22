package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;

import java.util.function.BiPredicate;

/**
 * Фабричный класс задачи мозга, прекращающей верховую езду при выполнении условий.
 * Останавливает езду если транспорт вышел за пределы дальности, умер или выполнено альтернативное условие.
 */
public class RidingTask {

	public static <E extends LivingEntity> Task<E> create(int range, BiPredicate<E, Entity> alternativeRideCondition) {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryOptional(MemoryModuleType.RIDE_TARGET)).apply(
						context, rideTarget -> (world, entity, time) -> {
							Entity vehicle = entity.getVehicle();
							Entity target = context.<Entity>getOptionalValue(rideTarget).orElse(null);

							if (vehicle == null && target == null) {
								return false;
							}

							Entity rideCandidate = vehicle == null ? target : vehicle;
							boolean shouldStopRiding = !canRideTarget(entity, rideCandidate, range)
									|| alternativeRideCondition.test((E) entity, rideCandidate);

							if (!shouldStopRiding) {
								return false;
							}

							entity.stopRiding();
							rideTarget.forget();
							return true;
						}
				)
		);
	}

	private static boolean canRideTarget(LivingEntity entity, Entity vehicle, int range) {
		return vehicle.isAlive()
				&& vehicle.isInRange(entity, range)
				&& vehicle.getEntityWorld() == entity.getEntityWorld();
	}
}
