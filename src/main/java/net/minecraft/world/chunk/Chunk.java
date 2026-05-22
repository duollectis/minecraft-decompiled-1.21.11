package net.minecraft.world.chunk;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureHolder;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.light.ChunkSkyLight;
import net.minecraft.world.chunk.light.LightSourceView;
import net.minecraft.world.event.listener.GameEventDispatcher;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.tick.BasicTickScheduler;
import net.minecraft.world.tick.Tick;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Базовый абстрактный класс чанка. Хранит секции блоков, карты высот,
 * блок-сущности, структуры и планировщики тиков. Является общим предком
 * для {@link WorldChunk} (загруженный чанк) и {@link ProtoChunk} (чанк генерации).
 */
public abstract class Chunk implements BiomeAccess.Storage, LightSourceView, StructureHolder, AttachmentTarget {

	public static final int MISSING_SECTION = -1;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final LongSet EMPTY_STRUCTURE_REFERENCES = new LongOpenHashSet();

	protected final @Nullable ShortList[] postProcessingLists;
	private volatile boolean needsSaving;
	private volatile boolean lightOn;
	protected final ChunkPos pos;
	private long inhabitedTime;

	@Deprecated
	private @Nullable GenerationSettings generationSettings;

	protected @Nullable ChunkNoiseSampler chunkNoiseSampler;
	protected final UpgradeData upgradeData;
	protected @Nullable BlendingData blendingData;
	protected final Map<Heightmap.Type, Heightmap> heightmaps = Maps.newEnumMap(Heightmap.Type.class);
	protected ChunkSkyLight chunkSkyLight;
	private final Map<Structure, StructureStart> structureStarts = Maps.newHashMap();
	private final Map<Structure, LongSet> structureReferences = Maps.newHashMap();
	protected final Map<BlockPos, NbtCompound> blockEntityNbts = Maps.newHashMap();
	protected final Map<BlockPos, BlockEntity> blockEntities = new Object2ObjectOpenHashMap();
	protected final HeightLimitView heightLimitView;
	protected final ChunkSection[] sectionArray;

	public Chunk(
		ChunkPos pos,
		UpgradeData upgradeData,
		HeightLimitView heightLimitView,
		PalettesFactory palettesFactory,
		long inhabitedTime,
		ChunkSection @Nullable [] sectionArray,
		@Nullable BlendingData blendingData
	) {
		this.pos = pos;
		this.upgradeData = upgradeData;
		this.heightLimitView = heightLimitView;
		this.sectionArray = new ChunkSection[heightLimitView.countVerticalSections()];
		this.inhabitedTime = inhabitedTime;
		this.postProcessingLists = new ShortList[heightLimitView.countVerticalSections()];
		this.blendingData = blendingData;
		this.chunkSkyLight = new ChunkSkyLight(heightLimitView);

		if (sectionArray != null) {
			if (this.sectionArray.length == sectionArray.length) {
				System.arraycopy(sectionArray, 0, this.sectionArray, 0, this.sectionArray.length);
			} else {
				LOGGER.warn(
					"Could not set level chunk sections, array length is {} instead of {}",
					sectionArray.length,
					this.sectionArray.length
				);
			}
		}

		fillSectionArray(palettesFactory, this.sectionArray);
	}

	private static void fillSectionArray(PalettesFactory palettesFactory, ChunkSection[] sections) {
		for (int index = 0; index < sections.length; index++) {
			if (sections[index] == null) {
				sections[index] = new ChunkSection(palettesFactory);
			}
		}
	}

	public GameEventDispatcher getGameEventDispatcher(int ySectionCoord) {
		return GameEventDispatcher.EMPTY;
	}

	public @Nullable BlockState setBlockState(BlockPos pos, BlockState state) {
		return setBlockState(pos, state, 3);
	}

	public abstract @Nullable BlockState setBlockState(
		BlockPos pos,
		BlockState state,
		@Block.SetBlockStateFlag int flags
	);

	public abstract void setBlockEntity(BlockEntity blockEntity);

	public abstract void addEntity(Entity entity);

