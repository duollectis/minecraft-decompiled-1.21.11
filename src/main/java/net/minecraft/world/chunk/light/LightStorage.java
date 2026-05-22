package net.minecraft.world.chunk.light;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import org.jspecify.annotations.Nullable;

/**
 * Абстрактное хранилище данных освещения для всех секций мира.
 * Управляет двумя копиями данных: кешированной (для быстрого чтения в основном потоке)
 * и некешированной (для потокобезопасного чтения из других потоков).
 * Поддерживает очередь ожидающих обновлений и уведомление чанков об изменениях.
 *
 * @param <M> тип карты nibble-массивов
 */
public abstract class LightStorage<M extends ChunkToNibbleArrayMap<M>> {

	private final LightType lightType;
	protected final ChunkProvider chunkProvider;
	protected final Long2ByteMap sectionPropagations = new Long2ByteOpenHashMap();
	private final LongSet enabledColumns = new LongOpenHashSet();
	protected volatile M uncachedStorage;
	protected final M storage;
	protected final LongSet dirtySections = new LongOpenHashSet();
	protected final LongSet notifySections = new LongOpenHashSet();
	protected final Long2ObjectMap<ChunkNibbleArray>
			queuedSections =
			Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap());
	private final LongSet columnsToRetain = new LongOpenHashSet();
	private final LongSet sectionsToRemove = new LongOpenHashSet();
	protected volatile boolean hasLightUpdates;

	protected LightStorage(LightType lightType, ChunkProvider chunkProvider, M lightData) {
		this.lightType = lightType;
		this.chunkProvider = chunkProvider;
		storage = lightData;
		uncachedStorage = lightData.copy();
		uncachedStorage.disableCache();
		sectionPropagations.defaultReturnValue((byte) 0);
	}

	protected boolean hasSection(long sectionPos) {
		return getLightSection(sectionPos, true) != null;
	}

	protected @Nullable ChunkNibbleArray getLightSection(long sectionPos, boolean cached) {
		return getLightSection(cached ? storage : uncachedStorage, sectionPos);
	}

	protected @Nullable ChunkNibbleArray getLightSection(M storageMap, long sectionPos) {
		return storageMap.get(sectionPos);
	}

	protected @Nullable ChunkNibbleArray getOrCreateLightSection(long sectionPos) {
		ChunkNibbleArray section = storage.get(sectionPos);

		if (section == null) {
			return null;
		}

		if (dirtySections.add(sectionPos)) {
			section = section.copy();
			storage.put(sectionPos, section);
			storage.clearCache();
		}

		return section;
	}

	public @Nullable ChunkNibbleArray getLightSection(long sectionPos) {
		ChunkNibbleArray queued = (ChunkNibbleArray) queuedSections.get(sectionPos);

		return queued != null ? queued : getLightSection(sectionPos, false);
	}

	protected abstract int getLight(long blockPos);

	protected int get(long blockPos) {
		long sectionPos = ChunkSectionPos.fromBlockPos(blockPos);
		ChunkNibbleArray section = getLightSection(sectionPos, true);

		return section.get(
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongX(blockPos)),
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongY(blockPos)),
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongZ(blockPos))
		);
	}

	protected void set(long blockPos, int value) {
		long sectionPos = ChunkSectionPos.fromBlockPos(blockPos);
		ChunkNibbleArray section = dirtySections.add(sectionPos)
				? storage.replaceWithCopy(sectionPos)
				: getLightSection(sectionPos, true);

		section.set(
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongX(blockPos)),
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongY(blockPos)),
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongZ(blockPos)),
				value
		);
		ChunkSectionPos.forEachChunkSectionAround(blockPos, notifySections::add);
	}

	/**
	 * Добавляет все 26 соседних секций вокруг заданной в набор секций для уведомления.
	 * Используется при изменении состояния секции, чтобы соседи пересчитали свет.
	 */
	protected void addNotifySections(long id) {
		int sectionX = ChunkSectionPos.unpackX(id);
		int sectionY = ChunkSectionPos.unpackY(id);
		int sectionZ = ChunkSectionPos.unpackZ(id);

		for (int dz = -1; dz <= 1; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					notifySections.add(ChunkSectionPos.asLong(sectionX + dx, sectionY + dy, sectionZ + dz));
				}
			}
		}
	}

	protected ChunkNibbleArray createSection(long sectionPos) {
		ChunkNibbleArray queued = (ChunkNibbleArray) queuedSections.get(sectionPos);

		return queued != null ? queued : new ChunkNibbleArray();
	}

	protected boolean hasLightUpdates() {
		return hasLightUpdates;
	}

	/**
	 * Применяет все ожидающие изменения секций: удаляет выгруженные секции,
	 * загружает новые данные из очереди и обновляет некешированную копию хранилища.
	 */
	protected void updateLight(ChunkLightProvider<M, ?> lightProvider) {
		if (!hasLightUpdates) {
			return;
		}

		hasLightUpdates = false;

		for (LongIterator it = sectionsToRemove.iterator(); it.hasNext(); ) {
			long sectionPos = it.nextLong();
			ChunkNibbleArray queued = (ChunkNibbleArray) queuedSections.remove(sectionPos);
			ChunkNibbleArray removed = storage.removeChunk(sectionPos);

			if (columnsToRetain.contains(ChunkSectionPos.withZeroY(sectionPos))) {
				if (queued != null) {
					queuedSections.put(sectionPos, queued);
				} else if (removed != null) {
					queuedSections.put(sectionPos, removed);
				}
			}
		}

		storage.clearCache();

		for (LongIterator it = sectionsToRemove.iterator(); it.hasNext(); ) {
			long sectionPos = it.nextLong();
			onUnloadSection(sectionPos);
			dirtySections.add(sectionPos);
		}

		sectionsToRemove.clear();

		ObjectIterator<Entry<ChunkNibbleArray>> queuedIterator = Long2ObjectMaps.fastIterator(queuedSections);

		while (queuedIterator.hasNext()) {
			Entry<ChunkNibbleArray> entry = queuedIterator.next();
			long sectionPos = entry.getLongKey();

			if (hasSection(sectionPos)) {
				ChunkNibbleArray newSection = entry.getValue();

				if (storage.get(sectionPos) != newSection) {
					storage.put(sectionPos, newSection);
					dirtySections.add(sectionPos);
				}

				queuedIterator.remove();
			}
		}

		storage.clearCache();
	}

	protected void onLoadSection(long sectionPos) {
	}

	protected void onUnloadSection(long sectionPos) {
	}

	protected void setColumnEnabled(long columnPos, boolean enabled) {
		if (enabled) {
			enabledColumns.add(columnPos);
		} else {
			enabledColumns.remove(columnPos);
		}
	}

	protected boolean isSectionInEnabledColumn(long sectionPos) {
		return enabledColumns.contains(ChunkSectionPos.withZeroY(sectionPos));
	}

	protected boolean isColumnEnabled(long columnPos) {
		return enabledColumns.contains(columnPos);
	}

	public void setRetainColumn(long sectionPos, boolean retain) {
		if (retain) {
			columnsToRetain.add(sectionPos);
		} else {
			columnsToRetain.remove(sectionPos);
		}
	}

	protected void enqueueSectionData(long sectionPos, @Nullable ChunkNibbleArray array) {
		if (array != null) {
			queuedSections.put(sectionPos, array);
			hasLightUpdates = true;
		} else {
			queuedSections.remove(sectionPos);
		}
	}

	protected void setSectionStatus(long sectionPos, boolean notReady) {
		byte current = sectionPropagations.get(sectionPos);
		byte updated = LightStorage.PropagationFlags.setReady(current, !notReady);

		if (current == updated) {
			return;
		}

		setSectionPropagation(sectionPos, updated);
		int neighborDelta = notReady ? -1 : 1;

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) {
						continue;
					}

					long neighborPos = ChunkSectionPos.offset(sectionPos, dx, dy, dz);
					byte neighborFlags = sectionPropagations.get(neighborPos);
					setSectionPropagation(
							neighborPos,
							LightStorage.PropagationFlags.withNeighborCount(
									neighborFlags,
									LightStorage.PropagationFlags.getNeighborCount(neighborFlags) + neighborDelta
							)
					);
				}
			}
		}
	}

	protected void setSectionPropagation(long sectionPos, byte flags) {
		if (flags != 0) {
			if (sectionPropagations.put(sectionPos, flags) == 0) {
				queueForUpdate(sectionPos);
			}
		} else if (sectionPropagations.remove(sectionPos) != 0) {
			queueForRemoval(sectionPos);
		}
	}

	private void queueForUpdate(long sectionPos) {
		if (!sectionsToRemove.remove(sectionPos)) {
			storage.put(sectionPos, createSection(sectionPos));
			dirtySections.add(sectionPos);
			onLoadSection(sectionPos);
			addNotifySections(sectionPos);
			hasLightUpdates = true;
		}
	}

	private void queueForRemoval(long sectionPos) {
		sectionsToRemove.add(sectionPos);
		hasLightUpdates = true;
	}

	/**
	 * Публикует изменённые секции в некешированное хранилище и уведомляет чанки об обновлении света.
	 * Вызывается после завершения всех обновлений освещения в текущем тике.
	 */
	protected void notifyChanges() {
		if (!dirtySections.isEmpty()) {
			M snapshot = storage.copy();
			snapshot.disableCache();
			uncachedStorage = snapshot;
			dirtySections.clear();
		}

		if (!notifySections.isEmpty()) {
			LongIterator it = notifySections.iterator();

			while (it.hasNext()) {
				chunkProvider.onLightUpdate(lightType, ChunkSectionPos.from(it.nextLong()));
			}

			notifySections.clear();
		}
	}

	public LightStorage.Status getStatus(long sectionPos) {
		return LightStorage.PropagationFlags.getStatus(sectionPropagations.get(sectionPos));
	}

	/**
	 * Битовые флаги состояния секции для системы распространения света.
	 * Биты 0–4: количество готовых соседей (0–26).
	 * Бит 5: флаг готовности самой секции.
	 */
	protected static class PropagationFlags {

		public static final byte EMPTY_LIGHT = 0;
		private static final int MAX_NEIGHBOR_COUNT = 26;
		private static final byte PENDING_FLAG = 32;
		private static final byte NEIGHBOR_COUNT_MASK = 31;

		public static byte setReady(byte packed, boolean ready) {
			return (byte) (ready ? packed | PENDING_FLAG : packed & -33);
		}

		/**
		 * Упаковывает количество готовых соседей в байт флагов.
		 * Количество соседей должно быть в диапазоне [0; 26].
		 */
		public static byte withNeighborCount(byte packed, int neighborCount) {
			if (neighborCount < 0 || neighborCount > MAX_NEIGHBOR_COUNT) {
				throw new IllegalArgumentException("Neighbor count was not within range [0; 26]");
			}

			return (byte) (packed & -PENDING_FLAG | neighborCount & NEIGHBOR_COUNT_MASK);
		}

		public static boolean isReady(byte packed) {
			return (packed & PENDING_FLAG) != 0;
		}

		public static int getNeighborCount(byte packed) {
			return packed & NEIGHBOR_COUNT_MASK;
		}

		public static LightStorage.Status getStatus(byte packed) {
			if (packed == 0) {
				return LightStorage.Status.EMPTY;
			}

			return isReady(packed) ? LightStorage.Status.LIGHT_AND_DATA : LightStorage.Status.LIGHT_ONLY;
		}
	}

	/**
	 * Статус секции в системе освещения.
	 * Определяет, доступны ли данные освещения для данной секции.
	 */
	public enum Status {
		EMPTY("2"),
		LIGHT_ONLY("1"),
		LIGHT_AND_DATA("0");

		private final String sigil;

		Status(final String sigil) {
			this.sigil = sigil;
		}

		public String getSigil() {
			return sigil;
		}
	}
}
