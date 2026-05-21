package net.minecraft.world.debug;

import net.minecraft.server.world.ServerWorld;
import org.jspecify.annotations.Nullable;

/**
 * {@code DebugTrackable}.
 */
public interface DebugTrackable {

	void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker);

	/**
	 * {@code DebugDataSupplier}.
	 */
	public interface DebugDataSupplier<T> {

		@Nullable T get();
	}

	/**
	 * {@code Tracker}.
	 */
	public interface Tracker {

		<T> void track(DebugSubscriptionType<T> type, DebugTrackable.DebugDataSupplier<T> dataSupplier);
	}
}
