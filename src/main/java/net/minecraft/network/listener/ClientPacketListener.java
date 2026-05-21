package net.minecraft.network.listener;

import net.minecraft.network.NetworkSide;

/**
 * Интерфейс client packet listener.
 */
public interface ClientPacketListener extends PacketListener {

	@Override
	default NetworkSide getSide() {
		return NetworkSide.CLIENTBOUND;
	}
}
