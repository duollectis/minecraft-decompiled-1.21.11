package net.minecraft.server.dedicated.management.dispatch;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.ServerConfigEntry;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.dedicated.management.RpcPlayer;
import net.minecraft.server.dedicated.management.network.ManagementConnectionId;
import net.minecraft.util.Util;

public class AllowlistRpcDispatcher {
   public static List<RpcPlayer> get(ManagementHandlerDispatcher dispatcher) {
      return dispatcher.getAllowlistHandler()
         .getAllowlist()
         .stream()
         .filter(entry -> entry.getKey() != null)
         .map(player -> RpcPlayer.of(player.getKey()))
         .toList();
   }

   public static List<RpcPlayer> add(ManagementHandlerDispatcher dispatcher, List<RpcPlayer> players, ManagementConnectionId remote) {
      List<CompletableFuture<Optional<PlayerConfigEntry>>> list = players.stream()
         .map(player -> dispatcher.getPlayerListHandler().getPlayerAsync(player.id(), player.name()))
         .toList();

      for (Optional<PlayerConfigEntry> optional : Util.combineSafe(list).join()) {
         optional.ifPresent(player -> dispatcher.getAllowlistHandler().add(new WhitelistEntry(player), remote));
      }

      return get(dispatcher);
   }

   public static List<RpcPlayer> clear(ManagementHandlerDispatcher dispatcher, ManagementConnectionId remote) {
      dispatcher.getAllowlistHandler().clear(remote);
      return get(dispatcher);
   }

   public static List<RpcPlayer> remove(ManagementHandlerDispatcher dispatcher, List<RpcPlayer> players, ManagementConnectionId remote) {
      List<CompletableFuture<Optional<PlayerConfigEntry>>> list = players.stream()
         .map(player -> dispatcher.getPlayerListHandler().getPlayerAsync(player.id(), player.name()))
         .toList();

      for (Optional<PlayerConfigEntry> optional : Util.combineSafe(list).join()) {
         optional.ifPresent(player -> dispatcher.getAllowlistHandler().remove(player, remote));
      }

      dispatcher.getAllowlistHandler().kickUnlisted(remote);
      return get(dispatcher);
   }

   public static List<RpcPlayer> set(ManagementHandlerDispatcher dispatcher, List<RpcPlayer> players, ManagementConnectionId remote) {
      List<CompletableFuture<Optional<PlayerConfigEntry>>> list = players.stream()
         .map(player -> dispatcher.getPlayerListHandler().getPlayerAsync(player.id(), player.name()))
         .toList();
      Set<PlayerConfigEntry> set = Util.combineSafe(list).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
      Set<PlayerConfigEntry> set2 = dispatcher.getAllowlistHandler().getAllowlist().stream().map(ServerConfigEntry::getKey).collect(Collectors.toSet());
      set2.stream().filter(player -> !set.contains(player)).forEach(player -> dispatcher.getAllowlistHandler().remove(player, remote));
      set.stream().filter(player -> !set2.contains(player)).forEach(player -> dispatcher.getAllowlistHandler().add(new WhitelistEntry(player), remote));
      dispatcher.getAllowlistHandler().kickUnlisted(remote);
      return get(dispatcher);
   }
}
