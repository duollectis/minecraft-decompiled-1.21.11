package net.minecraft.world.gen.chunk;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.*;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.BuiltinNoiseParameters;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Выполняет смешивание (blending) рельефа и биомов на границе старых и новых чанков.
 * Обеспечивает плавный переход между чанками, сгенерированными разными версиями генератора.
 */
public class Blender {

	private static final Blender
			NO_BLENDING =
			new Blender(new Long2ObjectOpenHashMap<>(), new Long2ObjectOpenHashMap<>()) {
				@Override
				public Blender.BlendResult calculate(int blockX, int blockZ) {
					return new Blender.BlendResult(1.0, 0.0);
				}

				@Override
				public double applyBlendDensity(DensityFunction.NoisePos pos, double density) {
					return density;
				}

				@Override
				public BiomeSupplier getBiomeSupplier(BiomeSupplier biomeSupplier) {
					return biomeSupplier;
				}
			};

	private static final DoublePerlinNoiseSampler OFFSET_NOISE = DoublePerlinNoiseSampler.create(
			new Xoroshiro128PlusPlusRandom(42L), BuiltinNoiseParameters.OFFSET
	);
	private static final int BLENDING_BIOME_DISTANCE_THRESHOLD = BiomeCoords.fromChunk(7) - 1;
	private static final int
			BLENDING_CHUNK_DISTANCE_THRESHOLD =
			BiomeCoords.toChunk(BLENDING_BIOME_DISTANCE_THRESHOLD + 3);
	private static final int BLEND_RADIUS = 2;
	private static final int CLOSE_BLENDING_DISTANCE_THRESHOLD = BiomeCoords.toChunk(5);
	private static final double BLEND_SCALE = 8.0;

	private final Long2ObjectOpenHashMap<BlendingData> blendingData;
	private final Long2ObjectOpenHashMap<BlendingData> closeBlendingData;

	/**
	 * @return экземпляр блендера без смешивания (no-op)
	 */
	public static Blender getNoBlending() {
		return NO_BLENDING;
	}

	/**
	 * Создаёт блендер для указанного региона чанков, собирая данные смешивания из соседних чанков.
	 *
	 * @param chunkRegion регион чанков, или {@code null} для отключения смешивания
	 * @return блендер с данными смешивания или {@link #NO_BLENDING}
	 */
	public static Blender getBlender(@Nullable ChunkRegion chunkRegion) {
		if (SharedConstants.DISABLE_BLENDING || chunkRegion == null) {
			return NO_BLENDING;
		}

		ChunkPos chunkPos = chunkRegion.getCenterPos();

		if (!chunkRegion.needsBlending(chunkPos, BLENDING_CHUNK_DISTANCE_THRESHOLD)) {
			return NO_BLENDING;
		}

		Long2ObjectOpenHashMap<BlendingData> allData = new Long2ObjectOpenHashMap<>();
		Long2ObjectOpenHashMap<BlendingData> closeData = new Long2ObjectOpenHashMap<>();
		int radiusSquared = MathHelper.square(BLENDING_CHUNK_DISTANCE_THRESHOLD + 1);

		for (int j = -BLENDING_CHUNK_DISTANCE_THRESHOLD; j <= BLENDING_CHUNK_DISTANCE_THRESHOLD; j++) {
			for (int k = -BLENDING_CHUNK_DISTANCE_THRESHOLD; k <= BLENDING_CHUNK_DISTANCE_THRESHOLD; k++) {
				if (j * j + k * k <= radiusSquared) {
					int l = chunkPos.x + j;
					int m = chunkPos.z + k;
					BlendingData found = BlendingData.getBlendingData(chunkRegion, l, m);

					if (found != null) {
						allData.put(ChunkPos.toLong(l, m), found);

						if (j >= -CLOSE_BLENDING_DISTANCE_THRESHOLD
								&& j <= CLOSE_BLENDING_DISTANCE_THRESHOLD
								&& k >= -CLOSE_BLENDING_DISTANCE_THRESHOLD
								&& k <= CLOSE_BLENDING_DISTANCE_THRESHOLD
						) {
							closeData.put(ChunkPos.toLong(l, m), found);
						}
					}
				}
			}
		}

		return allData.isEmpty() && closeData.isEmpty()
		       ? NO_BLENDING
		       : new Blender(allData, closeData);
	}

