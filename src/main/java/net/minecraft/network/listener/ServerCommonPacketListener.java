package net.minecraft.network.listener;

import net.minecraft.network.packet.c2s.common.*;

/**
 * Интерфейс server common packet listener.
 */
public interface ServerCommonPacketListener extends ServerCookieResponsePacketListener {

	void onKeepAlive(KeepAliveC2SPacket packet);

	void onPong(CommonPongC2SPacket packet);

	void onCustomPayload(CustomPayloadC2SPacket packet);

	void onResourcePackStatus(ResourcePackStatusC2SPacket packet);

	void onClientOptions(ClientOptionsC2SPacket packet);

	void onCustomClickAction(CustomClickActionC2SPacket packet);
}
