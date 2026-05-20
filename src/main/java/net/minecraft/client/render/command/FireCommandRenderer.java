package net.minecraft.client.render.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class FireCommandRenderer {
   public void render(BatchingRenderCommandQueue queue, VertexConsumerProvider.Immediate vertexConsumers, AtlasManager atlasManager) {
      for (OrderedRenderCommandQueueImpl.FireCommand fireCommand : queue.getFireCommands()) {
         this.render(fireCommand.matricesEntry(), vertexConsumers, fireCommand.renderState(), fireCommand.rotation(), atlasManager);
      }
   }

   private void render(
      MatrixStack.Entry matricesEntry, VertexConsumerProvider vertexConsumers, EntityRenderState renderState, Quaternionf rotation, AtlasManager atlasManager
   ) {
      Sprite sprite = atlasManager.getSprite(ModelBaker.FIRE_0);
      Sprite sprite2 = atlasManager.getSprite(ModelBaker.FIRE_1);
      float f = renderState.width * 1.4F;
      matricesEntry.scale(f, f, f);
      float g = 0.5F;
      float h = 0.0F;
      float i = renderState.height / f;
      float j = 0.0F;
      matricesEntry.rotate(rotation);
      matricesEntry.translate(0.0F, 0.0F, 0.3F - (int)i * 0.02F);
      float k = 0.0F;
      int l = 0;

      for (VertexConsumer vertexConsumer = vertexConsumers.getBuffer(TexturedRenderLayers.getEntityCutout()); i > 0.0F; l++) {
         Sprite sprite3 = l % 2 == 0 ? sprite : sprite2;
         float m = sprite3.getMinU();
         float n = sprite3.getMinV();
         float o = sprite3.getMaxU();
         float p = sprite3.getMaxV();
         if (l / 2 % 2 == 0) {
            float q = o;
            o = m;
            m = q;
         }

         vertex(matricesEntry, vertexConsumer, -g - 0.0F, 0.0F - j, k, o, p);
         vertex(matricesEntry, vertexConsumer, g - 0.0F, 0.0F - j, k, m, p);
         vertex(matricesEntry, vertexConsumer, g - 0.0F, 1.4F - j, k, m, n);
         vertex(matricesEntry, vertexConsumer, -g - 0.0F, 1.4F - j, k, o, n);
         i -= 0.45F;
         j -= 0.45F;
         g *= 0.9F;
         k -= 0.03F;
      }
   }

   private static void vertex(MatrixStack.Entry matricesEntry, VertexConsumer vertexConsumer, float x, float y, float z, float u, float v) {
      vertexConsumer.vertex(matricesEntry, x, y, z).color(-1).texture(u, v).overlay(0, 10).light(240).normal(matricesEntry, 0.0F, 1.0F, 0.0F);
   }
}
