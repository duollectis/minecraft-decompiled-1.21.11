package net.minecraft.network.packet.c2s.common;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ServerCookieResponsePacketListener;
import net.minecraft.network.packet.CookiePackets;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.s2c.common.StoreCookieS2CPacket;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Пакет C→S, возвращающий серверу ранее сохранённое cookie по его ключу.
 * Является ответом на запрос {@code CookieRequestS2CPacket}; payload может быть null, если cookie не найдено.
 */
public record CookieResponseC2SPacket(
		Identifier key,
		byte @Nullable [] payload
) implements Packet<ServerCookieResponsePacketListener> {

	public static final PacketCodec<PacketByteBuf, CookieResponseC2SPacket> CODEC = Packet.createCodec(
			CookieResponseC2SPacket::write, CookieResponseC2SPacket::new
	);

	private CookieResponseC2SPacket(PacketByteBuf buf) {
		this(buf.readIdentifier(), buf.readNullable(StoreCookieS2CPacket.COOKIE_PACKET_CODEC));
	}

	private void write(PacketByteBuf buf) {
		buf.writeIdentifier(key);
		buf.writeNullable(payload, StoreCookieS2CPacket.COOKIE_PACKET_CODEC);
	}

	@Override
	public PacketType<CookieResponseC2SPacket> getPacketType() {
		return CookiePackets.COOKIE_RESPONSE;
	}

	@Override
	public void apply(ServerCookieResponsePacketListener listener) {
		listener.onCookieResponse(this);
	}
}
