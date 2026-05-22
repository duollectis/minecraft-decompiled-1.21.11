package net.minecraft.entity.ai.brain.task;

import net.minecraft.block.BellBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

/**
 * Фабричный класс задачи мозга, звонящей в колокол на месте встречи жителей.
 * С вероятностью 5% звонит в колокол, если житель находится в радиусе {@code MAX_DISTANCE} блоков от него.
 */
public class RingBellTask {

	private static final float SKIP_CHANCE = 0.95F;
	public static final int MAX_DISTANCE = 3;

	public static Task<LivingEntity> create() {
		return TaskTriggerer.task(
				context -> context.group(context.queryMemoryValue(MemoryModuleType.MEETING_POINT)).apply(
						context, meetingPoint -> (world, entity, time) -> {
							if (world.random.nextFloat() <= SKIP_CHANCE) {
								return false;
							}

							BlockPos bellPos = context.<GlobalPos>getValue(meetingPoint).pos();

							if (bellPos.isWithinDistance(entity.getBlockPos(), MAX_DISTANCE)) {
								BlockState bellState = world.getBlockState(bellPos);

								if (bellState.isOf(Blocks.BELL)) {
									BellBlock bellBlock = (BellBlock) bellState.getBlock();
									bellBlock.ring(entity, world, bellPos, null);
								}
							}

							return true;
						}
				)
		);
	}
}
