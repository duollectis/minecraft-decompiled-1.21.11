package net.minecraft.util.thread;

import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

/**
 * Утилитарный класс для параллельного преобразования значений карты с помощью {@link CompletableFuture}.
 * Автоматически выбирает стратегию батчинга: одиночные задачи на каждый элемент ({@link Single})
 * или сгруппированные пакеты ({@link Batch}), исходя из размера входных данных и {@code batchSize}.
 */
public class AsyncHelper {

	private static final int MAX_TASKS = 16;

	/**
	 * Асинхронно применяет {@code function} к каждой паре ключ-значение карты {@code futures},
	 * разбивая работу на пакеты размером не более {@code batchSize} элементов.
	 * Элементы, для которых функция вернула {@code null}, исключаются из результата.
	 */
	public static <K, U, V> CompletableFuture<Map<K, V>> mapValues(
		Map<K, U> futures,
		BiFunction<K, U, @Nullable V> function,
		int batchSize,
		Executor executor
	) {
		int size = futures.size();

		if (size == 0) {
			return CompletableFuture.completedFuture(Map.of());
		}

		if (size == 1) {
			Entry<K, U> entry = futures.entrySet().iterator().next();
			K key = entry.getKey();
			U value = entry.getValue();

			return CompletableFuture.supplyAsync(
				() -> {
					V result = function.apply(key, value);
					return result != null ? Map.of(key, result) : Map.of();
				},
				executor
			);
		}

		Batcher<K, U, V> batcher = size <= batchSize
			? new Single<>(function, size)
			: new Batch<>(function, size, batchSize);

		return batcher.mapAsync(futures, executor);
	}

	/**
	 * Перегрузка с автоматическим вычислением {@code batchSize} на основе числа доступных потоков.
	 */
	public static <K, U, V> CompletableFuture<Map<K, V>> mapValues(
		Map<K, U> futures,
		BiFunction<K, U, @Nullable V> function,
		Executor executor
	) {
		int batchSize = Util.getAvailableBackgroundThreads() * MAX_TASKS;
		return mapValues(futures, function, batchSize, executor);
	}

	/**
	 * Стратегия батчинга: несколько элементов в одной задаче.
	 * Используется, когда размер входных данных превышает {@code batchSize}.
	 */
	static class Batch<K, U, V> extends Batcher<K, U, V> {

		private final Map<K, V> entries;
		private final int size;
		private final int start;

		Batch(BiFunction<K, U, V> biFunction, int totalSize, int batchSize) {
			super(biFunction, totalSize, batchSize);
			entries = new HashMap<>(totalSize);
			size = MathHelper.ceilDiv(totalSize, batchSize);
			int paddedTotal = size * batchSize;
			int padding = paddedTotal - totalSize;
			start = batchSize - padding;

			assert start > 0 && start <= batchSize;
		}

		@Override
		protected CompletableFuture<?> newBatch(
			Future<K, U, V> futures,
			int from,
			int to,
			Executor executor
		) {
			int batchLen = to - from;

			assert batchLen == size || batchLen == size - 1;

			return CompletableFuture.runAsync(newTask(entries, from, to, futures), executor);
		}

		@Override
		protected int getLastIndex(int batch) {
			return batch < start ? size : size - 1;
		}

		private static <K, U, V> Runnable newTask(
			Map<K, V> results,
			int from,
			int to,
			Future<K, U, V> entry
		) {
			return () -> {
				for (int i = from; i < to; i++) {
					entry.apply(i);
				}

				synchronized (results) {
					for (int i = from; i < to; i++) {
						entry.copy(i, results);
					}
				}
			};
		}

		@Override
		protected CompletableFuture<Map<K, V>> addLastTask(
			CompletableFuture<?> future,
			Future<K, U, V> entry
		) {
			return future.thenApply(obj -> entries);
		}
	}

	/**
	 * Базовый класс стратегии батчинга. Управляет разбивкой входной карты на задачи
	 * и сборкой результатов через {@link CompletableFuture#allOf}.
	 */
	abstract static class Batcher<K, U, V> {

		private int lastBatch;
		private int index;
		private final CompletableFuture<?>[] futures;
		private int batch;
		private final Future<K, U, V> entry;

		Batcher(BiFunction<K, U, V> function, int size, int startAt) {
			entry = new Future<>(function, size);
			futures = new CompletableFuture[startAt];
		}

		private int nextSize() {
			return index - lastBatch;
		}

		/**
		 * Асинхронно обрабатывает все записи карты, разбивая их на пакеты,
		 * и возвращает {@link CompletableFuture} с итоговой картой результатов.
		 */
		public CompletableFuture<Map<K, V>> mapAsync(Map<K, U> map, Executor executor) {
			map.forEach((key, value) -> {
				entry.put(index++, (K) key, (U) value);

				if (nextSize() == getLastIndex(batch)) {
					futures[batch++] = newBatch(entry, lastBatch, index, executor);
					lastBatch = index;
				}
			});

			assert index == entry.keySize();
			assert lastBatch == index;
			assert batch == futures.length;

			return addLastTask(CompletableFuture.allOf(futures), entry);
		}

		protected abstract int getLastIndex(int batch);

		protected abstract CompletableFuture<?> newBatch(
			Future<K, U, V> futures,
			int from,
			int to,
			Executor executor
		);

		protected abstract CompletableFuture<Map<K, V>> addLastTask(
			CompletableFuture<?> future,
			Future<K, U, V> entry
		);
	}

	/**
	 * Контейнер для хранения ключей, входных значений и вычисленных результатов
	 * в виде параллельных массивов. Используется внутри {@link Batcher}.
	 */
	record Future<K, U, V>(BiFunction<K, U, V> operation, @Nullable Object[] keys, @Nullable Object[] values) {

		public Future(BiFunction<K, U, V> function, int size) {
			this(function, new Object[size], new Object[size]);
		}

		public void put(int index, K key, U value) {
			keys[index] = key;
			values[index] = value;
		}

		private @Nullable K getKey(int index) {
			return (K) keys[index];
		}

		private @Nullable V getValue(int index) {
			return (V) values[index];
		}

		private @Nullable U getUValue(int index) {
			return (U) values[index];
		}

		public void apply(int index) {
			values[index] = operation.apply(getKey(index), getUValue(index));
		}

		public void copy(int index, Map<K, V> results) {
			V value = getValue(index);

			if (value == null) {
				return;
			}

			results.put(getKey(index), value);
		}

		public int keySize() {
			return keys.length;
		}
	}

	/**
	 * Стратегия батчинга: одна задача на каждый элемент.
	 * Используется, когда размер входных данных не превышает {@code batchSize}.
	 */
	static class Single<K, U, V> extends Batcher<K, U, V> {

		Single(BiFunction<K, U, V> function, int size) {
			super(function, size, size);
		}

		@Override
		protected int getLastIndex(int batch) {
			return 1;
		}

		@Override
		protected CompletableFuture<?> newBatch(
			Future<K, U, V> futures,
			int from,
			int to,
			Executor executor
		) {
			assert from + 1 == to;

			return CompletableFuture.runAsync(() -> futures.apply(from), executor);
		}

		@Override
		protected CompletableFuture<Map<K, V>> addLastTask(
			CompletableFuture<?> future,
			Future<K, U, V> entry
		) {
			return future.thenApply(obj -> {
				Map<K, V> map = new HashMap<>(entry.keySize());

				for (int i = 0; i < entry.keySize(); i++) {
					entry.copy(i, map);
				}

				return map;
			});
		}
	}
}
