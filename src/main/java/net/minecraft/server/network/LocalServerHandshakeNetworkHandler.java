package net.minecraft.server.network;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.listener.ServerHandshakePacketListener;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.state.LoginStates;
import net.minecraft.server.MinecraftServer;

public class LocalServerHandshakeNetworkHandler implements ServerHandshakePacketListener {
   private final MinecraftServer server;
   private final ClientConnection connection;

   public LocalServerHandshakeNetworkHandler(MinecraftServer server, ClientConnection connection) {
      this.server = server;
      this.connection = connection;
   }

   @Override
   public void onHandshake(HandshakeC2SPacket packet) {
      if (packet.intendedState() != ConnectionIntent.LOGIN) {
         throw new UnsupportedOperationException("Invalid intention " + packet.intendedState());
      } else {
         this.connection.transitionInbound(LoginStates.C2S, new ServerLoginNetworkHandler(this.server, this.connection, false));
         this.connection.transitionOutbound(LoginStates.S2C);
      }
   }

   @Override
   public void onDisconnected(DisconnectionInfo info) {
   }

   @Override
   public boolean isConnectionOpen() {
      return this.connection.isOpen();
   }
}
