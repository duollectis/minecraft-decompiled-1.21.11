package net.minecraft.world.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.debug.DebugSubscriptionTypes;
import net.minecraft.world.debug.DebugTrackable;
import net.minecraft.world.debug.data.StructureDebugData;
import net.minecraft.world.event.listener.GameEventDispatcher;
import net.minecraft.world.event.listener.GameEventListener;
import net.minecraft.world.event.listener.SimpleGameEventDispatcher;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.gen.chunk.DebugChunkGenerator;
import net.minecraft.world.tick.BasicTickScheduler;
import net.minecraft.world.tick.ChunkTickScheduler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Полностью загруженный чанк мира. Содержит блоки, блок-сущности,
 * планировщики тиков и диспетчеры игровых событий.
 * Создаётся из {@link ProtoChunk} при финализации генерации или загружается с диска.
 */
public class WorldChunk extends Chunk implements DebugTrackable {

	// Y-координата барьерного слоя в debug-мире
	private static final int DEBUG_BARRIER_Y = 60;
	// Y-координата слоя с блоками генератора в debug-мире
	private static final int DEBUG_GENERATOR_Y = 70;
	// Маска флага "не обновлять соседей" (бит 8)
	private static final int FLAG_NO_NEIGHBOR_UPDATE = 256;
	// Маска флага "принудительное обновление" (бит 6)
	private static final int FLAG_FORCE_UPDATE = 64;
	// Маска флага "обновить соседей" (бит 0)
	private static final int FLAG_NOTIFY_NEIGHBORS = 1;
	// Флаги постобработки блоков при runPostProcessing: NOTIFY | FORCE_STATE | NO_RERENDER
	private static final int POST_PROCESS_FLAGS = 276;

	static final Logger LOGGER = LogUtils.getLogger();

	private static final BlockEntityTickInvoker EMPTY_BLOCK_ENTITY_TICKER = new BlockEntityTickInvoker() {
		@Override
		public void tick() {
		}

		@Override
		public boolean isRemoved() {
			return true;
		}

		@Override
		public BlockPos getPos() {
			return BlockPos.ORIGIN;
		}

		@Override
		public String getName() {
			return "<null>";
		}
	};

	private final Map<BlockPos, WorldChunk.WrappedBlockEntityTickInvoker> blockEntityTickers = Maps.newHashMap();
	private boolean loadedToWorld;
	final World world;
	private @Nullable Supplier<ChunkLevelType> levelTypeProvider;
	private WorldChunk.@Nullable EntityLoader entityLoader;
	private final Int2ObjectMap<GameEventDispatcher> gameEventDispatchers;
	private final ChunkTickScheduler<Block> blockTickScheduler;
	private final ChunkTickScheduler<Fluid> fluidTickScheduler;
	private WorldChunk.UnsavedListener unsavedListener = chunkPos -> {};

	public WorldChunk(World world, ChunkPos pos) {
		this(
			world,
			pos,
			UpgradeData.NO_UPGRADE_DATA,
			new ChunkTickScheduler<>(),
			new ChunkTickScheduler<>(),
			0L,
			null,
			null,
			null
		);
	}

	public WorldChunk(
		World world,
		ChunkPos pos,
		UpgradeData upgradeData,
		ChunkTickScheduler<Block> blockTickScheduler,
		ChunkTickScheduler<Fluid> fluidTickScheduler,
		long inhabitedTime,
		ChunkSection @Nullable [] sectionArrayInitializer,
		WorldChunk.@Nullable EntityLoader entityLoader,
		@Nullable BlendingData blendingData
	) {
		super(
			pos,
			upgradeData,
			world,
			world.getPalettesFactory(),
			inhabitedTime,
			sectionArrayInitializer,
			blendingData
		);
		this.world = world;
		this.gameEventDispatchers = new Int2ObjectOpenHashMap();

		for (Heightmap.Type type : Heightmap.Type.values()) {
			if (ChunkStatus.FULL.getHeightmapTypes().contains(type)) {
				heightmaps.put(type, new Heightmap(this, type));
			}
		}

		this.entityLoader = entityLoader;
		this.blockTickScheduler = blockTickScheduler;
		this.fluidTickScheduler = fluidTickScheduler;
	}

