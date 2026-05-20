package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.datafixer.TypeReferences;

public class Schema3813 extends IdentifierNormalizingSchema {
   public Schema3813(int i, Schema schema) {
      super(i, schema);
   }

   public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> map, Map<String, Supplier<TypeTemplate>> map2) {
      super.registerTypes(schema, map, map2);
      schema.registerType(
         false,
         TypeReferences.SAVED_DATA_MAP_DATA,
         () -> DSL.optionalFields("data", DSL.optionalFields("banners", DSL.list(DSL.optionalFields("name", TypeReferences.TEXT_COMPONENT.in(schema)))))
      );
   }
}
