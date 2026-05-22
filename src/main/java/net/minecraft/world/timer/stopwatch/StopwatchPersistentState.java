package net.minecraft.world.timer.stopwatch;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

/**
 * Персистентное состояние, хранящее именованные секундомеры.
 * При сохранении конвертирует секундомеры в накопленное время (мс),
 * при загрузке — восстанавливает с текущим временем создания.
 * Это обеспечивает корректный отсчёт времени между перезагрузками сервера.
 */
public class StopwatchPersistentState extends PersistentState {

	private static final Codec<StopwatchPersistentState> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.LONG)
		.fieldOf("stopwatches")
		.codec()
		.xmap(
			StopwatchPersistentState::fromElapsedTimes,
			StopwatchPersistentState::toElapsedTimes
		);

	public static final PersistentStateType<StopwatchPersistentState> STATE_TYPE = new PersistentStateType<>(
		"stopwatches",
		StopwatchPersistentState::new,
		CODEC,
		DataFixTypes.SAVED_DATA_STOPWATCHES
	);

	private final Map<Identifier, Stopwatch> stopwatches = new Object2ObjectOpenHashMap<>();

	private StopwatchPersistentState() {
	}

	private static StopwatchPersistentState fromElapsedTimes(Map<Identifier, Long> elapsedTimes) {
		StopwatchPersistentState state = new StopwatchPersistentState();
		long now = getTimeMs();
		elapsedTimes.forEach((id, elapsed) -> state.stopwatches.put(id, new Stopwatch(now, elapsed)));
		return state;
	}

	private Map<Identifier, Long> toElapsedTimes() {
		long now = getTimeMs();
		Map<Identifier, Long> result = new TreeMap<>();
		stopwatches.forEach((id, stopwatch) -> result.put(id, stopwatch.getElapsedTimeMs(now)));
		return result;
	}

	/** @return секундомер по идентификатору, или {@code null} если не найден */
	public @Nullable Stopwatch get(Identifier id) {
		return stopwatches.get(id);
	}

	/**
	 * Добавляет секундомер, если секундомер с таким идентификатором ещё не существует.
	 *
	 * @param id        идентификатор секундомера
	 * @param stopwatch секундомер для добавления
	 * @return {@code true}, если секундомер был добавлен
	 */
	public boolean add(Identifier id, Stopwatch stopwatch) {
		if (stopwatches.putIfAbsent(id, stopwatch) != null) {
			return false;
		}

		markDirty();
		return true;
	}

	/**
	 * Обновляет существующий секундомер через функцию преобразования.
	 *
	 * @param id        идентификатор секундомера
	 * @param transform функция преобразования секундомера
	 * @return {@code true}, если секундомер был найден и обновлён
	 */
	public boolean update(Identifier id, UnaryOperator<Stopwatch> transform) {
		if (stopwatches.computeIfPresent(id, (key, stopwatch) -> transform.apply(stopwatch)) == null) {
			return false;
		}

		markDirty();
		return true;
	}

	/**
	 * Удаляет секундомер по идентификатору.
	 *
	 * @param id идентификатор секундомера
	 * @return {@code true}, если секундомер был найден и удалён
	 */
	public boolean remove(Identifier id) {
		if (stopwatches.remove(id) == null) {
			return false;
		}

		markDirty();
		return true;
	}

	@Override
	public boolean isDirty() {
		return super.isDirty() || !stopwatches.isEmpty();
	}

	/** @return неизменяемый список всех идентификаторов секундомеров */
	public List<Identifier> keys() {
		return List.copyOf(stopwatches.keySet());
	}

	public static long getTimeMs() {
		return Util.getMeasuringTimeMs();
	}
}
