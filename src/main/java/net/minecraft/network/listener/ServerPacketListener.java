package net.minecraft.network.listener;

import net.minecraft.network.NetworkSide;

/**
 * Интерфейс server packet listener.
 */
public interface ServerPacketListener extends PacketListener {

	@Override
	default NetworkSide getSide() {
		return NetworkSide.SERVERBOUND;
	}
}
