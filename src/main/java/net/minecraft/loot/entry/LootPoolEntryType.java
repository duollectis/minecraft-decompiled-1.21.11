package net.minecraft.loot.entry;

import com.mojang.serialization.MapCodec;

/**
 * {@code LootPoolEntryType}.
 */
public record LootPoolEntryType(MapCodec<? extends LootPoolEntry> codec) {
}
