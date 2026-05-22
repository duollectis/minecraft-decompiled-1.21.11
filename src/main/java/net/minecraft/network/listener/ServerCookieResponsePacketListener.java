package net.minecraft.network.listener;

import net.minecraft.network.packet.c2s.common.CookieResponseC2SPacket;

/**
 * Слушатель серверных пакетов ответа клиента на запрос cookie.
 */
public interface ServerCookieResponsePacketListener extends ServerCrashSafePacketListener {

	void onCookieResponse(CookieResponseC2SPacket packet);
}
