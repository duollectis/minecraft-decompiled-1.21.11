package net.minecraft.network.packet.s2c.login;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Интерфейс login query request payload.
 */
public interface LoginQueryRequestPayload {

	Identifier id();

	void write(PacketByteBuf buf);
}
