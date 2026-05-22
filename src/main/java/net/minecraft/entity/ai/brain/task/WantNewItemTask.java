package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.PiglinEntity;

import java.util.Optional;

/**
 * Фабричный класс задачи мозга пиглина, сбрасывающей флаг восхищения предметом.
 * Активируется когда рука свободна и ближайший желанный предмет вне радиуса {@code range}.
 */
public class WantNewItemTask<E extends PiglinEntity> {

	public static Task<LivingEntity> create(int range) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryValue(MemoryModuleType.ADMIRING_ITEM),
						                  context.queryMemoryOptional(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM)
				                  )
				                  .apply(
						                  context,
						                  (admiringItem, nearestVisibleWantedItem) -> (world, entity, time) -> {
							                  if (!entity.getOffHandStack().isEmpty()) {
								                  return false;
							                  }

							                  Optional<ItemEntity> nearestItemOpt = context.getOptionalValue(nearestVisibleWantedItem);

							                  if (nearestItemOpt.isPresent() && nearestItemOpt.get().isInRange(entity, range)) {
								                  return false;
							                  }

							                  admiringItem.forget();
							                  return true;
						                  }
				                  )
		);
	}
}
