package net.minecraft.client.render;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.chunk.AbstractChunkRenderData;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.Octree;
import net.minecraft.server.network.ChunkFilter;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.*;
import net.minecraft.world.HeightLimitView;
import org.joml.Vector3d;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Подготавливает данные рендеринга чанков: управляет графом видимости секций,
 * планирует обновления terrain и распространяет окклюзию между соседними чанками.
 */
@Environment(EnvType.CLIENT)
public class ChunkRenderingDataPreparer {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Direction[] DIRECTIONS = Direction.values();
	private static final int DEFAULT_SECTION_DISTANCE = 60;
	private static final int SECTION_DISTANCE = ChunkSectionPos.getSectionCoord(60);
	private static final double CHUNK_INNER_DIAGONAL_LENGTH = Math.ceil(Math.sqrt(3.0) * 16.0);
	private boolean terrainUpdateScheduled = true;
	private @Nullable Future<?> terrainUpdateFuture;
	private @Nullable BuiltChunkStorage builtChunkStorage;
	private final AtomicReference<ChunkRenderingDataPreparer.@Nullable PreparerState> state = new AtomicReference<>();
	private final AtomicReference<ChunkRenderingDataPreparer.@Nullable Events> events = new AtomicReference<>();
	private final AtomicBoolean needsUpdate = new AtomicBoolean(false);

	/**
	 * Устанавливает хранилище построенных чанков и сбрасывает состояние подготовщика.
	 */
	public void setStorage(@Nullable BuiltChunkStorage storage) {
		if (this.terrainUpdateFuture != null) {
			try {
				this.terrainUpdateFuture.get();
				this.terrainUpdateFuture = null;
			}
			catch (Exception var3) {
				LOGGER.warn("Full update failed", var3);
			}
		}

		this.builtChunkStorage = storage;
		if (storage != null) {
			this.state.set(new ChunkRenderingDataPreparer.PreparerState(storage));
			this.scheduleTerrainUpdate();
		}
		else {
			this.state.set(null);
		}
	}

	/**
	 * Помечает terrain как требующий полного пересчёта при следующем кадре.
	 */
	public void scheduleTerrainUpdate() {
		this.terrainUpdateScheduled = true;
	}

	/**
	 * Собирает видимые чанки из октодерева в переданные списки.
	 */
	public void collectChunks(
			Frustum frustum,
			List<ChunkBuilder.BuiltChunk> builtChunks,
			List<ChunkBuilder.BuiltChunk> nearbyChunks
	) {
		this.state.get().storage().octree.visit(
				(node, skipVisibilityCheck, depth, nearCenter) -> {
					ChunkBuilder.BuiltChunk builtChunk = node.getBuiltChunk();
					if (builtChunk != null) {
						builtChunks.add(builtChunk);
						if (nearCenter) {
							nearbyChunks.add(builtChunk);
						}
					}
				}, frustum, 32
		);
	}

	/**
	 * Сбрасывает флаг необходимости обновления frustum и возвращает его предыдущее значение.
	 */
	public boolean updateFrustum() {
		return this.needsUpdate.compareAndSet(true, false);
	}

	/**
	 * Уведомляет активные события о появлении новых соседей у чанка.
	 */
	public void addNeighbors(ChunkPos chunkPos) {
		ChunkRenderingDataPreparer.Events currentEvents = this.events.get();
		if (currentEvents != null) {
			this.addNeighbors(currentEvents, chunkPos);
		}

		ChunkRenderingDataPreparer.Events events2 = this.state.get().events;
		if (events2 != currentEvents) {
			this.addNeighbors(events2, chunkPos);
		}
	}

	/**
	 * Планирует распространение видимости от указанного построенного чанка.
	 */
	public void schedulePropagationFrom(ChunkBuilder.BuiltChunk builtChunk) {
		ChunkRenderingDataPreparer.Events currentEvents = this.events.get();
		if (currentEvents != null) {
			currentEvents.sectionsToPropagateFrom.add(builtChunk);
		}

		ChunkRenderingDataPreparer.Events events2 = this.state.get().events;
		if (events2 != currentEvents) {
			events2.sectionsToPropagateFrom.add(builtChunk);
		}
	}

