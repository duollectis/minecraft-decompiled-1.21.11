package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Фабричный класс задачи мозга, направляющей существо к случайно смещённой позиции из памяти.
 * Смещение ±1 блок по осям X и Z добавляет естественность движения.
 */
public class WalkTowardsFuzzyPosTask {

	private static BlockPos fuzz(MobEntity mob, BlockPos pos) {
		Random random = mob.getEntityWorld().random;
		return pos.add(fuzz(random), 0, fuzz(random));
	}

	private static int fuzz(Random random) {
		return random.nextInt(3) - 1;
	}

	public static <E extends MobEntity> SingleTickTask<E> create(
			MemoryModuleType<BlockPos> posModule,
			int completionRange,
			float speed
	) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryValue(posModule),
						                  context.queryMemoryAbsent(MemoryModuleType.ATTACK_TARGET),
						                  context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET)
				                  )
				                  .apply(
						                  context,
						                  (pos, attackTarget, walkTarget, lookTarget) -> (world, entity, time) -> {
							                  BlockPos targetPos = context.getValue(pos);
							                  boolean alreadyInRange = targetPos.isWithinDistance(entity.getBlockPos(), completionRange);

							                  if (!alreadyInRange) {
								                  TargetUtil.walkTowards(entity, fuzz(entity, targetPos), speed, completionRange);
							                  }

							                  return true;
						                  }
				                  )
		);
	}
}