	/**
	 * Конструктор конвертации: создаёт загруженный чанк из прото-чанка генерации.
	 * Переносит все секции, блок-сущности, карты высот и структуры.
	 */
	public WorldChunk(ServerWorld world, ProtoChunk protoChunk, WorldChunk.@Nullable EntityLoader entityLoader) {
		this(
			world,
			protoChunk.getPos(),
			protoChunk.getUpgradeData(),
			protoChunk.getBlockProtoTickScheduler(),
			protoChunk.getFluidProtoTickScheduler(),
			protoChunk.getInhabitedTime(),
			protoChunk.getSectionArray(),
			entityLoader,
			protoChunk.getBlendingData()
		);

		if (!Collections.disjoint(protoChunk.blockEntityNbts.keySet(), protoChunk.blockEntities.keySet())) {
			LOGGER.error("Chunk at {} contains duplicated block entities", protoChunk.getPos());
		}

		for (BlockEntity blockEntity : protoChunk.getBlockEntities().values()) {
			setBlockEntity(blockEntity);
		}

		blockEntityNbts.putAll(protoChunk.getBlockEntityNbts());

		for (int index = 0; index < protoChunk.getPostProcessingLists().length; index++) {
			postProcessingLists[index] = protoChunk.getPostProcessingLists()[index];
		}

		setStructureStarts(protoChunk.getStructureStarts());
		setStructureReferences(protoChunk.getStructureReferences());

		for (Entry<Heightmap.Type, Heightmap> entry : protoChunk.getHeightmaps()) {
			if (ChunkStatus.FULL.getHeightmapTypes().contains(entry.getKey())) {
				setHeightmap(entry.getKey(), entry.getValue().asLongArray());
			}
		}

		chunkSkyLight = protoChunk.chunkSkyLight;
		setLightOn(protoChunk.isLightOn());
		markNeedsSaving();
	}

	/**
	 * Устанавливает слушатель, вызываемый при пометке чанка как несохранённого.
	 * Если чанк уже помечен — немедленно уведомляет слушателя.
	 */
	public void setUnsavedListener(WorldChunk.UnsavedListener unsavedListener) {
		this.unsavedListener = unsavedListener;
		if (needsSaving()) {
			unsavedListener.setUnsaved(pos);
		}
	}

	@Override
	public void markNeedsSaving() {
		boolean wasSaving = needsSaving();
		super.markNeedsSaving();
		if (!wasSaving) {
			unsavedListener.setUnsaved(pos);
		}
	}

	@Override
	public BasicTickScheduler<Block> getBlockTickScheduler() {
		return blockTickScheduler;
	}

	@Override
	public BasicTickScheduler<Fluid> getFluidTickScheduler() {
		return fluidTickScheduler;
	}

	@Override
	public Chunk.TickSchedulers getTickSchedulers(long time) {
		return new Chunk.TickSchedulers(
			blockTickScheduler.collectTicks(time),
			fluidTickScheduler.collectTicks(time)
		);
	}

	@Override
	public GameEventDispatcher getGameEventDispatcher(int ySectionCoord) {
		return world instanceof ServerWorld serverWorld
			? gameEventDispatchers.computeIfAbsent(
				ySectionCoord,
				sectionCoord -> new SimpleGameEventDispatcher(
					serverWorld,
					ySectionCoord,
					this::removeGameEventDispatcher
				)
			)
			: super.getGameEventDispatcher(ySectionCoord);
	}

	/**
	 * Возвращает состояние блока по позиции. В debug-мире возвращает специальные блоки
	 * на фиксированных Y-уровнях ({@value #DEBUG_BARRIER_Y} и {@value #DEBUG_GENERATOR_Y}).
	 */
	@Override
	public BlockState getBlockState(BlockPos pos) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		if (world.isDebugWorld()) {
			return getDebugWorldBlockState(x, y, z);
		}

