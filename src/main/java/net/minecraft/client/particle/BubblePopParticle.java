package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class BubblePopParticle extends BillboardParticle {
   private final SpriteProvider spriteProvider;

   BubblePopParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider) {
      super(world, x, y, z, spriteProvider.getFirst());
      this.spriteProvider = spriteProvider;
      this.maxAge = 4;
      this.gravityStrength = 0.008F;
      this.velocityX = velocityX;
      this.velocityY = velocityY;
      this.velocityZ = velocityZ;
      this.updateSprite(spriteProvider);
   }

   @Override
   public void tick() {
      this.lastX = this.x;
      this.lastY = this.y;
      this.lastZ = this.z;
      if (this.age++ >= this.maxAge) {
         this.markDead();
      } else {
         this.velocityY = this.velocityY - this.gravityStrength;
         this.move(this.velocityX, this.velocityY, this.velocityZ);
         this.updateSprite(this.spriteProvider);
      }
   }

   @Override
   public BillboardParticle.RenderType getRenderType() {
      return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
   }

   @Environment(EnvType.CLIENT)
   public static class Factory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider spriteProvider;

      public Factory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, Random random
      ) {
         return new BubblePopParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider);
      }
   }
}
