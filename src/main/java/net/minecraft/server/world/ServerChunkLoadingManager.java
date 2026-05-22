package net.minecraft.server.world;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtException;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkBiomeDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ChunkFilter;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.PlayerAssociatedNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.CsvWriter;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.*;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.util.thread.SimpleConsecutiveExecutor;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.ChunkLoadingManager;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.rule.GameRules;
import net.minecraft.world.storage.StorageKey;
import net.minecraft.world.storage.VersionedChunkStorage;
import net.minecraft.world.updater.FeatureUpdater;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Stream;

/**
 * Класс Server Chunk Loading Manager.
 */
public class ServerChunkLoadingManager extends VersionedChunkStorage implements ChunkHolder.PlayersWatchingChunkProvider, ChunkLoadingManager {

	private static final OptionalChunk<List<Chunk>>
			UNLOADED_CHUNKS =
			OptionalChunk.of("Unloaded chunks found in range");
	private static final CompletableFuture<OptionalChunk<List<Chunk>>>
			UNLOADED_CHUNKS_FUTURE =
			CompletableFuture.completedFuture(UNLOADED_CHUNKS);
	private static final byte PROTO_CHUNK = -1;
	private static final byte UNMARKED_CHUNK = 0;
	private static final byte LEVEL_CHUNK = 1;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int UNLOAD_TASK_QUEUE_DRAIN_LIMIT = 200;
	private static final int MAX_CHUNKS_SAVED_PER_TICK = 20;
	private static final int CHUNK_SAVE_COOLDOWN_MS = 10000;
	private static final int MAX_CONCURRENT_SAVES = 128;
	public static final int DEFAULT_VIEW_DISTANCE = 2;
	public static final int MAX_VIEW_DISTANCE = 32;
	public static final int FORCED_CHUNK_LEVEL = ChunkLevels.getLevelFromType(ChunkLevelType.ENTITY_TICKING);
	private final Long2ObjectLinkedOpenHashMap<ChunkHolder> currentChunkHolders = new Long2ObjectLinkedOpenHashMap();
	private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> chunkHolders = this.currentChunkHolders.clone();
	private final Long2ObjectLinkedOpenHashMap<ChunkHolder> chunksToUnload = new Long2ObjectLinkedOpenHashMap();
	private final List<ChunkLoader> loaders = new ArrayList<>();
	final ServerWorld world;
	private final ServerLightingProvider lightingProvider;
	private final ThreadExecutor<Runnable> mainThreadExecutor;
	private final NoiseConfig noiseConfig;
	private final StructurePlacementCalculator structurePlacementCalculator;
	private final ChunkTicketManager ticketManager;
	private final PointOfInterestStorage pointOfInterestStorage;
	final LongSet unloadedChunks = new LongOpenHashSet();
	private boolean chunkHolderListDirty;
	private final ChunkTaskScheduler worldGenScheduler;
	private final ChunkTaskScheduler lightScheduler;
	private final ChunkStatusChangeListener chunkStatusChangeListener;
	private final ServerChunkLoadingManager.LevelManager levelManager;
	private final String saveDir;
	private final PlayerChunkWatchingManager playerChunkWatchingManager = new PlayerChunkWatchingManager();
	private final Int2ObjectMap<ServerChunkLoadingManager.EntityTracker> entityTrackers = new Int2ObjectOpenHashMap();
	private final Long2ByteMap chunkToType = new Long2ByteOpenHashMap();
	private final Long2LongMap chunkToNextSaveTimeMs = new Long2LongOpenHashMap();
	private final LongSet chunksToSave = new LongLinkedOpenHashSet();
	private final Queue<Runnable> unloadTaskQueue = Queues.newConcurrentLinkedQueue();
	private final AtomicInteger chunksBeingSavedCount = new AtomicInteger();
	private int watchDistance;
	private final ChunkGenerationContext generationContext;

	public ServerChunkLoadingManager(
			ServerWorld world,
			LevelStorage.Session session,
			DataFixer dataFixer,
			StructureTemplateManager structureTemplateManager,
			Executor executor,
			ThreadExecutor<Runnable> mainThreadExecutor,
			ChunkProvider chunkProvider,
			ChunkGenerator chunkGenerator,
			ChunkStatusChangeListener chunkStatusChangeListener,
			Supplier<PersistentStateManager> persistentStateManagerFactory,
			ChunkTicketManager ticketManager,
			int viewDistance,
			boolean dsync
	) {
		super(
				new StorageKey(session.getDirectoryName(), world.getRegistryKey(), "chunk"),
				session.getWorldDirectory(world.getRegistryKey()).resolve("region"),
				dataFixer,
				dsync,
				DataFixTypes.CHUNK,
				FeatureUpdater.create(world.getRegistryKey(), persistentStateManagerFactory, dataFixer)
		);
		Path path = session.getWorldDirectory(world.getRegistryKey());
		this.saveDir = path.getFileName().toString();
		this.world = world;
		DynamicRegistryManager dynamicRegistryManager = world.getRegistryManager();
		long l = world.getSeed();
		if (chunkGenerator instanceof NoiseChunkGenerator noiseChunkGenerator) {
			this.noiseConfig =
					NoiseConfig.create(
							noiseChunkGenerator.getSettings().value(),
							dynamicRegistryManager.getOrThrow(RegistryKeys.NOISE_PARAMETERS),
							l
					);
		}
		else {
			this.noiseConfig = NoiseConfig.create(
					ChunkGeneratorSettings.createMissingSettings(),
					dynamicRegistryManager.getOrThrow(RegistryKeys.NOISE_PARAMETERS),
					l
			);
		}

		this.structurePlacementCalculator = chunkGenerator.createStructurePlacementCalculator(
				dynamicRegistryManager.getOrThrow(RegistryKeys.STRUCTURE_SET), this.noiseConfig, l
		);
		this.mainThreadExecutor = mainThreadExecutor;
		SimpleConsecutiveExecutor simpleConsecutiveExecutor = new SimpleConsecutiveExecutor(executor, "worldgen");
		this.chunkStatusChangeListener = chunkStatusChangeListener;
		SimpleConsecutiveExecutor simpleConsecutiveExecutor2 = new SimpleConsecutiveExecutor(executor, "light");
		this.worldGenScheduler = new ChunkTaskScheduler(simpleConsecutiveExecutor, executor);
		this.lightScheduler = new ChunkTaskScheduler(simpleConsecutiveExecutor2, executor);
		this.lightingProvider = new ServerLightingProvider(
				chunkProvider,
				this,
				this.world.getDimension().hasSkyLight(),
				simpleConsecutiveExecutor2,
				this.lightScheduler
		);
		this.levelManager = new ServerChunkLoadingManager.LevelManager(ticketManager, executor, mainThreadExecutor);
		this.ticketManager = ticketManager;
		this.pointOfInterestStorage = new PointOfInterestStorage(
				new StorageKey(session.getDirectoryName(), world.getRegistryKey(), "poi"),
				path.resolve("poi"),
				dataFixer,
				dsync,
				dynamicRegistryManager,
				world.getServer(),
				world
		);
		this.setViewDistance(viewDistance);
		this.generationContext = new ChunkGenerationContext(
				world,
				chunkGenerator,
				structureTemplateManager,
				this.lightingProvider,
				mainThreadExecutor,
				this::markChunkNeedsSaving
		);
	}

	private void markChunkNeedsSaving(ChunkPos pos) {
		this.chunksToSave.add(pos.toLong());
	}

	protected ChunkGenerator getChunkGenerator() {
		return this.generationContext.generator();
	}

	protected StructurePlacementCalculator getStructurePlacementCalculator() {
		return this.structurePlacementCalculator;
	}

	protected NoiseConfig getNoiseConfig() {
		return this.noiseConfig;
	}

