package net.minecraft.world.tick;

import java.util.List;

/**
 * {@code SerializableTickScheduler}.
 */
public interface SerializableTickScheduler<T> {

	List<Tick<T>> collectTicks(long time);
}
