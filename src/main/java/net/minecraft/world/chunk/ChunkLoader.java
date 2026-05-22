package net.minecraft.world.chunk;

import net.minecraft.server.world.OptionalChunk;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.ScopedProfiler;
import net.minecraft.world.ChunkLoadingManager;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Управляет последовательной загрузкой чанка через все статусы генерации
 * вплоть до {@link #targetStatus}. Поддерживает отложенную выгрузку через
 * {@link #markPendingDisposal()} и освобождает захваченные соседние чанки
 * при завершении работы.
 */
public class ChunkLoader {

	private final ChunkLoadingManager chunkLoadingManager;
	private final ChunkPos pos;
	private @Nullable ChunkStatus currentlyLoadingStatus = null;
	public final ChunkStatus targetStatus;
	private volatile boolean pendingDisposal;
	private final List<CompletableFuture<OptionalChunk<Chunk>>> futures = new ArrayList<>();
	private final BoundedRegionArray<AbstractChunkHolder> chunks;
	private boolean allowGeneration;

	private ChunkLoader(
			ChunkLoadingManager chunkLoadingManager,
			ChunkStatus targetStatus,
			ChunkPos pos,
			BoundedRegionArray<AbstractChunkHolder> chunks
	) {
		this.chunkLoadingManager = chunkLoadingManager;
		this.targetStatus = targetStatus;
		this.pos = pos;
		this.chunks = chunks;
	}

	/**
	 * Создаёт загрузчик для указанного чанка, захватывая все необходимые соседние
	 * чанки в радиусе, определённом зависимостями шага генерации {@code EMPTY}.
	 */
	public static ChunkLoader create(ChunkLoadingManager chunkLoadingManager, ChunkStatus targetStatus, ChunkPos pos) {
		int radius = ChunkGenerationSteps.GENERATION.get(targetStatus).getAdditionalLevel(ChunkStatus.EMPTY);
		BoundedRegionArray<AbstractChunkHolder> region = BoundedRegionArray.create(
				pos.x, pos.z, radius, (x, z) -> chunkLoadingManager.acquire(ChunkPos.toLong(x, z))
		);
		return new ChunkLoader(chunkLoadingManager, targetStatus, pos, region);
	}

	/**
	 * Продвигает загрузку на один шаг: возвращает незавершённый фьючерс если он есть,
	 * либо запускает следующий статус. Возвращает {@code null} когда загрузка завершена.
	 */
	public @Nullable CompletableFuture<?> run() {
		while (true) {
			CompletableFuture<?> pending = getLatestPendingFuture();
			if (pending != null) {
				return pending;
			}

			if (pendingDisposal || currentlyLoadingStatus == targetStatus) {
				dispose();
				return null;
			}

			loadNextStatus();
		}
	}

	private void loadNextStatus() {
		ChunkStatus nextStatus;
		if (currentlyLoadingStatus == null) {
			nextStatus = ChunkStatus.EMPTY;
		} else if (!allowGeneration && currentlyLoadingStatus == ChunkStatus.EMPTY && !isGenerationUnnecessary()) {
			allowGeneration = true;
			nextStatus = ChunkStatus.EMPTY;
		} else {
			nextStatus = ChunkStatus.createOrderedList().get(currentlyLoadingStatus.getIndex() + 1);
		}

		loadAll(nextStatus, allowGeneration);
		currentlyLoadingStatus = nextStatus;
	}

	public void markPendingDisposal() {
		pendingDisposal = true;
	}

	private void dispose() {
		chunks.get(pos.x, pos.z).clearLoader(this);
		chunks.forEach(chunkLoadingManager::release);
	}

	/**
	 * Проверяет, можно ли пропустить генерацию и загрузить чанк только из диска.
	 * Возвращает {@code true} если все соседние чанки уже достигли требуемых статусов.
	 */
	private boolean isGenerationUnnecessary() {
		if (targetStatus == ChunkStatus.EMPTY) {
			return true;
		}

		ChunkStatus actualStatus = chunks.get(pos.x, pos.z).getActualStatus();
		if (actualStatus == null || actualStatus.isEarlierThan(targetStatus)) {
			return false;
		}

		GenerationDependencies dependencies = ChunkGenerationSteps.LOADING.get(targetStatus).accumulatedDependencies();
		int maxRadius = dependencies.getMaxLevel();

		for (int cx = pos.x - maxRadius; cx <= pos.x + maxRadius; cx++) {
			for (int cz = pos.z - maxRadius; cz <= pos.z + maxRadius; cz++) {
				int distance = pos.getChebyshevDistance(cx, cz);
				ChunkStatus required = dependencies.get(distance);
				ChunkStatus neighborStatus = chunks.get(cx, cz).getActualStatus();
				if (neighborStatus == null || neighborStatus.isEarlierThan(required)) {
					return false;
				}
			}
		}

		return true;
	}

	public AbstractChunkHolder getHolder() {
		return chunks.get(pos.x, pos.z);
	}

	private void loadAll(ChunkStatus targetStatus, boolean allowGeneration) {
		try (ScopedProfiler profiler = Profilers.get().scoped("scheduleLayer")) {
			profiler.addLabel(targetStatus::getId);
			int radius = getAdditionalLevel(targetStatus, allowGeneration);

			for (int cx = pos.x - radius; cx <= pos.x + radius; cx++) {
				for (int cz = pos.z - radius; cz <= pos.z + radius; cz++) {
					AbstractChunkHolder holder = chunks.get(cx, cz);
					if (pendingDisposal || !load(targetStatus, allowGeneration, holder)) {
						return;
					}
				}
			}
		}
	}

	private int getAdditionalLevel(ChunkStatus status, boolean generate) {
		ChunkGenerationSteps steps = generate ? ChunkGenerationSteps.GENERATION : ChunkGenerationSteps.LOADING;
		return steps.get(targetStatus).getAdditionalLevel(status);
	}

	private boolean load(ChunkStatus targetStatus, boolean allowGeneration, AbstractChunkHolder chunkHolder) {
		ChunkStatus actualStatus = chunkHolder.getActualStatus();
		boolean needsGeneration = actualStatus != null && targetStatus.isLaterThan(actualStatus);
		ChunkGenerationSteps steps = needsGeneration ? ChunkGenerationSteps.GENERATION : ChunkGenerationSteps.LOADING;

		if (needsGeneration && !allowGeneration) {
			throw new IllegalStateException("Can't load chunk, but didn't expect to need to generate");
		}

		CompletableFuture<OptionalChunk<Chunk>> future = chunkHolder.generate(
				steps.get(targetStatus), chunkLoadingManager, chunks
		);
		OptionalChunk<Chunk> result = future.getNow(null);

		if (result == null) {
			futures.add(future);
			return true;
		}

		if (result.isPresent()) {
			return true;
		}

		markPendingDisposal();
		return false;
	}

	private @Nullable CompletableFuture<?> getLatestPendingFuture() {
		while (!futures.isEmpty()) {
			CompletableFuture<OptionalChunk<Chunk>> future = futures.getLast();
			OptionalChunk<Chunk> result = future.getNow(null);

			if (result == null) {
				return future;
			}

			futures.removeLast();
			if (!result.isPresent()) {
				markPendingDisposal();
			}
		}

		return null;
	}
}
