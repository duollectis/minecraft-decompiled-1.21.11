package net.minecraft.client.particle;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.Submittable;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class ItemPickupParticleRenderer extends ParticleRenderer<ItemPickupParticle> {
   public ItemPickupParticleRenderer(ParticleManager particleManager) {
      super(particleManager);
   }

   @Override
   public Submittable render(Frustum frustum, Camera camera, float tickProgress) {
      return new ItemPickupParticleRenderer.Result(
         this.particles.stream().map(particle -> ItemPickupParticleRenderer.Instance.create(particle, camera, tickProgress)).toList()
      );
   }

   @Environment(EnvType.CLIENT)
   record Instance(EntityRenderState itemRenderState, double xOffset, double yOffset, double zOffset) {

      public static ItemPickupParticleRenderer.Instance create(ItemPickupParticle particle, Camera camera, float tickProgress) {
         float f = (particle.ticksExisted + tickProgress) / 3.0F;
         f *= f;
         double d = MathHelper.lerp((double)tickProgress, particle.lastTargetX, particle.targetX);
         double e = MathHelper.lerp((double)tickProgress, particle.lastTargetY, particle.targetY);
         double g = MathHelper.lerp((double)tickProgress, particle.lastTargetZ, particle.targetZ);
         double h = MathHelper.lerp((double)f, particle.renderState.x, d);
         double i = MathHelper.lerp((double)f, particle.renderState.y, e);
         double j = MathHelper.lerp((double)f, particle.renderState.z, g);
         Vec3d vec3d = camera.getCameraPos();
         return new ItemPickupParticleRenderer.Instance(particle.renderState, h - vec3d.getX(), i - vec3d.getY(), j - vec3d.getZ());
      }
   }

   @Environment(EnvType.CLIENT)
   record Result(List<ItemPickupParticleRenderer.Instance> instances) implements Submittable {
      @Override
      public void submit(OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState) {
         MatrixStack matrixStack = new MatrixStack();
         EntityRenderManager entityRenderManager = MinecraftClient.getInstance().getEntityRenderDispatcher();

         for (ItemPickupParticleRenderer.Instance instance : this.instances) {
            entityRenderManager.render(
               instance.itemRenderState, cameraRenderState, instance.xOffset, instance.yOffset, instance.zOffset, matrixStack, orderedRenderCommandQueue
            );
         }
      }
   }
}
