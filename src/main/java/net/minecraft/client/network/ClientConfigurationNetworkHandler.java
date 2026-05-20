package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.dialog.DialogNetworkAccess;
import net.minecraft.client.gui.screen.multiplayer.CodeOfConductScreen;
import net.minecraft.client.resource.ClientDataPackManager;
import net.minecraft.client.world.ClientChunkLoadProgress;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.listener.ClientConfigurationPacketListener;
import net.minecraft.network.listener.TickablePacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.config.AcceptCodeOfConductC2SPacket;
import net.minecraft.network.packet.c2s.config.ReadyC2SPacket;
import net.minecraft.network.packet.c2s.config.SelectKnownPacksC2SPacket;
import net.minecraft.network.packet.s2c.common.SynchronizeTagsS2CPacket;
import net.minecraft.network.packet.s2c.config.CodeOfConductS2CPacket;
import net.minecraft.network.packet.s2c.config.DynamicRegistriesS2CPacket;
import net.minecraft.network.packet.s2c.config.FeaturesS2CPacket;
import net.minecraft.network.packet.s2c.config.ReadyS2CPacket;
import net.minecraft.network.packet.s2c.config.ResetChatS2CPacket;
import net.minecraft.network.packet.s2c.config.SelectKnownPacksS2CPacket;
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.resource.LifecycledResourceManager;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ClientConfigurationNetworkHandler extends ClientCommonNetworkHandler implements ClientConfigurationPacketListener, TickablePacketListener {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final Text CODE_OF_CONDUCT_DISCONNECT_REASON = Text.translatable("multiplayer.disconnect.code_of_conduct");
   private final ClientChunkLoadProgress chunkLoadProgress;
   private final GameProfile profile;
   private FeatureSet enabledFeatures;
   private final DynamicRegistryManager.Immutable registryManager;
   private final ClientRegistries clientRegistries = new ClientRegistries();
   private @Nullable ClientDataPackManager dataPackManager;
   protected ChatHud.@Nullable ChatState chatState;
   private boolean receivedCodeOfConduct;

   public ClientConfigurationNetworkHandler(MinecraftClient minecraftClient, ClientConnection clientConnection, ClientConnectionState clientConnectionState) {
      super(minecraftClient, clientConnection, clientConnectionState);
      this.chunkLoadProgress = clientConnectionState.chunkLoadProgress();
      this.profile = clientConnectionState.localGameProfile();
      this.registryManager = clientConnectionState.receivedRegistries();
      this.enabledFeatures = clientConnectionState.enabledFeatures();
      this.chatState = clientConnectionState.chatState();
   }

   @Override
   public boolean isConnectionOpen() {
      return this.connection.isOpen();
   }

   @Override
   protected void onCustomPayload(CustomPayload payload) {
      this.handleCustomPayload(payload);
   }

   private void handleCustomPayload(CustomPayload payload) {
      LOGGER.warn("Unknown custom packet payload: {}", payload.getId().id());
   }

   @Override
   public void onDynamicRegistries(DynamicRegistriesS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      this.clientRegistries.putDynamicRegistry(packet.registry(), packet.entries());
   }

   @Override
   public void onSynchronizeTags(SynchronizeTagsS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      this.clientRegistries.putTags(packet.getGroups());
   }

   @Override
   public void onFeatures(FeaturesS2CPacket packet) {
      this.enabledFeatures = FeatureFlags.FEATURE_MANAGER.featureSetOf(packet.features());
   }

   @Override
   public void onSelectKnownPacks(SelectKnownPacksS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      if (this.dataPackManager == null) {
         this.dataPackManager = new ClientDataPackManager();
      }

      List<VersionedIdentifier> list = this.dataPackManager.getCommonKnownPacks(packet.knownPacks());
      this.sendPacket(new SelectKnownPacksC2SPacket(list));
   }

   @Override
   public void onResetChat(ResetChatS2CPacket packet) {
      this.chatState = null;
   }

   private <T> T openClientDataPack(Function<ResourceFactory, T> opener) {
      if (this.dataPackManager == null) {
         return opener.apply(ResourceFactory.MISSING);
      } else {
         Object var3;
         try (LifecycledResourceManager lifecycledResourceManager = this.dataPackManager.createResourceManager()) {
            var3 = opener.apply(lifecycledResourceManager);
         }

         return (T)var3;
      }
   }

   @Override
   public void onCodeOfConduct(CodeOfConductS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      if (this.receivedCodeOfConduct) {
         throw new IllegalStateException("Server sent duplicate Code of Conduct");
      } else {
         this.receivedCodeOfConduct = true;
         String string = packet.codeOfConduct();
         if (this.serverInfo != null && this.serverInfo.hasAcceptedCodeOfConduct(string)) {
            this.sendPacket(AcceptCodeOfConductC2SPacket.INSTANCE);
         } else {
            Screen screen = this.client.currentScreen;
            this.client.setScreen(new CodeOfConductScreen(this.serverInfo, screen, string, acknowledged -> {
               if (acknowledged) {
                  this.sendPacket(AcceptCodeOfConductC2SPacket.INSTANCE);
                  this.client.setScreen(screen);
               } else {
                  this.createDialogNetworkAccess().disconnect(CODE_OF_CONDUCT_DISCONNECT_REASON);
               }
            }));
         }
      }
   }

   @Override
   public void onReady(ReadyS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      DynamicRegistryManager.Immutable immutable = this.openClientDataPack(
         factory -> this.clientRegistries.createRegistryManager(factory, this.registryManager, this.connection.isLocal())
      );
      this.connection
         .transitionInbound(
            PlayStateFactories.S2C.bind(RegistryByteBuf.makeFactory(immutable)),
            new ClientPlayNetworkHandler(
               this.client,
               this.connection,
               new ClientConnectionState(
                  this.chunkLoadProgress,
                  this.profile,
                  this.worldSession,
                  immutable,
                  this.enabledFeatures,
                  this.brand,
                  this.serverInfo,
                  this.postDisconnectScreen,
                  this.serverCookies,
                  this.chatState,
                  this.customReportDetails,
                  this.getServerLinks(),
                  this.seenPlayers,
                  this.seenInsecureChatWarning
               )
            )
         );
      this.connection.send(ReadyC2SPacket.INSTANCE);
      this.connection
         .transitionOutbound(PlayStateFactories.C2S.bind(RegistryByteBuf.makeFactory(immutable), new PlayStateFactories.PacketCodecModifierContext() {
            @Override
            public boolean isInCreativeMode() {
               return true;
            }
         }));
   }

   @Override
   public void tick() {
      this.sendQueuedPackets();
   }

   @Override
   public void onDisconnected(DisconnectionInfo info) {
      super.onDisconnected(info);
      this.client.onDisconnected();
   }

   @Override
   protected DialogNetworkAccess createDialogNetworkAccess() {
      return new ClientCommonNetworkHandler.CommonDialogNetworkAccess() {
         @Override
         public void runClickEventCommand(String command, @Nullable Screen afterActionScreen) {
            ClientConfigurationNetworkHandler.LOGGER.warn("Commands are not supported in configuration phase, trying to run '{}'", command);
         }
      };
   }
}
