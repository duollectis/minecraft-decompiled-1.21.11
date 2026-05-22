package net.minecraft.world.debug.data;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * Отладочные данные слушателя игровых событий: радиус прослушивания.
 */
public record GameEventListenerDebugData(int listenerRadius) {

	public static final PacketCodec<RegistryByteBuf, GameEventListenerDebugData> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT,
			GameEventListenerDebugData::listenerRadius,
			GameEventListenerDebugData::new
	);
}
