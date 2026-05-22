package net.minecraft.world.tick;

import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;

/**
 * Представление мира, предоставляющее доступ к планировщикам тиков блоков и жидкостей,
 * а также удобные методы для постановки тиков в очередь.
 */
public interface ScheduledTickView {

	/**
	 * Создаёт упорядоченный тик с заданным приоритетом.
	 *
	 * @param pos      позиция блока
	 * @param type     тип объекта
	 * @param delay    задержка в тиках
	 * @param priority приоритет выполнения
	 */
	<T> OrderedTick<T> createOrderedTick(BlockPos pos, T type, int delay, TickPriority priority);

	/**
	 * Создаёт упорядоченный тик с приоритетом {@link TickPriority#NORMAL}.
	 *
	 * @param pos   позиция блока
	 * @param type  тип объекта
	 * @param delay задержка в тиках
	 */
	<T> OrderedTick<T> createOrderedTick(BlockPos pos, T type, int delay);

	QueryableTickScheduler<Block> getBlockTickScheduler();

	default void scheduleBlockTick(BlockPos pos, Block block, int delay, TickPriority priority) {
		getBlockTickScheduler().scheduleTick(createOrderedTick(pos, block, delay, priority));
	}

	default void scheduleBlockTick(BlockPos pos, Block block, int delay) {
		getBlockTickScheduler().scheduleTick(createOrderedTick(pos, block, delay));
	}

	QueryableTickScheduler<Fluid> getFluidTickScheduler();

	default void scheduleFluidTick(BlockPos pos, Fluid fluid, int delay, TickPriority priority) {
		getFluidTickScheduler().scheduleTick(createOrderedTick(pos, fluid, delay, priority));
	}

	default void scheduleFluidTick(BlockPos pos, Fluid fluid, int delay) {
		getFluidTickScheduler().scheduleTick(createOrderedTick(pos, fluid, delay));
	}
}
