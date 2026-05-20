package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.util.Util;

public abstract class ChoiceWriteReadFix extends DataFix {
   private final String name;
   private final String choiceName;
   private final TypeReference type;

   public ChoiceWriteReadFix(Schema schema, boolean bl, String string, TypeReference typeReference, String string2) {
      super(schema, bl);
      this.name = string;
      this.type = typeReference;
      this.choiceName = string2;
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(this.type);
      Type<?> type2 = this.getInputSchema().getChoiceType(this.type, this.choiceName);
      Type<?> type3 = this.getOutputSchema().getType(this.type);
      OpticFinder<?> opticFinder = DSL.namedChoice(this.choiceName, type2);
      Type<?> type4 = FixUtil.withTypeChanged(type, type, type3);
      return this.makeRule(type, type3, type4, opticFinder);
   }

   private <S, T, A> TypeRewriteRule makeRule(Type<S> type, Type<T> outputType, Type<?> type2, OpticFinder<A> opticFinder) {
      return this.fixTypeEverywhereTyped(this.name, type, outputType, typed -> {
         if (typed.getOptional(opticFinder).isEmpty()) {
            return FixUtil.withType(outputType, typed);
         } else {
            Typed<?> typed2 = FixUtil.withType(type2, typed);
            return Util.apply((Typed<A>)typed2, outputType, this::transform);
         }
      });
   }

   protected abstract <T> Dynamic<T> transform(Dynamic<T> data);
}
