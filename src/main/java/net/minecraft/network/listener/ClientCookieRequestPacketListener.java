package net.minecraft.network.listener;

import net.minecraft.network.packet.s2c.common.CookieRequestS2CPacket;

/**
 * Слушатель клиентских пакетов запроса cookie от сервера.
 */
public interface ClientCookieRequestPacketListener extends ClientPacketListener {

	void onCookieRequest(CookieRequestS2CPacket packet);
}
