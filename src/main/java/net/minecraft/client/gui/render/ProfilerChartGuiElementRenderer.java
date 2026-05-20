package net.minecraft.client.gui.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.render.state.special.ProfilerChartGuiElementRenderState;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.ProfilerTiming;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class ProfilerChartGuiElementRenderer extends SpecialGuiElementRenderer<ProfilerChartGuiElementRenderState> {
   public ProfilerChartGuiElementRenderer(VertexConsumerProvider.Immediate immediate) {
      super(immediate);
   }

   @Override
   public Class<ProfilerChartGuiElementRenderState> getElementClass() {
      return ProfilerChartGuiElementRenderState.class;
   }

   protected void render(ProfilerChartGuiElementRenderState profilerChartGuiElementRenderState, MatrixStack matrixStack) {
      double d = 0.0;
      matrixStack.translate(0.0F, -5.0F, 0.0F);
      Matrix4f matrix4f = matrixStack.peek().getPositionMatrix();

      for (ProfilerTiming profilerTiming : profilerChartGuiElementRenderState.chartData()) {
         int i = MathHelper.floor(profilerTiming.parentSectionUsagePercentage / 4.0) + 1;
         VertexConsumer vertexConsumer = this.vertexConsumers.getBuffer(RenderLayers.debugTriangleFan());
         int j = ColorHelper.fullAlpha(profilerTiming.getColor());
         int k = ColorHelper.mix(j, -8355712);
         vertexConsumer.vertex(matrix4f, 0.0F, 0.0F, 0.0F).color(j);

         for (int l = i; l >= 0; l--) {
            float f = (float)((d + profilerTiming.parentSectionUsagePercentage * l / i) * (float) (Math.PI * 2) / 100.0);
            float g = MathHelper.sin(f) * 105.0F;
            float h = MathHelper.cos(f) * 105.0F * 0.5F;
            vertexConsumer.vertex(matrix4f, g, h, 0.0F).color(j);
         }

         vertexConsumer = this.vertexConsumers.getBuffer(RenderLayers.debugQuads());

         for (int l = i; l > 0; l--) {
            float f = (float)((d + profilerTiming.parentSectionUsagePercentage * l / i) * (float) (Math.PI * 2) / 100.0);
            float g = MathHelper.sin(f) * 105.0F;
            float h = MathHelper.cos(f) * 105.0F * 0.5F;
            float m = (float)((d + profilerTiming.parentSectionUsagePercentage * (l - 1) / i) * (float) (Math.PI * 2) / 100.0);
            float n = MathHelper.sin(m) * 105.0F;
            float o = MathHelper.cos(m) * 105.0F * 0.5F;
            if (!((h + o) / 2.0F < 0.0F)) {
               vertexConsumer.vertex(matrix4f, g, h, 0.0F).color(k);
               vertexConsumer.vertex(matrix4f, g, h + 10.0F, 0.0F).color(k);
               vertexConsumer.vertex(matrix4f, n, o + 10.0F, 0.0F).color(k);
               vertexConsumer.vertex(matrix4f, n, o, 0.0F).color(k);
            }
         }

         d += profilerTiming.parentSectionUsagePercentage;
      }
   }

   @Override
   protected float getYOffset(int height, int windowScaleFactor) {
      return height / 2.0F;
   }

   @Override
   protected String getName() {
      return "profiler chart";
   }
}
