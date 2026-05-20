package net.minecraft.client.network;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.network.listener.ClientQueryPacketListener;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.ServerMetadata;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;
import net.minecraft.util.Util;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class MultiplayerServerListPinger {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Text CANNOT_CONNECT_TEXT = Text.translatable("multiplayer.status.cannot_connect").withColor(-65536);
   private final List<ClientConnection> clientConnections = Collections.synchronizedList(Lists.newArrayList());

   public void add(ServerInfo entry, Runnable saver, Runnable pingCallback, NetworkingBackend backend) throws UnknownHostException {
      final ServerAddress serverAddress = ServerAddress.parse(entry.address);
      Optional<InetSocketAddress> optional = AllowedAddressResolver.DEFAULT.resolve(serverAddress).map(Address::getInetSocketAddress);
      if (optional.isEmpty()) {
         this.showError(ConnectScreen.UNKNOWN_HOST_TEXT, entry);
      } else {
         final InetSocketAddress inetSocketAddress = optional.get();
         final ClientConnection clientConnection = ClientConnection.connect(inetSocketAddress, backend, (MultiValueDebugSampleLogImpl) null);
         this.clientConnections.add(clientConnection);
         entry.label = Text.translatable("multiplayer.status.pinging");
         entry.playerListSummary = Collections.emptyList();
         ClientQueryPacketListener clientQueryPacketListener = new ClientQueryPacketListener() {
            private boolean sentQuery;
            private boolean received;
            private long startTime;

            @Override
            public void onResponse(QueryResponseS2CPacket packet) {
               if (this.received) {
                  clientConnection.disconnect(Text.translatable("multiplayer.status.unrequested"));
               } else {
                  this.received = true;
                  ServerMetadata serverMetadata = packet.metadata();
                  entry.label = serverMetadata.description();
                  serverMetadata.version().ifPresentOrElse(version -> {
                     entry.version = Text.literal(version.gameVersion());
                     entry.protocolVersion = version.protocolVersion();
                  }, () -> {
                     entry.version = Text.translatable("multiplayer.status.old");
                     entry.protocolVersion = 0;
                  });
                  serverMetadata.players().ifPresentOrElse(players -> {
                     entry.playerCountLabel = MultiplayerServerListPinger.createPlayerCountText(players.online(), players.max());
                     entry.players = players;
                     if (!players.sample().isEmpty()) {
                        List<Text> list = new ArrayList<>(players.sample().size());

                        for (PlayerConfigEntry playerConfigEntry : players.sample()) {
                           Text text;
                           if (playerConfigEntry.equals(MinecraftServer.ANONYMOUS_PLAYER_PROFILE)) {
                              text = Text.translatable("multiplayer.status.anonymous_player");
                           } else {
                              text = Text.literal(playerConfigEntry.name());
                           }

                           list.add(text);
                        }

                        if (players.sample().size() < players.online()) {
                           list.add(Text.translatable("multiplayer.status.and_more", players.online() - players.sample().size()));
                        }

                        entry.playerListSummary = list;
                     } else {
                        entry.playerListSummary = List.of();
                     }
                  }, () -> entry.playerCountLabel = Text.translatable("multiplayer.status.unknown").formatted(Formatting.DARK_GRAY));
                  serverMetadata.favicon().ifPresent(favicon -> {
                     if (!Arrays.equals(favicon.iconBytes(), entry.getFavicon())) {
                        entry.setFavicon(ServerInfo.validateFavicon(favicon.iconBytes()));
                        saver.run();
                     }
                  });
                  this.startTime = Util.getMeasuringTimeMs();
                  clientConnection.send(new QueryPingC2SPacket(this.startTime));
                  this.sentQuery = true;
               }
            }

            @Override
            public void onPingResult(PingResultS2CPacket packet) {
               long l = this.startTime;
               long m = Util.getMeasuringTimeMs();
               entry.ping = m - l;
               clientConnection.disconnect(Text.translatable("multiplayer.status.finished"));
               pingCallback.run();
            }

            @Override
            public void onDisconnected(DisconnectionInfo info) {
               if (!this.sentQuery) {
                  MultiplayerServerListPinger.this.showError(info.reason(), entry);
                  MultiplayerServerListPinger.this.ping(inetSocketAddress, serverAddress, entry, backend);
               }
            }

            @Override
            public boolean isConnectionOpen() {
               return clientConnection.isOpen();
            }
         };

         try {
            clientConnection.connect(serverAddress.getAddress(), serverAddress.getPort(), clientQueryPacketListener);
            clientConnection.send(QueryRequestC2SPacket.INSTANCE);
         } catch (Throwable var11) {
            LOGGER.error("Failed to ping server {}", serverAddress, var11);
         }
      }
   }

   void showError(Text error, ServerInfo info) {
      LOGGER.error("Can't ping {}: {}", info.address, error.getString());
      info.label = CANNOT_CONNECT_TEXT;
      info.playerCountLabel = ScreenTexts.EMPTY;
   }

   void ping(InetSocketAddress socketAddress, ServerAddress address, ServerInfo serverInfo, NetworkingBackend backend) {
      ((Bootstrap)((Bootstrap)((Bootstrap)new Bootstrap().group(backend.getEventLoopGroup())).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(Channel channel) {
            try {
               channel.config().setOption(ChannelOption.TCP_NODELAY, true);
            } catch (ChannelException var3) {
            }

            channel.pipeline().addLast(new ChannelHandler[]{new LegacyServerPinger(address, (protocolVersion, version, label, currentPlayers, maxPlayers) -> {
               serverInfo.setStatus(ServerInfo.Status.INCOMPATIBLE);
               serverInfo.version = Text.literal(version);
               serverInfo.label = Text.literal(label);
               serverInfo.playerCountLabel = MultiplayerServerListPinger.createPlayerCountText(currentPlayers, maxPlayers);
               serverInfo.players = new ServerMetadata.Players(maxPlayers, currentPlayers, List.of());
            })});
         }
      })).channel(backend.getChannelClass())).connect(socketAddress.getAddress(), socketAddress.getPort());
   }

   public static Text createPlayerCountText(int current, int max) {
      Text text = Text.literal(Integer.toString(current)).formatted(Formatting.GRAY);
      Text text2 = Text.literal(Integer.toString(max)).formatted(Formatting.GRAY);
      return Text.translatable("multiplayer.status.player_count", text, text2).formatted(Formatting.DARK_GRAY);
   }

   public void tick() {
      synchronized (this.clientConnections) {
         Iterator<ClientConnection> iterator = this.clientConnections.iterator();

         while (iterator.hasNext()) {
            ClientConnection clientConnection = iterator.next();
            if (clientConnection.isOpen()) {
               clientConnection.tick();
            } else {
               iterator.remove();
               clientConnection.handleDisconnection();
            }
         }
      }
   }

   public void cancel() {
      synchronized (this.clientConnections) {
         Iterator<ClientConnection> iterator = this.clientConnections.iterator();

         while (iterator.hasNext()) {
            ClientConnection clientConnection = iterator.next();
            if (clientConnection.isOpen()) {
               iterator.remove();
               clientConnection.disconnect(Text.translatable("multiplayer.status.cancelled"));
            }
         }
      }
   }
}
