package net.minecraft.network.packet.c2s.common;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ServerCommonPacketListener;
import net.minecraft.network.packet.*;

import java.util.List;

/**
 * Пакет C→S для передачи произвольных данных плагинных каналов.
 * Поддерживает стандартный канал {@code minecraft:brand} и неизвестные каналы (до 32767 байт).
 */
public record CustomPayloadC2SPacket(CustomPayload payload) implements Packet<ServerCommonPacketListener> {

	private static final int MAX_PAYLOAD_SIZE = 32767;
	public static final PacketCodec<PacketByteBuf, CustomPayloadC2SPacket>
			CODEC =
			CustomPayload.<PacketByteBuf>createCodec(
					             id -> UnknownCustomPayload.createCodec(id, MAX_PAYLOAD_SIZE),
					             List.of(new CustomPayload.Type<>(BrandCustomPayload.ID, BrandCustomPayload.CODEC))
			             )
			             .xmap(CustomPayloadC2SPacket::new, CustomPayloadC2SPacket::payload);

	@Override
	public PacketType<CustomPayloadC2SPacket> getPacketType() {
		return CommonPackets.CUSTOM_PAYLOAD_C2S;
	}

	@Override
	public void apply(ServerCommonPacketListener listener) {
		listener.onCustomPayload(this);
	}
}
