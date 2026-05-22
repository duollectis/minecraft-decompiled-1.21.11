package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;

/**
 * Фабричный класс задачи мозга, направляющей сущность под укрытие при открытом небе.
 * Ищет ближайший блок без прямой видимости неба в радиусе 1 блока.
 */
public class GoIndoorsTask {

	public static Task<PathAwareEntity> create(float speed) {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryAbsent(MemoryModuleType.WALK_TARGET))
						.apply(
								context,
								walkTarget -> (world, entity, time) -> {
									if (world.isSkyVisible(entity.getBlockPos())) {
										return false;
									}

									BlockPos pos = entity.getBlockPos();
									List<BlockPos> nearby = BlockPos.stream(pos.add(-1, -1, -1), pos.add(1, 1, 1))
											.map(BlockPos::toImmutable)
											.collect(Util.toArrayList());
									Collections.shuffle(nearby);

									nearby.stream()
											.filter(p -> !world.isSkyVisible(p))
											.filter(p -> world.isTopSolid(p, entity))
											.filter(p -> world.isSpaceEmpty(entity))
											.findFirst()
											.ifPresent(p -> walkTarget.remember(new WalkTarget(p, speed, 0)));

									return true;
								}
						)
		);
	}
}
