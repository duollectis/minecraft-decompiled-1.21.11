package net.minecraft.world.poi;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BlockState;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.world.ChunkErrorHandler;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.SectionDistanceLevelPropagator;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.debug.data.PoiDebugData;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import net.minecraft.world.storage.StorageKey;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Хранилище точек интереса (POI) для всего мира.
 * Управляет загрузкой, сохранением и поиском POI по позиции, типу и статусу занятости.
 * Также отслеживает расстояние до ближайшего занятого POI через {@link PointOfInterestDistanceTracker}.
 */
public class PointOfInterestStorage extends SerializingRegionBasedStorage<PointOfInterestSet, PointOfInterestSet.Serialized> {

	public static final int SEARCH_RADIUS = 6;
	public static final int OCCUPATION_LIMIT = 1;

	private final PointOfInterestDistanceTracker pointOfInterestDistanceTracker;
	private final LongSet preloadedChunks = new LongOpenHashSet();

	public PointOfInterestStorage(
		StorageKey storageKey,
		Path directory,
		DataFixer dataFixer,
		boolean dsync,
		DynamicRegistryManager registryManager,
		ChunkErrorHandler errorHandler,
		HeightLimitView world
	) {
		super(
			new VersionedChunkStorage(storageKey, directory, dataFixer, dsync, DataFixTypes.POI_CHUNK),
			PointOfInterestSet.Serialized.CODEC,
			PointOfInterestSet::toSerialized,
			PointOfInterestSet.Serialized::toPointOfInterestSet,
			PointOfInterestSet::new,
			registryManager,
			errorHandler,
			world
		);
		pointOfInterestDistanceTracker = new PointOfInterestDistanceTracker();
	}

	/**
	 * Добавляет новую точку интереса в секцию чанка, соответствующую позиции.
	 *
	 * @param pos  позиция блока
	 * @param type тип POI
	 * @return созданный {@link PointOfInterest} или {@code null}, если добавление не удалось
	 */
	public @Nullable PointOfInterest add(BlockPos pos, RegistryEntry<PointOfInterestType> type) {
		return getOrCreate(ChunkSectionPos.toLong(pos)).add(pos, type);
	}

	/**
	 * Удаляет точку интереса по позиции блока, если секция загружена.
	 *
	 * @param pos позиция блока
	 */
	public void remove(BlockPos pos) {
		get(ChunkSectionPos.toLong(pos)).ifPresent(poiSet -> poiSet.remove(pos));
	}

	/**
	 * Подсчитывает количество POI в круге вокруг позиции, соответствующих предикату и статусу.
	 *
	 * @param typePredicate    предикат типа POI
	 * @param pos              центр поиска
	 * @param radius           радиус поиска в блоках
	 * @param occupationStatus требуемый статус занятости
	 * @return количество найденных POI
	 */
	public long count(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		BlockPos pos,
		int radius,
		OccupationStatus occupationStatus
	) {
		return getInCircle(typePredicate, pos, radius, occupationStatus).count();
	}

	public boolean hasTypeAt(RegistryKey<PointOfInterestType> type, BlockPos pos) {
		return test(pos, entry -> entry.matchesKey(type));
	}

	/**
	 * Возвращает поток POI в квадрате вокруг позиции.
	 * Квадрат выровнен по границам чанков и дополнительно фильтруется по точным координатам.
	 *
	 * @param typePredicate    предикат типа POI
	 * @param pos              центр поиска
	 * @param radius           половина стороны квадрата в блоках
	 * @param occupationStatus требуемый статус занятости
	 * @return поток POI в квадрате
	 */
	public Stream<PointOfInterest> getInSquare(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		BlockPos pos,
		int radius,
		OccupationStatus occupationStatus
	) {
		int chunkRadius = Math.floorDiv(radius, 16) + 1;
		return ChunkPos
			.stream(new ChunkPos(pos), chunkRadius)
			.flatMap(chunkPos -> getInChunk(typePredicate, chunkPos, occupationStatus))
			.filter(poi -> {
				BlockPos poiPos = poi.getPos();
				return Math.abs(poiPos.getX() - pos.getX()) <= radius
					&& Math.abs(poiPos.getZ() - pos.getZ()) <= radius;
			});
	}

	/**
	 * Возвращает поток POI в круге вокруг позиции (по квадрату расстояния).
	 *
	 * @param typePredicate    предикат типа POI
	 * @param pos              центр поиска
	 * @param radius           радиус в блоках
	 * @param occupationStatus требуемый статус занятости
	 * @return поток POI в круге
	 */
	public Stream<PointOfInterest> getInCircle(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		BlockPos pos,
		int radius,
		OccupationStatus occupationStatus
	) {
		int radiusSquared = radius * radius;
		return getInSquare(typePredicate, pos, radius, occupationStatus)
			.filter(poi -> poi.getPos().getSquaredDistance(pos) <= radiusSquared);
	}

