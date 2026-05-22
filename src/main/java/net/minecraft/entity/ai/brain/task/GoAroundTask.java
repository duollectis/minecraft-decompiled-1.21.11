package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.FuzzyTargeting;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.mutable.MutableLong;

import java.util.Optional;

/**
 * Фабричный класс задачи мозга, заставляющей сущность случайно бродить вокруг точки интереса.
 * Обновляет цель блуждания каждые {@code UPDATE_INTERVAL} тиков.
 */
public class GoAroundTask {

	private static final int UPDATE_INTERVAL = 180;
	private static final int HORIZONTAL_RANGE = 8;
	private static final int VERTICAL_RANGE = 6;

	public static SingleTickTask<PathAwareEntity> create(
			MemoryModuleType<GlobalPos> posModule,
			float walkSpeed,
			int maxDistance
	) {
		MutableLong nextUpdateTime = new MutableLong(0L);

		return TaskTriggerer.task(
				context -> context.group(
						context.queryMemoryOptional(MemoryModuleType.WALK_TARGET),
						context.queryMemoryValue(posModule)
				).apply(
						context,
						(walkTarget, pos) -> (world, entity, time) -> {
							GlobalPos globalPos = context.getValue(pos);

							if (world.getRegistryKey() != globalPos.dimension()
									|| !globalPos.pos().isWithinDistance(entity.getEntityPos(), maxDistance)) {
								return false;
							}

							if (time <= nextUpdateTime.longValue()) {
								return true;
							}

							Optional<Vec3d> wanderPos = Optional.ofNullable(
									FuzzyTargeting.find(entity, HORIZONTAL_RANGE, VERTICAL_RANGE)
							);
							walkTarget.remember(wanderPos.map(p -> new WalkTarget(p, walkSpeed, 1)));
							nextUpdateTime.setValue(time + UPDATE_INTERVAL);
							return true;
						}
				)
		);
	}
}
