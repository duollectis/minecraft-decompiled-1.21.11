package net.minecraft.util.function;

import java.util.function.Consumer;

/**
 * «Ленивый» потребитель для итерации: после каждого вызова {@link #accept} возвращает
 * {@link NextIteration}, позволяя прервать обход коллекции досрочно без исключений.
 */
@FunctionalInterface
public interface LazyIterationConsumer<T> {

	NextIteration accept(T value);

	static <T> LazyIterationConsumer<T> forConsumer(Consumer<T> consumer) {
		return value -> {
			consumer.accept(value);
			return NextIteration.CONTINUE;
		};
	}

	enum NextIteration {
		CONTINUE,
		ABORT;

		public boolean shouldAbort() {
			return this == ABORT;
		}
	}
}
