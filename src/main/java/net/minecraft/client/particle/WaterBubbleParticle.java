package net.minecraft.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public class WaterBubbleParticle extends BillboardParticle {
   WaterBubbleParticle(ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, Sprite sprite) {
      super(clientWorld, d, e, f, sprite);
      this.setBoundingBoxSpacing(0.02F, 0.02F);
      this.scale = this.scale * (this.random.nextFloat() * 0.6F + 0.2F);
      this.velocityX = g * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.velocityY = h * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.velocityZ = i * 0.2F + (this.random.nextFloat() * 2.0F - 1.0F) * 0.02F;
      this.maxAge = (int)(8.0 / (this.random.nextFloat() * 0.8 + 0.2));
   }

   @Override
   public void tick() {
      this.lastX = this.x;
      this.lastY = this.y;
      this.lastZ = this.z;
      if (this.maxAge-- <= 0) {
         this.markDead();
      } else {
         this.velocityY += 0.002;
         this.move(this.velocityX, this.velocityY, this.velocityZ);
         this.velocityX *= 0.85F;
         this.velocityY *= 0.85F;
         this.velocityZ *= 0.85F;
         if (!this.world.getFluidState(BlockPos.ofFloored(this.x, this.y, this.z)).isIn(FluidTags.WATER)) {
            this.markDead();
         }
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
         return new WaterBubbleParticle(clientWorld, d, e, f, g, h, i, this.spriteProvider.getSprite(random));
      }
   }
}
