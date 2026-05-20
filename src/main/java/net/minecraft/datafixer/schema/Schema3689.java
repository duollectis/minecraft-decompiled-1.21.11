package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.datafixer.TypeReferences;

public class Schema3689 extends IdentifierNormalizingSchema {
   public Schema3689(int i, Schema schema) {
      super(i, schema);
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(schema);
      schema.registerSimple(map, "minecraft:breeze");
      schema.registerSimple(map, "minecraft:wind_charge");
      schema.registerSimple(map, "minecraft:breeze_wind_charge");
      return map;
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema schema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(schema);
      schema.register(
         map,
         "minecraft:trial_spawner",
         () -> DSL.optionalFields(
            "spawn_potentials",
            DSL.list(DSL.fields("data", DSL.fields("entity", TypeReferences.ENTITY_TREE.in(schema)))),
            "spawn_data",
            DSL.fields("entity", TypeReferences.ENTITY_TREE.in(schema))
         )
      );
      return map;
   }
}
