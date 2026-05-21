package net.minecraft.network.packet.c2s.config;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ServerConfigurationPacketListener;
import net.minecraft.network.packet.ConfigPackets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.registry.VersionedIdentifier;

import java.util.List;

/**
 * Запись select known packs c2 s packet.
 */
public record SelectKnownPacksC2SPacket(List<VersionedIdentifier> knownPacks) implements Packet<ServerConfigurationPacketListener> {

	public static final PacketCodec<ByteBuf, SelectKnownPacksC2SPacket> CODEC = PacketCodec.tuple(
			VersionedIdentifier.PACKET_CODEC.collect(PacketCodecs.toList(64)),
			SelectKnownPacksC2SPacket::knownPacks,
			SelectKnownPacksC2SPacket::new
	);

	@Override
	public PacketType<SelectKnownPacksC2SPacket> getPacketType() {
		return ConfigPackets.SELECT_KNOWN_PACKS_C2S;
	}

	/**
	 * Apply.
	 *
	 * @param serverConfigurationPacketListener server configuration packet listener
	 */
	public void apply(ServerConfigurationPacketListener serverConfigurationPacketListener) {
		serverConfigurationPacketListener.onSelectKnownPacks(this);
	}
}
