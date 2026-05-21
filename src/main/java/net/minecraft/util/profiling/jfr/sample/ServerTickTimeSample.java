package net.minecraft.util.profiling.jfr.sample;

import jdk.jfr.consumer.RecordedEvent;

import java.time.Duration;
import java.time.Instant;

/**
 * {@code ServerTickTimeSample}.
 */
public record ServerTickTimeSample(Instant time, Duration averageTickMs) {

	/**
	 * From event.
	 *
	 * @param event event
	 *
	 * @return ServerTickTimeSample — результат операции
	 */
	public static ServerTickTimeSample fromEvent(RecordedEvent event) {
		return new ServerTickTimeSample(event.getStartTime(), event.getDuration("averageTickDuration"));
	}
}
