package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;

import java.util.Optional;

/**
 * Фабричный класс задачи мозга, ищущей ближайшую сущность заданного типа для взаимодействия.
 * Устанавливает найденную сущность как цель взгляда и цель взаимодействия.
 */
public class FindInteractionTargetTask {

	public static Task<LivingEntity> create(EntityType<?> type, int maxDistance) {
		int maxDistanceSq = maxDistance * maxDistance;

		return TaskTriggerer.task(
				context -> context.group(
						context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
						context.queryMemoryAbsent(MemoryModuleType.INTERACTION_TARGET),
						context.queryMemoryValue(MemoryModuleType.VISIBLE_MOBS)
				).apply(
						context,
						(lookTarget, interactionTarget, visibleMobs) -> (world, entity, time) -> {
							Optional<LivingEntity> found = context.<LivingTargetCache>getValue(visibleMobs)
									.findFirst(
											target -> target.squaredDistanceTo(entity) <= maxDistanceSq
													&& type.equals(target.getType())
									);

							if (found.isEmpty()) {
								return false;
							}

							LivingEntity target = found.get();
							interactionTarget.remember(target);
							lookTarget.remember(new EntityLookTarget(target, true));
							return true;
						}
				)
		);
	}
}
