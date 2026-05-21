package net.minecraft.network.packet.s2c.login;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Запись unknown login query request payload.
 */
public record UnknownLoginQueryRequestPayload(Identifier id) implements LoginQueryRequestPayload {

	@Override
	public void write(PacketByteBuf buf) {
	}
}
