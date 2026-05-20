package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.session.telemetry.WorldSession;
import net.minecraft.client.world.ClientChunkLoadProgress;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record ClientConnectionState(
   ClientChunkLoadProgress chunkLoadProgress,
   GameProfile localGameProfile,
   WorldSession worldSession,
   DynamicRegistryManager.Immutable receivedRegistries,
   FeatureSet enabledFeatures,
   @Nullable String serverBrand,
   @Nullable ServerInfo serverInfo,
   @Nullable Screen postDisconnectScreen,
   Map<Identifier, byte[]> serverCookies,
   ChatHud.@Nullable ChatState chatState,
   Map<String, String> customReportDetails,
   ServerLinks serverLinks,
   Map<UUID, PlayerListEntry> seenPlayers,
   boolean seenInsecureChatWarning
) {
}
