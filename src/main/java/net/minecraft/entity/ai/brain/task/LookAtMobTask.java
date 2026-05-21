package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * {@code LookAtMobTask}.
 */
public class LookAtMobTask {

	/**
	 * Create.
	 *
	 * @param spawnGroup spawn group
	 * @param maxDistance max distance
	 *
	 * @return Task — результат операции
	 */
	public static Task<LivingEntity> create(SpawnGroup spawnGroup, float maxDistance) {
		return create(entity -> spawnGroup.equals(entity.getType().getSpawnGroup()), maxDistance);
	}

	/**
	 * Create.
	 *
	 * @param type type
	 * @param maxDistance max distance
	 *
	 * @return SingleTickTask — результат операции
	 */
	public static SingleTickTask<LivingEntity> create(EntityType<?> type, float maxDistance) {
		return create(entity -> type.equals(entity.getType()), maxDistance);
	}

	/**
	 * Create.
	 *
	 * @param maxDistance max distance
	 *
	 * @return SingleTickTask — результат операции
	 */
	public static SingleTickTask<LivingEntity> create(float maxDistance) {
		return create(entity -> true, maxDistance);
	}

	/**
	 * Create.
	 *
	 * @param predicate predicate
	 * @param maxDistance max distance
	 *
	 * @return SingleTickTask — результат операции
	 */
	public static SingleTickTask<LivingEntity> create(Predicate<LivingEntity> predicate, float maxDistance) {
		float f = maxDistance * maxDistance;
		return TaskTriggerer.task(
				context -> context
						.group(
								context.queryMemoryAbsent(MemoryModuleType.LOOK_TARGET),
								context.queryMemoryValue(MemoryModuleType.VISIBLE_MOBS)
						)
						.apply(
								context,
								(lookTarget, visibleMobs) -> (world, entity, time) -> {
									Optional<LivingEntity> optional = context.<LivingTargetCache>getValue(visibleMobs)
									                                         .findFirst(predicate.and(target ->
											                                         target.squaredDistanceTo(entity)
													                                         <= f
													                                         && !entity.hasPassenger(
													                                         target)));
									if (optional.isEmpty()) {
										return false;
									}
									else {
										lookTarget.remember(new EntityLookTarget(optional.get(), true));
										return true;
									}
								}
						)
		);
	}
}
