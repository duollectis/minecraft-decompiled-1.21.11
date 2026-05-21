package net.minecraft.network.listener;

import net.minecraft.network.NetworkPhase;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;

/**
 * Интерфейс client query packet listener.
 */
public interface ClientQueryPacketListener extends ClientPingResultPacketListener, ClientPacketListener {

	@Override
	default NetworkPhase getPhase() {
		return NetworkPhase.STATUS;
	}

	void onResponse(QueryResponseS2CPacket packet);
}
