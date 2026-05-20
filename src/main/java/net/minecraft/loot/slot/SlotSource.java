package net.minecraft.loot.slot;

import com.mojang.serialization.MapCodec;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextAware;

public interface SlotSource extends LootContextAware {
   MapCodec<? extends SlotSource> getCodec();

   ItemStream stream(LootContext context);
}
