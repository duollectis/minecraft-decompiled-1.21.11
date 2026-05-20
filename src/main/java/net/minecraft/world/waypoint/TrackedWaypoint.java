package net.minecraft.world.waypoint;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.encoding.VarInts;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

public abstract class TrackedWaypoint implements Waypoint {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final PacketCodec<ByteBuf, TrackedWaypoint> PACKET_CODEC = PacketCodec.of(TrackedWaypoint::writeBuf, TrackedWaypoint::fromBuf);
   protected final Either<UUID, String> source;
   private final Waypoint.Config config;
   private final TrackedWaypoint.Type type;

   TrackedWaypoint(Either<UUID, String> source, Waypoint.Config config, TrackedWaypoint.Type type) {
      this.source = source;
      this.config = config;
      this.type = type;
   }

   public Either<UUID, String> getSource() {
      return this.source;
   }

   public abstract void handleUpdate(TrackedWaypoint waypoint);

   public void writeBuf(ByteBuf buf) {
      PacketByteBuf packetByteBuf = new PacketByteBuf(buf);
      packetByteBuf.writeEither(this.source, Uuids.PACKET_CODEC, PacketByteBuf::writeString);
      Waypoint.Config.PACKET_CODEC.encode(packetByteBuf, this.config);
      packetByteBuf.writeEnumConstant(this.type);
      this.writeAdditionalDataToBuf(buf);
   }

   public abstract void writeAdditionalDataToBuf(ByteBuf buf);

   private static TrackedWaypoint fromBuf(ByteBuf buf) {
      PacketByteBuf packetByteBuf = new PacketByteBuf(buf);
      Either<UUID, String> either = packetByteBuf.readEither(Uuids.PACKET_CODEC, PacketByteBuf::readString);
      Waypoint.Config config = Waypoint.Config.PACKET_CODEC.decode(packetByteBuf);
      TrackedWaypoint.Type type = packetByteBuf.readEnumConstant(TrackedWaypoint.Type.class);
      return (TrackedWaypoint)type.factory.apply(either, config, packetByteBuf);
   }

   public static TrackedWaypoint ofPos(UUID source, Waypoint.Config config, Vec3i pos) {
      return new TrackedWaypoint.Positional(source, config, pos);
   }

   public static TrackedWaypoint ofChunk(UUID source, Waypoint.Config config, ChunkPos chunkPos) {
      return new TrackedWaypoint.ChunkBased(source, config, chunkPos);
   }

   public static TrackedWaypoint ofAzimuth(UUID source, Waypoint.Config config, float azimuth) {
      return new TrackedWaypoint.Azimuth(source, config, azimuth);
   }

   public static TrackedWaypoint empty(UUID uuid) {
      return new TrackedWaypoint.Empty(uuid);
   }

   public abstract double getRelativeYaw(World world, TrackedWaypoint.YawProvider yawProvider, EntityTickProgress tickProgress);

   public abstract TrackedWaypoint.Pitch getPitch(World world, TrackedWaypoint.PitchProvider cameraProvider, EntityTickProgress tickProgress);

   public abstract double squaredDistanceTo(Entity receiver);

   public Waypoint.Config getConfig() {
      return this.config;
   }

   static class Azimuth extends TrackedWaypoint {
      private float azimuth;

      public Azimuth(UUID source, Waypoint.Config config, float azimuth) {
         super(Either.left(source), config, TrackedWaypoint.Type.AZIMUTH);
         this.azimuth = azimuth;
      }

      public Azimuth(Either<UUID, String> source, Waypoint.Config config, PacketByteBuf buf) {
         super(source, config, TrackedWaypoint.Type.AZIMUTH);
         this.azimuth = buf.readFloat();
      }

      @Override
      public void handleUpdate(TrackedWaypoint waypoint) {
         if (waypoint instanceof TrackedWaypoint.Azimuth azimuth) {
            this.azimuth = azimuth.azimuth;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", waypoint.getClass());
         }
      }

      @Override
      public void writeAdditionalDataToBuf(ByteBuf buf) {
         buf.writeFloat(this.azimuth);
      }

