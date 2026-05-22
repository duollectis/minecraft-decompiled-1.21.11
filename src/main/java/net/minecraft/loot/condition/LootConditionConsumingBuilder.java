package net.minecraft.loot.condition;

import java.util.function.Function;

/**
 * Строитель, поддерживающий добавление условий лута через fluent API.
 *
 * @param <T> тип конкретного строителя (self-referential generic)
 */
public interface LootConditionConsumingBuilder<T extends LootConditionConsumingBuilder<T>> {

	T conditionally(LootCondition.Builder condition);

	T getThisConditionConsumingBuilder();

	default <E> T conditionally(Iterable<E> conditions, Function<E, LootCondition.Builder> toBuilderFunction) {
		T builder = getThisConditionConsumingBuilder();

		for (E element : conditions) {
			builder = builder.conditionally(toBuilderFunction.apply(element));
		}

		return builder;
	}
}
