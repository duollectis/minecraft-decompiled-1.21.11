package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.world.rule.GameRules;

import java.util.Optional;

/**
 * {@code ForgetAngryAtTargetTask}.
 */
public class ForgetAngryAtTargetTask {

	/**
	 * Create.
	 *
	 * @return Task — результат операции
	 */
	public static Task<LivingEntity> create() {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryValue(MemoryModuleType.ANGRY_AT))
				                  .apply(
						                  context,
						                  angryAt -> (world, entity, time) -> {
							                  Optional.ofNullable(world.getEntity(context.getValue(angryAt)))
							                          .map(target -> target instanceof LivingEntity livingEntity
							                                         ? livingEntity : null)
							                          .filter(LivingEntity::isDead)
							                          .filter(target -> target.getType() != EntityType.PLAYER || world
									                          .getGameRules()
									                          .getValue(GameRules.FORGIVE_DEAD_PLAYERS))
							                          .ifPresent(target -> angryAt.forget());
							                  return true;
						                  }
				                  )
		);
	}
}
