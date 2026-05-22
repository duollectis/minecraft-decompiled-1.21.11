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
 * Интерфейс представления мира, объединяющий рендер-вид блоков, коллизии,
 * редстоун-логику и хранилище биомов. Предоставляет методы для работы
 * с чанками, высотами, биомами, освещением и погодными условиями.
 */
public interface WorldView extends BlockRenderView, CollisionView, RedstoneView, BiomeAccess.Storage {

	@Nullable Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create);

	@Deprecated
	boolean isChunkLoaded(int chunkX, int chunkZ);

	int getTopY(Heightmap.Type heightmap, int x, int z);

	default int getTopY(Heightmap.Type heightmap, BlockPos pos) {
		return getTopY(heightmap, pos.getX(), pos.getZ());
	}

	int getAmbientDarkness();

	BiomeAccess getBiomeAccess();

	default RegistryEntry<Biome> getBiome(BlockPos pos) {
		return getBiomeAccess().getBiome(pos);
	}

	default Stream<BlockState> getStatesInBoxIfLoaded(Box box) {
		int minX = MathHelper.floor(box.minX);
		int maxX = MathHelper.floor(box.maxX);
		int minY = MathHelper.floor(box.minY);
		int maxY = MathHelper.floor(box.maxY);
		int minZ = MathHelper.floor(box.minZ);
		int maxZ = MathHelper.floor(box.maxZ);
		return isRegionLoaded(minX, minY, minZ, maxX, maxY, maxZ)
			? getStatesInBox(box)
			: Stream.empty();
	}

	@Override
	default int getColor(BlockPos pos, ColorResolver colorResolver) {
		return colorResolver.getColor(getBiome(pos).value(), pos.getX(), pos.getZ());
	}

	@Override
	default RegistryEntry<Biome> getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
		Chunk chunk = getChunk(BiomeCoords.toChunk(biomeX), BiomeCoords.toChunk(biomeZ), ChunkStatus.BIOMES, false);
		return chunk != null
			? chunk.getBiomeForNoiseGen(biomeX, biomeY, biomeZ)
			: getGeneratorStoredBiome(biomeX, biomeY, biomeZ);
	}

	RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ);

	boolean isClient();

	int getSeaLevel();

	DimensionType getDimension();

	@Override
	default int getBottomY() {
		return getDimension().minY();
	}

	@Override
	default int getHeight() {
		return getDimension().height();
	}

	default BlockPos getTopPosition(Heightmap.Type heightmap, BlockPos pos) {
		return new BlockPos(pos.getX(), getTopY(heightmap, pos.getX(), pos.getZ()), pos.getZ());
	}

	default boolean isAir(BlockPos pos) {
		return getBlockState(pos).isAir();
	}

	/**
	 * Проверяет видимость неба с учётом уровня моря: если позиция ниже уровня моря,
	 * проверяет, нет ли непрозрачных нежидких блоков между позицией и уровнем моря.
	 */
	default boolean isSkyVisibleAllowingSea(BlockPos pos) {
		if (pos.getY() >= getSeaLevel()) {
			return isSkyVisible(pos);
		}

		BlockPos seaPos = new BlockPos(pos.getX(), getSeaLevel(), pos.getZ());
		if (!isSkyVisible(seaPos)) {
			return false;
		}

		for (BlockPos current = seaPos.down(); current.getY() > pos.getY(); current = current.down()) {
			BlockState blockState = getBlockState(current);
			if (blockState.getOpacity() > 0 && !blockState.isLiquid()) {
				return false;
			}
		}

		return true;
	}

	default float getPhototaxisFavor(BlockPos pos) {
		return getBrightness(pos) - 0.5F;
	}

	/**
	 * Возвращает нормализованную яркость блока с учётом ambient-освещения измерения.
	 * Формула: lerp(ambientLight, lightLevel / (4 - 3 * lightLevel), 1.0).
	 */
	@Deprecated
	default float getBrightness(BlockPos pos) {
		float lightFraction = getLightLevel(pos) / 15.0F;
		float adjusted = lightFraction / (4.0F - 3.0F * lightFraction);
		return MathHelper.lerp(getDimension().ambientLight(), adjusted, 1.0F);
	}

	default Chunk getChunk(BlockPos pos) {
		return getChunk(
			ChunkSectionPos.getSectionCoord(pos.getX()),
			ChunkSectionPos.getSectionCoord(pos.getZ())
		);
	}

	default Chunk getChunk(int chunkX, int chunkZ) {
		return getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
	}

	default Chunk getChunk(int chunkX, int chunkZ, ChunkStatus status) {
		return getChunk(chunkX, chunkZ, status, true);
	}

	@Override
	default @Nullable BlockView getChunkAsView(int chunkX, int chunkZ) {
		return getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false);
	}

	default boolean isWater(BlockPos pos) {
		return getFluidState(pos).isIn(FluidTags.WATER);
	}

	default boolean containsFluid(Box box) {
		int minX = MathHelper.floor(box.minX);
		int maxX = MathHelper.ceil(box.maxX);
		int minY = MathHelper.floor(box.minY);
		int maxY = MathHelper.ceil(box.maxY);
		int minZ = MathHelper.floor(box.minZ);
		int maxZ = MathHelper.ceil(box.maxZ);
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				for (int z = minZ; z < maxZ; z++) {
					BlockState blockState = getBlockState(mutable.set(x, y, z));
					if (!blockState.getFluidState().isEmpty()) {
						return true;
					}
				}
			}
		}

		return false;
	}

	default int getLightLevel(BlockPos pos) {
		return getLightLevel(pos, getAmbientDarkness());
	}

	default int getLightLevel(BlockPos pos, int ambientDarkness) {
		return pos.getX() >= -World.HORIZONTAL_LIMIT
			&& pos.getZ() >= -World.HORIZONTAL_LIMIT
			&& pos.getX() < World.HORIZONTAL_LIMIT
			&& pos.getZ() < World.HORIZONTAL_LIMIT
			? getBaseLightLevel(pos, ambientDarkness)
			: World.MAX_LIGHT_LEVEL;
	}

	@Deprecated
	default boolean isPosLoaded(int x, int z) {
		return isChunkLoaded(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
	}

	@Deprecated
	default boolean isChunkLoaded(BlockPos pos) {
		return isPosLoaded(pos.getX(), pos.getZ());
	}

	@Deprecated
	default boolean isRegionLoaded(BlockPos min, BlockPos max) {
		return isRegionLoaded(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
	}

	@Deprecated
	default boolean isRegionLoaded(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		return maxY >= getBottomY() && minY <= getTopYInclusive()
			? isRegionLoaded(minX, minZ, maxX, maxZ)
			: false;
	}

	@Deprecated
	default boolean isRegionLoaded(int minX, int minZ, int maxX, int maxZ) {
		int startChunkX = ChunkSectionPos.getSectionCoord(minX);
		int endChunkX = ChunkSectionPos.getSectionCoord(maxX);
		int startChunkZ = ChunkSectionPos.getSectionCoord(minZ);
		int endChunkZ = ChunkSectionPos.getSectionCoord(maxZ);

		for (int chunkX = startChunkX; chunkX <= endChunkX; chunkX++) {
			for (int chunkZ = startChunkZ; chunkZ <= endChunkZ; chunkZ++) {
				if (!isChunkLoaded(chunkX, chunkZ)) {
					return false;
				}
			}
		}

		return true;
	}

	DynamicRegistryManager getRegistryManager();

	FeatureSet getEnabledFeatures();

	default <T> RegistryWrapper<T> createCommandRegistryWrapper(RegistryKey<? extends Registry<? extends T>> registryRef) {
		Registry<T> registry = getRegistryManager().getOrThrow(registryRef);
		return registry.withFeatureFilter(getEnabledFeatures());
	}

	EnvironmentAttributeAccess getEnvironmentAttributes();
}