	/**
	 * Возвращает индекс самой верхней непустой секции чанка.
	 * Используется для оптимизации рендеринга и освещения.
	 *
	 * @return индекс секции, или {@code -1} если все секции пусты
	 */
	public int getHighestNonEmptySection() {
		ChunkSection[] sections = getSectionArray();

		for (int index = sections.length - 1; index >= 0; index--) {
			if (!sections[index].isEmpty()) {
				return index;
			}
		}

		return MISSING_SECTION;
	}

	@Deprecated(forRemoval = true)
	public int getHighestNonEmptySectionYOffset() {
		int sectionIndex = getHighestNonEmptySection();
		return sectionIndex == MISSING_SECTION
			? getBottomY()
			: ChunkSectionPos.getBlockCoord(sectionIndexToCoord(sectionIndex));
	}

	public Set<BlockPos> getBlockEntityPositions() {
		Set<BlockPos> positions = Sets.newHashSet(blockEntityNbts.keySet());
		positions.addAll(blockEntities.keySet());
		return positions;
	}

	public ChunkSection[] getSectionArray() {
		return sectionArray;
	}

	public ChunkSection getSection(int yIndex) {
		return getSectionArray()[yIndex];
	}

	public Collection<Entry<Heightmap.Type, Heightmap>> getHeightmaps() {
		return Collections.unmodifiableSet(heightmaps.entrySet());
	}

	public void setHeightmap(Heightmap.Type type, long[] heightmap) {
		getHeightmap(type).setTo(this, type, heightmap);
	}

	public Heightmap getHeightmap(Heightmap.Type type) {
		return heightmaps.computeIfAbsent(type, t -> new Heightmap(this, t));
	}

	public boolean hasHeightmap(Heightmap.Type type) {
		return heightmaps.get(type) != null;
	}

	/**
	 * Возвращает Y-координату верхнего непустого блока в столбце (x, z).
	 * Если карта высот ещё не инициализирована — заполняет её на лету.
	 *
	 * @return Y-координата верхнего блока (включительно), или {@code bottomY - 1} если столбец пуст
	 */
	public int sampleHeightmap(Heightmap.Type type, int x, int z) {
		Heightmap heightmap = heightmaps.get(type);

		if (heightmap == null) {
			if (SharedConstants.isDevelopment && this instanceof WorldChunk) {
				LOGGER.error("Unprimed heightmap: {} {} {}", type, x, z);
			}

			Heightmap.populateHeightmaps(this, EnumSet.of(type));
			heightmap = heightmaps.get(type);
		}

		return heightmap.get(x & 15, z & 15) - 1;
	}

	public ChunkPos getPos() {
		return pos;
	}

	@Override
	public @Nullable StructureStart getStructureStart(Structure structure) {
		return structureStarts.get(structure);
	}

	@Override
	public void setStructureStart(Structure structure, StructureStart start) {
		structureStarts.put(structure, start);
		markNeedsSaving();
	}

	public Map<Structure, StructureStart> getStructureStarts() {
		return Collections.unmodifiableMap(structureStarts);
	}

	public void setStructureStarts(Map<Structure, StructureStart> starts) {
		structureStarts.clear();
		structureStarts.putAll(starts);
		markNeedsSaving();
	}

	@Override
	public LongSet getStructureReferences(Structure structure) {
		return structureReferences.getOrDefault(structure, EMPTY_STRUCTURE_REFERENCES);
	}

	@Override
	public void addStructureReference(Structure structure, long reference) {
		structureReferences.computeIfAbsent(structure, s -> new LongOpenHashSet()).add(reference);
		markNeedsSaving();
	}

	@Override
	public Map<Structure, LongSet> getStructureReferences() {
		return Collections.unmodifiableMap(structureReferences);
	}

	@Override
	public void setStructureReferences(Map<Structure, LongSet> references) {
		structureReferences.clear();
		structureReferences.putAll(references);
		markNeedsSaving();
	}

