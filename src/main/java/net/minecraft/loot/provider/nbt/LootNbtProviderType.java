package net.minecraft.loot.provider.nbt;

import com.mojang.serialization.MapCodec;

/**
 * Тип провайдера NBT-данных, связывающий реализацию {@link LootNbtProvider}
 * с её {@link MapCodec} для сериализации через реестр.
 */
public record LootNbtProviderType(MapCodec<? extends LootNbtProvider> codec) {
}
