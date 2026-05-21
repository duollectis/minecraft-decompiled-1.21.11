package net.minecraft.network.packet.c2s.login;

import net.minecraft.network.PacketByteBuf;

/**
 * Интерфейс login query response payload.
 */
public interface LoginQueryResponsePayload {

	void write(PacketByteBuf buf);
}
