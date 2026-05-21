package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;

/**
 * {@code LootFunctionType}.
 */
public record LootFunctionType<T extends LootFunction>(MapCodec<T> codec) {
}
