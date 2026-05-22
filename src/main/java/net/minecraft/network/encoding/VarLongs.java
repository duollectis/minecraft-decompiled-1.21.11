package net.minecraft.network.encoding;

import io.netty.buffer.ByteBuf;

/**
 * Утилитарный класс для кодирования длинных целых чисел в формате VarLong.
 *
 * <p>VarLong использует от 1 до {@value #MAX_BYTES} байт: каждый байт содержит 7 бит данных
 * и 1 бит-флаг продолжения (старший бит). Аналог {@link VarInts}, но для типа {@code long}.</p>
 */
public class VarLongs {

	private static final int MAX_BYTES = 10;
	private static final int DATA_BITS_MASK = 127;
	private static final int MORE_BITS_MASK = 128;
	private static final int DATA_BITS_PER_BYTE = 7;

	/**
	 * Вычисляет количество байт, необходимых для кодирования значения в формате VarLong.
	 */
	public static int getSizeInBytes(long value) {
		for (int byteCount = 1; byteCount < MAX_BYTES; byteCount++) {
			if ((value & -1L << byteCount * DATA_BITS_PER_BYTE) == 0L) {
				return byteCount;
			}
		}

		return MAX_BYTES;
	}

	public static boolean shouldContinueRead(byte b) {
		return (b & MORE_BITS_MASK) == MORE_BITS_MASK;
	}

	/**
	 * Читает VarLong из буфера.
	 *
	 * @throws RuntimeException если VarLong занимает более {@value #MAX_BYTES} байт
	 */
	public static long read(ByteBuf buf) {
		long result = 0L;
		int byteIndex = 0;

		byte current;
		do {
			current = buf.readByte();
			result |= (long) (current & DATA_BITS_MASK) << byteIndex++ * DATA_BITS_PER_BYTE;
			if (byteIndex > MAX_BYTES) {
				throw new RuntimeException("VarLong too big");
			}
		} while (shouldContinueRead(current));

		return result;
	}

	/**
	 * Записывает значение в буфер в формате VarLong.
	 */
	public static ByteBuf write(ByteBuf buf, long value) {
		while ((value & -MORE_BITS_MASK) != 0L) {
			buf.writeByte((int) (value & DATA_BITS_MASK) | MORE_BITS_MASK);
			value >>>= DATA_BITS_PER_BYTE;
		}

		buf.writeByte((int) value);
		return buf;
	}
}
