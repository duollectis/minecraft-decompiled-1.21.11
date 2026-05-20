package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithHat;
import net.minecraft.client.render.entity.state.ItemHolderEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class VillagerHeldItemFeatureRenderer<S extends ItemHolderEntityRenderState, M extends EntityModel<S> & ModelWithHat> extends FeatureRenderer<S, M> {
   public VillagerHeldItemFeatureRenderer(FeatureRendererContext<S, M> context) {
      super(context);
   }

   public void render(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, S itemHolderEntityRenderState, float f, float g) {
      ItemRenderState itemRenderState = itemHolderEntityRenderState.itemRenderState;
      if (!itemRenderState.isEmpty()) {
         matrixStack.push();
         this.applyTransforms(itemHolderEntityRenderState, matrixStack);
         itemRenderState.render(matrixStack, orderedRenderCommandQueue, i, OverlayTexture.DEFAULT_UV, itemHolderEntityRenderState.outlineColor);
         matrixStack.pop();
      }
   }

   protected void applyTransforms(S state, MatrixStack matrices) {
      this.getContextModel().rotateArms(state, matrices);
      matrices.multiply(RotationAxis.POSITIVE_X.rotation(0.75F));
      matrices.scale(1.07F, 1.07F, 1.07F);
      matrices.translate(0.0F, 0.13F, -0.34F);
      matrices.multiply(RotationAxis.POSITIVE_X.rotation((float) Math.PI));
   }
}
