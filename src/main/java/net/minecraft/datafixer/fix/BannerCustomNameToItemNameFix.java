package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Optional;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;

public class BannerCustomNameToItemNameFix extends DataFix {
   public BannerCustomNameToItemNameFix(Schema outputSchema) {
      super(outputSchema, false);
   }

   @SuppressWarnings("unchecked")
   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(TypeReferences.BLOCK_ENTITY);
      TaggedChoiceType<?> taggedChoiceType = this.getInputSchema().findChoiceType(TypeReferences.BLOCK_ENTITY);
      OpticFinder<String> opticFinder = (OpticFinder<String>) type.findField("CustomName");
      OpticFinder<Pair<String, String>> opticFinder2 = (OpticFinder<Pair<String, String>>) DSL.typeFinder(this.getInputSchema().getType(TypeReferences.TEXT_COMPONENT));
      return this.fixTypeEverywhereTyped("Banner entity custom_name to item_name component fix", type, typed -> {
         Object object = ((Pair)typed.get(taggedChoiceType.finder())).getFirst();
         return object.equals("minecraft:banner") ? this.fix(typed, opticFinder2, opticFinder) : typed;
      });
   }

   private Typed<?> fix(Typed<?> typed, OpticFinder<Pair<String, String>> opticFinder, OpticFinder<String> opticFinder2) {
      Optional<String> optional = typed.getOptionalTyped(opticFinder2).flatMap(typedx -> typedx.getOptional(opticFinder).map(Pair::getSecond));
      boolean bl = optional.flatMap(TextFixes::getTranslate).filter(name -> name.equals("block.minecraft.ominous_banner")).isPresent();
      return bl
         ? Util.apply(
            typed,
            typed.getType(),
            dynamic -> {
               Dynamic<?> dynamic2 = dynamic.createMap(
                  Map.of(
                     dynamic.createString("minecraft:item_name"),
                     dynamic.createString(optional.get()),
                     dynamic.createString("minecraft:hide_additional_tooltip"),
                     dynamic.emptyMap()
                  )
               );
               return dynamic.set("components", dynamic2).remove("CustomName");
            }
         )
         : typed;
   }
}