	@Debug
	public Stream<PointOfInterest> getInChunk(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		ChunkPos chunkPos,
		OccupationStatus occupationStatus
	) {
		return IntStream.rangeClosed(world.getBottomSectionCoord(), world.getTopSectionCoord())
			.boxed()
			.map(coord -> get(ChunkSectionPos.from(chunkPos, coord).asLong()))
			.filter(Optional::isPresent)
			.flatMap(poiSet -> poiSet.get().get(typePredicate, occupationStatus));
	}

	/**
	 * Возвращает поток позиций POI в круге, дополнительно фильтруя по предикату позиции.
	 *
	 * @param typePredicate    предикат типа POI
	 * @param posPredicate     предикат позиции блока
	 * @param pos              центр поиска
	 * @param radius           радиус в блоках
	 * @param occupationStatus требуемый статус занятости
	 * @return поток позиций блоков
	 */
	public Stream<BlockPos> getPositions(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		Predicate<BlockPos> posPredicate,
		BlockPos pos,
		int radius,
		OccupationStatus occupationStatus
	) {
		return getInCircle(typePredicate, pos, radius, occupationStatus)
			.map(PointOfInterest::getPos)
			.filter(posPredicate);
	}

	public Stream<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> getTypesAndPositions(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		Predicate<BlockPos> posPredicate,
		BlockPos pos,
		int radius,
		OccupationStatus occupationStatus
	) {
		return getInCircle(typePredicate, pos, radius, occupationStatus)
			.filter(poi -> posPredicate.test(poi.getPos()))
			.map(poi -> Pair.of(poi.getType(), poi.getPos()));
	}

	public Stream<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> getSortedTypesAndPositions(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		Predicate<BlockPos> posPredicate,
		BlockPos pos,
		int radius,
		OccupationStatus occupationStatus
	) {
		return getTypesAndPositions(typePredicate, posPredicate, pos, radius, occupationStatus)
			.sorted(Comparator.comparingDouble(pair -> ((BlockPos) pair.getSecond()).getSquaredDistance(pos)));
	}

	public Optional<BlockPos> getPosition(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		Predicate<BlockPos> posPredicate,
		BlockPos pos,
		int radius,
		OccupationStatus occupationStatus
	) {
		return getPositions(typePredicate, posPredicate, pos, radius, occupationStatus).findFirst();
	}

	public Optional<BlockPos> getNearestPosition(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		BlockPos pos,
		int radius,
		OccupationStatus occupationStatus
	) {
		return getInCircle(typePredicate, pos, radius, occupationStatus)
			.map(PointOfInterest::getPos)
			.min(Comparator.comparingDouble(poiPos -> poiPos.getSquaredDistance(pos)));
	}

	public Optional<Pair<RegistryEntry<PointOfInterestType>, BlockPos>> getNearestTypeAndPosition(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		BlockPos pos,
		int radius,
		OccupationStatus occupationStatus
	) {
		return getInCircle(typePredicate, pos, radius, occupationStatus)
			.min(Comparator.comparingDouble(poi -> poi.getPos().getSquaredDistance(pos)))
			.map(poi -> Pair.of(poi.getType(), poi.getPos()));
	}

	public Optional<BlockPos> getNearestPosition(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		Predicate<BlockPos> posPredicate,
		BlockPos pos,
		int radius,
		OccupationStatus occupationStatus
	) {
		return getInCircle(typePredicate, pos, radius, occupationStatus)
			.map(PointOfInterest::getPos)
			.filter(posPredicate)
			.min(Comparator.comparingDouble(poiPos -> poiPos.getSquaredDistance(pos)));
	}

	/**
	 * Ищет первый подходящий POI с резервированием билета.
	 * Резервирует билет у найденного POI, чтобы другие сущности не могли его занять.
	 *
	 * @param typePredicate предикат типа POI
	 * @param posPredicate  дополнительный предикат позиции и типа
	 * @param pos           центр поиска
	 * @param radius        радиус в блоках
	 * @return позиция зарезервированного POI или {@link Optional#empty()}
	 */
	public Optional<BlockPos> getPosition(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		BiPredicate<RegistryEntry<PointOfInterestType>, BlockPos> posPredicate,
		BlockPos pos,
		int radius
	) {
		return getInCircle(typePredicate, pos, radius, OccupationStatus.HAS_SPACE)
			.filter(poi -> posPredicate.test(poi.getType(), poi.getPos()))
			.findFirst()
			.map(poi -> {
				poi.reserveTicket();
				return poi.getPos();
			});
	}

