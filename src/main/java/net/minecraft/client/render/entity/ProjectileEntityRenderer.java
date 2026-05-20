package net.minecraft.client.render.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.ArrowEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.ProjectileEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public abstract class ProjectileEntityRenderer<T extends PersistentProjectileEntity, S extends ProjectileEntityRenderState> extends EntityRenderer<T, S> {
   private final ArrowEntityModel model;

   public ProjectileEntityRenderer(EntityRendererFactory.Context context) {
      super(context);
      this.model = new ArrowEntityModel(context.getPart(EntityModelLayers.ARROW));
   }

   public void render(
      S projectileEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState
   ) {
      matrixStack.push();
      matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(projectileEntityRenderState.yaw - 90.0F));
      matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(projectileEntityRenderState.pitch));
      orderedRenderCommandQueue.submitModel(
         this.model,
         projectileEntityRenderState,
         matrixStack,
         RenderLayers.entityCutout(this.getTexture(projectileEntityRenderState)),
         projectileEntityRenderState.light,
         OverlayTexture.DEFAULT_UV,
         projectileEntityRenderState.outlineColor,
         null
      );
      matrixStack.pop();
      super.render(projectileEntityRenderState, matrixStack, orderedRenderCommandQueue, cameraRenderState);
   }

   protected abstract Identifier getTexture(S state);

   public void updateRenderState(T persistentProjectileEntity, S projectileEntityRenderState, float f) {
      super.updateRenderState(persistentProjectileEntity, projectileEntityRenderState, f);
      projectileEntityRenderState.pitch = persistentProjectileEntity.getLerpedPitch(f);
      projectileEntityRenderState.yaw = persistentProjectileEntity.getLerpedYaw(f);
      projectileEntityRenderState.shake = persistentProjectileEntity.shake - f;
   }
}