	/**
	 * @param blendingData      карта данных смешивания для всех соседних чанков
	 * @param closeBlendingData карта данных смешивания для ближних соседних чанков
	 */
	Blender(Long2ObjectOpenHashMap<BlendingData> blendingData, Long2ObjectOpenHashMap<BlendingData> closeBlendingData) {
		this.blendingData = blendingData;
		this.closeBlendingData = closeBlendingData;
	}

	/**
	 * @return {@code true}, если данные смешивания отсутствуют
	 */
	public boolean isEmpty() {
		return this.blendingData.isEmpty() && this.closeBlendingData.isEmpty();
	}

	/**
	 * Вычисляет результат смешивания для указанных координат блока.
	 *
	 * @param blockX координата X блока
	 * @param blockZ координата Z блока
	 * @return результат смешивания с альфой и смещением
	 */
	public Blender.BlendResult calculate(int blockX, int blockZ) {
		int i = BiomeCoords.fromBlock(blockX);
		int j = BiomeCoords.fromBlock(blockZ);
		double d = this.sampleClosest(i, 0, j, BlendingData::getHeight);

		if (d != Double.MAX_VALUE) {
			return new Blender.BlendResult(0.0, getBlendOffset(d));
		}

		MutableDouble mutableDouble = new MutableDouble(0.0);
		MutableDouble mutableDouble2 = new MutableDouble(0.0);
		MutableDouble mutableDouble3 = new MutableDouble(Double.POSITIVE_INFINITY);

		this.blendingData.forEach(
				(chunkPos, data) -> data.acceptHeights(
						BiomeCoords.fromChunk(ChunkPos.getPackedX(chunkPos)),
						BiomeCoords.fromChunk(ChunkPos.getPackedZ(chunkPos)),
						(biomeX, biomeZ, height) -> {
							double dx = MathHelper.hypot((float) (i - biomeX), (float) (j - biomeZ));

							if (!(dx > BLENDING_BIOME_DISTANCE_THRESHOLD)) {
								if (dx < mutableDouble3.doubleValue()) {
									mutableDouble3.setValue(dx);
								}

								double ex = 1.0 / (dx * dx * dx * dx);
								mutableDouble2.add(height * ex);
								mutableDouble.add(ex);
							}
						}
				)
		);

		if (mutableDouble3.doubleValue() == Double.POSITIVE_INFINITY) {
			return new Blender.BlendResult(1.0, 0.0);
		}

		double e = mutableDouble2.doubleValue() / mutableDouble.doubleValue();
		double f = MathHelper.clamp(mutableDouble3.doubleValue() / (BLENDING_BIOME_DISTANCE_THRESHOLD + 1), 0.0, 1.0);
		f = 3.0 * f * f - 2.0 * f * f * f;

		return new Blender.BlendResult(f, getBlendOffset(e));
	}

	/**
	 * Вычисляет смещение плотности для смешивания по высоте.
	 */
	private static double getBlendOffset(double height) {
		double d = 1.0;
		double e = height + 0.5;
		double f = MathHelper.floorMod(e, 8.0);
		return 1.0 * (32.0 * (e - 128.0) - 3.0 * (e - 120.0) * f + 3.0 * f * f) / (128.0 * (32.0 - 3.0 * f));
	}

