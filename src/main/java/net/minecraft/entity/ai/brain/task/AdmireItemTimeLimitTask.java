package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;

import java.util.Optional;

/**
 * Фабричный класс задачи мозга, ограничивающей время попытки пиглина добраться до восхищающего предмета.
 * По истечении {@code cooldown} тиков сбрасывает режим восхищения и блокирует ходьбу к предмету.
 */
public class AdmireItemTimeLimitTask {

	public static Task<LivingEntity> create(int cooldown, int timeLimit) {
		return TaskTriggerer.task(
				context -> context.group(
						context.queryMemoryValue(MemoryModuleType.ADMIRING_ITEM),
						context.queryMemoryValue(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM),
						context.queryMemoryOptional(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM),
						context.queryMemoryOptional(MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM)
				).apply(
						context,
						(admiringItem, nearestVisibleWantedItem, timeTryingToReachAdmireItem, disableWalkToAdmireItem) -> (world, entity, time) -> {
							if (!entity.getOffHandStack().isEmpty()) {
								return false;
							}

							Optional<Integer> elapsed = context.getOptionalValue(timeTryingToReachAdmireItem);
							if (elapsed.isEmpty()) {
								timeTryingToReachAdmireItem.remember(0);
								return true;
							}

							int ticks = elapsed.get();
							if (ticks > cooldown) {
								admiringItem.forget();
								timeTryingToReachAdmireItem.forget();
								disableWalkToAdmireItem.remember(true, timeLimit);
							} else {
								timeTryingToReachAdmireItem.remember(ticks + 1);
							}

							return true;
						}
				)
		);
	}
}
