package net.minecraft.client.render.entity.feature;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.IronGolemEntityModel;
import net.minecraft.client.render.entity.state.IronGolemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class IronGolemFlowerFeatureRenderer extends FeatureRenderer<IronGolemEntityRenderState, IronGolemEntityModel> {
   public IronGolemFlowerFeatureRenderer(FeatureRendererContext<IronGolemEntityRenderState, IronGolemEntityModel> featureRendererContext) {
      super(featureRendererContext);
   }

   public void render(
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int i,
      IronGolemEntityRenderState ironGolemEntityRenderState,
      float f,
      float g
   ) {
      if (ironGolemEntityRenderState.lookingAtVillagerTicks != 0) {
         matrixStack.push();
         ModelPart modelPart = this.getContextModel().getRightArm();
         modelPart.applyTransform(matrixStack);
         matrixStack.translate(-1.1875F, 1.0625F, -0.9375F);
         matrixStack.translate(0.5F, 0.5F, 0.5F);
         float h = 0.5F;
         matrixStack.scale(0.5F, 0.5F, 0.5F);
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
         matrixStack.translate(-0.5F, -0.5F, -0.5F);
         orderedRenderCommandQueue.submitBlock(
            matrixStack, Blocks.POPPY.getDefaultState(), i, OverlayTexture.DEFAULT_UV, ironGolemEntityRenderState.outlineColor
         );
         matrixStack.pop();
      }
   }
}
