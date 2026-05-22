package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Фабричный класс задачи мозга, направляющей взгляд сущности на источник помехи или цель рёва.
 * Приоритет: цель рёва ({@code ROAR_TARGET}) → место помехи ({@code DISTURBANCE_LOCATION}).
 */
public class LookAtDisturbanceTask {

	public static Task<LivingEntity> create() {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.DISTURBANCE_LOCATION),
						                  context.queryMemoryOptional(MemoryModuleType.ROAR_TARGET),
						                  context.queryMemoryAbsent(MemoryModuleType.ATTACK_TARGET)
				                  )
				                  .apply(
						                  context,
						                  (lookTarget, disturbanceLocation, roarTarget, attackTarget) -> (world, entity, time) -> {
							                  Optional<BlockPos> target = context.<LivingEntity>getOptionalValue(roarTarget)
							                                                     .map(Entity::getBlockPos)
							                                                     .or(() -> context.getOptionalValue(disturbanceLocation));

							                  if (target.isEmpty()) {
								                  return false;
							                  }

							                  lookTarget.remember(new BlockPosLookTarget(target.get()));
							                  return true;
						                  }
				                  )
		);
	}
}
