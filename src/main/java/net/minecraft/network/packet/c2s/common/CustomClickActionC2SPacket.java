package net.minecraft.network.packet.c2s.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ServerCommonPacketListener;
import net.minecraft.network.packet.CommonPackets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.util.Identifier;

import java.util.Optional;

public record CustomClickActionC2SPacket(
		Identifier id,
		Optional<NbtElement> payload
) implements Packet<ServerCommonPacketListener> {

	private static final PacketCodec<ByteBuf, Optional<NbtElement>>
			PAYLOAD_CODEC =
			PacketCodecs.nbtElement(() -> new NbtSizeTracker(32768L, 16))
			            .collect(PacketCodecs.lengthPrepended(65536));
	public static final PacketCodec<ByteBuf, CustomClickActionC2SPacket> CODEC = PacketCodec.tuple(
			Identifier.PACKET_CODEC,
			CustomClickActionC2SPacket::id,
			PAYLOAD_CODEC,
			CustomClickActionC2SPacket::payload,
			CustomClickActionC2SPacket::new
	);

	@Override
	public PacketType<CustomClickActionC2SPacket> getPacketType() {
		return CommonPackets.CUSTOM_CLICK_ACTION;
	}

	public void apply(ServerCommonPacketListener serverCommonPacketListener) {
		serverCommonPacketListener.onCustomClickAction(this);
	}
}
