package net.minecraft.world.chunk;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ChunkLevels;
import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkLoadingManager;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Базовый держатель чанка, управляющий набором {@link CompletableFuture} для каждого
 * статуса загрузки. Обеспечивает атомарное продвижение статуса, ленивое создание
 * загрузчика и корректную выгрузку диапазона статусов при понижении уровня.
 */
public abstract class AbstractChunkHolder {

	private static final List<ChunkStatus> STATUSES = ChunkStatus.createOrderedList();
	private static final OptionalChunk<Chunk> NOT_DONE = OptionalChunk.of("Not done yet");

	public static final OptionalChunk<Chunk> UNLOADED = OptionalChunk.of("Unloaded chunk");
	public static final CompletableFuture<OptionalChunk<Chunk>> UNLOADED_FUTURE = CompletableFuture.completedFuture(UNLOADED);

	protected final ChunkPos pos;

	private volatile @Nullable ChunkStatus status;
	private final AtomicReference<@Nullable ChunkStatus> currentStatus = new AtomicReference<>();
	private final AtomicReferenceArray<@Nullable CompletableFuture<OptionalChunk<Chunk>>> chunkFuturesByStatus =
			new AtomicReferenceArray<>(STATUSES.size());
	private final AtomicReference<@Nullable ChunkLoader> chunkLoader = new AtomicReference<>();
	private final AtomicInteger refCount = new AtomicInteger();
	private volatile CompletableFuture<Void> referenceFuture = CompletableFuture.completedFuture(null);

	public AbstractChunkHolder(ChunkPos pos) {
		this.pos = pos;
		if (!pos.isWithinGenerationArea()) {
			throw new IllegalStateException("Trying to create chunk out of reasonable bounds: " + pos);
		}
	}

	/**
	 * Возвращает или создаёт {@link CompletableFuture} для указанного статуса загрузки,
	 * при необходимости запуская новый {@link ChunkLoader}.
	 */
	public CompletableFuture<OptionalChunk<Chunk>> load(
			ChunkStatus requestedStatus,
			ServerChunkLoadingManager chunkLoadingManager
	) {
		if (cannotBeLoaded(requestedStatus)) {
			return UNLOADED_FUTURE;
		}

		CompletableFuture<OptionalChunk<Chunk>> future = getOrCreateFuture(requestedStatus);
		if (future.isDone()) {
			return future;
		}

		ChunkLoader loader = chunkLoader.get();
		if (loader == null || requestedStatus.isLaterThan(loader.targetStatus)) {
			createLoader(chunkLoadingManager, requestedStatus);
		}

		return future;
	}

	/**
	 * Запускает шаг генерации чанка, атомарно продвигая {@link #currentStatus}.
	 * При ошибке генерации устанавливает глобальное исключение сервера через
	 * {@link MinecraftServer#setWorldGenException}.
	 */
	CompletableFuture<OptionalChunk<Chunk>> generate(
			ChunkGenerationStep step,
			ChunkLoadingManager chunkLoadingManager,
			BoundedRegionArray<AbstractChunkHolder> chunks
	) {
		if (cannotBeLoaded(step.targetStatus())) {
			return UNLOADED_FUTURE;
		}

		if (!progressStatus(step.targetStatus())) {
			return getOrCreateFuture(step.targetStatus());
		}

		return chunkLoadingManager.generate(this, step, chunks)
				.handle((chunk, throwable) -> {
					if (throwable != null) {
						CrashReport crashReport = CrashReport.create(throwable, "Exception chunk generation/loading");
						MinecraftServer.setWorldGenException(new CrashException(crashReport));
					} else {
						completeChunkFuture(step.targetStatus(), chunk);
					}

					return OptionalChunk.of(chunk);
				});
	}

	/**
	 * Пересчитывает целевой статус на основе текущего уровня и при понижении
	 * выгружает диапазон статусов от нового до старого.
	 */
	public void updateStatus(ServerChunkLoadingManager chunkLoadingManager) {
		ChunkStatus oldStatus = status;
		ChunkStatus newStatus = ChunkLevels.getStatus(getLevel());
		status = newStatus;

		boolean isDowngrading = oldStatus != null && (newStatus == null || newStatus.isEarlierThan(oldStatus));
		if (!isDowngrading) {
			return;
		}

		unload(newStatus, oldStatus);
		if (chunkLoader.get() != null) {
			createLoader(chunkLoadingManager, getMaxPendingStatus(newStatus));
		}
	}

