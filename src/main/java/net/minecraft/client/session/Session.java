package net.minecraft.client.session;

import com.mojang.util.UndashedUuid;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Session {
   private final String username;
   private final UUID uuid;
   private final String accessToken;
   private final Optional<String> xuid;
   private final Optional<String> clientId;

   public Session(String username, UUID uuid, String accessToken, Optional<String> xuid, Optional<String> clientId) {
      this.username = username;
      this.uuid = uuid;
      this.accessToken = accessToken;
      this.xuid = xuid;
      this.clientId = clientId;
   }

   public String getSessionId() {
      return "token:" + this.accessToken + ":" + UndashedUuid.toString(this.uuid);
   }

   public UUID getUuidOrNull() {
      return this.uuid;
   }

   public String getUsername() {
      return this.username;
   }

   public String getAccessToken() {
      return this.accessToken;
   }

   public Optional<String> getClientId() {
      return this.clientId;
   }

   public Optional<String> getXuid() {
      return this.xuid;
   }
}
