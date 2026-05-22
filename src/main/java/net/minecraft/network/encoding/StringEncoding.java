package net.minecraft.network.encoding;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

import java.nio.charset.StandardCharsets;

/**
 * Утилитарный класс для кодирования и декодирования строк в сетевом протоколе Minecraft.
 *
 * <p>Строки передаются как VarInt-длина в байтах, за которой следуют UTF-8 байты.
 * При декодировании проверяется как байтовая длина, так и символьная длина строки.</p>
 */
public class StringEncoding {

	/**
	 * Декодирует строку UTF-8 из буфера, проверяя, что её длина не превышает {@code maxLength} символов.
	 *
	 * @param buf       буфер для чтения
	 * @param maxLength максимально допустимая длина строки в символах
	 * @return декодированная строка
	 * @throws DecoderException если длина буфера или строки превышает допустимый предел
	 */
	public static String decode(ByteBuf buf, int maxLength) {
		int maxByteLength = ByteBufUtil.utf8MaxBytes(maxLength);
		int byteLength = VarInts.read(buf);

		if (byteLength > maxByteLength) {
			throw new DecoderException(
					"The received encoded string buffer length is longer than maximum allowed ("
							+ byteLength + " > " + maxByteLength + ")");
		}

		if (byteLength < 0) {
			throw new DecoderException("The received encoded string buffer length is less than zero! Weird string!");
		}

		int available = buf.readableBytes();
		if (byteLength > available) {
			throw new DecoderException("Not enough bytes in buffer, expected " + byteLength + ", but got " + available);
		}

		String result = buf.toString(buf.readerIndex(), byteLength, StandardCharsets.UTF_8);
		buf.readerIndex(buf.readerIndex() + byteLength);

		if (result.length() > maxLength) {
			throw new DecoderException(
					"The received string length is longer than maximum allowed ("
							+ result.length() + " > " + maxLength + ")");
		}

		return result;
	}

	/**
	 * Кодирует строку UTF-8 в буфер, предваряя её VarInt-длиной в байтах.
	 *
	 * @param buf       буфер для записи
	 * @param string    строка для кодирования
	 * @param maxLength максимально допустимая длина строки в символах
	 * @throws EncoderException если строка превышает допустимый предел
	 */
	public static void encode(ByteBuf buf, CharSequence string, int maxLength) {
		if (string.length() > maxLength) {
			throw new EncoderException(
					"String too big (was " + string.length() + " characters, max " + maxLength + ")");
		}

		int maxBytes = ByteBufUtil.utf8MaxBytes(string);
		ByteBuf temp = buf.alloc().buffer(maxBytes);

		try {
			int writtenBytes = ByteBufUtil.writeUtf8(temp, string);
			int maxAllowedBytes = ByteBufUtil.utf8MaxBytes(maxLength);
			if (writtenBytes > maxAllowedBytes) {
				throw new EncoderException(
						"String too big (was " + writtenBytes + " bytes encoded, max " + maxAllowedBytes + ")");
			}

			VarInts.write(buf, writtenBytes);
			buf.writeBytes(temp);
		} finally {
			temp.release();
		}
	}
}
