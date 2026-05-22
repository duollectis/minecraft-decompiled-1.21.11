package net.minecraft.network.listener;

import net.minecraft.network.NetworkSide;

/**
 * Базовый интерфейс для всех клиентских слушателей пакетов.
 * Фиксирует сторону соединения как {@link NetworkSide#CLIENTBOUND}.
 */
public interface ClientPacketListener extends PacketListener {

	@Override
	default NetworkSide getSide() {
		return NetworkSide.CLIENTBOUND;
	}
}
