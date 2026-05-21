package net.minecraft.server.network;

import net.minecraft.network.packet.Packet;

/**
 * {@code PlayerAssociatedNetworkHandler}.
 */
public interface PlayerAssociatedNetworkHandler {

	ServerPlayerEntity getPlayer();

	void sendPacket(Packet<?> packet);
}
