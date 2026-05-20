package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class LavaEmberParticle extends BillboardParticle {
   LavaEmberParticle(ClientWorld clientWorld, double d, double e, double f, Sprite sprite) {
      super(clientWorld, d, e, f, 0.0, 0.0, 0.0, sprite);
      this.gravityStrength = 0.75F;
      this.velocityMultiplier = 0.999F;
      this.velocityX *= 0.8F;
      this.velocityY *= 0.8F;
      this.velocityZ *= 0.8F;
      this.velocityY = this.random.nextFloat() * 0.4F + 0.05F;
      this.scale = this.scale * (this.random.nextFloat() * 2.0F + 0.2F);
      this.maxAge = (int)(16.0 / (this.random.nextFloat() * 0.8 + 0.2));
   }

   @Override
   public BillboardParticle.RenderType getRenderType() {
      return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
   }

   @Override
   public int getBrightness(float tint) {
      int i = super.getBrightness(tint);
      int j = 240;
      int k = i >> 16 & 0xFF;
      return 240 | k << 16;
   }

   @Override
   public float getSize(float tickProgress) {
      float f = (this.age + tickProgress) / this.maxAge;
      return this.scale * (1.0F - f * f);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.dead) {
         float f = (float)this.age / this.maxAge;
         if (this.random.nextFloat() > f) {
            this.world.addParticleClient(ParticleTypes.SMOKE, this.x, this.y, this.z, this.velocityX, this.velocityY, this.velocityZ);
         }
      }
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
         return new LavaEmberParticle(clientWorld, d, e, f, this.spriteProvider.getSprite(random));
      }
   }
}
