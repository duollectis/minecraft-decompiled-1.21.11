package net.minecraft.client.render.entity.feature;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
public class EmissiveFeatureRenderer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends FeatureRenderer<S, M> {
   private final Function<S, Identifier> textureFunction;
   private final EmissiveFeatureRenderer.AnimationAlphaAdjuster<S> animationAlphaAdjuster;
   private final M model;
   private final Function<Identifier, RenderLayer> renderLayerFunction;
   private final boolean ignoresInvisibility;

   public EmissiveFeatureRenderer(
      FeatureRendererContext<S, M> context,
      Function<S, Identifier> textureFunction,
      EmissiveFeatureRenderer.AnimationAlphaAdjuster<S> animationAlphaAdjuster,
      M model,
      Function<Identifier, RenderLayer> renderLayerFunction,
      boolean ignoresInvisibility
   ) {
      super(context);
      this.textureFunction = textureFunction;
      this.animationAlphaAdjuster = animationAlphaAdjuster;
      this.model = model;
      this.renderLayerFunction = renderLayerFunction;
      this.ignoresInvisibility = ignoresInvisibility;
   }

   public void render(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, S livingEntityRenderState, float f, float g) {
      if (!livingEntityRenderState.invisible || this.ignoresInvisibility) {
         float h = this.animationAlphaAdjuster.apply(livingEntityRenderState, livingEntityRenderState.age);
         if (!(h <= 1.0E-5F)) {
            int j = ColorHelper.getWhite(h);
            RenderLayer renderLayer = this.renderLayerFunction.apply(this.textureFunction.apply(livingEntityRenderState));
            orderedRenderCommandQueue.getBatchingQueue(1)
               .submitModel(
                  this.model,
                  livingEntityRenderState,
                  matrixStack,
                  renderLayer,
                  i,
                  LivingEntityRenderer.getOverlay(livingEntityRenderState, 0.0F),
                  j,
                  null,
                  livingEntityRenderState.outlineColor,
                  null
               );
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public interface AnimationAlphaAdjuster<S extends LivingEntityRenderState> {
      float apply(S state, float tickProgress);
   }
}
