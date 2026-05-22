package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.MobEntity;

import java.util.Optional;
import java.util.function.Function;

/**
 * Фабричный класс задачи мозга, управляющей сближением с целью атаки для дальнего боя.
 * Если цель видима и находится в радиусе атаки — сбрасывает цель ходьбы; иначе — идёт к ней.
 */
public class RangedApproachTask {

	private static final int WEAPON_REACH_REDUCTION = 1;

	public static Task<MobEntity> create(float speed) {
		return create(entity -> speed);
	}

	public static Task<MobEntity> create(Function<LivingEntity, Float> speed) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryOptional(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
						                  context.queryMemoryValue(MemoryModuleType.ATTACK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.VISIBLE_MOBS)
				                  )
				                  .apply(
						                  context,
						                  (walkTarget, lookTarget, attackTarget, visibleMobs) -> (world, entity, time) -> {
							                  LivingEntity target = context.getValue(attackTarget);
							                  Optional<LivingTargetCache> mobs = context.getOptionalValue(visibleMobs);
							                  boolean inRangeAndVisible = mobs.isPresent()
									                  && mobs.get().contains(target)
									                  && TargetUtil.isTargetWithinAttackRange(entity, target, WEAPON_REACH_REDUCTION);

							                  if (inRangeAndVisible) {
								                  walkTarget.forget();
							                  } else {
								                  lookTarget.remember(new EntityLookTarget(target, true));
								                  walkTarget.remember(new WalkTarget(
										                  new EntityLookTarget(target, false),
										                  speed.apply(entity),
										                  0
								                  ));
							                  }

							                  return true;
						                  }
				                  )
		);
	}
}
