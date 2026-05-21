package net.minecraft.world.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortListIterator;
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
 * Полностью загруженный чанк мира: содержит блоки, блок-сущности,
 * планировщики тиков и диспетчеры игровых событий.
 */
public class WorldChunk extends Chunk implements DebugTrackable {

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
	private WorldChunk.UnsavedListener unsavedListener = posx -> {};

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
				this.heightmaps.put(type, new Heightmap(this, type));
			}
		}

		this.entityLoader = entityLoader;
		this.blockTickScheduler = blockTickScheduler;
		this.fluidTickScheduler = fluidTickScheduler;
	}

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
			this.setBlockEntity(blockEntity);
		}

		this.blockEntityNbts.putAll(protoChunk.getBlockEntityNbts());

		for (int i = 0; i < protoChunk.getPostProcessingLists().length; i++) {
			this.postProcessingLists[i] = protoChunk.getPostProcessingLists()[i];
		}

		this.setStructureStarts(protoChunk.getStructureStarts());
		this.setStructureReferences(protoChunk.getStructureReferences());

		for (Entry<Heightmap.Type, Heightmap> entry : protoChunk.getHeightmaps()) {
			if (ChunkStatus.FULL.getHeightmapTypes().contains(entry.getKey())) {
				this.setHeightmap(entry.getKey(), entry.getValue().asLongArray());
			}
		}

		this.chunkSkyLight = protoChunk.chunkSkyLight;
		this.setLightOn(protoChunk.isLightOn());
		this.markNeedsSaving();
	}

	/**
	 * Устанавливает слушатель, вызываемый при пометке чанка как несохранённого.
	 */
	public void setUnsavedListener(WorldChunk.UnsavedListener unsavedListener) {
		this.unsavedListener = unsavedListener;
		if (this.needsSaving()) {
			unsavedListener.setUnsaved(this.pos);
		}
	}

	@Override
	public void markNeedsSaving() {
		boolean bl = this.needsSaving();
		super.markNeedsSaving();
		if (!bl) {
			this.unsavedListener.setUnsaved(this.pos);
		}
	}

	@Override
	public BasicTickScheduler<Block> getBlockTickScheduler() {
		return this.blockTickScheduler;
	}

	@Override
	public BasicTickScheduler<Fluid> getFluidTickScheduler() {
		return this.fluidTickScheduler;
	}

	@Override
	public Chunk.TickSchedulers getTickSchedulers(long time) {
		return new Chunk.TickSchedulers(
				this.blockTickScheduler.collectTicks(time),
				this.fluidTickScheduler.collectTicks(time)
		);
	}

	@Override
	public GameEventDispatcher getGameEventDispatcher(int ySectionCoord) {
		return this.world instanceof ServerWorld serverWorld
		       ? (GameEventDispatcher) this.gameEventDispatchers
		                               .computeIfAbsent(
				                               ySectionCoord,
				                               sectionCoord -> new SimpleGameEventDispatcher(
						                               serverWorld,
						                               ySectionCoord,
						                               this::removeGameEventDispatcher
				                               )
		                               )
		       : super.getGameEventDispatcher(ySectionCoord);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		int i = pos.getX();
		int j = pos.getY();
		int k = pos.getZ();
		if (this.world.isDebugWorld()) {
			BlockState blockState = null;
			if (j == 60) {
				blockState = Blocks.BARRIER.getDefaultState();
			}

			if (j == 70) {
				blockState = DebugChunkGenerator.getBlockState(i, k);
			}

			return blockState == null ? Blocks.AIR.getDefaultState() : blockState;
		}
		else {
			try {
				int l = this.getSectionIndex(j);
				if (l >= 0 && l < this.sectionArray.length) {
					ChunkSection chunkSection = this.sectionArray[l];
					if (!chunkSection.isEmpty()) {
						return chunkSection.getBlockState(i & 15, j & 15, k & 15);
					}
				}

				return Blocks.AIR.getDefaultState();
			}
			catch (Throwable var8) {
				CrashReport crashReport = CrashReport.create(var8, "Getting block state");
				CrashReportSection crashReportSection = crashReport.addElement("Block being got");
				crashReportSection.add("Location", () -> CrashReportSection.createPositionString(this, i, j, k));
				throw new CrashException(crashReport);
			}
		}
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
	}

	public FluidState getFluidState(int x, int y, int z) {
		try {
			int i = this.getSectionIndex(y);
			if (i >= 0 && i < this.sectionArray.length) {
				ChunkSection chunkSection = this.sectionArray[i];
				if (!chunkSection.isEmpty()) {
					return chunkSection.getFluidState(x & 15, y & 15, z & 15);
				}
			}

			return Fluids.EMPTY.getDefaultState();
		}
		catch (Throwable var7) {
			CrashReport crashReport = CrashReport.create(var7, "Getting fluid state");
			CrashReportSection crashReportSection = crashReport.addElement("Block being got");
			crashReportSection.add("Location", () -> CrashReportSection.createPositionString(this, x, y, z));
			throw new CrashException(crashReport);
		}
	}

	@Override
	public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, @Block.SetBlockStateFlag int flags) {
		int i = pos.getY();
		ChunkSection chunkSection = this.getSection(this.getSectionIndex(i));
		boolean bl = chunkSection.isEmpty();
		if (bl && state.isAir()) {
			return null;
		}
		else {
			int j = pos.getX() & 15;
			int k = i & 15;
			int l = pos.getZ() & 15;
			BlockState blockState = chunkSection.setBlockState(j, k, l, state);
			if (blockState == state) {
				return null;
			}
			else {
				Block block = state.getBlock();
				this.heightmaps.get(Heightmap.Type.MOTION_BLOCKING).trackUpdate(j, i, l, state);
				this.heightmaps.get(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES).trackUpdate(j, i, l, state);
				this.heightmaps.get(Heightmap.Type.OCEAN_FLOOR).trackUpdate(j, i, l, state);
				this.heightmaps.get(Heightmap.Type.WORLD_SURFACE).trackUpdate(j, i, l, state);
				boolean bl2 = chunkSection.isEmpty();
				if (bl != bl2) {
					this.world.getChunkManager().getLightingProvider().setSectionStatus(pos, bl2);
					this.world
							.getChunkManager()
							.onSectionStatusChanged(this.pos.x, ChunkSectionPos.getSectionCoord(i), this.pos.z, bl2);
				}

				if (ChunkLightProvider.needsLightUpdate(blockState, state)) {
					Profiler profiler = Profilers.get();
					profiler.push("updateSkyLightSources");
					this.chunkSkyLight.isSkyLightAccessible(this, j, i, l);
					profiler.swap("queueCheckLight");
					this.world.getChunkManager().getLightingProvider().checkBlock(pos);
					profiler.pop();
				}

				boolean bl3 = !blockState.isOf(block);
				boolean bl4 = (flags & 64) != 0;
				boolean bl5 = (flags & 256) == 0;
				if (bl3 && blockState.hasBlockEntity() && !state.keepBlockEntityWhenReplacedWith(blockState)) {
					if (!this.world.isClient() && bl5) {
						BlockEntity blockEntity = this.world.getBlockEntity(pos);
						if (blockEntity != null) {
							blockEntity.onBlockReplaced(pos, blockState);
						}
					}

					this.removeBlockEntity(pos);
				}

				if ((bl3 || block instanceof AbstractRailBlock) && this.world instanceof ServerWorld serverWorld && (
						(flags & 1) != 0 || bl4
				)) {
					blockState.onStateReplaced(serverWorld, pos, bl4);
				}

				if (!chunkSection.getBlockState(j, k, l).isOf(block)) {
					return null;
				}
				else {
					if (!this.world.isClient() && (flags & 512) == 0) {
						state.onBlockAdded(this.world, pos, blockState, bl4);
					}

					if (state.hasBlockEntity()) {
						BlockEntity blockEntity = this.getBlockEntity(pos, WorldChunk.CreationType.CHECK);
						if (blockEntity != null && !blockEntity.supports(state)) {
							LOGGER.warn(
									"Found mismatched block entity @ {}: type = {}, state = {}",
									new Object[]{
											pos,
											blockEntity.getType().getRegistryEntry().registryKey().getValue(),
											state
									}
							);
							this.removeBlockEntity(pos);
							blockEntity = null;
						}

						if (blockEntity == null) {
							blockEntity = ((BlockEntityProvider) block).createBlockEntity(pos, state);
							if (blockEntity != null) {
								this.addBlockEntity(blockEntity);
							}
						}
						else {
							blockEntity.setCachedState(state);
							this.updateTicker(blockEntity);
						}
					}

					this.markNeedsSaving();
					return blockState;
				}
			}
		}
	}

	@Deprecated
	@Override
	public void addEntity(Entity entity) {
	}

	/**
	 * Создаёт блок-сущность для блока по указанной позиции, если блок её поддерживает.
	 */
	private @Nullable BlockEntity createBlockEntity(BlockPos pos) {
		BlockState blockState = this.getBlockState(pos);
		return !blockState.hasBlockEntity() ? null : ((BlockEntityProvider) blockState.getBlock()).createBlockEntity(
				pos,
				blockState
		);
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		return this.getBlockEntity(pos, WorldChunk.CreationType.CHECK);
	}

	public @Nullable BlockEntity getBlockEntity(BlockPos pos, WorldChunk.CreationType creationType) {
		BlockEntity blockEntity = this.blockEntities.get(pos);
		if (blockEntity == null) {
			NbtCompound nbtCompound = this.blockEntityNbts.remove(pos);
			if (nbtCompound != null) {
				BlockEntity blockEntity2 = this.loadBlockEntity(pos, nbtCompound);
				if (blockEntity2 != null) {
					return blockEntity2;
				}
			}
		}

		if (blockEntity == null) {
			if (creationType == WorldChunk.CreationType.IMMEDIATE) {
				blockEntity = this.createBlockEntity(pos);
				if (blockEntity != null) {
					this.addBlockEntity(blockEntity);
				}
			}
		}
		else if (blockEntity.isRemoved()) {
			this.blockEntities.remove(pos);
			return null;
		}

		return blockEntity;
	}

	/**
	 * Добавляет блок-сущность в чанк и регистрирует её тикер и слушатель событий.
	 */
	public void addBlockEntity(BlockEntity blockEntity) {
		this.setBlockEntity(blockEntity);
		if (this.canTickBlockEntities()) {
			if (this.world instanceof ServerWorld serverWorld) {
				this.updateGameEventListener(blockEntity, serverWorld);
			}

			this.world.loadBlockEntity(blockEntity);
			this.updateTicker(blockEntity);
		}
	}

	private boolean canTickBlockEntities() {
		return this.loadedToWorld || this.world.isClient();
	}

	boolean canTickBlockEntity(BlockPos pos) {
		if (!this.world.getWorldBorder().contains(pos)) {
			return false;
		}
		else {
			return !(this.world instanceof ServerWorld serverWorld)
			       ? true
			       : this.getLevelType().isAfter(ChunkLevelType.BLOCK_TICKING)
			         && serverWorld.isChunkLoaded(ChunkPos.toLong(pos));
		}
	}

	@Override
	public void setBlockEntity(BlockEntity blockEntity) {
		BlockPos blockPos = blockEntity.getPos();
		BlockState blockState = this.getBlockState(blockPos);
		if (!blockState.hasBlockEntity()) {
			LOGGER.warn(
					"Trying to set block entity {} at position {}, but state {} does not allow it",
					new Object[]{blockEntity, blockPos, blockState}
			);
		}
		else {
			BlockState blockState2 = blockEntity.getCachedState();
			if (blockState != blockState2) {
				if (!blockEntity.getType().supports(blockState)) {
					LOGGER.warn(
							"Trying to set block entity {} at position {}, but state {} does not allow it",
							new Object[]{blockEntity, blockPos, blockState}
					);
					return;
				}

				if (blockState.getBlock() != blockState2.getBlock()) {
					LOGGER.warn(
							"Block state mismatch on block entity {} in position {}, {} != {}, updating",
							new Object[]{blockEntity, blockPos, blockState, blockState2}
					);
				}

				blockEntity.setCachedState(blockState);
			}

			blockEntity.setWorld(this.world);
			blockEntity.cancelRemoval();
			BlockEntity blockEntity2 = this.blockEntities.put(blockPos.toImmutable(), blockEntity);
			if (blockEntity2 != null && blockEntity2 != blockEntity) {
				blockEntity2.markRemoved();
			}
		}
	}

	@Override
	public @Nullable NbtCompound getPackedBlockEntityNbt(BlockPos pos, RegistryWrapper.WrapperLookup registries) {
		BlockEntity blockEntity = this.getBlockEntity(pos);
		if (blockEntity != null && !blockEntity.isRemoved()) {
			NbtCompound nbtCompound = blockEntity.createNbtWithIdentifyingData(this.world.getRegistryManager());
			nbtCompound.putBoolean("keepPacked", false);
			return nbtCompound;
		}
		else {
			NbtCompound nbtCompound = this.blockEntityNbts.get(pos);
			if (nbtCompound != null) {
				nbtCompound = nbtCompound.copy();
				nbtCompound.putBoolean("keepPacked", true);
			}

			return nbtCompound;
		}
	}

	@Override
	public void removeBlockEntity(BlockPos pos) {
		if (this.canTickBlockEntities()) {
			BlockEntity blockEntity = this.blockEntities.remove(pos);
			if (blockEntity != null) {
				if (this.world instanceof ServerWorld serverWorld) {
					this.removeGameEventListener(blockEntity, serverWorld);
					serverWorld.getSubscriptionTracker().untrackBlockEntity(pos);
				}

				blockEntity.markRemoved();
			}
		}

		this.removeBlockEntityTicker(pos);
	}

	private <T extends BlockEntity> void removeGameEventListener(T blockEntity, ServerWorld world) {
		Block block = blockEntity.getCachedState().getBlock();
		if (block instanceof BlockEntityProvider) {
			GameEventListener
					gameEventListener =
					((BlockEntityProvider) block).getGameEventListener(world, blockEntity);
			if (gameEventListener != null) {
				int i = ChunkSectionPos.getSectionCoord(blockEntity.getPos().getY());
				GameEventDispatcher gameEventDispatcher = this.getGameEventDispatcher(i);
				gameEventDispatcher.removeListener(gameEventListener);
			}
		}
	}

	private void removeGameEventDispatcher(int ySectionCoord) {
		this.gameEventDispatchers.remove(ySectionCoord);
	}

	private void removeBlockEntityTicker(BlockPos pos) {
		WorldChunk.WrappedBlockEntityTickInvoker wrappedBlockEntityTickInvoker = this.blockEntityTickers.remove(pos);
		if (wrappedBlockEntityTickInvoker != null) {
			wrappedBlockEntityTickInvoker.setWrapped(EMPTY_BLOCK_ENTITY_TICKER);
		}
	}

	/**
	 * Загружает сущности чанка через зарегистрированный загрузчик.
	 */
	public void loadEntities() {
		if (this.entityLoader != null) {
			this.entityLoader.run(this);
			this.entityLoader = null;
		}
	}

	public boolean isEmpty() {
		return false;
	}

	/**
	 * Загружает данные чанка из сетевого пакета: секции, карты высот и блок-сущности.
	 */
	public void loadFromPacket(
			PacketByteBuf buf,
			Map<Heightmap.Type, long[]> heightmaps,
			Consumer<ChunkData.BlockEntityVisitor> blockEntityVisitorConsumer
	) {
		this.clear();

		for (ChunkSection chunkSection : this.sectionArray) {
			chunkSection.readDataPacket(buf);
		}

		heightmaps.forEach(this::setHeightmap);
		this.refreshSurfaceY();

		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(this.getErrorReporterContext(), LOGGER)) {
			blockEntityVisitorConsumer.accept((pos, blockEntityType, nbt) -> {
				BlockEntity blockEntity = this.getBlockEntity(pos, WorldChunk.CreationType.IMMEDIATE);
				if (blockEntity != null && nbt != null && blockEntity.getType() == blockEntityType) {
					blockEntity.read(NbtReadView.create(
							logging.makeChild(blockEntity.getReporterContext()),
							this.world.getRegistryManager(),
							nbt
					));
				}
			});
		}
	}

	public void loadBiomeFromPacket(PacketByteBuf buf) {
		for (ChunkSection chunkSection : this.sectionArray) {
			chunkSection.readBiomePacket(buf);
		}
	}

	public void setLoadedToWorld(boolean loadedToWorld) {
		this.loadedToWorld = loadedToWorld;
	}

	public World getWorld() {
		return this.world;
	}

	public Map<BlockPos, BlockEntity> getBlockEntities() {
		return this.blockEntities;
	}

	/**
	 * Выполняет постобработку чанка: применяет отложенные обновления блоков и жидкостей.
	 */
	public void runPostProcessing(ServerWorld world) {
		ChunkPos chunkPos = this.getPos();

		for (int i = 0; i < this.postProcessingLists.length; i++) {
			ShortList shortList = this.postProcessingLists[i];
			if (shortList != null) {
				ShortListIterator var5 = shortList.iterator();

				while (var5.hasNext()) {
					Short short_ = (Short) var5.next();
					BlockPos blockPos = ProtoChunk.joinBlockPos(short_, this.sectionIndexToCoord(i), chunkPos);
					BlockState blockState = this.getBlockState(blockPos);
					FluidState fluidState = blockState.getFluidState();
					if (!fluidState.isEmpty()) {
						fluidState.onScheduledTick(world, blockPos, blockState);
					}

					if (!(blockState.getBlock() instanceof FluidBlock)) {
						BlockState blockState2 = Block.postProcessState(blockState, world, blockPos);
						if (blockState2 != blockState) {
							world.setBlockState(blockPos, blockState2, 276);
						}
					}
				}

				shortList.clear();
			}
		}

		UnmodifiableIterator var11 = ImmutableList.copyOf(this.blockEntityNbts.keySet()).iterator();

		while (var11.hasNext()) {
			BlockPos blockPos2 = (BlockPos) var11.next();
			this.getBlockEntity(blockPos2);
		}

		this.blockEntityNbts.clear();
		this.upgradeData.upgrade(this);
	}

	private @Nullable BlockEntity loadBlockEntity(BlockPos pos, NbtCompound nbt) {
		BlockState blockState = this.getBlockState(pos);
		BlockEntity blockEntity;
		if ("DUMMY".equals(nbt.getString("id", ""))) {
			if (blockState.hasBlockEntity()) {
				blockEntity = ((BlockEntityProvider) blockState.getBlock()).createBlockEntity(pos, blockState);
			}
			else {
				blockEntity = null;
				LOGGER.warn(
						"Tried to load a DUMMY block entity @ {} but found not block entity block {} at location",
						pos,
						blockState
				);
			}
		}
		else {
			blockEntity = BlockEntity.createFromNbt(pos, blockState, nbt, this.world.getRegistryManager());
		}

		if (blockEntity != null) {
			blockEntity.setWorld(this.world);
			this.addBlockEntity(blockEntity);
		}
		else {
			LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", blockState, pos);
		}

		return blockEntity;
	}

	/**
	 * Отключает планировщики тиков блоков и жидкостей для данного чанка.
	 */
	public void disableTickSchedulers(long time) {
		this.blockTickScheduler.disable(time);
		this.fluidTickScheduler.disable(time);
	}

	/**
	 * Регистрирует планировщики тиков чанка в серверном мире.
	 */
	public void addChunkTickSchedulers(ServerWorld world) {
		world.getBlockTickScheduler().addChunkTickScheduler(this.pos, this.blockTickScheduler);
		world.getFluidTickScheduler().addChunkTickScheduler(this.pos, this.fluidTickScheduler);
	}

	/**
	 * Удаляет планировщики тиков чанка из серверного мира.
	 */
	public void removeChunkTickSchedulers(ServerWorld world) {
		world.getBlockTickScheduler().removeChunkTickScheduler(this.pos);
		world.getFluidTickScheduler().removeChunkTickScheduler(this.pos);
	}

	@Override
	public void registerTracking(ServerWorld world, DebugTrackable.Tracker tracker) {
		if (!this.getStructureStarts().isEmpty()) {
			tracker.track(
					DebugSubscriptionTypes.STRUCTURES, () -> {
						List<StructureDebugData> list = new ArrayList<>();

						for (StructureStart structureStart : this.getStructureStarts().values()) {
							BlockBox blockBox = structureStart.getBoundingBox();
							List<StructurePiece> list2 = structureStart.getChildren();
							List<StructureDebugData.Piece> list3 = new ArrayList<>(list2.size());

							for (int i = 0; i < list2.size(); i++) {
								boolean bl = i == 0;
								list3.add(new StructureDebugData.Piece(list2.get(i).getBoundingBox(), bl));
							}

							list.add(new StructureDebugData(blockBox, list3));
						}

						return list;
					}
			);
		}

		tracker.track(DebugSubscriptionTypes.RAIDS, () -> world.getRaidManager().getRaidCenters(this.pos));
	}

	@Override
	public ChunkStatus getStatus() {
		return ChunkStatus.FULL;
	}

	public ChunkLevelType getLevelType() {
		return this.levelTypeProvider == null ? ChunkLevelType.FULL : this.levelTypeProvider.get();
	}

	public void setLevelTypeProvider(Supplier<ChunkLevelType> levelTypeProvider) {
		this.levelTypeProvider = levelTypeProvider;
	}

	public void clear() {
		this.blockEntities.values().forEach(BlockEntity::markRemoved);
		this.blockEntities.clear();
		this.blockEntityTickers.values().forEach(ticker -> ticker.setWrapped(EMPTY_BLOCK_ENTITY_TICKER));
		this.blockEntityTickers.clear();
	}

	public void updateAllBlockEntities() {
		this.blockEntities.values().forEach(blockEntity -> {
			if (this.world instanceof ServerWorld serverWorld) {
				this.updateGameEventListener(blockEntity, serverWorld);
			}

			this.world.loadBlockEntity(blockEntity);
			this.updateTicker(blockEntity);
		});
	}

	private <T extends BlockEntity> void updateGameEventListener(T blockEntity, ServerWorld world) {
		Block block = blockEntity.getCachedState().getBlock();
		if (block instanceof BlockEntityProvider) {
			GameEventListener
					gameEventListener =
					((BlockEntityProvider) block).getGameEventListener(world, blockEntity);
			if (gameEventListener != null) {
				this
						.getGameEventDispatcher(ChunkSectionPos.getSectionCoord(blockEntity.getPos().getY()))
						.addListener(gameEventListener);
			}
		}
	}

	private <T extends BlockEntity> void updateTicker(T blockEntity) {
		BlockState blockState = blockEntity.getCachedState();
		BlockEntityTicker<T>
				blockEntityTicker =
				blockState.getBlockEntityTicker(this.world, (BlockEntityType<T>) blockEntity.getType());
		if (blockEntityTicker == null) {
			this.removeBlockEntityTicker(blockEntity.getPos());
		}
		else {
			this.blockEntityTickers.compute(
					blockEntity.getPos(), (pos, ticker) -> {
						BlockEntityTickInvoker blockEntityTickInvoker = this.wrapTicker(blockEntity, blockEntityTicker);
						if (ticker != null) {
							ticker.setWrapped(blockEntityTickInvoker);
							return (WorldChunk.WrappedBlockEntityTickInvoker) ticker;
						}
						else if (this.canTickBlockEntities()) {
							WorldChunk.WrappedBlockEntityTickInvoker
									wrappedBlockEntityTickInvoker =
									new WorldChunk.WrappedBlockEntityTickInvoker(blockEntityTickInvoker);
							this.world.addBlockEntityTicker(wrappedBlockEntityTickInvoker);
							return wrappedBlockEntityTickInvoker;
						}
						else {
							return null;
						}
					}
			);
		}
	}

	private <T extends BlockEntity> BlockEntityTickInvoker wrapTicker(
			T blockEntity,
			BlockEntityTicker<T> blockEntityTicker
	) {
		return new WorldChunk.DirectBlockEntityTickInvoker<>(blockEntity, blockEntityTicker);
	}

	/**
	 * {@code CreationType}.
	 */
	public static enum CreationType {
		IMMEDIATE,
		QUEUED,
		CHECK;
	}

	/**
	 * {@code DirectBlockEntityTickInvoker}.
	 */
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
			if (!this.blockEntity.isRemoved() && this.blockEntity.hasWorld()) {
				BlockPos blockPos = this.blockEntity.getPos();
				if (WorldChunk.this.canTickBlockEntity(blockPos)) {
					try {
						Profiler profiler = Profilers.get();
						profiler.push(this::getName);
						BlockState blockState = WorldChunk.this.getBlockState(blockPos);
						if (this.blockEntity.getType().supports(blockState)) {
							this.ticker.tick(
									WorldChunk.this.world,
									this.blockEntity.getPos(),
									blockState,
									this.blockEntity
							);
							this.hasWarned = false;
						}
						else if (!this.hasWarned) {
							this.hasWarned = true;
							WorldChunk.LOGGER
									.warn(
											"Block entity {} @ {} state {} invalid for ticking:",
											new Object[]{
													LogUtils.defer(this::getName),
													LogUtils.defer(this::getPos),
													blockState
											}
									);
						}

						profiler.pop();
					}
					catch (Throwable var5) {
						CrashReport crashReport = CrashReport.create(var5, "Ticking block entity");
						CrashReportSection crashReportSection = crashReport.addElement("Block entity being ticked");
						this.blockEntity.populateCrashReport(crashReportSection);
						throw new CrashException(crashReport);
					}
				}
			}
		}

		@Override
		public boolean isRemoved() {
			return this.blockEntity.isRemoved();
		}

		@Override
		public BlockPos getPos() {
			return this.blockEntity.getPos();
		}

		@Override
		public String getName() {
			return BlockEntityType.getId(this.blockEntity.getType()).toString();
		}

		@Override
		public String toString() {
			return "Level ticker for " + this.getName() + "@" + this.getPos();
		}
	}

	@FunctionalInterface
	/**
	 * {@code EntityLoader}.
	 */
	public interface EntityLoader {

		void run(WorldChunk chunk);
	}

	@FunctionalInterface
	/**
	 * {@code UnsavedListener}.
	 */
	public interface UnsavedListener {

		void setUnsaved(ChunkPos pos);
	}

	/**
	 * Обёртка над {@link BlockEntityTickInvoker}, позволяющая заменять
	 * внутренний тикер без пересоздания объекта.
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
			this.wrapped.tick();
		}

		@Override
		public boolean isRemoved() {
			return this.wrapped.isRemoved();
		}

		@Override
		public BlockPos getPos() {
			return this.wrapped.getPos();
		}

		@Override
		public String getName() {
			return this.wrapped.getName();
		}

		@Override
		public String toString() {
			return this.wrapped + " <wrapped>";
		}
	}
}
