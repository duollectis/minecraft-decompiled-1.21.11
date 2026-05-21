package net.minecraft.util.function;

import java.util.function.Consumer;

@FunctionalInterface
/**
 * {@code LazyIterationConsumer}.
 */
public interface LazyIterationConsumer<T> {

	LazyIterationConsumer.NextIteration accept(T value);

	static <T> LazyIterationConsumer<T> forConsumer(Consumer<T> consumer) {
		return value -> {
			consumer.accept(value);
			return LazyIterationConsumer.NextIteration.CONTINUE;
		};
	}

	/**
	 * {@code NextIteration}.
	 */
	public static enum NextIteration {
		CONTINUE,
		ABORT;

		public boolean shouldAbort() {
			return this == ABORT;
		}
	}
}
