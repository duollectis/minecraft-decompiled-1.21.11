package net.minecraft.util;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.util.profiler.DummyProfiler;
import net.minecraft.util.profiler.ProfileResult;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.ProfilerSystem;
import net.minecraft.util.profiler.ReadableProfiler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.util.function.LongSupplier;

/**
 * Монитор длительности тиков: если тик превышает порог {@code overtime},
 * результат профилирования сохраняется в файл для последующего анализа.
 */
public class TickDurationMonitor {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final LongSupplier timeGetter;
	private final long overtime;
	private final File tickResultsDirectory;
	private int tickCount;
	private ReadableProfiler profiler = DummyProfiler.INSTANCE;

	public TickDurationMonitor(LongSupplier timeGetter, String filename, long overtime) {
		this.timeGetter = timeGetter;
		this.tickResultsDirectory = new File("debug", filename);
		this.overtime = overtime;
	}

	/**
	 * Создаёт новый профилировщик для следующего тика и инкрементирует счётчик тиков.
	 *
	 * @return активный {@link Profiler} для текущего тика
	 */
	public Profiler nextProfiler() {
		profiler = new ProfilerSystem(timeGetter, () -> tickCount, () -> true);
		tickCount++;
		return profiler;
	}

	/**
	 * Завершает тик: если время выполнения превысило порог, сохраняет результат профилирования в файл.
	 */
	public void endTick() {
		if (profiler == DummyProfiler.INSTANCE) {
			return;
		}

		ProfileResult profileResult = profiler.getResult();
		profiler = DummyProfiler.INSTANCE;

		if (profileResult.getTimeSpan() >= overtime) {
			File file = new File(tickResultsDirectory, "tick-results-" + Util.getFormattedCurrentTime() + ".txt");
			profileResult.save(file.toPath());
			LOGGER.info("Recorded long tick -- wrote info to: {}", file.getAbsolutePath());
		}
	}

	/**
	 * Создаёт монитор, если включён флаг {@link SharedConstants#MONITOR_TICK_TIMES},
	 * иначе возвращает {@code null}.
	 *
	 * @param name имя файла для сохранения результатов
	 * @return монитор или {@code null}
	 */
	public static @Nullable TickDurationMonitor create(String name) {
		return SharedConstants.MONITOR_TICK_TIMES
				? new TickDurationMonitor(Util.nanoTimeSupplier, name, SharedConstants.TICK_OVERTIME_THRESHOLD_NS)
				: null;
	}

	/**
	 * Объединяет переданный профилировщик с профилировщиком монитора (если монитор активен).
	 *
	 * @param profiler базовый профилировщик
	 * @param monitor  монитор или {@code null}
	 * @return объединённый или исходный профилировщик
	 */
	public static Profiler tickProfiler(Profiler profiler, @Nullable TickDurationMonitor monitor) {
		return monitor != null ? Profiler.union(monitor.nextProfiler(), profiler) : profiler;
	}
}
