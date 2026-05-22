package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/**
 * Фабричный класс задачи мозга жителя, идущего к запомненной глобальной позиции (дом, работа, встреча).
 * При превышении максимального расстояния ищет промежуточную точку через {@link NoPenaltyTargeting};
 * при исчерпании попыток освобождает тикет POI и запоминает время недостижимости.
 */
public class VillagerWalkTowardsTask {

	private static final int SEARCH_HORIZONTAL_RANGE = 15;
	private static final int SEARCH_VERTICAL_RANGE = 7;
	private static final int MAX_SEARCH_ATTEMPTS = 1000;

	public static SingleTickTask<VillagerEntity> create(
			MemoryModuleType<GlobalPos> destination, float speed, int completionRange, int maxDistance, int maxRunTime
	) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryOptional(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE),
						                  context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryValue(destination)
				                  )
				                  .apply(
						                  context,
						                  (cantReachWalkTargetSince, walkTarget, destinationResult) -> (world, entity, time) -> {
							                  GlobalPos destPos = context.getValue(destinationResult);
							                  Optional<Long> cantReachSince = context.getOptionalValue(cantReachWalkTargetSince);
							                  boolean sameWorld = destPos.dimension() == world.getRegistryKey();
							                  boolean notTimedOut = cantReachSince.isEmpty()
									                  || world.getTime() - cantReachSince.get() <= maxRunTime;

							                  if (sameWorld && notTimedOut) {
								                  if (destPos.pos().getManhattanDistance(entity.getBlockPos()) > maxDistance) {
									                  Vec3d targetPos = null;
									                  int attempts = 0;

									                  while (targetPos == null
											                  || BlockPos.ofFloored(targetPos).getManhattanDistance(entity.getBlockPos()) > maxDistance
									                  ) {
										                  targetPos = NoPenaltyTargeting.findTo(
												                  entity,
												                  SEARCH_HORIZONTAL_RANGE,
												                  SEARCH_VERTICAL_RANGE,
												                  Vec3d.ofBottomCenter(destPos.pos()),
												                  (float) (Math.PI / 2)
										                  );

										                  if (++attempts == MAX_SEARCH_ATTEMPTS) {
											                  entity.releaseTicketFor(destination);
											                  destinationResult.forget();
											                  cantReachWalkTargetSince.remember(time);
											                  return true;
										                  }
									                  }

									                  walkTarget.remember(new WalkTarget(targetPos, speed, completionRange));
								                  } else if (destPos.pos().getManhattanDistance(entity.getBlockPos()) > completionRange) {
									                  walkTarget.remember(new WalkTarget(destPos.pos(), speed, completionRange));
								                  }
							                  } else {
								                  entity.releaseTicketFor(destination);
								                  destinationResult.forget();
								                  cantReachWalkTargetSince.remember(time);
							                  }

							                  return true;
						                  }
				                  )
		);
	}
}
