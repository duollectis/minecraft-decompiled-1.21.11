package net.minecraft.datafixer.schema;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.datafixer.TypeReferences;

public class Schema102 extends Schema {
   public Schema102(int versionKey, Schema parent) {
      super(versionKey, parent);
   }

   public void registerTypes(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes) {
      super.registerTypes(schema, entityTypes, blockEntityTypes);
      schema.registerType(
         true,
         TypeReferences.ITEM_STACK,
         () -> DSL.hook(
            DSL.optionalFields("id", TypeReferences.ITEM_NAME.in(schema), "tag", Schema99.method_66195(schema)), Schema99.field_5747, HookFunction.IDENTITY
         )
      );
   }
}
