package net.minecraft.network.packet.s2c.play;

import net.minecraft.entity.EntityPosition;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;

import java.util.Set;

/**
 * Запись player position look s2 c packet.
 */
public record PlayerPositionLookS2CPacket(
		int teleportId,
		EntityPosition change,
		Set<PositionFlag> relatives
) implements Packet<ClientPlayPacketListener> {

	public static final PacketCodec<PacketByteBuf, PlayerPositionLookS2CPacket> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT,
			PlayerPositionLookS2CPacket::teleportId,
			EntityPosition.PACKET_CODEC,
			PlayerPositionLookS2CPacket::change,
			PositionFlag.PACKET_CODEC,
			PlayerPositionLookS2CPacket::relatives,
			PlayerPositionLookS2CPacket::new
	);

	/**
	 * Of.
	 *
	 * @param teleportId teleport id
	 * @param pos pos
	 * @param flags flags
	 *
	 * @return PlayerPositionLookS2CPacket — результат операции
	 */
	public static PlayerPositionLookS2CPacket of(int teleportId, EntityPosition pos, Set<PositionFlag> flags) {
		return new PlayerPositionLookS2CPacket(teleportId, pos, flags);
	}

	@Override
	public PacketType<PlayerPositionLookS2CPacket> getPacketType() {
		return PlayPackets.PLAYER_POSITION;
	}

	/**
	 * Apply.
	 *
	 * @param clientPlayPacketListener client play packet listener
	 */
	public void apply(ClientPlayPacketListener clientPlayPacketListener) {
		clientPlayPacketListener.onPlayerPositionLook(this);
	}
}
