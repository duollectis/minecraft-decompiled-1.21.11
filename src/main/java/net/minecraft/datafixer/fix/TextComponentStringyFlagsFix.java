package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Optional;
import net.minecraft.datafixer.TypeReferences;

public class TextComponentStringyFlagsFix extends DataFix {
   public TextComponentStringyFlagsFix(Schema outputSchema) {
      super(outputSchema, false);
   }

   @SuppressWarnings("unchecked")
   protected TypeRewriteRule makeRule() {
      Type<Pair<String, Either<?, Pair<?, Pair<?, Pair<?, Dynamic<?>>>>>>> type = (Type<Pair<String, Either<?, Pair<?, Pair<?, Pair<?, Dynamic<?>>>>>>>)this.getInputSchema().getType(TypeReferences.TEXT_COMPONENT);
      return this.fixTypeEverywhere(
         "TextComponentStringyFlagsFix",
         type,
         dynamicOps -> pair -> pair.mapSecond(
            either -> either.mapRight(
               pairx -> pairx.mapSecond(
                  pairxx -> pairxx.mapSecond(
                     pairxxx -> pairxxx.mapSecond(
                        dynamic -> dynamic.update("bold", TextComponentStringyFlagsFix::method_66136)
                           .update("italic", TextComponentStringyFlagsFix::method_66136)
                           .update("underlined", TextComponentStringyFlagsFix::method_66136)
                           .update("strikethrough", TextComponentStringyFlagsFix::method_66136)
                           .update("obfuscated", TextComponentStringyFlagsFix::method_66136)
                     )
                  )
               )
            )
         )
      );
   }

   private static <T> Dynamic<T> method_66136(Dynamic<T> dynamic) {
      Optional<String> optional = dynamic.asString().result();
      return optional.isPresent() ? dynamic.createBoolean(Boolean.parseBoolean(optional.get())) : dynamic;
   }
}
