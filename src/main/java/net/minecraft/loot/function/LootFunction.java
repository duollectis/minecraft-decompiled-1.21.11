package net.minecraft.loot.function;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextAware;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * {@code LootFunction}.
 */
public interface LootFunction extends LootContextAware, BiFunction<ItemStack, LootContext, ItemStack> {

	LootFunctionType<? extends LootFunction> getType();

	static Consumer<ItemStack> apply(
			BiFunction<ItemStack, LootContext, ItemStack> itemApplier,
			Consumer<ItemStack> lootConsumer,
			LootContext context
	) {
		return stack -> lootConsumer.accept(itemApplier.apply(stack, context));
	}

	/**
	 * {@code Builder}.
	 */
	public interface Builder {

		LootFunction build();
	}
}
