package net.minecraft.entity.ai.brain.task;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 * Фабричный класс задачи мозга, направляющей существо к ближайшей воде.
 * Предпочитает открытую воду (без блока сверху), при отсутствии — ищет покрытую воду.
 */
public class SeekWaterTask {

	private static final long RETRY_DELAY_SHORT = 20L;
	private static final long RETRY_DELAY_LONG = 40L;
	private static final long RETRY_DELAY_EXTRA = 2L;
	private static final double MIN_WATER_ENTRY_DISTANCE = 1.5;

	public static Task<PathAwareEntity> create(int range, float speed) {
		MutableLong nextUpdateTime = new MutableLong(0L);

		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryAbsent(MemoryModuleType.ATTACK_TARGET),
						                  context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET)
				                  )
				                  .apply(
						                  context, (attackTarget, walkTarget, lookTarget) -> (world, entity, time) -> {
							                  if (world.getFluidState(entity.getBlockPos()).isIn(FluidTags.WATER)) {
								                  return false;
							                  }

							                  if (time < nextUpdateTime.longValue()) {
								                  nextUpdateTime.setValue(time + RETRY_DELAY_SHORT + RETRY_DELAY_EXTRA);
								                  return true;
							                  }

							                  BlockPos entityPos = entity.getBlockPos();
							                  BlockPos openWaterPos = null;
							                  BlockPos coveredWaterPos = null;

							                  for (BlockPos candidate : BlockPos.iterateOutwards(entityPos, range, range, range)) {
								                  if (candidate.getX() == entityPos.getX() && candidate.getZ() == entityPos.getZ()) {
									                  continue;
								                  }

								                  BlockState aboveState = entity.getEntityWorld().getBlockState(candidate.up());
								                  BlockState candidateState = entity.getEntityWorld().getBlockState(candidate);

								                  if (!candidateState.isOf(Blocks.WATER)) {
									                  continue;
								                  }

								                  if (aboveState.isAir()) {
									                  openWaterPos = candidate.toImmutable();
									                  break;
								                  }

								                  if (coveredWaterPos == null
										                  && !candidate.isWithinDistance(entity.getEntityPos(), MIN_WATER_ENTRY_DISTANCE)
								                  ) {
									                  coveredWaterPos = candidate.toImmutable();
								                  }
							                  }

							                  BlockPos targetPos = openWaterPos != null ? openWaterPos : coveredWaterPos;

							                  if (targetPos != null) {
								                  lookTarget.remember(new BlockPosLookTarget(targetPos));
								                  walkTarget.remember(new WalkTarget(new BlockPosLookTarget(targetPos), speed, 0));
							                  }

							                  nextUpdateTime.setValue(time + RETRY_DELAY_LONG);
							                  return true;
						                  }
				                  )
		);
	}
}
