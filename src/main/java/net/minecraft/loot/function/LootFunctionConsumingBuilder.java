package net.minecraft.loot.function;

import java.util.Arrays;
import java.util.function.Function;

/** Строитель, поддерживающий последовательное применение функций лута. */
public interface LootFunctionConsumingBuilder<T extends LootFunctionConsumingBuilder<T>> {

	T apply(LootFunction.Builder function);

	default <E> T apply(Iterable<E> functions, Function<E, LootFunction.Builder> toBuilderFunction) {
		T builder = getThisFunctionConsumingBuilder();

		for (E element : functions) {
			builder = builder.apply(toBuilderFunction.apply(element));
		}

		return builder;
	}

	default <E> T apply(E[] functions, Function<E, LootFunction.Builder> toBuilderFunction) {
		return apply(Arrays.asList(functions), toBuilderFunction);
	}

	T getThisFunctionConsumingBuilder();
}