	/**
	 * Обновляет граф окклюзии секций: при необходимости запускает полный пересчёт terrain
	 * и применяет инкрементальные изменения от событий.
	 */
	public void updateSectionOcclusionGraph(
			boolean cullChunks,
			Camera camera,
			Frustum frustum,
			List<ChunkBuilder.BuiltChunk> builtChunk,
			LongOpenHashSet activeSections
	) {
		Vec3d vec3d = camera.getCameraPos();
		if (this.terrainUpdateScheduled && (this.terrainUpdateFuture == null || this.terrainUpdateFuture.isDone())) {
			this.updateTerrain(cullChunks, camera, vec3d, activeSections);
		}

		this.updateNow(cullChunks, frustum, builtChunk, vec3d, activeSections);
	}

	/**
	 * Асинхронно пересчитывает полный граф видимости terrain.
	 */
	private void updateTerrain(boolean cullChunks, Camera camera, Vec3d cameraPos, LongOpenHashSet activeSections) {
		this.terrainUpdateScheduled = false;
		LongOpenHashSet longOpenHashSet = activeSections.clone();
		this.terrainUpdateFuture = CompletableFuture.runAsync(
				() -> {
					ChunkRenderingDataPreparer.PreparerState
							preparerState =
							new ChunkRenderingDataPreparer.PreparerState(this.builtChunkStorage);
					this.events.set(preparerState.events);
					Queue<ChunkRenderingDataPreparer.ChunkInfo> traversalQueue = Queues.newArrayDeque();
					this.scheduleLater(camera, traversalQueue);
					traversalQueue.forEach(info -> preparerState.storage.infoList.setInfo(info.chunk, info));
					this.update(
							preparerState.storage,
							cameraPos,
							traversalQueue,
							cullChunks,
							builtChunk -> {},
							longOpenHashSet
					);
					this.state.set(preparerState);
					this.events.set(null);
					this.needsUpdate.set(true);
				}, Util.getMainWorkerExecutor()
		);
	}

	/**
	 * Синхронно применяет накопленные события к текущему состоянию графа видимости.
	 */
	private void updateNow(
			boolean cullChunks,
			Frustum frustum,
			List<ChunkBuilder.BuiltChunk> builtChunks,
			Vec3d cameraPos,
			LongOpenHashSet activeSections
	) {
		ChunkRenderingDataPreparer.PreparerState preparerState = this.state.get();
		this.scheduleNew(preparerState);
		if (!preparerState.events.sectionsToPropagateFrom.isEmpty()) {
			Queue<ChunkRenderingDataPreparer.ChunkInfo> propagationQueue = Queues.newArrayDeque();

			while (!preparerState.events.sectionsToPropagateFrom.isEmpty()) {
				ChunkBuilder.BuiltChunk builtChunk = preparerState.events.sectionsToPropagateFrom.poll();
				ChunkRenderingDataPreparer.ChunkInfo chunkInfo = preparerState.storage.infoList.getInfo(builtChunk);
				if (chunkInfo != null && chunkInfo.chunk == builtChunk) {
					propagationQueue.add(chunkInfo);
				}
			}

			Frustum frustum2 = WorldRenderer.offsetFrustum(frustum);
			Consumer<ChunkBuilder.BuiltChunk> consumer = builtChunk -> {
				if (frustum2.isVisible(builtChunk.getBoundingBox())) {
					this.needsUpdate.set(true);
				}
			};
			this.update(preparerState.storage, cameraPos, propagationQueue, cullChunks, consumer, activeSections);
		}
	}

	/**
	 * Переносит чанки, получившие соседей, в очередь распространения.
	 */
	private void scheduleNew(ChunkRenderingDataPreparer.PreparerState preparerState) {
		LongIterator longIterator = preparerState.events.chunksWhichReceivedNeighbors.iterator();

		while (longIterator.hasNext()) {
			long l = longIterator.nextLong();
			List<ChunkBuilder.BuiltChunk> list = (List<ChunkBuilder.BuiltChunk>) preparerState.storage.queue.get(l);
			if (list != null && list.get(0).shouldBuild()) {
				preparerState.events.sectionsToPropagateFrom.addAll(list);
				preparerState.storage.queue.remove(l);
			}
		}

		preparerState.events.chunksWhichReceivedNeighbors.clear();
	}

