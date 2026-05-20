package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.datafixer.TypeReferences;

public class Schema4300 extends IdentifierNormalizingSchema {
   public Schema4300(int i, Schema schema) {
      super(i, schema);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
      schema.register(map, "minecraft:llama", string -> method_66634(schema));
      schema.register(map, "minecraft:trader_llama", string -> method_66634(schema));
      schema.register(map, "minecraft:donkey", string -> method_66634(schema));
      schema.register(map, "minecraft:mule", string -> method_66634(schema));
      schema.registerSimple(map, "minecraft:horse");
      schema.registerSimple(map, "minecraft:skeleton_horse");
      schema.registerSimple(map, "minecraft:zombie_horse");
      return map;
   }

   private static TypeTemplate method_66634(Schema schema) {
      return DSL.optionalFields("Items", DSL.list(TypeReferences.ITEM_STACK.in(schema)));
   }
}
