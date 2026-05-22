package net.minecraft.entity.ai.brain.task;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.fluid.Fluids;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.event.GameEvent;

/**
 * Фабричный класс задачи мозга беременной лягушки, откладывающей икру рядом с водой.
 * Ищет горизонтально смежный водный блок с открытым верхом и размещает икру над ним.
 */
public class LayFrogSpawnTask {

	public static Task<LivingEntity> create(Block frogSpawn) {
		return TaskTriggerer.task(
				context -> context.group(
						                  context.queryMemoryAbsent(MemoryModuleType.ATTACK_TARGET),
						                  context.queryMemoryValue(MemoryModuleType.WALK_TARGET),
						                  context.queryMemoryValue(MemoryModuleType.IS_PREGNANT)
				                  )
				                  .apply(
						                  context,
						                  (attackTarget, walkTarget, isPregnant) -> (world, entity, time) -> {
							                  if (entity.isTouchingWater() || !entity.isOnGround()) {
								                  return false;
							                  }

							                  BlockPos belowPos = entity.getBlockPos().down();

							                  for (Direction direction : Direction.Type.HORIZONTAL) {
								                  BlockPos adjacentPos = belowPos.offset(direction);
								                  boolean hasOpenTop = world.getBlockState(adjacentPos)
								                                            .getCollisionShape(world, adjacentPos)
								                                            .getFace(Direction.UP)
								                                            .isEmpty();

								                  if (hasOpenTop && world.getFluidState(adjacentPos).isOf(Fluids.WATER)) {
									                  BlockPos spawnPos = adjacentPos.up();

									                  if (world.getBlockState(spawnPos).isAir()) {
										                  BlockState spawnState = frogSpawn.getDefaultState();
										                  world.setBlockState(spawnPos, spawnState, 3);
										                  world.emitGameEvent(
												                  GameEvent.BLOCK_PLACE,
												                  spawnPos,
												                  GameEvent.Emitter.of(entity, spawnState)
										                  );
										                  world.playSoundFromEntity(
												                  null,
												                  entity,
												                  SoundEvents.ENTITY_FROG_LAY_SPAWN,
												                  SoundCategory.BLOCKS,
												                  1.0F,
												                  1.0F
										                  );
										                  isPregnant.forget();
										                  return true;
									                  }
								                  }
							                  }

							                  return true;
						                  }
				                  )
		);
	}
}
