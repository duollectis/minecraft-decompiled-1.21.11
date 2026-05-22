package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.GlobalPos;
import org.apache.commons.lang3.mutable.MutableLong;

import java.util.List;

/**
 * Фабричный класс задачи мозга, направляющей жителя к одной из вторичных точек интереса.
 * Активируется только когда житель находится рядом с основной позицией.
 */
public class GoToSecondaryPositionTask {

	private static final long UPDATE_INTERVAL = 100L;

	public static Task<VillagerEntity> create(
			MemoryModuleType<List<GlobalPos>> secondaryPositions,
			float speed,
			int completionRange,
			int primaryPositionActivationDistance,
			MemoryModuleType<GlobalPos> primaryPosition
	) {
		MutableLong nextUpdateTime = new MutableLong(0L);
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryOptional(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryValue(secondaryPositions),
						                  context.queryMemoryValue(primaryPosition)
				                  )
				                  .apply(
						                  context,
						                  (walkTarget, secondary, primary) -> (world, entity, time) -> {
							                  List<GlobalPos> positions = context.getValue(secondary);
							                  GlobalPos primaryPos = context.getValue(primary);

							                  if (positions.isEmpty()) {
								                  return false;
							                  }

							                  GlobalPos secondaryPos = positions.get(world.getRandom().nextInt(positions.size()));

							                  if (secondaryPos == null
									                  || world.getRegistryKey() != secondaryPos.dimension()
									                  || !primaryPos.pos().isWithinDistance(
									                  entity.getEntityPos(),
									                  primaryPositionActivationDistance
							                  )) {
								                  return false;
							                  }

							                  if (time > nextUpdateTime.longValue()) {
								                  walkTarget.remember(new WalkTarget(
										                  secondaryPos.pos(),
										                  speed,
										                  completionRange
								                  ));
								                  nextUpdateTime.setValue(time + UPDATE_INTERVAL);
							                  }

							                  return true;
						                  }
				                  )
		);
	}
}
