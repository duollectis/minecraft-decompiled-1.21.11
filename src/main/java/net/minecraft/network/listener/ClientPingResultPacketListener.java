package net.minecraft.network.listener;

import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;

public interface ClientPingResultPacketListener extends PacketListener {
   void onPingResult(PingResultS2CPacket packet);
}
