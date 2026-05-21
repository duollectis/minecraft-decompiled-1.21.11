package net.minecraft.world;

import net.minecraft.block.BlockState;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.attribute.EnvironmentAttributeAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.dimension.DimensionType;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * {@code WorldView}.
 */
public interface WorldView extends BlockRenderView, CollisionView, RedstoneView, BiomeAccess.Storage {

	@Nullable Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create);

	@Deprecated
	boolean isChunkLoaded(int chunkX, int chunkZ);

	int getTopY(Heightmap.Type heightmap, int x, int z);

	default int getTopY(Heightmap.Type heightmap, BlockPos pos) {
		return this.getTopY(heightmap, pos.getX(), pos.getZ());
	}

	int getAmbientDarkness();

	BiomeAccess getBiomeAccess();

	default RegistryEntry<Biome> getBiome(BlockPos pos) {
		return this.getBiomeAccess().getBiome(pos);
	}

	default Stream<BlockState> getStatesInBoxIfLoaded(Box box) {
		int i = MathHelper.floor(box.minX);
		int j = MathHelper.floor(box.maxX);
		int k = MathHelper.floor(box.minY);
		int l = MathHelper.floor(box.maxY);
		int m = MathHelper.floor(box.minZ);
		int n = MathHelper.floor(box.maxZ);
		return this.isRegionLoaded(i, k, m, j, l, n) ? this.getStatesInBox(box) : Stream.empty();
	}

	@Override
	default int getColor(BlockPos pos, ColorResolver colorResolver) {
		return colorResolver.getColor(this.getBiome(pos).value(), pos.getX(), pos.getZ());
	}

	@Override
	default RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
		Chunk
				chunk =
				this.getChunk(BiomeCoords.toChunk(biomeX), BiomeCoords.toChunk(biomeZ), ChunkStatus.BIOMES, false);
		return chunk != null ? chunk.getBiomeForNoiseGen(biomeX, biomeY, biomeZ)
		                     : this.getGeneratorStoredBiome(biomeX, biomeY, biomeZ);
	}

	RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ);

	boolean isClient();

	int getSeaLevel();

	DimensionType getDimension();

	@Override
	default int getBottomY() {
		return this.getDimension().minY();
	}

	@Override
	default int getHeight() {
		return this.getDimension().height();
	}

	default BlockPos getTopPosition(Heightmap.Type heightmap, BlockPos pos) {
		return new BlockPos(pos.getX(), this.getTopY(heightmap, pos.getX(), pos.getZ()), pos.getZ());
	}

	default boolean isAir(BlockPos pos) {
		return this.getBlockState(pos).isAir();
	}

	default boolean isSkyVisibleAllowingSea(BlockPos pos) {
		if (pos.getY() >= this.getSeaLevel()) {
			return this.isSkyVisible(pos);
		}
		else {
			BlockPos blockPos = new BlockPos(pos.getX(), this.getSeaLevel(), pos.getZ());
			if (!this.isSkyVisible(blockPos)) {
				return false;
			}
			else {
				for (BlockPos var4 = blockPos.down(); var4.getY() > pos.getY(); var4 = var4.down()) {
					BlockState blockState = this.getBlockState(var4);
					if (blockState.getOpacity() > 0 && !blockState.isLiquid()) {
						return false;
					}
				}

				return true;
			}
		}
	}

	default float getPhototaxisFavor(BlockPos pos) {
		return this.getBrightness(pos) - 0.5F;
	}

	@Deprecated
	default float getBrightness(BlockPos pos) {
		float f = this.getLightLevel(pos) / 15.0F;
		float g = f / (4.0F - 3.0F * f);
		return MathHelper.lerp(this.getDimension().ambientLight(), g, 1.0F);
	}

	default Chunk getChunk(BlockPos pos) {
		return this.getChunk(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()));
	}

	default Chunk getChunk(int chunkX, int chunkZ) {
		return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
	}

	default Chunk getChunk(int chunkX, int chunkZ, ChunkStatus status) {
		return this.getChunk(chunkX, chunkZ, status, true);
	}

	@Override
	default @Nullable BlockView getChunkAsView(int chunkX, int chunkZ) {
		return this.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
	}

	default boolean isWater(BlockPos pos) {
		return this.getFluidState(pos).isIn(FluidTags.WATER);
	}

	default boolean containsFluid(Box box) {
		int i = MathHelper.floor(box.minX);
		int j = MathHelper.ceil(box.maxX);
		int k = MathHelper.floor(box.minY);
		int l = MathHelper.ceil(box.maxY);
		int m = MathHelper.floor(box.minZ);
		int n = MathHelper.ceil(box.maxZ);
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int o = i; o < j; o++) {
			for (int p = k; p < l; p++) {
				for (int q = m; q < n; q++) {
					BlockState blockState = this.getBlockState(mutable.set(o, p, q));
					if (!blockState.getFluidState().isEmpty()) {
						return true;
					}
				}
			}
		}

		return false;
	}

	default int getLightLevel(BlockPos pos) {
		return this.getLightLevel(pos, this.getAmbientDarkness());
	}

	default int getLightLevel(BlockPos pos, int ambientDarkness) {
		return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000
		       ? this.getBaseLightLevel(pos, ambientDarkness)
		       : 15;
	}

	@Deprecated
	default boolean isPosLoaded(int x, int z) {
		return this.isChunkLoaded(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
	}

	@Deprecated
	default boolean isChunkLoaded(BlockPos pos) {
		return this.isPosLoaded(pos.getX(), pos.getZ());
	}

	@Deprecated
	default boolean isRegionLoaded(BlockPos min, BlockPos max) {
		return this.isRegionLoaded(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
	}

	@Deprecated
	default boolean isRegionLoaded(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return maxY >= this.getBottomY() && minY <= this.getTopYInclusive() ? this.isRegionLoaded(
				minX,
				minZ,
				maxX,
				maxZ
		) : false;
	}

	@Deprecated
	default boolean isRegionLoaded(int minX, int minZ, int maxX, int maxZ) {
		int i = ChunkSectionPos.getSectionCoord(minX);
		int j = ChunkSectionPos.getSectionCoord(maxX);
		int k = ChunkSectionPos.getSectionCoord(minZ);
		int l = ChunkSectionPos.getSectionCoord(maxZ);

		for (int m = i; m <= j; m++) {
			for (int n = k; n <= l; n++) {
				if (!this.isChunkLoaded(m, n)) {
					return false;
				}
			}
		}

		return true;
	}

	DynamicRegistryManager getRegistryManager();

	FeatureSet getEnabledFeatures();

	default <T> RegistryWrapper<T> createCommandRegistryWrapper(RegistryKey<? extends Registry<? extends T>> registryRef) {
		Registry<T> registry = this.getRegistryManager().getOrThrow(registryRef);
		return registry.withFeatureFilter(this.getEnabledFeatures());
	}

	EnvironmentAttributeAccess getEnvironmentAttributes();
}