	/**
	 * Заменяет все {@link ProtoChunk}-фьючерсы (кроме последнего) на завершённый
	 * фьючерс с переданным {@link WrapperProtoChunk}. Используется при финализации
	 * генерации чанка.
	 */
	public void replaceWith(WrapperProtoChunk chunk) {
		CompletableFuture<OptionalChunk<Chunk>> wrappedFuture = CompletableFuture.completedFuture(OptionalChunk.of(chunk));

		for (int i = 0; i < chunkFuturesByStatus.length() - 1; i++) {
			CompletableFuture<OptionalChunk<Chunk>> existing = chunkFuturesByStatus.get(i);
			Objects.requireNonNull(existing);

			Chunk current = existing.getNow(NOT_DONE).orElse(null);
			if (!(current instanceof ProtoChunk)) {
				throw new IllegalStateException("Trying to replace a ProtoChunk, but found " + current);
			}

			if (!chunkFuturesByStatus.compareAndSet(i, existing, wrappedFuture)) {
				throw new IllegalStateException("Future changed by other thread while trying to replace it");
			}
		}
	}

	void clearLoader(ChunkLoader loader) {
		chunkLoader.compareAndSet(loader, null);
	}

	private void createLoader(ServerChunkLoadingManager chunkLoadingManager, @Nullable ChunkStatus requestedStatus) {
		ChunkLoader newLoader = requestedStatus != null
				? chunkLoadingManager.createLoader(requestedStatus, getPos())
				: null;

		ChunkLoader oldLoader = chunkLoader.getAndSet(newLoader);
		if (oldLoader != null) {
			oldLoader.markPendingDisposal();
		}
	}

	private CompletableFuture<OptionalChunk<Chunk>> getOrCreateFuture(ChunkStatus status) {
		if (cannotBeLoaded(status)) {
			return UNLOADED_FUTURE;
		}

		int index = status.getIndex();
		CompletableFuture<OptionalChunk<Chunk>> existing = chunkFuturesByStatus.get(index);

		while (existing == null) {
			CompletableFuture<OptionalChunk<Chunk>> newFuture = new CompletableFuture<>();
			existing = chunkFuturesByStatus.compareAndExchange(index, null, newFuture);

			if (existing == null) {
				if (cannotBeLoaded(status)) {
					unload(index, newFuture);
					return UNLOADED_FUTURE;
				}

				return newFuture;
			}
		}

		return existing;
	}

	private void unload(@Nullable ChunkStatus from, ChunkStatus to) {
		int startIndex = from == null ? 0 : from.getIndex() + 1;
		int endIndex = to.getIndex();

		for (int i = startIndex; i <= endIndex; i++) {
			CompletableFuture<OptionalChunk<Chunk>> future = chunkFuturesByStatus.get(i);
			if (future != null) {
				unload(i, future);
			}
		}
	}

	private void unload(int statusIndex, CompletableFuture<OptionalChunk<Chunk>> previousFuture) {
		if (previousFuture.complete(UNLOADED) && !chunkFuturesByStatus.compareAndSet(statusIndex, previousFuture, null)) {
			throw new IllegalStateException("Nothing else should replace the future here");
		}
	}

	private void completeChunkFuture(ChunkStatus status, Chunk chunk) {
		OptionalChunk<Chunk> result = OptionalChunk.of(chunk);
		int index = status.getIndex();

		while (true) {
			CompletableFuture<OptionalChunk<Chunk>> future = chunkFuturesByStatus.get(index);

			if (future == null) {
				if (chunkFuturesByStatus.compareAndSet(index, null, CompletableFuture.completedFuture(result))) {
					return;
				}
			} else {
				if (future.complete(result)) {
					return;
				}

				if (future.getNow(NOT_DONE).isPresent()) {
					throw new IllegalStateException(
							"Trying to complete a future but found it to be completed successfully already");
				}

				Thread.yield();
			}
		}
	}

