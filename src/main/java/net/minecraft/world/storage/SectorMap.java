package net.minecraft.world.storage;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.BitSet;

/**
 * Карта занятых секторов в файле региона (.mca).
 * Каждый бит соответствует одному сектору размером 4096 байт.
 * Используется для поиска свободных диапазонов при записи чанков.
 */
public class SectorMap {

	private final BitSet bitSet = new BitSet();

	/**
	 * Помечает диапазон секторов как занятый.
	 *
	 * @param start начальный индекс сектора
	 * @param size количество секторов
	 */
	public void allocate(int start, int size) {
		bitSet.set(start, start + size);
	}

	/**
	 * Освобождает диапазон секторов.
	 *
	 * @param start начальный индекс сектора
	 * @param size количество секторов
	 */
	public void free(int start, int size) {
		bitSet.clear(start, start + size);
	}

	/**
	 * Находит первый свободный непрерывный диапазон нужного размера,
	 * помечает его как занятый и возвращает начальный индекс.
	 *
	 * @param size требуемое количество секторов
	 * @return начальный индекс выделенного диапазона
	 */
	public int allocate(int size) {
		int searchFrom = 0;

		while (true) {
			int freeStart = bitSet.nextClearBit(searchFrom);
			int nextOccupied = bitSet.nextSetBit(freeStart);

			if (nextOccupied == -1 || nextOccupied - freeStart >= size) {
				allocate(freeStart, size);
				return freeStart;
			}

			searchFrom = nextOccupied;
		}
	}

	@VisibleForTesting
	public IntSet getAllocatedBits() {
		return bitSet.stream().collect(IntArraySet::new, IntCollection::add, IntCollection::addAll);
	}
}
