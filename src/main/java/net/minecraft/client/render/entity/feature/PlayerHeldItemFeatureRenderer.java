package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.model.ModelWithHead;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class PlayerHeldItemFeatureRenderer<S extends PlayerEntityRenderState, M extends EntityModel<S> & ModelWithArms & ModelWithHead>
   extends HeldItemFeatureRenderer<S, M> {
   private static final float HEAD_YAW = (float) (-Math.PI / 6);
   private static final float HEAD_ROLL = (float) (Math.PI / 2);

   public PlayerHeldItemFeatureRenderer(FeatureRendererContext<S, M> featureRendererContext) {
      super(featureRendererContext);
   }

   protected void renderItem(
      S playerEntityRenderState,
      ItemRenderState itemRenderState,
      ItemStack itemStack,
      Arm arm,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i
   ) {
      if (!itemRenderState.isEmpty()) {
         Hand hand = arm == playerEntityRenderState.mainArm ? Hand.MAIN_HAND : Hand.OFF_HAND;
         if (playerEntityRenderState.isUsingItem
            && playerEntityRenderState.activeHand == hand
            && playerEntityRenderState.handSwingProgress < 1.0E-5F
            && !playerEntityRenderState.spyglassState.isEmpty()) {
            this.renderSpyglass(playerEntityRenderState, arm, matrixStack, orderedRenderCommandQueue, i);
         } else {
            super.renderItem(playerEntityRenderState, itemRenderState, itemStack, arm, matrixStack, orderedRenderCommandQueue, i);
         }
      }
   }

   private void renderSpyglass(S playerEntityRenderState, Arm arm, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light) {
      matrices.push();
      this.getContextModel().getRootPart().applyTransform(matrices);
      ModelPart modelPart = this.getContextModel().getHead();
      float f = modelPart.pitch;
      modelPart.pitch = MathHelper.clamp(modelPart.pitch, (float) (-Math.PI / 6), (float) (Math.PI / 2));
      modelPart.applyTransform(matrices);
      modelPart.pitch = f;
      HeadFeatureRenderer.translate(matrices, HeadFeatureRenderer.HeadTransformation.DEFAULT);
      boolean bl = arm == Arm.LEFT;
      matrices.translate((bl ? -2.5F : 2.5F) / 16.0F, -0.0625F, 0.0F);
      playerEntityRenderState.spyglassState.render(matrices, orderedRenderCommandQueue, light, OverlayTexture.DEFAULT_UV, playerEntityRenderState.outlineColor);
      matrices.pop();
   }
}
