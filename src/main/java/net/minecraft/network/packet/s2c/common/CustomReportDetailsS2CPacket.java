package net.minecraft.network.packet.s2c.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.packet.CommonPackets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;

import java.util.HashMap;
import java.util.Map;

/**
 * Запись custom report details s2 c packet.
 */
public record CustomReportDetailsS2CPacket(Map<String, String> details) implements Packet<ClientCommonPacketListener> {

	private static final int MAX_KEY_LENGTH = 128;
	private static final int MAX_VALUE_LENGTH = 4096;
	private static final int MAX_DETAILS_SIZE = 32;
	private static final PacketCodec<ByteBuf, Map<String, String>> DETAILS_CODEC = PacketCodecs.map(
			HashMap::new, PacketCodecs.string(MAX_KEY_LENGTH), PacketCodecs.string(MAX_VALUE_LENGTH), MAX_DETAILS_SIZE
	);
	public static final PacketCodec<ByteBuf, CustomReportDetailsS2CPacket> CODEC = PacketCodec.tuple(
			DETAILS_CODEC, CustomReportDetailsS2CPacket::details, CustomReportDetailsS2CPacket::new
	);

	@Override
	public PacketType<CustomReportDetailsS2CPacket> getPacketType() {
		return CommonPackets.CUSTOM_REPORT_DETAILS;
	}

	/**
	 * Apply.
	 *
	 * @param clientCommonPacketListener client common packet listener
	 */
	public void apply(ClientCommonPacketListener clientCommonPacketListener) {
		clientCommonPacketListener.onCustomReportDetails(this);
	}
}
