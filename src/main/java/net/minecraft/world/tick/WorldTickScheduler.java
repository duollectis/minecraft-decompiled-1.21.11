package net.minecraft.world.tick;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;

/**
 * Глобальный планировщик тиков мира, агрегирующий {@link ChunkTickScheduler} по всем загруженным чанкам.
 * Реализует двухфазный алгоритм: сначала собирает тики, готовые к выполнению, затем выполняет их.
 *
 * <p>Алгоритм сбора тиков:
 * <ol>
 *   <li>Из {@code nextTriggerTickByChunkPos} выбираются чанки, у которых время следующего тика ≤ текущему.</li>
 *   <li>Из готовых чанков извлекаются тики до достижения лимита {@code maxTicks}.</li>
 *   <li>Оставшиеся чанки возвращаются в {@code nextTriggerTickByChunkPos} с обновлённым временем.</li>
 * </ol>
 *
 * @param <T> тип объекта, для которого планируются тики
 */
public class WorldTickScheduler<T> implements QueryableTickScheduler<T> {

	private static final Comparator<ChunkTickScheduler<?>> CHUNK_COMPARATOR =
		(a, b) -> OrderedTick.BASIC_COMPARATOR.compare(a.peekNextTick(), b.peekNextTick());

	private final LongPredicate tickingFutureReadyPredicate;
	private final Long2ObjectMap<ChunkTickScheduler<T>> chunkSchedulers = new Long2ObjectOpenHashMap<>();
	private final Long2LongMap nextTriggerTickByChunk =
		Util.make(new Long2LongOpenHashMap(), map -> map.defaultReturnValue(Long.MAX_VALUE));
	private final Queue<ChunkTickScheduler<T>> tickableChunkSchedulers = new PriorityQueue<>(CHUNK_COMPARATOR);
	private final Queue<OrderedTick<T>> tickableTicks = new ArrayDeque<>();
	private final List<OrderedTick<T>> tickedTicks = new ArrayList<>();
	private final Set<OrderedTick<?>> tickingSet = new ObjectOpenCustomHashSet<>(OrderedTick.HASH_STRATEGY);

	private final BiConsumer<ChunkTickScheduler<T>, OrderedTick<T>> queuedTickConsumer = (chunkScheduler, tick) -> {
		if (tick.equals(chunkScheduler.peekNextTick())) {
			scheduleNextTrigger(tick);
		}
	};

	/**
	 * @param tickingFutureReadyPredicate предикат, проверяющий, готов ли чанк (по long-ключу) к тикам
	 */
	public WorldTickScheduler(LongPredicate tickingFutureReadyPredicate) {
		this.tickingFutureReadyPredicate = tickingFutureReadyPredicate;
	}

	/**
	 * Регистрирует планировщик чанка и подписывает его на уведомления о новых тиках.
	 *
	 * @param pos       позиция чанка
	 * @param scheduler планировщик тиков для данного чанка
	 */
	public void addChunkTickScheduler(ChunkPos pos, ChunkTickScheduler<T> scheduler) {
		long chunkKey = pos.toLong();
		chunkSchedulers.put(chunkKey, scheduler);

		OrderedTick<T> nextTick = scheduler.peekNextTick();

		if (nextTick != null) {
			nextTriggerTickByChunk.put(chunkKey, nextTick.triggerTick());
		}

		scheduler.setTickConsumer(queuedTickConsumer);
	}

	/**
	 * Удаляет планировщик чанка и отписывает его от уведомлений.
	 *
	 * @param pos позиция чанка
	 */
	public void removeChunkTickScheduler(ChunkPos pos) {
		long chunkKey = pos.toLong();
		ChunkTickScheduler<T> scheduler = chunkSchedulers.remove(chunkKey);
		nextTriggerTickByChunk.remove(chunkKey);

		if (scheduler != null) {
			scheduler.setTickConsumer(null);
		}
	}

	@Override
	public void scheduleTick(OrderedTick<T> orderedTick) {
		long chunkKey = ChunkPos.toLong(orderedTick.pos());
		ChunkTickScheduler<T> scheduler = chunkSchedulers.get(chunkKey);

		if (scheduler == null) {
			Util.logErrorOrPause("Trying to schedule tick in not loaded position " + orderedTick.pos());
			return;
		}

		scheduler.scheduleTick(orderedTick);
	}

