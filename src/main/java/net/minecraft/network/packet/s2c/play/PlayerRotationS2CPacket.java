package net.minecraft.network.packet.s2c.play;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;

public record PlayerRotationS2CPacket(
		float yaw,
		boolean relativeYaw,
		float pitch,
		boolean relativePitch
) implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<PacketByteBuf, PlayerRotationS2CPacket> CODEC = PacketCodec.tuple(
			PacketCodecs.FLOAT,
			PlayerRotationS2CPacket::yaw,
			PacketCodecs.BOOLEAN,
			PlayerRotationS2CPacket::relativeYaw,
			PacketCodecs.FLOAT,
			PlayerRotationS2CPacket::pitch,
			PacketCodecs.BOOLEAN,
			PlayerRotationS2CPacket::relativePitch,
			PlayerRotationS2CPacket::new
	);

	@Override
	public PacketType<PlayerRotationS2CPacket> getPacketType() {
		return PlayPackets.PLAYER_ROTATION;
	}

	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onPlayerRotation(this);
	}
}
