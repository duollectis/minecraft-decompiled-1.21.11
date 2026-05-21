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

import java.util.*;
import java.util.stream.Stream;

/**
 * {@code Timer}.
 */
public class Timer<T> {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String CALLBACK_KEY = "Callback";
	private static final String NAME_KEY = "Name";
	private static final String TRIGGER_TIME_KEY = "TriggerTime";
	private final TimerCallbackSerializer<T> callback;
	private final Queue<Timer.Event<T>> events = new PriorityQueue<>(createEventComparator());
	private UnsignedLong eventCounter = UnsignedLong.ZERO;
	private final Table<String, Long, Timer.Event<T>> eventsByName = HashBasedTable.create();

	private static <T> Comparator<Timer.Event<T>> createEventComparator() {
		return Comparator.<Timer.Event<T>>comparingLong(event -> event.triggerTime).thenComparing(event -> event.id);
	}

	public Timer(TimerCallbackSerializer<T> timerCallbackSerializer, Stream<? extends Dynamic<?>> nbts) {
		this(timerCallbackSerializer);
		this.events.clear();
		this.eventsByName.clear();
		this.eventCounter = UnsignedLong.ZERO;
		nbts.forEach(nbt -> {
			NbtElement nbtElement = (NbtElement) nbt.convert(NbtOps.INSTANCE).getValue();
			if (nbtElement instanceof NbtCompound nbtCompound) {
				this.addEvent(nbtCompound);
			}
			else {
				LOGGER.warn("Invalid format of events: {}", nbtElement);
			}
		});
	}

	public Timer(TimerCallbackSerializer<T> timerCallbackSerializer) {
		this.callback = timerCallbackSerializer;
	}

	/**
	 * Обрабатывает events.
	 *
	 * @param server server
	 * @param time time
	 */
	public void processEvents(T server, long time) {
		while (true) {
			Timer.Event<T> event = this.events.peek();
			if (event == null || event.triggerTime > time) {
				return;
			}

			this.events.remove();
			this.eventsByName.remove(event.name, time);
			event.callback.call(server, this, time);
		}
	}

	public void setEventIfAbsent(String name, long triggerTime, TimerCallback<T> callback) {
		if (!this.eventsByName.contains(name, triggerTime)) {
			this.eventCounter = this.eventCounter.plus(UnsignedLong.ONE);
			Timer.Event<T> event = new Timer.Event<>(triggerTime, this.eventCounter, name, callback);
			this.eventsByName.put(name, triggerTime, event);
			this.events.add(event);
		}
	}

	/**
	 * Remove.
	 *
	 * @param name name
	 *
	 * @return int — результат операции
	 */
	public int remove(String name) {
		Collection<Timer.Event<T>> collection = this.eventsByName.row(name).values();
		collection.forEach(this.events::remove);
		int i = collection.size();
		collection.clear();
		return i;
	}

	public Set<String> getEventNames() {
		return Collections.unmodifiableSet(this.eventsByName.rowKeySet());
	}

	private void addEvent(NbtCompound nbt) {
		TimerCallback<T> timerCallback = nbt.<TimerCallback<T>>get("Callback", this.callback.getCodec()).orElse(null);
		if (timerCallback != null) {
			String string = nbt.getString("Name", "");
			long l = nbt.getLong("TriggerTime", 0L);
			this.setEventIfAbsent(string, l, timerCallback);
		}
	}

	private NbtCompound serialize(Timer.Event<T> event) {
		NbtCompound nbtCompound = new NbtCompound();
		nbtCompound.putString("Name", event.name);
		nbtCompound.putLong("TriggerTime", event.triggerTime);
		nbtCompound.put("Callback", this.callback.getCodec(), event.callback);
		return nbtCompound;
	}

	/**
	 * To nbt.
	 *
	 * @return NbtList — результат операции
	 */
	public NbtList toNbt() {
		NbtList nbtList = new NbtList();
		this.events.stream().sorted(createEventComparator()).map(this::serialize).forEach(nbtList::add);
		return nbtList;
	}

	/**
	 * {@code Event}.
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
