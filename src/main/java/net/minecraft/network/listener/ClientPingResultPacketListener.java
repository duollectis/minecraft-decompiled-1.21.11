package net.minecraft.network.listener;

import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;

/**
 * Слушатель клиентских пакетов результата пинга от сервера.
 */
public interface ClientPingResultPacketListener extends PacketListener {

	void onPingResult(PingResultS2CPacket packet);
}
