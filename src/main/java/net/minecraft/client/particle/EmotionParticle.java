package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class EmotionParticle extends BillboardParticle {
   EmotionParticle(ClientWorld clientWorld, double d, double e, double f, Sprite sprite) {
      super(clientWorld, d, e, f, 0.0, 0.0, 0.0, sprite);
      this.ascending = true;
      this.velocityMultiplier = 0.86F;
      this.velocityX *= 0.01F;
      this.velocityY *= 0.01F;
      this.velocityZ *= 0.01F;
      this.velocityY += 0.1;
      this.scale *= 1.5F;
      this.maxAge = 16;
      this.collidesWithWorld = false;
   }

   @Override
   public BillboardParticle.RenderType getRenderType() {
      return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
   }

   @Override
   public float getSize(float tickProgress) {
      return this.scale * MathHelper.clamp((this.age + tickProgress) / this.maxAge * 32.0F, 0.0F, 1.0F);
   }

   @Environment(EnvType.CLIENT)
   public static class AngryVillagerFactory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider spriteProvider;

      public AngryVillagerFactory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, Random random
      ) {
         EmotionParticle emotionParticle = new EmotionParticle(clientWorld, d, e + 0.5, f, this.spriteProvider.getSprite(random));
         emotionParticle.setColor(1.0F, 1.0F, 1.0F);
         return emotionParticle;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class HeartFactory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider spriteProvider;

      public HeartFactory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, Random random
      ) {
         return new EmotionParticle(clientWorld, d, e, f, this.spriteProvider.getSprite(random));
      }
   }
}