	/**
	 * Проверяет, пусты ли все секции чанка в диапазоне Y-координат.
	 * Диапазон автоматически зажимается до границ чанка.
	 */
	public boolean areSectionsEmptyBetween(int lowerHeight, int upperHeight) {
		int clampedLower = Math.max(lowerHeight, getBottomY());
		int clampedUpper = Math.min(upperHeight, getTopYInclusive());

		for (int y = clampedLower; y <= clampedUpper; y += 16) {
			if (!getSection(getSectionIndex(y)).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	public void markNeedsSaving() {
		needsSaving = true;
	}

	public boolean tryMarkSaved() {
		if (!needsSaving) {
			return false;
		}

		needsSaving = false;
		return true;
	}

	public boolean needsSaving() {
		return needsSaving;
	}

	public abstract ChunkStatus getStatus();

	public ChunkStatus getMaxStatus() {
		ChunkStatus status = getStatus();
		BelowZeroRetrogen retrogen = getBelowZeroRetrogen();
		return retrogen != null
			? ChunkStatus.max(retrogen.getTargetStatus(), status)
			: status;
	}

	public abstract void removeBlockEntity(BlockPos pos);

	public void markBlockForPostProcessing(BlockPos pos) {
		LOGGER.warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", pos);
	}

	public @Nullable ShortList[] getPostProcessingLists() {
		return postProcessingLists;
	}

	public void markBlocksForPostProcessing(ShortList packedPositions, int index) {
		getList(getPostProcessingLists(), index).addAll(packedPositions);
	}

	public void addPendingBlockEntityNbt(NbtCompound nbt) {
		BlockPos blockPos = BlockEntity.posFromNbt(pos, nbt);
		if (!blockEntities.containsKey(blockPos)) {
			blockEntityNbts.put(blockPos, nbt);
		}
	}

	public @Nullable NbtCompound getBlockEntityNbt(BlockPos pos) {
		return blockEntityNbts.get(pos);
	}

	public abstract @Nullable NbtCompound getPackedBlockEntityNbt(
		BlockPos pos,
		RegistryWrapper.WrapperLookup registries
	);

	@Override
	public final void forEachLightSource(BiConsumer<BlockPos, BlockState> callback) {
		forEachBlockMatchingPredicate(blockState -> blockState.getLuminance() != 0, callback);
	}

	/**
	 * Итерирует все блоки во всех секциях чанка, вызывая {@code consumer}
	 * для каждого блока, удовлетворяющего {@code predicate}.
	 * Использует быструю проверку {@link ChunkSection#hasAny} для пропуска пустых секций.
	 */
	public void forEachBlockMatchingPredicate(
		Predicate<BlockState> predicate,
		BiConsumer<BlockPos, BlockState> consumer
	) {
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int sectionCoord = getBottomSectionCoord(); sectionCoord <= getTopSectionCoord(); sectionCoord++) {
			ChunkSection section = getSection(sectionCoordToIndex(sectionCoord));

			if (!section.hasAny(predicate)) {
				continue;
			}

			BlockPos sectionMinPos = ChunkSectionPos.from(pos, sectionCoord).getMinPos();

			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						BlockState blockState = section.getBlockState(x, y, z);
						if (predicate.test(blockState)) {
							consumer.accept(mutable.set(sectionMinPos, x, y, z), blockState);
						}
					}
				}
			}
		}
	}

	public abstract BasicTickScheduler<Block> getBlockTickScheduler();

	public abstract BasicTickScheduler<Fluid> getFluidTickScheduler();

	public boolean isSerializable() {
		return true;
	}

	public abstract Chunk.TickSchedulers getTickSchedulers(long time);

	public UpgradeData getUpgradeData() {
		return upgradeData;
	}

	public boolean usesOldNoise() {
		return blendingData != null;
	}

	public @Nullable BlendingData getBlendingData() {
		return blendingData;
	}

	public long getInhabitedTime() {
		return inhabitedTime;
	}

	public void increaseInhabitedTime(long timeDelta) {
		inhabitedTime += timeDelta;
	}

	public void setInhabitedTime(long inhabitedTime) {
		this.inhabitedTime = inhabitedTime;
	}

	/**
	 * Возвращает (или создаёт) список отложенной постобработки для секции по индексу.
	 * Используется для хранения позиций блоков, требующих обновления после загрузки чанка.
	 */
	public static ShortList getList(@Nullable ShortList[] lists, int index) {
		ShortList list = lists[index];
		if (list == null) {
			list = new ShortArrayList();
			lists[index] = list;
		}

		return list;
	}

	public boolean isLightOn() {
		return lightOn;
	}

	public void setLightOn(boolean lightOn) {
		this.lightOn = lightOn;
		markNeedsSaving();
	}

	@Override
	public int getBottomY() {
		return heightLimitView.getBottomY();
	}

	@Override
	public int getHeight() {
		return heightLimitView.getHeight();
	}

	public ChunkNoiseSampler getOrCreateChunkNoiseSampler(Function<Chunk, ChunkNoiseSampler> creator) {
		if (chunkNoiseSampler == null) {
			chunkNoiseSampler = creator.apply(this);
		}

		return chunkNoiseSampler;
	}

	@Deprecated
	public GenerationSettings getOrCreateGenerationSettings(Supplier<GenerationSettings> creator) {
		if (generationSettings == null) {
			generationSettings = creator.get();
		}

		return generationSettings;
	}

	/**
	 * Возвращает биом для генерации шума по биомным координатам.
	 * Координата Y зажимается до допустимого диапазона секций чанка.
	 */
	@Override
	public RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
		try {
			int bottomBiomeCoord = BiomeCoords.fromBlock(getBottomY());
			int topBiomeCoord = bottomBiomeCoord + BiomeCoords.fromBlock(getHeight()) - 1;
			int clampedBiomeY = MathHelper.clamp(biomeY, bottomBiomeCoord, topBiomeCoord);
			int sectionIndex = getSectionIndex(BiomeCoords.toBlock(clampedBiomeY));
			return sectionArray[sectionIndex].getBiome(biomeX & 3, clampedBiomeY & 3, biomeZ & 3);
		} catch (Throwable error) {
			CrashReport crashReport = CrashReport.create(error, "Getting biome");
			CrashReportSection section = crashReport.addElement("Biome being got");
			section.add("Location", () -> CrashReportSection.createPositionString(this, biomeX, biomeY, biomeZ));
			throw new CrashException(crashReport);
		}
	}

	/**
	 * Заполняет биомные контейнеры всех секций чанка через {@link BiomeSupplier}.
	 */
	public void populateBiomes(BiomeSupplier biomeSupplier, MultiNoiseUtil.MultiNoiseSampler sampler) {
		ChunkPos chunkPos = getPos();
		int startBiomeX = BiomeCoords.fromBlock(chunkPos.getStartX());
		int startBiomeZ = BiomeCoords.fromBlock(chunkPos.getStartZ());
		HeightLimitView heightLimit = getHeightLimitView();

		for (int sectionCoord = heightLimit.getBottomSectionCoord();
			 sectionCoord <= heightLimit.getTopSectionCoord();
			 sectionCoord++
		) {
			ChunkSection section = getSection(sectionCoordToIndex(sectionCoord));
			int sectionBiomeY = BiomeCoords.fromChunk(sectionCoord);
			section.populateBiomes(biomeSupplier, sampler, startBiomeX, sectionBiomeY, startBiomeZ);
		}
	}

	public boolean hasStructureReferences() {
		return !getStructureReferences().isEmpty();
	}

	public @Nullable BelowZeroRetrogen getBelowZeroRetrogen() {
		return null;
	}

	public boolean hasBelowZeroRetrogen() {
		return getBelowZeroRetrogen() != null;
	}

	public HeightLimitView getHeightLimitView() {
		return this;
	}

	public void refreshSurfaceY() {
		chunkSkyLight.refreshSurfaceY(this);
	}

	@Override
	public ChunkSkyLight getChunkSkyLight() {
		return chunkSkyLight;
	}

	public static ErrorReporter.Context createErrorReporterContext(ChunkPos pos) {
		return new Chunk.ErrorReporterContext(pos);
	}

	public ErrorReporter.Context getErrorReporterContext() {
		return createErrorReporterContext(getPos());
	}

	record ErrorReporterContext(ChunkPos pos) implements ErrorReporter.Context {

		@Override
		public String getName() {
			return "chunk@" + pos;
		}
	}

	public record TickSchedulers(List<Tick<Block>> blocks, List<Tick<Fluid>> fluids) {
	}
}