	/**
	 * Ищет случайный подходящий POI из перемешанного списка.
	 *
	 * @param typePredicate    предикат типа POI
	 * @param posPredicate     предикат позиции
	 * @param occupationStatus требуемый статус занятости
	 * @param pos              центр поиска
	 * @param radius           радиус в блоках
	 * @param random           источник случайности для перемешивания
	 * @return случайная позиция подходящего POI или {@link Optional#empty()}
	 */
	public Optional<BlockPos> getPosition(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		Predicate<BlockPos> posPredicate,
		OccupationStatus occupationStatus,
		BlockPos pos,
		int radius,
		Random random
	) {
		List<PointOfInterest> shuffled = Util.copyShuffled(getInCircle(typePredicate, pos, radius, occupationStatus), random);
		return shuffled
			.stream()
			.filter(poi -> posPredicate.test(poi.getPos()))
			.findFirst()
			.map(PointOfInterest::getPos);
	}

	/**
	 * Освобождает билет POI на указанной позиции.
	 *
	 * @param pos позиция блока
	 * @return {@code true}, если билет успешно освобождён
	 * @throws IllegalStateException если POI не зарегистрирован на данной позиции
	 */
	public boolean releaseTicket(BlockPos pos) {
		return get(ChunkSectionPos.toLong(pos))
			.map(poiSet -> poiSet.releaseTicket(pos))
			.orElseThrow(() -> Util.getFatalOrPause(new IllegalStateException("POI never registered at " + pos)));
	}

	/**
	 * Проверяет, соответствует ли тип POI на данной позиции предикату.
	 *
	 * @param pos       позиция блока
	 * @param predicate предикат для проверки типа
	 * @return {@code true}, если POI существует и его тип удовлетворяет предикату
	 */
	public boolean test(BlockPos pos, Predicate<RegistryEntry<PointOfInterestType>> predicate) {
		return get(ChunkSectionPos.toLong(pos)).map(poiSet -> poiSet.test(pos, predicate)).orElse(false);
	}

	public Optional<RegistryEntry<PointOfInterestType>> getType(BlockPos pos) {
		return get(ChunkSectionPos.toLong(pos)).flatMap(poiSet -> poiSet.getType(pos));
	}

	@Debug
	public @Nullable PoiDebugData getDebugData(BlockPos pos) {
		return get(ChunkSectionPos.toLong(pos)).flatMap(poiSet -> poiSet.getDebugData(pos)).orElse(null);
	}

	public int getDistanceFromNearestOccupied(ChunkSectionPos pos) {
		pointOfInterestDistanceTracker.update();
		return pointOfInterestDistanceTracker.getLevel(pos.asLong());
	}

	boolean isOccupied(long pos) {
		Optional<PointOfInterestSet> optional = getIfLoaded(pos);

		if (optional == null) {
			return false;
		}

		return optional.<Boolean>map(
			poiSet -> poiSet
				.get(entry -> entry.isIn(PointOfInterestTypeTags.VILLAGE), OccupationStatus.IS_OCCUPIED)
				.findAny()
				.isPresent()
		).orElse(false);
	}

	@Override
	public void tick(BooleanSupplier shouldKeepTicking) {
		super.tick(shouldKeepTicking);
		pointOfInterestDistanceTracker.update();
	}

	@Override
	protected void onUpdate(long pos) {
		super.onUpdate(pos);
		pointOfInterestDistanceTracker.update(
			pos,
			pointOfInterestDistanceTracker.getInitialLevel(pos),
			false
		);
	}

	@Override
	protected void onLoad(long pos) {
		pointOfInterestDistanceTracker.update(
			pos,
			pointOfInterestDistanceTracker.getInitialLevel(pos),
			false
		);
	}

	/**
	 * Инициализирует POI для секции чанка на основе палитры блоков.
	 * Если секция уже загружена — обновляет существующий набор POI.
	 * Если секция не загружена, но содержит POI-блоки — создаёт новый набор.
	 *
	 * @param sectionPos   позиция секции чанка
	 * @param chunkSection секция чанка с данными блоков
	 */
	public void initForPalette(ChunkSectionPos sectionPos, ChunkSection chunkSection) {
		Util.ifPresentOrElse(
			get(sectionPos.asLong()),
			poiSet -> poiSet.updatePointsOfInterest(populator -> {
				if (shouldScan(chunkSection)) {
					scanAndPopulate(chunkSection, sectionPos, populator);
				}
			}),
			() -> {
				if (shouldScan(chunkSection)) {
					PointOfInterestSet poiSet = getOrCreate(sectionPos.asLong());
					scanAndPopulate(chunkSection, sectionPos, poiSet::add);
				}
			}
		);
	}

