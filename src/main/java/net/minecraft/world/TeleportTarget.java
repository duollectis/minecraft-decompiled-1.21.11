package net.minecraft.world;

import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public record TeleportTarget(
   ServerWorld world,
   Vec3d position,
   Vec3d velocity,
   float yaw,
   float pitch,
   boolean missingRespawnBlock,
   boolean asPassenger,
   Set<PositionFlag> relatives,
   TeleportTarget.PostDimensionTransition postTeleportTransition
) {
   public static final TeleportTarget.PostDimensionTransition NO_OP = entity -> {};
   public static final TeleportTarget.PostDimensionTransition SEND_TRAVEL_THROUGH_PORTAL_PACKET = TeleportTarget::sendTravelThroughPortalPacket;
   public static final TeleportTarget.PostDimensionTransition ADD_PORTAL_CHUNK_TICKET = TeleportTarget::addPortalChunkTicket;

   public TeleportTarget(ServerWorld world, Vec3d pos, Vec3d velocity, float yaw, float pitch, TeleportTarget.PostDimensionTransition postDimensionTransition) {
      this(world, pos, velocity, yaw, pitch, Set.of(), postDimensionTransition);
   }

   public TeleportTarget(
      ServerWorld world,
      Vec3d pos,
      Vec3d velocity,
      float yaw,
      float pitch,
      Set<PositionFlag> flags,
      TeleportTarget.PostDimensionTransition postDimensionTransition
   ) {
      this(world, pos, velocity, yaw, pitch, false, false, flags, postDimensionTransition);
   }

   private static void sendTravelThroughPortalPacket(Entity entity) {
      if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
         serverPlayerEntity.networkHandler.sendPacket(new WorldEventS2CPacket(1032, BlockPos.ORIGIN, 0, false));
      }
   }

   private static void addPortalChunkTicket(Entity entity) {
      entity.addPortalChunkTicketAt(BlockPos.ofFloored(entity.getEntityPos()));
   }

   public static TeleportTarget noRespawnPointSet(ServerPlayerEntity player, TeleportTarget.PostDimensionTransition postDimensionTransition) {
      ServerWorld serverWorld = player.getEntityWorld().getServer().getSpawnWorld();
      WorldProperties.SpawnPoint spawnPoint = serverWorld.getSpawnPoint();
      return new TeleportTarget(
         serverWorld, getWorldSpawnPos(serverWorld, player), Vec3d.ZERO, spawnPoint.yaw(), spawnPoint.pitch(), false, false, Set.of(), postDimensionTransition
      );
   }

   public static TeleportTarget missingSpawnBlock(ServerPlayerEntity player, TeleportTarget.PostDimensionTransition postDimensionTransition) {
      ServerWorld serverWorld = player.getEntityWorld().getServer().getSpawnWorld();
      WorldProperties.SpawnPoint spawnPoint = serverWorld.getSpawnPoint();
      return new TeleportTarget(
         serverWorld, getWorldSpawnPos(serverWorld, player), Vec3d.ZERO, spawnPoint.yaw(), spawnPoint.pitch(), true, false, Set.of(), postDimensionTransition
      );
   }

   private static Vec3d getWorldSpawnPos(ServerWorld world, Entity entity) {
      return entity.getWorldSpawnPos(world, world.getSpawnPoint().getPos()).toBottomCenterPos();
   }

   public TeleportTarget withRotation(float yaw, float pitch) {
      return new TeleportTarget(
         this.world(),
         this.position(),
         this.velocity(),
         yaw,
         pitch,
         this.missingRespawnBlock(),
         this.asPassenger(),
         this.relatives(),
         this.postTeleportTransition()
      );
   }

   public TeleportTarget withPosition(Vec3d position) {
      return new TeleportTarget(
         this.world(),
         position,
         this.velocity(),
         this.yaw(),
         this.pitch(),
         this.missingRespawnBlock(),
         this.asPassenger(),
         this.relatives(),
         this.postTeleportTransition()
      );
   }

   public TeleportTarget withAsPassenger() {
      return new TeleportTarget(
         this.world(),
         this.position(),
         this.velocity(),
         this.yaw(),
         this.pitch(),
         this.missingRespawnBlock(),
         true,
         this.relatives(),
         this.postTeleportTransition()
      );
   }

   @FunctionalInterface
   public interface PostDimensionTransition {
      void onTransition(Entity entity);

      default TeleportTarget.PostDimensionTransition then(TeleportTarget.PostDimensionTransition next) {
         return entity -> {
            this.onTransition(entity);
            next.onTransition(entity);
         };
      }
   }
}
