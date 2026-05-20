package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

public class EntityShulkerColorFix extends ChoiceFix {
   public EntityShulkerColorFix(Schema schema, boolean bl) {
      super(schema, bl, "EntityShulkerColorFix", TypeReferences.ENTITY, "minecraft:shulker");
   }

   public Dynamic<?> fixShulkerColor(Dynamic<?> shulkerDynamic) {
      return shulkerDynamic.get("Color").map(Dynamic::asNumber).result().isEmpty()
         ? shulkerDynamic.set("Color", shulkerDynamic.createByte((byte)10))
         : shulkerDynamic;
   }

   @Override
   protected Typed<?> transform(Typed<?> inputTyped) {
      return inputTyped.update(DSL.remainderFinder(), this::fixShulkerColor);
   }
}
