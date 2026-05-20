package net.minecraft.datafixer.fix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

public class RemoveFilteredSignTextFix extends ChoiceWriteReadFix {
   public RemoveFilteredSignTextFix(Schema outputSchema) {
      super(outputSchema, false, "Remove filtered text from signs", TypeReferences.BLOCK_ENTITY, "minecraft:sign");
   }

   @Override
   protected <T> Dynamic<T> transform(Dynamic<T> data) {
      return data.remove("FilteredText1").remove("FilteredText2").remove("FilteredText3").remove("FilteredText4");
   }
}