      @Override
      public double getRelativeYaw(World world, TrackedWaypoint.YawProvider yawProvider, EntityTickProgress tickProgress) {
         return MathHelper.subtractAngles(yawProvider.getCameraYaw(), this.azimuth * (180.0F / (float)Math.PI));
      }

      @Override
      public TrackedWaypoint.Pitch getPitch(World world, TrackedWaypoint.PitchProvider cameraProvider, EntityTickProgress tickProgress) {
         double d = cameraProvider.getPitch();
         if (d < -1.0) {
            return TrackedWaypoint.Pitch.DOWN;
         } else {
            return d > 1.0 ? TrackedWaypoint.Pitch.UP : TrackedWaypoint.Pitch.NONE;
         }
      }

      @Override
      public double squaredDistanceTo(Entity receiver) {
         return Double.POSITIVE_INFINITY;
      }
   }

   static class ChunkBased extends TrackedWaypoint {
      private ChunkPos chunkPos;

      public ChunkBased(UUID source, Waypoint.Config config, ChunkPos chunkPos) {
         super(Either.left(source), config, TrackedWaypoint.Type.CHUNK);
         this.chunkPos = chunkPos;
      }

      public ChunkBased(Either<UUID, String> source, Waypoint.Config config, PacketByteBuf buf) {
         super(source, config, TrackedWaypoint.Type.CHUNK);
         this.chunkPos = new ChunkPos(buf.readVarInt(), buf.readVarInt());
      }

      @Override
      public void handleUpdate(TrackedWaypoint waypoint) {
         if (waypoint instanceof TrackedWaypoint.ChunkBased chunkBased) {
            this.chunkPos = chunkBased.chunkPos;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", waypoint.getClass());
         }
      }

      @Override
      public void writeAdditionalDataToBuf(ByteBuf buf) {
         VarInts.write(buf, this.chunkPos.x);
         VarInts.write(buf, this.chunkPos.z);
      }

      private Vec3d getChunkCenterPos(double y) {
         return Vec3d.ofCenter(this.chunkPos.getCenterAtY((int)y));
      }

      @Override
      public double getRelativeYaw(World world, TrackedWaypoint.YawProvider yawProvider, EntityTickProgress tickProgress) {
         Vec3d vec3d = yawProvider.getCameraPos();
         Vec3d vec3d2 = vec3d.subtract(this.getChunkCenterPos(vec3d.getY())).rotateYClockwise();
         float f = (float)MathHelper.atan2(vec3d2.getZ(), vec3d2.getX()) * (180.0F / (float)Math.PI);
         return MathHelper.subtractAngles(yawProvider.getCameraYaw(), f);
      }

      @Override
      public TrackedWaypoint.Pitch getPitch(World world, TrackedWaypoint.PitchProvider cameraProvider, EntityTickProgress tickProgress) {
         double d = cameraProvider.getPitch();
         if (d < -1.0) {
            return TrackedWaypoint.Pitch.DOWN;
         } else {
            return d > 1.0 ? TrackedWaypoint.Pitch.UP : TrackedWaypoint.Pitch.NONE;
         }
      }

      @Override
      public double squaredDistanceTo(Entity receiver) {
         return receiver.squaredDistanceTo(Vec3d.ofCenter(this.chunkPos.getCenterAtY(receiver.getBlockY())));
      }
   }

   static class Empty extends TrackedWaypoint {
      private Empty(Either<UUID, String> source, Waypoint.Config config, PacketByteBuf buf) {
         super(source, config, TrackedWaypoint.Type.EMPTY);
      }

      Empty(UUID source) {
         super(Either.left(source), Waypoint.Config.DEFAULT, TrackedWaypoint.Type.EMPTY);
      }

      @Override
      public void handleUpdate(TrackedWaypoint waypoint) {
      }

      @Override
      public void writeAdditionalDataToBuf(ByteBuf buf) {
      }

      @Override
      public double getRelativeYaw(World world, TrackedWaypoint.YawProvider yawProvider, EntityTickProgress tickProgress) {
         return Double.NaN;
      }

      @Override
      public TrackedWaypoint.Pitch getPitch(World world, TrackedWaypoint.PitchProvider cameraProvider, EntityTickProgress tickProgress) {
         return TrackedWaypoint.Pitch.NONE;
      }

