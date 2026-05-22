package net.minecraft.network.listener;

import net.minecraft.network.NetworkPhase;
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket;

/**
 * Слушатель серверных пакетов фазы {@link net.minecraft.network.NetworkPhase#STATUS}.
 * Обрабатывает запрос статуса сервера (MOTD, версия, количество игроков).
 */
public interface ServerQueryPacketListener extends ServerCrashSafePacketListener, ServerQueryPingPacketListener {

	@Override
	default NetworkPhase getPhase() {
		return NetworkPhase.STATUS;
	}

	void onRequest(QueryRequestC2SPacket packet);
}
