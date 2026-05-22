package net.minecraft.util.crash;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.util.collection.ArrayListDeque;

import java.util.Queue;

/**
 * Отслеживает подавленные (suppressed) исключения, накапливая последние записи
 * и счётчики по ключу (место возникновения + тип исключения).
 * Используется для диагностики в отчётах о сбоях.
 */
public class SuppressedExceptionsTracker {

	private static final int MAX_QUEUE_SIZE = 8;

	private final Queue<Entry> queue = new ArrayListDeque<>();
	@SuppressWarnings("rawtypes")
	private final Object2IntLinkedOpenHashMap<Key> keyToCount = new Object2IntLinkedOpenHashMap();

	private static long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	/**
	 * Регистрирует подавленное исключение. Добавляет запись в очередь последних событий
	 * и увеличивает счётчик для пары (location, тип исключения).
	 * Очередь ограничена {@value MAX_QUEUE_SIZE} элементами — старые записи вытесняются.
	 *
	 * @param location  место в коде, где было подавлено исключение
	 * @param exception подавленное исключение
	 */
	@SuppressWarnings("unchecked")
	public synchronized void onSuppressedException(String location, Throwable exception) {
		long nowMs = currentTimeMillis();
		String message = exception.getMessage();
		Class<? extends Throwable> exceptionClass = (Class<? extends Throwable>) exception.getClass();

		queue.add(new Entry(nowMs, location, exceptionClass, message));

		while (queue.size() > MAX_QUEUE_SIZE) {
			queue.remove();
		}

		Key key = new Key(location, exceptionClass);
		int count = keyToCount.getInt(key);
		keyToCount.putAndMoveToFirst(key, count + 1);
	}

	/**
	 * Формирует строку с диагностической информацией: последними записями и счётчиками.
	 * Если данных нет — возвращает {@code "~~NONE~~"}.
	 */
	public synchronized String collect() {
		long nowMs = currentTimeMillis();
		StringBuilder builder = new StringBuilder();

		if (!queue.isEmpty()) {
			builder.append("\n\t\tLatest entries:\n");

			for (Entry entry : queue) {
				builder.append("\t\t\t")
					.append(entry.location)
					.append(":")
					.append(entry.cls)
					.append(": ")
					.append(entry.message)
					.append(" (")
					.append(nowMs - entry.timestampMs)
					.append("ms ago)")
					.append("\n");
			}
		}

		if (!keyToCount.isEmpty()) {
			if (builder.isEmpty()) {
				builder.append("\n");
			}

			builder.append("\t\tEntry counts:\n");

			for (Object2IntMap.Entry<Key> entry : Object2IntMaps.fastIterable(keyToCount)) {
				Key key = entry.getKey();
				builder.append("\t\t\t")
					.append(key.location)
					.append(":")
					.append(key.cls)
					.append(" x ")
					.append(entry.getIntValue())
					.append("\n");
			}
		}

		return builder.isEmpty() ? "~~NONE~~" : builder.toString();
	}

	record Entry(long timestampMs, String location, Class<? extends Throwable> cls, String message) {
	}

	record Key(String location, Class<? extends Throwable> cls) {
	}
}
