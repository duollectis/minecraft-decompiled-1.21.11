package net.minecraft.loot;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;

import java.util.function.Consumer;

/**
 * Один вариант выбора в лут-пуле с весом и логикой генерации предметов.
 *
 * <p>Вес определяет вероятность выбора данного варианта относительно других
 * в пуле. Чем выше удача ({@code luck}), тем больший вес может получить вариант.</p>
 */
public interface LootChoice {

	int getWeight(float luck);

	void generateLoot(Consumer<ItemStack> lootConsumer, LootContext context);
}
