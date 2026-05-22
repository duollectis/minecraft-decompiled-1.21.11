package net.minecraft.util.profiler;

import com.mojang.logging.LogUtils;
import net.minecraft.util.CsvWriter;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Сохраняет результаты профилирования на диск: CSV-файлы метрик по типам сэмплеров,
 * текстовые файлы отклонений и общий дамп профиля.
 */
public class RecordDumper {

	public static final Path DEBUG_PROFILING_DIRECTORY = Paths.get("debug/profiling");
	public static final String METRICS_DIRECTORY = "metrics";
	public static final String DEVIATIONS_DIRECTORY = "deviations";
	public static final String FILE_NAME = "profiling.txt";

	private static final Logger LOGGER = LogUtils.getLogger();

	private final String type;

	public RecordDumper(String type) {
		this.type = type;
	}

	/**
	 * Создаёт временную директорию с полным дампом: метрики, отклонения и профиль.
	 * Возвращает путь к корню временной директории.
	 */
	public Path createDump(Set<Sampler> samplers, Map<Sampler, List<Deviation>> deviations, ProfileResult result) {
		try {
			Files.createDirectories(DEBUG_PROFILING_DIRECTORY);
		}
		catch (IOException error) {
			throw new UncheckedIOException(error);
		}

		try {
			Path tempDir = Files.createTempDirectory("minecraft-profiling");
			tempDir.toFile().deleteOnExit();
			Files.createDirectories(DEBUG_PROFILING_DIRECTORY);

			Path typeDir = tempDir.resolve(type);
			Path metricsDir = typeDir.resolve(METRICS_DIRECTORY);
			writeSamplers(samplers, metricsDir);

			if (!deviations.isEmpty()) {
				writeDeviations(deviations, typeDir.resolve(DEVIATIONS_DIRECTORY));
			}

			save(result, typeDir);
			return tempDir;
		}
		catch (IOException error) {
			throw new UncheckedIOException(error);
		}
	}

	private void writeSamplers(Set<Sampler> samplers, Path directory) {
		if (samplers.isEmpty()) {
			throw new IllegalArgumentException("Expected at least one sampler to persist");
		}

		Map<SampleType, List<Sampler>> byType = samplers.stream()
			.collect(Collectors.groupingBy(Sampler::getType));

		byType.forEach((sampleType, typeSamplers) -> writeSamplersInType(sampleType, typeSamplers, directory));
	}

	private void writeSamplersInType(SampleType sampleType, List<Sampler> samplers, Path directory) {
		Path csvPath = directory.resolve(
			Util.replaceInvalidChars(sampleType.getName(), Identifier::isPathCharacterValid) + ".csv"
		);
		Writer writer = null;

		try {
			Files.createDirectories(csvPath.getParent());
			writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8);

			CsvWriter.Header header = CsvWriter.makeHeader();
			header.addColumn("@tick");

			for (Sampler sampler : samplers) {
				header.addColumn(sampler.getName());
			}

			CsvWriter csvWriter = header.startBody(writer);
			List<Sampler.Data> dataList = samplers.stream()
				.map(Sampler::collectData)
				.collect(Collectors.toList());

			int minTick = dataList.stream().mapToInt(Sampler.Data::getStartTick).summaryStatistics().getMin();
			int maxTick = dataList.stream().mapToInt(Sampler.Data::getEndTick).summaryStatistics().getMax();

			for (int tick = minTick; tick <= maxTick; tick++) {
				int currentTick = tick;
				Stream<String> values = dataList.stream().map(data -> String.valueOf(data.getValue(currentTick)));
				Object[] row = Stream.concat(Stream.of(String.valueOf(tick)), values).toArray(String[]::new);
				csvWriter.printRow(row);
			}

			LOGGER.info("Flushed metrics to {}", csvPath);
		}
		catch (Exception error) {
			LOGGER.error("Could not save profiler results to {}", csvPath, error);
		}
		finally {
			IOUtils.closeQuietly(writer);
		}
	}

	private void writeDeviations(Map<Sampler, List<Deviation>> deviations, Path deviationsDirectory) {
		DateTimeFormatter formatter = DateTimeFormatter
			.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS", Locale.UK)
			.withZone(ZoneId.systemDefault());

		deviations.forEach((sampler, sampleDeviations) -> sampleDeviations.forEach(deviation -> {
			String timestamp = formatter.format(deviation.instant);
			Path deviationPath = deviationsDirectory
				.resolve(Util.replaceInvalidChars(sampler.getName(), Identifier::isPathCharacterValid))
				.resolve(String.format(Locale.ROOT, "%d@%s.txt", deviation.ticks, timestamp));
			deviation.result.save(deviationPath);
		}));
	}

	private void save(ProfileResult result, Path directory) {
		result.save(directory.resolve(FILE_NAME));
	}
}
