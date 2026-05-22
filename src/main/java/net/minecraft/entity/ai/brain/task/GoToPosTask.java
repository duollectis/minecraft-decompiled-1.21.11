package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.GlobalPos;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 * Фабричный класс задачи мозга, направляющей сущность к позиции из указанного модуля памяти.
 * Обновляет цель ходьбы каждые {@code UPDATE_INTERVAL} тиков, пока позиция в пределах {@code maxDistance}.
 */
public class GoToPosTask {

	private static final long UPDATE_INTERVAL = 80L;

	public static Task<PathAwareEntity> create(
			MemoryModuleType<GlobalPos> posModule,
			float walkSpeed,
			int completionRange,
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

							walkTarget.remember(new WalkTarget(globalPos.pos(), walkSpeed, completionRange));
							nextUpdateTime.setValue(time + UPDATE_INTERVAL);
							return true;
						}
				)
		);
	}
}
