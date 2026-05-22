package net.minecraft.util.profiler;

import java.util.function.Supplier;

/**
 * Основной интерфейс профайлера, отслеживающего иерархические секции выполнения
 * по принципу стека push/pop. Поддерживает интеграцию с Tracy через зоны и маркеры.
 */
public interface Profiler {

	String ROOT_NAME = "root";

	void startTick();

	void endTick();

	void push(String location);

	void push(Supplier<String> locationGetter);

	void pop();

	void swap(String location);

	void swap(Supplier<String> locationGetter);

	default void addZoneText(String label) {
	}

	default void addZoneValue(long value) {
	}

	default void setZoneColor(int color) {
	}

	default ScopedProfiler scoped(String name) {
		push(name);
		return new ScopedProfiler(this);
	}

	default ScopedProfiler scoped(Supplier<String> nameSupplier) {
		push(nameSupplier);
		return new ScopedProfiler(this);
	}

	void markSampleType(SampleType type);

	default void visit(String marker) {
		visit(marker, 1);
	}

	void visit(String marker, int num);

	default void visit(Supplier<String> markerGetter) {
		visit(markerGetter, 1);
	}

	void visit(Supplier<String> markerGetter, int num);

	/**
	 * Объединяет два профайлера в один: вызовы делегируются обоим.
	 * Если один из них — {@link DummyProfiler}, возвращается другой без обёртки.
	 */
	static Profiler union(Profiler first, Profiler second) {
		if (first == DummyProfiler.INSTANCE) {
			return second;
		}

		return second == DummyProfiler.INSTANCE ? first : new Profiler.UnionProfiler(first, second);
	}

	/**
	 * Составной профайлер, транслирующий все вызовы двум вложенным профайлерам одновременно.
	 * Используется для параллельного профилирования (например, системный + Tracy).
	 */
	class UnionProfiler implements Profiler {

		private final Profiler first;
		private final Profiler second;

		public UnionProfiler(Profiler first, Profiler second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public void startTick() {
			first.startTick();
			second.startTick();
		}

		@Override
		public void endTick() {
			first.endTick();
			second.endTick();
		}

		@Override
		public void push(String location) {
			first.push(location);
			second.push(location);
		}

		@Override
		public void push(Supplier<String> locationGetter) {
			first.push(locationGetter);
			second.push(locationGetter);
		}

		@Override
		public void markSampleType(SampleType type) {
			first.markSampleType(type);
			second.markSampleType(type);
		}

		@Override
		public void pop() {
			first.pop();
			second.pop();
		}

		@Override
		public void swap(String location) {
			first.swap(location);
			second.swap(location);
		}

		@Override
		public void swap(Supplier<String> locationGetter) {
			first.swap(locationGetter);
			second.swap(locationGetter);
		}

		@Override
		public void visit(String marker, int num) {
			first.visit(marker, num);
			second.visit(marker, num);
		}

		@Override
		public void visit(Supplier<String> markerGetter, int num) {
			first.visit(markerGetter, num);
			second.visit(markerGetter, num);
		}

		@Override
		public void addZoneText(String label) {
			first.addZoneText(label);
			second.addZoneText(label);
		}

		@Override
		public void addZoneValue(long value) {
			first.addZoneValue(value);
			second.addZoneValue(value);
		}

		@Override
		public void setZoneColor(int color) {
			first.setZoneColor(color);
			second.setZoneColor(color);
		}
	}
}
