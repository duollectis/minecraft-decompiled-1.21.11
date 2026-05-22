package net.minecraft.loot.slot;

import com.mojang.serialization.MapCodec;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextAware;

/**
 * Источник слотов инвентаря для системы лута.
 * Предоставляет ленивый поток копий предметов ({@link ItemStream}) в заданном контексте лута.
 */
public interface SlotSource extends LootContextAware {

	MapCodec<? extends SlotSource> getCodec();

	ItemStream stream(LootContext context);
}
