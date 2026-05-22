package net.minecraft.entity.ai.brain.task;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.brain.BlockPosLookTarget;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 * Фабричный класс задачи мозга водного существа, ищущего ближайшую сушу.
 * Итерирует позиции в радиусе {@code range} с кулдауном {@code TASK_COOLDOWN} тиков между попытками.
 */
public class WalkTowardsLandTask {

	private static final int TASK_COOLDOWN = 60;

	public static Task<PathAwareEntity> create(int range, float speed) {
		MutableLong nextUpdateTime = new MutableLong(0L);
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryAbsent(MemoryModuleType.ATTACK_TARGET),
						                  context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryOptional(MemoryModuleType.LOOK_TARGET)
				                  )
				                  .apply(
						                  context,
						                  (attackTarget, walkTarget, lookTarget) -> (world, entity, time) -> {
							                  if (!world.getFluidState(entity.getBlockPos()).isIn(FluidTags.WATER)) {
								                  return false;
							                  }

							                  if (time < nextUpdateTime.longValue()) {
								                  nextUpdateTime.setValue(time + TASK_COOLDOWN);
								                  return true;
							                  }

							                  BlockPos entityPos = entity.getBlockPos();
							                  BlockPos.Mutable mutablePos = new BlockPos.Mutable();
							                  ShapeContext shapeCtx = ShapeContext.of(entity);

							                  for (BlockPos candidate : BlockPos.iterateOutwards(entityPos, range, range, range)) {
								                  if (candidate.getX() == entityPos.getX() && candidate.getZ() == entityPos.getZ()) {
									                  continue;
								                  }

								                  BlockState candidateState = world.getBlockState(candidate);
								                  BlockState belowState = world.getBlockState(mutablePos.set(candidate, Direction.DOWN));

								                  if (!candidateState.isOf(Blocks.WATER)
										                  && world.getFluidState(candidate).isEmpty()
										                  && candidateState.getCollisionShape(world, candidate, shapeCtx).isEmpty()
										                  && belowState.isSideSolidFullSquare(world, mutablePos, Direction.UP)
								                  ) {
									                  BlockPos landPos = candidate.toImmutable();
									                  lookTarget.remember(new BlockPosLookTarget(landPos));
									                  walkTarget.remember(new WalkTarget(new BlockPosLookTarget(landPos), speed, 1));
									                  break;
								                  }
							                  }

							                  nextUpdateTime.setValue(time + TASK_COOLDOWN);
							                  return true;
						                  }
				                  )
		);
	}
}
