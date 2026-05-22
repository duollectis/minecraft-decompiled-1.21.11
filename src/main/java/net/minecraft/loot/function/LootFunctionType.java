package net.minecraft.loot.function;

import com.mojang.serialization.MapCodec;

/**
 * Тип функции лута, связывающий конкретную реализацию {@link LootFunction}
 * с её {@link MapCodec} для сериализации и десериализации через реестр.
 */
public record LootFunctionType<T extends LootFunction>(MapCodec<T> codec) {
}
