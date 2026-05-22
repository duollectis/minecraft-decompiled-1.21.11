package net.minecraft.client.render.chunk;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.VertexSorter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.*;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.profiler.ScopedProfiler;
import net.minecraft.util.thread.NameableExecutor;
import net.minecraft.util.thread.SimpleConsecutiveExecutor;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Управляет асинхронной компиляцией и загрузкой секций чанков на GPU.
 * Поддерживает очередь задач перестройки ({@link BuiltChunk.RebuildTask}) и сортировки
 * прозрачных вершин ({@link BuiltChunk.SortTask}), выполняемых в пуле потоков.
 * Загрузка скомпилированных буферов на GPU происходит в главном потоке через {@link #upload()}.
 */
@Environment(EnvType.CLIENT)
public class ChunkBuilder {

	private final ChunkRenderTaskScheduler scheduler = new ChunkRenderTaskScheduler();
	private final Queue<Runnable> uploadQueue = Queues.newConcurrentLinkedQueue();
	final Executor uploadExecutor = this.uploadQueue::add;
	final Queue<AbstractChunkRenderData> renderQueue = Queues.newConcurrentLinkedQueue();
	final BlockBufferAllocatorStorage buffers;
	private final BlockBufferBuilderPool buffersPool;
	volatile boolean stopped;
	private final SimpleConsecutiveExecutor consecutiveExecutor;
	private final NameableExecutor executor;
	ClientWorld world;
	final WorldRenderer worldRenderer;
	Vec3d cameraPosition = Vec3d.ZERO;
	final SectionBuilder sectionBuilder;

	public ChunkBuilder(
			ClientWorld world,
			WorldRenderer worldRenderer,
			NameableExecutor executor,
			BufferBuilderStorage bufferBuilderStorage,
			BlockRenderManager blockRenderManager,
			BlockEntityRenderManager blockEntityRenderDispatcher
	) {
		this.world = world;
		this.worldRenderer = worldRenderer;
		this.buffers = bufferBuilderStorage.getBlockBufferBuilders();
		this.buffersPool = bufferBuilderStorage.getBlockBufferBuildersPool();
		this.executor = executor;
		this.consecutiveExecutor = new SimpleConsecutiveExecutor(executor, "Section Renderer");
		this.consecutiveExecutor.send(this::scheduleRunTasks);
		this.sectionBuilder = new SectionBuilder(blockRenderManager, blockEntityRenderDispatcher);
	}

	public void setWorld(ClientWorld world) {
		this.world = world;
	}

	private void scheduleRunTasks() {
		if (!this.stopped && !this.buffersPool.hasNoAvailableBuilder()) {
			ChunkBuilder.BuiltChunk.Task task = this.scheduler.dequeueNearest(this.cameraPosition);
			if (task != null) {
				BlockBufferAllocatorStorage
						blockBufferAllocatorStorage =
						Objects.requireNonNull(this.buffersPool.acquire());
				CompletableFuture.<CompletableFuture<ChunkBuilder.Result>>supplyAsync(
						                 () -> task.run(blockBufferAllocatorStorage), this.executor.named(task.getName())
				                 )
				                 .thenCompose(future -> (CompletionStage<ChunkBuilder.Result>) future)
				                 .whenComplete((result, throwable) -> {
					                 if (throwable != null) {
						                 MinecraftClient
								                 .getInstance()
								                 .setCrashReportSupplierAndAddDetails(CrashReport.create(
										                 throwable,
										                 "Batching sections"
								                 ));
					                 }
					                 else {
						                 task.finished.set(true);
						                 this.consecutiveExecutor.send(() -> {
							                 if (result == ChunkBuilder.Result.SUCCESSFUL) {
								                 blockBufferAllocatorStorage.clear();
							                 }
							                 else {
								                 blockBufferAllocatorStorage.reset();
							                 }

							                 this.buffersPool.release(blockBufferAllocatorStorage);
							                 this.scheduleRunTasks();
						                 });
					                 }
				                 });
			}
		}
	}

	public void setCameraPosition(Vec3d cameraPosition) {
		this.cameraPosition = cameraPosition;
	}

	/**
	 * Выполняет все накопленные задачи загрузки буферов на GPU и освобождает устаревшие данные рендеринга.
	 * Должен вызываться строго в главном потоке рендеринга.
	 */
	public void upload() {
		Runnable task;
		while ((task = uploadQueue.poll()) != null) {
			task.run();
		}

		AbstractChunkRenderData staleData;
		while ((staleData = renderQueue.poll()) != null) {
			staleData.close();
		}
	}

	public void rebuild(ChunkBuilder.BuiltChunk chunk, ChunkRendererRegionBuilder builder) {
		chunk.rebuild(builder);
	}

	public void send(ChunkBuilder.BuiltChunk.Task task) {
		if (stopped) {
			return;
		}

		consecutiveExecutor.send(() -> {
			if (stopped) {
				return;
			}

			scheduler.enqueue(task);
			scheduleRunTasks();
		});
	}

	public void cancelAllTasks() {
		scheduler.cancelAll();
	}

	public boolean isEmpty() {
		return scheduler.size() == 0 && uploadQueue.isEmpty();
	}

	public void stop() {
		stopped = true;
		cancelAllTasks();
		upload();
	}

	@Debug
	public String getDebugString() {
		return String.format(
				Locale.ROOT,
				"pC: %03d, pU: %02d, aB: %02d",
				this.scheduler.size(),
				this.uploadQueue.size(),
				this.buffersPool.getAvailableBuilderCount()
		);
	}

	@Debug
	public int getScheduledTaskCount() {
		return this.scheduler.size();
	}

	@Debug
	public int getChunksToUpload() {
		return this.uploadQueue.size();
	}

	@Debug
	public int getFreeBufferCount() {
		return this.buffersPool.getAvailableBuilderCount();
	}

	/**
	 * Представляет одну скомпилированную секцию чанка (16×16×16 блоков).
	 * Хранит текущие данные рендеринга, управляет задачами перестройки и сортировки,
	 * а также отслеживает прогресс плавного появления секции (fade-in) после загрузки на GPU.
	 */
	@Environment(EnvType.CLIENT)
	public class BuiltChunk {

		public static final int CHUNK_SIZE = 16;
		public final int index;
		public final AtomicReference<AbstractChunkRenderData>
				currentRenderData =
				new AtomicReference<>(ChunkRenderData.HIDDEN);
		private ChunkBuilder.BuiltChunk.@Nullable RebuildTask rebuildTask;
		private ChunkBuilder.BuiltChunk.@Nullable SortTask sortTask;
		private Box boundingBox;
		private boolean needsRebuild = true;
		volatile long sectionPos = ChunkSectionPos.asLong(-1, -1, -1);
		final BlockPos.Mutable origin = new BlockPos.Mutable(-1, -1, -1);
		private boolean needsImportantRebuild;
		private long uploadStartTimeMs;
		private long fadeInDurationMs;
		private boolean fullyUploaded;

		public BuiltChunk(final int index, final long sectionPos) {
			this.index = index;
			this.setSectionPos(sectionPos);
		}

		public float getFadeInProgress(long currentTimeMs) {
			if (uploadStartTimeMs == 0L) {
				return 0.0F;
			}

			long elapsed = currentTimeMs - uploadStartTimeMs;

			return fadeInDurationMs == 0L || elapsed >= fadeInDurationMs
			       ? 1.0F
			       : (float) elapsed / (float) fadeInDurationMs;
		}

		public void setFadeInDuration(long l) {
			this.fadeInDurationMs = l;
		}

		public void setFullyUploaded(boolean bl) {
			this.fullyUploaded = bl;
		}

		public boolean isFullyUploaded() {
			return this.fullyUploaded;
		}

		private boolean isChunkNonEmpty(long sectionPos) {
			Chunk
					chunk =
					ChunkBuilder.this.world.getChunk(
							ChunkSectionPos.unpackX(sectionPos),
							ChunkSectionPos.unpackZ(sectionPos),
							ChunkStatus.FULL,
							false
					);
			return chunk != null && ChunkBuilder.this.world
					.getLightingProvider()
					.isLightingEnabled(ChunkSectionPos.withZeroY(sectionPos));
		}

		/**
		 * Проверяет, загружены ли все 8 соседних чанков (включая диагональные) и включено ли освещение.
		 * Секция не должна компилироваться, пока хотя бы один сосед ещё не готов — иначе
		 * на границах чанков появятся артефакты освещения.
		 *
		 * @return {@code true}, если все соседние чанки загружены и освещение активно
		 */
		public boolean shouldBuild() {
			return this.isChunkNonEmpty(ChunkSectionPos.offset(this.sectionPos, Direction.WEST))
					&& this.isChunkNonEmpty(ChunkSectionPos.offset(this.sectionPos, Direction.NORTH))
					&& this.isChunkNonEmpty(ChunkSectionPos.offset(this.sectionPos, Direction.EAST))
					&& this.isChunkNonEmpty(ChunkSectionPos.offset(this.sectionPos, Direction.SOUTH))
					&& this.isChunkNonEmpty(ChunkSectionPos.offset(this.sectionPos, -1, 0, -1))
					&& this.isChunkNonEmpty(ChunkSectionPos.offset(this.sectionPos, -1, 0, 1))
					&& this.isChunkNonEmpty(ChunkSectionPos.offset(this.sectionPos, 1, 0, -1))
					&& this.isChunkNonEmpty(ChunkSectionPos.offset(this.sectionPos, 1, 0, 1));
		}

		public Box getBoundingBox() {
			return this.boundingBox;
		}

		public CompletableFuture<Void> uploadLayer(
				Map<BlockRenderLayer, BuiltBuffer> buffersByLayer,
				ChunkRenderData renderData
		) {
			if (ChunkBuilder.this.stopped) {
				buffersByLayer.values().forEach(BuiltBuffer::close);
				return CompletableFuture.completedFuture(null);
			}
			else {
				return CompletableFuture.runAsync(
						() -> buffersByLayer.forEach((layer, buffer) -> {
							try (ScopedProfiler scopedProfiler = Profilers.get().scoped("Upload Section Layer")) {
								renderData.upload(layer, buffer, this.sectionPos);
								buffer.close();
							}

							if (this.uploadStartTimeMs == 0L) {
								this.uploadStartTimeMs = Util.getMeasuringTimeMs();
							}
						}), ChunkBuilder.this.uploadExecutor
				);
			}
		}

		public CompletableFuture<Void> uploadIndices(
				ChunkRenderData data,
				BufferAllocator.CloseableBuffer buffer,
				BlockRenderLayer layer
		) {
			if (ChunkBuilder.this.stopped) {
				buffer.close();
				return CompletableFuture.completedFuture(null);
			}
			else {
				return CompletableFuture.runAsync(
						() -> {
							try (ScopedProfiler scopedProfiler = Profilers.get().scoped("Upload Section Indices")) {
								data.uploadIndexBuffer(layer, buffer, this.sectionPos);
								buffer.close();
							}
						}, ChunkBuilder.this.uploadExecutor
				);
			}
		}

		public void setSectionPos(long sectionPos) {
			clear();
			this.sectionPos = sectionPos;
			int blockX = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackX(sectionPos));
			int blockY = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackY(sectionPos));
			int blockZ = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackZ(sectionPos));
			origin.set(blockX, blockY, blockZ);
			boundingBox = new Box(blockX, blockY, blockZ, blockX + CHUNK_SIZE, blockY + CHUNK_SIZE, blockZ + CHUNK_SIZE);
		}

		public AbstractChunkRenderData getCurrentRenderData() {
			return this.currentRenderData.get();
		}

		public void clear() {
			cancel();
			currentRenderData.getAndSet(ChunkRenderData.HIDDEN).close();
			needsRebuild = true;
			uploadStartTimeMs = 0L;
			fullyUploaded = false;
		}

		public BlockPos getOrigin() {
			return this.origin;
		}

		public long getSectionPos() {
			return this.sectionPos;
		}

		public void scheduleRebuild(boolean important) {
			boolean wasScheduled = needsRebuild;
			needsRebuild = true;
			needsImportantRebuild = important | (wasScheduled && needsImportantRebuild);
		}

		public void cancelRebuild() {
			needsRebuild = false;
			needsImportantRebuild = false;
		}

		public boolean needsRebuild() {
			return needsRebuild;
		}

		public boolean needsImportantRebuild() {
			return needsRebuild && needsImportantRebuild;
		}

		public long getOffsetSectionPos(Direction direction) {
			return ChunkSectionPos.offset(this.sectionPos, direction);
		}

		public void scheduleSort(ChunkBuilder builder) {
			if (this.getCurrentRenderData() instanceof ChunkRenderData chunkRenderData) {
				this.sortTask = new ChunkBuilder.BuiltChunk.SortTask(chunkRenderData);
				builder.send(this.sortTask);
			}
		}

		public boolean hasTranslucentLayer() {
			return this.getCurrentRenderData().hasTranslucentLayers();
		}

		public boolean isCurrentlySorting() {
			return this.sortTask != null && !this.sortTask.finished.get();
		}

		protected void cancel() {
			if (rebuildTask != null) {
				rebuildTask.cancel();
				rebuildTask = null;
			}

			if (sortTask != null) {
				sortTask.cancel();
				sortTask = null;
			}
		}

		public ChunkBuilder.BuiltChunk.Task createRebuildTask(ChunkRendererRegionBuilder builder) {
			this.cancel();
			ChunkRendererRegion chunkRendererRegion = builder.build(ChunkBuilder.this.world, this.sectionPos);
			boolean bl = this.currentRenderData.get() != ChunkRenderData.HIDDEN;
			this.rebuildTask = new ChunkBuilder.BuiltChunk.RebuildTask(chunkRendererRegion, bl);
			return this.rebuildTask;
		}

		public void scheduleRebuild(ChunkRendererRegionBuilder builder) {
			ChunkBuilder.BuiltChunk.Task task = createRebuildTask(builder);
			ChunkBuilder.this.send(task);
		}

		public void rebuild(ChunkRendererRegionBuilder builder) {
			ChunkBuilder.BuiltChunk.Task task = createRebuildTask(builder);
			task.run(ChunkBuilder.this.buffers);
		}

		void setCurrentRenderData(AbstractChunkRenderData data) {
			AbstractChunkRenderData abstractChunkRenderData = this.currentRenderData.getAndSet(data);
			ChunkBuilder.this.renderQueue.add(abstractChunkRenderData);
			ChunkBuilder.this.worldRenderer.addBuiltChunk(this);
		}

		VertexSorter getVertexSorter(ChunkSectionPos sectionPos) {
			Vec3d vec3d = ChunkBuilder.this.cameraPosition;
			return VertexSorter.byDistance(
					(float) (vec3d.x - sectionPos.getMinX()),
					(float) (vec3d.y - sectionPos.getMinY()),
					(float) (vec3d.z - sectionPos.getMinZ())
			);
		}

		/** Задача полной перестройки геометрии секции чанка в фоновом потоке. */
		@Environment(EnvType.CLIENT)
		class RebuildTask extends ChunkBuilder.BuiltChunk.Task {

			protected final ChunkRendererRegion region;

			public RebuildTask(final ChunkRendererRegion region, final boolean prioritized) {
				super(prioritized);
				this.region = region;
			}

			@Override
			protected String getName() {
				return "rend_chk_rebuild";
			}

			@Override
			public CompletableFuture<ChunkBuilder.Result> run(BlockBufferAllocatorStorage buffers) {
				if (this.cancelled.get()) {
					return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
				}
				else {
					long l = BuiltChunk.this.sectionPos;
					ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(l);
					if (this.cancelled.get()) {
						return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
					}
					else {
						SectionBuilder.RenderData renderData;
						try (ScopedProfiler scopedProfiler = Profilers.get().scoped("Compile Section")) {
							renderData = ChunkBuilder.this.sectionBuilder
									.build(
											chunkSectionPos,
											this.region,
											BuiltChunk.this.getVertexSorter(chunkSectionPos),
											buffers
									);
						}

						NormalizedRelativePos
								normalizedRelativePos =
								NormalizedRelativePos.of(ChunkBuilder.this.cameraPosition, l);
						if (this.cancelled.get()) {
							renderData.close();
							return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
						}
						else {
							ChunkRenderData chunkRenderData = new ChunkRenderData(normalizedRelativePos, renderData);
							CompletableFuture<Void>
									completableFuture =
									BuiltChunk.this.uploadLayer(renderData.buffers, chunkRenderData);
							return completableFuture.handle((void_, throwable) -> {
								if (throwable != null && !(throwable instanceof CancellationException)
										&& !(throwable instanceof InterruptedException)) {
									MinecraftClient
											.getInstance()
											.setCrashReportSupplierAndAddDetails(CrashReport.create(
													throwable,
													"Rendering section"
											));
								}

								if (!this.cancelled.get() && !ChunkBuilder.this.stopped) {
									BuiltChunk.this.setCurrentRenderData(chunkRenderData);
									return ChunkBuilder.Result.SUCCESSFUL;
								}
								else {
									ChunkBuilder.this.renderQueue.add(chunkRenderData);
									return ChunkBuilder.Result.CANCELLED;
								}
							});
						}
					}
				}
			}

			@Override
			public void cancel() {
				if (this.cancelled.compareAndSet(false, true)) {
					BuiltChunk.this.scheduleRebuild(false);
				}
			}
		}

		/** Задача пересортировки прозрачных вершин секции без полной перестройки геометрии. */
		@Environment(EnvType.CLIENT)
		class SortTask extends ChunkBuilder.BuiltChunk.Task {

			private final ChunkRenderData renderData;

			public SortTask(final ChunkRenderData data) {
				super(true);
				this.renderData = data;
			}

			@Override
			protected String getName() {
				return "rend_chk_sort";
			}

			@Override
			public CompletableFuture<ChunkBuilder.Result> run(BlockBufferAllocatorStorage buffers) {
				if (this.cancelled.get()) {
					return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
				}
				else {
					BuiltBuffer.SortState sortState = this.renderData.getTranslucencySortingData();
					if (sortState != null && this.renderData.containsLayer(BlockRenderLayer.TRANSLUCENT)) {
						long l = BuiltChunk.this.sectionPos;
						VertexSorter vertexSorter = BuiltChunk.this.getVertexSorter(ChunkSectionPos.from(l));
						NormalizedRelativePos
								normalizedRelativePos =
								NormalizedRelativePos.of(ChunkBuilder.this.cameraPosition, l);
						if (!this.renderData.hasPosition(normalizedRelativePos)
								&& !normalizedRelativePos.isOnCameraAxis()) {
							return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
						}
						else {
							BufferAllocator.CloseableBuffer
									closeableBuffer =
									sortState.sortAndStore(buffers.get(BlockRenderLayer.TRANSLUCENT), vertexSorter);
							if (closeableBuffer == null) {
								return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
							}
							else if (this.cancelled.get()) {
								closeableBuffer.close();
								return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
							}
							else {
								CompletableFuture<Void> completableFuture = BuiltChunk.this.uploadIndices(
										this.renderData, closeableBuffer, BlockRenderLayer.TRANSLUCENT
								);
								return completableFuture.handle((void_, throwable) -> {
									if (throwable != null && !(throwable instanceof CancellationException)
											&& !(throwable instanceof InterruptedException)) {
										MinecraftClient
												.getInstance()
												.setCrashReportSupplierAndAddDetails(CrashReport.create(
														throwable,
														"Rendering section"
												));
									}

									if (this.cancelled.get()) {
										return ChunkBuilder.Result.CANCELLED;
									}
									else {
										this.renderData.setPos(normalizedRelativePos);
										return ChunkBuilder.Result.SUCCESSFUL;
									}
								});
							}
						}
					}
					else {
						return CompletableFuture.completedFuture(ChunkBuilder.Result.CANCELLED);
					}
				}
			}

			@Override
			public void cancel() {
				this.cancelled.set(true);
			}
		}

		/** Базовый класс задачи рендеринга секции, выполняемой в фоновом потоке. */
		@Environment(EnvType.CLIENT)
		public abstract class Task {

			protected final AtomicBoolean cancelled = new AtomicBoolean(false);
			protected final AtomicBoolean finished = new AtomicBoolean(false);
			protected final boolean prioritized;

			public Task(final boolean prioritized) {
				this.prioritized = prioritized;
			}

			public abstract CompletableFuture<ChunkBuilder.Result> run(BlockBufferAllocatorStorage buffers);

			public abstract void cancel();

			protected abstract String getName();

			public boolean isPrioritized() {
				return prioritized;
			}

			public BlockPos getOrigin() {
				return BuiltChunk.this.origin;
			}
		}
	}

	/** Результат выполнения задачи рендеринга секции. */
	@Environment(EnvType.CLIENT)
	enum Result {
		SUCCESSFUL,
		CANCELLED;
	}
}
