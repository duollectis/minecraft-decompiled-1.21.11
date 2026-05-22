package net.minecraft.network.packet.s2c.common;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.packet.*;

import java.util.List;

/**
 * Запись custom payload s2 c packet.
 */
public record CustomPayloadS2CPacket(CustomPayload payload) implements Packet<ClientCommonPacketListener> {

	private static final int MAX_PAYLOAD_SIZE = 1048576;
	public static final PacketCodec<RegistryByteBuf, CustomPayloadS2CPacket>
			PLAY_CODEC =
			CustomPayload.<RegistryByteBuf>createCodec(
					             id -> UnknownCustomPayload.createCodec(id, MAX_PAYLOAD_SIZE),
					             List.of(new CustomPayload.Type<>(BrandCustomPayload.ID, BrandCustomPayload.CODEC))
			             )
			             .xmap(CustomPayloadS2CPacket::new, CustomPayloadS2CPacket::payload);
	public static final PacketCodec<PacketByteBuf, CustomPayloadS2CPacket>
			CONFIGURATION_CODEC =
			CustomPayload.<PacketByteBuf>createCodec(
					             id -> UnknownCustomPayload.createCodec(id, MAX_PAYLOAD_SIZE),
					             List.of(new CustomPayload.Type<>(BrandCustomPayload.ID, BrandCustomPayload.CODEC))
			             )
			             .xmap(CustomPayloadS2CPacket::new, CustomPayloadS2CPacket::payload);

	@Override
	public PacketType<CustomPayloadS2CPacket> getPacketType() {
		return CommonPackets.CUSTOM_PAYLOAD_S2C;
	}

	/**
	 * Apply.
	 *
	 * @param clientCommonPacketListener client common packet listener
	 */
	public void apply(ClientCommonPacketListener clientCommonPacketListener) {
		clientCommonPacketListener.onCustomPayload(this);
	}
}
