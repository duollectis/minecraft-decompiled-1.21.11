package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class FireflyParticle extends BillboardParticle {
   private static final float field_56803 = 0.3F;
   private static final float field_56804 = 0.1F;
   private static final float field_56801 = 0.5F;
   private static final float field_56802 = 0.3F;
   private static final int MIN_MAX_AGE = 200;
   private static final int MAX_MAX_AGE = 300;

   FireflyParticle(ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, Sprite sprite) {
      super(clientWorld, d, e, f, g, h, i, sprite);
      this.ascending = true;
      this.velocityMultiplier = 0.96F;
      this.scale *= 0.75F;
      this.velocityY *= 0.8F;
      this.velocityX *= 0.8F;
      this.velocityZ *= 0.8F;
   }

   @Override
   public BillboardParticle.RenderType getRenderType() {
      return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
   }

   @Override
   public int getBrightness(float tint) {
      return (int)(255.0F * method_67878(this.method_67879(this.age + tint), 0.1F, 0.3F));
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.world.getBlockState(BlockPos.ofFloored(this.x, this.y, this.z)).isAir()) {
         this.markDead();
      } else {
         this.setAlpha(method_67878(this.method_67879(this.age), 0.3F, 0.5F));
         if (this.random.nextFloat() > 0.95F || this.age == 1) {
            this.setVelocity(-0.05F + 0.1F * this.random.nextFloat(), -0.05F + 0.1F * this.random.nextFloat(), -0.05F + 0.1F * this.random.nextFloat());
         }
      }
   }

   private float method_67879(float f) {
      return MathHelper.clamp(f / this.maxAge, 0.0F, 1.0F);
   }

   private static float method_67878(float f, float g, float h) {
      if (f >= 1.0F - g) {
         return (1.0F - f) / g;
      } else {
         return f <= h ? f / h : 1.0F;
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
         FireflyParticle fireflyParticle = new FireflyParticle(
            clientWorld, d, e, f, 0.5 - random.nextDouble(), random.nextBoolean() ? h : -h, 0.5 - random.nextDouble(), this.spriteProvider.getSprite(random)
         );
         fireflyParticle.setMaxAge(random.nextBetween(200, 300));
         fireflyParticle.scale(1.5F);
         fireflyParticle.setAlpha(0.0F);
         return fireflyParticle;
      }
   }
}
