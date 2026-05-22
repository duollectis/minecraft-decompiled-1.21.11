package net.minecraft.network.handler;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * Утилитарный класс для работы с legacy-запросами пинга (протоколы до 1.7).
 * Строки кодируются в UTF-16BE с предшествующим short-размером.
 */
public class LegacyQueries {

	public static final int HEADER = 250;
	public static final String PING_HOST = "MC|PingHost";
	public static final int QUERY_PACKET_ID = 254;
	public static final int LEGACY_PING_BYTE = 1;
	public static final int BUFFER_SIZE = 255;
	public static final int PROTOCOL_VERSION = 127;

	public static void write(ByteBuf buf, String string) {
		buf.writeShort(string.length());
		buf.writeCharSequence(string, StandardCharsets.UTF_16BE);
	}

	public static String read(ByteBuf buf) {
		int charCount = buf.readShort();
		int byteCount = charCount * 2;
		String result = buf.toString(buf.readerIndex(), byteCount, StandardCharsets.UTF_16BE);
		buf.skipBytes(byteCount);
		return result;
	}
}
