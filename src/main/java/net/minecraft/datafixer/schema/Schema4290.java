package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.datafixer.TypeReferences;

public class Schema4290 extends IdentifierNormalizingSchema {
   public Schema4290(int i, Schema schema) {
      super(i, schema);
   }

   public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> map, Map<String, Supplier<TypeTemplate>> map2) {
      super.registerTypes(schema, map, map2);
      schema.registerType(
         true,
         TypeReferences.TEXT_COMPONENT,
         () -> DSL.or(
            DSL.or(DSL.constType(DSL.string()), DSL.list(TypeReferences.TEXT_COMPONENT.in(schema))),
            DSL.optionalFields(
               "extra",
               DSL.list(TypeReferences.TEXT_COMPONENT.in(schema)),
               "separator",
               TypeReferences.TEXT_COMPONENT.in(schema),
               "hoverEvent",
               DSL.taggedChoice(
                  "action",
                  DSL.string(),
                  Map.of(
                     "show_text",
                     DSL.optionalFields("contents", TypeReferences.TEXT_COMPONENT.in(schema)),
                     "show_item",
                     DSL.optionalFields("contents", DSL.or(TypeReferences.ITEM_STACK.in(schema), TypeReferences.ITEM_NAME.in(schema))),
                     "show_entity",
                     DSL.optionalFields("type", TypeReferences.ENTITY_NAME.in(schema), "name", TypeReferences.TEXT_COMPONENT.in(schema))
                  )
               )
            )
         )
      );
   }
}
