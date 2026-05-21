package net.minecraft.loot.provider.nbt;

import com.mojang.serialization.MapCodec;

/**
 * {@code LootNbtProviderType}.
 */
public record LootNbtProviderType(MapCodec<? extends LootNbtProvider> codec) {
}
