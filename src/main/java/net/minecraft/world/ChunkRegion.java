package net.minecraft.world;

import com.mojang.logging.LogUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.Util;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.attribute.EnvironmentAttributeAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.*;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.MultiTickScheduler;
import net.minecraft.world.tick.QueryableTickScheduler;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Ограниченный вид мира, предоставляемый генераторам чанков во время генерации.
 * <p>
 * Содержит только чанки в радиусе, определённом {@link ChunkGenerationStep#directDependencies()}.
 * Операции записи блоков ограничены радиусом {@link ChunkGenerationStep#blockStateWriteRadius()}.
 * Звуки, частицы и события игры не передаются — это заглушки для совместимости с интерфейсом.
 */
public class ChunkRegion implements StructureWorldAccess {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Identifier WORLDGEN_REGION_RANDOM_ID = Identifier.ofVanilla("worldgen_region_random");

	/** Идентификатор блока-заглушки для BlockEntity, созданных до финализации чанка. */
	private static final String DUMMY_BLOCK_ENTITY_ID = "DUMMY";

	private final BoundedRegionArray<AbstractChunkHolder> chunks;
	private final Chunk centerChunk;
	private final ServerWorld world;
	private final long seed;
	private final WorldProperties levelProperties;
	private final Random random;
	private final DimensionType dimension;
	private final MultiTickScheduler<Block> blockTickScheduler =
		new MultiTickScheduler<>(pos -> getChunk(pos).getBlockTickScheduler());
	private final MultiTickScheduler<Fluid> fluidTickScheduler =
		new MultiTickScheduler<>(pos -> getChunk(pos).getFluidTickScheduler());
	private final BiomeAccess biomeAccess;
	private final ChunkGenerationStep generationStep;
	private @Nullable Supplier<String> currentlyGeneratingStructureName;
	private final AtomicLong tickOrder = new AtomicLong();

	public ChunkRegion(
		ServerWorld world,
		BoundedRegionArray<AbstractChunkHolder> chunks,
		ChunkGenerationStep generationStep,
		Chunk centerChunk
	) {
		this.generationStep = generationStep;
		this.chunks = chunks;
		this.centerChunk = centerChunk;
		this.world = world;
		this.seed = world.getSeed();
		this.levelProperties = world.getLevelProperties();
		this.random = world.getChunkManager()
			.getNoiseConfig()
			.getOrCreateRandomDeriver(WORLDGEN_REGION_RANDOM_ID)
			.split(centerChunk.getPos().getStartPos());
		this.dimension = world.getDimension();
		this.biomeAccess = new BiomeAccess(this, BiomeAccess.hashSeed(seed));
	}

	/**
	 * Проверяет, нужно ли смешивание (blending) для данного чанка.
	 *
	 * @param chunkPos    позиция чанка
	 * @param checkRadius радиус проверки
	 * @return {@code true} если смешивание необходимо
	 */
	public boolean needsBlending(ChunkPos chunkPos, int checkRadius) {
		return world.getChunkManager().chunkLoadingManager.needsBlending(chunkPos, checkRadius);
	}

	public ChunkPos getCenterPos() {
		return centerChunk.getPos();
	}

	@Override
	public void setCurrentlyGeneratingStructureName(@Nullable Supplier<String> structureName) {
		currentlyGeneratingStructureName = structureName;
	}

	@Override
	public Chunk getChunk(int chunkX, int chunkZ) {
		return getChunk(chunkX, chunkZ, ChunkStatus.EMPTY);
	}

	/**
	 * Возвращает чанк по координатам, если он доступен в текущем шаге генерации.
	 * <p>
	 * Если чанк недоступен (вне кэша или статус ниже требуемого), бросает
	 * {@link CrashException} с подробной диагностикой.
	 *
	 * @param chunkX      координата X чанка
	 * @param chunkZ      координата Z чанка
	 * @param leastStatus минимально допустимый статус чанка
	 * @param create      не используется в данной реализации
	 * @return чанк или бросает CrashException
	 */
	@Override
	public @Nullable Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
		int distance = centerChunk.getPos().getChebyshevDistance(chunkX, chunkZ);
		ChunkStatus maxAllowedStatus = distance >= generationStep.directDependencies().size()
			? null
			: generationStep.directDependencies().get(distance);

		AbstractChunkHolder holder = null;
		if (maxAllowedStatus != null) {
			holder = chunks.get(chunkX, chunkZ);
			if (leastStatus.isAtMost(maxAllowedStatus)) {
				Chunk chunk = holder.getUncheckedOrNull(maxAllowedStatus);
				if (chunk != null) {
					return chunk;
				}
			}
		}

		final AbstractChunkHolder finalHolder = holder;
		CrashReport crashReport = CrashReport.create(
			new IllegalStateException("Requested chunk unavailable during world generation"),
			"Exception generating new chunk"
		);
		CrashReportSection section = crashReport.addElement("Chunk request details");
		section.add("Requested chunk", String.format(Locale.ROOT, "%d, %d", chunkX, chunkZ));
		section.add("Generating status", () -> generationStep.targetStatus().getId());
		section.add("Requested status", leastStatus::getId);
		section.add("Actual status", () -> finalHolder == null
			? "[out of cache bounds]"
			: finalHolder.getActualStatus().getId()
		);
		section.add("Maximum allowed status", () -> maxAllowedStatus == null ? "null" : maxAllowedStatus.getId());
		section.add("Dependencies", generationStep.directDependencies()::toString);
		section.add("Requested distance", distance);
		section.add("Generating chunk", centerChunk.getPos()::toString);
		throw new CrashException(crashReport);
	}

	@Override
	public boolean isChunkLoaded(int chunkX, int chunkZ) {
		int distance = centerChunk.getPos().getChebyshevDistance(chunkX, chunkZ);
		return distance < generationStep.directDependencies().size();
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()))
			.getBlockState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getChunk(pos).getFluidState(pos);
	}

	@Override
	public @Nullable PlayerEntity getClosestPlayer(
		double x,
		double y,
		double z,
		double maxDistance,
		@Nullable Predicate<Entity> targetPredicate
	) {
		return null;
	}

	@Override
	public int getAmbientDarkness() {
		return 0;
	}

	@Override
	public BiomeAccess getBiomeAccess() {
		return biomeAccess;
	}

	@Override
	public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
		return world.getGeneratorStoredBiome(biomeX, biomeY, biomeZ);
	}

	@Override
	public float getBrightness(Direction direction, boolean shaded) {
		return 1.0F;
	}

	@Override
	public LightingProvider getLightingProvider() {
		return world.getLightingProvider();
	}

	@Override
	public boolean breakBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
		BlockState blockState = getBlockState(pos);
		if (blockState.isAir()) {
			return false;
		}

		if (drop) {
			BlockEntity blockEntity = blockState.hasBlockEntity() ? getBlockEntity(pos) : null;
			Block.dropStacks(blockState, world, pos, blockEntity, breakingEntity, ItemStack.EMPTY);
		}

		return setBlockState(pos, Blocks.AIR.getDefaultState(), 3, maxUpdateDepth);
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		Chunk chunk = getChunk(pos);
		BlockEntity blockEntity = chunk.getBlockEntity(pos);
		if (blockEntity != null) {
			return blockEntity;
		}

		NbtCompound pendingNbt = chunk.getBlockEntityNbt(pos);
		if (pendingNbt == null) {
			BlockState blockState = chunk.getBlockState(pos);
			if (blockState.hasBlockEntity()) {
				LOGGER.warn("Tried to access a block entity before it was created. {}", pos);
			}
			return null;
		}

		BlockState blockState = chunk.getBlockState(pos);
		if (DUMMY_BLOCK_ENTITY_ID.equals(pendingNbt.getString("id", ""))) {
			if (!blockState.hasBlockEntity()) {
				return null;
			}
			blockEntity = ((BlockEntityProvider) blockState.getBlock()).createBlockEntity(pos, blockState);
		} else {
			blockEntity = BlockEntity.createFromNbt(pos, blockState, pendingNbt, world.getRegistryManager());
		}

		if (blockEntity != null) {
			chunk.setBlockEntity(blockEntity);
			return blockEntity;
		}

		if (blockState.hasBlockEntity()) {
			LOGGER.warn("Tried to access a block entity before it was created. {}", pos);
		}

		return null;
	}

	/**
	 * Проверяет, разрешена ли запись блока в данную позицию.
	 * <p>
	 * Запись разрешена только в пределах {@link ChunkGenerationStep#blockStateWriteRadius()}
	 * от центрального чанка. При нарушении логирует ошибку и возвращает {@code false}.
	 *
	 * @param pos позиция блока
	 * @return {@code true} если запись разрешена
	 */
	@Override
	public boolean isValidForSetBlock(BlockPos pos) {
		int chunkX = ChunkSectionPos.getSectionCoord(pos.getX());
		int chunkZ = ChunkSectionPos.getSectionCoord(pos.getZ());
		ChunkPos centerPos = getCenterPos();
		int distX = Math.abs(centerPos.x - chunkX);
		int distZ = Math.abs(centerPos.z - chunkZ);

		if (distX > generationStep.blockStateWriteRadius() || distZ > generationStep.blockStateWriteRadius()) {
			Util.logErrorOrPause(
				"Detected setBlock in a far chunk ["
					+ chunkX + ", " + chunkZ
					+ "], pos: " + pos
					+ ", status: " + generationStep.targetStatus()
					+ (currentlyGeneratingStructureName == null
						? ""
						: ", currently generating: " + currentlyGeneratingStructureName.get())
			);
			return false;
		}

		if (centerChunk.hasBelowZeroRetrogen()) {
			HeightLimitView heightLimitView = centerChunk.getHeightLimitView();
			if (heightLimitView.isOutOfHeightLimit(pos.getY())) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean setBlockState(
		BlockPos pos,
		BlockState state,
		@Block.SetBlockStateFlag int flags,
		int maxUpdateDepth
	) {
		if (!isValidForSetBlock(pos)) {
			return false;
		}

		Chunk chunk = getChunk(pos);
		BlockState previousState = chunk.setBlockState(pos, state, flags);
		if (previousState != null) {
			world.onBlockStateChanged(pos, previousState, state);
		}

		if (state.hasBlockEntity()) {
			if (chunk.getStatus().getChunkType() == ChunkType.LEVELCHUNK) {
				BlockEntity blockEntity = ((BlockEntityProvider) state.getBlock()).createBlockEntity(pos, state);
				if (blockEntity != null) {
					chunk.setBlockEntity(blockEntity);
				} else {
					chunk.removeBlockEntity(pos);
				}
			} else {
				NbtCompound dummyNbt = new NbtCompound();
				dummyNbt.putInt("x", pos.getX());
				dummyNbt.putInt("y", pos.getY());
				dummyNbt.putInt("z", pos.getZ());
				dummyNbt.putString("id", DUMMY_BLOCK_ENTITY_ID);
				chunk.addPendingBlockEntityNbt(dummyNbt);
			}
		} else if (previousState != null && previousState.hasBlockEntity()) {
			chunk.removeBlockEntity(pos);
		}

		// Флаг 16 (NO_OBSERVER) подавляет постобработку
		if (state.shouldPostProcess(this, pos) && (flags & 16) == 0) {
			markBlockForPostProcessing(pos);
		}

		return true;
	}

	private void markBlockForPostProcessing(BlockPos pos) {
		getChunk(pos).markBlockForPostProcessing(pos);
	}

	@Override
	public boolean spawnEntity(Entity entity) {
		int chunkX = ChunkSectionPos.getSectionCoord(entity.getBlockX());
		int chunkZ = ChunkSectionPos.getSectionCoord(entity.getBlockZ());
		getChunk(chunkX, chunkZ).addEntity(entity);
		return true;
	}

	@Override
	public boolean removeBlock(BlockPos pos, boolean move) {
		return setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
	}

	@Override
	public WorldBorder getWorldBorder() {
		return world.getWorldBorder();
	}

	@Override
	public boolean isClient() {
		return false;
	}

	@Deprecated
	@Override
	public ServerWorld toServerWorld() {
		return world;
	}

	@Override
	public DynamicRegistryManager getRegistryManager() {
		return world.getRegistryManager();
	}

	@Override
	public FeatureSet getEnabledFeatures() {
		return world.getEnabledFeatures();
	}

	@Override
	public WorldProperties getLevelProperties() {
		return levelProperties;
	}

	@Override
	public LocalDifficulty getLocalDifficulty(BlockPos pos) {
		if (!isChunkLoaded(
			ChunkSectionPos.getSectionCoord(pos.getX()),
			ChunkSectionPos.getSectionCoord(pos.getZ())
		)) {
			throw new RuntimeException("We are asking a region for a chunk out of bound");
		}

		return new LocalDifficulty(world.getDifficulty(), world.getTimeOfDay(), 0L, world.getMoonSize(pos));
	}

	@Override
	public @Nullable MinecraftServer getServer() {
		return world.getServer();
	}

	@Override
	public ChunkManager getChunkManager() {
		return world.getChunkManager();
	}

	@Override
	public long getSeed() {
		return seed;
	}

	@Override
	public QueryableTickScheduler<Block> getBlockTickScheduler() {
		return blockTickScheduler;
	}

	@Override
	public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
		return fluidTickScheduler;
	}

	@Override
	public int getSeaLevel() {
		return world.getSeaLevel();
	}

	@Override
	public Random getRandom() {
		return random;
	}

	@Override
	public int getTopY(Heightmap.Type heightmap, int x, int z) {
		return getChunk(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z))
			.sampleHeightmap(heightmap, x & 15, z & 15) + 1;
	}

	@Override
	public void playSound(
		@Nullable Entity source,
		BlockPos pos,
		SoundEvent sound,
		SoundCategory category,
		float volume,
		float pitch
	) {
	}

	@Override
	public void addParticleClient(
		ParticleEffect parameters,
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ
	) {
	}

	@Override
	public void syncWorldEvent(@Nullable Entity source, int eventId, BlockPos pos, int data) {
	}

	@Override
	public void emitGameEvent(RegistryEntry<GameEvent> event, Vec3d emitterPos, GameEvent.Emitter emitter) {
	}

	@Override
	public DimensionType getDimension() {
		return dimension;
	}

	@Override
	public boolean testBlockState(BlockPos pos, Predicate<BlockState> state) {
		return state.test(getBlockState(pos));
	}

	@Override
	public boolean testFluidState(BlockPos pos, Predicate<FluidState> state) {
		return state.test(getFluidState(pos));
	}

	@Override
	public <T extends Entity> List<T> getEntitiesByType(
		TypeFilter<Entity, T> filter,
		Box box,
		Predicate<? super T> predicate
	) {
		return Collections.emptyList();
	}

	@Override
	public List<Entity> getOtherEntities(
		@Nullable Entity except,
		Box box,
		@Nullable Predicate<? super Entity> predicate
	) {
		return Collections.emptyList();
	}

	@Override
	public List<PlayerEntity> getPlayers() {
		return Collections.emptyList();
	}

	@Override
	public int getBottomY() {
		return world.getBottomY();
	}

	@Override
	public int getHeight() {
		return world.getHeight();
	}

	@Override
	public long getTickOrder() {
		return tickOrder.getAndIncrement();
	}

	@Override
	public EnvironmentAttributeAccess getEnvironmentAttributes() {
		return EnvironmentAttributeAccess.DEFAULT;
	}
}
