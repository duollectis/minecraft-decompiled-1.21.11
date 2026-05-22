package net.minecraft.util.profiler;

import com.mojang.jtracy.TracyClient;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Утилитарный класс для управления текущим активным профайлером в рамках потока.
 * Поддерживает интеграцию с Tracy и встроенным профайлером через thread-local хранилище.
 */
public final class Profilers {

	private static final ThreadLocal<TracyProfiler> TRACY_PROFILER = ThreadLocal.withInitial(TracyProfiler::new);
	private static final ThreadLocal<@Nullable Profiler> BUILTIN_PROFILER = new ThreadLocal<>();
	private static final AtomicInteger ACTIVE_BUILTIN_PROFILER_COUNT = new AtomicInteger();

	private Profilers() {
	}

	public static Profilers.Scoped using(Profiler profiler) {
		activate(profiler);
		return Profilers::deactivate;
	}

	private static void activate(Profiler profiler) {
		if (BUILTIN_PROFILER.get() != null) {
			throw new IllegalStateException("Profiler is already active");
		}

		Profiler combined = union(profiler);
		BUILTIN_PROFILER.set(combined);
		ACTIVE_BUILTIN_PROFILER_COUNT.incrementAndGet();
		combined.startTick();
	}

	private static void deactivate() {
		Profiler profiler = BUILTIN_PROFILER.get();
		if (profiler == null) {
			throw new IllegalStateException("Profiler was not active");
		}

		BUILTIN_PROFILER.remove();
		ACTIVE_BUILTIN_PROFILER_COUNT.decrementAndGet();
		profiler.endTick();
	}

	private static Profiler union(Profiler builtinProfiler) {
		return Profiler.union(getDefault(), builtinProfiler);
	}

	/**
	 * Возвращает активный профайлер для текущего потока.
	 * Если встроенный профайлер не активен — возвращает Tracy или заглушку.
	 */
	public static Profiler get() {
		return ACTIVE_BUILTIN_PROFILER_COUNT.get() == 0
			? getDefault()
			: Objects.requireNonNullElseGet(BUILTIN_PROFILER.get(), Profilers::getDefault);
	}

	private static Profiler getDefault() {
		return TracyClient.isAvailable() ? TRACY_PROFILER.get() : DummyProfiler.INSTANCE;
	}

	/**
	 * Маркер автоматического закрытия области профилирования (try-with-resources).
	 */
	public interface Scoped extends AutoCloseable {

		@Override
		void close();
	}
}
