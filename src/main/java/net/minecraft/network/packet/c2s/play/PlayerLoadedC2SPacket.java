package net.minecraft.network.packet.c2s.play;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;

/**
 * Запись player loaded c2 s packet.
 */
public record PlayerLoadedC2SPacket() implements Packet<ServerPlayPacketListener> {

	public static final PacketCodec<ByteBuf, PlayerLoadedC2SPacket>
			CODEC =
			PacketCodec.unit(new PlayerLoadedC2SPacket());

	@Override
	public PacketType<PlayerLoadedC2SPacket> getPacketType() {
		return PlayPackets.PLAYER_LOADED;
	}

	/**
	 * Apply.
	 *
	 * @param serverPlayPacketListener server play packet listener
	 */
	public void apply(ServerPlayPacketListener serverPlayPacketListener) {
		serverPlayPacketListener.onPlayerLoaded(this);
	}
}
