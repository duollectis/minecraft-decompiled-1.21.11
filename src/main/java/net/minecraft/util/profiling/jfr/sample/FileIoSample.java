package net.minecraft.util.profiling.jfr.sample;

import com.mojang.datafixers.util.Pair;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Образец операции файлового ввода-вывода. Хранит длительность операции,
 * путь к файлу и количество байт. Используется для профилирования дисковой
 * активности через JFR-события {@code jdk.FileRead} и {@code jdk.FileWrite}.
 */
public record FileIoSample(Duration duration, @Nullable String path, long bytes) {

	public static FileIoSample.Statistics toStatistics(Duration duration, List<FileIoSample> samples) {
		long totalBytes = samples.stream().mapToLong(sample -> sample.bytes).sum();
		return new FileIoSample.Statistics(
				totalBytes,
				(double) totalBytes / duration.getSeconds(),
				samples.size(),
				(double) samples.size() / duration.getSeconds(),
				samples.stream().map(FileIoSample::duration).reduce(Duration.ZERO, Duration::plus),
				samples.stream()
						.filter(sample -> sample.path != null)
						.collect(Collectors.groupingBy(
								sample -> sample.path,
								Collectors.summingLong(sample -> sample.bytes)
						))
						.entrySet()
						.stream()
						.sorted(Entry.<String, Long>comparingByValue().reversed())
						.map(entry -> Pair.of(entry.getKey(), entry.getValue()))
						.limit(10L)
						.toList()
		);
	}

	public record Statistics(
			long totalBytes,
			double bytesPerSecond,
			long count,
			double countPerSecond,
			Duration totalDuration,
			List<Pair<String, Long>> topContributors
	) {
	}
}
