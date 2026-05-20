package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class OminousSpawningParticle extends BillboardParticle {
   private final double startX;
   private final double startY;
   private final double startZ;
   private final int fromColor;
   private final int toColor;

   OminousSpawningParticle(
      ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, int fromColor, int toColor, Sprite sprite
   ) {
      super(world, x, y, z, sprite);
      this.velocityX = velocityX;
      this.velocityY = velocityY;
      this.velocityZ = velocityZ;
      this.startX = x;
      this.startY = y;
      this.startZ = z;
      this.lastX = x + velocityX;
      this.lastY = y + velocityY;
      this.lastZ = z + velocityZ;
      this.x = this.lastX;
      this.y = this.lastY;
      this.z = this.lastZ;
      this.scale = 0.1F * (this.random.nextFloat() * 0.5F + 0.2F);
      this.collidesWithWorld = false;
      this.maxAge = (int)(this.random.nextFloat() * 5.0F) + 25;
      this.fromColor = fromColor;
      this.toColor = toColor;
   }

   @Override
   public BillboardParticle.RenderType getRenderType() {
      return BillboardParticle.RenderType.PARTICLE_ATLAS_OPAQUE;
   }

   @Override
   public void move(double dx, double dy, double dz) {
   }

   @Override
   public int getBrightness(float tint) {
      return 240;
   }

   @Override
   public void tick() {
      this.lastX = this.x;
      this.lastY = this.y;
      this.lastZ = this.z;
      if (this.age++ >= this.maxAge) {
         this.markDead();
      } else {
         float f = (float)this.age / this.maxAge;
         float g = 1.0F - f;
         this.x = this.startX + this.velocityX * g;
         this.y = this.startY + this.velocityY * g;
         this.z = this.startZ + this.velocityZ * g;
         int i = ColorHelper.lerp(f, this.fromColor, this.toColor);
         this.setColor(ColorHelper.getRed(i) / 255.0F, ColorHelper.getGreen(i) / 255.0F, ColorHelper.getBlue(i) / 255.0F);
         this.setAlpha(ColorHelper.getAlpha(i) / 255.0F);
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
         OminousSpawningParticle ominousSpawningParticle = new OminousSpawningParticle(
            clientWorld, d, e, f, g, h, i, -12210434, -1, this.spriteProvider.getSprite(random)
         );
         ominousSpawningParticle.scale(MathHelper.nextBetween(clientWorld.getRandom(), 3.0F, 5.0F));
         return ominousSpawningParticle;
      }
   }
}
