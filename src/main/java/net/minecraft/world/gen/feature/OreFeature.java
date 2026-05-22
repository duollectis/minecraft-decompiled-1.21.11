package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkSectionCache;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.BitSet;
import java.util.function.Function;

/**
 * Генерирует жилу руды в форме вытянутого эллипсоида.
 * Алгоритм строит цепочку перекрывающихся сфер вдоль случайного отрезка,
 * затем удаляет сферы, полностью поглощённые соседними (оптимизация).
 * Использует {@link ChunkSectionCache} для прямого доступа к секциям чанков
 * без лишних аллокаций.
 */
public class OreFeature extends Feature<OreFeatureConfig> {

	private static final int Y_RANDOM_RANGE = 3;
	private static final int Y_RANDOM_OFFSET = 2;

	public OreFeature(Codec<OreFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<OreFeatureConfig> context) {
		Random random = context.getRandom();
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();
		OreFeatureConfig config = context.getConfig();

		float angle = random.nextFloat() * (float) Math.PI;
		float halfSize = config.size / 8.0F;
		int padding = MathHelper.ceil((config.size / 16.0F * 2.0F + 1.0F) / 2.0F);

		double startX = origin.getX() + Math.sin(angle) * halfSize;
		double endX = origin.getX() - Math.sin(angle) * halfSize;
		double startZ = origin.getZ() + Math.cos(angle) * halfSize;
		double endZ = origin.getZ() - Math.cos(angle) * halfSize;
		double startY = origin.getY() + random.nextInt(Y_RANDOM_RANGE) - Y_RANDOM_OFFSET;
		double endY = origin.getY() + random.nextInt(Y_RANDOM_RANGE) - Y_RANDOM_OFFSET;

		int minX = origin.getX() - MathHelper.ceil(halfSize) - padding;
		int minY = origin.getY() - Y_RANDOM_OFFSET - padding;
		int minZ = origin.getZ() - MathHelper.ceil(halfSize) - padding;
		int horizontalSize = 2 * (MathHelper.ceil(halfSize) + padding);
		int verticalSize = 2 * (Y_RANDOM_OFFSET + padding);

		for (int x = minX; x <= minX + horizontalSize; x++) {
			for (int z = minZ; z <= minZ + horizontalSize; z++) {
				if (minY <= world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, x, z)) {
					return generateVeinPart(
						world,
						random,
						config,
						startX,
						endX,
						startZ,
						endZ,
						startY,
						endY,
						minX,
						minY,
						minZ,
						horizontalSize,
						verticalSize
					);
				}
			}
		}

