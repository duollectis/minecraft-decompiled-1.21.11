package net.minecraft.loot;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;

import java.util.function.Consumer;

/**
 * {@code LootChoice}.
 */
public interface LootChoice {

	int getWeight(float luck);

	void generateLoot(Consumer<ItemStack> lootConsumer, LootContext context);
}
