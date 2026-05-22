package net.minecraft.network.listener;

import net.minecraft.network.NetworkPhase;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;

/**
 * Слушатель серверных пакетов фазы {@link net.minecraft.network.NetworkPhase#HANDSHAKING}.
 * Обрабатывает первоначальное рукопожатие клиента, определяющее следующую фазу протокола.
 */
public interface ServerHandshakePacketListener extends ServerCrashSafePacketListener {

	@Override
	default NetworkPhase getPhase() {
		return NetworkPhase.HANDSHAKING;
	}

	void onHandshake(HandshakeC2SPacket packet);
}
