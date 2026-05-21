package net.minecraft.loot.condition;

import com.mojang.serialization.MapCodec;

/**
 * {@code LootConditionType}.
 */
public record LootConditionType(MapCodec<? extends LootCondition> codec) {
}
