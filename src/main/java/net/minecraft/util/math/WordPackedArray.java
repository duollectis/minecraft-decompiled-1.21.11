package net.minecraft.util.math;

import org.apache.commons.lang3.Validate;

/**
 * Массив целых чисел с упаковкой нескольких значений в одно 64-битное слово.
 * Каждый элемент занимает ровно {@code unitSize} бит; элементы могут пересекать границы слов.
 * Используется для компактного хранения данных с ограниченным диапазоном значений.
 */
public class WordPackedArray {

	private static final int BIT_TO_LONG_INDEX_SHIFT = 6;

	private final long[] array;
	private final int unitSize;
	private final long maxValue;
	private final int length;

	public WordPackedArray(int unitSize, int length) {
		this(unitSize, length, new long[MathHelper.roundUpToMultiple(length * unitSize, 64) / 64]);
	}

	public WordPackedArray(int unitSize, int length, long[] array) {
		Validate.inclusiveBetween(1L, 32L, unitSize);
		this.length = length;
		this.unitSize = unitSize;
		this.array = array;
		this.maxValue = (1L << unitSize) - 1L;
		int expectedWords = MathHelper.roundUpToMultiple(length * unitSize, 64) / 64;

		if (array.length != expectedWords) {
			throw new IllegalArgumentException(
				"Invalid length given for storage, got: " + array.length + " but expected: " + expectedWords
			);
		}
	}

	/**
	 * Записывает значение по индексу. Поддерживает запись через границу 64-битного слова.
	 *
	 * @param index индекс элемента (0..length-1)
	 * @param value значение (0..maxValue)
	 */
	public void set(int index, int value) {
		Validate.inclusiveBetween(0L, length - 1, index);
		Validate.inclusiveBetween(0L, maxValue, value);
		int bitOffset = index * unitSize;
		int wordIndex = bitOffset >> BIT_TO_LONG_INDEX_SHIFT;
		int lastWordIndex = (index + 1) * unitSize - 1 >> BIT_TO_LONG_INDEX_SHIFT;
		int bitInWord = bitOffset ^ wordIndex << BIT_TO_LONG_INDEX_SHIFT;
		array[wordIndex] = array[wordIndex] & ~(maxValue << bitInWord) | (value & maxValue) << bitInWord;

		if (wordIndex != lastWordIndex) {
			int bitsInFirstWord = 64 - bitInWord;
			int bitsInSecondWord = unitSize - bitsInFirstWord;
			array[lastWordIndex] = array[lastWordIndex] >>> bitsInSecondWord << bitsInSecondWord
				| (value & maxValue) >> bitsInFirstWord;
		}
	}

	/**
	 * Читает значение по индексу. Поддерживает чтение через границу 64-битного слова.
	 *
	 * @param index индекс элемента (0..length-1)
	 * @return значение элемента (0..maxValue)
	 */
	public int get(int index) {
		Validate.inclusiveBetween(0L, length - 1, index);
		int bitOffset = index * unitSize;
		int wordIndex = bitOffset >> BIT_TO_LONG_INDEX_SHIFT;
		int lastWordIndex = (index + 1) * unitSize - 1 >> BIT_TO_LONG_INDEX_SHIFT;
		int bitInWord = bitOffset ^ wordIndex << BIT_TO_LONG_INDEX_SHIFT;

		if (wordIndex == lastWordIndex) {
			return (int) (array[wordIndex] >>> bitInWord & maxValue);
		}

		int bitsInFirstWord = 64 - bitInWord;
		return (int) ((array[wordIndex] >>> bitInWord | array[lastWordIndex] << bitsInFirstWord) & maxValue);
	}

	public long[] getAlignedArray() {
		return array;
	}

	public int getUnitSize() {
		return unitSize;
	}
}
