package net.minecraft.loot.provider.number;

import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextAware;

/** Провайдер числового значения для лут-таблиц, поддерживающий как целые, так и вещественные числа. */
public interface LootNumberProvider extends LootContextAware {

	float nextFloat(LootContext context);

	default int nextInt(LootContext context) {
		return Math.round(nextFloat(context));
	}

	LootNumberProviderType getType();
}