	/**
	 * Выполняет все тики, готовые к срабатыванию в данный игровой тик.
	 *
	 * @param time     текущее игровое время в тиках
	 * @param maxTicks максимальное количество тиков за один вызов
	 * @param ticker   обработчик тика: принимает позицию и тип объекта
	 */
	public void tick(long time, int maxTicks, BiConsumer<BlockPos, T> ticker) {
		Profiler profiler = Profilers.get();
		profiler.push("collect");
		collectTickableTicks(time, maxTicks, profiler);
		profiler.swap("run");
		profiler.visit("ticksToRun", tickableTicks.size());
		executeTicks(ticker);
		profiler.swap("cleanup");
		clearTickState();
		profiler.pop();
	}

	private void collectTickableTicks(long time, int maxTicks, Profiler profiler) {
		collectTickableChunkSchedulers(time);
		profiler.visit("containersToTick", tickableChunkSchedulers.size());
		addTickableTicks(time, maxTicks);
		returnDelayedChunks();
	}

	private void collectTickableChunkSchedulers(long time) {
		ObjectIterator<Entry> iterator = Long2LongMaps.fastIterator(nextTriggerTickByChunk);

		while (iterator.hasNext()) {
			Entry entry = iterator.next();
			long chunkKey = entry.getLongKey();
			long triggerTick = entry.getLongValue();

			if (triggerTick > time) {
				continue;
			}

			ChunkTickScheduler<T> scheduler = chunkSchedulers.get(chunkKey);

			if (scheduler == null) {
				iterator.remove();
				continue;
			}

			OrderedTick<T> nextTick = scheduler.peekNextTick();

			if (nextTick == null) {
				iterator.remove();
				continue;
			}

			if (nextTick.triggerTick() > time) {
				entry.setValue(nextTick.triggerTick());
				continue;
			}

			if (tickingFutureReadyPredicate.test(chunkKey)) {
				iterator.remove();
				tickableChunkSchedulers.add(scheduler);
			}
		}
	}

	private void addTickableTicks(long time, int maxTicks) {
		ChunkTickScheduler<T> scheduler;

		while (isUnderTickLimit(maxTicks) && (scheduler = tickableChunkSchedulers.poll()) != null) {
			OrderedTick<T> firstTick = scheduler.pollNextTick();
			addTickableTick(firstTick);
			drainTicksFromChunk(tickableChunkSchedulers, scheduler, time, maxTicks);

			OrderedTick<T> nextTick = scheduler.peekNextTick();

			if (nextTick != null) {
				if (nextTick.triggerTick() <= time && isUnderTickLimit(maxTicks)) {
					tickableChunkSchedulers.add(scheduler);
				} else {
					scheduleNextTrigger(nextTick);
				}
			}
		}
	}

	private void returnDelayedChunks() {
		for (ChunkTickScheduler<T> scheduler : tickableChunkSchedulers) {
			scheduleNextTrigger(scheduler.peekNextTick());
		}
	}

	private void scheduleNextTrigger(OrderedTick<T> tick) {
		nextTriggerTickByChunk.put(ChunkPos.toLong(tick.pos()), tick.triggerTick());
	}

	/**
	 * Извлекает из чанка все тики, которые должны выполниться раньше первого тика
	 * следующего чанка в очереди, соблюдая лимит.
	 */
	private void drainTicksFromChunk(
		Queue<ChunkTickScheduler<T>> queue,
		ChunkTickScheduler<T> scheduler,
		long time,
		int maxTicks
	) {
		if (!isUnderTickLimit(maxTicks)) {
			return;
		}

		ChunkTickScheduler<T> nextChunk = queue.peek();
		OrderedTick<T> nextChunkFirstTick = nextChunk != null ? nextChunk.peekNextTick() : null;

		while (isUnderTickLimit(maxTicks)) {
			OrderedTick<T> candidate = scheduler.peekNextTick();

			if (candidate == null || candidate.triggerTick() > time) {
				break;
			}

			if (nextChunkFirstTick != null && OrderedTick.BASIC_COMPARATOR.compare(candidate, nextChunkFirstTick) > 0) {
				break;
			}

			scheduler.pollNextTick();
			addTickableTick(candidate);
		}
	}

	private void addTickableTick(OrderedTick<T> tick) {
		tickableTicks.add(tick);
	}

	private boolean isUnderTickLimit(int maxTicks) {
		return tickableTicks.size() < maxTicks;
	}

	private void executeTicks(BiConsumer<BlockPos, T> ticker) {
		while (!tickableTicks.isEmpty()) {
			OrderedTick<T> tick = tickableTicks.poll();

			if (!tickingSet.isEmpty()) {
				tickingSet.remove(tick);
			}

			tickedTicks.add(tick);
			ticker.accept(tick.pos(), tick.type());
		}
	}

