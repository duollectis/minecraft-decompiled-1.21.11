package net.minecraft.network.packet.c2s.play;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.message.LastSeenMessageList;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.PlayPackets;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

public record ChatMessageC2SPacket(
		String chatMessage,
		Instant timestamp,
		long salt,
		@Nullable MessageSignatureData signature,
		LastSeenMessageList.Acknowledgment acknowledgment
) implements Packet<ServerPlayPacketListener> {

	public static final PacketCodec<PacketByteBuf, ChatMessageC2SPacket>
			CODEC =
			Packet.createCodec(ChatMessageC2SPacket::write, ChatMessageC2SPacket::new);

	private ChatMessageC2SPacket(PacketByteBuf buf) {
		this(
				buf.readString(256),
				buf.readInstant(),
				buf.readLong(),
				buf.readNullable(MessageSignatureData::fromBuf),
				new LastSeenMessageList.Acknowledgment(buf)
		);
	}

	private void write(PacketByteBuf buf) {
		buf.writeString(this.chatMessage, 256);
		buf.writeInstant(this.timestamp);
		buf.writeLong(this.salt);
		buf.writeNullable(this.signature, MessageSignatureData::write);
		this.acknowledgment.write(buf);
	}

	@Override
	public PacketType<ChatMessageC2SPacket> getPacketType() {
		return PlayPackets.CHAT;
	}

	public void apply(ServerPlayPacketListener serverPlayPacketListener) {
		serverPlayPacketListener.onChatMessage(this);
	}
}
