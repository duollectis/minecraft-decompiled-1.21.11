package net.minecraft.loot.provider.score;

import com.mojang.serialization.MapCodec;

/**
 * Тип провайдера очков таблицы лута, хранящий {@link MapCodec} для сериализации
 * конкретной реализации {@link LootScoreProvider}.
 */
public record LootScoreProviderType(MapCodec<? extends LootScoreProvider> codec) {
}
