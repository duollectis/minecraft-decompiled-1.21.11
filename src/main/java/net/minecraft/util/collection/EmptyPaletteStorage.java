package net.minecraft.util.collection;

import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.function.IntConsumer;

/**
 * Реализация {@link PaletteStorage} для палитры с единственным элементом (0 бит на запись).
 * Все операции чтения возвращают 0, запись допускает только значение 0.
 * Используется как оптимизация, когда весь чанк заполнен одним блоком.
 */
public class EmptyPaletteStorage implements PaletteStorage {

	public static final long[] EMPTY_DATA = new long[0];

	private final int size;

	public EmptyPaletteStorage(int size) {
		this.size = size;
	}

	@Override
	public int swap(int index, int value) {
		Validate.inclusiveBetween(0L, size - 1, index);
		Validate.inclusiveBetween(0L, 0L, value);
		return 0;
	}

	@Override
	public void set(int index, int value) {
		Validate.inclusiveBetween(0L, size - 1, index);
		Validate.inclusiveBetween(0L, 0L, value);
	}

	@Override
	public int get(int index) {
		Validate.inclusiveBetween(0L, size - 1, index);
		return 0;
	}

	@Override
	public long[] getData() {
		return EMPTY_DATA;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public int getElementBits() {
		return 0;
	}

	@Override
	public void forEach(IntConsumer action) {
		for (int i = 0; i < size; i++) {
			action.accept(0);
		}
	}

	@Override
	public void writePaletteIndices(int[] out) {
		Arrays.fill(out, 0, size, 0);
	}

	@Override
	public PaletteStorage copy() {
		return this;
	}
}
