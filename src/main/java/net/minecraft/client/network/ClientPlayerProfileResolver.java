package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.GameProfileResolver;

@Environment(EnvType.CLIENT)
public class ClientPlayerProfileResolver implements GameProfileResolver {
   private final MinecraftClient client;
   private final GameProfileResolver profileResolver;

   public ClientPlayerProfileResolver(MinecraftClient client, GameProfileResolver profileResolver) {
      this.client = client;
      this.profileResolver = profileResolver;
   }

   @Override
   public Optional<GameProfile> getProfileByName(String name) {
      ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.getNetworkHandler();
      if (clientPlayNetworkHandler != null) {
         PlayerListEntry playerListEntry = clientPlayNetworkHandler.getCaseInsensitivePlayerInfo(name);
         if (playerListEntry != null) {
            return Optional.of(playerListEntry.getProfile());
         }
      }

      return this.profileResolver.getProfileByName(name);
   }

   @Override
   public Optional<GameProfile> getProfileById(UUID id) {
      ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.getNetworkHandler();
      if (clientPlayNetworkHandler != null) {
         PlayerListEntry playerListEntry = clientPlayNetworkHandler.getPlayerListEntry(id);
         if (playerListEntry != null) {
            return Optional.of(playerListEntry.getProfile());
         }
      }

      return this.profileResolver.getProfileById(id);
   }
}
