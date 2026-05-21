package net.minecraft.client.session.telemetry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
/**
 * {@code TelemetryLogger}.
 */
public interface TelemetryLogger {

	void log(SentTelemetryEvent event);
}
