package net.minecraft.datafixer.fix;

import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.TypeReferences;

public class BlockEntityUuidFix extends AbstractUuidFix {
   public BlockEntityUuidFix(Schema outputSchema) {
      super(outputSchema, TypeReferences.BLOCK_ENTITY);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped("BlockEntityUUIDFix", this.getInputSchema().getType(this.typeReference), typed -> {
         typed = this.updateTyped(typed, "minecraft:conduit", this::updateConduit);
         return this.updateTyped(typed, "minecraft:skull", this::updateSkull);
      });
   }

   @SuppressWarnings("unchecked")
   private Dynamic<?> updateSkull(Dynamic<?> skullDynamic) {
      return (Dynamic<?>) skullDynamic.get("Owner")
         .get()
         .map(ownerDynamic -> updateStringUuid(ownerDynamic, "Id", "Id").orElse((Dynamic) ownerDynamic))
         .map(ownerDynamic -> skullDynamic.remove("Owner").set("SkullOwner", ownerDynamic))
         .result()
         .orElse((Dynamic) skullDynamic);
   }

   private Dynamic<?> updateConduit(Dynamic<?> conduitDynamic) {
      return updateCompoundUuid(conduitDynamic, "target_uuid", "Target").orElse(conduitDynamic);
   }
}
