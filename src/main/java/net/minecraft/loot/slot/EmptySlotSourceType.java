package net.minecraft.loot.slot;

import com.mojang.serialization.MapCodec;
import net.minecraft.loot.context.LootContext;

public record EmptySlotSourceType() implements SlotSource {
   public static final MapCodec<EmptySlotSourceType> CODEC = MapCodec.unit(new EmptySlotSourceType());

   @Override
   public MapCodec<EmptySlotSourceType> getCodec() {
      return CODEC;
   }

   @Override
   public ItemStream stream(LootContext context) {
      return ItemStream.EMPTY;
   }
}
