package net.minecraft.network.packet.s2c.play;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import net.minecraft.world.debug.DebugSubscriptionType;

/**
 * Запись event debug s2 c packet.
 */
public record EventDebugS2CPacket(DebugSubscriptionType.Value<?> event) implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<RegistryByteBuf, EventDebugS2CPacket> PACKET_CODEC = PacketCodec.tuple(
			DebugSubscriptionType.Value.PACKET_CODEC, EventDebugS2CPacket::event, EventDebugS2CPacket::new
	);

	@Override
	public PacketType<EventDebugS2CPacket> getPacketType() {
		return PlayPackets.EVENT_DEBUG;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onEventDebug(this);
	}
}
