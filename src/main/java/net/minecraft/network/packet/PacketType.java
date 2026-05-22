package net.minecraft.network.packet;

import net.minecraft.network.NetworkSide;
import net.minecraft.util.Identifier;

/**
 * Идентификатор типа пакета: сторона сети и строковый идентификатор.
 * Используется для диспетчеризации пакетов в {@link net.minecraft.network.handler.PacketCodecDispatcher}.
 *
 * @param <T> тип пакета
 */
public record PacketType<T extends Packet<?>>(NetworkSide side, Identifier id) {

	@Override
	public String toString() {
		return side.getName() + "/" + id;
	}
}
