package net.minecraft.entity;

import java.util.Set;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

public record EntityPosition(Vec3d position, Vec3d deltaMovement, float yaw, float pitch) {
   public static final PacketCodec<PacketByteBuf, EntityPosition> PACKET_CODEC = PacketCodec.tuple(
      Vec3d.PACKET_CODEC,
      EntityPosition::position,
      Vec3d.PACKET_CODEC,
      EntityPosition::deltaMovement,
      PacketCodecs.FLOAT,
      EntityPosition::yaw,
      PacketCodecs.FLOAT,
      EntityPosition::pitch,
      EntityPosition::new
   );

   public static EntityPosition fromEntity(Entity entity) {
      return entity.isInterpolating()
         ? new EntityPosition(
            entity.getInterpolator().getLerpedPos(), entity.getMovement(), entity.getInterpolator().getLerpedYaw(), entity.getInterpolator().getLerpedPitch()
         )
         : new EntityPosition(entity.getEntityPos(), entity.getMovement(), entity.getYaw(), entity.getPitch());
   }

   public EntityPosition withRotation(float yaw, float pitch) {
      return new EntityPosition(this.position(), this.deltaMovement(), yaw, pitch);
   }

   public static EntityPosition fromTeleportTarget(TeleportTarget teleportTarget) {
      return new EntityPosition(teleportTarget.position(), teleportTarget.velocity(), teleportTarget.yaw(), teleportTarget.pitch());
   }

   public static EntityPosition apply(EntityPosition currentPos, EntityPosition newPos, Set<PositionFlag> flags) {
      double d = flags.contains(PositionFlag.X) ? currentPos.position.x : 0.0;
      double e = flags.contains(PositionFlag.Y) ? currentPos.position.y : 0.0;
      double f = flags.contains(PositionFlag.Z) ? currentPos.position.z : 0.0;
      float g = flags.contains(PositionFlag.Y_ROT) ? currentPos.yaw : 0.0F;
      float h = flags.contains(PositionFlag.X_ROT) ? currentPos.pitch : 0.0F;
      Vec3d vec3d = new Vec3d(d + newPos.position.x, e + newPos.position.y, f + newPos.position.z);
      float i = g + newPos.yaw;
      float j = MathHelper.clamp(h + newPos.pitch, -90.0F, 90.0F);
      Vec3d vec3d2 = currentPos.deltaMovement;
      if (flags.contains(PositionFlag.ROTATE_DELTA)) {
         float k = currentPos.yaw - i;
         float l = currentPos.pitch - j;
         vec3d2 = vec3d2.rotateX((float)Math.toRadians(l));
         vec3d2 = vec3d2.rotateY((float)Math.toRadians(k));
      }

      Vec3d vec3d3 = new Vec3d(
         resolve(vec3d2.x, newPos.deltaMovement.x, flags, PositionFlag.DELTA_X),
         resolve(vec3d2.y, newPos.deltaMovement.y, flags, PositionFlag.DELTA_Y),
         resolve(vec3d2.z, newPos.deltaMovement.z, flags, PositionFlag.DELTA_Z)
      );
      return new EntityPosition(vec3d, vec3d3, i, j);
   }

   private static double resolve(double delta, double value, Set<PositionFlag> flags, PositionFlag deltaFlag) {
      return flags.contains(deltaFlag) ? delta + value : value;
   }
}
