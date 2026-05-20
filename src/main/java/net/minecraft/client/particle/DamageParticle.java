package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class DamageParticle extends BillboardParticle {
   DamageParticle(ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, Sprite sprite) {
      super(clientWorld, d, e, f, 0.0, 0.0, 0.0, sprite);
      this.velocityMultiplier = 0.7F;
      this.gravityStrength = 0.5F;
      this.velocityX *= 0.1F;
      this.velocityY *= 0.1F;
      this.velocityZ *= 0.1F;
      this.velocityX += g * 0.4;
      this.velocityY += h * 0.4;
      this.velocityZ += i * 0.4;
      float j = this.random.nextFloat() * 0.3F + 0.6F;
      this.red = j;
      this.green = j;
      this.blue = j;
      this.scale *= 0.75F;
      this.maxAge = Math.max((int)(6.0 / (this.random.nextFloat() * 0.8 + 0.6)), 1);
      this.collidesWithWorld = false;
      this.tick();
   }

   @Override
   public float getSize(float tickProgress) {
      return this.scale * MathHelper.clamp((this.age + tickProgress) / this.maxAge * 32.0F, 0.0F, 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      this.green *= 0.96F;
      this.blue *= 0.9F;
   }

   @Override
   public BillboardParticle.RenderType getRenderType() {
      return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
   }

   @Environment(EnvType.CLIENT)
   public static class DefaultFactory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider spriteProvider;

      public DefaultFactory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, Random random
      ) {
         DamageParticle damageParticle = new DamageParticle(clientWorld, d, e, f, g, h + 1.0, i, this.spriteProvider.getSprite(random));
         damageParticle.setMaxAge(20);
         return damageParticle;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class EnchantedHitFactory implements ParticleFactory<SimpleParticleType> {
      private final SpriteProvider spriteProvider;

      public EnchantedHitFactory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, Random random
      ) {
         DamageParticle damageParticle = new DamageParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider.getSprite(random));
         damageParticle.red *= 0.3F;
         damageParticle.green *= 0.8F;
         return damageParticle;
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
         return new DamageParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider.getSprite(random));
      }
   }
}
