package net.minecraft.network.packet.s2c.common;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.packet.CommonPackets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.util.Uuids;

import java.util.Optional;
import java.util.UUID;

/**
 * Запись resource pack remove s2 c packet.
 */
public record ResourcePackRemoveS2CPacket(Optional<UUID> id) implements Packet<ClientCommonPacketListener> {

	public static final PacketCodec<PacketByteBuf, ResourcePackRemoveS2CPacket> CODEC = Packet.createCodec(
			ResourcePackRemoveS2CPacket::write, ResourcePackRemoveS2CPacket::new
	);

	private ResourcePackRemoveS2CPacket(PacketByteBuf buf) {
		this(buf.readOptional(Uuids.PACKET_CODEC));
	}

	private void write(PacketByteBuf buf) {
		buf.writeOptional(this.id, Uuids.PACKET_CODEC);
	}

	@Override
	public PacketType<ResourcePackRemoveS2CPacket> getPacketType() {
		return CommonPackets.RESOURCE_PACK_POP;
	}

	/**
	 * Apply.
	 *
	 * @param clientCommonPacketListener client common packet listener
	 */
	public void apply(ClientCommonPacketListener clientCommonPacketListener) {
		clientCommonPacketListener.onResourcePackRemove(this);
	}
}
