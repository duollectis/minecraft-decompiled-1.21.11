package net.minecraft.server.network;

import net.minecraft.network.packet.Packet;

/**
 * Класс Player Associated Network Handler.
 */
public interface PlayerAssociatedNetworkHandler {

	ServerPlayerEntity getPlayer();

	void sendPacket(Packet<?> packet);
}
