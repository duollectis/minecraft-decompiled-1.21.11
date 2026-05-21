package net.minecraft.network.listener;

import net.minecraft.network.packet.s2c.common.CookieRequestS2CPacket;

/**
 * Интерфейс client cookie request packet listener.
 */
public interface ClientCookieRequestPacketListener extends ClientPacketListener {

	void onCookieRequest(CookieRequestS2CPacket packet);
}
