package net.minecraft.loot.entry;

import com.mojang.serialization.MapCodec;

/** Тип записи пула лута, хранящий её {@link MapCodec} для сериализации. */
public record LootPoolEntryType(MapCodec<? extends LootPoolEntry> codec) {
}
