package net.minecraft.util.profiling.jfr.sample;

import jdk.jfr.consumer.RecordedEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Образец статистики кучи JVM после сборки мусора (GC). Хранит момент времени,
 * объём используемой кучи и тип снимка (до или после GC). Используется для
 * вычисления скорости выделения памяти между циклами GC.
 */
public record GcHeapSummarySample(Instant time, long heapUsed, GcHeapSummarySample.SummaryType summaryType) {

	public static GcHeapSummarySample fromEvent(RecordedEvent event) {
		return new GcHeapSummarySample(
				event.getStartTime(),
				event.getLong("heapUsed"),
				event.getString("when").equalsIgnoreCase("before gc")
						? GcHeapSummarySample.SummaryType.BEFORE_GC
						: GcHeapSummarySample.SummaryType.AFTER_GC
		);
	}

	public static GcHeapSummarySample.Statistics toStatistics(
			Duration duration,
			List<GcHeapSummarySample> samples,
			Duration gcDuration,
			int count
	) {
		return new GcHeapSummarySample.Statistics(duration, gcDuration, count, getAllocatedBytesPerSecond(samples));
	}

	/**
	 * Вычисляет среднюю скорость выделения памяти в байтах/сек между циклами GC.
	 * Для каждого цикла берётся разница между снимком «до GC» текущего цикла
	 * и снимком «после GC» предыдущего — это и есть объём выделенной памяти.
	 */
	private static double getAllocatedBytesPerSecond(List<GcHeapSummarySample> samples) {
		long totalAllocated = 0L;
		Map<GcHeapSummarySample.SummaryType, List<GcHeapSummarySample>> grouped = samples.stream()
				.collect(Collectors.groupingBy(sample -> sample.summaryType));

		List<GcHeapSummarySample> beforeGcSamples = grouped.get(GcHeapSummarySample.SummaryType.BEFORE_GC);
		List<GcHeapSummarySample> afterGcSamples = grouped.get(GcHeapSummarySample.SummaryType.AFTER_GC);

		for (int sampleIndex = 1; sampleIndex < beforeGcSamples.size(); sampleIndex++) {
			GcHeapSummarySample beforeSample = beforeGcSamples.get(sampleIndex);
			GcHeapSummarySample afterPrevSample = afterGcSamples.get(sampleIndex - 1);
			totalAllocated += beforeSample.heapUsed - afterPrevSample.heapUsed;
		}

		Duration duration = Duration.between(samples.get(1).time, samples.get(samples.size() - 1).time);
		return (double) totalAllocated / duration.getSeconds();
	}

	public record Statistics(Duration duration, Duration gcDuration, int count, double allocatedBytesPerSecond) {

		public float getGcDurationRatio() {
			return (float) gcDuration.toMillis() / (float) duration.toMillis();
		}
	}

	enum SummaryType {
		BEFORE_GC,
		AFTER_GC
	}
}
