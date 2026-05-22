package net.minecraft.network.listener;

import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;

/**
 * Слушатель серверных пакетов пинга от клиента в фазе {@link net.minecraft.network.NetworkPhase#STATUS}.
 */
public interface ServerQueryPingPacketListener extends PacketListener {

	void onQueryPing(QueryPingC2SPacket packet);
}
