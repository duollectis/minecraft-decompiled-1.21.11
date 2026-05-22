package net.minecraft.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

/**
 * Метаданные PNG-изображения, извлекаемые из заголовка файла без полной декодировки.
 * Читает только сигнатуру PNG и чанк IHDR для получения размеров изображения.
 */
public record PngMetadata(int width, int height) {

	private static final HexFormat HEX_FORMAT = HexFormat.of().withUpperCase().withPrefix("0x");

	/** Магическая сигнатура PNG-файла (первые 8 байт). */
	private static final long PNG_SIGNATURE = -8552249625308161526L;

	/** Тип чанка IHDR в виде 4-байтового целого числа (ASCII "IHDR"). */
	private static final int IHDR_CHUNK_TYPE = 1229472850;

	/** Фиксированная длина данных чанка IHDR в байтах. */
	private static final int IHDR_CHUNK_LENGTH = 13;

	/** Минимальный размер буфера для валидации заголовка PNG (сигнатура + длина + тип чанка). */
	private static final int MIN_HEADER_SIZE = 16;

	/**
	 * Читает метаданные PNG из потока, проверяя сигнатуру и структуру чанка IHDR.
	 *
	 * @param stream входной поток PNG-данных
	 * @return метаданные с шириной и высотой изображения
	 * @throws IOException если сигнатура или структура чанка некорректны
	 */
	public static PngMetadata fromStream(InputStream stream) throws IOException {
		DataInputStream dataInputStream = new DataInputStream(stream);

		long signature = dataInputStream.readLong();
		if (signature != PNG_SIGNATURE) {
			throw new IOException("Bad PNG Signature: " + HEX_FORMAT.toHexDigits(signature));
		}

		int chunkLength = dataInputStream.readInt();
		if (chunkLength != IHDR_CHUNK_LENGTH) {
			throw new IOException("Bad length for IHDR chunk: " + chunkLength);
		}

		int chunkType = dataInputStream.readInt();
		if (chunkType != IHDR_CHUNK_TYPE) {
			throw new IOException("Bad type for IHDR chunk: " + HEX_FORMAT.toHexDigits(chunkType));
		}

		int width = dataInputStream.readInt();
		int height = dataInputStream.readInt();
		return new PngMetadata(width, height);
	}

	/**
	 * Читает метаданные PNG из массива байт.
	 *
	 * @param bytes байты PNG-файла
	 * @return метаданные с шириной и высотой изображения
	 * @throws IOException если данные не являются корректным PNG
	 */
	public static PngMetadata fromBytes(byte[] bytes) throws IOException {
		return fromStream(new ByteArrayInputStream(bytes));
	}

	/**
	 * Проверяет корректность заголовка PNG в буфере без извлечения метаданных.
	 * Восстанавливает исходный порядок байт буфера после проверки.
	 *
	 * @param buf буфер с PNG-данными
	 * @throws IOException если заголовок отсутствует или некорректен
	 */
	public static void validate(ByteBuffer buf) throws IOException {
		ByteOrder originalOrder = buf.order();
		buf.order(ByteOrder.BIG_ENDIAN);

		try {
			if (buf.limit() < MIN_HEADER_SIZE) {
				throw new IOException("PNG header missing");
			}

			if (buf.getLong(0) != PNG_SIGNATURE) {
				throw new IOException("Bad PNG Signature");
			}

			if (buf.getInt(8) != IHDR_CHUNK_LENGTH) {
				throw new IOException("Bad length for IHDR chunk!");
			}

			if (buf.getInt(12) != IHDR_CHUNK_TYPE) {
				throw new IOException("Bad type for IHDR chunk!");
			}
		} finally {
			buf.order(originalOrder);
		}
	}
}
