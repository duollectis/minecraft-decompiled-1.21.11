package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.ScopedProfiler;

@Environment(EnvType.CLIENT)
public class OtherClientPlayerEntity extends AbstractClientPlayerEntity {
   private Vec3d clientVelocity = Vec3d.ZERO;
   private int velocityLerpDivisor;

   public OtherClientPlayerEntity(ClientWorld clientWorld, GameProfile gameProfile) {
      super(clientWorld, gameProfile);
      this.noClip = true;
   }

   @Override
   public boolean shouldRender(double distance) {
      double d = this.getBoundingBox().getAverageSideLength() * 10.0;
      if (Double.isNaN(d)) {
         d = 1.0;
      }

      d *= 64.0 * getRenderDistanceMultiplier();
      return distance < d * d;
   }

   @Override
   public boolean clientDamage(DamageSource source) {
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      this.updateLimbs(false);
   }

   @Override
   public void tickMovement() {
      if (this.isInterpolating()) {
         this.getInterpolator().tick();
      }

      if (this.headTrackingIncrements > 0) {
         this.lerpHeadYaw(this.headTrackingIncrements, this.serverHeadYaw);
         this.headTrackingIncrements--;
      }

      if (this.velocityLerpDivisor > 0) {
         this.addVelocityInternal(
            new Vec3d(
               (this.clientVelocity.x - this.getVelocity().x) / this.velocityLerpDivisor,
               (this.clientVelocity.y - this.getVelocity().y) / this.velocityLerpDivisor,
               (this.clientVelocity.z - this.getVelocity().z) / this.velocityLerpDivisor
            )
         );
         this.velocityLerpDivisor--;
      }

      this.tickHandSwing();
      this.tickPlayerMovement();

      try (ScopedProfiler scopedProfiler = Profilers.get().scoped("push")) {
         this.tickCramming();
      }
   }

   @Override
   public void setVelocityClient(Vec3d clientVelocity) {
      this.clientVelocity = clientVelocity;
      this.velocityLerpDivisor = this.getType().getTrackTickInterval() + 1;
   }

   @Override
   protected void updatePose() {
   }

   @Override
   public void onSpawnPacket(EntitySpawnS2CPacket packet) {
      super.onSpawnPacket(packet);
      this.resetPosition();
   }
}
