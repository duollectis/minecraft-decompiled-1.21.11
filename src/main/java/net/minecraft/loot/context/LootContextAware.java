package net.minecraft.loot.context;

import net.minecraft.loot.LootTableReporter;
import net.minecraft.util.context.ContextParameter;

import java.util.Set;

/** Интерфейс для объектов, осведомлённых о контексте лута и поддерживающих валидацию. */
public interface LootContextAware {

	default Set<ContextParameter<?>> getAllowedParameters() {
		return Set.of();
	}

	default void validate(LootTableReporter reporter) {
		reporter.validateContext(this);
	}
}
