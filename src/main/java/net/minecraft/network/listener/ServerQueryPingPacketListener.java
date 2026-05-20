package net.minecraft.network.listener;

import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;

public interface ServerQueryPingPacketListener extends PacketListener {
   void onQueryPing(QueryPingC2SPacket packet);
}
