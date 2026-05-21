package net.minecraft.world.tick;

import net.minecraft.util.math.BlockPos;

/**
 * {@code QueryableTickScheduler}.
 */
public interface QueryableTickScheduler<T> extends TickScheduler<T> {

	boolean isTicking(BlockPos pos, T type);
}
