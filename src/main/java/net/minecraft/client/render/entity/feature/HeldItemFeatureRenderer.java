package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.Lancing;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.SwingAnimationType;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class HeldItemFeatureRenderer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ModelWithArms> extends FeatureRenderer<S, M> {
   public HeldItemFeatureRenderer(FeatureRendererContext<S, M> featureRendererContext) {
      super(featureRendererContext);
   }

   public void render(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, S armedEntityRenderState, float f, float g) {
      this.renderItem(
         armedEntityRenderState,
         armedEntityRenderState.rightHandItemState,
         armedEntityRenderState.rightHandItem,
         Arm.RIGHT,
         matrixStack,
         orderedRenderCommandQueue,
         i
      );
      this.renderItem(
         armedEntityRenderState,
         armedEntityRenderState.leftHandItemState,
         armedEntityRenderState.leftHandItem,
         Arm.LEFT,
         matrixStack,
         orderedRenderCommandQueue,
         i
      );
   }

   protected void renderItem(
      S entityState,
      ItemRenderState itemRenderState,
      ItemStack itemStack,
      Arm arm,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i
   ) {
      if (!itemRenderState.isEmpty()) {
         matrixStack.push();
         this.getContextModel().setArmAngle(entityState, arm, matrixStack);
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
         boolean bl = arm == Arm.LEFT;
         matrixStack.translate((bl ? -1 : 1) / 16.0F, 0.125F, -0.625F);
         if (entityState.handSwingProgress > 0.0F && entityState.mainArm == arm && entityState.swingAnimationType == SwingAnimationType.STAB) {
            Lancing.method_75395(entityState, matrixStack);
         }

         float f = entityState.method_75468(arm);
         if (f != 0.0F) {
            (arm == Arm.RIGHT ? entityState.rightArmPose : entityState.leftArmPose).method_75382(entityState, matrixStack, f, arm, itemStack);
         }

         itemRenderState.render(matrixStack, orderedRenderCommandQueue, i, OverlayTexture.DEFAULT_UV, entityState.outlineColor);
         matrixStack.pop();
      }
   }
}