	/**
	 * Добавляет все 8 соседних позиций чанка в набор получивших соседей.
	 */
	private void addNeighbors(ChunkRenderingDataPreparer.Events events, ChunkPos chunkPos) {
		events.chunksWhichReceivedNeighbors.add(ChunkPos.toLong(chunkPos.x - 1, chunkPos.z));
		events.chunksWhichReceivedNeighbors.add(ChunkPos.toLong(chunkPos.x, chunkPos.z - 1));
		events.chunksWhichReceivedNeighbors.add(ChunkPos.toLong(chunkPos.x + 1, chunkPos.z));
		events.chunksWhichReceivedNeighbors.add(ChunkPos.toLong(chunkPos.x, chunkPos.z + 1));
		events.chunksWhichReceivedNeighbors.add(ChunkPos.toLong(chunkPos.x - 1, chunkPos.z - 1));
		events.chunksWhichReceivedNeighbors.add(ChunkPos.toLong(chunkPos.x - 1, chunkPos.z + 1));
		events.chunksWhichReceivedNeighbors.add(ChunkPos.toLong(chunkPos.x + 1, chunkPos.z - 1));
		events.chunksWhichReceivedNeighbors.add(ChunkPos.toLong(chunkPos.x + 1, chunkPos.z + 1));
	}

	/**
	 * Заполняет очередь начальными чанками для обхода графа видимости,
	 * начиная с позиции камеры.
	 */
	private void scheduleLater(Camera camera, Queue<ChunkRenderingDataPreparer.ChunkInfo> queue) {
		BlockPos blockPos = camera.getBlockPos();
		long l = ChunkSectionPos.toLong(blockPos);
		int i = ChunkSectionPos.unpackY(l);
		ChunkBuilder.BuiltChunk builtChunk = this.builtChunkStorage.getRenderedChunk(l);
		if (builtChunk == null) {
			HeightLimitView heightLimitView = this.builtChunkStorage.getWorld();
			boolean bl = i < heightLimitView.getBottomSectionCoord();
			int j = bl ? heightLimitView.getBottomSectionCoord() : heightLimitView.getTopSectionCoord();
			int k = this.builtChunkStorage.getViewDistance();
			List<ChunkRenderingDataPreparer.ChunkInfo> list = Lists.newArrayList();
			int m = ChunkSectionPos.unpackX(l);
			int n = ChunkSectionPos.unpackZ(l);

			for (int o = -k; o <= k; o++) {
				for (int p = -k; p <= k; p++) {
					ChunkBuilder.BuiltChunk
							builtChunk2 =
							this.builtChunkStorage.getRenderedChunk(ChunkSectionPos.asLong(o + m, j, p + n));
					if (builtChunk2 != null && this.isWithinViewDistance(l, builtChunk2.getSectionPos())) {
						Direction startDirection = bl ? Direction.UP : Direction.DOWN;
						ChunkRenderingDataPreparer.ChunkInfo
								chunkInfo =
								new ChunkRenderingDataPreparer.ChunkInfo(builtChunk2, startDirection, 0);
						chunkInfo.updateCullingState(chunkInfo.cullingState, startDirection);
						if (o > 0) {
							chunkInfo.updateCullingState(chunkInfo.cullingState, Direction.EAST);
						}
						else if (o < 0) {
							chunkInfo.updateCullingState(chunkInfo.cullingState, Direction.WEST);
						}

						if (p > 0) {
							chunkInfo.updateCullingState(chunkInfo.cullingState, Direction.SOUTH);
						}
						else if (p < 0) {
							chunkInfo.updateCullingState(chunkInfo.cullingState, Direction.NORTH);
						}

						list.add(chunkInfo);
					}
				}
			}

			list.sort(Comparator.comparingDouble(chunkInfox -> blockPos.getSquaredDistance(ChunkSectionPos
					.from(chunkInfox.chunk.getSectionPos())
					.getCenterPos())));
			queue.addAll(list);
		}
		else {
			queue.add(new ChunkRenderingDataPreparer.ChunkInfo(builtChunk, null, 0));
		}
	}

