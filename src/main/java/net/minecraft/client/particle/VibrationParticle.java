package net.minecraft.client.particle;

import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.VibrationParticleEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.event.PositionSource;
import org.joml.Quaternionf;

@Environment(EnvType.CLIENT)
public class VibrationParticle extends BillboardParticle {
   private final PositionSource vibration;
   private float field_28250;
   private float field_28248;
   private float field_40507;
   private float field_40508;

   VibrationParticle(ClientWorld world, double x, double y, double z, PositionSource vibration, int maxAge, Sprite sprite) {
      super(world, x, y, z, 0.0, 0.0, 0.0, sprite);
      this.scale = 0.3F;
      this.vibration = vibration;
      this.maxAge = maxAge;
      Optional<Vec3d> optional = vibration.getPos(world);
      if (optional.isPresent()) {
         Vec3d vec3d = optional.get();
         double d = x - vec3d.getX();
         double e = y - vec3d.getY();
         double f = z - vec3d.getZ();
         this.field_28248 = this.field_28250 = (float)MathHelper.atan2(d, f);
         this.field_40508 = this.field_40507 = (float)MathHelper.atan2(e, Math.sqrt(d * d + f * f));
      }
   }

   @Override
   public void render(BillboardParticleSubmittable submittable, Camera camera, float tickProgress) {
      float f = MathHelper.sin((this.age + tickProgress - (float) (Math.PI * 2)) * 0.05F) * 2.0F;
      float g = MathHelper.lerp(tickProgress, this.field_28248, this.field_28250);
      float h = MathHelper.lerp(tickProgress, this.field_40508, this.field_40507) + (float) (Math.PI / 2);
      Quaternionf quaternionf = new Quaternionf();
      quaternionf.rotationY(g).rotateX(-h).rotateY(f);
      this.render(submittable, camera, quaternionf, tickProgress);
      quaternionf.rotationY((float) -Math.PI + g).rotateX(h).rotateY(f);
      this.render(submittable, camera, quaternionf, tickProgress);
   }

   @Override
   public int getBrightness(float tint) {
      return 240;
   }

   @Override
   public BillboardParticle.RenderType getRenderType() {
      return BillboardParticle.RenderType.PARTICLE_ATLAS_TRANSLUCENT;
   }

   @Override
   public void tick() {
      this.lastX = this.x;
      this.lastY = this.y;
      this.lastZ = this.z;
      if (this.age++ >= this.maxAge) {
         this.markDead();
      } else {
         Optional<Vec3d> optional = this.vibration.getPos(this.world);
         if (optional.isEmpty()) {
            this.markDead();
         } else {
            int i = this.maxAge - this.age;
            double d = 1.0 / i;
            Vec3d vec3d = optional.get();
            this.x = MathHelper.lerp(d, this.x, vec3d.getX());
            this.y = MathHelper.lerp(d, this.y, vec3d.getY());
            this.z = MathHelper.lerp(d, this.z, vec3d.getZ());
            double e = this.x - vec3d.getX();
            double f = this.y - vec3d.getY();
            double g = this.z - vec3d.getZ();
            this.field_28248 = this.field_28250;
            this.field_28250 = (float)MathHelper.atan2(e, g);
            this.field_40508 = this.field_40507;
            this.field_40507 = (float)MathHelper.atan2(f, Math.sqrt(e * e + g * g));
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Factory implements ParticleFactory<VibrationParticleEffect> {
      private final SpriteProvider spriteProvider;

      public Factory(SpriteProvider spriteProvider) {
         this.spriteProvider = spriteProvider;
      }

      public Particle createParticle(
         VibrationParticleEffect vibrationParticleEffect, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, Random random
      ) {
         VibrationParticle vibrationParticle = new VibrationParticle(
            clientWorld, d, e, f, vibrationParticleEffect.getVibration(), vibrationParticleEffect.getArrivalInTicks(), this.spriteProvider.getSprite(random)
         );
         vibrationParticle.setAlpha(1.0F);
         return vibrationParticle;
      }
   }
}