	private void clearTickState() {
		tickableTicks.clear();
		tickableChunkSchedulers.clear();
		tickedTicks.clear();
		tickingSet.clear();
	}

	@Override
	public boolean isQueued(BlockPos pos, T type) {
		ChunkTickScheduler<T> scheduler = chunkSchedulers.get(ChunkPos.toLong(pos));
		return scheduler != null && scheduler.isQueued(pos, type);
	}

	@Override
	public boolean isTicking(BlockPos pos, T type) {
		ensureTickingSetPopulated();
		return tickingSet.contains(OrderedTick.create(type, pos));
	}

	private void ensureTickingSetPopulated() {
		if (tickingSet.isEmpty() && !tickableTicks.isEmpty()) {
			tickingSet.addAll(tickableTicks);
		}
	}

	private void visitChunks(BlockBox box, ChunkVisitor<T> visitor) {
		int minChunkX = ChunkSectionPos.getSectionCoord((double) box.getMinX());
		int minChunkZ = ChunkSectionPos.getSectionCoord((double) box.getMinZ());
		int maxChunkX = ChunkSectionPos.getSectionCoord((double) box.getMaxX());
		int maxChunkZ = ChunkSectionPos.getSectionCoord((double) box.getMaxZ());

		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				long chunkKey = ChunkPos.toLong(chunkX, chunkZ);
				ChunkTickScheduler<T> scheduler = chunkSchedulers.get(chunkKey);

				if (scheduler != null) {
					visitor.accept(chunkKey, scheduler);
				}
			}
		}
	}

	/**
	 * Удаляет все запланированные тики в заданной области блоков.
	 * Используется при взрывах и других операциях, изменяющих блоки.
	 *
	 * @param box область блоков для очистки тиков
	 */
	public void clearNextTicks(BlockBox box) {
		Predicate<OrderedTick<T>> inBox = tick -> box.contains(tick.pos());

		visitChunks(box, (chunkKey, scheduler) -> {
			OrderedTick<T> before = scheduler.peekNextTick();
			scheduler.removeTicksIf(inBox);
			OrderedTick<T> after = scheduler.peekNextTick();

			if (after != before) {
				if (after != null) {
					scheduleNextTrigger(after);
				} else {
					nextTriggerTickByChunk.remove(chunkKey);
				}
			}
		});

		tickedTicks.removeIf(inBox);
		tickableTicks.removeIf(inBox);
	}

	/**
	 * Копирует тики из заданной области в этот же планировщик со смещением позиций.
	 * Используется при вставке структур.
	 *
	 * @param box    область блоков для копирования тиков
	 * @param offset смещение позиций
	 */
	public void scheduleTicks(BlockBox box, Vec3i offset) {
		scheduleTicks(this, box, offset);
	}

	/**
	 * Копирует тики из другого планировщика в заданной области в этот планировщик со смещением.
	 * Порядковые номера тиков нормализуются относительно минимального значения в выборке.
	 *
	 * @param source исходный планировщик
	 * @param box    область блоков для копирования тиков
	 * @param offset смещение позиций
	 */
	public void scheduleTicks(WorldTickScheduler<T> source, BlockBox box, Vec3i offset) {
		List<OrderedTick<T>> ticks = new ArrayList<>();
		Predicate<OrderedTick<T>> inBox = tick -> box.contains(tick.pos());

		source.tickedTicks.stream().filter(inBox).forEach(ticks::add);
		source.tickableTicks.stream().filter(inBox).forEach(ticks::add);
		source.visitChunks(
			box,
			(chunkKey, scheduler) -> scheduler.getQueueAsStream().filter(inBox).forEach(ticks::add)
		);

		LongSummaryStatistics stats = ticks.stream().mapToLong(OrderedTick::subTickOrder).summaryStatistics();
		long minOrder = stats.getMin();
		long maxOrder = stats.getMax();

		ticks.forEach(tick -> scheduleTick(
			new OrderedTick<>(
				tick.type(),
				tick.pos().add(offset),
				tick.triggerTick(),
				tick.priority(),
				tick.subTickOrder() - minOrder + maxOrder + 1L
			)
		));
	}

	@Override
	public int getTickCount() {
		return chunkSchedulers.values().stream().mapToInt(TickScheduler::getTickCount).sum();
	}

	@FunctionalInterface
	interface ChunkVisitor<T> {

		void accept(long chunkKey, ChunkTickScheduler<T> scheduler);
	}
}
