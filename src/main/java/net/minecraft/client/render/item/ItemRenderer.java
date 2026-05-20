package net.minecraft.client.render.item;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumers;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MatrixUtil;

@Environment(EnvType.CLIENT)
public class ItemRenderer {
   public static final Identifier ENTITY_ENCHANTMENT_GLINT = Identifier.ofVanilla("textures/misc/enchanted_glint_armor.png");
   public static final Identifier ITEM_ENCHANTMENT_GLINT = Identifier.ofVanilla("textures/misc/enchanted_glint_item.png");
   public static final float field_60154 = 0.5F;
   public static final float field_60155 = 0.75F;
   public static final float field_60156 = 0.0078125F;
   public static final int NO_TINT = -1;

   public static void renderItem(
      ItemDisplayContext displayContext,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumers,
      int light,
      int overlay,
      int[] tints,
      List<BakedQuad> quads,
      RenderLayer layer,
      ItemRenderState.Glint glint
   ) {
      VertexConsumer vertexConsumer;
      if (glint == ItemRenderState.Glint.SPECIAL) {
         MatrixStack.Entry entry = matrices.peek().copy();
         if (displayContext == ItemDisplayContext.GUI) {
            MatrixUtil.scale(entry.getPositionMatrix(), 0.5F);
         } else if (displayContext.isFirstPerson()) {
            MatrixUtil.scale(entry.getPositionMatrix(), 0.75F);
         }

         vertexConsumer = getSpecialItemGlintConsumer(vertexConsumers, layer, entry);
      } else {
         vertexConsumer = getItemGlintConsumer(vertexConsumers, layer, true, glint != ItemRenderState.Glint.NONE);
      }

      renderBakedItemQuads(matrices, vertexConsumer, quads, tints, light, overlay);
   }

   private static VertexConsumer getSpecialItemGlintConsumer(VertexConsumerProvider consumers, RenderLayer layer, MatrixStack.Entry matrix) {
      return VertexConsumers.union(
         new OverlayVertexConsumer(consumers.getBuffer(useTransparentGlint(layer) ? RenderLayers.glintTranslucent() : RenderLayers.glint()), matrix, 0.0078125F),
         consumers.getBuffer(layer)
      );
   }

   public static VertexConsumer getItemGlintConsumer(VertexConsumerProvider vertexConsumers, RenderLayer layer, boolean solid, boolean glint) {
      if (glint) {
         return useTransparentGlint(layer)
            ? VertexConsumers.union(vertexConsumers.getBuffer(RenderLayers.glintTranslucent()), vertexConsumers.getBuffer(layer))
            : VertexConsumers.union(vertexConsumers.getBuffer(solid ? RenderLayers.glint() : RenderLayers.entityGlint()), vertexConsumers.getBuffer(layer));
      } else {
         return vertexConsumers.getBuffer(layer);
      }
   }

   public static List<RenderLayer> getGlintRenderLayers(RenderLayer renderLayer, boolean solid, boolean glint) {
      if (glint) {
         return useTransparentGlint(renderLayer)
            ? List.of(renderLayer, RenderLayers.glintTranslucent())
            : List.of(renderLayer, solid ? RenderLayers.glint() : RenderLayers.entityGlint());
      } else {
         return List.of(renderLayer);
      }
   }

   private static boolean useTransparentGlint(RenderLayer renderLayer) {
      return MinecraftClient.usesImprovedTransparency()
         && (renderLayer == TexturedRenderLayers.getItemTranslucentCull() || renderLayer == TexturedRenderLayers.getBlockTranslucentCull());
   }

   private static int getTint(int[] tints, int index) {
      return index >= 0 && index < tints.length ? tints[index] : -1;
   }

   private static void renderBakedItemQuads(MatrixStack matrices, VertexConsumer vertexConsumer, List<BakedQuad> quads, int[] tints, int light, int overlay) {
      MatrixStack.Entry entry = matrices.peek();

      for (BakedQuad bakedQuad : quads) {
         float f;
         float g;
         float h;
         float j;
         if (bakedQuad.hasTint()) {
            int i = getTint(tints, bakedQuad.tintIndex());
            f = ColorHelper.getAlpha(i) / 255.0F;
            g = ColorHelper.getRed(i) / 255.0F;
            h = ColorHelper.getGreen(i) / 255.0F;
            j = ColorHelper.getBlue(i) / 255.0F;
         } else {
            f = 1.0F;
            g = 1.0F;
            h = 1.0F;
            j = 1.0F;
         }

         vertexConsumer.quad(entry, bakedQuad, g, h, j, f, light, overlay);
      }
   }
}