	private @Nullable ChunkStatus getMaxPendingStatus(@Nullable ChunkStatus upperBound) {
		if (upperBound == null) {
			return null;
		}

		ChunkStatus candidate = upperBound;

		for (ChunkStatus reached = currentStatus.get();
		     reached == null || candidate.isLaterThan(reached);
		     candidate = candidate.getPrevious()
		) {
			if (chunkFuturesByStatus.get(candidate.getIndex()) != null) {
				return candidate;
			}

			if (candidate == ChunkStatus.EMPTY) {
				break;
			}
		}

		return null;
	}

	private boolean progressStatus(ChunkStatus nextStatus) {
		ChunkStatus expectedPrevious = nextStatus == ChunkStatus.EMPTY ? null : nextStatus.getPrevious();
		ChunkStatus actual = currentStatus.compareAndExchange(expectedPrevious, nextStatus);

		if (actual == expectedPrevious) {
			return true;
		}

		if (actual != null && !nextStatus.isLaterThan(actual)) {
			return false;
		}

		throw new IllegalStateException(
				"Unexpected last startedWork status: " + actual + " while trying to start: " + nextStatus);
	}

	private boolean cannotBeLoaded(ChunkStatus status) {
		ChunkStatus allowed = this.status;
		return allowed == null || status.isLaterThan(allowed);
	}

	protected abstract void combineSavingFuture(CompletableFuture<?> savingFuture);

	public void incrementRefCount() {
		if (refCount.getAndIncrement() == 0) {
			referenceFuture = new CompletableFuture<>();
			combineSavingFuture(referenceFuture);
		}
	}

	public void decrementRefCount() {
		CompletableFuture<Void> future = referenceFuture;
		int remaining = refCount.decrementAndGet();

		if (remaining == 0) {
			future.complete(null);
		}

		if (remaining < 0) {
			throw new IllegalStateException("More releases than claims. Count: " + remaining);
		}
	}

	public @Nullable Chunk getUncheckedOrNull(ChunkStatus requestedStatus) {
		CompletableFuture<OptionalChunk<Chunk>> future = chunkFuturesByStatus.get(requestedStatus.getIndex());
		return future == null ? null : future.getNow(NOT_DONE).orElse(null);
	}

	public @Nullable Chunk getOrNull(ChunkStatus requestedStatus) {
		return cannotBeLoaded(requestedStatus) ? null : getUncheckedOrNull(requestedStatus);
	}

	public @Nullable Chunk getLatest() {
		ChunkStatus reached = currentStatus.get();
		if (reached == null) {
			return null;
		}

		Chunk chunk = getUncheckedOrNull(reached);
		return chunk != null ? chunk : getUncheckedOrNull(reached.getPrevious());
	}

	public @Nullable ChunkStatus getActualStatus() {
		CompletableFuture<OptionalChunk<Chunk>> future = chunkFuturesByStatus.get(ChunkStatus.EMPTY.getIndex());
		Chunk chunk = future == null ? null : future.getNow(NOT_DONE).orElse(null);
		return chunk == null ? null : chunk.getStatus();
	}

	public ChunkPos getPos() {
		return pos;
	}

	public ChunkLevelType getLevelType() {
		return ChunkLevels.getType(getLevel());
	}

	public abstract int getLevel();

	public abstract int getCompletedLevel();

	/**
	 * Возвращает список пар (статус → фьючерс) для всех известных статусов.
	 * Используется исключительно в отладочных целях.
	 */
	@Debug
	public List<Pair<ChunkStatus, @Nullable CompletableFuture<OptionalChunk<Chunk>>>> enumerateFutures() {
		List<Pair<ChunkStatus, CompletableFuture<OptionalChunk<Chunk>>>> list = new ArrayList<>();

		for (int i = 0; i < STATUSES.size(); i++) {
			list.add(Pair.of(STATUSES.get(i), chunkFuturesByStatus.get(i)));
		}

		return list;
	}

	@Debug
	public @Nullable ChunkStatus getLatestStatus() {
		ChunkStatus reached = currentStatus.get();
		if (reached == null) {
			return null;
		}

		Chunk chunk = getUncheckedOrNull(reached);
		return chunk != null ? reached : reached.getPrevious();
	}
}
