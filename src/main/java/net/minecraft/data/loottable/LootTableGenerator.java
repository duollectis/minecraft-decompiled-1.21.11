package net.minecraft.data.loottable;

import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;

import java.util.function.BiConsumer;

/**
 * Функциональный интерфейс генератора таблиц лута.
 * Реализации принимают {@link BiConsumer}, которому передают пары ключ реестра → строитель таблицы.
 */
@FunctionalInterface
public interface LootTableGenerator {

	void accept(BiConsumer<RegistryKey<LootTable>, LootTable.Builder> lootTableBiConsumer);
}
