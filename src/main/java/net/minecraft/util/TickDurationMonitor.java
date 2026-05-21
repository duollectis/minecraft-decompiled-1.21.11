package net.minecraft.util;

import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.util.profiler.*;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.util.function.LongSupplier;

/**
 * {@code TickDurationMonitor}.
 */
public class TickDurationMonitor {

	private static final Logger LOGGER = LogUtils.getLogger();
	private final LongSupplier timeGetter;
	private final long overtime;
	private int tickCount;
	private final File tickResultsDirectory;
	private ReadableProfiler profiler = DummyProfiler.INSTANCE;

	public TickDurationMonitor(LongSupplier timeGetter, String filename, long overtime) {
		this.timeGetter = timeGetter;
		this.tickResultsDirectory = new File("debug", filename);
		this.overtime = overtime;
	}

	/**
	 * Next profiler.
	 *
	 * @return Profiler — результат операции
	 */
	public Profiler nextProfiler() {
		this.profiler = new ProfilerSystem(this.timeGetter, () -> this.tickCount, () -> true);
		this.tickCount++;
		return this.profiler;
	}

	/**
	 * End tick.
	 */
	public void endTick() {
		if (this.profiler != DummyProfiler.INSTANCE) {
			ProfileResult profileResult = this.profiler.getResult();
			this.profiler = DummyProfiler.INSTANCE;
			if (profileResult.getTimeSpan() >= this.overtime) {
				File
						file =
						new File(this.tickResultsDirectory, "tick-results-" + Util.getFormattedCurrentTime() + ".txt");
				profileResult.save(file.toPath());
				LOGGER.info("Recorded long tick -- wrote info to: {}", file.getAbsolutePath());
			}
		}
	}

	/**
	 * Create.
	 *
	 * @param name name
	 *
	 * @return @Nullable TickDurationMonitor — результат операции
	 */
	public static @Nullable TickDurationMonitor create(String name) {
		return SharedConstants.MONITOR_TICK_TIMES ? new TickDurationMonitor(
				Util.nanoTimeSupplier,
				name,
				SharedConstants.TICK_OVERTIME_THRESHOLD_NS
		) : null;
	}

	/**
	 * Выполняет тик обновления для profiler.
	 *
	 * @param profiler profiler
	 * @param monitor monitor
	 *
	 * @return Profiler — результат операции
	 */
	public static Profiler tickProfiler(Profiler profiler, @Nullable TickDurationMonitor monitor) {
		return monitor != null ? Profiler.union(monitor.nextProfiler(), profiler) : profiler;
	}
}
