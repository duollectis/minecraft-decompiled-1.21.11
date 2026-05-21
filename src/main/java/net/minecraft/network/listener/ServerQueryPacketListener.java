package net.minecraft.network.listener;

import net.minecraft.network.NetworkPhase;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;

/**
 * Интерфейс server query packet listener.
 */
public interface ServerQueryPacketListener extends ServerCrashSafePacketListener, ServerQueryPingPacketListener {

	@Override
	default NetworkPhase getPhase() {
		return NetworkPhase.STATUS;
	}

	void onRequest(QueryRequestC2SPacket packet);
}
