package net.minecraft.datafixer.schema;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class Schema4648 extends IdentifierNormalizingSchema {
   public Schema4648(int i, Schema schema) {
      super(i, schema);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
      schema.registerSimple(map, "minecraft:nautilus");
      schema.registerSimple(map, "minecraft:zombie_nautilus");
      return map;
   }
}
