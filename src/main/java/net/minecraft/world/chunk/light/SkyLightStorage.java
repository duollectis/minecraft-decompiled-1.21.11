package net.minecraft.world.chunk.light;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;

/**
 * Хранилище небесного освещения.
 * <p>
 * В отличие от блочного света, небесный свет имеет уровень 15 во всех секциях
 * выше самой верхней загруженной секции колонки. Для этого хранится карта
 * {@link Data#columnToTopSection}: колонка → Y-индекс верхней секции.
 * <p>
 * Секции выше {@code columnToTopSection} считаются полностью освещёнными (уровень 15),
 * секции ниже — читаются из nibble-массивов как обычно.
 */
public class SkyLightStorage extends LightStorage<SkyLightStorage.Data> {

	protected SkyLightStorage(ChunkProvider chunkProvider) {
		super(
			LightType.SKY,
			chunkProvider,
			new SkyLightStorage.Data(new Long2ObjectOpenHashMap<>(), new Long2IntOpenHashMap(), Integer.MAX_VALUE)
		);
	}

	@Override
	protected int getLight(long blockPos) {
		return getLight(blockPos, false);
	}

	/**
	 * Возвращает уровень небесного света для позиции блока.
	 * <p>
	 * Если секция выше верхней загруженной секции колонки — возвращает 15.
	 * Если секция не имеет nibble-массива — поднимается вверх до первой секции с данными.
	 *
	 * @param cached {@code true} — использовать кэшированную копию данных
	 */
	protected int getLight(long blockPos, boolean cached) {
		long sectionPos = ChunkSectionPos.fromBlockPos(blockPos);
		int sectionY = ChunkSectionPos.unpackY(sectionPos);
		SkyLightStorage.Data data = cached ? storage : uncachedStorage;
		int topSection = data.columnToTopSection.get(ChunkSectionPos.withZeroY(sectionPos));

		if (topSection == data.minSectionY || sectionY >= topSection) {
			return cached && !isSectionInEnabledColumn(sectionPos) ? 0 : 15;
		}

		ChunkNibbleArray section = getLightSection(data, sectionPos);

		if (section == null) {
			// Поднимаемся вверх до первой секции с данными, т.к. промежуточные секции полностью освещены
			long currentPos = BlockPos.removeChunkSectionLocalY(blockPos);

			while (section == null) {
				if (++sectionY >= topSection) {
					return 15;
				}

				sectionPos = ChunkSectionPos.offset(sectionPos, Direction.UP);
				section = getLightSection(data, sectionPos);
			}

			blockPos = currentPos;
		}

		return section.get(
			ChunkSectionPos.getLocalCoord(BlockPos.unpackLongX(blockPos)),
			ChunkSectionPos.getLocalCoord(BlockPos.unpackLongY(blockPos)),
			ChunkSectionPos.getLocalCoord(BlockPos.unpackLongZ(blockPos))
		);
	}

	@Override
	protected void onLoadSection(long sectionPos) {
		int sectionY = ChunkSectionPos.unpackY(sectionPos);

		if (storage.minSectionY > sectionY) {
			storage.minSectionY = sectionY;
			storage.columnToTopSection.defaultReturnValue(storage.minSectionY);
		}

		long columnPos = ChunkSectionPos.withZeroY(sectionPos);
		int currentTop = storage.columnToTopSection.get(columnPos);

		if (currentTop < sectionY + 1) {
			storage.columnToTopSection.put(columnPos, sectionY + 1);
		}
	}

	@Override
	protected void onUnloadSection(long sectionPos) {
		long columnPos = ChunkSectionPos.withZeroY(sectionPos);
		int sectionY = ChunkSectionPos.unpackY(sectionPos);

		if (storage.columnToTopSection.get(columnPos) != sectionY + 1) {
			return;
		}

		// Ищем новую верхнюю секцию, спускаясь вниз
		long current = sectionPos;

		while (!hasSection(current) && isAboveMinHeight(sectionY)) {
			sectionY--;
			current = ChunkSectionPos.offset(current, Direction.DOWN);
		}

		if (hasSection(current)) {
			storage.columnToTopSection.put(columnPos, sectionY + 1);
		} else {
			storage.columnToTopSection.remove(columnPos);
		}
	}

	@Override
	protected ChunkNibbleArray createSection(long sectionPos) {
		ChunkNibbleArray queued = (ChunkNibbleArray) queuedSections.get(sectionPos);

		if (queued != null) {
			return queued;
		}

		int topSection = storage.columnToTopSection.get(ChunkSectionPos.withZeroY(sectionPos));
		int sectionY = ChunkSectionPos.unpackY(sectionPos);

		if (topSection == storage.minSectionY || sectionY >= topSection) {
			return isSectionInEnabledColumn(sectionPos) ? new ChunkNibbleArray(15) : new ChunkNibbleArray();
		}

		// Копируем данные из ближайшей секции выше (небесный свет распространяется сверху вниз)
		long above = ChunkSectionPos.offset(sectionPos, Direction.UP);
		ChunkNibbleArray aboveSection;

		while ((aboveSection = getLightSection(above, true)) == null) {
			above = ChunkSectionPos.offset(above, Direction.UP);
		}

		return copy(aboveSection);
	}

	/**
	 * Создаёт копию nibble-массива с горизонтальным дублированием строк.
	 * Используется для инициализации секций небесного света ниже уже загруженных:
	 * каждая строка верхней секции копируется во все 16 строк новой секции.
	 */
	private static ChunkNibbleArray copy(ChunkNibbleArray source) {
		if (source.isArrayUninitialized()) {
			return source.copy();
		}

		byte[] sourceBytes = source.asByteArray();
		byte[] destBytes = new byte[2048];

		for (int row = 0; row < 16; row++) {
			System.arraycopy(sourceBytes, 0, destBytes, row * 128, 128);
		}

		return new ChunkNibbleArray(destBytes);
	}

	protected boolean isAboveMinHeight(int sectionY) {
		return sectionY >= storage.minSectionY;
	}

	protected boolean isAtOrAboveTopmostSection(long sectionPos) {
		long columnPos = ChunkSectionPos.withZeroY(sectionPos);
		int topSection = storage.columnToTopSection.get(columnPos);

		return topSection == storage.minSectionY || ChunkSectionPos.unpackY(sectionPos) >= topSection;
	}

	protected int getTopSectionForColumn(long columnPos) {
		return storage.columnToTopSection.get(columnPos);
	}

	protected int getMinSectionY() {
		return storage.minSectionY;
	}

	/**
	 * Данные небесного освещения: nibble-массивы секций + карта верхних секций колонок.
	 */
	protected static final class Data extends ChunkToNibbleArrayMap<SkyLightStorage.Data> {

		int minSectionY;
		final Long2IntOpenHashMap columnToTopSection;

		public Data(
			Long2ObjectOpenHashMap<ChunkNibbleArray> arrays,
			Long2IntOpenHashMap columnToTopSection,
			int minSectionY
		) {
			super(arrays);
			this.columnToTopSection = columnToTopSection;
			columnToTopSection.defaultReturnValue(minSectionY);
			this.minSectionY = minSectionY;
		}

		public SkyLightStorage.Data copy() {
			return new SkyLightStorage.Data(arrays.clone(), columnToTopSection.clone(), minSectionY);
		}
	}
}
