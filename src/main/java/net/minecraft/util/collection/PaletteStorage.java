package net.minecraft.util.collection;

import java.util.function.IntConsumer;

/**
 * Хранилище целочисленных индексов палитры для чанков Minecraft.
 * Каждый элемент хранит индекс в палитре блоков, а не сам блок.
 * Реализации различаются по количеству бит на элемент.
 *
 * @see EmptyPaletteStorage
 * @see PackedIntegerArray
 */
public interface PaletteStorage {

	int swap(int index, int value);

	void set(int index, int value);

	int get(int index);

	long[] getData();

	int getSize();

	int getElementBits();

	void forEach(IntConsumer action);

	void writePaletteIndices(int[] out);

	PaletteStorage copy();
}
