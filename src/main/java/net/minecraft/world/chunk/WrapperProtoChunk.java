package net.minecraft.world.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.light.ChunkSkyLight;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.chunk.BlendingData;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.tick.BasicTickScheduler;
import net.minecraft.world.tick.EmptyTickSchedulers;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Обёртка над {@link WorldChunk}, реализующая интерфейс {@link ProtoChunk}.
 * Используется при финализации генерации: позволяет передавать {@link WorldChunk}
 * туда, где ожидается {@link ProtoChunk}, с опциональной проксировкой мутаций.
 */
public class WrapperProtoChunk extends ProtoChunk {

	private final WorldChunk wrapped;
	private final boolean propagateToWrapped;

	public WrapperProtoChunk(WorldChunk wrapped, boolean propagateToWrapped) {
		super(
				wrapped.getPos(),
				UpgradeData.NO_UPGRADE_DATA,
				wrapped.heightLimitView,
				wrapped.getWorld().getPalettesFactory(),
				wrapped.getBlendingData()
		);
		this.wrapped = wrapped;
		this.propagateToWrapped = propagateToWrapped;
	}

	@Override
	public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
		return wrapped.getBlockEntity(pos);
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		return wrapped.getBlockState(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return wrapped.getFluidState(pos);
	}

	@Override
	public ChunkSection getSection(int yIndex) {
		return propagateToWrapped ? wrapped.getSection(yIndex) : super.getSection(yIndex);
	}

	@Override
	public @Nullable BlockState setBlockState(BlockPos pos, BlockState state, @Block.SetBlockStateFlag int flags) {
		return propagateToWrapped ? wrapped.setBlockState(pos, state, flags) : null;
	}

	@Override
	public void setBlockEntity(BlockEntity blockEntity) {
		if (propagateToWrapped) {
			wrapped.setBlockEntity(blockEntity);
		}
	}

	@Override
	public void addEntity(Entity entity) {
		if (propagateToWrapped) {
			wrapped.addEntity(entity);
		}
	}

	@Override
	public void setStatus(ChunkStatus status) {
		if (propagateToWrapped) {
			super.setStatus(status);
		}
	}

	@Override
	public ChunkSection[] getSectionArray() {
		return wrapped.getSectionArray();
	}

	@Override
	public void setHeightmap(Heightmap.Type type, long[] heightmap) {
	}

	private Heightmap.Type transformHeightmapType(Heightmap.Type type) {
		if (type == Heightmap.Type.WORLD_SURFACE_WG) {
			return Heightmap.Type.WORLD_SURFACE;
		}

		return type == Heightmap.Type.OCEAN_FLOOR_WG ? Heightmap.Type.OCEAN_FLOOR : type;
	}

	@Override
	public Heightmap getHeightmap(Heightmap.Type type) {
		return wrapped.getHeightmap(type);
	}

	@Override
	public int sampleHeightmap(Heightmap.Type type, int x, int z) {
		return wrapped.sampleHeightmap(transformHeightmapType(type), x, z);
	}

	@Override
	public RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
		return wrapped.getBiomeForNoiseGen(biomeX, biomeY, biomeZ);
	}

	@Override
	public ChunkPos getPos() {
		return wrapped.getPos();
	}

	@Override
	public @Nullable StructureStart getStructureStart(Structure structure) {
		return wrapped.getStructureStart(structure);
	}

	@Override
	public void setStructureStart(Structure structure, StructureStart start) {
	}

	@Override
	public Map<Structure, StructureStart> getStructureStarts() {
		return wrapped.getStructureStarts();
	}

	@Override
	public void setStructureStarts(Map<Structure, StructureStart> structureStarts) {
	}

	@Override
	public LongSet getStructureReferences(Structure structure) {
		return wrapped.getStructureReferences(structure);
	}

	@Override
	public void addStructureReference(Structure structure, long reference) {
	}

	@Override
	public Map<Structure, LongSet> getStructureReferences() {
		return wrapped.getStructureReferences();
	}

	@Override
	public void setStructureReferences(Map<Structure, LongSet> structureReferences) {
	}

	@Override
	public void markNeedsSaving() {
		wrapped.markNeedsSaving();
	}

	@Override
	public boolean isSerializable() {
		return false;
	}

	@Override
	public boolean tryMarkSaved() {
		return false;
	}

	@Override
	public boolean needsSaving() {
		return false;
	}

	@Override
	public ChunkStatus getStatus() {
		return wrapped.getStatus();
	}

	@Override
	public void removeBlockEntity(BlockPos pos) {
	}

	@Override
	public void markBlockForPostProcessing(BlockPos pos) {
	}

	@Override
	public void addPendingBlockEntityNbt(NbtCompound nbt) {
	}

	@Override
	public @Nullable NbtCompound getBlockEntityNbt(BlockPos pos) {
		return wrapped.getBlockEntityNbt(pos);
	}

	@Override
	public @Nullable NbtCompound getPackedBlockEntityNbt(BlockPos pos, RegistryWrapper.WrapperLookup registries) {
		return wrapped.getPackedBlockEntityNbt(pos, registries);
	}

	@Override
	public void forEachBlockMatchingPredicate(
			Predicate<BlockState> predicate,
			BiConsumer<BlockPos, BlockState> consumer
	) {
		wrapped.forEachBlockMatchingPredicate(predicate, consumer);
	}

	@Override
	public BasicTickScheduler<Block> getBlockTickScheduler() {
		return propagateToWrapped ? wrapped.getBlockTickScheduler() : EmptyTickSchedulers.getReadOnlyTickScheduler();
	}

	@Override
	public BasicTickScheduler<Fluid> getFluidTickScheduler() {
		return propagateToWrapped ? wrapped.getFluidTickScheduler() : EmptyTickSchedulers.getReadOnlyTickScheduler();
	}

	@Override
	public Chunk.TickSchedulers getTickSchedulers(long time) {
		return wrapped.getTickSchedulers(time);
	}

	@Override
	public @Nullable BlendingData getBlendingData() {
		return wrapped.getBlendingData();
	}

	@Override
	public CarvingMask getCarvingMask() {
		if (propagateToWrapped) {
			return super.getCarvingMask();
		}

		throw (UnsupportedOperationException) Util.getFatalOrPause(
				new UnsupportedOperationException("Meaningless in this context"));
	}

	@Override
	public CarvingMask getOrCreateCarvingMask() {
		if (propagateToWrapped) {
			return super.getOrCreateCarvingMask();
		}

		throw (UnsupportedOperationException) Util.getFatalOrPause(
				new UnsupportedOperationException("Meaningless in this context"));
	}

	public WorldChunk getWrappedChunk() {
		return wrapped;
	}

	@Override
	public boolean isLightOn() {
		return wrapped.isLightOn();
	}

	@Override
	public void setLightOn(boolean lightOn) {
		wrapped.setLightOn(lightOn);
	}

	@Override
	public void populateBiomes(BiomeSupplier biomeSupplier, MultiNoiseUtil.MultiNoiseSampler sampler) {
		if (propagateToWrapped) {
			wrapped.populateBiomes(biomeSupplier, sampler);
		}
	}

	@Override
	public void refreshSurfaceY() {
		wrapped.refreshSurfaceY();
	}

	@Override
	public ChunkSkyLight getChunkSkyLight() {
		return wrapped.getChunkSkyLight();
	}
}
