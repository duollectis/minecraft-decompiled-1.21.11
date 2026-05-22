package net.minecraft.entity.ai.brain.task;

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
 * Фабричный класс задачи мозга, направляющей существо к ближайшей воде с берега.
 * Ищет позицию рядом с блоком воды с твёрдым основанием; повторяет попытку каждые {@code RETRY_DELAY} тиков.
 */
public class WalkTowardsWaterTask {

	private static final long RETRY_DELAY = 40L;

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
							                  if (world.getFluidState(entity.getBlockPos()).isIn(FluidTags.WATER)) {
								                  return false;
							                  }

							                  if (time < nextUpdateTime.longValue()) {
								                  nextUpdateTime.setValue(time + RETRY_DELAY);
								                  return true;
							                  }

							                  ShapeContext shapeCtx = ShapeContext.of(entity);
							                  BlockPos entityPos = entity.getBlockPos();
							                  BlockPos.Mutable mutablePos = new BlockPos.Mutable();
							                  boolean found = false;

							                  outer:
							                  for (BlockPos candidate : BlockPos.iterateOutwards(entityPos, range, range, range)) {
								                  if (candidate.getX() == entityPos.getX() && candidate.getZ() == entityPos.getZ()) {
									                  continue;
								                  }

								                  boolean candidatePassable = world.getBlockState(candidate)
								                                                   .getCollisionShape(world, candidate, shapeCtx)
								                                                   .isEmpty();
								                  boolean belowSolid = !world.getBlockState(mutablePos.set(candidate, Direction.DOWN))
								                                             .getCollisionShape(world, candidate, shapeCtx)
								                                             .isEmpty();

								                  if (candidatePassable && belowSolid) {
									                  for (Direction direction : Direction.Type.HORIZONTAL) {
										                  mutablePos.set(candidate, direction);

										                  if (world.getBlockState(mutablePos).isAir()
												                  && world.getBlockState(mutablePos.move(Direction.DOWN)).isOf(Blocks.WATER)
										                  ) {
											                  lookTarget.remember(new BlockPosLookTarget(candidate));
											                  walkTarget.remember(new WalkTarget(new BlockPosLookTarget(candidate), speed, 0));
											                  found = true;
											                  break outer;
										                  }
									                  }
								                  }
							                  }

							                  nextUpdateTime.setValue(time + RETRY_DELAY);
							                  return true;
						                  }
				                  )
		);
	}
}
