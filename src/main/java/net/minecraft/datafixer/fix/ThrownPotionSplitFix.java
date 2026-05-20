package net.minecraft.datafixer.fix;

import com.google.common.base.Suppliers;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.function.Supplier;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;

public class ThrownPotionSplitFix extends EntityTransformFix {
   private final Supplier<ThrownPotionSplitFix.class_10681> field_56251 = Suppliers.memoize(
      () -> {
         Type<?> type = this.getInputSchema().getChoiceType(TypeReferences.ENTITY, "minecraft:potion");
         Type<?> type2 = FixUtil.withTypeChanged(
            type, this.getInputSchema().getType(TypeReferences.ENTITY), this.getOutputSchema().getType(TypeReferences.ENTITY)
         );
         OpticFinder<?> opticFinder = type2.findField("Item");
         OpticFinder<Pair<String, String>> opticFinder2 = DSL.fieldFinder(
            "id", DSL.named(TypeReferences.ITEM_NAME.typeName(), IdentifierNormalizingSchema.getIdentifierType())
         );
         return new ThrownPotionSplitFix.class_10681(opticFinder, opticFinder2);
      }
   );

   public ThrownPotionSplitFix(Schema schema) {
      super("ThrownPotionSplitFix", schema, true);
   }

   @Override
   protected Pair<String, Typed<?>> transform(String choice, Typed<?> entityTyped) {
      if (!choice.equals("minecraft:potion")) {
         return Pair.of(choice, entityTyped);
      } else {
         String string = this.field_56251.get().method_67102(entityTyped);
         return "minecraft:lingering_potion".equals(string)
            ? Pair.of("minecraft:lingering_potion", entityTyped)
            : Pair.of("minecraft:splash_potion", entityTyped);
      }
   }

   record class_10681(OpticFinder<?> itemFinder, OpticFinder<Pair<String, String>> itemIdFinder) {
      public String method_67102(Typed<?> typed) {
         return typed.getOptionalTyped(this.itemFinder)
            .flatMap(typedx -> typedx.getOptional(this.itemIdFinder))
            .<String>map(Pair::getSecond)
            .map(IdentifierNormalizingSchema::normalize)
            .orElse("");
      }
   }
}