	/**
	 * Применяет смешивание плотности для генерации рельефа.
	 *
	 * @param pos     позиция в пространстве шума
	 * @param density исходная плотность
	 * @return смешанная плотность
	 */
	public double applyBlendDensity(DensityFunction.NoisePos pos, double density) {
		int i = BiomeCoords.fromBlock(pos.blockX());
		int j = pos.blockY() / 8;
		int k = BiomeCoords.fromBlock(pos.blockZ());
		double d = this.sampleClosest(i, j, k, BlendingData::getCollidableBlockDensity);

		if (d != Double.MAX_VALUE) {
			return d;
		}

		MutableDouble mutableDouble = new MutableDouble(0.0);
		MutableDouble mutableDouble2 = new MutableDouble(0.0);
		MutableDouble mutableDouble3 = new MutableDouble(Double.POSITIVE_INFINITY);

		this.closeBlendingData.forEach(
				(chunkPos, data) -> data.acceptCollidableBlockDensities(
						BiomeCoords.fromChunk(ChunkPos.getPackedX(chunkPos)),
						BiomeCoords.fromChunk(ChunkPos.getPackedZ(chunkPos)),
						j - 1,
						j + 1,
						(biomeX, halfSectionY, biomeZ, collidableBlockDensity) -> {
							double dx = MathHelper.magnitude(
									(double) (i - biomeX),
									(double) ((j - halfSectionY) * 2),
									(double) (k - biomeZ)
							);

							if (!(dx > 2.0)) {
								if (dx < mutableDouble3.doubleValue()) {
									mutableDouble3.setValue(dx);
								}

								double ex = 1.0 / (dx * dx * dx * dx);
								mutableDouble2.add(collidableBlockDensity * ex);
								mutableDouble.add(ex);
							}
						}
				)
		);

		if (mutableDouble3.doubleValue() == Double.POSITIVE_INFINITY) {
			return density;
		}

		double e = mutableDouble2.doubleValue() / mutableDouble.doubleValue();
		double f = MathHelper.clamp(mutableDouble3.doubleValue() / 3.0, 0.0, 1.0);

		return MathHelper.lerp(f, e, density);
	}

	/**
	 * Сэмплирует ближайшее значение из данных смешивания для указанных биомных координат.
	 */
	private double sampleClosest(int biomeX, int biomeY, int biomeZ, Blender.BlendingSampler sampler) {
		int i = BiomeCoords.toChunk(biomeX);
		int j = BiomeCoords.toChunk(biomeZ);
		boolean bl = (biomeX & 3) == 0;
		boolean bl2 = (biomeZ & 3) == 0;
		double d = this.sample(sampler, i, j, biomeX, biomeY, biomeZ);

		if (d == Double.MAX_VALUE) {
			if (bl && bl2) {
				d = this.sample(sampler, i - 1, j - 1, biomeX, biomeY, biomeZ);
			}

			if (d == Double.MAX_VALUE) {
				if (bl) {
					d = this.sample(sampler, i - 1, j, biomeX, biomeY, biomeZ);
				}

				if (d == Double.MAX_VALUE && bl2) {
					d = this.sample(sampler, i, j - 1, biomeX, biomeY, biomeZ);
				}
			}
		}

		return d;
	}

	/**
	 * Сэмплирует значение из данных смешивания конкретного чанка.
	 */
	private double sample(Blender.BlendingSampler sampler, int chunkX, int chunkZ, int biomeX, int biomeY, int biomeZ) {
		BlendingData chunkData = (BlendingData) this.blendingData.get(ChunkPos.toLong(chunkX, chunkZ));

		return chunkData != null
		       ? sampler.get(
				chunkData,
				biomeX - BiomeCoords.fromChunk(chunkX),
				biomeY,
				biomeZ - BiomeCoords.fromChunk(chunkZ)
		)
		       : Double.MAX_VALUE;
	}

	/**
	 * Оборачивает поставщика биомов, добавляя смешивание биомов на границах старых чанков.
	 *
	 * @param biomeSupplier исходный поставщик биомов
	 * @return поставщик биомов со смешиванием
	 */
	public BiomeSupplier getBiomeSupplier(BiomeSupplier biomeSupplier) {
		return (x, y, z, noise) -> {
			RegistryEntry<Biome> registryEntry = this.blendBiome(x, y, z);
			return registryEntry == null ? biomeSupplier.getBiome(x, y, z, noise) : registryEntry;
		};
	}

