package net.minecraft.loot.provider.number;

import com.mojang.serialization.MapCodec;

/**
 * Тип провайдера числового значения, связывающий реализацию {@link LootNumberProvider}
 * с её {@link MapCodec} для сериализации через реестр.
 */
public record LootNumberProviderType(MapCodec<? extends LootNumberProvider> codec) {
}
