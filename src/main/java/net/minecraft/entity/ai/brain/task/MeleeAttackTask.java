package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Hand;

import java.util.function.Predicate;

/**
 * Фабричный класс задачи мозга для атаки в ближнем бою.
 * Атака выполняется только если цель видима, в радиусе удара и моб не держит дальнобойное оружие.
 */
public class MeleeAttackTask {

	public static <T extends MobEntity> SingleTickTask<T> create(int cooldown) {
		return create(target -> true, cooldown);
	}

	public static <T extends MobEntity> SingleTickTask<T> create(Predicate<T> targetPredicate, int cooldown) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET),
						                  context.queryMemoryValue(MemoryModuleType.ATTACK_TARGET),
						                  context.queryMemoryAbsent(MemoryModuleType.ATTACK_COOLING_DOWN),
						                  context.queryMemoryValue(MemoryModuleType.VISIBLE_MOBS)
				                  )
				                  .apply(
						                  context,
						                  (lookTarget, attackTarget, attackCoolingDown, visibleMobs) -> (world, entity, time) -> {
							                  LivingEntity target = context.getValue(attackTarget);
							                  boolean canAttack = targetPredicate.test((T) entity)
									                  && !isHoldingUsableRangedWeapon(entity)
									                  && entity.isInAttackRange(target)
									                  && context.<LivingTargetCache>getValue(visibleMobs).contains(target);

							                  if (!canAttack) {
								                  return false;
							                  }

							                  lookTarget.remember(new EntityLookTarget(target, true));
							                  entity.swingHand(Hand.MAIN_HAND);
							                  entity.tryAttack(world, target);
							                  attackCoolingDown.remember(true, cooldown);
							                  return true;
						                  }
				                  )
		);
	}

	private static boolean isHoldingUsableRangedWeapon(MobEntity mob) {
		return mob.isHolding(mob::canUseRangedWeapon);
	}
}
