package net.minecraft.world.timer;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.UnsignedLong;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Таймер игровых событий, срабатывающих в заданный игровой тик.
 * Хранит очередь событий, отсортированных по времени срабатывания.
 * Каждое событие идентифицируется именем и временем — это позволяет
 * избежать дублирования через {@link #setEventIfAbsent}.
 *
 * @param <T> тип сервера, передаваемого в callback при срабатывании
 */
public class Timer<T> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String KEY_CALLBACK = "Callback";
	private static final String KEY_NAME = "Name";
	private static final String KEY_TRIGGER_TIME = "TriggerTime";

	private final TimerCallbackSerializer<T> callbackSerializer;
	private final Queue<Timer.Event<T>> events = new PriorityQueue<>(createEventComparator());
	private UnsignedLong eventCounter = UnsignedLong.ZERO;
	private final Table<String, Long, Timer.Event<T>> eventsByName = HashBasedTable.create();

	private static <T> Comparator<Timer.Event<T>> createEventComparator() {
		return Comparator.<Timer.Event<T>>comparingLong(event -> event.triggerTime)
			.thenComparing(event -> event.id);
	}

	/**
	 * Создаёт таймер и загружает события из NBT-потока.
	 *
	 * @param callbackSerializer сериализатор callback-ов
	 * @param nbtStream          поток NBT-элементов из сохранения
	 */
	public Timer(TimerCallbackSerializer<T> callbackSerializer, Stream<? extends Dynamic<?>> nbtStream) {
		this(callbackSerializer);
		events.clear();
		eventsByName.clear();
		eventCounter = UnsignedLong.ZERO;

		nbtStream.forEach(dynamic -> {
			NbtElement element = (NbtElement) dynamic.convert(NbtOps.INSTANCE).getValue();

			if (element instanceof NbtCompound compound) {
				addEvent(compound);
			} else {
				LOGGER.warn("Invalid format of events: {}", element);
			}
		});
	}

	public Timer(TimerCallbackSerializer<T> callbackSerializer) {
		this.callbackSerializer = callbackSerializer;
	}

	/**
	 * Обрабатывает все события, время срабатывания которых не превышает {@code time}.
	 * Вызывается каждый серверный тик.
	 *
	 * @param server объект сервера, передаваемый в callback
	 * @param time   текущее игровое время в тиках
	 */
	public void processEvents(T server, long time) {
		while (true) {
			Timer.Event<T> event = events.peek();

			if (event == null || event.triggerTime > time) {
				return;
			}

			events.remove();
			eventsByName.remove(event.name, time);
			event.callback.call(server, this, time);
		}
	}

	/**
	 * Добавляет событие, только если событие с таким именем и временем ещё не существует.
	 * Гарантирует уникальность пары (имя, время срабатывания).
	 *
	 * @param name        уникальное имя события
	 * @param triggerTime игровой тик срабатывания
	 * @param callback    действие при срабатывании
	 */
	public void setEventIfAbsent(String name, long triggerTime, TimerCallback<T> callback) {
		if (eventsByName.contains(name, triggerTime)) {
			return;
		}

		eventCounter = eventCounter.plus(UnsignedLong.ONE);
		Timer.Event<T> event = new Timer.Event<>(triggerTime, eventCounter, name, callback);
		eventsByName.put(name, triggerTime, event);
		events.add(event);
	}

	/**
	 * Удаляет все события с заданным именем.
	 *
	 * @param name имя событий для удаления
	 * @return количество удалённых событий
	 */
	public int remove(String name) {
		Collection<Timer.Event<T>> namedEvents = eventsByName.row(name).values();
		namedEvents.forEach(events::remove);
		int count = namedEvents.size();
		namedEvents.clear();
		return count;
	}

	public Set<String> getEventNames() {
		return Collections.unmodifiableSet(eventsByName.rowKeySet());
	}

	private void addEvent(NbtCompound nbt) {
		TimerCallback<T> callback = nbt.<TimerCallback<T>>get(KEY_CALLBACK, callbackSerializer.getCodec()).orElse(null);

		if (callback == null) {
			return;
		}

		String name = nbt.getString(KEY_NAME, "");
		long triggerTime = nbt.getLong(KEY_TRIGGER_TIME, 0L);
		setEventIfAbsent(name, triggerTime, callback);
	}

	private NbtCompound serialize(Timer.Event<T> event) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString(KEY_NAME, event.name);
		nbt.putLong(KEY_TRIGGER_TIME, event.triggerTime);
		nbt.put(KEY_CALLBACK, callbackSerializer.getCodec(), event.callback);
		return nbt;
	}

	/**
	 * Сериализует все события в {@link NbtList} для сохранения в файл мира.
	 * События сортируются по времени срабатывания для детерминированного порядка.
	 */
	public NbtList toNbt() {
		NbtList list = new NbtList();
		events.stream().sorted(createEventComparator()).map(this::serialize).forEach(list::add);
		return list;
	}

	/**
	 * Запланированное событие таймера.
	 *
	 * @param <T> тип сервера
	 */
	public static class Event<T> {

		public final long triggerTime;
		public final UnsignedLong id;
		public final String name;
		public final TimerCallback<T> callback;

		Event(long triggerTime, UnsignedLong id, String name, TimerCallback<T> callback) {
			this.triggerTime = triggerTime;
			this.id = id;
			this.name = name;
			this.callback = callback;
		}
	}
}
