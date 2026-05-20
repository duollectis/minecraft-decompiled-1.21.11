package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.datafixer.TypeReferences;

public class Schema2505 extends IdentifierNormalizingSchema {
   public Schema2505(int i, Schema schema) {
      super(i, schema);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
      schema.register(map, "minecraft:piglin", () -> DSL.optionalFields("Inventory", DSL.list(TypeReferences.ITEM_STACK.in(schema))));
      return map;
   }
}
