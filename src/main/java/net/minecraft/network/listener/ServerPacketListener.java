package net.minecraft.network.listener;

import net.minecraft.network.NetworkSide;

/**
 * Базовый интерфейс для всех серверных слушателей пакетов.
 * Фиксирует сторону соединения как {@link NetworkSide#SERVERBOUND}.
 */
public interface ServerPacketListener extends PacketListener {

	@Override
	default NetworkSide getSide() {
		return NetworkSide.SERVERBOUND;
	}
}
