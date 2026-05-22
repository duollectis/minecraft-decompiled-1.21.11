package net.minecraft.world.gen.chunk;

import com.google.common.primitives.Doubles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Данные смешивания (blending) для плавного перехода между старыми и новыми чанками.
 * Хранит высоты поверхности, плотности столкновений и биомы вдоль границ чанка.
 * Используется {@link Blender} для интерполяции рельефа на границах версий мира.
 */
public class BlendingData {

	private static final double COLLIDABLE_BLOCK_DENSITY_THRESHOLD = 0.1;
	protected static final int BIOME_BLOCK_STEP = 4;
	protected static final int BLEND_SECTION_COUNT = 8;
	protected static final int BLEND_RADIUS = 2;
	private static final double MAX_DENSITY = 1.0;
	private static final double MIN_DENSITY = -1.0;
	private static final int EXTRA_SECTION_COUNT = 2;
	private static final int BIOMES_PER_CHUNK = BiomeCoords.fromBlock(16);
	private static final int LAST_CHUNK_BIOME_INDEX = BIOMES_PER_CHUNK - 1;
	private static final int CHUNK_BIOME_END_INDEX = BIOMES_PER_CHUNK;
	private static final int NORTH_WEST_END_INDEX = 2 * LAST_CHUNK_BIOME_INDEX + 1;
	private static final int SOUTH_EAST_END_INDEX_PART = 2 * CHUNK_BIOME_END_INDEX + 1;
	static final int HORIZONTAL_BIOME_COUNT = NORTH_WEST_END_INDEX + SOUTH_EAST_END_INDEX_PART;
	private final HeightLimitView oldHeightLimit;
	private static final List<Block> SURFACE_BLOCKS = List.of(
			Blocks.PODZOL,
			Blocks.GRAVEL,
			Blocks.GRASS_BLOCK,
			Blocks.STONE,
			Blocks.COARSE_DIRT,
			Blocks.SAND,
			Blocks.RED_SAND,
			Blocks.MYCELIUM,
			Blocks.SNOW_BLOCK,
			Blocks.TERRACOTTA,
			Blocks.DIRT
	);
	protected static final double UNSET_SURFACE_HEIGHT = Double.MAX_VALUE;
	private boolean initializedBlendingData;
	private final double[] surfaceHeights;
	private final List<@Nullable List<@Nullable RegistryEntry<Biome>>> biomes;
	private final transient double[][] collidableBlockDensities;

	private BlendingData(int oldBottomSectionY, int oldTopSectionY, Optional<double[]> heights) {
		this.surfaceHeights =
				heights.orElseGet(() -> Util.make(
						new double[HORIZONTAL_BIOME_COUNT],
						ds -> Arrays.fill(ds, Double.MAX_VALUE)
				));
		this.collidableBlockDensities = new double[HORIZONTAL_BIOME_COUNT][];
		ObjectArrayList<List<RegistryEntry<Biome>>> objectArrayList = new ObjectArrayList(HORIZONTAL_BIOME_COUNT);
		objectArrayList.size(HORIZONTAL_BIOME_COUNT);
		this.biomes = objectArrayList;
		int i = ChunkSectionPos.getBlockCoord(oldBottomSectionY);
		int j = ChunkSectionPos.getBlockCoord(oldTopSectionY) - i;
		this.oldHeightLimit = HeightLimitView.create(i, j);
	}

	/**
	 * From serialized.
	 *
	 * @param serialized serialized
	 *
	 * @return @Nullable BlendingData — результат операции
	 */
	public static @Nullable BlendingData fromSerialized(BlendingData.@Nullable Serialized serialized) {
		return serialized == null ? null : new BlendingData(
				serialized.minSection(),
				serialized.maxSection(),
				serialized.heights()
		);
	}

	public BlendingData.Serialized toSerialized() {
		boolean hasHeights = false;

		for (double height : surfaceHeights) {
			if (height != Double.MAX_VALUE) {
				hasHeights = true;
				break;
			}
		}

		return new BlendingData.Serialized(
				oldHeightLimit.getBottomSectionCoord(),
				oldHeightLimit.getTopSectionCoord() + 1,
				hasHeights ? Optional.of(DoubleArrays.copy(surfaceHeights)) : Optional.empty()
		);
	}

