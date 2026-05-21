package net.minecraft.client.session.telemetry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

@Environment(EnvType.CLIENT)
/**
 * {@code WorldLoadTimesEvent}.
 */
public class WorldLoadTimesEvent {

	private final boolean newWorld;
	private final @Nullable Duration worldLoadTime;

	public WorldLoadTimesEvent(boolean newWorld, @Nullable Duration worldLoadTime) {
		this.worldLoadTime = worldLoadTime;
		this.newWorld = newWorld;
	}

	public void send(TelemetrySender sender) {
		if (this.worldLoadTime != null) {
			sender.send(
					TelemetryEventType.WORLD_LOAD_TIMES, builder -> {
						builder.put(TelemetryEventProperty.WORLD_LOAD_TIME_MS, (int) this.worldLoadTime.toMillis());
						builder.put(TelemetryEventProperty.NEW_WORLD, this.newWorld);
					}
			);
		}
	}
}
