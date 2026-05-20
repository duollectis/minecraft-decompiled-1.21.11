package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.IdentifierNormalizingSchema;
import net.minecraft.util.Util;

public class EntityCustomNameToTextFix extends DataFix {
   public EntityCustomNameToTextFix(Schema schema) {
      super(schema, true);
   }

   @SuppressWarnings("unchecked")
   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(TypeReferences.ENTITY);
      Type<?> type2 = this.getOutputSchema().getType(TypeReferences.ENTITY);
      OpticFinder<String> opticFinder = DSL.fieldFinder("id", IdentifierNormalizingSchema.getIdentifierType());
      OpticFinder<String> opticFinder2 = (OpticFinder<String>) type.findField("CustomName");
      Type<?> type3 = type2.findFieldType("CustomName");
      return this.fixTypeEverywhereTyped("EntityCustomNameToComponentFix", type, type2, typed -> method_66064(typed, type2, opticFinder, opticFinder2, type3));
   }

   private static <T> Typed<?> method_66064(Typed<?> typed, Type<?> type, OpticFinder<String> opticFinder, OpticFinder<String> opticFinder2, Type<T> type2) {
      Optional<String> optional = typed.getOptional(opticFinder2);
      if (optional.isEmpty()) {
         return FixUtil.withType(type, (Typed<T>)typed);
      } else if (optional.get().isEmpty()) {
         return Util.apply(typed, type, dynamicx -> dynamicx.remove("CustomName"));
      } else {
         String string = typed.getOptional(opticFinder).orElse("");
         Dynamic<?> dynamic = method_66066(typed.getOps(), optional.get(), string);
         return typed.set(opticFinder2, Util.readTyped(type2, dynamic));
      }
   }

   private static <T> Dynamic<T> method_66066(DynamicOps<T> dynamicOps, String string, String string2) {
      return "minecraft:commandblock_minecart".equals(string2) ? new Dynamic(dynamicOps, dynamicOps.createString(string)) : TextFixes.text(dynamicOps, string);
   }
}
