package net.minecraft.world.chunk.light;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Абстрактный провайдер освещения для одного чанка.
 * <p>
 * Управляет двумя очередями BFS: очередью уменьшения света ({@link #lightDecreaseQueue})
 * и очередью увеличения света ({@link #lightIncreaseQueue}). Каждый элемент очереди
 * представлен парой {@code long}: позиция блока и упакованные флаги {@link PackedInfo}.
 * <p>
 * Подклассы реализуют конкретную логику распространения для блочного и небесного света.
 *
 * @param <M> тип карты nibble-массивов
 * @param <S> тип хранилища освещения
 */
public abstract class ChunkLightProvider<M extends ChunkToNibbleArrayMap<M>, S extends LightStorage<M>> implements ChunkLightingView {

	public static final int MAX_LIGHT_LEVEL = 15;
	protected static final int INITIAL_LIGHT_LEVEL = 1;
	protected static final long INITIAL_PACKED_INFO = ChunkLightProvider.PackedInfo.packWithAllDirectionsSet(1);
	private static final int BLOCK_POSITIONS_INITIAL_CAPACITY = 512;
	private static final int CHUNK_CACHE_SIZE = 2;
	protected static final Direction[] DIRECTIONS = Direction.values();

	protected final ChunkProvider chunkProvider;
	protected final S lightStorage;
	private final LongOpenHashSet blockPositionsToCheck = new LongOpenHashSet(BLOCK_POSITIONS_INITIAL_CAPACITY, 0.5F);
	private final LongArrayFIFOQueue lightDecreaseQueue = new LongArrayFIFOQueue();
	private final LongArrayFIFOQueue lightIncreaseQueue = new LongArrayFIFOQueue();
	private final long[] cachedChunkPositions = new long[CHUNK_CACHE_SIZE];
	private final LightSourceView[] cachedChunks = new LightSourceView[CHUNK_CACHE_SIZE];

	protected ChunkLightProvider(ChunkProvider chunkProvider, S lightStorage) {
		this.chunkProvider = chunkProvider;
		this.lightStorage = lightStorage;
		clearChunkCache();
	}

	/**
	 * Определяет, нужно ли пересчитывать освещение при смене состояния блока.
	 * Пересчёт нужен, если изменилась непрозрачность, яркость или наличие боковой прозрачности.
	 */
	public static boolean needsLightUpdate(BlockState oldState, BlockState newState) {
		if (newState == oldState) {
			return false;
		}

		return newState.getOpacity() != oldState.getOpacity()
			|| newState.getLuminance() != oldState.getLuminance()
			|| newState.hasSidedTransparency()
			|| oldState.hasSidedTransparency();
	}

	/**
	 * Возвращает реалистичную непрозрачность между двумя соседними блоками с учётом форм их граней.
	 * Если обе грани вместе полностью перекрывают квадрат, возвращает 16 (полная блокировка).
	 */
	public static int getRealisticOpacity(BlockState state1, BlockState state2, Direction direction, int opacity2) {
		boolean trivialSource = isTrivialForLighting(state1);
		boolean trivialTarget = isTrivialForLighting(state2);

		if (trivialSource && trivialTarget) {
			return opacity2;
		}

		VoxelShape sourceShape = trivialSource ? VoxelShapes.empty() : state1.getCullingShape();
		VoxelShape targetShape = trivialTarget ? VoxelShapes.empty() : state2.getCullingShape();

		return VoxelShapes.adjacentSidesCoverSquare(sourceShape, targetShape, direction) ? 16 : opacity2;
	}

	public static VoxelShape getOpaqueShape(BlockState state, Direction direction) {
		return isTrivialForLighting(state) ? VoxelShapes.empty() : state.getCullingFace(direction);
	}

	protected static boolean isTrivialForLighting(BlockState blockState) {
		return !blockState.isOpaque() || !blockState.hasSidedTransparency();
	}

	protected BlockState getStateForLighting(BlockPos pos) {
		int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
		int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ());
		LightSourceView chunk = getChunk(chunkX, chunkZ);

		return chunk == null ? Blocks.BEDROCK.getDefaultState() : chunk.getBlockState(pos);
	}

	protected int getOpacity(BlockState state) {
		return Math.max(1, state.getOpacity());
	}

	protected boolean shapesCoverFullCube(BlockState source, BlockState target, Direction direction) {
		VoxelShape sourceShape = getOpaqueShape(source, direction);
		VoxelShape targetShape = getOpaqueShape(target, direction.getOpposite());

		return VoxelShapes.unionCoversFullCube(sourceShape, targetShape);
	}

	/**
	 * Возвращает чанк по координатам секции с кэшированием последних двух запросов.
	 * Кэш реализован как кольцевой буфер размером {@link #CHUNK_CACHE_SIZE}.
	 */
	protected @Nullable LightSourceView getChunk(int chunkX, int chunkZ) {
		long chunkKey = ChunkPos.toLong(chunkX, chunkZ);

		for (int slot = 0; slot < CHUNK_CACHE_SIZE; slot++) {
			if (chunkKey == cachedChunkPositions[slot]) {
				return cachedChunks[slot];
			}
		}

		LightSourceView chunk = chunkProvider.getChunk(chunkX, chunkZ);

		for (int slot = 1; slot > 0; slot--) {
			cachedChunkPositions[slot] = cachedChunkPositions[slot - 1];
			cachedChunks[slot] = cachedChunks[slot - 1];
		}

		cachedChunkPositions[0] = chunkKey;
		cachedChunks[0] = chunk;

		return chunk;
	}

	private void clearChunkCache() {
		Arrays.fill(cachedChunkPositions, ChunkPos.MARKER);
		Arrays.fill(cachedChunks, null);
	}

	@Override
	public void checkBlock(BlockPos pos) {
		blockPositionsToCheck.add(pos.asLong());
	}

	public void enqueueSectionData(long sectionPos, @Nullable ChunkNibbleArray lightArray) {
		lightStorage.enqueueSectionData(sectionPos, lightArray);
	}

	public void setRetainColumn(ChunkPos pos, boolean retainData) {
		lightStorage.setRetainColumn(ChunkSectionPos.withZeroY(pos.x, pos.z), retainData);
	}

	@Override
	public void setSectionStatus(ChunkSectionPos pos, boolean notReady) {
		lightStorage.setSectionStatus(pos.asLong(), notReady);
	}

	@Override
	public void setColumnEnabled(ChunkPos pos, boolean retainData) {
		lightStorage.setColumnEnabled(ChunkSectionPos.withZeroY(pos.x, pos.z), retainData);
	}

	/**
	 * Выполняет все накопленные обновления освещения за один тик.
	 * Порядок: проверка изменённых блоков → очередь уменьшения → очередь увеличения.
	 *
	 * @return суммарное количество обработанных обновлений
	 */
	@Override
	public int doLightUpdates() {
		LongIterator iterator = blockPositionsToCheck.iterator();

		while (iterator.hasNext()) {
			checkForLightUpdate(iterator.nextLong());
		}

		blockPositionsToCheck.clear();
		blockPositionsToCheck.trim(BLOCK_POSITIONS_INITIAL_CAPACITY);

		int updates = 0;
		updates += processLightDecreaseQueue();
		updates += processLightIncreaseQueue();

		clearChunkCache();
		lightStorage.updateLight(this);
		lightStorage.notifyChanges();

		return updates;
	}

	private int processLightIncreaseQueue() {
		int processed = 0;

		while (!lightIncreaseQueue.isEmpty()) {
			long blockPos = lightIncreaseQueue.dequeueLong();
			long packed = lightIncreaseQueue.dequeueLong();
			int storedLevel = lightStorage.get(blockPos);
			int packedLevel = ChunkLightProvider.PackedInfo.getLightLevel(packed);

			if (ChunkLightProvider.PackedInfo.forceSet(packed) && storedLevel < packedLevel) {
				lightStorage.set(blockPos, packedLevel);
				storedLevel = packedLevel;
			}

			if (storedLevel == packedLevel) {
				propagateLightIncrease(blockPos, packed, storedLevel);
			}

			processed++;
		}

		return processed;
	}

	private int processLightDecreaseQueue() {
		int processed = 0;

		while (!lightDecreaseQueue.isEmpty()) {
			long blockPos = lightDecreaseQueue.dequeueLong();
			long packed = lightDecreaseQueue.dequeueLong();
			propagateLightDecrease(blockPos, packed);
			processed++;
		}

		return processed;
	}

	protected void queueLightDecrease(long blockPos, long flags) {
		lightDecreaseQueue.enqueue(blockPos);
		lightDecreaseQueue.enqueue(flags);
	}

	protected void queueLightIncrease(long blockPos, long flags) {
		lightIncreaseQueue.enqueue(blockPos);
		lightIncreaseQueue.enqueue(flags);
	}

	@Override
	public boolean hasUpdates() {
		return lightStorage.hasLightUpdates()
			|| !blockPositionsToCheck.isEmpty()
			|| !lightDecreaseQueue.isEmpty()
			|| !lightIncreaseQueue.isEmpty();
	}

	@Override
	public @Nullable ChunkNibbleArray getLightSection(ChunkSectionPos pos) {
		return lightStorage.getLightSection(pos.asLong());
	}

	@Override
	public int getLightLevel(BlockPos pos) {
		return lightStorage.getLight(pos.asLong());
	}

	public String displaySectionLevel(long sectionPos) {
		return getStatus(sectionPos).getSigil();
	}

	public LightStorage.Status getStatus(long sectionPos) {
		return lightStorage.getStatus(sectionPos);
	}

	protected abstract void checkForLightUpdate(long blockPos);

	protected abstract void propagateLightIncrease(long blockPos, long packed, int lightLevel);

	protected abstract void propagateLightDecrease(long blockPos, long packed);

	/**
	 * Утилитарный класс для упаковки данных распространения света в одно {@code long}-значение.
	 * <p>
	 * Формат упакованного значения (биты):
	 * <ul>
	 *   <li>Биты 0–3: уровень света (0–15)</li>
	 *   <li>Биты 4–9: флаги направлений (по одному биту на каждое из 6 направлений)</li>
	 *   <li>Бит 10: флаг «тривиальный» (блок не имеет сложной формы)</li>
	 *   <li>Бит 11: флаг «принудительная установка» (force set)</li>
	 * </ul>
	 */
	public static class PackedInfo {

		private static final int DIRECTION_BIT_OFFSET = 4;
		private static final int DIRECTION_COUNT = 6;
		private static final long LIGHT_LEVEL_MASK = 15L;
		private static final long DIRECTION_BIT_MASK = 1008L;
		private static final long TRIVIAL_FLAG = 1024L;
		private static final long FORCE_SET_FLAG = 2048L;

		public static long packWithOneDirectionCleared(int lightLevel, Direction direction) {
			long packed = clearDirectionBit(DIRECTION_BIT_MASK, direction);

			return withLightLevel(packed, lightLevel);
		}

		public static long packWithAllDirectionsSet(int lightLevel) {
			return withLightLevel(DIRECTION_BIT_MASK, lightLevel);
		}

		public static long packWithForce(int lightLevel, boolean trivial) {
			long packed = DIRECTION_BIT_MASK | FORCE_SET_FLAG;

			if (trivial) {
				packed |= TRIVIAL_FLAG;
			}

			return withLightLevel(packed, lightLevel);
		}

		public static long packWithOneDirectionCleared(int lightLevel, boolean trivial, Direction direction) {
			long packed = clearDirectionBit(DIRECTION_BIT_MASK, direction);

			if (trivial) {
				packed |= TRIVIAL_FLAG;
			}

			return withLightLevel(packed, lightLevel);
		}

		public static long packWithRepropagate(int lightLevel, boolean trivial, Direction direction) {
			long packed = 0L;

			if (trivial) {
				packed |= TRIVIAL_FLAG;
			}

			packed = setDirectionBit(packed, direction);

			return withLightLevel(packed, lightLevel);
		}

		/**
		 * Упаковывает параметры распространения небесного света вниз и по горизонтали.
		 * Используется при инициализации освещения нового чанка.
		 */
		public static long packSkyLightPropagation(
			boolean down,
			boolean north,
			boolean south,
			boolean west,
			boolean east
		) {
			long packed = withLightLevel(0L, MAX_LIGHT_LEVEL);

			if (down) {
				packed = setDirectionBit(packed, Direction.DOWN);
			}

			if (north) {
				packed = setDirectionBit(packed, Direction.NORTH);
			}

			if (south) {
				packed = setDirectionBit(packed, Direction.SOUTH);
			}

			if (west) {
				packed = setDirectionBit(packed, Direction.WEST);
			}

			if (east) {
				packed = setDirectionBit(packed, Direction.EAST);
			}

			return packed;
		}

		public static int getLightLevel(long packed) {
			return (int) (packed & LIGHT_LEVEL_MASK);
		}

		public static boolean isTrivial(long packed) {
			return (packed & TRIVIAL_FLAG) != 0L;
		}

		public static boolean forceSet(long packed) {
			return (packed & FORCE_SET_FLAG) != 0L;
		}

		public static boolean isDirectionBitSet(long packed, Direction direction) {
			return (packed & 1L << direction.ordinal() + DIRECTION_BIT_OFFSET) != 0L;
		}

		private static long withLightLevel(long packed, int lightLevel) {
			return packed & -16L | lightLevel & LIGHT_LEVEL_MASK;
		}

		private static long setDirectionBit(long packed, Direction direction) {
			return packed | 1L << direction.ordinal() + DIRECTION_BIT_OFFSET;
		}

		private static long clearDirectionBit(long packed, Direction direction) {
			return packed & ~(1L << direction.ordinal() + DIRECTION_BIT_OFFSET);
		}
	}
}
