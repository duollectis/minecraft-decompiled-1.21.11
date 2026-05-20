package net.minecraft.client.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class ClientPlayerLikeState {
   private Vec3d velocity = Vec3d.ZERO;
   private float distanceMoved;
   private float lastDistanceMoved;
   private double x;
   private double y;
   private double z;
   private double lastX;
   private double lastY;
   private double lastZ;
   private float movement;
   private float lastMovement;

   public void tick(Vec3d pos, Vec3d velocity) {
      this.lastDistanceMoved = this.distanceMoved;
      this.velocity = velocity;
      this.setPos(pos);
   }

   public void addDistanceMoved(float distanceMoved) {
      this.distanceMoved += distanceMoved;
   }

   public Vec3d getVelocity() {
      return this.velocity;
   }

   private void setPos(Vec3d pos) {
      this.lastX = this.x;
      this.lastY = this.y;
      this.lastZ = this.z;
      double d = pos.getX() - this.x;
      double e = pos.getY() - this.y;
      double f = pos.getZ() - this.z;
      double g = 10.0;
      if (!(d > 10.0) && !(d < -10.0)) {
         this.x += d * 0.25;
      } else {
         this.x = pos.getX();
         this.lastX = this.x;
      }

      if (!(e > 10.0) && !(e < -10.0)) {
         this.y += e * 0.25;
      } else {
         this.y = pos.getY();
         this.lastY = this.y;
      }

      if (!(f > 10.0) && !(f < -10.0)) {
         this.z += f * 0.25;
      } else {
         this.z = pos.getZ();
         this.lastZ = this.z;
      }
   }

   public double lerpX(float tickProgress) {
      return MathHelper.lerp((double)tickProgress, this.lastX, this.x);
   }

   public double lerpY(float tickProgress) {
      return MathHelper.lerp((double)tickProgress, this.lastY, this.y);
   }

   public double lerpZ(float tickProgress) {
      return MathHelper.lerp((double)tickProgress, this.lastZ, this.z);
   }

   public void tickMovement(float movement) {
      this.lastMovement = this.movement;
      this.movement = this.movement + (movement - this.movement) * 0.4F;
   }

   public void tickRiding() {
      this.lastMovement = this.movement;
      this.movement = 0.0F;
   }

   public float lerpMovement(float tickProgress) {
      return MathHelper.lerp(tickProgress, this.lastMovement, this.movement);
   }

   public float getReverseLerpedDistanceMoved(float tickProgress) {
      float f = this.distanceMoved - this.lastDistanceMoved;
      return -(this.distanceMoved + f * tickProgress);
   }

   public float getLerpedDistanceMoved(float tickProgress) {
      return MathHelper.lerp(tickProgress, this.lastDistanceMoved, this.distanceMoved);
   }
}
