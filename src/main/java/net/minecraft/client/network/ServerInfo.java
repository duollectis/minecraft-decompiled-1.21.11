package net.minecraft.client.network;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.PngMetadata;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ServerInfo {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_FAVICON_SIZE = 1024;
   public String name;
   public String address;
   public Text playerCountLabel;
   public Text label;
   public ServerMetadata.@Nullable Players players;
   public long ping;
   public int protocolVersion = SharedConstants.getGameVersion().protocolVersion();
   public Text version = Text.literal(SharedConstants.getGameVersion().name());
   public List<Text> playerListSummary = Collections.emptyList();
   private ServerInfo.ResourcePackPolicy resourcePackPolicy = ServerInfo.ResourcePackPolicy.PROMPT;
   private byte @Nullable [] favicon;
   private ServerInfo.ServerType serverType;
   private int acceptedCodeOfConduct;
   private ServerInfo.Status status = ServerInfo.Status.INITIAL;

   public ServerInfo(String name, String address, ServerInfo.ServerType serverType) {
      this.name = name;
      this.address = address;
      this.serverType = serverType;
   }

   public NbtCompound toNbt() {
      NbtCompound nbtCompound = new NbtCompound();
      nbtCompound.putString("name", this.name);
      nbtCompound.putString("ip", this.address);
      nbtCompound.putNullable("icon", Codecs.BASE_64, this.favicon);
      nbtCompound.copyFromCodec(ServerInfo.ResourcePackPolicy.CODEC, this.resourcePackPolicy);
      if (this.acceptedCodeOfConduct != 0) {
         nbtCompound.putInt("acceptedCodeOfConduct", this.acceptedCodeOfConduct);
      }

      return nbtCompound;
   }

   public ServerInfo.ResourcePackPolicy getResourcePackPolicy() {
      return this.resourcePackPolicy;
   }

   public void setResourcePackPolicy(ServerInfo.ResourcePackPolicy resourcePackPolicy) {
      this.resourcePackPolicy = resourcePackPolicy;
   }

   public static ServerInfo fromNbt(NbtCompound root) {
      ServerInfo serverInfo = new ServerInfo(root.getString("name", ""), root.getString("ip", ""), ServerInfo.ServerType.OTHER);
      serverInfo.setFavicon(root.<byte[]>get("icon", Codecs.BASE_64).orElse(null));
      serverInfo.setResourcePackPolicy(
         root.<ServerInfo.ResourcePackPolicy>decode(ServerInfo.ResourcePackPolicy.CODEC).orElse(ServerInfo.ResourcePackPolicy.PROMPT)
      );
      serverInfo.acceptedCodeOfConduct = root.getInt("acceptedCodeOfConduct", 0);
      return serverInfo;
   }

   public byte @Nullable [] getFavicon() {
      return this.favicon;
   }

   public void setFavicon(byte @Nullable [] favicon) {
      this.favicon = favicon;
   }

   public boolean isLocal() {
      return this.serverType == ServerInfo.ServerType.LAN;
   }

   public boolean isRealm() {
      return this.serverType == ServerInfo.ServerType.REALM;
   }

   public ServerInfo.ServerType getServerType() {
      return this.serverType;
   }

   public boolean hasAcceptedCodeOfConduct(String codeOfConductText) {
      return this.acceptedCodeOfConduct == codeOfConductText.hashCode();
   }

   public void setAcceptedCodeOfConduct(String codeOfConductText) {
      this.acceptedCodeOfConduct = codeOfConductText.hashCode();
   }

   public void resetAcceptedCodeOfConduct() {
      this.acceptedCodeOfConduct = 0;
   }

   public void copyFrom(ServerInfo serverInfo) {
      this.address = serverInfo.address;
      this.name = serverInfo.name;
      this.favicon = serverInfo.favicon;
   }

   public void copyWithSettingsFrom(ServerInfo serverInfo) {
      this.copyFrom(serverInfo);
      this.setResourcePackPolicy(serverInfo.getResourcePackPolicy());
      this.serverType = serverInfo.serverType;
   }

   public ServerInfo.Status getStatus() {
      return this.status;
   }

   public void setStatus(ServerInfo.Status status) {
      this.status = status;
   }

   public static byte @Nullable [] validateFavicon(byte @Nullable [] favicon) {
      if (favicon != null) {
         try {
            PngMetadata pngMetadata = PngMetadata.fromBytes(favicon);
            if (pngMetadata.width() <= 1024 && pngMetadata.height() <= 1024) {
               return favicon;
            }
         } catch (IOException var2) {
            LOGGER.warn("Failed to decode server icon", var2);
         }
      }

      return null;
   }

   @Environment(EnvType.CLIENT)
   public static enum ResourcePackPolicy {
      ENABLED("enabled"),
      DISABLED("disabled"),
      PROMPT("prompt");

      public static final MapCodec<ServerInfo.ResourcePackPolicy> CODEC = Codec.BOOL
         .optionalFieldOf("acceptTextures")
         .xmap(value -> value.<ServerInfo.ResourcePackPolicy>map(acceptTextures -> acceptTextures ? ENABLED : DISABLED).orElse(PROMPT), value -> {
            return switch (value) {
               case ENABLED -> Optional.of(true);
               case DISABLED -> Optional.of(false);
               case PROMPT -> Optional.empty();
            };
         });
      private final Text name;

      private ResourcePackPolicy(final String name) {
         this.name = Text.translatable("manageServer.resourcePack." + name);
      }

      public Text getName() {
         return this.name;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum ServerType {
      LAN,
      REALM,
      OTHER;
   }

   @Environment(EnvType.CLIENT)
   public static enum Status {
      INITIAL,
      PINGING,
      UNREACHABLE,
      INCOMPATIBLE,
      SUCCESSFUL;
   }
}
