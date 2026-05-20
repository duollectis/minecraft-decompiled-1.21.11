package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.datafixer.TypeReferences;

public class AreaEffectCloudDurationScaleFix extends ChoiceFix {
   public AreaEffectCloudDurationScaleFix(Schema outputSchema) {
      super(outputSchema, false, "AreaEffectCloudDurationScaleFix", TypeReferences.ENTITY, "minecraft:area_effect_cloud");
   }

   @Override
   protected Typed<?> transform(Typed<?> inputTyped) {
      return inputTyped.update(DSL.remainderFinder(), dynamic -> dynamic.set("potion_duration_scale", dynamic.createFloat(0.25F)));
   }
}
