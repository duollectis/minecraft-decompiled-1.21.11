package net.minecraft.data.loottable;

import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;

import java.util.function.BiConsumer;

@FunctionalInterface
/**
 * {@code LootTableGenerator}.
 */
public interface LootTableGenerator {

	void accept(BiConsumer<RegistryKey<LootTable>, LootTable.Builder> lootTableBiConsumer);
}
