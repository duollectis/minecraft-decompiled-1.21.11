package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice.TaggedChoiceType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.util.Util;

public abstract class EntityTransformFix extends DataFix {
   protected final String name;

   public EntityTransformFix(String name, Schema outputSchema, boolean changesType) {
      super(outputSchema, changesType);
      this.name = name;
   }

   @SuppressWarnings("unchecked")
   public TypeRewriteRule makeRule() {
      TaggedChoiceType<String> taggedChoiceType = (TaggedChoiceType<String>) this.getInputSchema().findChoiceType(TypeReferences.ENTITY);
      TaggedChoiceType<String> taggedChoiceType2 = (TaggedChoiceType<String>) this.getOutputSchema().findChoiceType(TypeReferences.ENTITY);
      Function<String, Type<?>> function = Util.memoize(string -> {
         Type<?> type = (Type<?>)taggedChoiceType.types().get(string);
         return FixUtil.withTypeChanged(type, taggedChoiceType, taggedChoiceType2);
      });
      return this.fixTypeEverywhere(
         this.name,
         taggedChoiceType,
         taggedChoiceType2,
         dynamicOps -> pair -> {
            String string = (String)pair.getFirst();
            Type<?> type = function.apply(string);
            Pair<String, Typed<?>> pair2 = this.transform(string, this.makeTyped(pair.getSecond(), dynamicOps, type));
            Type<?> type2 = (Type<?>)taggedChoiceType2.types().get(pair2.getFirst());
            if (!type2.equals(((Typed)pair2.getSecond()).getType(), true, true)) {
               throw new IllegalStateException(
                  String.format(Locale.ROOT, "Dynamic type check failed: %s not equal to %s", type2, ((Typed)pair2.getSecond()).getType())
               );
            } else {
               return Pair.of((String)pair2.getFirst(), ((Typed)pair2.getSecond()).getValue());
            }
         }
      );
   }

   private <A> Typed<A> makeTyped(Object object, DynamicOps<?> dynamicOps, Type<A> type) {
      return new Typed(type, dynamicOps, object);
   }

   protected abstract Pair<String, Typed<?>> transform(String choice, Typed<?> entityTyped);
}
