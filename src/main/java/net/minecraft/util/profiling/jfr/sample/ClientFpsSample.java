package net.minecraft.util.profiling.jfr.sample;

import jdk.jfr.consumer.RecordedEvent;

/**
 * {@code ClientFpsSample}.
 */
public record ClientFpsSample(int fps) {

	public static ClientFpsSample fromEvent(RecordedEvent event, String key) {
		return new ClientFpsSample(event.getInt(key));
	}
}
