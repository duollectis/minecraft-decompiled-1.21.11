package net.minecraft.network.encoding;

import io.netty.buffer.ByteBuf;

/**
 * Утилитарный класс для кодирования целых чисел в формате VarInt.
 *
 * <p>VarInt использует от 1 до {@value #MAX_BYTES} байт: каждый байт содержит 7 бит данных
 * и 1 бит-флаг продолжения (старший бит). Это позволяет эффективно передавать малые числа.</p>
 */
public class VarInts {

	public static final int MAX_BYTES = 5;
	private static final int DATA_BITS_MASK = 127;
	private static final int MORE_BITS_MASK = 128;
	private static final int DATA_BITS_PER_BYTE = 7;

	/**
	 * Вычисляет количество байт, необходимых для кодирования значения в формате VarInt.
	 */
	public static int getSizeInBytes(int value) {
		for (int byteCount = 1; byteCount < MAX_BYTES; byteCount++) {
			if ((value & -1 << byteCount * DATA_BITS_PER_BYTE) == 0) {
				return byteCount;
			}
		}

		return MAX_BYTES;
	}

	public static boolean shouldContinueRead(byte b) {
		return (b & MORE_BITS_MASK) == MORE_BITS_MASK;
	}

	/**
	 * Читает VarInt из буфера.
	 *
	 * @throws RuntimeException если VarInt занимает более {@value #MAX_BYTES} байт
	 */
	public static int read(ByteBuf buf) {
		int result = 0;
		int byteIndex = 0;

		byte current;
		do {
			current = buf.readByte();
			result |= (current & DATA_BITS_MASK) << byteIndex++ * DATA_BITS_PER_BYTE;
			if (byteIndex > MAX_BYTES) {
				throw new RuntimeException("VarInt too big");
			}
		} while (shouldContinueRead(current));

		return result;
	}

	/**
	 * Записывает значение в буфер в формате VarInt.
	 */
	public static ByteBuf write(ByteBuf buf, int value) {
		while ((value & -MORE_BITS_MASK) != 0) {
			buf.writeByte(value & DATA_BITS_MASK | MORE_BITS_MASK);
			value >>>= DATA_BITS_PER_BYTE;
		}

		buf.writeByte(value);
		return buf;
	}
}
