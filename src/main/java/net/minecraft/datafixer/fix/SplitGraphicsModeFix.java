package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

public class SplitGraphicsModeFix extends DataFix {
   private final String optionName;
   private final String fastValue;
   private final String fancyValue;
   private final String fabulousValue;

   public SplitGraphicsModeFix(Schema schema, String optionName, String fastValue, String fancyValue, String fabulousValue) {
      super(schema, true);
      this.optionName = optionName;
      this.fastValue = fastValue;
      this.fancyValue = fancyValue;
      this.fabulousValue = fabulousValue;
   }

   public TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "graphicsMode split to " + this.optionName,
         this.getInputSchema().getType(TypeReferences.OPTIONS),
         typed -> typed.update(
            DSL.remainderFinder(),
            options -> (Dynamic)DataFixUtils.orElseGet(
               options.get("graphicsMode")
                  .asString()
                  .map(graphicsMode -> options.set(this.optionName, options.createString(this.getValueForMode(graphicsMode))))
                  .result(),
               () -> options.set(this.optionName, options.createString(this.fancyValue))
            )
         )
      );
   }

   private String getValueForMode(String graphicsMode) {
      return switch (graphicsMode) {
         case "2" -> this.fabulousValue;
         case "0" -> this.fastValue;
         default -> this.fancyValue;
      };
   }
}
