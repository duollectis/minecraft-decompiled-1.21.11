package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import org.jspecify.annotations.Nullable;

public class TridentAnimationFix extends ComponentFix {
   public TridentAnimationFix(Schema schema) {
      super(schema, "TridentAnimationFix", "minecraft:consumable");
   }

   @Override
   protected <T> @Nullable Dynamic<T> fixComponent(Dynamic<T> dynamic) {
      return dynamic.update("animation", value -> {
         String string = value.asString().result().orElse("");
         return "spear".equals(string) ? value.createString("trident") : value;
      });
   }
}
