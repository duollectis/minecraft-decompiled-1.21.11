package net.minecraft.network.listener;

import net.minecraft.network.packet.c2s.common.*;

/**
 * Слушатель серверных пакетов, общих для фаз {@code CONFIGURATION} и {@code PLAY}:
 * keep-alive, pong, кастомные payload, статус ресурс-паков, настройки клиента и кастомные действия.
 */
public interface ServerCommonPacketListener extends ServerCookieResponsePacketListener {

	void onKeepAlive(KeepAliveC2SPacket packet);

	void onPong(CommonPongC2SPacket packet);

	void onCustomPayload(CustomPayloadC2SPacket packet);

	void onResourcePackStatus(ResourcePackStatusC2SPacket packet);

	void onClientOptions(ClientOptionsC2SPacket packet);

	void onCustomClickAction(CustomClickActionC2SPacket packet);
}
