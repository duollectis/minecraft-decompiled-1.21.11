package net.minecraft.util.profiler;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * {@code TickTimeTracker}.
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
		return this.profiler != DummyProfiler.INSTANCE;
	}

	/**
	 * Disable.
	 */
	public void disable() {
		this.profiler = DummyProfiler.INSTANCE;
	}

	/**
	 * Enable.
	 */
	public void enable() {
		this.profiler = new ProfilerSystem(this.timeGetter, this.tickGetter, this.timeoutDisabled);
	}

	public Profiler getProfiler() {
		return this.profiler;
	}

	public ProfileResult getResult() {
		return this.profiler.getResult();
	}
}
