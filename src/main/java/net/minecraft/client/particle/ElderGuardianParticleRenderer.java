package net.minecraft.client.particle;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Submittable;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Unit;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class ElderGuardianParticleRenderer extends ParticleRenderer<ElderGuardianParticle> {
   public ElderGuardianParticleRenderer(ParticleManager particleManager) {
      super(particleManager);
   }

   @Override
   public Submittable render(Frustum frustum, Camera camera, float tickProgress) {
      return new ElderGuardianParticleRenderer.Result(
         this.particles.stream().map(elderGuardianParticle -> ElderGuardianParticleRenderer.State.create(elderGuardianParticle, camera, tickProgress)).toList()
      );
   }

   @Environment(EnvType.CLIENT)
   record Result(List<ElderGuardianParticleRenderer.State> states) implements Submittable {
      @Override
      public void submit(OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState) {
         for (ElderGuardianParticleRenderer.State state : this.states) {
            orderedRenderCommandQueue.submitModel(
               state.model, Unit.INSTANCE, state.matrices, state.renderLayer, 15728880, OverlayTexture.DEFAULT_UV, state.color, null, 0, null
            );
         }
      }
   }

   @Environment(EnvType.CLIENT)
   record State(Model<Unit> model, MatrixStack matrices, RenderLayer renderLayer, int color) {

      public static ElderGuardianParticleRenderer.State create(ElderGuardianParticle particle, Camera camera, float tickProgress) {
         float f = (particle.age + tickProgress) / particle.maxAge;
         float g = 0.05F + 0.5F * MathHelper.sin(f * (float) Math.PI);
         int i = ColorHelper.fromFloats(g, 1.0F, 1.0F, 1.0F);
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.push();
         matrixStack.multiply(camera.getRotation());
         matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(60.0F - 150.0F * f));
         float h = 0.42553192F;
         matrixStack.scale(0.42553192F, -0.42553192F, -0.42553192F);
         matrixStack.translate(0.0F, -0.56F, 3.5F);
         return new ElderGuardianParticleRenderer.State(particle.model, matrixStack, particle.renderLayer, i);
      }
   }
}
