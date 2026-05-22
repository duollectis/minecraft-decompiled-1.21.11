package net.minecraft.world.entity;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.PrimitiveIterator.OfLong;
import java.util.Spliterators;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Пространственный кэш сущностей, разбитый по чанк-секциям (16×16×16 блоков).
 * Обеспечивает эффективный поиск сущностей в заданном AABB через итерацию
 * только по секциям, пересекающимся с областью поиска.
 *
 * <p>Секции организованы в отсортированном множестве по упакованным координатам,
 * что позволяет быстро находить все секции в заданном диапазоне X.
 *
 * @param <T> тип сущностей в кэше
 */
public class SectionedEntityCache<T extends EntityLike> {

	/** Сдвиг для перевода координат блоков в координаты секций (2^2 = 4 блока на секцию). */
	public static final int SECTION_SHIFT = 2;
	/** Размер секции в блоках по каждой оси. */
	public static final int SECTION_SIZE = 4;

	/**
	 * Расширение области поиска по горизонтали (X/Z) при запросе секций.
	 * Компенсирует сущности, чьи bounding box выходят за пределы их секции.
	 */
	private static final double HORIZONTAL_SEARCH_EXPANSION = 2.0;
	/**
	 * Расширение области поиска вниз по Y.
	 * Увеличено до 4.0 для учёта высоких сущностей (лошади, великаны и т.д.).
	 */
	private static final double VERTICAL_SEARCH_EXPANSION_DOWN = 4.0;

	/** Характеристики сплитератора: ORDERED | DISTINCT | NONNULL | IMMUTABLE. */
	private static final int SPLITERATOR_CHARACTERISTICS = 1301;

	private final Class<T> entityClass;
	private final Long2ObjectFunction<EntityTrackingStatus> posToStatus;
	private final Long2ObjectMap<EntityTrackingSection<T>> trackingSections = new Long2ObjectOpenHashMap<>();
	private final LongSortedSet trackedPositions = new LongAVLTreeSet();

	public SectionedEntityCache(
		Class<T> entityClass,
		Long2ObjectFunction<EntityTrackingStatus> chunkStatusDiscriminator
	) {
		this.entityClass = entityClass;
		this.posToStatus = chunkStatusDiscriminator;
	}

	/**
	 * Итерирует все непустые отслеживаемые секции, пересекающиеся с указанным box.
	 * Область поиска расширяется на {@link #HORIZONTAL_SEARCH_EXPANSION} по X/Z
	 * и на {@link #VERTICAL_SEARCH_EXPANSION_DOWN} вниз по Y для корректного захвата
	 * сущностей на границах секций.
	 *
	 * @param box      область поиска
	 * @param consumer получатель секций; итерация прерывается при {@code ABORT}
	 */
	public void forEachInBox(Box box, LazyIterationConsumer<EntityTrackingSection<T>> consumer) {
		int minSectionX = ChunkSectionPos.getSectionCoord(box.minX - HORIZONTAL_SEARCH_EXPANSION);
		int minSectionY = ChunkSectionPos.getSectionCoord(box.minY - VERTICAL_SEARCH_EXPANSION_DOWN);
		int minSectionZ = ChunkSectionPos.getSectionCoord(box.minZ - HORIZONTAL_SEARCH_EXPANSION);
		int maxSectionX = ChunkSectionPos.getSectionCoord(box.maxX + HORIZONTAL_SEARCH_EXPANSION);
		int maxSectionY = ChunkSectionPos.getSectionCoord(box.maxY + 0.0);
		int maxSectionZ = ChunkSectionPos.getSectionCoord(box.maxZ + HORIZONTAL_SEARCH_EXPANSION);

		for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
			long rangeStart = ChunkSectionPos.asLong(sectionX, 0, 0);
			long rangeEnd = ChunkSectionPos.asLong(sectionX, -1, -1);
			LongIterator iterator = trackedPositions.subSet(rangeStart, rangeEnd + 1L).iterator();

			while (iterator.hasNext()) {
				long sectionPos = iterator.nextLong();
				int sectionY = ChunkSectionPos.unpackY(sectionPos);
				int sectionZ = ChunkSectionPos.unpackZ(sectionPos);

				if (sectionY < minSectionY || sectionY > maxSectionY
					|| sectionZ < minSectionZ || sectionZ > maxSectionZ
				) {
					continue;
				}

				EntityTrackingSection<T> section = trackingSections.get(sectionPos);
				if (section != null
					&& !section.isEmpty()
					&& section.getStatus().shouldTrack()
					&& consumer.accept(section).shouldAbort()
				) {
					return;
				}
			}
		}
	}

	/**
	 * Возвращает поток упакованных позиций всех секций в указанном чанке.
	 *
	 * @param chunkPos упакованная позиция чанка
	 * @return поток упакованных позиций секций
	 */
	public LongStream getSections(long chunkPos) {
		int chunkX = ChunkPos.getPackedX(chunkPos);
		int chunkZ = ChunkPos.getPackedZ(chunkPos);
		LongSortedSet sections = getSectionsInColumn(chunkX, chunkZ);
		if (sections.isEmpty()) {
			return LongStream.empty();
		}

		OfLong iterator = sections.iterator();
		return StreamSupport.longStream(
			Spliterators.spliteratorUnknownSize(iterator, SPLITERATOR_CHARACTERISTICS),
			false
		);
	}

	private LongSortedSet getSectionsInColumn(int chunkX, int chunkZ) {
		long rangeStart = ChunkSectionPos.asLong(chunkX, 0, chunkZ);
		long rangeEnd = ChunkSectionPos.asLong(chunkX, -1, chunkZ);
		return trackedPositions.subSet(rangeStart, rangeEnd + 1L);
	}

	public Stream<EntityTrackingSection<T>> getTrackingSections(long chunkPos) {
		return getSections(chunkPos)
			.<EntityTrackingSection<T>>mapToObj(trackingSections::get)
			.filter(Objects::nonNull);
	}

	private static long chunkPosFromSectionPos(long sectionPos) {
		return ChunkPos.toLong(ChunkSectionPos.unpackX(sectionPos), ChunkSectionPos.unpackZ(sectionPos));
	}

	public EntityTrackingSection<T> getTrackingSection(long sectionPos) {
		return trackingSections.computeIfAbsent(sectionPos, this::addSection);
	}

	public @Nullable EntityTrackingSection<T> findTrackingSection(long sectionPos) {
		return trackingSections.get(sectionPos);
	}

	private EntityTrackingSection<T> addSection(long sectionPos) {
		long chunkPos = chunkPosFromSectionPos(sectionPos);
		EntityTrackingStatus status = posToStatus.get(chunkPos);
		trackedPositions.add(sectionPos);
		return new EntityTrackingSection<>(entityClass, status);
	}

	public LongSet getChunkPositions() {
		LongSet result = new LongOpenHashSet();
		trackingSections.keySet().forEach(sectionPos -> result.add(chunkPosFromSectionPos(sectionPos)));
		return result;
	}

	public void forEachIntersects(Box box, LazyIterationConsumer<T> consumer) {
		forEachInBox(box, section -> section.forEach(box, consumer));
	}

	public <U extends T> void forEachIntersects(TypeFilter<T, U> filter, Box box, LazyIterationConsumer<U> consumer) {
		forEachInBox(box, section -> section.forEach(filter, box, consumer));
	}

	public void removeSection(long sectionPos) {
		trackingSections.remove(sectionPos);
		trackedPositions.remove(sectionPos);
	}

	@Debug
	public int sectionCount() {
		return trackedPositions.size();
	}
}
