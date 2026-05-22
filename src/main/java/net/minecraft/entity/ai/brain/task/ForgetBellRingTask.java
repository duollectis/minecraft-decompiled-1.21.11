package net.minecraft.entity.ai.brain.task;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import org.apache.commons.lang3.mutable.MutableInt;

/**
 * Фабричный класс задачи мозга, сбрасывающей воспоминание о звоне колокола после укрытия.
 * Забывает место укрытия и время звона, когда истёк лимит времени или сущность достаточно спряталась.
 */
public class ForgetBellRingTask {

	private static final int MIN_HEARD_BELL_TIME = 300;
	private static final int TICKS_PER_SECOND = 20;

	public static Task<LivingEntity> create(int maxHiddenSeconds, int distance) {
		int maxHiddenTicks = maxHiddenSeconds * TICKS_PER_SECOND;
		MutableInt hiddenTicks = new MutableInt(0);

		return TaskTriggerer.task(
				context -> context.group(
						context.queryMemoryValue(MemoryModuleType.HIDING_PLACE),
						context.queryMemoryValue(MemoryModuleType.HEARD_BELL_TIME)
				).apply(
						context,
						(hidingPlace, heardBellTime) -> (world, entity, time) -> {
							long bellTime = context.<Long>getValue(heardBellTime);
							boolean bellExpired = bellTime + MIN_HEARD_BELL_TIME <= time;

							if (hiddenTicks.intValue() <= maxHiddenTicks && !bellExpired) {
								BlockPos hidePos = context.<GlobalPos>getValue(hidingPlace).pos();

								if (hidePos.isWithinDistance(entity.getBlockPos(), distance)) {
									hiddenTicks.increment();
								}

								return true;
							}

							heardBellTime.forget();
							hidingPlace.forget();
							entity.getBrain().refreshActivities(
									world.getEnvironmentAttributes(),
									world.getTime(),
									entity.getEntityPos()
							);
							hiddenTicks.setValue(0);
							return true;
						}
				)
		);
	}
}
