package net.minecraft.world.chunk;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.function.Finishable;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Описывает один шаг пайплайна генерации чанка: целевой статус, зависимости от соседних
 * чанков, радиус записи блоков и задачу генерации. Является иммутабельным record-ом.
 */
public record ChunkGenerationStep(
		ChunkStatus targetStatus,
		GenerationDependencies directDependencies,
		GenerationDependencies accumulatedDependencies,
		int blockStateWriteRadius,
		GenerationTask task
) {

	public int getAdditionalLevel(ChunkStatus status) {
		return status == targetStatus ? 0 : accumulatedDependencies.getAdditionalLevel(status);
	}

	/**
	 * Запускает задачу генерации для чанка. Если чанк ещё не достиг целевого статуса,
	 * оборачивает выполнение в JFR-профилировщик и обновляет статус после завершения.
	 */
	public CompletableFuture<Chunk> run(
			ChunkGenerationContext context,
			BoundedRegionArray<AbstractChunkHolder> chunks,
			Chunk chunk
	) {
		if (chunk.getStatus().isEarlierThan(targetStatus)) {
			Finishable profiling = FlightProfiler.INSTANCE.startChunkGenerationProfiling(
					chunk.getPos(),
					context.world().getRegistryKey(),
					targetStatus.getId()
			);
			return task.doWork(context, this, chunks, chunk)
					.thenApply(generated -> finalizeGeneration(generated, profiling));
		}

		return task.doWork(context, this, chunks, chunk);
	}

	private Chunk finalizeGeneration(Chunk chunk, @Nullable Finishable finishCallback) {
		if (chunk instanceof ProtoChunk protoChunk && protoChunk.getStatus().isEarlierThan(targetStatus)) {
			protoChunk.setStatus(targetStatus);
		}

		if (finishCallback != null) {
			finishCallback.finish(true);
		}

		return chunk;
	}

	public static class Builder {

		private final ChunkStatus targetStatus;
		private final @Nullable ChunkGenerationStep previousStep;
		private ChunkStatus[] directDependencies;
		private int blockStateWriteRadius = -1;
		private GenerationTask task = ChunkGenerating::noop;

		protected Builder(ChunkStatus targetStatus) {
			if (targetStatus.getPrevious() != targetStatus) {
				throw new IllegalArgumentException("Not starting with the first status: " + targetStatus);
			}

			this.targetStatus = targetStatus;
			previousStep = null;
			directDependencies = new ChunkStatus[0];
		}

		protected Builder(ChunkStatus targetStatus, ChunkGenerationStep previousStep) {
			if (previousStep.targetStatus.getIndex() != targetStatus.getIndex() - 1) {
				throw new IllegalArgumentException("Out of order status: " + targetStatus);
			}

			this.targetStatus = targetStatus;
			this.previousStep = previousStep;
			directDependencies = new ChunkStatus[]{previousStep.targetStatus};
		}

		/**
		 * Добавляет зависимость: все чанки в радиусе {@code level} должны достичь
		 * статуса {@code status} перед запуском этого шага.
		 */
		public ChunkGenerationStep.Builder dependsOn(ChunkStatus status, int level) {
			if (status.isAtLeast(targetStatus)) {
				throw new IllegalArgumentException("Status " + status + " can not be required by " + targetStatus);
			}

			ChunkStatus[] current = directDependencies;
			int newSize = level + 1;

			if (newSize > current.length) {
				directDependencies = new ChunkStatus[newSize];
				Arrays.fill(directDependencies, status);
			}

			for (int i = 0; i < Math.min(newSize, current.length); i++) {
				directDependencies[i] = ChunkStatus.max(current[i], status);
			}

			return this;
		}

		public ChunkGenerationStep.Builder blockStateWriteRadius(int blockStateWriteRadius) {
			this.blockStateWriteRadius = blockStateWriteRadius;
			return this;
		}

		public ChunkGenerationStep.Builder task(GenerationTask task) {
			this.task = task;
			return this;
		}

		public ChunkGenerationStep build() {
			return new ChunkGenerationStep(
					targetStatus,
					new GenerationDependencies(ImmutableList.copyOf(directDependencies)),
					new GenerationDependencies(ImmutableList.copyOf(accumulateDependencies())),
					blockStateWriteRadius,
					task
			);
		}

		/**
		 * Объединяет прямые зависимости текущего шага с накопленными зависимостями
		 * предыдущего шага, выбирая максимальный статус для каждого уровня.
		 */
		private ChunkStatus[] accumulateDependencies() {
			if (previousStep == null) {
				return directDependencies;
			}

			int parentOffset = getParentStatus(previousStep.targetStatus);
			GenerationDependencies parentDeps = previousStep.accumulatedDependencies;
			ChunkStatus[] result = new ChunkStatus[Math.max(parentOffset + parentDeps.size(), directDependencies.length)];

			for (int i = 0; i < result.length; i++) {
				int parentIndex = i - parentOffset;
				boolean hasParent = parentIndex >= 0 && parentIndex < parentDeps.size();
				boolean hasDirect = i < directDependencies.length;

				if (!hasParent) {
					result[i] = directDependencies[i];
				} else if (!hasDirect) {
					result[i] = parentDeps.get(parentIndex);
				} else {
					result[i] = ChunkStatus.max(directDependencies[i], parentDeps.get(parentIndex));
				}
			}

			return result;
		}

		private int getParentStatus(ChunkStatus status) {
			for (int i = directDependencies.length - 1; i >= 0; i--) {
				if (directDependencies[i].isAtLeast(status)) {
					return i;
				}
			}

			return 0;
		}
	}
}
