package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;

@Environment(EnvType.CLIENT)
public class NoRenderParticle extends Particle {
   protected NoRenderParticle(ClientWorld clientWorld, double d, double e, double f) {
      super(clientWorld, d, e, f);
   }

   protected NoRenderParticle(ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
      super(clientWorld, d, e, f, g, h, i);
   }

   @Override
   public ParticleTextureSheet textureSheet() {
      return ParticleTextureSheet.NO_RENDER;
   }
}
