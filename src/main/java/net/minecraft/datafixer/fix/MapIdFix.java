package net.minecraft.datafixer.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;
import net.minecraft.datafixer.TypeReferences;

public class MapIdFix extends DataFix {
   public MapIdFix(Schema schema) {
      super(schema, false);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped(
         "Map id fix",
         this.getInputSchema().getType(TypeReferences.SAVED_DATA_IDCOUNTS),
         typed -> typed.update(DSL.remainderFinder(), dynamic -> dynamic.createMap(Map.of(dynamic.createString("data"), dynamic)))
      );
   }
}
