package net.minecraft.world.poi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.debug.data.PoiDebugData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Набор точек интереса (POI) в пределах одной секции чанка.
 * Хранит POI в двух индексах: по упакованной локальной позиции и по типу,
 * что обеспечивает эффективный поиск как по координатам, так и по типу.
 */
public class PointOfInterestSet {

	private static final Logger LOGGER = LogUtils.getLogger();

	private final Short2ObjectMap<PointOfInterest> pointsOfInterestByPos = new Short2ObjectOpenHashMap<>();
	private final Map<RegistryEntry<PointOfInterestType>, Set<PointOfInterest>> pointsOfInterestByType = Maps.newHashMap();
	private final Runnable updateListener;
	private boolean valid;

	public PointOfInterestSet(Runnable updateListener) {
		this(updateListener, true, ImmutableList.of());
	}

	PointOfInterestSet(Runnable updateListener, boolean valid, List<PointOfInterest> pois) {
		this.updateListener = updateListener;
		this.valid = valid;
		pois.forEach(this::add);
	}

	public Serialized toSerialized() {
		return new Serialized(
			valid,
			pointsOfInterestByPos.values().stream().map(PointOfInterest::toSerialized).toList()
		);
	}

	/**
	 * Возвращает поток POI, соответствующих предикату типа и статусу занятости.
	 *
	 * @param typePredicate    предикат для фильтрации по типу POI
	 * @param occupationStatus требуемый статус занятости
	 * @return поток подходящих точек интереса
	 */
	public Stream<PointOfInterest> get(
		Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
		PointOfInterestStorage.OccupationStatus occupationStatus
	) {
		return pointsOfInterestByType
			.entrySet()
			.stream()
			.filter(entry -> typePredicate.test(entry.getKey()))
			.flatMap(entry -> entry.getValue().stream())
			.filter(occupationStatus.getPredicate());
	}

	/**
	 * Создаёт и добавляет новую точку интереса по позиции и типу.
	 * Если POI уже существует на данной позиции с тем же типом — возвращает {@code null}.
	 *
	 * @param pos  позиция блока
	 * @param type тип точки интереса
	 * @return созданный {@link PointOfInterest} или {@code null}, если добавление не удалось
	 */
	public @Nullable PointOfInterest add(BlockPos pos, RegistryEntry<PointOfInterestType> type) {
		PointOfInterest poi = new PointOfInterest(pos, type, updateListener);

		if (!add(poi)) {
			return null;
		}

		LOGGER.debug("Added POI of type {} @ {}", type.getIdAsString(), pos);
		updateListener.run();
		return poi;
	}

	private boolean add(PointOfInterest poi) {
		BlockPos blockPos = poi.getPos();
		RegistryEntry<PointOfInterestType> poiType = poi.getType();
		short packedPos = ChunkSectionPos.packLocal(blockPos);
		PointOfInterest existing = pointsOfInterestByPos.get(packedPos);

		if (existing != null) {
			if (poiType.equals(existing.getType())) {
				return false;
			}

			Util.logErrorOrPause("POI data mismatch: already registered at " + blockPos);
		}

		pointsOfInterestByPos.put(packedPos, poi);
		pointsOfInterestByType.computeIfAbsent(poiType, type -> Sets.newHashSet()).add(poi);
		return true;
	}

	/**
	 * Удаляет точку интереса по позиции блока.
	 * Логирует ошибку, если POI на данной позиции не зарегистрирован.
	 *
	 * @param pos позиция блока
	 */
	public void remove(BlockPos pos) {
		PointOfInterest removed = pointsOfInterestByPos.remove(ChunkSectionPos.packLocal(pos));

		if (removed == null) {
			LOGGER.error("POI data mismatch: never registered at {}", pos);
			return;
		}

		pointsOfInterestByType.get(removed.getType()).remove(removed);
		LOGGER.debug(
			"Removed POI of type {} @ {}",
			LogUtils.defer(removed::getType),
			LogUtils.defer(removed::getPos)
		);
		updateListener.run();
	}

