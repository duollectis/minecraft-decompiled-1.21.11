package net.minecraft.network.state;

import net.minecraft.network.NetworkPhase;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ServerHandshakePacketListener;
import net.minecraft.network.packet.HandshakePackets;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;

/**
 * Класс handshake states.
 */
public class HandshakeStates {

	public static final NetworkStateFactory<ServerHandshakePacketListener, PacketByteBuf>
			C2S_FACTORY =
			NetworkStateBuilder.c2s(
					NetworkPhase.HANDSHAKING,
					builder -> builder.add(HandshakePackets.INTENTION, HandshakeC2SPacket.CODEC)
			);
	public static final NetworkState<ServerHandshakePacketListener> C2S = C2S_FACTORY.bind(PacketByteBuf::new);
}
