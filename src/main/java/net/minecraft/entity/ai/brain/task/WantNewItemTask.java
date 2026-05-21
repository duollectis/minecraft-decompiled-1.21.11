package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.PiglinEntity;

import java.util.Optional;

/**
 * {@code WantNewItemTask}.
 */
public class WantNewItemTask<E extends PiglinEntity> {

	/**
	 * Create.
	 *
	 * @param range range
	 *
	 * @return Task — результат операции
	 */
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
							                  else {
								                  Optional<ItemEntity>
										                  optional =
										                  context.getOptionalValue(nearestVisibleWantedItem);
								                  if (optional.isPresent() && optional.get().isInRange(entity, range)) {
									                  return false;
								                  }
								                  else {
									                  admiringItem.forget();
									                  return true;
								                  }
							                  }
						                  }
				                  )
		);
	}
}