      @Override
      public double squaredDistanceTo(Entity receiver) {
         return Double.POSITIVE_INFINITY;
      }
   }

   public static enum Pitch {
      NONE,
      UP,
      DOWN;
   }

   public interface PitchProvider {
      Vec3d project(Vec3d sourcePos);

      double getPitch();
   }

   static class Positional extends TrackedWaypoint {
      private Vec3i pos;

      public Positional(UUID uuid, Waypoint.Config config, Vec3i pos) {
         super(Either.left(uuid), config, TrackedWaypoint.Type.VEC3I);
         this.pos = pos;
      }

      public Positional(Either<UUID, String> source, Waypoint.Config config, PacketByteBuf buf) {
         super(source, config, TrackedWaypoint.Type.VEC3I);
         this.pos = new Vec3i(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
      }

      @Override
      public void handleUpdate(TrackedWaypoint waypoint) {
         if (waypoint instanceof TrackedWaypoint.Positional positional) {
            this.pos = positional.pos;
         } else {
            TrackedWaypoint.LOGGER.warn("Unsupported Waypoint update operation: {}", waypoint.getClass());
         }
      }

      @Override
      public void writeAdditionalDataToBuf(ByteBuf buf) {
         VarInts.write(buf, this.pos.getX());
         VarInts.write(buf, this.pos.getY());
         VarInts.write(buf, this.pos.getZ());
      }

      private Vec3d getSourcePos(World world, EntityTickProgress tickProgress) {
         return this.source
            .left()
            .map(world::getEntity)
            .map(entity -> entity.getBlockPos().getManhattanDistance(this.pos) > 3 ? null : entity.getCameraPosVec(tickProgress.getTickProgress(entity)))
            .orElseGet(() -> Vec3d.ofCenter(this.pos));
      }

      @Override
      public double getRelativeYaw(World world, TrackedWaypoint.YawProvider yawProvider, EntityTickProgress tickProgress) {
         Vec3d vec3d = yawProvider.getCameraPos().subtract(this.getSourcePos(world, tickProgress)).rotateYClockwise();
         float f = (float)MathHelper.atan2(vec3d.getZ(), vec3d.getX()) * (180.0F / (float)Math.PI);
         return MathHelper.subtractAngles(yawProvider.getCameraYaw(), f);
      }

      @Override
      public TrackedWaypoint.Pitch getPitch(World world, TrackedWaypoint.PitchProvider cameraProvider, EntityTickProgress tickProgress) {
         Vec3d vec3d = cameraProvider.project(this.getSourcePos(world, tickProgress));
         boolean bl = vec3d.z > 1.0;
         double d = bl ? -vec3d.y : vec3d.y;
         if (d < -1.0) {
            return TrackedWaypoint.Pitch.DOWN;
         } else if (d > 1.0) {
            return TrackedWaypoint.Pitch.UP;
         } else {
            if (bl) {
               if (vec3d.y > 0.0) {
                  return TrackedWaypoint.Pitch.UP;
               }

               if (vec3d.y < 0.0) {
                  return TrackedWaypoint.Pitch.DOWN;
               }
            }

            return TrackedWaypoint.Pitch.NONE;
         }
      }

      @Override
      public double squaredDistanceTo(Entity receiver) {
         return receiver.squaredDistanceTo(Vec3d.ofCenter(this.pos));
      }
   }

   static enum Type {
      EMPTY(TrackedWaypoint.Empty::new),
      VEC3I(TrackedWaypoint.Positional::new),
      CHUNK(TrackedWaypoint.ChunkBased::new),
      AZIMUTH(TrackedWaypoint.Azimuth::new);

      final TriFunction<Either<UUID, String>, Waypoint.Config, PacketByteBuf, TrackedWaypoint> factory;

      private Type(final TriFunction<Either<UUID, String>, Waypoint.Config, PacketByteBuf, TrackedWaypoint> factory) {
         this.factory = factory;
      }
   }

   public interface YawProvider {
      float getCameraYaw();

      Vec3d getCameraPos();
   }
}
