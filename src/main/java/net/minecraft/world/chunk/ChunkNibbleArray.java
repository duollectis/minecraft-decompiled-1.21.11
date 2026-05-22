package net.minecraft.world.chunk;

import net.minecraft.util.Util;
import net.minecraft.util.annotation.Debug;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Компактный массив 4-битных значений освещения для одной секции чанка (16×16×16 блоков).
 * Хранит 4096 значений в 2048 байтах: два значения упакованы в один байт (нибблы).
 * Поддерживает ленивую инициализацию: массив байт создаётся только при первой записи.
 */
public class ChunkNibbleArray {

	public static final int COPY_TIMES = 16;
	public static final int COPY_BLOCK_SIZE = 128;
	public static final int BYTES_LENGTH = 2048;

	private static final int NIBBLE_BITS = 4;
	private static final int NIBBLE_MASK = 15;
	private static final int TOTAL_ENTRIES = 4096;
	private static final int BOTTOM_LAYER_ENTRIES = 256;
	private static final int ROW_SIZE = 16;

	protected byte @Nullable [] bytes;
	private int defaultValue;

	public ChunkNibbleArray() {
		this(0);
	}

	public ChunkNibbleArray(int defaultValue) {
		this.defaultValue = defaultValue;
	}

	public ChunkNibbleArray(byte[] bytes) {
		this.bytes = bytes;
		this.defaultValue = 0;
		if (bytes.length != BYTES_LENGTH) {
			throw (IllegalArgumentException) Util.getFatalOrPause(
					new IllegalArgumentException("DataLayer should be 2048 bytes not: " + bytes.length));
		}
	}

	public int get(int x, int y, int z) {
		return get(getIndex(x, y, z));
	}

	public void set(int x, int y, int z, int value) {
		set(getIndex(x, y, z), value);
	}

	private static int getIndex(int x, int y, int z) {
		return y << 8 | z << NIBBLE_BITS | x;
	}

	private int get(int index) {
		if (bytes == null) {
			return defaultValue;
		}

		int arrayIndex = getArrayIndex(index);
		int nibbleShift = occupiesSmallerBits(index);
		return bytes[arrayIndex] >> NIBBLE_BITS * nibbleShift & NIBBLE_MASK;
	}

	private void set(int index, int value) {
		byte[] data = asByteArray();
		int arrayIndex = getArrayIndex(index);
		int nibbleShift = occupiesSmallerBits(index);
		int clearMask = ~(NIBBLE_MASK << NIBBLE_BITS * nibbleShift);
		int setBits = (value & NIBBLE_MASK) << NIBBLE_BITS * nibbleShift;
		data[arrayIndex] = (byte) (data[arrayIndex] & clearMask | setBits);
	}

	private static int occupiesSmallerBits(int index) {
		return index & 1;
	}

	private static int getArrayIndex(int index) {
		return index >> 1;
	}

	public void clear(int defaultValue) {
		this.defaultValue = defaultValue;
		bytes = null;
	}

	private static byte pack(int value) {
		byte packed = (byte) value;

		for (int shift = NIBBLE_BITS; shift < 8; shift += NIBBLE_BITS) {
			packed = (byte) (packed | value << shift);
		}

		return packed;
	}

	public byte[] asByteArray() {
		if (bytes == null) {
			bytes = new byte[BYTES_LENGTH];
			if (defaultValue != 0) {
				Arrays.fill(bytes, pack(defaultValue));
			}
		}

		return bytes;
	}

	public ChunkNibbleArray copy() {
		return bytes == null
				? new ChunkNibbleArray(defaultValue)
				: new ChunkNibbleArray(bytes.clone());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < TOTAL_ENTRIES; i++) {
			builder.append(Integer.toHexString(get(i)));
			if ((i & 15) == 15) {
				builder.append("\n");
			}

			if ((i & 0xFF) == 255) {
				builder.append("\n");
			}
		}

		return builder.toString();
	}

	/**
	 * Возвращает строковое представление нижнего слоя (Y=0) секции.
	 * Используется исключительно в отладочных целях.
	 */
	@Debug
	public String bottomToString(int unused) {
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < BOTTOM_LAYER_ENTRIES; i++) {
			builder.append(Integer.toHexString(get(i)));
			if ((i & 15) == 15) {
				builder.append("\n");
			}
		}

		return builder.toString();
	}

	public boolean isArrayUninitialized() {
		return bytes == null;
	}

	public boolean isUninitialized(int expectedDefaultValue) {
		return bytes == null && defaultValue == expectedDefaultValue;
	}

	public boolean isUninitialized() {
		return bytes == null && defaultValue == 0;
	}
}
