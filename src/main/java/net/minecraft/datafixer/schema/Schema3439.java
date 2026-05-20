package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.datafixer.TypeReferences;

public class Schema3439 extends IdentifierNormalizingSchema {
   public Schema3439(int i, Schema schema) {
      super(i, schema);
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
      this.register(map, "minecraft:sign", () -> method_66179(schema));
      return map;
   }

   public static TypeTemplate method_66179(Schema schema) {
      return DSL.optionalFields(
         "front_text",
         DSL.optionalFields(
            "messages", DSL.list(TypeReferences.TEXT_COMPONENT.in(schema)), "filtered_messages", DSL.list(TypeReferences.TEXT_COMPONENT.in(schema))
         ),
         "back_text",
         DSL.optionalFields(
            "messages", DSL.list(TypeReferences.TEXT_COMPONENT.in(schema)), "filtered_messages", DSL.list(TypeReferences.TEXT_COMPONENT.in(schema))
         )
      );
   }
}