	@Deprecated
	@Debug
	public int getFreeTickets(BlockPos pos) {
		return get(pos).map(PointOfInterest::getFreeTickets).orElse(0);
	}

	/**
	 * Освобождает билет для POI на указанной позиции.
	 * Бросает исключение, если POI на данной позиции не зарегистрирован.
	 *
	 * @param pos позиция блока
	 * @return {@code true}, если билет успешно освобождён
	 * @throws IllegalStateException если POI не зарегистрирован на данной позиции
	 */
	public boolean releaseTicket(BlockPos pos) {
		PointOfInterest poi = pointsOfInterestByPos.get(ChunkSectionPos.packLocal(pos));

		if (poi == null) {
			throw (IllegalStateException) Util.getFatalOrPause(
				new IllegalStateException("POI never registered at " + pos)
			);
		}

		boolean released = poi.releaseTicket();
		updateListener.run();
		return released;
	}

	/**
	 * Проверяет, соответствует ли тип POI на данной позиции предикату.
	 *
	 * @param pos       позиция блока
	 * @param predicate предикат для проверки типа
	 * @return {@code true}, если POI существует и его тип удовлетворяет предикату
	 */
	public boolean test(BlockPos pos, Predicate<RegistryEntry<PointOfInterestType>> predicate) {
		return getType(pos).filter(predicate).isPresent();
	}

	public Optional<RegistryEntry<PointOfInterestType>> getType(BlockPos pos) {
		return get(pos).map(PointOfInterest::getType);
	}

	private Optional<PointOfInterest> get(BlockPos pos) {
		return Optional.ofNullable(pointsOfInterestByPos.get(ChunkSectionPos.packLocal(pos)));
	}

	public Optional<PoiDebugData> getDebugData(BlockPos pos) {
		return get(pos).map(PoiDebugData::new);
	}

	/**
	 * Обновляет набор POI, используя внешний поставщик данных.
	 * Вызывается при загрузке секции чанка, когда данные ещё не были проверены.
	 * Если набор уже валиден — метод ничего не делает.
	 *
	 * @param updater функция, принимающая {@link BiConsumer} для регистрации актуальных POI
	 */
	public void updatePointsOfInterest(Consumer<BiConsumer<BlockPos, RegistryEntry<PointOfInterestType>>> updater) {
		if (valid) {
			return;
		}

		Short2ObjectMap<PointOfInterest> snapshot = new Short2ObjectOpenHashMap<>(pointsOfInterestByPos);
		clear();

		updater.accept((pos, poiEntry) -> {
			short packedPos = ChunkSectionPos.packLocal(pos);
			PointOfInterest poi = snapshot.computeIfAbsent(
				packedPos, key -> new PointOfInterest(pos, poiEntry, updateListener)
			);
			add(poi);
		});

		valid = true;
		updateListener.run();
	}

	private void clear() {
		pointsOfInterestByPos.clear();
		pointsOfInterestByType.clear();
	}

	boolean isValid() {
		return valid;
	}

	/**
	 * Сериализованное представление {@link PointOfInterestSet} для записи на диск.
	 */
	public record Serialized(boolean isValid, List<PointOfInterest.Serialized> records) {

		public static final Codec<Serialized> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.BOOL
					.lenientOptionalFieldOf("Valid", false)
					.forGetter(Serialized::isValid),
				PointOfInterest.Serialized.CODEC
					.listOf()
					.fieldOf("Records")
					.forGetter(Serialized::records)
			).apply(instance, Serialized::new)
		);

		/**
		 * Восстанавливает {@link PointOfInterestSet} из сериализованного состояния.
		 *
		 * @param updateListener слушатель изменений
		 * @return восстановленный набор точек интереса
		 */
		public PointOfInterestSet toPointOfInterestSet(Runnable updateListener) {
			return new PointOfInterestSet(
				updateListener,
				isValid,
				records.stream().map(serialized -> serialized.toPointOfInterest(updateListener)).toList()
			);
		}
	}
}
