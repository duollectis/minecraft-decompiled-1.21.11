package net.minecraft.loot.provider.number;

import com.mojang.serialization.MapCodec;

/**
 * {@code LootNumberProviderType}.
 */
public record LootNumberProviderType(MapCodec<? extends LootNumberProvider> codec) {
}
