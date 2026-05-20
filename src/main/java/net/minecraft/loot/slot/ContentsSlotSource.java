package net.minecraft.loot.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.loot.ContainerComponentModifier;
import net.minecraft.loot.ContainerComponentModifiers;

public class ContentsSlotSource extends TransformSlotSource {
   public static final MapCodec<ContentsSlotSource> CODEC = RecordCodecBuilder.mapCodec(
      instance -> addSlotSourceField(instance)
         .and(ContainerComponentModifiers.MODIFIER_CODEC.fieldOf("component").forGetter(source -> source.component))
         .apply(instance, ContentsSlotSource::new)
   );
   private final ContainerComponentModifier<?> component;

   private ContentsSlotSource(SlotSource slotSource, ContainerComponentModifier<?> component) {
      super(slotSource);
      this.component = component;
   }

   @Override
   public MapCodec<ContentsSlotSource> getCodec() {
      return CODEC;
   }

   @Override
   protected ItemStream transform(ItemStream stream) {
      return stream.map(this.component::stream);
   }
}
