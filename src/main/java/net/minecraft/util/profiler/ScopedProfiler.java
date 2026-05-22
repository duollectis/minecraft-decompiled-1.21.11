package net.minecraft.util.profiler;

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Обёртка над {@link Profiler}, реализующая {@link AutoCloseable} для использования
 * в try-with-resources. При закрытии автоматически вызывает {@link Profiler#pop()}.
 */
public class ScopedProfiler implements AutoCloseable {

	public static final ScopedProfiler DUMMY = new ScopedProfiler(null);

	private final @Nullable Profiler wrapped;

	ScopedProfiler(@Nullable Profiler wrapped) {
		this.wrapped = wrapped;
	}

	public ScopedProfiler addLabel(String label) {
		if (wrapped != null) {
			wrapped.addZoneText(label);
		}

		return this;
	}

	public ScopedProfiler addLabel(Supplier<String> labelSupplier) {
		if (wrapped != null) {
			wrapped.addZoneText(labelSupplier.get());
		}

		return this;
	}

	public ScopedProfiler addValue(long value) {
		if (wrapped != null) {
			wrapped.addZoneValue(value);
		}

		return this;
	}

	public ScopedProfiler setColor(int color) {
		if (wrapped != null) {
			wrapped.setZoneColor(color);
		}

		return this;
	}

	@Override
	public void close() {
		if (wrapped != null) {
			wrapped.pop();
		}
	}
}