	/**
	 * Обходит граф видимости в ширину, добавляя видимые чанки в октодерево
	 * и планируя построение невидимых.
	 */
	private void update(
			ChunkRenderingDataPreparer.RenderableChunks renderableChunks,
			Vec3d pos,
			Queue<ChunkRenderingDataPreparer.ChunkInfo> queue,
			boolean cullChunks,
			Consumer<ChunkBuilder.BuiltChunk> consumer,
			LongOpenHashSet longOpenHashSet
	) {
		ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(pos);
		long l = chunkSectionPos.asLong();
		BlockPos blockPos = chunkSectionPos.getCenterPos();

		while (!queue.isEmpty()) {
			ChunkRenderingDataPreparer.ChunkInfo chunkInfo = queue.poll();
			ChunkBuilder.BuiltChunk builtChunk = chunkInfo.chunk;
			if (!longOpenHashSet.contains(chunkInfo.chunk.getSectionPos())) {
				if (renderableChunks.octree.add(chunkInfo.chunk)) {
					consumer.accept(chunkInfo.chunk);
				}
			}
			else {
				chunkInfo.chunk.currentRenderData.compareAndSet(ChunkRenderData.HIDDEN, ChunkRenderData.READY);
			}

			long m = builtChunk.getSectionPos();
			boolean bl = Math.abs(ChunkSectionPos.unpackX(m) - chunkSectionPos.getSectionX()) > SECTION_DISTANCE
					|| Math.abs(ChunkSectionPos.unpackY(m) - chunkSectionPos.getSectionY()) > SECTION_DISTANCE
					|| Math.abs(ChunkSectionPos.unpackZ(m) - chunkSectionPos.getSectionZ()) > SECTION_DISTANCE;

			for (Direction neighborDirection : DIRECTIONS) {
				ChunkBuilder.BuiltChunk builtChunk2 = this.getRenderedChunk(l, builtChunk, neighborDirection);
				if (builtChunk2 != null && (!cullChunks || !chunkInfo.canCull(neighborDirection.getOpposite()))) {
					if (cullChunks && chunkInfo.hasAnyDirection()) {
						AbstractChunkRenderData abstractChunkRenderData = builtChunk.getCurrentRenderData();
						boolean bl2 = false;

						for (int i = 0; i < DIRECTIONS.length; i++) {
							if (chunkInfo.hasDirection(i) && abstractChunkRenderData.isVisibleThrough(
									DIRECTIONS[i].getOpposite(),
									neighborDirection
							)) {
								bl2 = true;
								break;
							}
						}

						if (!bl2) {
							continue;
						}
					}

					if (cullChunks && bl) {
						int j = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackX(m));
						int k = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackY(m));
						int ix = ChunkSectionPos.getBlockCoord(ChunkSectionPos.unpackZ(m));
						boolean
								bl3 =
								neighborDirection.getAxis() == Direction.Axis.X ? blockPos.getX() > j
								                                                : blockPos.getX() < j;
						boolean
								bl4 =
								neighborDirection.getAxis() == Direction.Axis.Y ? blockPos.getY() > k
								                                                : blockPos.getY() < k;
						boolean
								bl5 =
								neighborDirection.getAxis() == Direction.Axis.Z ? blockPos.getZ() > ix
								                                                : blockPos.getZ() < ix;
						Vector3d vector3d = new Vector3d(j + (bl3 ? 16 : 0), k + (bl4 ? 16 : 0), ix + (bl5 ? 16 : 0));
						Vector3d
								vector3d2 =
								new Vector3d(pos.x, pos.y, pos.z)
										.sub(vector3d)
										.normalize()
										.mul(CHUNK_INNER_DIAGONAL_LENGTH);
						boolean bl6 = true;

						while (vector3d.distanceSquared(pos.x, pos.y, pos.z) > 3600.0) {
							vector3d.add(vector3d2);
							HeightLimitView heightLimitView = this.builtChunkStorage.getWorld();
							if (vector3d.y > heightLimitView.getTopYInclusive()
									|| vector3d.y < heightLimitView.getBottomY()) {
								break;
							}

							ChunkBuilder.BuiltChunk
									builtChunk3 =
									this.builtChunkStorage.getRenderedChunk(BlockPos.ofFloored(
											vector3d.x,
											vector3d.y,
											vector3d.z
									));
							if (builtChunk3 == null || renderableChunks.infoList.getInfo(builtChunk3) == null) {
								bl6 = false;
								break;
							}
						}

						if (!bl6) {
							continue;
						}
					}

					ChunkRenderingDataPreparer.ChunkInfo chunkInfo2 = renderableChunks.infoList.getInfo(builtChunk2);
					if (chunkInfo2 != null) {
						chunkInfo2.addDirection(neighborDirection);
					}
					else {
						ChunkRenderingDataPreparer.ChunkInfo chunkInfo3 = new ChunkRenderingDataPreparer.ChunkInfo(
								builtChunk2, neighborDirection, chunkInfo.propagationLevel + 1
						);
						chunkInfo3.updateCullingState(chunkInfo.cullingState, neighborDirection);
						if (builtChunk2.shouldBuild()) {
							queue.add(chunkInfo3);
							renderableChunks.infoList.setInfo(builtChunk2, chunkInfo3);
						}
						else if (this.isWithinViewDistance(l, builtChunk2.getSectionPos())) {
							renderableChunks.infoList.setInfo(builtChunk2, chunkInfo3);
							long n = ChunkSectionPos.toChunkPos(builtChunk2.getSectionPos());
							((List) renderableChunks.queue.computeIfAbsent(n, lx -> new ArrayList())).add(builtChunk2);
						}
					}
				}
			}
		}
	}

	/**
	 * Проверяет, находится ли секция в пределах дистанции прорисовки.
	 */
	private boolean isWithinViewDistance(long centerSectionPos, long otherSectionPos) {
		return ChunkFilter.isWithinDistanceExcludingEdge(
				ChunkSectionPos.unpackX(centerSectionPos),
				ChunkSectionPos.unpackZ(centerSectionPos),
				this.builtChunkStorage.getViewDistance(),
				ChunkSectionPos.unpackX(otherSectionPos),
				ChunkSectionPos.unpackZ(otherSectionPos)
		);
	}

	/**
	 * Возвращает соседний построенный чанк в указанном направлении или {@code null}, если он вне дистанции.
	 */
	private ChunkBuilder.@Nullable BuiltChunk getRenderedChunk(
			long sectionPos,
			ChunkBuilder.BuiltChunk chunk,
			Direction direction
	) {
		long l = chunk.getOffsetSectionPos(direction);
		if (!this.isWithinViewDistance(sectionPos, l)) {
			return null;
		}
		else {
			return MathHelper.abs(ChunkSectionPos.unpackY(sectionPos) - ChunkSectionPos.unpackY(l))
					       > this.builtChunkStorage.getViewDistance()
			       ? null
			       : this.builtChunkStorage.getRenderedChunk(l);
		}
	}

	@Debug
	public ChunkRenderingDataPreparer.@Nullable ChunkInfo getInfo(ChunkBuilder.BuiltChunk chunk) {
		return this.state.get().storage.infoList.getInfo(chunk);
	}

	public Octree getOctree() {
		return this.state.get().storage.octree;
	}

	/**
	 * Хранит информацию о чанке в графе видимости: направление прихода,
	 * маску отсечения и уровень распространения.
	 */
	@Environment(EnvType.CLIENT)
	@Debug
	public static class ChunkInfo {

		@Debug
		protected final ChunkBuilder.BuiltChunk chunk;
		private byte direction;
		byte cullingState;
		@Debug
		public final int propagationLevel;

		ChunkInfo(ChunkBuilder.BuiltChunk builtChunk, @Nullable Direction fromDirection, int level) {
			this.chunk = builtChunk;
			if (fromDirection != null) {
				this.addDirection(fromDirection);
			}

			this.propagationLevel = level;
		}

		/**
		 * Обновляет маску отсечения, добавляя направление прихода.
		 */
		void updateCullingState(byte parentCullingState, Direction from) {
			this.cullingState = (byte) (this.cullingState | parentCullingState | 1 << from.ordinal());
		}

		/**
		 * Проверяет, можно ли отсечь чанк со стороны указанного направления.
		 */
		boolean canCull(Direction from) {
			return (this.cullingState & 1 << from.ordinal()) > 0;
		}

		/**
		 * Добавляет направление в битовую маску посещённых направлений.
		 */
		void addDirection(Direction direction) {
			this.direction = (byte) (this.direction | this.direction | 1 << direction.ordinal());
		}

		/**
		 * Проверяет, было ли посещено направление с данным порядковым номером.
		 */
		@Debug
		public boolean hasDirection(int ordinal) {
			return (this.direction & 1 << ordinal) > 0;
		}

		/**
		 * Возвращает {@code true}, если чанк был достигнут хотя бы из одного направления.
		 */
		boolean hasAnyDirection() {
			return this.direction != 0;
		}

		@Override
		public int hashCode() {
			return Long.hashCode(this.chunk.getSectionPos());
		}

		@Override
		public boolean equals(Object o) {
			return !(o instanceof ChunkRenderingDataPreparer.ChunkInfo chunkInfo) ? false : this.chunk.getSectionPos()
			                                                                                == chunkInfo.chunk.getSectionPos();
		}
	}

	/**
	 * Индексированный массив для быстрого поиска {@link ChunkInfo} по индексу чанка.
	 */
	@Environment(EnvType.CLIENT)
	static class ChunkInfoList {

		private final ChunkRenderingDataPreparer.ChunkInfo[] current;

		ChunkInfoList(int size) {
			this.current = new ChunkRenderingDataPreparer.ChunkInfo[size];
		}

		/**
		 * Сохраняет информацию о чанке по его индексу.
		 */
		public void setInfo(ChunkBuilder.BuiltChunk chunk, ChunkRenderingDataPreparer.ChunkInfo info) {
			this.current[chunk.index] = info;
		}

		/**
		 * Возвращает информацию о чанке или {@code null}, если индекс вне диапазона.
		 */
		public ChunkRenderingDataPreparer.@Nullable ChunkInfo getInfo(ChunkBuilder.BuiltChunk chunk) {
			int i = chunk.index;
			return i >= 0 && i < this.current.length ? this.current[i] : null;
		}
	}

	/**
	 * Накапливает события между кадрами: чанки, получившие соседей,
	 * и секции, от которых нужно распространить видимость.
	 */
	@Environment(EnvType.CLIENT)
	record Events(
			LongSet chunksWhichReceivedNeighbors,
			BlockingQueue<ChunkBuilder.BuiltChunk> sectionsToPropagateFrom
	) {

		Events() {
			this(new LongOpenHashSet(), new LinkedBlockingQueue<>());
		}
	}

	/**
	 * Снимок состояния подготовщика: хранилище видимых чанков и текущие события.
	 */
	@Environment(EnvType.CLIENT)
	record PreparerState(
			ChunkRenderingDataPreparer.RenderableChunks storage,
			ChunkRenderingDataPreparer.Events events
	) {

		PreparerState(BuiltChunkStorage storage) {
			this(new ChunkRenderingDataPreparer.RenderableChunks(storage), new ChunkRenderingDataPreparer.Events());
		}
	}

	/**
	 * Хранит структуры данных для рендеримых чанков: октодерево, список информации
	 * и очередь ожидающих построения чанков.
	 */
	@Environment(EnvType.CLIENT)
	static class RenderableChunks {

		public final ChunkRenderingDataPreparer.ChunkInfoList infoList;
		public final Octree octree;
		public final Long2ObjectMap<List<ChunkBuilder.BuiltChunk>> queue;

		public RenderableChunks(BuiltChunkStorage storage) {
			this.infoList = new ChunkRenderingDataPreparer.ChunkInfoList(storage.chunks.length);
			this.octree =
					new Octree(
							storage.getSectionPos(),
							storage.getViewDistance(),
							storage.sizeY,
							storage.world.getBottomY()
					);
			this.queue = new Long2ObjectOpenHashMap();
		}
	}
}
