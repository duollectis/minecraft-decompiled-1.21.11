package net.minecraft.loot.entry;

import net.minecraft.loot.LootChoice;
import net.minecraft.loot.context.LootContext;

import java.util.Objects;
import java.util.function.Consumer;

/** Функциональный интерфейс для комбинирования записей пула лута с поддержкой AND/OR. */
@FunctionalInterface
interface EntryCombiner {

	EntryCombiner ALWAYS_FALSE = (context, choiceConsumer) -> false;
	EntryCombiner ALWAYS_TRUE = (context, choiceConsumer) -> true;

	boolean expand(LootContext context, Consumer<LootChoice> choiceConsumer);

	default EntryCombiner and(EntryCombiner other) {
		Objects.requireNonNull(other);
		return (context, choiceConsumer) -> expand(context, choiceConsumer) && other.expand(context, choiceConsumer);
	}

	default EntryCombiner or(EntryCombiner other) {
		Objects.requireNonNull(other);
		return (context, choiceConsumer) -> expand(context, choiceConsumer) || other.expand(context, choiceConsumer);
	}
}