		return false;
	}

	protected boolean generateVeinPart(
		StructureWorldAccess world,
		Random random,
		OreFeatureConfig config,
		double startX,
		double endX,
		double startZ,
		double endZ,
		double startY,
		double endY,
		int x,
		int y,
		int z,
		int horizontalSize,
		int verticalSize
	) {
		int placed = 0;
		BitSet visited = new BitSet(horizontalSize * verticalSize * horizontalSize);
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int veinSize = config.size;

		// Строим массив сфер вдоль отрезка [start, end]
		double[] spheres = new double[veinSize * 4];

		for (int idx = 0; idx < veinSize; idx++) {
			float progress = (float) idx / veinSize;
			double cx = MathHelper.lerp((double) progress, startX, endX);
			double cy = MathHelper.lerp((double) progress, startY, endY);
			double cz = MathHelper.lerp((double) progress, startZ, endZ);
			double radius = random.nextDouble() * veinSize / 16.0;
			double r = ((MathHelper.sin((float) Math.PI * progress) + 1.0F) * radius + 1.0) / 2.0;
			spheres[idx * 4] = cx;
			spheres[idx * 4 + 1] = cy;
			spheres[idx * 4 + 2] = cz;
			spheres[idx * 4 + 3] = r;
		}

		// Удаляем сферы, полностью поглощённые соседними
		for (int a = 0; a < veinSize - 1; a++) {
			if (spheres[a * 4 + 3] <= 0.0) {
				continue;
			}

			for (int b = a + 1; b < veinSize; b++) {
				if (spheres[b * 4 + 3] <= 0.0) {
					continue;
				}

				double dx = spheres[a * 4] - spheres[b * 4];
				double dy = spheres[a * 4 + 1] - spheres[b * 4 + 1];
				double dz = spheres[a * 4 + 2] - spheres[b * 4 + 2];
				double dr = spheres[a * 4 + 3] - spheres[b * 4 + 3];

				if (dr * dr > dx * dx + dy * dy + dz * dz) {
					if (dr > 0.0) {
						spheres[b * 4 + 3] = -1.0;
					} else {
						spheres[a * 4 + 3] = -1.0;
					}
				}
			}
		}

		try (ChunkSectionCache cache = new ChunkSectionCache(world)) {
			for (int idx = 0; idx < veinSize; idx++) {
				double radius = spheres[idx * 4 + 3];

				if (radius < 0.0) {
					continue;
				}

				double cx = spheres[idx * 4];
				double cy = spheres[idx * 4 + 1];
				double cz = spheres[idx * 4 + 2];

				int minBX = Math.max(MathHelper.floor(cx - radius), x);
				int minBY = Math.max(MathHelper.floor(cy - radius), y);
				int minBZ = Math.max(MathHelper.floor(cz - radius), z);
				int maxBX = Math.max(MathHelper.floor(cx + radius), minBX);
				int maxBY = Math.max(MathHelper.floor(cy + radius), minBY);
				int maxBZ = Math.max(MathHelper.floor(cz + radius), minBZ);

				for (int bx = minBX; bx <= maxBX; bx++) {
					double nx = (bx + 0.5 - cx) / radius;

					if (nx * nx >= 1.0) {
						continue;
					}

					for (int by = minBY; by <= maxBY; by++) {
						double ny = (by + 0.5 - cy) / radius;

						if (nx * nx + ny * ny >= 1.0) {
							continue;
						}

						for (int bz = minBZ; bz <= maxBZ; bz++) {
							double nz = (bz + 0.5 - cz) / radius;

							if (nx * nx + ny * ny + nz * nz >= 1.0 || world.isOutOfHeightLimit(by)) {
								continue;
							}

							int bitIndex = bx - x + (by - y) * horizontalSize + (bz - z) * horizontalSize * verticalSize;

							if (visited.get(bitIndex)) {
								continue;
							}

							visited.set(bitIndex);
							mutable.set(bx, by, bz);

							if (!world.isValidForSetBlock(mutable)) {
								continue;
							}

							ChunkSection section = cache.getSection(mutable);

							if (section == null) {
								continue;
							}

							int localX = ChunkSectionPos.getLocalCoord(bx);
							int localY = ChunkSectionPos.getLocalCoord(by);
							int localZ = ChunkSectionPos.getLocalCoord(bz);
							BlockState existing = section.getBlockState(localX, localY, localZ);

							for (OreFeatureConfig.Target target : config.targets) {
								if (shouldPlace(existing, cache::getBlockState, random, config, target, mutable)) {
									section.setBlockState(localX, localY, localZ, target.state, false);
									placed++;
									break;
								}
							}
						}
					}
				}
			}
		}

		return placed > 0;
	}

	public static boolean shouldPlace(
		BlockState state,
		Function<BlockPos, BlockState> posToState,
		Random random,
		OreFeatureConfig config,
		OreFeatureConfig.Target target,
		BlockPos.Mutable pos
	) {
		if (!target.target.test(state, random)) {
			return false;
		}

		return shouldNotDiscard(random, config.discardOnAirChance) || !isExposedToAir(posToState, pos);
	}

	protected static boolean shouldNotDiscard(Random random, float chance) {
		if (chance <= 0.0F) {
			return true;
		}

		return chance < 1.0F && random.nextFloat() >= chance;
	}
}
