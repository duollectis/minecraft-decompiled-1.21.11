package net.minecraft.util.profiler;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * Управляет жизненным циклом {@link ProfilerSystem}: включает и выключает профилирование,
 * предоставляя доступ к текущему профайлеру и результатам.
 */
public class TickTimeTracker {

	private final LongSupplier timeGetter;
	private final IntSupplier tickGetter;
	private final BooleanSupplier timeoutDisabled;
	private ReadableProfiler profiler = DummyProfiler.INSTANCE;

	public TickTimeTracker(LongSupplier timeGetter, IntSupplier tickGetter, BooleanSupplier timeoutDisabled) {
		this.timeGetter = timeGetter;
		this.tickGetter = tickGetter;
		this.timeoutDisabled = timeoutDisabled;
	}

	public boolean isActive() {
		return profiler != DummyProfiler.INSTANCE;
	}

	public void disable() {
		profiler = DummyProfiler.INSTANCE;
	}

	public void enable() {
		profiler = new ProfilerSystem(timeGetter, tickGetter, timeoutDisabled);
	}

	public Profiler getProfiler() {
		return profiler;
	}

	public ProfileResult getResult() {
		return profiler.getResult();
	}
}
