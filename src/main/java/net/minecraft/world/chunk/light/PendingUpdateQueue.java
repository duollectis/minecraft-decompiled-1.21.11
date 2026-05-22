package net.minecraft.world.chunk.light;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

/**
 * Очередь ожидающих обновлений освещения, упорядоченная по уровням приоритета.
 * Каждый уровень хранит набор идентификаторов блоков/секций, ожидающих обработки.
 * Поддерживает быстрое извлечение элемента с минимальным уровнем.
 */
public class PendingUpdateQueue {

	private final int levelCount;
	private final LongLinkedOpenHashSet[] pendingIdUpdatesByLevel;
	private int minPendingLevel;

	public PendingUpdateQueue(int levelCount, int expectedLevelSize) {
		this.levelCount = levelCount;
		this.pendingIdUpdatesByLevel = new LongLinkedOpenHashSet[levelCount];

		for (int level = 0; level < levelCount; level++) {
			// Переопределяем rehash, чтобы не расширять хеш-таблицу сверх ожидаемого размера
			this.pendingIdUpdatesByLevel[level] = new LongLinkedOpenHashSet(expectedLevelSize, 0.5F) {
				@Override
				protected void rehash(int newN) {
					if (newN > expectedLevelSize) {
						super.rehash(newN);
					}
				}
			};
		}

		this.minPendingLevel = levelCount;
	}

	public long dequeue() {
		LongLinkedOpenHashSet bucket = pendingIdUpdatesByLevel[minPendingLevel];
		long id = bucket.removeFirstLong();

		if (bucket.isEmpty()) {
			increaseMinPendingLevel(levelCount);
		}

		return id;
	}

	public boolean isEmpty() {
		return minPendingLevel >= levelCount;
	}

	public void remove(long id, int level, int newLevelCount) {
		LongLinkedOpenHashSet bucket = pendingIdUpdatesByLevel[level];
		bucket.remove(id);

		if (bucket.isEmpty() && minPendingLevel == level) {
			increaseMinPendingLevel(newLevelCount);
		}
	}

	public void enqueue(long id, int level) {
		pendingIdUpdatesByLevel[level].add(id);

		if (minPendingLevel > level) {
			minPendingLevel = level;
		}
	}

	private void increaseMinPendingLevel(int maxLevel) {
		int previousMin = minPendingLevel;
		minPendingLevel = maxLevel;

		for (int level = previousMin + 1; level < maxLevel; level++) {
			if (!pendingIdUpdatesByLevel[level].isEmpty()) {
				minPendingLevel = level;
				break;
			}
		}
	}
}