	/**
	 * Возвращает смешанный биом для указанных координат, или {@code null} если смешивание не нужно.
	 */
	private RegistryEntry<Biome> blendBiome(int x, int y, int z) {
		MutableDouble mutableDouble = new MutableDouble(Double.POSITIVE_INFINITY);
		MutableObject<RegistryEntry<Biome>> mutableObject = new MutableObject<>();

		this.blendingData.forEach(
				(chunkPos, data) -> data.acceptBiomes(
						BiomeCoords.fromChunk(ChunkPos.getPackedX(chunkPos)),
						y,
						BiomeCoords.fromChunk(ChunkPos.getPackedZ(chunkPos)),
						(biomeX, biomeZ, biome) -> {
							double dx = MathHelper.hypot((float) (x - biomeX), (float) (z - biomeZ));

							if (!(dx > BLENDING_BIOME_DISTANCE_THRESHOLD)) {
								if (dx < mutableDouble.doubleValue()) {
									mutableObject.setValue(biome);
									mutableDouble.setValue(dx);
								}
							}
						}
				)
		);

		if (mutableDouble.doubleValue() == Double.POSITIVE_INFINITY) {
			return null;
		}

		double d = OFFSET_NOISE.sample(x, 0.0, z) * 12.0;
		double
				e =
				MathHelper.clamp((mutableDouble.doubleValue() + d) / (BLENDING_BIOME_DISTANCE_THRESHOLD + 1), 0.0, 1.0);

		return e > 0.5 ? null : (RegistryEntry) mutableObject.get();
	}

