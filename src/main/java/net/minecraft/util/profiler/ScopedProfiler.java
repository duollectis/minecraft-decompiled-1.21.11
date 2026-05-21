package net.minecraft.util.profiler;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * {@code ScopedProfiler}.
 */
public class ScopedProfiler implements AutoCloseable {

	public static final ScopedProfiler DUMMY = new ScopedProfiler(null);
	private final @Nullable Profiler wrapped;

	ScopedProfiler(@Nullable Profiler wrapped) {
		this.wrapped = wrapped;
	}

	/**
	 * Добавляет label.
	 *
	 * @param label label
	 *
	 * @return ScopedProfiler — результат операции
	 */
	public ScopedProfiler addLabel(String label) {
		if (this.wrapped != null) {
			this.wrapped.addZoneText(label);
		}

		return this;
	}

	/**
	 * Добавляет label.
	 *
	 * @param labelSupplier label supplier
	 *
	 * @return ScopedProfiler — результат операции
	 */
	public ScopedProfiler addLabel(Supplier<String> labelSupplier) {
		if (this.wrapped != null) {
			this.wrapped.addZoneText(labelSupplier.get());
		}

		return this;
	}

	/**
	 * Добавляет value.
	 *
	 * @param value value
	 *
	 * @return ScopedProfiler — результат операции
	 */
	public ScopedProfiler addValue(long value) {
		if (this.wrapped != null) {
			this.wrapped.addZoneValue(value);
		}

		return this;
	}

	public ScopedProfiler setColor(int color) {
		if (this.wrapped != null) {
			this.wrapped.setZoneColor(color);
		}

		return this;
	}

	@Override
	public void close() {
		if (this.wrapped != null) {
			this.wrapped.pop();
		}
	}
}
