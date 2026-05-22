package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Фабричный класс задачи мозга, направляющей взгляд моба на цель атаки и выполняющей страфинг назад.
 * Активируется только если цель видима и находится в пределах заданного расстояния.
 */
public class LookTowardsAttackTargetTask {

	public static SingleTickTask<MobEntity> create(int distance, float forwardMovement) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
						                  context.queryMemoryValue(MemoryModuleType.ATTACK_TARGET),
						                  context.queryMemoryValue(MemoryModuleType.VISIBLE_MOBS)
				                  )
				                  .apply(
						                  context,
						                  (walkTarget, lookTarget, attackTarget, visibleMobs) -> (world, entity, time) -> {
							                  LivingEntity target = context.getValue(attackTarget);
							                  boolean inRangeAndVisible = target.isInRange(entity, distance)
									                  && context.<LivingTargetCache>getValue(visibleMobs).contains(target);

							                  if (!inRangeAndVisible) {
								                  return false;
							                  }

							                  lookTarget.remember(new EntityLookTarget(target, true));
							                  entity.getMoveControl().strafeTo(-forwardMovement, 0.0F);
							                  entity.setYaw(MathHelper.clampAngle(entity.getYaw(), entity.headYaw, 0.0F));
							                  return true;
						                  }
				                  )
		);
	}
}
