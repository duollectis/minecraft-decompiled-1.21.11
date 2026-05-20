package net.minecraft.client.render.command;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.texture.AtlasManager;

@Environment(EnvType.CLIENT)
public class RenderDispatcher implements AutoCloseable {
   private final OrderedRenderCommandQueueImpl queue;
   private final BlockRenderManager blockRenderManager;
   private final VertexConsumerProvider.Immediate vertexConsumers;
   private final AtlasManager atlasManager;
   private final OutlineVertexConsumerProvider outlineVertexConsumers;
   private final VertexConsumerProvider.Immediate crumblingOverlayVertexConsumers;
   private final TextRenderer textRenderer;
   private final ShadowPiecesCommandRenderer shadowPiecesCommandRenderer = new ShadowPiecesCommandRenderer();
   private final FireCommandRenderer fireCommandRenderer = new FireCommandRenderer();
   private final ModelCommandRenderer modelCommandRenderer = new ModelCommandRenderer();
   private final ModelPartCommandRenderer modelPartCommandRenderer = new ModelPartCommandRenderer();
   private final LabelCommandRenderer labelCommandRenderer = new LabelCommandRenderer();
   private final TextCommandRenderer textCommandRenderer = new TextCommandRenderer();
   private final LeashCommandRenderer leashCommandRenderer = new LeashCommandRenderer();
   private final ItemCommandRenderer itemCommandRenderer = new ItemCommandRenderer();
   private final CustomCommandRenderer customCommandRenderer = new CustomCommandRenderer();
   private final FallingBlockCommandRenderer fallingBlockCommandRenderer = new FallingBlockCommandRenderer();
   private final LayeredCustomCommandRenderer layeredCustomCommandRenderer = new LayeredCustomCommandRenderer();

   public RenderDispatcher(
      OrderedRenderCommandQueueImpl queue,
      BlockRenderManager blockRenderManager,
      VertexConsumerProvider.Immediate vertexConsumers,
      AtlasManager atlasManager,
      OutlineVertexConsumerProvider outlineVertexConsumers,
      VertexConsumerProvider.Immediate crumblingOverlayVertexConsumers,
      TextRenderer textRenderer
   ) {
      this.queue = queue;
      this.blockRenderManager = blockRenderManager;
      this.vertexConsumers = vertexConsumers;
      this.atlasManager = atlasManager;
      this.outlineVertexConsumers = outlineVertexConsumers;
      this.crumblingOverlayVertexConsumers = crumblingOverlayVertexConsumers;
      this.textRenderer = textRenderer;
   }

   public void render() {
      ObjectIterator var1 = this.queue.getBatchingQueues().values().iterator();

      while (var1.hasNext()) {
         BatchingRenderCommandQueue batchingRenderCommandQueue = (BatchingRenderCommandQueue)var1.next();
         this.shadowPiecesCommandRenderer.render(batchingRenderCommandQueue, this.vertexConsumers);
         this.modelCommandRenderer.render(batchingRenderCommandQueue, this.vertexConsumers, this.outlineVertexConsumers, this.crumblingOverlayVertexConsumers);
         this.modelPartCommandRenderer
            .render(batchingRenderCommandQueue, this.vertexConsumers, this.outlineVertexConsumers, this.crumblingOverlayVertexConsumers);
         this.fireCommandRenderer.render(batchingRenderCommandQueue, this.vertexConsumers, this.atlasManager);
         this.labelCommandRenderer.render(batchingRenderCommandQueue, this.vertexConsumers, this.textRenderer);
         this.textCommandRenderer.render(batchingRenderCommandQueue, this.vertexConsumers);
         this.leashCommandRenderer.render(batchingRenderCommandQueue, this.vertexConsumers);
         this.itemCommandRenderer.render(batchingRenderCommandQueue, this.vertexConsumers, this.outlineVertexConsumers);
         this.fallingBlockCommandRenderer.render(batchingRenderCommandQueue, this.vertexConsumers, this.blockRenderManager, this.outlineVertexConsumers);
         this.customCommandRenderer.render(batchingRenderCommandQueue, this.vertexConsumers);
         this.layeredCustomCommandRenderer.render(batchingRenderCommandQueue);
      }

      this.queue.clear();
   }

   public void endLayeredCustoms() {
      this.layeredCustomCommandRenderer.end();
   }

   public OrderedRenderCommandQueueImpl getQueue() {
      return this.queue;
   }

   @Override
   public void close() {
      this.layeredCustomCommandRenderer.close();
   }
}
