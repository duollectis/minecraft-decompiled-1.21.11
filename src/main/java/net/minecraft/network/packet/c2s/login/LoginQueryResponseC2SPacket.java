package net.minecraft.network.packet.c2s.login;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ServerLoginPacketListener;
import net.minecraft.network.packet.LoginPackets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import org.jspecify.annotations.Nullable;

/**
 * Запись login query response c2 s packet.
 */
public record LoginQueryResponseC2SPacket(
		int queryId,
		@Nullable LoginQueryResponsePayload response
) implements Packet<ServerLoginPacketListener> {

	public static final PacketCodec<PacketByteBuf, LoginQueryResponseC2SPacket> CODEC = Packet.createCodec(
			LoginQueryResponseC2SPacket::write, LoginQueryResponseC2SPacket::read
	);
	private static final int MAX_PAYLOAD_SIZE = 1048576;

	private static LoginQueryResponseC2SPacket read(PacketByteBuf buf) {
		int i = buf.readVarInt();
		return new LoginQueryResponseC2SPacket(i, readPayload(i, buf));
	}

	private static LoginQueryResponsePayload readPayload(int queryId, PacketByteBuf buf) {
		return getVanillaPayload(buf);
	}

	private static LoginQueryResponsePayload getVanillaPayload(PacketByteBuf buf) {
		int i = buf.readableBytes();
		if (i >= 0 && i <= MAX_PAYLOAD_SIZE) {
			buf.skipBytes(i);
			return UnknownLoginQueryResponsePayload.INSTANCE;
		}
		else {
			throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
		}
	}

	private void write(PacketByteBuf buf) {
		buf.writeVarInt(this.queryId);
		buf.writeNullable(this.response, (bufx, response) -> response.write(bufx));
	}

	@Override
	public PacketType<LoginQueryResponseC2SPacket> getPacketType() {
		return LoginPackets.CUSTOM_QUERY_ANSWER;
	}

	/**
	 * Apply.
	 *
	 * @param serverLoginPacketListener server login packet listener
	 */
	public void apply(ServerLoginPacketListener serverLoginPacketListener) {
		serverLoginPacketListener.onQueryResponse(this);
	}
}
