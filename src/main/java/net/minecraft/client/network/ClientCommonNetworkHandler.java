package net.minecraft.client.network;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.dialog.DialogNetworkAccess;
import net.minecraft.client.gui.screen.dialog.DialogScreen;
import net.minecraft.client.gui.screen.dialog.DialogScreens;
import net.minecraft.client.gui.screen.dialog.WaitingForResponseScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.resource.server.ServerResourcePackLoader;
import net.minecraft.client.session.telemetry.WorldSession;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.listener.ServerPacketListener;
import net.minecraft.network.packet.BrandCustomPayload;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.UnknownCustomPayload;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.CookieResponseC2SPacket;
import net.minecraft.network.packet.c2s.common.CustomClickActionC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ClearDialogS2CPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.common.CookieRequestS2CPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.network.packet.s2c.common.CustomReportDetailsS2CPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackRemoveS2CPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.network.packet.s2c.common.ServerLinksS2CPacket;
import net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket;
import net.minecraft.network.packet.s2c.common.ShowDialogS2CPacket;
import net.minecraft.network.packet.s2c.common.StoreCookieS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.ServerLinks;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.crash.ReportType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public abstract class ClientCommonNetworkHandler implements ClientCommonPacketListener {
   private static final Text LOST_CONNECTION_TEXT = Text.translatable("disconnect.lost");
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final MinecraftClient client;
   protected final ClientConnection connection;
   protected final @Nullable ServerInfo serverInfo;
   protected @Nullable String brand;
   protected final WorldSession worldSession;
   protected final @Nullable Screen postDisconnectScreen;
   protected boolean transferring;
   private final List<ClientCommonNetworkHandler.QueuedPacket> queuedPackets = new ArrayList<>();
   protected final Map<Identifier, byte[]> serverCookies;
   protected Map<String, String> customReportDetails;
   private ServerLinks serverLinks;
   protected final Map<UUID, PlayerListEntry> seenPlayers;
   protected boolean seenInsecureChatWarning;

   protected ClientCommonNetworkHandler(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
      this.client = client;
      this.connection = connection;
      this.serverInfo = connectionState.serverInfo();
      this.brand = connectionState.serverBrand();
      this.worldSession = connectionState.worldSession();
      this.postDisconnectScreen = connectionState.postDisconnectScreen();
      this.serverCookies = connectionState.serverCookies();
      this.customReportDetails = connectionState.customReportDetails();
      this.serverLinks = connectionState.serverLinks();
      this.seenPlayers = new HashMap<>(connectionState.seenPlayers());
      this.seenInsecureChatWarning = connectionState.seenInsecureChatWarning();
   }

   public ServerLinks getServerLinks() {
      return this.serverLinks;
   }

   @Override
   public void onPacketException(Packet packet, Exception exception) {
      LOGGER.error("Failed to handle packet {}, disconnecting", packet, exception);
      Optional<Path> optional = this.savePacketErrorReport(packet, exception);
      Optional<URI> optional2 = this.serverLinks.getEntryFor(ServerLinks.Known.BUG_REPORT).map(ServerLinks.Entry::link);
      this.connection.disconnect(new DisconnectionInfo(Text.translatable("disconnect.packetError"), optional, optional2));
   }

   @Override
   public DisconnectionInfo createDisconnectionInfo(Text reason, Throwable exception) {
      Optional<Path> optional = this.savePacketErrorReport(null, exception);
      Optional<URI> optional2 = this.serverLinks.getEntryFor(ServerLinks.Known.BUG_REPORT).map(ServerLinks.Entry::link);
      return new DisconnectionInfo(reason, optional, optional2);
   }

   private Optional<Path> savePacketErrorReport(@Nullable Packet packet, Throwable exception) {
      CrashReport crashReport = CrashReport.create(exception, "Packet handling error");
      NetworkThreadUtils.fillCrashReport(crashReport, this, packet);
      Path path = this.client.runDirectory.toPath().resolve("debug");
      Path path2 = path.resolve("disconnect-" + Util.getFormattedCurrentTime() + "-client.txt");
      Optional<ServerLinks.Entry> optional = this.serverLinks.getEntryFor(ServerLinks.Known.BUG_REPORT);
      List<String> list = optional.<List<String>>map(bugReportEntry -> List.of("Server bug reporting link: " + bugReportEntry.link())).orElse(List.of());
      return crashReport.writeToFile(path2, ReportType.MINECRAFT_NETWORK_PROTOCOL_ERROR_REPORT, list) ? Optional.of(path2) : Optional.empty();
   }

   @Override
   public boolean accepts(Packet<?> packet) {
      return ClientCommonPacketListener.super.accepts(packet)
         ? true
         : this.transferring && (packet instanceof StoreCookieS2CPacket || packet instanceof ServerTransferS2CPacket);
   }

   @Override
   public void onKeepAlive(KeepAliveS2CPacket packet) {
      this.send(new KeepAliveC2SPacket(packet.getId()), () -> !RenderSystem.isFrozenAtPollEvents(), Duration.ofMinutes(1L));
   }

   @Override
   public void onPing(CommonPingS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      this.sendPacket(new CommonPongC2SPacket(packet.getParameter()));
   }

   @Override
   public void onCustomPayload(CustomPayloadS2CPacket packet) {
      CustomPayload customPayload = packet.payload();
      if (!(customPayload instanceof UnknownCustomPayload)) {
         NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
         if (customPayload instanceof BrandCustomPayload brandCustomPayload) {
            this.brand = brandCustomPayload.brand();
            this.worldSession.setBrand(brandCustomPayload.brand());
         } else {
            this.onCustomPayload(customPayload);
         }
      }
   }

   protected abstract void onCustomPayload(CustomPayload payload);

   @Override
   public void onResourcePackSend(ResourcePackSendS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      UUID uUID = packet.id();
      URL uRL = getParsedResourcePackUrl(packet.url());
      if (uRL == null) {
         this.connection.send(new ResourcePackStatusC2SPacket(uUID, ResourcePackStatusC2SPacket.Status.INVALID_URL));
      } else {
         String string = packet.hash();
         boolean bl = packet.required();
         ServerInfo.ResourcePackPolicy resourcePackPolicy = this.serverInfo != null
            ? this.serverInfo.getResourcePackPolicy()
            : ServerInfo.ResourcePackPolicy.PROMPT;
         if (resourcePackPolicy != ServerInfo.ResourcePackPolicy.PROMPT && (!bl || resourcePackPolicy != ServerInfo.ResourcePackPolicy.DISABLED)) {
            this.client.getServerResourcePackProvider().addResourcePack(uUID, uRL, string);
         } else {
            this.client.setScreen(this.createConfirmServerResourcePackScreen(uUID, uRL, string, bl, packet.prompt().orElse(null)));
         }
      }
   }

   @Override
   public void onResourcePackRemove(ResourcePackRemoveS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      packet.id().ifPresentOrElse(id -> this.client.getServerResourcePackProvider().remove(id), () -> this.client.getServerResourcePackProvider().removeAll());
   }

   static Text getPrompt(Text requirementPrompt, @Nullable Text customPrompt) {
      return (Text)(customPrompt == null ? requirementPrompt : Text.translatable("multiplayer.texturePrompt.serverPrompt", requirementPrompt, customPrompt));
   }

   private static @Nullable URL getParsedResourcePackUrl(String url) {
      try {
         URL uRL = new URL(url);
         String string = uRL.getProtocol();
         return !"http".equals(string) && !"https".equals(string) ? null : uRL;
      } catch (MalformedURLException var3) {
         return null;
      }
   }

   @Override
   public void onCookieRequest(CookieRequestS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      this.connection.send(new CookieResponseC2SPacket(packet.key(), this.serverCookies.get(packet.key())));
   }

   @Override
   public void onStoreCookie(StoreCookieS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      this.serverCookies.put(packet.key(), packet.payload());
   }

   @Override
   public void onCustomReportDetails(CustomReportDetailsS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      this.customReportDetails = packet.details();
   }

   @Override
   public void onServerLinks(ServerLinksS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      List<ServerLinks.StringifiedEntry> list = packet.links();
      Builder<ServerLinks.Entry> builder = ImmutableList.builderWithExpectedSize(list.size());

      for (ServerLinks.StringifiedEntry stringifiedEntry : list) {
         try {
            URI uRI = Util.validateUri(stringifiedEntry.link());
            builder.add(new ServerLinks.Entry(stringifiedEntry.type(), uRI));
         } catch (Exception var7) {
            LOGGER.warn("Received invalid link for type {}:{}", new Object[]{stringifiedEntry.type(), stringifiedEntry.link(), var7});
         }
      }

      this.serverLinks = new ServerLinks(builder.build());
   }

   @Override
   public void onShowDialog(ShowDialogS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      this.showDialog(packet.dialog(), this.client.currentScreen);
   }

   protected abstract DialogNetworkAccess createDialogNetworkAccess();

   public void showDialog(RegistryEntry<Dialog> dialog, @Nullable Screen previousScreen) {
      this.showDialog(dialog, this.createDialogNetworkAccess(), previousScreen);
   }

   protected void showDialog(RegistryEntry<Dialog> dialog, DialogNetworkAccess networkAccess, @Nullable Screen previousScreen) {
      if (previousScreen instanceof DialogScreen.WarningScreen warningScreen) {
         Screen screen = warningScreen.getDialogScreen();
         Screen screen2 = screen instanceof DialogScreen<?> dialogScreen ? dialogScreen.getParentScreen() : screen;
         DialogScreen<?> dialogScreenx = DialogScreens.create(dialog.value(), screen2, networkAccess);
         if (dialogScreenx != null) {
            warningScreen.setDialogScreen(dialogScreenx);
         } else {
            LOGGER.warn("Failed to show dialog for data {}", dialog);
         }
      } else {
         Screen screen3;
         if (previousScreen instanceof DialogScreen<?> dialogScreen2) {
            screen3 = dialogScreen2.getParentScreen();
         } else if (previousScreen instanceof WaitingForResponseScreen waitingForResponseScreen) {
            screen3 = waitingForResponseScreen.getParentScreen();
         } else {
            screen3 = previousScreen;
         }

         Screen screen = DialogScreens.create(dialog.value(), screen3, networkAccess);
         if (screen != null) {
            this.client.setScreen(screen);
         } else {
            LOGGER.warn("Failed to show dialog for data {}", dialog);
         }
      }
   }

   @Override
   public void onClearDialog(ClearDialogS2CPacket packet) {
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      this.clearDialog();
   }

   public void clearDialog() {
      if (this.client.currentScreen instanceof DialogScreen.WarningScreen warningScreen) {
         if (warningScreen.getDialogScreen() instanceof DialogScreen<?> dialogScreen) {
            warningScreen.setDialogScreen(dialogScreen.getParentScreen());
         }
      } else if (this.client.currentScreen instanceof DialogScreen<?> dialogScreen2) {
         this.client.setScreen(dialogScreen2.getParentScreen());
      }
   }

   @Override
   public void onServerTransfer(ServerTransferS2CPacket packet) {
      this.transferring = true;
      NetworkThreadUtils.forceMainThread(packet, this, this.client.getPacketApplyBatcher());
      if (this.serverInfo == null) {
         throw new IllegalStateException("Cannot transfer to server from singleplayer");
      } else {
         this.connection.disconnect(Text.translatable("disconnect.transfer"));
         this.connection.tryDisableAutoRead();
         this.connection.handleDisconnection();
         ServerAddress serverAddress = new ServerAddress(packet.host(), packet.port());
         ConnectScreen.connect(
            Objects.requireNonNullElseGet(this.postDisconnectScreen, TitleScreen::new),
            this.client,
            serverAddress,
            this.serverInfo,
            false,
            new CookieStorage(this.serverCookies, this.seenPlayers, this.seenInsecureChatWarning)
         );
      }
   }

   @Override
   public void onDisconnect(DisconnectS2CPacket packet) {
      this.connection.disconnect(packet.reason());
   }

   protected void sendQueuedPackets() {
      Iterator<ClientCommonNetworkHandler.QueuedPacket> iterator = this.queuedPackets.iterator();

      while (iterator.hasNext()) {
         ClientCommonNetworkHandler.QueuedPacket queuedPacket = iterator.next();
         if (queuedPacket.sendCondition().getAsBoolean()) {
            this.sendPacket(queuedPacket.packet);
            iterator.remove();
         } else if (queuedPacket.expirationTime() <= Util.getMeasuringTimeMs()) {
            iterator.remove();
         }
      }
   }

   public void sendPacket(Packet<?> packet) {
      this.connection.send(packet);
   }

   @Override
   public void onDisconnected(DisconnectionInfo info) {
      this.worldSession.onUnload();
      this.client.method_76795(this.createDisconnectedScreen(info), this.transferring);
      LOGGER.warn("Client disconnected with reason: {}", info.reason().getString());
   }

   @Override
   public void addCustomCrashReportInfo(CrashReport report, CrashReportSection section) {
      section.add("Is Local", () -> String.valueOf(this.connection.isLocal()));
      section.add("Server type", () -> this.serverInfo != null ? this.serverInfo.getServerType().toString() : "<none>");
      section.add("Server brand", () -> this.brand);
      if (!this.customReportDetails.isEmpty()) {
         CrashReportSection crashReportSection = report.addElement("Custom Server Details");
         this.customReportDetails.forEach(crashReportSection::add);
      }
   }

   protected Screen createDisconnectedScreen(DisconnectionInfo info) {
      Screen screen = Objects.requireNonNullElseGet(
         this.postDisconnectScreen, () -> (Screen)(this.serverInfo != null ? new MultiplayerScreen(new TitleScreen()) : new TitleScreen())
      );
      return this.serverInfo != null && this.serverInfo.isRealm()
         ? new DisconnectedScreen(screen, LOST_CONNECTION_TEXT, info, ScreenTexts.BACK)
         : new DisconnectedScreen(screen, LOST_CONNECTION_TEXT, info);
   }

   public @Nullable String getBrand() {
      return this.brand;
   }

   private void send(Packet<? extends ServerPacketListener> packet, BooleanSupplier sendCondition, Duration expiry) {
      if (sendCondition.getAsBoolean()) {
         this.sendPacket(packet);
      } else {
         this.queuedPackets.add(new ClientCommonNetworkHandler.QueuedPacket(packet, sendCondition, Util.getMeasuringTimeMs() + expiry.toMillis()));
      }
   }

   private Screen createConfirmServerResourcePackScreen(UUID id, URL url, String hash, boolean required, @Nullable Text prompt) {
      Screen screen = this.client.currentScreen;
      return screen instanceof ClientCommonNetworkHandler.ConfirmServerResourcePackScreen confirmServerResourcePackScreen
         ? confirmServerResourcePackScreen.add(this.client, id, url, hash, required, prompt)
         : new ClientCommonNetworkHandler.ConfirmServerResourcePackScreen(
            this.client, screen, List.of(new ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack(id, url, hash)), required, prompt
         );
   }

   @Environment(EnvType.CLIENT)
   protected abstract class CommonDialogNetworkAccess implements DialogNetworkAccess {
      @Override
      public void disconnect(Text reason) {
         ClientCommonNetworkHandler.this.connection.disconnect(reason);
         ClientCommonNetworkHandler.this.connection.handleDisconnection();
      }

      @Override
      public void showDialog(RegistryEntry<Dialog> dialog, @Nullable Screen afterActionScreen) {
         ClientCommonNetworkHandler.this.showDialog(dialog, this, afterActionScreen);
      }

      @Override
      public void sendCustomClickActionPacket(Identifier id, Optional<NbtElement> payload) {
         ClientCommonNetworkHandler.this.sendPacket(new CustomClickActionC2SPacket(id, payload));
      }

      @Override
      public ServerLinks getServerLinks() {
         return ClientCommonNetworkHandler.this.getServerLinks();
      }
   }

   @Environment(EnvType.CLIENT)
   class ConfirmServerResourcePackScreen extends ConfirmScreen {
      private final List<ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack> packs;
      private final @Nullable Screen parent;

      ConfirmServerResourcePackScreen(
         final @Nullable MinecraftClient client,
         final Screen parent,
         final List<ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack> pack,
         final @Nullable boolean required,
         final Text prompt
      ) {
         super(
            confirmed -> {
               client.setScreen(parent);
               ServerResourcePackLoader serverResourcePackLoader = client.getServerResourcePackProvider();
               if (confirmed) {
                  if (ClientCommonNetworkHandler.this.serverInfo != null) {
                     ClientCommonNetworkHandler.this.serverInfo.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.ENABLED);
                  }

                  serverResourcePackLoader.acceptAll();
               } else {
                  serverResourcePackLoader.declineAll();
                  if (required) {
                     ClientCommonNetworkHandler.this.connection.disconnect(Text.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                  } else if (ClientCommonNetworkHandler.this.serverInfo != null) {
                     ClientCommonNetworkHandler.this.serverInfo.setResourcePackPolicy(ServerInfo.ResourcePackPolicy.DISABLED);
                  }
               }

               for (ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack packx : pack) {
                  serverResourcePackLoader.addResourcePack(packx.id, packx.url, packx.hash);
               }

               if (ClientCommonNetworkHandler.this.serverInfo != null) {
                  ServerList.updateServerListEntry(ClientCommonNetworkHandler.this.serverInfo);
               }
            },
            required ? Text.translatable("multiplayer.requiredTexturePrompt.line1") : Text.translatable("multiplayer.texturePrompt.line1"),
            ClientCommonNetworkHandler.getPrompt(
               required
                  ? Text.translatable("multiplayer.requiredTexturePrompt.line2").formatted(Formatting.YELLOW, Formatting.BOLD)
                  : Text.translatable("multiplayer.texturePrompt.line2"),
               prompt
            ),
            required ? ScreenTexts.PROCEED : ScreenTexts.YES,
            required ? ScreenTexts.DISCONNECT : ScreenTexts.NO
         );
         this.packs = pack;
         this.parent = parent;
      }

      public ClientCommonNetworkHandler.ConfirmServerResourcePackScreen add(
         MinecraftClient client, UUID id, URL url, String hash, boolean required, @Nullable Text prompt
      ) {
         List<ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack> list = ImmutableList.<ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack>builderWithExpectedSize(this.packs.size() + 1)
            .addAll(this.packs)
            .add(new ClientCommonNetworkHandler.ConfirmServerResourcePackScreen.Pack(id, url, hash))
            .build();
         return ClientCommonNetworkHandler.this.new ConfirmServerResourcePackScreen(client, this.parent, list, required, prompt);
      }

      @Environment(EnvType.CLIENT)
      record Pack(UUID id, URL url, String hash) {
      }
   }

   @Environment(EnvType.CLIENT)
   record QueuedPacket(Packet<? extends ServerPacketListener> packet, BooleanSupplier sendCondition, long expirationTime) {
   }
}
