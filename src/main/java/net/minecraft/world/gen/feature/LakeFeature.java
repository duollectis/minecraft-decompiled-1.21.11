package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

/**
 * Генерирует небольшое озеро (лаву или воду) под землёй.
 * Форма озера — объединение нескольких случайных эллипсоидов в 16×8×16 объёме.
 * Верхняя половина (y >= 4) — воздух, нижняя — жидкость.
 * По периметру озера размещается барьерный блок (например, камень).
 *
 * @deprecated Устаревшая фича, заменена более новыми механизмами генерации.
 */
@Deprecated
public class LakeFeature extends Feature<LakeFeature.Config> {

	private static final BlockState CAVE_AIR = Blocks.CAVE_AIR.getDefaultState();
	private static final int LAKE_WIDTH = 16;
	private static final int LAKE_HEIGHT = 8;
	private static final int WATER_LEVEL = 4;
	private static final int MIN_Y_ABOVE_BOTTOM = 4;
	private static final int ORIGIN_DEPTH = 4;
	private static final int ELLIPSOID_COUNT_MIN = 4;
	private static final int ELLIPSOID_COUNT_EXTRA = 4;

	public LakeFeature(Codec<LakeFeature.Config> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<LakeFeature.Config> context) {
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		LakeFeature.Config config = context.getConfig();

		if (origin.getY() <= world.getBottomY() + MIN_Y_ABOVE_BOTTOM) {
			return false;
		}

		origin = origin.down(ORIGIN_DEPTH);

		// Булев 3D-массив [x][z][y] — маска заполненных ячеек озера
		boolean[] lakeShape = new boolean[LAKE_WIDTH * LAKE_WIDTH * LAKE_HEIGHT];
		int ellipsoidCount = random.nextInt(ELLIPSOID_COUNT_EXTRA) + ELLIPSOID_COUNT_MIN;

		for (int ellipsoid = 0; ellipsoid < ellipsoidCount; ellipsoid++) {
			double sizeX = random.nextDouble() * 6.0 + 3.0;
			double sizeY = random.nextDouble() * 4.0 + 2.0;
			double sizeZ = random.nextDouble() * 6.0 + 3.0;
			double centerX = random.nextDouble() * (LAKE_WIDTH - sizeX - 2.0) + 1.0 + sizeX / 2.0;
			double centerY = random.nextDouble() * (LAKE_HEIGHT - sizeY - 4.0) + 2.0 + sizeY / 2.0;
			double centerZ = random.nextDouble() * (LAKE_WIDTH - sizeZ - 2.0) + 1.0 + sizeZ / 2.0;

			for (int x = 1; x < LAKE_WIDTH - 1; x++) {
				for (int z = 1; z < LAKE_WIDTH - 1; z++) {
					for (int y = 1; y < LAKE_HEIGHT - 1; y++) {
						double nx = (x - centerX) / (sizeX / 2.0);
						double ny = (y - centerY) / (sizeY / 2.0);
						double nz = (z - centerZ) / (sizeZ / 2.0);

						if (nx * nx + ny * ny + nz * nz < 1.0) {
							lakeShape[(x * LAKE_WIDTH + z) * LAKE_HEIGHT + y] = true;
						}
					}
				}
			}
		}

		BlockState fluidState = config.fluid().get(random, origin);

		// Проверяем, что граничные блоки озера не нарушают структуру мира
		for (int x = 0; x < LAKE_WIDTH; x++) {
			for (int z = 0; z < LAKE_WIDTH; z++) {
				for (int y = 0; y < LAKE_HEIGHT; y++) {
					boolean isBorder = !lakeShape[(x * LAKE_WIDTH + z) * LAKE_HEIGHT + y]
						&& (
						(x < LAKE_WIDTH - 1 && lakeShape[((x + 1) * LAKE_WIDTH + z) * LAKE_HEIGHT + y])
							|| (x > 0 && lakeShape[((x - 1) * LAKE_WIDTH + z) * LAKE_HEIGHT + y])
							|| (z < LAKE_WIDTH - 1 && lakeShape[(x * LAKE_WIDTH + z + 1) * LAKE_HEIGHT + y])
							|| (z > 0 && lakeShape[(x * LAKE_WIDTH + (z - 1)) * LAKE_HEIGHT + y])
							|| (y < LAKE_HEIGHT - 1 && lakeShape[(x * LAKE_WIDTH + z) * LAKE_HEIGHT + y + 1])
							|| (y > 0 && lakeShape[(x * LAKE_WIDTH + z) * LAKE_HEIGHT + (y - 1)])
					);

					if (!isBorder) {
						continue;
					}

					BlockState existing = world.getBlockState(origin.add(x, y, z));

					if (y >= WATER_LEVEL && existing.isLiquid()) {
						return false;
					}

					if (y < WATER_LEVEL && !existing.isSolid() && world.getBlockState(origin.add(x, y, z)) != fluidState) {
						return false;
					}
				}
			}
		}

		// Размещаем жидкость и воздух
		for (int x = 0; x < LAKE_WIDTH; x++) {
			for (int z = 0; z < LAKE_WIDTH; z++) {
				for (int y = 0; y < LAKE_HEIGHT; y++) {
					if (lakeShape[(x * LAKE_WIDTH + z) * LAKE_HEIGHT + y] == false) {
						continue;
					}

					BlockPos pos = origin.add(x, y, z);

					if (!canReplace(world.getBlockState(pos))) {
						continue;
					}

					boolean isAboveWaterLevel = y >= WATER_LEVEL;
					world.setBlockState(pos, isAboveWaterLevel ? CAVE_AIR : fluidState, 2);

					if (isAboveWaterLevel) {
						world.scheduleBlockTick(pos, CAVE_AIR.getBlock(), 0);
						markBlocksAboveForPostProcessing(world, pos);
					}
				}
			}
		}

		// Размещаем барьерный блок по периметру
		BlockState barrierState = config.barrier().get(random, origin);

		if (barrierState.isAir()) {
			return true;
		}

		for (int x = 0; x < LAKE_WIDTH; x++) {
			for (int z = 0; z < LAKE_WIDTH; z++) {
				for (int y = 0; y < LAKE_HEIGHT; y++) {
					boolean isBorder = !lakeShape[(x * LAKE_WIDTH + z) * LAKE_HEIGHT + y]
						&& (
						(x < LAKE_WIDTH - 1 && lakeShape[((x + 1) * LAKE_WIDTH + z) * LAKE_HEIGHT + y])
							|| (x > 0 && lakeShape[((x - 1) * LAKE_WIDTH + z) * LAKE_HEIGHT + y])
							|| (z < LAKE_WIDTH - 1 && lakeShape[(x * LAKE_WIDTH + z + 1) * LAKE_HEIGHT + y])
							|| (z > 0 && lakeShape[(x * LAKE_WIDTH + (z - 1)) * LAKE_HEIGHT + y])
							|| (y < LAKE_HEIGHT - 1 && lakeShape[(x * LAKE_WIDTH + z) * LAKE_HEIGHT + y + 1])
							|| (y > 0 && lakeShape[(x * LAKE_WIDTH + z) * LAKE_HEIGHT + (y - 1)])
					);

					if (!isBorder || (y >= WATER_LEVEL && random.nextInt(2) == 0)) {
						continue;
					}

					BlockState existing = world.getBlockState(origin.add(x, y, z));

					if (existing.isSolid() && !existing.isIn(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE)) {
						BlockPos pos = origin.add(x, y, z);
						world.setBlockState(pos, barrierState, 2);
						markBlocksAboveForPostProcessing(world, pos);
					}
				}
			}
		}

		// Замораживаем поверхность воды в холодных биомах
		if (fluidState.getFluidState().isIn(FluidTags.WATER)) {
			for (int x = 0; x < LAKE_WIDTH; x++) {
				for (int z = 0; z < LAKE_WIDTH; z++) {
					BlockPos surfacePos = origin.add(x, WATER_LEVEL, z);

					if (world.getBiome(surfacePos).value().canSetIce(world, surfacePos, false)
						&& canReplace(world.getBlockState(surfacePos))
					) {
						world.setBlockState(surfacePos, Blocks.ICE.getDefaultState(), 2);
					}
				}
			}
		}

		return true;
	}

	private boolean canReplace(BlockState state) {
		return !state.isIn(BlockTags.FEATURES_CANNOT_REPLACE);
	}

	/**
	 * Конфигурация озера: жидкость внутри и барьерный блок по периметру.
	 */
	public record Config(BlockStateProvider fluid, BlockStateProvider barrier) implements FeatureConfig {

		public static final Codec<LakeFeature.Config> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
				BlockStateProvider.TYPE_CODEC.fieldOf("fluid").forGetter(LakeFeature.Config::fluid),
				BlockStateProvider.TYPE_CODEC.fieldOf("barrier").forGetter(LakeFeature.Config::barrier)
			).apply(instance, LakeFeature.Config::new)
		);
	}
}
