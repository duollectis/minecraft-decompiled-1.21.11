package net.minecraft.server.dedicated.management.handler;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public interface PlayerListManagementHandler {
   List<ServerPlayerEntity> getPlayerList();

   @Nullable ServerPlayerEntity getPlayer(UUID uuid);

   default CompletableFuture<Optional<PlayerConfigEntry>> getPlayerAsync(Optional<UUID> uuid, Optional<String> name) {
      if (uuid.isPresent()) {
         Optional<PlayerConfigEntry> optional = this.getByUuid(uuid.get());
         return optional.isPresent()
            ? CompletableFuture.completedFuture(optional)
            : CompletableFuture.supplyAsync(() -> this.fetchPlayer(uuid.get()), Util.getDownloadWorkerExecutor());
      } else {
         return name.isPresent()
            ? CompletableFuture.supplyAsync(() -> this.findByName(name.get()), Util.getDownloadWorkerExecutor())
            : CompletableFuture.completedFuture(Optional.empty());
      }
   }

   Optional<PlayerConfigEntry> findByName(String name);

   Optional<PlayerConfigEntry> fetchPlayer(UUID uuid);

   Optional<PlayerConfigEntry> getByUuid(UUID uuid);

   Optional<ServerPlayerEntity> getPlayer(Optional<UUID> uuid, Optional<String> name);

   List<ServerPlayerEntity> getPlayersByIpAddress(String ipAddress);

   @Nullable ServerPlayerEntity getPlayer(String name);

   void removePlayer(ServerPlayerEntity player, ManagementConnectionId remote);
}