	/**
	 * Помечает листья и жидкости на границах старых чанков для пост-обработки.
	 *
	 * @param chunkRegion регион чанков
	 * @param chunk       обрабатываемый чанк
	 */
	public static void tickLeavesAndFluids(ChunkRegion chunkRegion, Chunk chunk) {
		if (SharedConstants.DISABLE_BLENDING) {
			return;
		}

		ChunkPos chunkPos = chunk.getPos();
		boolean bl = chunk.usesOldNoise();
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		BlockPos blockPos = new BlockPos(chunkPos.getStartX(), 0, chunkPos.getStartZ());
		BlendingData chunkBlendingData = chunk.getBlendingData();

		if (chunkBlendingData != null) {
			int i = chunkBlendingData.getOldHeightLimit().getBottomY();
			int j = chunkBlendingData.getOldHeightLimit().getTopYInclusive();

			if (bl) {
				for (int k = 0; k < 16; k++) {
					for (int l = 0; l < 16; l++) {
						tickLeavesAndFluids(chunk, mutable.set(blockPos, k, i - 1, l));
						tickLeavesAndFluids(chunk, mutable.set(blockPos, k, i, l));
						tickLeavesAndFluids(chunk, mutable.set(blockPos, k, j, l));
						tickLeavesAndFluids(chunk, mutable.set(blockPos, k, j + 1, l));
					}
				}
			}

			for (Direction direction : Direction.Type.HORIZONTAL) {
				if (chunkRegion
						.getChunk(chunkPos.x + direction.getOffsetX(), chunkPos.z + direction.getOffsetZ())
						.usesOldNoise() != bl) {
					int m = direction == Direction.EAST ? 15 : 0;
					int n = direction == Direction.WEST ? 0 : 15;
					int o = direction == Direction.SOUTH ? 15 : 0;
					int p = direction == Direction.NORTH ? 0 : 15;

					for (int q = m; q <= n; q++) {
						for (int r = o; r <= p; r++) {
							int s = Math.min(j, chunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, q, r)) + 1;

							for (int t = i; t < s; t++) {
								tickLeavesAndFluids(chunk, mutable.set(blockPos, q, t, r));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Помечает один блок для пост-обработки, если это листья или жидкость.
	 */
	private static void tickLeavesAndFluids(Chunk chunk, BlockPos pos) {
		BlockState blockState = chunk.getBlockState(pos);

		if (blockState.isIn(BlockTags.LEAVES)) {
			chunk.markBlockForPostProcessing(pos);
		}

		FluidState fluidState = chunk.getFluidState(pos);

		if (!fluidState.isEmpty()) {
			chunk.markBlockForPostProcessing(pos);
		}
	}

	/**
	 * Создаёт маски карвинга для чанка на основе данных смешивания соседей.
	 *
	 * @param world мир
	 * @param chunk прото-чанк для обработки
	 */
	public static void createCarvingMasks(StructureWorldAccess world, ProtoChunk chunk) {
		if (SharedConstants.DISABLE_BLENDING) {
			return;
		}

		ChunkPos chunkPos = chunk.getPos();
		Builder<EightWayDirection, BlendingData> builder = ImmutableMap.builder();

		for (EightWayDirection eightWayDirection : EightWayDirection.values()) {
			int i = chunkPos.x + eightWayDirection.getOffsetX();
			int j = chunkPos.z + eightWayDirection.getOffsetZ();
			BlendingData neighborData = world.getChunk(i, j).getBlendingData();

			if (neighborData != null) {
				builder.put(eightWayDirection, neighborData);
			}
		}

		ImmutableMap<EightWayDirection, BlendingData> immutableMap = builder.build();

		if (chunk.usesOldNoise() || !immutableMap.isEmpty()) {
			Blender.DistanceFunction
					distanceFunction =
					createClosestDistanceFunction(chunk.getBlendingData(), immutableMap);
			CarvingMask.MaskPredicate maskPredicate = (offsetX, y, offsetZ) -> {
				double d = offsetX + 0.5 + OFFSET_NOISE.sample(offsetX, y, offsetZ) * 4.0;
				double e = y + 0.5 + OFFSET_NOISE.sample(y, offsetZ, offsetX) * 4.0;
				double f = offsetZ + 0.5 + OFFSET_NOISE.sample(offsetZ, offsetX, y) * 4.0;
				return distanceFunction.getDistance(d, e, f) < 4.0;
			};

			chunk.getOrCreateCarvingMask().setMaskPredicate(maskPredicate);
		}
	}

	/**
	 * Создаёт функцию расстояния, возвращающую минимум по всем источникам данных смешивания.
	 *
	 * @param data         данные смешивания текущего чанка (может быть {@code null})
	 * @param neighborData данные смешивания соседних чанков
	 * @return функция минимального расстояния
	 */
	public static Blender.DistanceFunction createClosestDistanceFunction(
			@Nullable BlendingData data,
			Map<EightWayDirection, BlendingData> neighborData
	) {
		List<Blender.DistanceFunction> list = Lists.newArrayList();

		if (data != null) {
			list.add(createDistanceFunction(null, data));
		}

		neighborData.forEach((direction, datax) -> list.add(createDistanceFunction(direction, datax)));

		return (offsetX, y, offsetZ) -> {
			double d = Double.POSITIVE_INFINITY;

			for (Blender.DistanceFunction distanceFunction : list) {
				double e = distanceFunction.getDistance(offsetX, y, offsetZ);

				if (e < d) {
					d = e;
				}
			}

			return d;
		};
	}

	/**
	 * Создаёт функцию расстояния для конкретного источника данных смешивания.
	 */
	private static Blender.DistanceFunction createDistanceFunction(
			@Nullable EightWayDirection direction,
			BlendingData data
	) {
		double d = 0.0;
		double e = 0.0;

		if (direction != null) {
			for (Direction direction2 : direction.getDirections()) {
				d += direction2.getOffsetX() * 16;
				e += direction2.getOffsetZ() * 16;
			}
		}

		double f = d;
		double g = e;
		double h = data.getOldHeightLimit().getHeight() / 2.0;
		double i = data.getOldHeightLimit().getBottomY() + h;

		return (offsetX, y, offsetZ) -> getDistance(offsetX - 8.0 - f, y - i, offsetZ - 8.0 - g, 8.0, h, 8.0);
	}

	/**
	 * Вычисляет расстояние от точки до прямоугольного параллелепипеда.
	 */
	private static double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
		double d = Math.abs(x1) - x2;
		double e = Math.abs(y1) - y2;
		double f = Math.abs(z1) - z2;
		return MathHelper.magnitude(Math.max(0.0, d), Math.max(0.0, e), Math.max(0.0, f));
	}

	/**
	 * Результат смешивания: альфа-коэффициент и смещение плотности.
	 *
	 * @param alpha          коэффициент смешивания (0 = полное смешивание, 1 = без смешивания)
	 * @param blendingOffset смещение плотности от старого рельефа
	 */
	public record BlendResult(double alpha, double blendingOffset) {
	}

	/**
	 * Функциональный интерфейс для сэмплирования значений из данных смешивания.
	 */
	interface BlendingSampler {

		double get(BlendingData data, int biomeX, int biomeY, int biomeZ);
	}

	/**
	 * Функциональный интерфейс для вычисления расстояния в пространстве смешивания.
	 */
	public interface DistanceFunction {

		double getDistance(double offsetX, double y, double offsetZ);
	}
}
