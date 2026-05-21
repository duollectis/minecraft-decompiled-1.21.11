package net.minecraft.network.packet.s2c.play;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;

/**
 * Класс start chunk send s2 c packet.
 */
public class StartChunkSendS2CPacket implements Packet<ClientPlayPacketListener> {

	public static final StartChunkSendS2CPacket INSTANCE = new StartChunkSendS2CPacket();
	public static final PacketCodec<ByteBuf, StartChunkSendS2CPacket> CODEC = PacketCodec.unit(INSTANCE);

	private StartChunkSendS2CPacket() {
	}

	@Override
	public PacketType<StartChunkSendS2CPacket> getPacketType() {
		return PlayPackets.CHUNK_BATCH_START;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onStartChunkSend(this);
	}
}
