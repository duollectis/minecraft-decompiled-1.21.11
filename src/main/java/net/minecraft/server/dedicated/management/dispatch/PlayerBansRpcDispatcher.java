package net.minecraft.server.dedicated.management.dispatch;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.dedicated.management.RpcPlayer;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

public class PlayerBansRpcDispatcher {
   private static final String DEFAULT_SOURCE = "Management server";

   public static List<PlayerBansRpcDispatcher.RpcEntry> get(ManagementHandlerDispatcher dispatcher) {
      return dispatcher.getBanHandler()
         .getUserBanList()
         .stream()
         .filter(entry -> entry.getKey() != null)
         .map(PlayerBansRpcDispatcher.ConfigEntry::of)
         .map(PlayerBansRpcDispatcher.RpcEntry::of)
         .toList();
   }

   public static List<PlayerBansRpcDispatcher.RpcEntry> add(
      ManagementHandlerDispatcher dispatcher, List<PlayerBansRpcDispatcher.RpcEntry> players, ManagementConnectionId remote
   ) {
      List<CompletableFuture<Optional<PlayerBansRpcDispatcher.ConfigEntry>>> list = players.stream()
         .map(
            player -> dispatcher.getPlayerListHandler()
               .getPlayerAsync(player.player().id(), player.player().name())
               .thenApply(playerEntry -> playerEntry.map(player::toConfigEntry))
         )
         .toList();

      for (Optional<PlayerBansRpcDispatcher.ConfigEntry> optional : Util.combineSafe(list).join()) {
         if (!optional.isEmpty()) {
            PlayerBansRpcDispatcher.ConfigEntry configEntry = optional.get();
            dispatcher.getBanHandler().addPlayer(configEntry.toBannedPlayerEntry(), remote);
            ServerPlayerEntity serverPlayerEntity = dispatcher.getPlayerListHandler().getPlayer(optional.get().player().id());
            if (serverPlayerEntity != null) {
               serverPlayerEntity.networkHandler.disconnect(Text.translatable("multiplayer.disconnect.banned"));
            }
         }
      }

      return get(dispatcher);
   }

   public static List<PlayerBansRpcDispatcher.RpcEntry> clear(ManagementHandlerDispatcher dispatcher, ManagementConnectionId remote) {
      dispatcher.getBanHandler().clearBanList(remote);
      return get(dispatcher);
   }

   public static List<PlayerBansRpcDispatcher.RpcEntry> remove(ManagementHandlerDispatcher dispatcher, List<RpcPlayer> players, ManagementConnectionId remote) {
      List<CompletableFuture<Optional<PlayerConfigEntry>>> list = players.stream()
         .map(player -> dispatcher.getPlayerListHandler().getPlayerAsync(player.id(), player.name()))
         .toList();

      for (Optional<PlayerConfigEntry> optional : Util.combineSafe(list).join()) {
         if (!optional.isEmpty()) {
            dispatcher.getBanHandler().removePlayer(optional.get(), remote);
         }
      }

      return get(dispatcher);
   }

   public static List<PlayerBansRpcDispatcher.RpcEntry> set(
      ManagementHandlerDispatcher dispatcher, List<PlayerBansRpcDispatcher.RpcEntry> players, ManagementConnectionId remote
   ) {
      List<CompletableFuture<Optional<PlayerBansRpcDispatcher.ConfigEntry>>> list = players.stream()
         .map(
            player -> dispatcher.getPlayerListHandler()
               .getPlayerAsync(player.player().id(), player.player().name())
               .thenApply(playerEntry -> playerEntry.map(player::toConfigEntry))
         )
         .toList();
      Set<PlayerBansRpcDispatcher.ConfigEntry> set = Util.combineSafe(list).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
      Set<PlayerBansRpcDispatcher.ConfigEntry> set2 = dispatcher.getBanHandler()
         .getUserBanList()
         .stream()
         .filter(entry -> entry.getKey() != null)
         .map(PlayerBansRpcDispatcher.ConfigEntry::of)
         .collect(Collectors.toSet());
      set2.stream().filter(player -> !set.contains(player)).forEach(player -> dispatcher.getBanHandler().removePlayer(player.player(), remote));
      set.stream().filter(player -> !set2.contains(player)).forEach(player -> {
         dispatcher.getBanHandler().addPlayer(player.toBannedPlayerEntry(), remote);
         ServerPlayerEntity serverPlayerEntity = dispatcher.getPlayerListHandler().getPlayer(player.player().id());
         if (serverPlayerEntity != null) {
            serverPlayerEntity.networkHandler.disconnect(Text.translatable("multiplayer.disconnect.banned"));
         }
      });
      return get(dispatcher);
   }

   record ConfigEntry(PlayerConfigEntry player, @Nullable String reason, String source, Optional<Instant> expires) {
      static PlayerBansRpcDispatcher.ConfigEntry of(BannedPlayerEntry entry) {
         return new PlayerBansRpcDispatcher.ConfigEntry(
            Objects.requireNonNull(entry.getKey()), entry.getReason(), entry.getSource(), Optional.ofNullable(entry.getExpiryDate()).map(Date::toInstant)
         );
      }

      BannedPlayerEntry toBannedPlayerEntry() {
         return new BannedPlayerEntry(
            new PlayerConfigEntry(this.player().id(), this.player().name()), null, this.source(), this.expires().map(Date::from).orElse(null), this.reason()
         );
      }
   }

   public record RpcEntry(RpcPlayer player, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<PlayerBansRpcDispatcher.RpcEntry> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               RpcPlayer.CODEC.codec().fieldOf("player").forGetter(PlayerBansRpcDispatcher.RpcEntry::player),
               Codec.STRING.optionalFieldOf("reason").forGetter(PlayerBansRpcDispatcher.RpcEntry::reason),
               Codec.STRING.optionalFieldOf("source").forGetter(PlayerBansRpcDispatcher.RpcEntry::source),
               Codecs.INSTANT.optionalFieldOf("expires").forGetter(PlayerBansRpcDispatcher.RpcEntry::expires)
            )
            .apply(instance, PlayerBansRpcDispatcher.RpcEntry::new)
      );

      private static PlayerBansRpcDispatcher.RpcEntry of(PlayerBansRpcDispatcher.ConfigEntry entry) {
         return new PlayerBansRpcDispatcher.RpcEntry(
            RpcPlayer.of(entry.player()), Optional.ofNullable(entry.reason()), Optional.of(entry.source()), entry.expires()
         );
      }

      public static PlayerBansRpcDispatcher.RpcEntry of(BannedPlayerEntry entry) {
         return of(PlayerBansRpcDispatcher.ConfigEntry.of(entry));
      }

      private PlayerBansRpcDispatcher.ConfigEntry toConfigEntry(PlayerConfigEntry player) {
         return new PlayerBansRpcDispatcher.ConfigEntry(player, this.reason().orElse(null), this.source().orElse("Management server"), this.expires());
      }
   }
}