	public static @Nullable BlendingData getBlendingData(ChunkRegion chunkRegion, int chunkX, int chunkZ) {
		Chunk chunk = chunkRegion.getChunk(chunkX, chunkZ);
		BlendingData blendingData = chunk.getBlendingData();

		if (blendingData == null || chunk.getMaxStatus().isEarlierThan(ChunkStatus.BIOMES)) {
			return null;
		}

		blendingData.initChunkBlendingData(chunk, getAdjacentChunksWithNoise(chunkRegion, chunkX, chunkZ, false));
		return blendingData;
	}

	public static Set<EightWayDirection> getAdjacentChunksWithNoise(
			StructureWorldAccess access,
			int chunkX,
			int chunkZ,
			boolean oldNoise
	) {
		Set<EightWayDirection> set = EnumSet.noneOf(EightWayDirection.class);

		for (EightWayDirection eightWayDirection : EightWayDirection.values()) {
			int i = chunkX + eightWayDirection.getOffsetX();
			int j = chunkZ + eightWayDirection.getOffsetZ();
			if (access.getChunk(i, j).usesOldNoise() == oldNoise) {
				set.add(eightWayDirection);
			}
		}

		return set;
	}

	private void initChunkBlendingData(Chunk chunk, Set<EightWayDirection> newNoiseChunkDirections) {
		if (!this.initializedBlendingData) {
			if (newNoiseChunkDirections.contains(EightWayDirection.NORTH)
					|| newNoiseChunkDirections.contains(EightWayDirection.WEST)
					|| newNoiseChunkDirections.contains(EightWayDirection.NORTH_WEST)) {
				this.initBlockColumn(getNorthWestIndex(0, 0), chunk, 0, 0);
			}

			if (newNoiseChunkDirections.contains(EightWayDirection.NORTH)) {
				for (int i = 1; i < BIOMES_PER_CHUNK; i++) {
					this.initBlockColumn(getNorthWestIndex(i, 0), chunk, 4 * i, 0);
				}
			}

			if (newNoiseChunkDirections.contains(EightWayDirection.WEST)) {
				for (int i = 1; i < BIOMES_PER_CHUNK; i++) {
					this.initBlockColumn(getNorthWestIndex(0, i), chunk, 0, 4 * i);
				}
			}

			if (newNoiseChunkDirections.contains(EightWayDirection.EAST)) {
				for (int i = 1; i < BIOMES_PER_CHUNK; i++) {
					this.initBlockColumn(getSouthEastIndex(CHUNK_BIOME_END_INDEX, i), chunk, 15, 4 * i);
				}
			}

			if (newNoiseChunkDirections.contains(EightWayDirection.SOUTH)) {
				for (int i = 0; i < BIOMES_PER_CHUNK; i++) {
					this.initBlockColumn(getSouthEastIndex(i, CHUNK_BIOME_END_INDEX), chunk, 4 * i, 15);
				}
			}

			if (newNoiseChunkDirections.contains(EightWayDirection.EAST) && newNoiseChunkDirections.contains(
					EightWayDirection.NORTH_EAST)) {
				this.initBlockColumn(getSouthEastIndex(CHUNK_BIOME_END_INDEX, 0), chunk, 15, 0);
			}

			if (newNoiseChunkDirections.contains(EightWayDirection.EAST)
					&& newNoiseChunkDirections.contains(EightWayDirection.SOUTH)
					&& newNoiseChunkDirections.contains(EightWayDirection.SOUTH_EAST)) {
				this.initBlockColumn(getSouthEastIndex(CHUNK_BIOME_END_INDEX, CHUNK_BIOME_END_INDEX), chunk, 15, 15);
			}

			this.initializedBlendingData = true;
		}
	}

	private void initBlockColumn(int index, Chunk chunk, int chunkBlockX, int chunkBlockZ) {
		if (this.surfaceHeights[index] == Double.MAX_VALUE) {
			this.surfaceHeights[index] = this.getSurfaceBlockY(chunk, chunkBlockX, chunkBlockZ);
		}

		this.collidableBlockDensities[index] = this.calculateCollidableBlockDensityColumn(
				chunk, chunkBlockX, chunkBlockZ, MathHelper.floor(this.surfaceHeights[index])
		);
		this.biomes.set(index, this.getVerticalBiomeSections(chunk, chunkBlockX, chunkBlockZ));
	}

