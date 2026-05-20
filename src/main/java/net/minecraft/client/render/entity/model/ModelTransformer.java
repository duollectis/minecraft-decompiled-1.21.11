package net.minecraft.client.render.entity.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelData;

@FunctionalInterface
@Environment(EnvType.CLIENT)
public interface ModelTransformer {
   ModelTransformer NO_OP = data -> data;

   static ModelTransformer scaling(float scale) {
      float f = 24.016F * (1.0F - scale);
      return data -> data.transform((net.minecraft.client.model.ModelTransform transform) -> transform.scaled(scale).moveOrigin(0.0F, f, 0.0F));
   }

   ModelData apply(ModelData modelData);
}
