package net.minecraft.world.chunk;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.tick.BasicTickScheduler;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.SimpleTickScheduler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Прото-чанк: промежуточное представление чанка в процессе генерации мира.
 * Используется на всех стадиях генерации от {@link ChunkStatus#EMPTY} до {@link ChunkStatus#FULL}.
 * После завершения генерации конвертируется в {@link WorldChunk}.
 */
public class ProtoChunk extends Chunk {

	private static final Logger LOGGER = LogUtils.getLogger();

	private volatile @Nullable LightingProvider lightingProvider;
	private volatile ChunkStatus status = ChunkStatus.EMPTY;
	private final List<NbtCompound> entities = Lists.newArrayList();
	private @Nullable CarvingMask carvingMask;
	private @Nullable BelowZeroRetrogen belowZeroRetrogen;
	private final SimpleTickScheduler<Block> blockTickScheduler;
	private final SimpleTickScheduler<Fluid> fluidTickScheduler;

	public ProtoChunk(
		ChunkPos pos,
		UpgradeData upgradeData,
		HeightLimitView world,
		PalettesFactory palettesFactory,
		@Nullable BlendingData blendingData
	) {
		this(
			pos,
			upgradeData,
			null,
			new SimpleTickScheduler<>(),
			new SimpleTickScheduler<>(),
			world,
			palettesFactory,
			blendingData
		);
	}

	public ProtoChunk(
		ChunkPos pos,
		UpgradeData upgradeData,
		ChunkSection @Nullable [] sections,
		SimpleTickScheduler<Block> blockTickScheduler,
		SimpleTickScheduler<Fluid> fluidTickScheduler,
		HeightLimitView world,
		PalettesFactory palettesFactory,
		@Nullable BlendingData blendingData
	) {
		super(pos, upgradeData, world, palettesFactory, 0L, sections, blendingData);
		this.blockTickScheduler = blockTickScheduler;
		this.fluidTickScheduler = fluidTickScheduler;
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
	public BlockState getBlockState(BlockPos pos) {
		int y = pos.getY();

		if (isOutOfHeightLimit(y)) {
			return Blocks.VOID_AIR.getDefaultState();
		}

		ChunkSection section = getSection(getSectionIndex(y));
		return section.isEmpty()
			? Blocks.AIR.getDefaultState()
			: section.getBlockState(pos.getX() & 15, y & 15, pos.getZ() & 15);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		int y = pos.getY();

		if (isOutOfHeightLimit(y)) {
			return Fluids.EMPTY.getDefaultState();
		}

		ChunkSection section = getSection(getSectionIndex(y));
		return section.isEmpty()
			? Fluids.EMPTY.getDefaultState()
			: section.getFluidState(pos.getX() & 15, y & 15, pos.getZ() & 15);
	}

	/**
	 * Устанавливает состояние блока в прото-чанке. Обновляет освещение (если статус
	 * достиг {@link ChunkStatus#INITIALIZE_LIGHT}) и карты высот текущего статуса.
	 */
	@Override
	public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, @Block.SetBlockStateFlag int flags) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		if (isOutOfHeightLimit(y)) {
			return Blocks.VOID_AIR.getDefaultState();
		}

		int sectionIndex = getSectionIndex(y);
		ChunkSection section = getSection(sectionIndex);
		boolean wasEmpty = section.isEmpty();

		if (wasEmpty && state.isOf(Blocks.AIR)) {
			return state;
		}

		int localX = ChunkSectionPos.getLocalCoord(x);
		int localY = ChunkSectionPos.getLocalCoord(y);
		int localZ = ChunkSectionPos.getLocalCoord(z);
		BlockState previousState = section.setBlockState(localX, localY, localZ, state);

		if (status.isAtLeast(ChunkStatus.INITIALIZE_LIGHT)) {
			boolean nowEmpty = section.isEmpty();
			if (nowEmpty != wasEmpty) {
				lightingProvider.setSectionStatus(pos, nowEmpty);
			}

			if (ChunkLightProvider.needsLightUpdate(previousState, state)) {
				chunkSkyLight.isSkyLightAccessible(this, localX, y, localZ);
				lightingProvider.checkBlock(pos);
			}
		}

		EnumSet<Heightmap.Type> requiredTypes = getStatus().getHeightmapTypes();
		EnumSet<Heightmap.Type> missingTypes = null;

		for (Heightmap.Type type : requiredTypes) {
			if (heightmaps.get(type) == null) {
				if (missingTypes == null) {
					missingTypes = EnumSet.noneOf(Heightmap.Type.class);
				}

				missingTypes.add(type);
			}
		}

		if (missingTypes != null) {
			Heightmap.populateHeightmaps(this, missingTypes);
		}

		for (Heightmap.Type type : requiredTypes) {
			heightmaps.get(type).trackUpdate(localX, y, localZ, state);
		}

		return previousState;
	}

	@Override
	public void setBlockEntity(BlockEntity blockEntity) {
		blockEntityNbts.remove(blockEntity.getPos());
		blockEntities.put(blockEntity.getPos(), blockEntity);
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		return blockEntities.get(pos);
	}

	public Map<BlockPos, BlockEntity> getBlockEntities() {
		return blockEntities;
	}

	public void addEntity(NbtCompound entityNbt) {
		entities.add(entityNbt);
	}

	@Override
	public void addEntity(Entity entity) {
		if (entity.hasVehicle()) {
			return;
		}

		try (ErrorReporter.Logging logging = new ErrorReporter.Logging(entity.getErrorReporterContext(), LOGGER)) {
			NbtWriteView nbtWriteView = NbtWriteView.create(logging, entity.getRegistryManager());
			entity.saveData(nbtWriteView);
			addEntity(nbtWriteView.getNbt());
		}
	}

	/**
	 * Устанавливает старт структуры, проверяя что её ограничивающий бокс
	 * не выходит за пределы высотного диапазона при наличии {@link BelowZeroRetrogen}.
	 */
	@Override
	public void setStructureStart(Structure structure, StructureStart start) {
		BelowZeroRetrogen retrogen = getBelowZeroRetrogen();
		if (retrogen != null && start.hasChildren()) {
			BlockBox boundingBox = start.getBoundingBox();
			HeightLimitView heightLimit = getHeightLimitView();
			if (boundingBox.getMinY() < heightLimit.getBottomY()
				|| boundingBox.getMaxY() > heightLimit.getTopYInclusive()
			) {
				return;
			}
		}

		super.setStructureStart(structure, start);
	}

	public List<NbtCompound> getEntities() {
		return entities;
	}

	@Override
	public ChunkStatus getStatus() {
		return status;
	}

	/**
	 * Устанавливает статус генерации чанка. Если статус достиг цели ретрогенерации
	 * ниже нуля — сбрасывает {@link BelowZeroRetrogen}.
	 */
	public void setStatus(ChunkStatus status) {
		this.status = status;
		if (belowZeroRetrogen != null && status.isAtLeast(belowZeroRetrogen.getTargetStatus())) {
			setBelowZeroRetrogen(null);
		}

		markNeedsSaving();
	}

	@Override
	public RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
		if (!getMaxStatus().isAtLeast(ChunkStatus.BIOMES)) {
			throw new IllegalStateException("Asking for biomes before we have biomes");
		}

		return super.getBiomeForNoiseGen(biomeX, biomeY, biomeZ);
	}

	/**
	 * Упаковывает локальные координаты блока внутри секции в 12-битное short-значение.
	 * Формат: биты 0-3 = X, биты 4-7 = Y, биты 8-11 = Z.
	 */
	public static short getPackedSectionRelative(BlockPos pos) {
		int localX = pos.getX() & 15;
		int localY = pos.getY() & 15;
		int localZ = pos.getZ() & 15;
		return (short) (localX | localY << 4 | localZ << 8);
	}

	/**
	 * Восстанавливает абсолютную позицию блока из упакованного short-значения секции.
	 *
	 * @param sectionRel упакованные локальные координаты (формат см. {@link #getPackedSectionRelative})
	 * @param sectionY   Y-координата секции
	 * @param chunkPos   позиция чанка
	 */
	public static BlockPos joinBlockPos(short sectionRel, int sectionY, ChunkPos chunkPos) {
		int x = ChunkSectionPos.getOffsetPos(chunkPos.x, sectionRel & 15);
		int y = ChunkSectionPos.getOffsetPos(sectionY, sectionRel >>> 4 & 15);
		int z = ChunkSectionPos.getOffsetPos(chunkPos.z, sectionRel >>> 8 & 15);
		return new BlockPos(x, y, z);
	}

	@Override
	public void markBlockForPostProcessing(BlockPos pos) {
		if (isOutOfHeightLimit(pos)) {
			return;
		}

		Chunk.getList(postProcessingLists, getSectionIndex(pos.getY()))
			.add(getPackedSectionRelative(pos));
	}

	@Override
	public void markBlocksForPostProcessing(ShortList packedPositions, int index) {
		Chunk.getList(postProcessingLists, index).addAll(packedPositions);
	}

	public Map<BlockPos, NbtCompound> getBlockEntityNbts() {
		return Collections.unmodifiableMap(blockEntityNbts);
	}

	@Override
	public @Nullable NbtCompound getPackedBlockEntityNbt(BlockPos pos, RegistryWrapper.WrapperLookup registries) {
		BlockEntity blockEntity = getBlockEntity(pos);
		return blockEntity != null
			? blockEntity.createNbtWithIdentifyingData(registries)
			: blockEntityNbts.get(pos);
	}

	@Override
	public void removeBlockEntity(BlockPos pos) {
		blockEntities.remove(pos);
		blockEntityNbts.remove(pos);
	}

	public @Nullable CarvingMask getCarvingMask() {
		return carvingMask;
	}

	public CarvingMask getOrCreateCarvingMask() {
		if (carvingMask == null) {
			carvingMask = new CarvingMask(getHeight(), getBottomY());
		}

		return carvingMask;
	}

	public void setCarvingMask(CarvingMask carvingMask) {
		this.carvingMask = carvingMask;
	}

	public void setLightingProvider(LightingProvider lightingProvider) {
		this.lightingProvider = lightingProvider;
	}

	public void setBelowZeroRetrogen(@Nullable BelowZeroRetrogen belowZeroRetrogen) {
		this.belowZeroRetrogen = belowZeroRetrogen;
	}

	@Override
	public @Nullable BelowZeroRetrogen getBelowZeroRetrogen() {
		return belowZeroRetrogen;
	}

	private static <T> ChunkTickScheduler<T> createProtoTickScheduler(SimpleTickScheduler<T> tickScheduler) {
		return new ChunkTickScheduler<>(tickScheduler.getTicks());
	}

	public ChunkTickScheduler<Block> getBlockProtoTickScheduler() {
		return createProtoTickScheduler(blockTickScheduler);
	}

	public ChunkTickScheduler<Fluid> getFluidProtoTickScheduler() {
		return createProtoTickScheduler(fluidTickScheduler);
	}

	@Override
	public HeightLimitView getHeightLimitView() {
		return hasBelowZeroRetrogen() ? BelowZeroRetrogen.BELOW_ZERO_VIEW : this;
	}
}
