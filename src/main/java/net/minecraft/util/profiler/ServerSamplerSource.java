package net.minecraft.util.profiler;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.SystemDetails;
import net.minecraft.util.thread.ExecutorSampling;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

/**
 * Источник сэмплеров для серверного профилирования: время тика, загрузка CPU
 * по каждому логическому ядру, использование кучи JVM и метрики пулов потоков.
 */
public class ServerSamplerSource implements SamplerSource {

	private static final Logger LOGGER = LogUtils.getLogger();

	// Порог обновления данных CPU: не чаще одного раза в 501 мс
	private static final long CPU_REFRESH_INTERVAL_MS = 501L;

	private final Set<Sampler> samplers = new ObjectOpenHashSet<>();
	private final SamplerFactory factory = new SamplerFactory();

	public ServerSamplerSource(LongSupplier nanoTimeSupplier, boolean includeSystem) {
		samplers.add(createTickTimeTracker(nanoTimeSupplier));

		if (includeSystem) {
			samplers.addAll(createSystemSamplers());
		}
	}

	/**
	 * Создаёт набор системных сэмплеров: CPU по ядрам, куча JVM и пулы потоков.
	 * При недоступности OSHI логирует предупреждение и пропускает CPU-метрики.
	 */
	public static Set<Sampler> createSystemSamplers() {
		Builder<Sampler> builder = ImmutableSet.builder();

		try {
			ServerSamplerSource.CpuUsageFetcher cpuFetcher = new ServerSamplerSource.CpuUsageFetcher();
			IntStream.range(0, cpuFetcher.logicalProcessorCount)
				.mapToObj(index -> Sampler.create(
					"cpu#" + index,
					SampleType.CPU,
					() -> cpuFetcher.getCpuUsage(index)
				))
				.forEach(builder::add);
		}
		catch (Throwable error) {
			LOGGER.warn("Failed to query cpu, no cpu stats will be recorded", error);
		}

		builder.add(
			Sampler.create(
				"heap MiB",
				SampleType.JVM,
				() -> SystemDetails.toMebibytes(
					Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
				)
			)
		);

		builder.addAll(ExecutorSampling.INSTANCE.createSamplers());
		return builder.build();
	}

	@Override
	public Set<Sampler> getSamplers(Supplier<ReadableProfiler> profilerSupplier) {
		samplers.addAll(factory.createSamplers(profilerSupplier));
		return samplers;
	}

	/**
	 * Создаёт сэмплер времени тика на основе {@link Stopwatch} с кастомным {@link Ticker}.
	 * Детектор отклонений срабатывает при росте времени тика более чем в 2 раза.
	 */
	public static Sampler createTickTimeTracker(LongSupplier nanoTimeSupplier) {
		Stopwatch stopwatch = Stopwatch.createUnstarted(new Ticker() {
			@Override
			public long read() {
				return nanoTimeSupplier.getAsLong();
			}
		});

		ToDoubleFunction<Stopwatch> elapsed = watch -> {
			if (watch.isRunning()) {
				watch.stop();
			}

			long nanos = watch.elapsed(TimeUnit.NANOSECONDS);
			watch.reset();
			return nanos;
		};

		Sampler.RatioDeviationChecker deviationChecker = new Sampler.RatioDeviationChecker(2.0F);
		return Sampler.builder("ticktime", SampleType.TICK_LOOP, elapsed, stopwatch)
			.startAction(Stopwatch::start)
			.deviationChecker(deviationChecker)
			.build();
	}

	/**
	 * Кэширующий поставщик загрузки CPU через OSHI.
	 * Обновляет данные не чаще одного раза в {@link #CPU_REFRESH_INTERVAL_MS} мс.
	 */
	static class CpuUsageFetcher {

		private final SystemInfo systemInfo = new SystemInfo();
		private final CentralProcessor processor = systemInfo.getHardware().getProcessor();
		public final int logicalProcessorCount = processor.getLogicalProcessorCount();
		private long[][] loadTicks = processor.getProcessorCpuLoadTicks();
		private double[] loadBetweenTicks = processor.getProcessorCpuLoadBetweenTicks(loadTicks);
		private long lastCheckTime;

		public double getCpuUsage(int index) {
			long now = System.currentTimeMillis();

			if (lastCheckTime == 0L || lastCheckTime + CPU_REFRESH_INTERVAL_MS < now) {
				loadBetweenTicks = processor.getProcessorCpuLoadBetweenTicks(loadTicks);
				loadTicks = processor.getProcessorCpuLoadTicks();
				lastCheckTime = now;
			}

			return loadBetweenTicks[index] * 100.0;
		}
	}
}