	public boolean isTracked(ServerPlayerEntity player, int chunkX, int chunkZ) {
		return player.getChunkFilter().isWithinDistance(chunkX, chunkZ)
				&& !player.networkHandler.chunkDataSender.isInNextBatch(ChunkPos.toLong(chunkX, chunkZ));
	}

	private boolean isOnTrackEdge(ServerPlayerEntity player, int chunkX, int chunkZ) {
		if (!this.isTracked(player, chunkX, chunkZ)) {
			return false;
		}
		else {
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					if ((i != 0 || j != 0) && !this.isTracked(player, chunkX + i, chunkZ + j)) {
						return true;
					}
				}
			}

			return false;
		}
	}

	protected ServerLightingProvider getLightingProvider() {
		return this.lightingProvider;
	}

	public @Nullable ChunkHolder getCurrentChunkHolder(long pos) {
		return (ChunkHolder) this.currentChunkHolders.get(pos);
	}

	protected @Nullable ChunkHolder getChunkHolder(long pos) {
		return (ChunkHolder) this.chunkHolders.get(pos);
	}

	public @Nullable ChunkStatus getStatus(long chunkPos) {
		ChunkHolder chunkHolder = this.getChunkHolder(chunkPos);
		return chunkHolder != null ? chunkHolder.getLatestStatus() : null;
	}

	protected IntSupplier getCompletedLevelSupplier(long pos) {
		return () -> {
			ChunkHolder chunkHolder = this.getChunkHolder(pos);
			return chunkHolder == null ? LevelPrioritizedQueue.LEVEL_COUNT - 1 : Math.min(
					chunkHolder.getCompletedLevel(),
					LevelPrioritizedQueue.LEVEL_COUNT - 1
			);
		};
	}

	public String getChunkLoadingDebugInfo(ChunkPos chunkPos) {
		ChunkHolder chunkHolder = this.getChunkHolder(chunkPos.toLong());
		if (chunkHolder == null) {
			return "null";
		}
		else {
			String string = chunkHolder.getLevel() + "\n";
			ChunkStatus chunkStatus = chunkHolder.getLatestStatus();
			Chunk chunk = chunkHolder.getLatest();
			if (chunkStatus != null) {
				string = string + "St: §" + chunkStatus.getIndex() + chunkStatus + "§r\n";
			}

			if (chunk != null) {
				string = string + "Ch: §" + chunk.getStatus().getIndex() + chunk.getStatus() + "§r\n";
			}

			ChunkLevelType chunkLevelType = chunkHolder.getLevelType();
			string = string + '§' + chunkLevelType.ordinal() + chunkLevelType;
			return string + "§r";
		}
	}

	CompletableFuture<OptionalChunk<List<Chunk>>> getRegion(
			ChunkHolder centerChunk,
			int margin,
			IntFunction<ChunkStatus> distanceToStatus
	) {
		if (margin == 0) {
			ChunkStatus chunkStatus = distanceToStatus.apply(0);
			return centerChunk.load(chunkStatus, this).thenApply(chunk -> chunk.map(List::of));
		}
		else {
			int i = MathHelper.square(margin * 2 + 1);
			List<CompletableFuture<OptionalChunk<Chunk>>> list = new ArrayList<>(i);
			ChunkPos chunkPos = centerChunk.getPos();

			for (int j = -margin; j <= margin; j++) {
				for (int k = -margin; k <= margin; k++) {
					int l = Math.max(Math.abs(k), Math.abs(j));
					long m = ChunkPos.toLong(chunkPos.x + k, chunkPos.z + j);
					ChunkHolder chunkHolder = this.getCurrentChunkHolder(m);
					if (chunkHolder == null) {
						return UNLOADED_CHUNKS_FUTURE;
					}

					ChunkStatus chunkStatus2 = distanceToStatus.apply(l);
					list.add(chunkHolder.load(chunkStatus2, this));
				}
			}

			return Util.combineSafe(list).thenApply(chunks -> {
				List<Chunk> listx = new ArrayList<>(chunks.size());

				for (OptionalChunk<Chunk> optionalChunk : chunks) {
					if (optionalChunk == null) {
						throw this.crash(
								new IllegalStateException("At least one of the chunk futures were null"),
								"n/a"
						);
					}

					Chunk chunk = optionalChunk.orElse(null);
					if (chunk == null) {
						return UNLOADED_CHUNKS;
					}

					listx.add(chunk);
				}

				return OptionalChunk.of(listx);
			});
		}
	}

	/**
	 * Crash.
	 *
	 * @param exception exception
	 * @param details details
	 *
	 * @return CrashException — результат операции
	 */
	public CrashException crash(IllegalStateException exception, String details) {
		StringBuilder stringBuilder = new StringBuilder();
		Consumer<ChunkHolder> consumer = chunkHolder -> chunkHolder.enumerateFutures()
		                                                           .forEach(
				                                                           pair -> {
					                                                           ChunkStatus
							                                                           chunkStatus =
							                                                           (ChunkStatus) pair.getFirst();
					                                                           CompletableFuture<OptionalChunk<Chunk>>
							                                                           completableFuture =
							                                                           (CompletableFuture<OptionalChunk<Chunk>>) pair.getSecond();
					                                                           if (completableFuture != null
							                                                           && completableFuture.isDone()
							                                                           && completableFuture.join()
							                                                           == null) {
						                                                           stringBuilder
								                                                           .append(chunkHolder.getPos())
								                                                           .append(" - status: ")
								                                                           .append(chunkStatus)
								                                                           .append(" future: ")
								                                                           .append(completableFuture)
								                                                           .append(System.lineSeparator());
					                                                           }
				                                                           }
		                                                           );
		stringBuilder.append("Updating:").append(System.lineSeparator());
		this.currentChunkHolders.values().forEach(consumer);
		stringBuilder.append("Visible:").append(System.lineSeparator());
		this.chunkHolders.values().forEach(consumer);
		CrashReport crashReport = CrashReport.create(exception, "Chunk loading");
		CrashReportSection crashReportSection = crashReport.addElement("Chunk loading");
		crashReportSection.add("Details", details);
		crashReportSection.add("Futures", stringBuilder);
		return new CrashException(crashReport);
	}

	/**
	 * Make chunk entities tickable.
	 *
	 * @param holder holder
	 *
	 * @return CompletableFuture> — результат операции
	 */
	public CompletableFuture<OptionalChunk<WorldChunk>> makeChunkEntitiesTickable(ChunkHolder holder) {
		return this
				.getRegion(holder, 2, distance -> ChunkStatus.FULL)
				.thenApply(chunk -> chunk.map(chunks -> (WorldChunk) chunks.get(chunks.size() / 2)));
	}

	@Nullable ChunkHolder setLevel(long pos, int level, @Nullable ChunkHolder holder, int i) {
		if (!ChunkLevels.isAccessible(i) && !ChunkLevels.isAccessible(level)) {
			return holder;
		}
		else {
			if (holder != null) {
				holder.setLevel(level);
			}

			if (holder != null) {
				if (!ChunkLevels.isAccessible(level)) {
					this.unloadedChunks.add(pos);
				}
				else {
					this.unloadedChunks.remove(pos);
				}
			}

			if (ChunkLevels.isAccessible(level) && holder == null) {
				holder = (ChunkHolder) this.chunksToUnload.remove(pos);
				if (holder != null) {
					holder.setLevel(level);
				}
				else {
					holder =
							new ChunkHolder(
									new ChunkPos(pos),
									level,
									this.world,
									this.lightingProvider,
									this::updateLevel,
									this
							);
				}

				this.currentChunkHolders.put(pos, holder);
				this.chunkHolderListDirty = true;
			}

			return holder;
		}
	}

	private void updateLevel(ChunkPos pos, IntSupplier levelGetter, int targetLevel, IntConsumer levelSetter) {
		this.worldGenScheduler.updateLevel(pos, levelGetter, targetLevel, levelSetter);
		this.lightScheduler.updateLevel(pos, levelGetter, targetLevel, levelSetter);
	}

	@Override
	public void close() throws IOException {
		try {
			this.worldGenScheduler.close();
			this.lightScheduler.close();
			this.pointOfInterestStorage.close();
		}
		finally {
			super.close();
		}
	}

	/**
	 * Save.
	 *
	 * @param flush flush
	 */
	protected void save(boolean flush) {
		if (flush) {
			List<ChunkHolder>
					list =
					this.chunkHolders
							.values()
							.stream()
							.filter(ChunkHolder::isAccessible)
							.peek(ChunkHolder::updateAccessibleStatus)
							.toList();
			MutableBoolean mutableBoolean = new MutableBoolean();

			do {
				mutableBoolean.setFalse();
				list
						.stream()
						.map(holder -> {
							this.mainThreadExecutor.runTasks(holder::isSavable);
							return holder.getLatest();
						})
						.filter(chunk -> chunk instanceof WrapperProtoChunk || chunk instanceof WorldChunk)
						.filter(this::save)
						.forEach(chunk -> mutableBoolean.setTrue());
			}
			while (mutableBoolean.isTrue());

			this.pointOfInterestStorage.save();
			this.unloadChunks(() -> true);
			this.completeAll(true).join();
		}
		else {
			this.chunkToNextSaveTimeMs.clear();
			long l = Util.getMeasuringTimeMs();
			ObjectIterator var4 = this.chunkHolders.values().iterator();

			while (var4.hasNext()) {
				ChunkHolder chunkHolder = (ChunkHolder) var4.next();
				this.save(chunkHolder, l);
			}
		}
	}

	/**
	 * Tick.
	 *
	 * @param shouldKeepTicking should keep ticking
	 */
	protected void tick(BooleanSupplier shouldKeepTicking) {
		Profiler profiler = Profilers.get();
		profiler.push("poi");
		this.pointOfInterestStorage.tick(shouldKeepTicking);
		profiler.swap("chunk_unload");
		if (!this.world.isSavingDisabled()) {
			this.unloadChunks(shouldKeepTicking);
		}

		profiler.pop();
	}

	/**
	 * Определяет, следует ли delay shutdown.
	 *
	 * @return boolean — результат операции
	 */
	public boolean shouldDelayShutdown() {
		return this.lightingProvider.hasUpdates()
				|| !this.chunksToUnload.isEmpty()
				|| !this.currentChunkHolders.isEmpty()
				|| this.pointOfInterestStorage.hasUnsavedElements()
				|| !this.unloadedChunks.isEmpty()
				|| !this.unloadTaskQueue.isEmpty()
				|| this.worldGenScheduler.shouldDelayShutdown()
				|| this.lightScheduler.shouldDelayShutdown()
				|| this.levelManager.shouldDelayShutdown();
	}

	private void unloadChunks(BooleanSupplier shouldKeepTicking) {
		for (LongIterator longIterator = this.unloadedChunks.iterator();
		     longIterator.hasNext();
		     longIterator.remove()) {
			long l = longIterator.nextLong();
			ChunkHolder chunkHolder = (ChunkHolder) this.currentChunkHolders.get(l);
			if (chunkHolder != null) {
				this.currentChunkHolders.remove(l);
				this.chunksToUnload.put(l, chunkHolder);
				this.chunkHolderListDirty = true;
				this.tryUnloadChunk(l, chunkHolder);
			}
		}

		int i = Math.max(0, this.unloadTaskQueue.size() - 2000);

		Runnable runnable;
		while ((i > 0 || shouldKeepTicking.getAsBoolean()) && (runnable = this.unloadTaskQueue.poll()) != null) {
			i--;
			runnable.run();
		}

		this.saveChunks(shouldKeepTicking);
	}

	private void saveChunks(BooleanSupplier shouldKeepTicking) {
		long l = Util.getMeasuringTimeMs();
		int i = 0;
		LongIterator longIterator = this.chunksToSave.iterator();

		while (i < MAX_CHUNKS_SAVED_PER_TICK && this.chunksBeingSavedCount.get() < MAX_CONCURRENT_SAVES && shouldKeepTicking.getAsBoolean()
				&& longIterator.hasNext()) {
			long m = longIterator.nextLong();
			ChunkHolder chunkHolder = (ChunkHolder) this.chunkHolders.get(m);
			Chunk chunk = chunkHolder != null ? chunkHolder.getLatest() : null;
			if (chunk == null || !chunk.needsSaving()) {
				longIterator.remove();
			}
			else if (this.save(chunkHolder, l)) {
				i++;
				longIterator.remove();
			}
		}
	}

	private void tryUnloadChunk(long pos, ChunkHolder chunk) {
		CompletableFuture<?> completableFuture = chunk.getSavingFuture();
		completableFuture.thenRunAsync(
				() -> {
					CompletableFuture<?> completableFuture2 = chunk.getSavingFuture();
					if (completableFuture2 != completableFuture) {
						this.tryUnloadChunk(pos, chunk);
					}
					else {
						Chunk chunkx = chunk.getLatest();
						if (this.chunksToUnload.remove(pos, chunk) && chunkx != null) {
							if (chunkx instanceof WorldChunk worldChunk) {
								worldChunk.setLoadedToWorld(false);
							}

							this.save(chunkx);
							if (chunkx instanceof WorldChunk worldChunk) {
								this.world.unloadEntities(worldChunk);
							}

							this.lightingProvider.updateChunkStatus(chunkx.getPos());
							this.lightingProvider.tick();
							this.chunkToNextSaveTimeMs.remove(chunkx.getPos().toLong());
						}
					}
				}, this.unloadTaskQueue::add
		).whenComplete((void_, throwable) -> {
			if (throwable != null) {
				LOGGER.error("Failed to save chunk {}", chunk.getPos(), throwable);
			}
		});
	}

	/**
	 * Обновляет holder map.
	 *
	 * @return boolean — результат операции
	 */
	protected boolean updateHolderMap() {
		if (!this.chunkHolderListDirty) {
			return false;
		}
		else {
			this.chunkHolders = this.currentChunkHolders.clone();
			this.chunkHolderListDirty = false;
			return true;
		}
	}

	private CompletableFuture<Chunk> loadChunk(ChunkPos pos) {
		CompletableFuture<Optional<SerializedChunk>> completableFuture = this.getUpdatedChunkNbt(pos).thenApplyAsync(
				optional -> optional.map(nbtCompound -> {
					SerializedChunk
							serializedChunk =
							SerializedChunk.fromNbt(this.world, this.world.getPalettesFactory(), nbtCompound);
					if (serializedChunk == null) {
						LOGGER.error("Chunk file at {} is missing level data, skipping", pos);
					}

					return serializedChunk;
				}), Util.getMainWorkerExecutor().named("parseChunk")
		);
		CompletableFuture<?> completableFuture2 = this.pointOfInterestStorage.load(pos);
		return completableFuture.<Object, Optional<SerializedChunk>>thenCombine(
				                        (CompletionStage<? extends Object>) completableFuture2, (optional, object) -> optional
		                        )
		                        .thenApplyAsync(
				                        nbt -> {
					                        Profilers.get().visit("chunkLoad");
					                        if (nbt.isPresent()) {
						                        Chunk
								                        chunk =
								                        nbt
										                        .get()
										                        .convert(
												                        this.world,
												                        this.pointOfInterestStorage,
												                        this.getStorageKey(),
												                        pos
										                        );
						                        this.mark(pos, chunk.getStatus().getChunkType());
						                        return chunk;
					                        }
					                        else {
						                        return this.getProtoChunk(pos);
					                        }
				                        }, this.mainThreadExecutor
		                        )
		                        .exceptionallyAsync(
				                        throwable -> this.recoverFromException(throwable, pos),
				                        this.mainThreadExecutor
		                        );
	}

	private Chunk recoverFromException(Throwable throwable, ChunkPos chunkPos) {
		Throwable
				throwable2 =
				throwable instanceof CompletionException completionException ? completionException.getCause()
				                                                             : throwable;
		Throwable
				throwable3 =
				throwable2 instanceof CrashException crashException ? crashException.getCause() : throwable2;
		boolean bl = throwable3 instanceof Error;
		boolean bl2 = throwable3 instanceof IOException || throwable3 instanceof NbtException;
		if (!bl) {
			if (!bl2) {
			}

			this.world.getServer().onChunkLoadFailure(throwable3, this.getStorageKey(), chunkPos);
			return this.getProtoChunk(chunkPos);
		}
		else {
			CrashReport crashReport = CrashReport.create(throwable, "Exception loading chunk");
			CrashReportSection crashReportSection = crashReport.addElement("Chunk being loaded");
			crashReportSection.add("pos", chunkPos);
			this.markAsProtoChunk(chunkPos);
			throw new CrashException(crashReport);
		}
	}

	private Chunk getProtoChunk(ChunkPos chunkPos) {
		this.markAsProtoChunk(chunkPos);
		return new ProtoChunk(chunkPos, UpgradeData.NO_UPGRADE_DATA, this.world, this.world.getPalettesFactory(), null);
	}

	private void markAsProtoChunk(ChunkPos pos) {
		this.chunkToType.put(pos.toLong(), (byte) -1);
	}

	private byte mark(ChunkPos pos, ChunkType type) {
		return this.chunkToType.put(pos.toLong(), (byte) (type == ChunkType.PROTOCHUNK ? -1 : 1));
	}

	@Override
	public AbstractChunkHolder acquire(long pos) {
		ChunkHolder chunkHolder = (ChunkHolder) this.currentChunkHolders.get(pos);
		chunkHolder.incrementRefCount();
		return chunkHolder;
	}

	@Override
	public void release(AbstractChunkHolder chunkHolder) {
		chunkHolder.decrementRefCount();
	}

	@Override
	public CompletableFuture<Chunk> generate(
			AbstractChunkHolder chunkHolder,
			ChunkGenerationStep step,
			BoundedRegionArray<AbstractChunkHolder> chunks
	) {
		ChunkPos chunkPos = chunkHolder.getPos();
		if (step.targetStatus() == ChunkStatus.EMPTY) {
			return this.loadChunk(chunkPos);
		}
		else {
			try {
				AbstractChunkHolder abstractChunkHolder = chunks.get(chunkPos.x, chunkPos.z);
				Chunk chunk = abstractChunkHolder.getUncheckedOrNull(step.targetStatus().getPrevious());
				if (chunk == null) {
					throw new IllegalStateException("Parent chunk missing");
				}
				else {
					return step.run(this.generationContext, chunks, chunk);
				}
			}
			catch (Exception var8) {
				var8.getStackTrace();
				CrashReport crashReport = CrashReport.create(var8, "Exception generating new chunk");
				CrashReportSection crashReportSection = crashReport.addElement("Chunk to be generated");
				crashReportSection.add("Status being generated", () -> step.targetStatus().getId());
				crashReportSection.add("Location", String.format(Locale.ROOT, "%d,%d", chunkPos.x, chunkPos.z));
				crashReportSection.add("Position hash", ChunkPos.toLong(chunkPos.x, chunkPos.z));
				crashReportSection.add("Generator", this.getChunkGenerator());
				this.mainThreadExecutor.execute(() -> {
					throw new CrashException(crashReport);
				});
				throw new CrashException(crashReport);
			}
		}
	}

	@Override
	public ChunkLoader createLoader(ChunkStatus requestedStatus, ChunkPos pos) {
		ChunkLoader chunkLoader = ChunkLoader.create(this, requestedStatus, pos);
		this.loaders.add(chunkLoader);
		return chunkLoader;
	}

	private void schedule(ChunkLoader loader) {
		AbstractChunkHolder abstractChunkHolder = loader.getHolder();
		this.worldGenScheduler.add(
				() -> {
					CompletableFuture<?> completableFuture = loader.run();
					if (completableFuture != null) {
						completableFuture.thenRun(() -> this.schedule(loader));
					}
				}, abstractChunkHolder.getPos().toLong(), abstractChunkHolder::getCompletedLevel
		);
	}

	@Override
	public void updateChunks() {
		this.loaders.forEach(this::schedule);
		this.loaders.clear();
	}

	/**
	 * Make chunk tickable.
	 *
	 * @param holder holder
	 *
	 * @return CompletableFuture> — результат операции
	 */
	public CompletableFuture<OptionalChunk<WorldChunk>> makeChunkTickable(ChunkHolder holder) {
		CompletableFuture<OptionalChunk<List<Chunk>>>
				completableFuture =
				this.getRegion(holder, 1, distance -> ChunkStatus.FULL);
		return completableFuture.thenApplyAsync(
				optionalChunk -> optionalChunk.map(chunks -> {
					WorldChunk worldChunk = (WorldChunk) chunks.get(chunks.size() / 2);
					worldChunk.runPostProcessing(this.world);
					this.world.disableTickSchedulers(worldChunk);
					CompletableFuture<?> completableFuturex = holder.getPostProcessingFuture();
					if (completableFuturex.isDone()) {
						this.sendToPlayers(holder, worldChunk);
					}
					else {
						completableFuturex.thenAcceptAsync(
								object -> this.sendToPlayers(holder, worldChunk),
								this.mainThreadExecutor
						);
					}

					return worldChunk;
				}), this.mainThreadExecutor
		);
	}

	private void sendToPlayers(ChunkHolder chunkHolder, WorldChunk chunk) {
		ChunkPos chunkPos = chunk.getPos();

		for (ServerPlayerEntity serverPlayerEntity : this.playerChunkWatchingManager.getPlayersWatchingChunk()) {
			if (serverPlayerEntity.getChunkFilter().isWithinDistance(chunkPos)) {
				track(serverPlayerEntity, chunk);
			}
		}

		this.world.getChunkManager().markForUpdate(chunkHolder);
		this.world.getSubscriptionTracker().trackChunk(chunk);
	}

	/**
	 * Make chunk accessible.
	 *
	 * @param holder holder
	 *
	 * @return CompletableFuture> — результат операции
	 */
	public CompletableFuture<OptionalChunk<WorldChunk>> makeChunkAccessible(ChunkHolder holder) {
		return this.getRegion(holder, 1, ChunkLevels::getStatusForAdditionalLevel)
		           .thenApply(optionalChunks -> optionalChunks.map(chunks -> (WorldChunk) chunks.get(
				           chunks.size() / 2)));
	}

	public Stream<ChunkHolder> getChunkHolders(ChunkStatus status) {
		int i = ChunkLevels.getLevelFromStatus(status);
		return this.chunkHolders.values().stream().filter(holder -> holder.getLevel() <= i);
	}

	private boolean save(ChunkHolder chunkHolder, long currentTime) {
		if (chunkHolder.isAccessible() && chunkHolder.isSavable()) {
			Chunk chunk = chunkHolder.getLatest();
			if (!(chunk instanceof WrapperProtoChunk) && !(chunk instanceof WorldChunk)) {
				return false;
			}
			else if (!chunk.needsSaving()) {
				return false;
			}
			else {
				long l = chunk.getPos().toLong();
				long m = this.chunkToNextSaveTimeMs.getOrDefault(l, -1L);
				if (currentTime < m) {
					return false;
				}
				else {
					boolean bl = this.save(chunk);
					chunkHolder.updateAccessibleStatus();
					if (bl) {
						this.chunkToNextSaveTimeMs.put(l, currentTime + CHUNK_SAVE_COOLDOWN_MS);
					}

					return bl;
				}
			}
		}
		else {
			return false;
		}
	}

	private boolean save(Chunk chunk) {
		this.pointOfInterestStorage.saveChunk(chunk.getPos());
		if (!chunk.tryMarkSaved()) {
			return false;
		}
		else {
			ChunkPos chunkPos = chunk.getPos();

			try {
				ChunkStatus chunkStatus = chunk.getStatus();
				if (chunkStatus.getChunkType() != ChunkType.LEVELCHUNK) {
					if (this.isLevelChunk(chunkPos)) {
						return false;
					}

					if (chunkStatus == ChunkStatus.EMPTY && chunk
							.getStructureStarts()
							.values()
							.stream()
							.noneMatch(StructureStart::hasChildren)) {
						return false;
					}
				}

				Profilers.get().visit("chunkSave");
				this.chunksBeingSavedCount.incrementAndGet();
				SerializedChunk serializedChunk = SerializedChunk.fromChunk(this.world, chunk);
				CompletableFuture<NbtCompound>
						completableFuture =
						CompletableFuture.supplyAsync(serializedChunk::serialize, Util.getMainWorkerExecutor());
				this.set(chunkPos, completableFuture::join).handle((void_, exceptionx) -> {
					if (exceptionx != null) {
						this.world.getServer().onChunkSaveFailure(exceptionx, this.getStorageKey(), chunkPos);
					}

					this.chunksBeingSavedCount.decrementAndGet();
					return null;
				});
				this.mark(chunkPos, chunkStatus.getChunkType());
				return true;
			}
			catch (Exception var6) {
				this.world.getServer().onChunkSaveFailure(var6, this.getStorageKey(), chunkPos);
				return false;
			}
		}
	}

	private boolean isLevelChunk(ChunkPos pos) {
		byte b = this.chunkToType.get(pos.toLong());
		if (b != 0) {
			return b == 1;
		}
		else {
			NbtCompound nbtCompound;
			try {
				nbtCompound = this.getUpdatedChunkNbt(pos).join().orElse(null);
				if (nbtCompound == null) {
					this.markAsProtoChunk(pos);
					return false;
				}
			}
			catch (Exception var5) {
				LOGGER.error("Failed to read chunk {}", pos, var5);
				this.markAsProtoChunk(pos);
				return false;
			}

			ChunkType chunkType = SerializedChunk.getChunkStatus(nbtCompound).getChunkType();
			return this.mark(pos, chunkType) == 1;
		}
	}

	protected void setViewDistance(int watchDistance) {
		int i = MathHelper.clamp(watchDistance, 2, MAX_VIEW_DISTANCE);
		if (i != this.watchDistance) {
			this.watchDistance = i;
			this.levelManager.setWatchDistance(this.watchDistance);

			for (ServerPlayerEntity serverPlayerEntity : this.playerChunkWatchingManager.getPlayersWatchingChunk()) {
				this.sendWatchPackets(serverPlayerEntity);
			}
		}
	}

	int getViewDistance(ServerPlayerEntity player) {
		return MathHelper.clamp(player.getViewDistance(), 2, this.watchDistance);
	}

	private void track(ServerPlayerEntity player, ChunkPos pos) {
		WorldChunk worldChunk = this.getPostProcessedChunk(pos.toLong());
		if (worldChunk != null) {
			track(player, worldChunk);
		}
	}

	private static void track(ServerPlayerEntity player, WorldChunk chunk) {
		player.networkHandler.chunkDataSender.add(chunk);
	}

	private static void untrack(ServerPlayerEntity player, ChunkPos pos) {
		player.networkHandler.chunkDataSender.unload(player, pos);
	}

	public @Nullable WorldChunk getPostProcessedChunk(long pos) {
		ChunkHolder chunkHolder = this.getChunkHolder(pos);
		return chunkHolder == null ? null : chunkHolder.getPostProcessedChunk();
	}

	public int getLoadedChunkCount() {
		return this.chunkHolders.size();
	}

	public ChunkLevelManager getLevelManager() {
		return this.levelManager;
	}

	void dump(Writer writer) throws IOException {
		CsvWriter csvWriter = CsvWriter.makeHeader()
		                               .addColumn("x")
		                               .addColumn("z")
		                               .addColumn("level")
		                               .addColumn("in_memory")
		                               .addColumn("status")
		                               .addColumn("full_status")
		                               .addColumn("accessible_ready")
		                               .addColumn("ticking_ready")
		                               .addColumn("entity_ticking_ready")
		                               .addColumn("ticket")
		                               .addColumn("spawning")
		                               .addColumn("block_entity_count")
		                               .addColumn("ticking_ticket")
		                               .addColumn("ticking_level")
		                               .addColumn("block_ticks")
		                               .addColumn("fluid_ticks")
		                               .startBody(writer);
		ObjectBidirectionalIterator var3 = this.chunkHolders.long2ObjectEntrySet().iterator();

		while (var3.hasNext()) {
			Entry<ChunkHolder> entry = (Entry<ChunkHolder>) var3.next();
			long l = entry.getLongKey();
			ChunkPos chunkPos = new ChunkPos(l);
			ChunkHolder chunkHolder = (ChunkHolder) entry.getValue();
			Optional<Chunk> optional = Optional.ofNullable(chunkHolder.getLatest());
			Optional<WorldChunk>
					optional2 =
					optional.flatMap(chunk -> chunk instanceof WorldChunk ? Optional.of((WorldChunk) chunk)
					                                                      : Optional.empty());
			csvWriter.printRow(
					chunkPos.x,
					chunkPos.z,
					chunkHolder.getLevel(),
					optional.isPresent(),
					optional.map(Chunk::getStatus).orElse(null),
					optional2.map(WorldChunk::getLevelType).orElse(null),
					getFutureStatus(chunkHolder.getAccessibleFuture()),
					getFutureStatus(chunkHolder.getTickingFuture()),
					getFutureStatus(chunkHolder.getEntityTickingFuture()),
					this.ticketManager.getDebugString(l, false),
					this.shouldTick(chunkPos),
					optional2.<Integer>map(chunk -> chunk.getBlockEntities().size()).orElse(0),
					this.ticketManager.getDebugString(l, true),
					this.levelManager.getLevel(l, true),
					optional2.<Integer>map(chunk -> chunk.getBlockTickScheduler().getTickCount()).orElse(0),
					optional2.<Integer>map(chunk -> chunk.getFluidTickScheduler().getTickCount()).orElse(0)
			);
		}
	}

	private static String getFutureStatus(CompletableFuture<OptionalChunk<WorldChunk>> future) {
		try {
			OptionalChunk<WorldChunk> optionalChunk = future.getNow(null);
			if (optionalChunk != null) {
				return optionalChunk.isPresent() ? "done" : "unloaded";
			}
			else {
				return "not completed";
			}
		}
		catch (CompletionException var2) {
			return "failed " + var2.getCause().getMessage();
		}
		catch (CancellationException var3) {
			return "cancelled";
		}
	}

	private CompletableFuture<Optional<NbtCompound>> getUpdatedChunkNbt(ChunkPos chunkPos) {
		return this
				.getNbt(chunkPos)
				.thenApplyAsync(
						nbt -> nbt.map(this::updateChunkNbt),
						Util.getMainWorkerExecutor().named("upgradeChunk")
				);
	}

	private NbtCompound updateChunkNbt(NbtCompound nbt) {
		return this.updateChunkNbt(
				nbt,
				-1,
				getContextNbt(this.world.getRegistryKey(), this.getChunkGenerator().getCodecKey())
		);
	}

	public static NbtCompound getContextNbt(
			RegistryKey<World> dimensionKey,
			Optional<RegistryKey<MapCodec<? extends ChunkGenerator>>> chunkGeneratorKey
	) {
		NbtCompound nbtCompound = new NbtCompound();
		nbtCompound.putString("dimension", dimensionKey.getValue().toString());
		chunkGeneratorKey.ifPresent(generator -> nbtCompound.putString("generator", generator.getValue().toString()));
		return nbtCompound;
	}

	void collectSpawningChunks(List<WorldChunk> chunks) {
		LongIterator longIterator = this.levelManager.iterateChunkPosToTick();

		while (longIterator.hasNext()) {
			ChunkHolder chunkHolder = (ChunkHolder) this.chunkHolders.get(longIterator.nextLong());
			if (chunkHolder != null) {
				WorldChunk worldChunk = chunkHolder.getWorldChunk();
				if (worldChunk != null && this.isAnyPlayerTicking(chunkHolder.getPos())) {
					chunks.add(worldChunk);
				}
			}
		}
	}

	void forEachBlockTickingChunk(Consumer<WorldChunk> chunkConsumer) {
		this.levelManager.forEachBlockTickingChunk(chunkPos -> {
			ChunkHolder chunkHolder = (ChunkHolder) this.chunkHolders.get(chunkPos);
			if (chunkHolder != null) {
				WorldChunk worldChunk = chunkHolder.getWorldChunk();
				if (worldChunk != null) {
					chunkConsumer.accept(worldChunk);
				}
			}
		});
	}

	boolean shouldTick(ChunkPos pos) {
		TriState triState = this.levelManager.shouldTick(pos.toLong());
		return triState == TriState.DEFAULT ? this.isAnyPlayerTicking(pos) : triState.asBoolean(true);
	}

	boolean isAnyNonSpectatorWithin(BlockPos pos, int distance) {
		Vec3d vec3d = new Vec3d(pos);

		for (ServerPlayerEntity serverPlayerEntity : this.playerChunkWatchingManager.getPlayersWatchingChunk()) {
			if (this.isNonSpectatorWithinDistance(serverPlayerEntity, vec3d, distance)) {
				return true;
			}
		}

		return false;
	}

	private boolean isAnyPlayerTicking(ChunkPos pos) {
		for (ServerPlayerEntity serverPlayerEntity : this.playerChunkWatchingManager.getPlayersWatchingChunk()) {
			if (this.canTickChunk(serverPlayerEntity, pos)) {
				return true;
			}
		}

		return false;
	}

	public List<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos pos) {
		long l = pos.toLong();
		if (!this.levelManager.shouldTick(l).asBoolean(true)) {
			return List.of();
		}
		else {
			Builder<ServerPlayerEntity> builder = ImmutableList.builder();

			for (ServerPlayerEntity serverPlayerEntity : this.playerChunkWatchingManager.getPlayersWatchingChunk()) {
				if (this.canTickChunk(serverPlayerEntity, pos)) {
					builder.add(serverPlayerEntity);
				}
			}

			return builder.build();
		}
	}

	private boolean canTickChunk(ServerPlayerEntity player, ChunkPos pos) {
		if (player.isSpectator()) {
			return false;
		}
		else {
			double d = getSquaredDistance(pos, player.getEntityPos());
			return d < 16384.0;
		}
	}

	private boolean isNonSpectatorWithinDistance(ServerPlayerEntity player, Vec3d pos, int distance) {
		if (player.isSpectator()) {
			return false;
		}
		else {
			double d = player.getEntityPos().distanceTo(pos);
			return d < distance;
		}
	}

	private static double getSquaredDistance(ChunkPos chunkPos, Vec3d pos) {
		double d = ChunkSectionPos.getOffsetPos(chunkPos.x, 8);
		double e = ChunkSectionPos.getOffsetPos(chunkPos.z, 8);
		double f = d - pos.x;
		double g = e - pos.z;
		return f * f + g * g;
	}

	private boolean doesNotGenerateChunks(ServerPlayerEntity player) {
		return player.isSpectator() && !this.world.getGameRules().getValue(GameRules.SPECTATORS_GENERATE_CHUNKS);
	}

	void handlePlayerAddedOrRemoved(ServerPlayerEntity player, boolean added) {
		boolean bl = this.doesNotGenerateChunks(player);
		boolean bl2 = this.playerChunkWatchingManager.isWatchInactive(player);
		if (added) {
			this.playerChunkWatchingManager.add(player, bl);
			this.updateWatchedSection(player);
			if (!bl) {
				this.levelManager.handleChunkEnter(ChunkSectionPos.from(player), player);
			}

			player.setChunkFilter(ChunkFilter.IGNORE_ALL);
			this.sendWatchPackets(player);
		}
		else {
			ChunkSectionPos chunkSectionPos = player.getWatchedSection();
			this.playerChunkWatchingManager.remove(player);
			if (!bl2) {
				this.levelManager.handleChunkLeave(chunkSectionPos, player);
			}

			this.sendWatchPackets(player, ChunkFilter.IGNORE_ALL);
		}
	}

	private void updateWatchedSection(ServerPlayerEntity player) {
		ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(player);
		player.setWatchedSection(chunkSectionPos);
	}

	/**
	 * Обновляет position.
	 *
	 * @param player player
	 */
	public void updatePosition(ServerPlayerEntity player) {
		ObjectIterator chunkSectionPos = this.entityTrackers.values().iterator();

		while (chunkSectionPos.hasNext()) {
			ServerChunkLoadingManager.EntityTracker
					entityTracker =
					(ServerChunkLoadingManager.EntityTracker) chunkSectionPos.next();
			if (entityTracker.entity == player) {
				entityTracker.updateTrackedStatus(this.world.getPlayers());
			}
			else {
				entityTracker.updateTrackedStatus(player);
			}
		}

		ChunkSectionPos chunkSectionPosx = player.getWatchedSection();
		ChunkSectionPos chunkSectionPos2 = ChunkSectionPos.from(player);
		boolean bl = this.playerChunkWatchingManager.isWatchDisabled(player);
		boolean bl2 = this.doesNotGenerateChunks(player);
		boolean bl3 = chunkSectionPosx.asLong() != chunkSectionPos2.asLong();
		if (bl3 || bl != bl2) {
			this.updateWatchedSection(player);
			if (!bl) {
				this.levelManager.handleChunkLeave(chunkSectionPosx, player);
			}

			if (!bl2) {
				this.levelManager.handleChunkEnter(chunkSectionPos2, player);
			}

			if (!bl && bl2) {
				this.playerChunkWatchingManager.disableWatch(player);
			}

			if (bl && !bl2) {
				this.playerChunkWatchingManager.enableWatch(player);
			}

			this.sendWatchPackets(player);
		}
	}

	private void sendWatchPackets(ServerPlayerEntity player) {
		ChunkPos chunkPos = player.getChunkPos();
		int i = this.getViewDistance(player);
		if (!(player.getChunkFilter() instanceof ChunkFilter.Cylindrical cylindrical && cylindrical
				.center()
				.equals(chunkPos) && cylindrical.viewDistance() == i
		)
		) {
			this.sendWatchPackets(player, ChunkFilter.cylindrical(chunkPos, i));
		}
	}

	private void sendWatchPackets(ServerPlayerEntity player, ChunkFilter chunkFilter) {
		if (player.getEntityWorld() == this.world) {
			ChunkFilter chunkFilter2 = player.getChunkFilter();
			if (chunkFilter instanceof ChunkFilter.Cylindrical cylindrical
					&& !(chunkFilter2 instanceof ChunkFilter.Cylindrical cylindrical2 && cylindrical2
					.center()
					.equals(cylindrical.center())
			)) {
				player.networkHandler.sendPacket(new ChunkRenderDistanceCenterS2CPacket(
						cylindrical.center().x,
						cylindrical.center().z
				));
			}

			ChunkFilter.forEachChangedChunk(
					chunkFilter2,
					chunkFilter,
					chunkPos -> this.track(player, chunkPos),
					chunkPos -> untrack(player, chunkPos)
			);
			player.setChunkFilter(chunkFilter);
		}
	}

	@Override
	public List<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
		Set<ServerPlayerEntity> set = this.playerChunkWatchingManager.getPlayersWatchingChunk();
		Builder<ServerPlayerEntity> builder = ImmutableList.builder();

		for (ServerPlayerEntity serverPlayerEntity : set) {
			if (onlyOnWatchDistanceEdge && this.isOnTrackEdge(serverPlayerEntity, chunkPos.x, chunkPos.z)
					|| !onlyOnWatchDistanceEdge && this.isTracked(serverPlayerEntity, chunkPos.x, chunkPos.z)) {
				builder.add(serverPlayerEntity);
			}
		}

		return builder.build();
	}

	/**
	 * Загружает entity.
	 *
	 * @param entity entity
	 */
	protected void loadEntity(Entity entity) {
		if (!(entity instanceof EnderDragonPart)) {
			EntityType<?> entityType = entity.getType();
			int i = entityType.getMaxTrackDistance() * 16;
			if (i != 0) {
				int j = entityType.getTrackTickInterval();
				if (this.entityTrackers.containsKey(entity.getId())) {
					throw (IllegalStateException) Util.getFatalOrPause(new IllegalStateException(
							"Entity is already tracked!"));
				}
				else {
					ServerChunkLoadingManager.EntityTracker entityTracker = new ServerChunkLoadingManager.EntityTracker(
							entity, i, j, entityType.alwaysUpdateVelocity()
					);
					this.entityTrackers.put(entity.getId(), entityTracker);
					entityTracker.updateTrackedStatus(this.world.getPlayers());
					if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
						this.handlePlayerAddedOrRemoved(serverPlayerEntity, true);
						ObjectIterator var7 = this.entityTrackers.values().iterator();

						while (var7.hasNext()) {
							ServerChunkLoadingManager.EntityTracker
									entityTracker2 =
									(ServerChunkLoadingManager.EntityTracker) var7.next();
							if (entityTracker2.entity != serverPlayerEntity) {
								entityTracker2.updateTrackedStatus(serverPlayerEntity);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Unload entity.
	 *
	 * @param entity entity
	 */
	protected void unloadEntity(Entity entity) {
		if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
			this.handlePlayerAddedOrRemoved(serverPlayerEntity, false);
			ObjectIterator var3 = this.entityTrackers.values().iterator();

			while (var3.hasNext()) {
				ServerChunkLoadingManager.EntityTracker
						entityTracker =
						(ServerChunkLoadingManager.EntityTracker) var3.next();
				entityTracker.stopTracking(serverPlayerEntity);
			}
		}

		ServerChunkLoadingManager.EntityTracker
				entityTracker2 =
				(ServerChunkLoadingManager.EntityTracker) this.entityTrackers.remove(entity.getId());
		if (entityTracker2 != null) {
			entityTracker2.stopTracking();
		}
	}

	/**
	 * Выполняет тик обновления для entity movement.
	 */
	protected void tickEntityMovement() {
		for (ServerPlayerEntity serverPlayerEntity : this.playerChunkWatchingManager.getPlayersWatchingChunk()) {
			this.sendWatchPackets(serverPlayerEntity);
		}

		List<ServerPlayerEntity> list = Lists.newArrayList();
		List<ServerPlayerEntity> list2 = this.world.getPlayers();
		ObjectIterator var3 = this.entityTrackers.values().iterator();

		while (var3.hasNext()) {
			ServerChunkLoadingManager.EntityTracker
					entityTracker =
					(ServerChunkLoadingManager.EntityTracker) var3.next();
			ChunkSectionPos chunkSectionPos = entityTracker.trackedSection;
			ChunkSectionPos chunkSectionPos2 = ChunkSectionPos.from(entityTracker.entity);
			boolean bl = !Objects.equals(chunkSectionPos, chunkSectionPos2);
			if (bl) {
				entityTracker.updateTrackedStatus(list2);
				Entity entity = entityTracker.entity;
				if (entity instanceof ServerPlayerEntity) {
					list.add((ServerPlayerEntity) entity);
				}

				entityTracker.trackedSection = chunkSectionPos2;
			}

			if (bl || entityTracker.entity.velocityDirty || this.levelManager.shouldTickEntities(chunkSectionPos2
					.toChunkPos()
					.toLong())) {
				entityTracker.entry.tick();
			}
		}

		if (!list.isEmpty()) {
			var3 = this.entityTrackers.values().iterator();

			while (var3.hasNext()) {
				ServerChunkLoadingManager.EntityTracker
						entityTrackerx =
						(ServerChunkLoadingManager.EntityTracker) var3.next();
				entityTrackerx.updateTrackedStatus(list);
			}
		}
	}

	/**
	 * Отправляет to other nearby players.
	 *
	 * @param entity entity
	 * @param packet packet
	 */
	public void sendToOtherNearbyPlayers(Entity entity, Packet<? super ClientPlayPacketListener> packet) {
		ServerChunkLoadingManager.EntityTracker
				entityTracker =
				(ServerChunkLoadingManager.EntityTracker) this.entityTrackers.get(entity.getId());
		if (entityTracker != null) {
			entityTracker.sendToListeners(packet);
		}
	}

	public void sendToOtherNearbyPlayersIf(
			Entity entity,
			Packet<? super ClientPlayPacketListener> packet,
			Predicate<ServerPlayerEntity> predicate
	) {
		ServerChunkLoadingManager.EntityTracker
				entityTracker =
				(ServerChunkLoadingManager.EntityTracker) this.entityTrackers.get(entity.getId());
		if (entityTracker != null) {
			entityTracker.sendToListenersIf(packet, predicate);
		}
	}

	/**
	 * Отправляет to nearby players.
	 *
	 * @param entity entity
	 * @param packet packet
	 */
	protected void sendToNearbyPlayers(Entity entity, Packet<? super ClientPlayPacketListener> packet) {
		ServerChunkLoadingManager.EntityTracker
				entityTracker =
				(ServerChunkLoadingManager.EntityTracker) this.entityTrackers.get(entity.getId());
		if (entityTracker != null) {
			entityTracker.sendToSelfAndListeners(packet);
		}
	}

	public boolean hasTrackingPlayer(Entity entity) {
		ServerChunkLoadingManager.EntityTracker
				entityTracker =
				(ServerChunkLoadingManager.EntityTracker) this.entityTrackers.get(entity.getId());
		return entityTracker != null ? !entityTracker.listeners.isEmpty() : false;
	}

	/**
	 * For each entity tracked by.
	 *
	 * @param player player
	 * @param action action
	 */
	public void forEachEntityTrackedBy(ServerPlayerEntity player, Consumer<Entity> action) {
		ObjectIterator var3 = this.entityTrackers.values().iterator();

		while (var3.hasNext()) {
			ServerChunkLoadingManager.EntityTracker
					entityTracker =
					(ServerChunkLoadingManager.EntityTracker) var3.next();
			if (entityTracker.listeners.contains(player.networkHandler)) {
				action.accept(entityTracker.entity);
			}
		}
	}

	/**
	 * Отправляет chunk biome packets.
	 *
	 * @param chunks chunks
	 */
	public void sendChunkBiomePackets(List<Chunk> chunks) {
		Map<ServerPlayerEntity, List<WorldChunk>> map = new HashMap<>();

		for (Chunk chunk : chunks) {
			ChunkPos chunkPos = chunk.getPos();
			WorldChunk worldChunk2;
			if (chunk instanceof WorldChunk worldChunk) {
				worldChunk2 = worldChunk;
			}
			else {
				worldChunk2 = this.world.getChunk(chunkPos.x, chunkPos.z);
			}

			for (ServerPlayerEntity serverPlayerEntity : this.getPlayersWatchingChunk(chunkPos, false)) {
				map.computeIfAbsent(serverPlayerEntity, player -> new ArrayList<>()).add(worldChunk2);
			}
		}

		map.forEach((player, chunksx) -> player.networkHandler.sendPacket(ChunkBiomeDataS2CPacket.create((List<WorldChunk>) chunksx)));
	}

	protected PointOfInterestStorage getPointOfInterestStorage() {
		return this.pointOfInterestStorage;
	}

	public String getSaveDir() {
		return this.saveDir;
	}

	void onChunkStatusChange(ChunkPos chunkPos, ChunkLevelType levelType) {
		this.chunkStatusChangeListener.onChunkStatusChange(chunkPos, levelType);
	}

	/**
	 * Force lighting.
	 *
	 * @param centerPos center pos
	 * @param radius radius
	 */
	public void forceLighting(ChunkPos centerPos, int radius) {
		int i = radius + 1;
		ChunkPos.stream(centerPos, i).forEach(pos -> {
			ChunkHolder chunkHolder = this.getChunkHolder(pos.toLong());
			if (chunkHolder != null) {
				chunkHolder.combinePostProcessingFuture(this.lightingProvider.enqueue(pos.x, pos.z));
			}
		});
	}

	/**
	 * For each chunk.
	 *
	 * @param action action
	 */
	public void forEachChunk(Consumer<WorldChunk> action) {
		ObjectIterator var2 = this.chunkHolders.values().iterator();

		while (var2.hasNext()) {
			ChunkHolder chunkHolder = (ChunkHolder) var2.next();
			WorldChunk worldChunk = chunkHolder.getPostProcessedChunk();
			if (worldChunk != null) {
				action.accept(worldChunk);
			}
		}
	}

	class EntityTracker implements EntityTrackerEntry.TrackerPacketSender {

		final EntityTrackerEntry entry;
		final Entity entity;
		private final int maxDistance;
		ChunkSectionPos trackedSection;
		final Set<PlayerAssociatedNetworkHandler> listeners = Sets.newIdentityHashSet();

		public EntityTracker(
				final Entity entity,
				final int maxDistance,
				final int tickInterval,
				final boolean alwaysUpdateVelocity
		) {
			this.entry =
					new EntityTrackerEntry(
							ServerChunkLoadingManager.this.world,
							entity,
							tickInterval,
							alwaysUpdateVelocity,
							this
					);
			this.entity = entity;
			this.maxDistance = maxDistance;
			this.trackedSection = ChunkSectionPos.from(entity);
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof ServerChunkLoadingManager.EntityTracker
			       ? ((ServerChunkLoadingManager.EntityTracker) o).entity.getId() == this.entity.getId()
			       : false;
		}

		@Override
		public int hashCode() {
			return this.entity.getId();
		}

		@Override
		public void sendToListeners(Packet<? super ClientPlayPacketListener> packet) {
			for (PlayerAssociatedNetworkHandler playerAssociatedNetworkHandler : this.listeners) {
				playerAssociatedNetworkHandler.sendPacket(packet);
			}
		}

		@Override
		public void sendToSelfAndListeners(Packet<? super ClientPlayPacketListener> packet) {
			this.sendToListeners(packet);
			if (this.entity instanceof ServerPlayerEntity serverPlayerEntity) {
				serverPlayerEntity.networkHandler.sendPacket(packet);
			}
		}

		@Override
		public void sendToListenersIf(
				Packet<? super ClientPlayPacketListener> packet,
				Predicate<ServerPlayerEntity> predicate
		) {
			for (PlayerAssociatedNetworkHandler playerAssociatedNetworkHandler : this.listeners) {
				if (predicate.test(playerAssociatedNetworkHandler.getPlayer())) {
					playerAssociatedNetworkHandler.sendPacket(packet);
				}
			}
		}

		/**
		 * Останавливает tracking.
		 */
		public void stopTracking() {
			for (PlayerAssociatedNetworkHandler playerAssociatedNetworkHandler : this.listeners) {
				this.entry.stopTracking(playerAssociatedNetworkHandler.getPlayer());
			}
		}

		/**
		 * Останавливает tracking.
		 *
		 * @param player player
		 */
		public void stopTracking(ServerPlayerEntity player) {
			if (this.listeners.remove(player.networkHandler)) {
				this.entry.stopTracking(player);
				if (this.listeners.isEmpty()) {
					ServerChunkLoadingManager.this.world.getSubscriptionTracker().untrackEntity(this.entity);
				}
			}
		}

		/**
		 * Обновляет tracked status.
		 *
		 * @param player player
		 */
		public void updateTrackedStatus(ServerPlayerEntity player) {
			if (player != this.entity) {
				Vec3d vec3d = player.getEntityPos().subtract(this.entity.getEntityPos());
				int i = ServerChunkLoadingManager.this.getViewDistance(player);
				double d = Math.min(this.getMaxTrackDistance(), i * 16);
				double e = vec3d.x * vec3d.x + vec3d.z * vec3d.z;
				double f = d * d;
				boolean bl = e <= f
						&& this.entity.canBeSpectated(player)
						&& ServerChunkLoadingManager.this.isTracked(
						player,
						this.entity.getChunkPos().x,
						this.entity.getChunkPos().z
				);
				if (bl) {
					if (this.listeners.add(player.networkHandler)) {
						this.entry.startTracking(player);
						if (this.listeners.size() == 1) {
							ServerChunkLoadingManager.this.world.getSubscriptionTracker().trackEntity(this.entity);
						}

						ServerChunkLoadingManager.this.world
								.getSubscriptionTracker()
								.sendInitialIfSubscribed(player, this.entity);
					}
				}
				else {
					this.stopTracking(player);
				}
			}
		}

		private int adjustTrackingDistance(int initialDistance) {
			return ServerChunkLoadingManager.this.world.getServer().adjustTrackingDistance(initialDistance);
		}

		private int getMaxTrackDistance() {
			int i = this.maxDistance;

			for (Entity entity : this.entity.getPassengersDeep()) {
				int j = entity.getType().getMaxTrackDistance() * 16;
				if (j > i) {
					i = j;
				}
			}

			return this.adjustTrackingDistance(i);
		}

		/**
		 * Обновляет tracked status.
		 *
		 * @param players players
		 */
		public void updateTrackedStatus(List<ServerPlayerEntity> players) {
			for (ServerPlayerEntity serverPlayerEntity : players) {
				this.updateTrackedStatus(serverPlayerEntity);
			}
		}
	}

	class LevelManager extends ChunkLevelManager {

		protected LevelManager(
				final ChunkTicketManager ticketManager,
				final Executor executor,
				final Executor mainThreadExecutor
		) {
			super(ticketManager, executor, mainThreadExecutor);
		}

		@Override
		protected boolean isUnloaded(long pos) {
			return ServerChunkLoadingManager.this.unloadedChunks.contains(pos);
		}

		@Override
		protected @Nullable ChunkHolder getChunkHolder(long pos) {
			return ServerChunkLoadingManager.this.getCurrentChunkHolder(pos);
		}

		@Override
		protected @Nullable ChunkHolder setLevel(long pos, int level, @Nullable ChunkHolder holder, int i) {
			return ServerChunkLoadingManager.this.setLevel(pos, level, holder, i);
		}
	}
}