	private int getSurfaceBlockY(Chunk chunk, int blockX, int blockZ) {
		int i;
		if (chunk.hasHeightmap(Heightmap.Type.WORLD_SURFACE_WG)) {
			i =
					Math.min(
							chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, blockX, blockZ),
							this.oldHeightLimit.getTopYInclusive()
					);
		}
		else {
			i = this.oldHeightLimit.getTopYInclusive();
		}

		int j = this.oldHeightLimit.getBottomY();
		BlockPos.Mutable mutable = new BlockPos.Mutable(blockX, i, blockZ);

		while (mutable.getY() > j) {
			if (SURFACE_BLOCKS.contains(chunk.getBlockState(mutable).getBlock())) {
				return mutable.getY();
			}

			mutable.move(Direction.DOWN);
		}

		return j;
	}

	private static double getAboveCollidableBlockValue(Chunk chunk, BlockPos.Mutable mutablePos) {
		return isCollidableAndNotTreeAt(chunk, mutablePos.move(Direction.DOWN)) ? 1.0 : -1.0;
	}

	private static double getCollidableBlockDensityBelow(Chunk chunk, BlockPos.Mutable mutablePos) {
		double d = 0.0;

		for (int i = 0; i < 7; i++) {
			d += getAboveCollidableBlockValue(chunk, mutablePos);
		}

		return d;
	}

	private double[] calculateCollidableBlockDensityColumn(
			Chunk chunk,
			int chunkBlockX,
			int chunkBlockZ,
			int surfaceHeight
	) {
		double[] ds = new double[this.getVerticalHalfSectionCount()];
		Arrays.fill(ds, -1.0);
		BlockPos.Mutable
				mutable =
				new BlockPos.Mutable(chunkBlockX, this.oldHeightLimit.getTopYInclusive() + 1, chunkBlockZ);
		double d = getCollidableBlockDensityBelow(chunk, mutable);

		for (int i = ds.length - 2; i >= 0; i--) {
			double e = getAboveCollidableBlockValue(chunk, mutable);
			double f = getCollidableBlockDensityBelow(chunk, mutable);
			ds[i] = (d + e + f) / 15.0;
			d = f;
		}

		int i = this.getHalfSectionHeight(MathHelper.floorDiv(surfaceHeight, 8));
		if (i >= 0 && i < ds.length - 1) {
			double e = (surfaceHeight + 0.5) % 8.0 / 8.0;
			double f = (1.0 - e) / e;
			double g = Math.max(f, 1.0) * 0.25;
			ds[i + 1] = -f / g;
			ds[i] = 1.0 / g;
		}

		return ds;
	}

	private List<RegistryEntry<Biome>> getVerticalBiomeSections(Chunk chunk, int chunkBlockX, int chunkBlockZ) {
		ObjectArrayList<RegistryEntry<Biome>> objectArrayList = new ObjectArrayList(this.getVerticalBiomeCount());
		objectArrayList.size(this.getVerticalBiomeCount());

		for (int i = 0; i < objectArrayList.size(); i++) {
			int j = i + BiomeCoords.fromBlock(this.oldHeightLimit.getBottomY());
			objectArrayList.set(
					i,
					chunk.getBiomeForNoiseGen(BiomeCoords.fromBlock(chunkBlockX), j, BiomeCoords.fromBlock(chunkBlockZ))
			);
		}

		return objectArrayList;
	}

	private static boolean isCollidableAndNotTreeAt(Chunk chunk, BlockPos pos) {
		BlockState blockState = chunk.getBlockState(pos);

		if (blockState.isAir()) {
			return false;
		}

		if (blockState.isIn(BlockTags.LEAVES) || blockState.isIn(BlockTags.LOGS)) {
			return false;
		}

		if (blockState.isOf(Blocks.BROWN_MUSHROOM_BLOCK) || blockState.isOf(Blocks.RED_MUSHROOM_BLOCK)) {
			return false;
		}

		return !blockState.getCollisionShape(chunk, pos).isEmpty();
	}

	protected double getHeight(int biomeX, int biomeY, int biomeZ) {
		if (biomeX == CHUNK_BIOME_END_INDEX || biomeZ == CHUNK_BIOME_END_INDEX) {
			return this.surfaceHeights[getSouthEastIndex(biomeX, biomeZ)];
		}
		else {
			return biomeX != 0 && biomeZ != 0 ? Double.MAX_VALUE
			                                  : this.surfaceHeights[getNorthWestIndex(biomeX, biomeZ)];
		}
	}

	private double getCollidableBlockDensity(double @Nullable [] collidableBlockDensityColumn, int halfSectionY) {
		if (collidableBlockDensityColumn == null) {
			return Double.MAX_VALUE;
		}
		else {
			int i = this.getHalfSectionHeight(halfSectionY);
			return i >= 0 && i < collidableBlockDensityColumn.length ? collidableBlockDensityColumn[i] * COLLIDABLE_BLOCK_DENSITY_THRESHOLD
			                                                         : Double.MAX_VALUE;
		}
	}

	protected double getCollidableBlockDensity(int chunkBiomeX, int halfSectionY, int chunkBiomeZ) {
		if (halfSectionY == this.getBottomHalfSectionY()) {
			return COLLIDABLE_BLOCK_DENSITY_THRESHOLD;
		}
		else if (chunkBiomeX == CHUNK_BIOME_END_INDEX || chunkBiomeZ == CHUNK_BIOME_END_INDEX) {
			return this.getCollidableBlockDensity(
					this.collidableBlockDensities[getSouthEastIndex(
							chunkBiomeX,
							chunkBiomeZ
					)], halfSectionY
			);
		}
		else {
			return chunkBiomeX != 0 && chunkBiomeZ != 0
			       ? Double.MAX_VALUE
			       : this.getCollidableBlockDensity(
					       this.collidableBlockDensities[getNorthWestIndex(
							       chunkBiomeX,
							       chunkBiomeZ
					       )], halfSectionY
			       );
		}
	}

	/**
	 * Accept biomes.
	 *
	 * @param biomeX biome x
	 * @param biomeY biome y
	 * @param biomeZ biome z
	 * @param consumer consumer
	 */
	protected void acceptBiomes(int biomeX, int biomeY, int biomeZ, BlendingData.BiomeConsumer consumer) {
		if (biomeY >= BiomeCoords.fromBlock(this.oldHeightLimit.getBottomY())
				&& biomeY <= BiomeCoords.fromBlock(this.oldHeightLimit.getTopYInclusive())) {
			int i = biomeY - BiomeCoords.fromBlock(this.oldHeightLimit.getBottomY());

			for (int j = 0; j < this.biomes.size(); j++) {
				List<RegistryEntry<Biome>> list = this.biomes.get(j);
				if (list != null) {
					RegistryEntry<Biome> registryEntry = list.get(i);
					if (registryEntry != null) {
						consumer.consume(biomeX + getX(j), biomeZ + getZ(j), registryEntry);
					}
				}
			}
		}
	}

	/**
	 * Accept heights.
	 *
	 * @param biomeX biome x
	 * @param biomeZ biome z
	 * @param consumer consumer
	 */
	protected void acceptHeights(int biomeX, int biomeZ, BlendingData.HeightConsumer consumer) {
		for (int i = 0; i < this.surfaceHeights.length; i++) {
			double d = this.surfaceHeights[i];
			if (d != Double.MAX_VALUE) {
				consumer.consume(biomeX + getX(i), biomeZ + getZ(i), d);
			}
		}
	}

	protected void acceptCollidableBlockDensities(
			int biomeX,
			int biomeZ,
			int minHalfSectionY,
			int maxHalfSectionY,
			BlendingData.CollidableBlockDensityConsumer consumer
	) {
		int i = this.getOneAboveBottomHalfSectionY();
		int j = Math.max(0, minHalfSectionY - i);
		int k = Math.min(this.getVerticalHalfSectionCount(), maxHalfSectionY - i);

		for (int l = 0; l < this.collidableBlockDensities.length; l++) {
			double[] ds = this.collidableBlockDensities[l];
			if (ds != null) {
				int m = biomeX + getX(l);
				int n = biomeZ + getZ(l);

				for (int o = j; o < k; o++) {
					consumer.consume(m, o + i, n, ds[o] * COLLIDABLE_BLOCK_DENSITY_THRESHOLD);
				}
			}
		}
	}

	private int getVerticalHalfSectionCount() {
		return this.oldHeightLimit.countVerticalSections() * 2;
	}

	private int getVerticalBiomeCount() {
		return BiomeCoords.fromChunk(this.oldHeightLimit.countVerticalSections());
	}

	private int getOneAboveBottomHalfSectionY() {
		return this.getBottomHalfSectionY() + 1;
	}

	private int getBottomHalfSectionY() {
		return this.oldHeightLimit.getBottomSectionCoord() * 2;
	}

	private int getHalfSectionHeight(int halfSectionY) {
		return halfSectionY - this.getOneAboveBottomHalfSectionY();
	}

	private static int getNorthWestIndex(int chunkBiomeX, int chunkBiomeZ) {
		return LAST_CHUNK_BIOME_INDEX - chunkBiomeX + chunkBiomeZ;
	}

	private static int getSouthEastIndex(int chunkBiomeX, int chunkBiomeZ) {
		return NORTH_WEST_END_INDEX + chunkBiomeX + CHUNK_BIOME_END_INDEX - chunkBiomeZ;
	}

	private static int getX(int index) {
		if (index < NORTH_WEST_END_INDEX) {
			return clampPositive(LAST_CHUNK_BIOME_INDEX - index);
		}
		else {
			int i = index - NORTH_WEST_END_INDEX;
			return CHUNK_BIOME_END_INDEX - clampPositive(CHUNK_BIOME_END_INDEX - i);
		}
	}

	private static int getZ(int index) {
		if (index < NORTH_WEST_END_INDEX) {
			return clampPositive(index - LAST_CHUNK_BIOME_INDEX);
		}
		else {
			int i = index - NORTH_WEST_END_INDEX;
			return CHUNK_BIOME_END_INDEX - clampPositive(i - CHUNK_BIOME_END_INDEX);
		}
	}

	private static int clampPositive(int value) {
		return value & ~(value >> 31);
	}

	public HeightLimitView getOldHeightLimit() {
		return this.oldHeightLimit;
	}

	/**
	 * {@code BiomeConsumer}.
	 */
	protected interface BiomeConsumer {

		void consume(int biomeX, int biomeZ, RegistryEntry<Biome> biome);
	}

	/**
	 * {@code CollidableBlockDensityConsumer}.
	 */
	protected interface CollidableBlockDensityConsumer {

		void consume(int biomeX, int halfSectionY, int biomeZ, double collidableBlockDensity);
	}

	/**
	 * {@code HeightConsumer}.
	 */
	protected interface HeightConsumer {

		void consume(int biomeX, int biomeZ, double height);
	}

	/**
	 * Сериализованное представление данных смешивания для хранения в NBT чанка.
	 */
	public record Serialized(int minSection, int maxSection, Optional<double[]> heights) {

		private static final Codec<double[]> DOUBLE_ARRAY_CODEC =
				Codec.DOUBLE.listOf().xmap(Doubles::toArray, Doubles::asList);

		public static final Codec<BlendingData.Serialized> CODEC = RecordCodecBuilder.<BlendingData.Serialized>create(
				instance -> instance.group(
						Codec.INT.fieldOf("min_section").forGetter(BlendingData.Serialized::minSection),
						Codec.INT.fieldOf("max_section").forGetter(BlendingData.Serialized::maxSection),
						DOUBLE_ARRAY_CODEC.lenientOptionalFieldOf("heights").forGetter(BlendingData.Serialized::heights)
				).apply(instance, BlendingData.Serialized::new)
		).validate(BlendingData.Serialized::validate);

		private static DataResult<BlendingData.Serialized> validate(BlendingData.Serialized serialized) {
			return serialized.heights.isPresent()
					&& serialized.heights.get().length != BlendingData.HORIZONTAL_BIOME_COUNT
					? DataResult.error(() -> "heights has to be of length " + BlendingData.HORIZONTAL_BIOME_COUNT)
					: DataResult.success(serialized);
		}
	}
}