		try {
			int sectionIndex = getSectionIndex(y);
			if (sectionIndex >= 0 && sectionIndex < sectionArray.length) {
				ChunkSection section = sectionArray[sectionIndex];
				if (!section.isEmpty()) {
					return section.getBlockState(x & 15, y & 15, z & 15);
				}
			}

			return Blocks.AIR.getDefaultState();
		} catch (Throwable error) {
			CrashReport crashReport = CrashReport.create(error, "Getting block state");
			CrashReportSection section = crashReport.addElement("Block being got");
			section.add("Location", () -> CrashReportSection.createPositionString(this, x, y, z));
			throw new CrashException(crashReport);
		}
	}

	private BlockState getDebugWorldBlockState(int x, int y, int z) {
		if (y == DEBUG_BARRIER_Y) {
			return Blocks.BARRIER.getDefaultState();
		}

		if (y == DEBUG_GENERATOR_Y) {
			return DebugChunkGenerator.getBlockState(x, z);
		}

		return Blocks.AIR.getDefaultState();
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getFluidState(pos.getX(), pos.getY(), pos.getZ());
	}

	public FluidState getFluidState(int x, int y, int z) {
		try {
			int sectionIndex = getSectionIndex(y);
			if (sectionIndex >= 0 && sectionIndex < sectionArray.length) {
				ChunkSection section = sectionArray[sectionIndex];
				if (!section.isEmpty()) {
					return section.getFluidState(x & 15, y & 15, z & 15);
				}
			}

			return Fluids.EMPTY.getDefaultState();
		} catch (Throwable error) {
			CrashReport crashReport = CrashReport.create(error, "Getting fluid state");
			CrashReportSection section = crashReport.addElement("Block being got");
			section.add("Location", () -> CrashReportSection.createPositionString(this, x, y, z));
			throw new CrashException(crashReport);
		}
	}

	/**
	 * Устанавливает состояние блока в чанке. Обновляет карты высот, освещение,
	 * статус секции и блок-сущности. Возвращает предыдущее состояние блока,
	 * или {@code null} если блок не изменился или секция пуста.
	 */
	@Override
	public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, @Block.SetBlockStateFlag int flags) {
		int y = pos.getY();
		ChunkSection section = getSection(getSectionIndex(y));
		boolean wasEmpty = section.isEmpty();

		if (wasEmpty && state.isAir()) {
			return null;
		}

		int localX = pos.getX() & 15;
		int localY = y & 15;
		int localZ = pos.getZ() & 15;
		BlockState previousState = section.setBlockState(localX, localY, localZ, state);

		if (previousState == state) {
			return null;
		}

		Block newBlock = state.getBlock();
		heightmaps.get(Heightmap.Type.MOTION_BLOCKING).trackUpdate(localX, y, localZ, state);
		heightmaps.get(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES).trackUpdate(localX, y, localZ, state);
		heightmaps.get(Heightmap.Type.OCEAN_FLOOR).trackUpdate(localX, y, localZ, state);
		heightmaps.get(Heightmap.Type.WORLD_SURFACE).trackUpdate(localX, y, localZ, state);

		boolean nowEmpty = section.isEmpty();
		if (wasEmpty != nowEmpty) {
			world.getChunkManager().getLightingProvider().setSectionStatus(pos, nowEmpty);
			world.getChunkManager().onSectionStatusChanged(
				this.pos.x,
				ChunkSectionPos.getSectionCoord(y),
				this.pos.z,
				nowEmpty
			);
		}

		if (ChunkLightProvider.needsLightUpdate(previousState, state)) {
			Profiler profiler = Profilers.get();
			profiler.push("updateSkyLightSources");
			chunkSkyLight.isSkyLightAccessible(this, localX, y, localZ);
			profiler.swap("queueCheckLight");
			world.getChunkManager().getLightingProvider().checkBlock(pos);
			profiler.pop();
		}

		boolean blockTypeChanged = !previousState.isOf(newBlock);
		boolean forceUpdate = (flags & FLAG_FORCE_UPDATE) != 0;
		boolean notifyNeighbors = (flags & FLAG_NO_NEIGHBOR_UPDATE) == 0;

		if (blockTypeChanged && previousState.hasBlockEntity() && !state.keepBlockEntityWhenReplacedWith(previousState)) {
			if (!world.isClient() && notifyNeighbors) {
				BlockEntity existing = world.getBlockEntity(pos);
				if (existing != null) {
					existing.onBlockReplaced(pos, previousState);
				}
			}

			removeBlockEntity(pos);
		}

		if ((blockTypeChanged || newBlock instanceof AbstractRailBlock)
			&& world instanceof ServerWorld serverWorld
			&& ((flags & FLAG_NOTIFY_NEIGHBORS) != 0 || forceUpdate)
		) {
			previousState.onStateReplaced(serverWorld, pos, forceUpdate);
		}

		if (!section.getBlockState(localX, localY, localZ).isOf(newBlock)) {
			return null;
		}

		if (!world.isClient() && (flags & World.MAX_UPDATE_DEPTH) == 0) {
			state.onBlockAdded(world, pos, previousState, forceUpdate);
		}

		if (state.hasBlockEntity()) {
			BlockEntity blockEntity = getBlockEntity(pos, WorldChunk.CreationType.CHECK);

			if (blockEntity != null && !blockEntity.supports(state)) {
				LOGGER.warn(
					"Found mismatched block entity @ {}: type = {}, state = {}",
					pos,
					blockEntity.getType().getRegistryEntry().registryKey().getValue(),
					state
				);
				removeBlockEntity(pos);
				blockEntity = null;
			}

			if (blockEntity == null) {
				blockEntity = ((BlockEntityProvider) newBlock).createBlockEntity(pos, state);
				if (blockEntity != null) {
					addBlockEntity(blockEntity);
				}
			} else {
				blockEntity.setCachedState(state);
				updateTicker(blockEntity);
			}
		}

		markNeedsSaving();
		return previousState;
	}

	@Deprecated
	@Override
	public void addEntity(Entity entity) {
	}

	private @Nullable BlockEntity createBlockEntity(BlockPos pos) {
		BlockState blockState = getBlockState(pos);
		return !blockState.hasBlockEntity()
			? null
			: ((BlockEntityProvider) blockState.getBlock()).createBlockEntity(pos, blockState);
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		return getBlockEntity(pos, WorldChunk.CreationType.CHECK);
	}

	/**
	 * Возвращает блок-сущность по позиции. Если сущность не загружена,
	 * пытается восстановить её из NBT. При {@link CreationType#IMMEDIATE} создаёт новую.
	 */
	public @Nullable BlockEntity getBlockEntity(BlockPos pos, WorldChunk.CreationType creationType) {
		BlockEntity blockEntity = blockEntities.get(pos);

		if (blockEntity == null) {
			NbtCompound pendingNbt = blockEntityNbts.remove(pos);
			if (pendingNbt != null) {
				BlockEntity loaded = loadBlockEntity(pos, pendingNbt);
				if (loaded != null) {
					return loaded;
				}
			}
		}

		if (blockEntity == null) {
			if (creationType == WorldChunk.CreationType.IMMEDIATE) {
				blockEntity = createBlockEntity(pos);
				if (blockEntity != null) {
					addBlockEntity(blockEntity);
				}
			}
		} else if (blockEntity.isRemoved()) {
			blockEntities.remove(pos);
			return null;
		}

		return blockEntity;
	}

	/**
	 * Добавляет блок-сущность в чанк и регистрирует её тикер и слушатель событий.
	 */
	public void addBlockEntity(BlockEntity blockEntity) {
		setBlockEntity(blockEntity);

		if (!canTickBlockEntities()) {
			return;
		}

		if (world instanceof ServerWorld serverWorld) {
			updateGameEventListener(blockEntity, serverWorld);
		}

		world.loadBlockEntity(blockEntity);
		updateTicker(blockEntity);
	}

	private boolean canTickBlockEntities() {
		return loadedToWorld || world.isClient();
	}

	boolean canTickBlockEntity(BlockPos pos) {
		if (!world.getWorldBorder().contains(pos)) {
			return false;
		}

		return !(world instanceof ServerWorld serverWorld)
			|| getLevelType().isAfter(ChunkLevelType.BLOCK_TICKING)
			&& serverWorld.isChunkLoaded(ChunkPos.toLong(pos));
	}

	@Override
	public void setBlockEntity(BlockEntity blockEntity) {
		BlockPos blockPos = blockEntity.getPos();
		BlockState blockState = getBlockState(blockPos);

		if (!blockState.hasBlockEntity()) {
			LOGGER.warn(
				"Trying to set block entity {} at position {}, but state {} does not allow it",
				blockEntity, blockPos, blockState
			);
			return;
		}

		BlockState cachedState = blockEntity.getCachedState();
		if (blockState != cachedState) {
			if (!blockEntity.getType().supports(blockState)) {
				LOGGER.warn(
					"Trying to set block entity {} at position {}, but state {} does not allow it",
					blockEntity, blockPos, blockState
				);
				return;
			}

			if (blockState.getBlock() != cachedState.getBlock()) {
				LOGGER.warn(
					"Block state mismatch on block entity {} in position {}, {} != {}, updating",
					blockEntity, blockPos, blockState, cachedState
				);
			}

			blockEntity.setCachedState(blockState);
		}

		blockEntity.setWorld(world);
		blockEntity.cancelRemoval();

		BlockEntity previous = blockEntities.put(blockPos.toImmutable(), blockEntity);
		if (previous != null && previous != blockEntity) {
			previous.markRemoved();
		}
	}

	@Override
	public @Nullable NbtCompound getPackedBlockEntityNbt(BlockPos pos, RegistryWrapper.WrapperLookup registries) {
		BlockEntity blockEntity = getBlockEntity(pos);

		if (blockEntity != null && !blockEntity.isRemoved()) {
			NbtCompound nbt = blockEntity.createNbtWithIdentifyingData(world.getRegistryManager());
			nbt.putBoolean("keepPacked", false);
			return nbt;
		}

		NbtCompound pendingNbt = blockEntityNbts.get(pos);
		if (pendingNbt != null) {
			pendingNbt = pendingNbt.copy();
			pendingNbt.putBoolean("keepPacked", true);
		}

		return pendingNbt;
	}

	@Override
	public void removeBlockEntity(BlockPos pos) {
		if (canTickBlockEntities()) {
			BlockEntity blockEntity = blockEntities.remove(pos);
			if (blockEntity != null) {
				if (world instanceof ServerWorld serverWorld) {
					removeGameEventListener(blockEntity, serverWorld);
					serverWorld.getSubscriptionTracker().untrackBlockEntity(pos);
				}

				blockEntity.markRemoved();
			}
		}

		removeBlockEntityTicker(pos);
	}

	private <T extends BlockEntity> void removeGameEventListener(T blockEntity, ServerWorld world) {
		Block block = blockEntity.getCachedState().getBlock();
		if (!(block instanceof BlockEntityProvider provider)) {
			return;
		}

		GameEventListener listener = provider.getGameEventListener(world, blockEntity);
		if (listener == null) {
			return;
		}

		int sectionCoord = ChunkSectionPos.getSectionCoord(blockEntity.getPos().getY());
		getGameEventDispatcher(sectionCoord).removeListener(listener);
	}

	private void removeGameEventDispatcher(int ySectionCoord) {
		gameEventDispatchers.remove(ySectionCoord);
	}

	private void removeBlockEntityTicker(BlockPos pos) {
		WorldChunk.WrappedBlockEntityTickInvoker wrapper = blockEntityTickers.remove(pos);
		if (wrapper != null) {
			wrapper.setWrapped(EMPTY_BLOCK_ENTITY_TICKER);
		}
	}

	/**
	 * Загружает сущности чанка через зарегистрированный загрузчик.
	 * После вызова загрузчик сбрасывается, чтобы не выполняться повторно.
	 */
	public void loadEntities() {
		if (entityLoader != null) {
			entityLoader.run(this);
			entityLoader = null;
		}
	}

	public boolean isEmpty() {
		return false;
	}

	/**
	 * Загружает данные чанка из сетевого пакета: секции, карты высот и блок-сущности.
	 * Полностью очищает предыдущее состояние перед загрузкой.
	 */
	public void loadFromPacket(
		PacketByteBuf buf,
		Map<Heightmap.Type, long[]> heightmaps,
		Consumer<ChunkData.BlockEntityVisitor> blockEntityVisitorConsumer
	) {
		clear();

		for (ChunkSection section : sectionArray) {
			section.readDataPacket(buf);
		}

		heightmaps.forEach(this::setHeightmap);
		refreshSurfaceY();

		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(getErrorReporterContext(), LOGGER)) {
			blockEntityVisitorConsumer.accept((bePos, blockEntityType, nbt) -> {
				BlockEntity blockEntity = getBlockEntity(bePos, WorldChunk.CreationType.IMMEDIATE);
				if (blockEntity != null && nbt != null && blockEntity.getType() == blockEntityType) {
					blockEntity.read(NbtReadView.create(
						logging.makeChild(blockEntity.getReporterContext()),
						world.getRegistryManager(),
						nbt
					));
				}
			});
		}
	}

	public void loadBiomeFromPacket(PacketByteBuf buf) {
		for (ChunkSection section : sectionArray) {
			section.readBiomePacket(buf);
		}
	}

	public void setLoadedToWorld(boolean loadedToWorld) {
		this.loadedToWorld = loadedToWorld;
	}

	public World getWorld() {
		return world;
	}

	public Map<BlockPos, BlockEntity> getBlockEntities() {
		return blockEntities;
	}

	/**
	 * Выполняет постобработку чанка: применяет отложенные обновления блоков и жидкостей,
	 * загружает оставшиеся NBT блок-сущностей и запускает апгрейд данных.
	 */
	public void runPostProcessing(ServerWorld world) {
		ChunkPos chunkPos = getPos();

		for (int index = 0; index < postProcessingLists.length; index++) {
			ShortList shortList = postProcessingLists[index];
			if (shortList == null) {
				continue;
			}

			for (short packed : shortList) {
				BlockPos blockPos = ProtoChunk.joinBlockPos(packed, sectionIndexToCoord(index), chunkPos);
				BlockState blockState = getBlockState(blockPos);
				FluidState fluidState = blockState.getFluidState();

				if (!fluidState.isEmpty()) {
					fluidState.onScheduledTick(world, blockPos, blockState);
				}

				if (!(blockState.getBlock() instanceof FluidBlock)) {
					BlockState processed = Block.postProcessState(blockState, world, blockPos);
					if (processed != blockState) {
						world.setBlockState(blockPos, processed, POST_PROCESS_FLAGS);
					}
				}
			}

			shortList.clear();
		}

		for (BlockPos pendingPos : ImmutableList.copyOf(blockEntityNbts.keySet())) {
			getBlockEntity(pendingPos);
		}

		blockEntityNbts.clear();
		upgradeData.upgrade(this);
	}

	private @Nullable BlockEntity loadBlockEntity(BlockPos pos, NbtCompound nbt) {
		BlockState blockState = getBlockState(pos);
		BlockEntity blockEntity;

		if ("DUMMY".equals(nbt.getString("id", ""))) {
			if (blockState.hasBlockEntity()) {
				blockEntity = ((BlockEntityProvider) blockState.getBlock()).createBlockEntity(pos, blockState);
			} else {
				blockEntity = null;
				LOGGER.warn(
					"Tried to load a DUMMY block entity @ {} but found not block entity block {} at location",
					pos,
					blockState
				);
			}
		} else {
			blockEntity = BlockEntity.createFromNbt(pos, blockState, nbt, world.getRegistryManager());
		}

		if (blockEntity != null) {
			blockEntity.setWorld(world);
			addBlockEntity(blockEntity);
		} else {
			LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", blockState, pos);
		}

		return blockEntity;
	}

	/**
	 * Отключает планировщики тиков блоков и жидкостей для данного чанка.
	 */
	public void disableTickSchedulers(long time) {
		blockTickScheduler.disable(time);
		fluidTickScheduler.disable(time);
	}

	/**
	 * Регистрирует планировщики тиков чанка в серверном мире.
	 */
	public void addChunkTickSchedulers(ServerWorld world) {
		world.getBlockTickScheduler().addChunkTickScheduler(pos, blockTickScheduler);
		world.getFluidTickScheduler().addChunkTickScheduler(pos, fluidTickScheduler);
	}

	/**
	 * Удаляет планировщики тиков чанка из серверного мира.
	 */
	public void removeChunkTickSchedulers(ServerWorld world) {
		world.getBlockTickScheduler().removeChunkTickScheduler(pos);
		world.getFluidTickScheduler().removeChunkTickScheduler(pos);
	}

	@Override
	public void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker) {
		if (getStructureStarts().isEmpty()) {
			return;
		}

		tracker.track(
			DebugSubscriptionTypes.STRUCTURES, () -> {
				List<StructureDebugData> debugDataList = new ArrayList<>();

				for (StructureStart structureStart : getStructureStarts().values()) {
					BlockBox boundingBox = structureStart.getBoundingBox();
					List<StructurePiece> pieces = structureStart.getChildren();
					List<StructureDebugData.Piece> debugPieces = new ArrayList<>(pieces.size());

					for (int i = 0; i < pieces.size(); i++) {
						boolean isFirst = i == 0;
						debugPieces.add(new StructureDebugData.Piece(pieces.get(i).getBoundingBox(), isFirst));
					}

					debugDataList.add(new StructureDebugData(boundingBox, debugPieces));
				}

				return debugDataList;
			}
		);

		tracker.track(DebugSubscriptionTypes.RAIDS, () -> world.getRaidManager().getRaidCenters(pos));
	}

	@Override
	public ChunkStatus getStatus() {
		return ChunkStatus.FULL;
	}

	public ChunkLevelType getLevelType() {
		return levelTypeProvider == null ? ChunkLevelType.FULL : levelTypeProvider.get();
	}

	public void setLevelTypeProvider(Supplier<ChunkLevelType> levelTypeProvider) {
		this.levelTypeProvider = levelTypeProvider;
	}

	public void clear() {
		blockEntities.values().forEach(BlockEntity::markRemoved);
		blockEntities.clear();
		blockEntityTickers.values().forEach(ticker -> ticker.setWrapped(EMPTY_BLOCK_ENTITY_TICKER));
		blockEntityTickers.clear();
	}

	/**
		* Регистрирует все блок-сущности чанка в мире: обновляет слушателей событий и тикеры.
		* Вызывается при загрузке чанка в мир.
		*/
	public void updateAllBlockEntities() {
		blockEntities.values().forEach(blockEntity -> {
			if (world instanceof ServerWorld serverWorld) {
				updateGameEventListener(blockEntity, serverWorld);
			}

			world.loadBlockEntity(blockEntity);
			updateTicker(blockEntity);
		});
	}

	private <T extends BlockEntity> void updateGameEventListener(T blockEntity, ServerWorld world) {
		Block block = blockEntity.getCachedState().getBlock();
		if (!(block instanceof BlockEntityProvider provider)) {
			return;
		}

		GameEventListener listener = provider.getGameEventListener(world, blockEntity);
		if (listener == null) {
			return;
		}

		getGameEventDispatcher(ChunkSectionPos.getSectionCoord(blockEntity.getPos().getY()))
			.addListener(listener);
	}

	private <T extends BlockEntity> void updateTicker(T blockEntity) {
		BlockState blockState = blockEntity.getCachedState();
		BlockEntityTicker<T> ticker = blockState.getBlockEntityTicker(world, (BlockEntityType<T>) blockEntity.getType());

		if (ticker == null) {
			removeBlockEntityTicker(blockEntity.getPos());
			return;
		}

		blockEntityTickers.compute(
			blockEntity.getPos(), (tickerPos, existing) -> {
				BlockEntityTickInvoker invoker = wrapTicker(blockEntity, ticker);
				if (existing != null) {
					existing.setWrapped(invoker);
					return existing;
				}

				if (!canTickBlockEntities()) {
					return null;
				}

				WorldChunk.WrappedBlockEntityTickInvoker wrapped = new WorldChunk.WrappedBlockEntityTickInvoker(invoker);
				world.addBlockEntityTicker(wrapped);
				return wrapped;
			}
		);
	}

	private <T extends BlockEntity> BlockEntityTickInvoker wrapTicker(
		T blockEntity,
		BlockEntityTicker<T> ticker
	) {
		return new WorldChunk.DirectBlockEntityTickInvoker<>(blockEntity, ticker);
	}

	public enum CreationType {
		IMMEDIATE,
		QUEUED,
		CHECK;
	}

	class DirectBlockEntityTickInvoker<T extends BlockEntity> implements BlockEntityTickInvoker {

		private final T blockEntity;
		private final BlockEntityTicker<T> ticker;
		private boolean hasWarned;

		DirectBlockEntityTickInvoker(final T blockEntity, final BlockEntityTicker<T> ticker) {
			this.blockEntity = blockEntity;
			this.ticker = ticker;
		}

		@Override
		public void tick() {
			if (blockEntity.isRemoved() || !blockEntity.hasWorld()) {
				return;
			}

			BlockPos blockPos = blockEntity.getPos();
			if (!WorldChunk.this.canTickBlockEntity(blockPos)) {
				return;
			}

			try {
				Profiler profiler = Profilers.get();
				profiler.push(this::getName);
				BlockState blockState = WorldChunk.this.getBlockState(blockPos);

				if (blockEntity.getType().supports(blockState)) {
					ticker.tick(WorldChunk.this.world, blockEntity.getPos(), blockState, blockEntity);
					hasWarned = false;
				} else if (!hasWarned) {
					hasWarned = true;
					WorldChunk.LOGGER.warn(
						"Block entity {} @ {} state {} invalid for ticking:",
						LogUtils.defer(this::getName),
						LogUtils.defer(this::getPos),
						blockState
					);
				}

				profiler.pop();
			} catch (Throwable error) {
				CrashReport crashReport = CrashReport.create(error, "Ticking block entity");
				CrashReportSection section = crashReport.addElement("Block entity being ticked");
				blockEntity.populateCrashReport(section);
				throw new CrashException(crashReport);
			}
		}

		@Override
		public boolean isRemoved() {
			return blockEntity.isRemoved();
		}

		@Override
		public BlockPos getPos() {
			return blockEntity.getPos();
		}

		@Override
		public String getName() {
			return BlockEntityType.getId(blockEntity.getType()).toString();
		}

		@Override
		public String toString() {
			return "Level ticker for " + getName() + "@" + getPos();
		}
	}

	@FunctionalInterface
	public interface EntityLoader {

		void run(WorldChunk chunk);
	}

	@FunctionalInterface
	public interface UnsavedListener {

		void setUnsaved(ChunkPos pos);
	}

	/**
		* Обёртка над {@link BlockEntityTickInvoker}, позволяющая заменять
		* внутренний тикер без пересоздания объекта в списке мира.
		*/
	static class WrappedBlockEntityTickInvoker implements BlockEntityTickInvoker {

		private BlockEntityTickInvoker wrapped;

		WrappedBlockEntityTickInvoker(BlockEntityTickInvoker initialTicker) {
			this.wrapped = initialTicker;
		}

		void setWrapped(BlockEntityTickInvoker newTicker) {
			this.wrapped = newTicker;
		}

		@Override
		public void tick() {
			wrapped.tick();
		}

		@Override
		public boolean isRemoved() {
			return wrapped.isRemoved();
		}

		@Override
		public BlockPos getPos() {
			return wrapped.getPos();
		}

		@Override
		public String getName() {
			return wrapped.getName();
		}

		@Override
		public String toString() {
			return wrapped + " <wrapped>";
		}
	}
}
