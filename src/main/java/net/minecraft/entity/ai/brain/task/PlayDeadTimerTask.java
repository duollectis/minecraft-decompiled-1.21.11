package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;

/**
 * Фабричный класс задачи мозга, отсчитывающей таймер притворства мёртвым.
 * По истечении таймера сбрасывает память {@code PLAY_DEAD_TICKS} и восстанавливает активности.
 */
public class PlayDeadTimerTask {

	public static Task<LivingEntity> create() {
		return TaskTriggerer.task(
				context -> context
						.group(
								context.queryMemoryValue(MemoryModuleType.PLAY_DEAD_TICKS),
								context.queryMemoryOptional(MemoryModuleType.HURT_BY_ENTITY)
						)
						.apply(
								context, (playDeadTicks, hurtByEntity) -> (world, entity, time) -> {
									int ticks = context.<Integer>getValue(playDeadTicks);
	
									if (ticks > 0) {
										playDeadTicks.remember(ticks - 1);
										return true;
									}
	
									playDeadTicks.forget();
									hurtByEntity.forget();
									entity.getBrain().resetPossibleActivities();

									return true;
								}
						)
		);
	}
}
