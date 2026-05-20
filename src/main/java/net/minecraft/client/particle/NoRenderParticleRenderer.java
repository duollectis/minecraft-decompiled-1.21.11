package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.Submittable;

@Environment(EnvType.CLIENT)
public class NoRenderParticleRenderer extends ParticleRenderer<NoRenderParticle> {
   private static final Submittable EMPTY = (queue, cameraRenderState) -> {};

   public NoRenderParticleRenderer(ParticleManager particleManager) {
      super(particleManager);
   }

   @Override
   public Submittable render(Frustum frustum, Camera camera, float tickProgress) {
      return EMPTY;
   }
}
