package net.minecraft.world.chunk.light;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.util.math.MathHelper;

import java.util.function.LongPredicate;

/**
 * Абстрактный алгоритм распространения уровней (BFS по уровням приоритета).
 * Используется системой освещения для пошагового обновления уровней света.
 * Поддерживает как увеличение (increase), так и уменьшение (decrease) уровней.
 */
public abstract class LevelPropagator {

	public static final long REMOVED_LEVEL = Long.MAX_VALUE;
	private static final int MAX_LEVEL = 255;
	protected final int levelCount;
	private final PendingUpdateQueue pendingUpdateQueue;
	private final Long2ByteMap pendingUpdates;
	private volatile boolean hasPendingUpdates;

	protected LevelPropagator(int levelCount, int expectedLevelSize, int expectedTotalSize) {
		if (levelCount >= 254) {
			throw new IllegalArgumentException("Level count must be < 254.");
		}

		this.levelCount = levelCount;
		pendingUpdateQueue = new PendingUpdateQueue(levelCount, expectedLevelSize);
		// Переопределяем rehash, чтобы не расширять хеш-таблицу сверх ожидаемого размера
		pendingUpdates = new Long2ByteOpenHashMap(expectedTotalSize, 0.5F) {
			@Override
			protected void rehash(int newN) {
				if (newN > expectedTotalSize) {
					super.rehash(newN);
				}
			}
		};
		pendingUpdates.defaultReturnValue((byte) -1);
	}

	protected void removePendingUpdate(long id) {
		int pendingLevel = pendingUpdates.remove(id) & MAX_LEVEL;

		if (pendingLevel == MAX_LEVEL) {
			return;
		}

		int currentLevel = getLevel(id);
		int queueLevel = calculateLevel(currentLevel, pendingLevel);
		pendingUpdateQueue.remove(id, queueLevel, levelCount);
		hasPendingUpdates = !pendingUpdateQueue.isEmpty();
	}

	public void removePendingUpdateIf(LongPredicate predicate) {
		LongList toRemove = new LongArrayList();
		pendingUpdates.keySet().forEach(id -> {
			if (predicate.test(id)) {
				toRemove.add(id);
			}
		});
		toRemove.forEach(this::removePendingUpdate);
	}

	private int calculateLevel(int a, int b) {
		return Math.min(Math.min(a, b), levelCount - 1);
	}

	protected void resetLevel(long id) {
		updateLevel(id, id, levelCount - 1, false);
	}

	protected void updateLevel(long sourceId, long id, int level, boolean decrease) {
		updateLevel(sourceId, id, level, getLevel(id), pendingUpdates.get(id) & MAX_LEVEL, decrease);
		hasPendingUpdates = !pendingUpdateQueue.isEmpty();
	}

	private void updateLevel(long sourceId, long id, int level, int currentLevel, int pendingLevel, boolean decrease) {
		if (isMarker(id)) {
			return;
		}

		level = MathHelper.clamp(level, 0, levelCount - 1);
		currentLevel = MathHelper.clamp(currentLevel, 0, levelCount - 1);
		boolean noPending = pendingLevel == MAX_LEVEL;

		if (noPending) {
			pendingLevel = currentLevel;
		}

		int targetLevel = decrease
				? Math.min(pendingLevel, level)
				: MathHelper.clamp(recalculateLevel(id, sourceId, level), 0, levelCount - 1);

		int oldQueueLevel = calculateLevel(currentLevel, pendingLevel);

		if (currentLevel != targetLevel) {
			int newQueueLevel = calculateLevel(currentLevel, targetLevel);

			if (oldQueueLevel != newQueueLevel && !noPending) {
				pendingUpdateQueue.remove(id, oldQueueLevel, newQueueLevel);
			}

			pendingUpdateQueue.enqueue(id, newQueueLevel);
			pendingUpdates.put(id, (byte) targetLevel);
		} else if (!noPending) {
			pendingUpdateQueue.remove(id, oldQueueLevel, levelCount);
			pendingUpdates.remove(id);
		}
	}

	/**
	 * Распространяет уровень от источника к цели через алгоритм BFS.
	 * При уменьшении (decrease=true) немедленно обновляет уровень цели.
	 * При увеличении проверяет, не совпадает ли уже распространённый уровень с текущим.
	 */
	protected final void propagateLevel(long sourceId, long targetId, int level, boolean decrease) {
		int pendingLevel = pendingUpdates.get(targetId) & MAX_LEVEL;
		int propagated = MathHelper.clamp(getPropagatedLevel(sourceId, targetId, level), 0, levelCount - 1);

		if (decrease) {
			updateLevel(sourceId, targetId, propagated, getLevel(targetId), pendingLevel, decrease);
			return;
		}

		boolean noPending = pendingLevel == MAX_LEVEL;
		int effectiveLevel = noPending
				? MathHelper.clamp(getLevel(targetId), 0, levelCount - 1)
				: pendingLevel;

		if (propagated == effectiveLevel) {
			updateLevel(
					sourceId,
					targetId,
					levelCount - 1,
					noPending ? effectiveLevel : getLevel(targetId),
					pendingLevel,
					decrease
			);
		}
	}

	protected final boolean hasPendingUpdates() {
		return hasPendingUpdates;
	}

	/**
	 * Обрабатывает ожидающие обновления уровней, не более {@code maxSteps} итераций.
	 * Возвращает оставшееся количество шагов.
	 */
	protected final int applyPendingUpdates(int maxSteps) {
		if (pendingUpdateQueue.isEmpty()) {
			return maxSteps;
		}

		while (!pendingUpdateQueue.isEmpty() && maxSteps > 0) {
			maxSteps--;
			long id = pendingUpdateQueue.dequeue();
			int currentLevel = MathHelper.clamp(getLevel(id), 0, levelCount - 1);
			int pendingLevel = pendingUpdates.remove(id) & MAX_LEVEL;

			if (pendingLevel < currentLevel) {
				setLevel(id, pendingLevel);
				propagateLevel(id, pendingLevel, true);
			} else if (pendingLevel > currentLevel) {
				setLevel(id, levelCount - 1);

				if (pendingLevel != levelCount - 1) {
					pendingUpdateQueue.enqueue(id, calculateLevel(levelCount - 1, pendingLevel));
					pendingUpdates.put(id, (byte) pendingLevel);
				}

				propagateLevel(id, currentLevel, false);
			}
		}

		hasPendingUpdates = !pendingUpdateQueue.isEmpty();

		return maxSteps;
	}

	public int getPendingUpdateCount() {
		return pendingUpdates.size();
	}

	protected boolean isMarker(long id) {
		return id == Long.MAX_VALUE;
	}

	protected abstract int recalculateLevel(long id, long excludedId, int maxLevel);

	protected abstract void propagateLevel(long id, int level, boolean decrease);

	protected abstract int getLevel(long id);

	protected abstract void setLevel(long id, int level);

	protected abstract int getPropagatedLevel(long sourceId, long targetId, int level);
}
