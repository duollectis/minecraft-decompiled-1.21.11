package net.minecraft.client.render.command;

import com.mojang.blaze3d.systems.RenderPass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.BillboardParticleSubmittable;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface OrderedRenderCommandQueue extends RenderCommandQueue {
   RenderCommandQueue getBatchingQueue(int order);

   @Environment(EnvType.CLIENT)
   public interface Custom {
      void render(MatrixStack.Entry matricesEntry, VertexConsumer vertexConsumer);
   }

   @Environment(EnvType.CLIENT)
   public interface LayeredCustom {
      BillboardParticleSubmittable.@Nullable Buffers submit(LayeredCustomCommandRenderer.VerticesCache cache);

      void render(
         BillboardParticleSubmittable.Buffers buffers,
         LayeredCustomCommandRenderer.VerticesCache cache,
         RenderPass renderPass,
         TextureManager manager,
         boolean translucent
      );
   }
}
