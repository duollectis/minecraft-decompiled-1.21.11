package net.minecraft.client.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public record TextRenderLayerSet(RenderLayer normal, RenderLayer seeThrough, RenderLayer polygonOffset, RenderPipeline guiPipeline) {
   public static TextRenderLayerSet ofIntensity(Identifier textureId) {
      return new TextRenderLayerSet(
         RenderLayers.textIntensity(textureId),
         RenderLayers.textIntensitySeeThrough(textureId),
         RenderLayers.textIntensityPolygonOffset(textureId),
         RenderPipelines.GUI_TEXT_INTENSITY
      );
   }

   public static TextRenderLayerSet of(Identifier textureId) {
      return new TextRenderLayerSet(
         RenderLayers.text(textureId), RenderLayers.textSeeThrough(textureId), RenderLayers.textPolygonOffset(textureId), RenderPipelines.GUI_TEXT
      );
   }

   public RenderLayer getRenderLayer(TextRenderer.TextLayerType layerType) {
      return switch (layerType) {
         case NORMAL -> this.normal;
         case SEE_THROUGH -> this.seeThrough;
         case POLYGON_OFFSET -> this.polygonOffset;
      };
   }
}
