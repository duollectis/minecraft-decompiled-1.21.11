package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EvokerFangsEntityModel;
import net.minecraft.client.render.entity.state.EvokerFangsEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class EvokerFangsEntityRenderer extends EntityRenderer<EvokerFangsEntity, EvokerFangsEntityRenderState> {
   private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/illager/evoker_fangs.png");
   private final EvokerFangsEntityModel model;

   public EvokerFangsEntityRenderer(EntityRendererFactory.Context context) {
      super(context);
      this.model = new EvokerFangsEntityModel(context.getPart(EntityModelLayers.EVOKER_FANGS));
   }

   public void render(
      EvokerFangsEntityRenderState evokerFangsEntityRenderState,
      MatrixStack matrixStack,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      CameraRenderState cameraRenderState
   ) {
      float f = evokerFangsEntityRenderState.animationProgress;
      if (f != 0.0F) {
         matrixStack.push();
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F - evokerFangsEntityRenderState.yaw));
         matrixStack.scale(-1.0F, -1.0F, 1.0F);
         matrixStack.translate(0.0F, -1.501F, 0.0F);
         orderedRenderCommandQueue.submitModel(
            this.model,
            evokerFangsEntityRenderState,
            matrixStack,
            this.model.getLayer(TEXTURE),
            evokerFangsEntityRenderState.light,
            OverlayTexture.DEFAULT_UV,
            evokerFangsEntityRenderState.outlineColor,
            null
         );
         matrixStack.pop();
         super.render(evokerFangsEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
      }
   }

   public EvokerFangsEntityRenderState createRenderState() {
      return new EvokerFangsEntityRenderState();
   }

   public void updateRenderState(EvokerFangsEntity evokerFangsEntity, EvokerFangsEntityRenderState evokerFangsEntityRenderState, float f) {
      super.updateRenderState(evokerFangsEntity, evokerFangsEntityRenderState, f);
      evokerFangsEntityRenderState.yaw = evokerFangsEntity.getYaw();
      evokerFangsEntityRenderState.animationProgress = evokerFangsEntity.getAnimationProgress(f);
   }
}