	private static boolean shouldScan(ChunkSection chunkSection) {
		return chunkSection.hasAny(PointOfInterestTypes::isPointOfInterest);
	}

	/**
	 * Сканирует все блоки секции и регистрирует найденные POI через переданный {@code populator}.
	 *
	 * @param chunkSection секция чанка
	 * @param sectionPos   позиция секции
	 * @param populator    функция регистрации POI по позиции и типу
	 */
	private void scanAndPopulate(
		ChunkSection chunkSection,
		ChunkSectionPos sectionPos,
		BiConsumer<BlockPos, RegistryEntry<PointOfInterestType>> populator
	) {
		sectionPos.streamBlocks().forEach(pos -> {
			BlockState blockState = chunkSection.getBlockState(
				ChunkSectionPos.getLocalCoord(pos.getX()),
				ChunkSectionPos.getLocalCoord(pos.getY()),
				ChunkSectionPos.getLocalCoord(pos.getZ())
			);
			PointOfInterestTypes
				.getTypeForState(blockState)
				.ifPresent(poiType -> populator.accept(pos, (RegistryEntry<PointOfInterestType>) poiType));
		});
	}

	/**
	 * Предзагружает чанки вокруг позиции, чтобы данные POI были доступны синхронно.
	 * Пропускает чанки, которые уже загружены и валидны.
	 *
	 * @param world  вид мира для загрузки чанков
	 * @param pos    центральная позиция
	 * @param radius радиус предзагрузки в блоках
	 */
	public void preloadChunks(WorldView world, BlockPos pos, int radius) {
		ChunkSectionPos
			.stream(
				new ChunkPos(pos),
				Math.floorDiv(radius, 16),
				this.world.getBottomSectionCoord(),
				this.world.getTopSectionCoord()
			)
			.map(sectionPos -> Pair.of(sectionPos, get(sectionPos.asLong())))
			.filter(pair -> ((Optional<PointOfInterestSet>) pair.getSecond())
				.map(PointOfInterestSet::isValid)
				.orElse(false) == false)
			.map(pair -> ((ChunkSectionPos) pair.getFirst()).toChunkPos())
			.filter(chunkPos -> preloadedChunks.add(chunkPos.toLong()))
			.forEach(chunkPos -> world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.EMPTY));
	}

	/**
	 * Статус занятости точки интереса, используемый при поиске POI.
	 */
	public enum OccupationStatus {
		HAS_SPACE(PointOfInterest::hasSpace),
		IS_OCCUPIED(PointOfInterest::isOccupied),
		ANY(poi -> true);

		private final Predicate<? super PointOfInterest> predicate;

		OccupationStatus(Predicate<? super PointOfInterest> predicate) {
			this.predicate = predicate;
		}

		public Predicate<? super PointOfInterest> getPredicate() {
			return predicate;
		}
	}

	/**
	 * Трекер расстояния до ближайшего занятого POI деревни.
	 * Использует алгоритм распространения уровней по секциям чанков.
	 * Уровень 0 — секция содержит занятый POI деревни; уровень 7 — вне зоны деревни.
	 */
	final class PointOfInterestDistanceTracker extends SectionDistanceLevelPropagator {

		private static final int MAX_DISTANCE = 7;
		private static final int OCCUPIED_LEVEL = 0;
		private static final byte DEFAULT_DISTANCE = 7;

		private final Long2ByteMap distances = new Long2ByteOpenHashMap();

		protected PointOfInterestDistanceTracker() {
			super(MAX_DISTANCE, 16, 256);
			distances.defaultReturnValue(DEFAULT_DISTANCE);
		}

		@Override
		protected int getInitialLevel(long id) {
			return PointOfInterestStorage.this.isOccupied(id) ? OCCUPIED_LEVEL : MAX_DISTANCE;
		}

		@Override
		protected int getLevel(long id) {
			return distances.get(id);
		}

		@Override
		protected void setLevel(long id, int level) {
			if (level > MAX_DISTANCE - 1) {
				distances.remove(id);
			} else {
				distances.put(id, (byte) level);
			}
		}

		public void update() {
			super.applyPendingUpdates(Integer.MAX_VALUE);
		}
	}
}
