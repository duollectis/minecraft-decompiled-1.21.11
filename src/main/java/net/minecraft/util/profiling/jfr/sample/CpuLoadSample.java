package net.minecraft.util.profiling.jfr.sample;

import jdk.jfr.consumer.RecordedEvent;

/**
 * {@code CpuLoadSample}.
 */
public record CpuLoadSample(double jvm, double userJvm, double system) {

	/**
	 * From event.
	 *
	 * @param event event
	 *
	 * @return CpuLoadSample — результат операции
	 */
	public static CpuLoadSample fromEvent(RecordedEvent event) {
		return new CpuLoadSample(
				event.getFloat("jvmSystem"),
				event.getFloat("jvmUser"),
				event.getFloat("machineTotal")
		);
	}
}
