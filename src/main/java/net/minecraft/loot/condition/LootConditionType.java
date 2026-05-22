package net.minecraft.loot.condition;

import com.mojang.serialization.MapCodec;

/**
 * Тип условия лута, хранящий его {@link MapCodec} для сериализации.
 */
public record LootConditionType(MapCodec<? extends LootCondition> codec) {
}
