package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.floatprovider.ClampedNormalFloatProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.feature.util.CaveSurface;
import net.minecraft.world.gen.feature.util.DripstoneHelper;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Генерирует кластер сталактитов и сталагмитов из друзы (dripstone).
 * Для каждой позиции в радиусе находит потолок и пол пещеры,
 * затем размещает пары сталактит/сталагмит с учётом высоты пещеры.
 */
public class DripstoneClusterFeature extends Feature<DripstoneClusterFeatureConfig> {

	public DripstoneClusterFeature(Codec<DripstoneClusterFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DripstoneClusterFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		DripstoneClusterFeatureConfig config = context.getConfig();
		Random random = context.getRandom();

		if (!DripstoneHelper.canGenerate(world, origin)) {
			return false;
		}

		int height = config.height.get(random);
		float wetness = config.wetness.get(random);
		float density = config.density.get(random);
		int radiusX = config.radius.get(random);
		int radiusZ = config.radius.get(random);

		for (int dx = -radiusX; dx <= radiusX; dx++) {
			for (int dz = -radiusZ; dz <= radiusZ; dz++) {
				double chance = dripstoneChance(radiusX, radiusZ, dx, dz, config);
				BlockPos columnPos = origin.add(dx, 0, dz);
				generateColumn(world, random, columnPos, dx, dz, wetness, chance, height, density, config);
			}
		}

		return true;
	}

	/**
	 * Генерирует пару сталактит/сталагмит в одной вертикальной колонне.
	 * Учитывает наличие воды, лавы и высоту пещеры для корректного размещения.
	 */
	private void generateColumn(
		StructureWorldAccess world,
		Random random,
		BlockPos pos,
		int localX,
		int localZ,
		float wetness,
		double dripstoneChance,
		int maxHeight,
		float density,
		DripstoneClusterFeatureConfig config
	) {
		Optional<CaveSurface> surface = CaveSurface.create(
			world,
			pos,
			config.floorToCeilingSearchRange,
			DripstoneHelper::canGenerate,
			DripstoneHelper::cannotGenerate
		);

		if (surface.isEmpty()) {
			return;
		}

		OptionalInt ceilingY = surface.get().getCeilingHeight();
		OptionalInt floorY = surface.get().getFloorHeight();

		if (ceilingY.isEmpty() && floorY.isEmpty()) {
			return;
		}

		// Опционально заполняем пол водой
		boolean placeWater = random.nextFloat() < wetness;
		CaveSurface effectiveSurface;

		if (placeWater && floorY.isPresent() && canWaterSpawn(world, pos.withY(floorY.getAsInt()))) {
			int waterY = floorY.getAsInt();
			effectiveSurface = surface.get().withFloor(OptionalInt.of(waterY - 1));
			world.setBlockState(pos.withY(waterY), Blocks.WATER.getDefaultState(), 2);
		} else {
			effectiveSurface = surface.get();
		}

		OptionalInt effectiveFloorY = effectiveSurface.getFloorHeight();

		// Высота сталактита (свисает с потолка)
		boolean placeStalactite = random.nextDouble() < dripstoneChance;
		int stalactiteHeight;

		if (ceilingY.isPresent() && placeStalactite && !isLava(world, pos.withY(ceilingY.getAsInt()))) {
			int blockLayerThickness = config.dripstoneBlockLayerThickness.get(random);
			placeDripstoneBlocks(world, pos.withY(ceilingY.getAsInt()), blockLayerThickness, Direction.UP);

			int availableHeight = effectiveFloorY.isPresent()
				? Math.min(maxHeight, ceilingY.getAsInt() - effectiveFloorY.getAsInt())
				: maxHeight;

			stalactiteHeight = getHeight(random, localX, localZ, density, availableHeight, config);
		} else {
			stalactiteHeight = 0;
		}

		// Высота сталагмита (растёт с пола)
		boolean placeStalagmite = random.nextDouble() < dripstoneChance;
		int stalagmiteHeight;

		if (effectiveFloorY.isPresent() && placeStalagmite && !isLava(world, pos.withY(effectiveFloorY.getAsInt()))) {
			int blockLayerThickness = config.dripstoneBlockLayerThickness.get(random);
			placeDripstoneBlocks(world, pos.withY(effectiveFloorY.getAsInt()), blockLayerThickness, Direction.DOWN);

			if (ceilingY.isPresent()) {
				stalagmiteHeight = Math.max(
					0,
					stalactiteHeight + MathHelper.nextBetween(
						random,
						-config.maxStalagmiteStalactiteHeightDiff,
						config.maxStalagmiteStalactiteHeightDiff
					)
				);
			} else {
				stalagmiteHeight = getHeight(random, localX, localZ, density, maxHeight, config);
			}
		} else {
			stalagmiteHeight = 0;
		}

		// Корректируем высоты, если сталактит и сталагмит пересекаются
		int finalStalactiteHeight;
		int finalStalagmiteHeight;

		if (ceilingY.isPresent() && effectiveFloorY.isPresent()
			&& ceilingY.getAsInt() - stalactiteHeight <= effectiveFloorY.getAsInt() + stalagmiteHeight
		) {
			int floorVal = effectiveFloorY.getAsInt();
			int ceilVal = ceilingY.getAsInt();
			int minStalactiteBase = Math.max(ceilVal - stalactiteHeight, floorVal + 1);
			int maxStalagmiteTop = Math.min(floorVal + stalagmiteHeight, ceilVal - 1);
			int splitY = MathHelper.nextBetween(random, minStalactiteBase, maxStalagmiteTop + 1);
			finalStalactiteHeight = ceilVal - splitY;
			finalStalagmiteHeight = splitY - 1 - floorVal;
		} else {
			finalStalactiteHeight = stalactiteHeight;
			finalStalagmiteHeight = stalagmiteHeight;
		}

		boolean isMerged = random.nextBoolean()
			&& finalStalactiteHeight > 0
			&& finalStalagmiteHeight > 0
			&& effectiveSurface.getOptionalHeight().isPresent()
			&& finalStalactiteHeight + finalStalagmiteHeight == effectiveSurface.getOptionalHeight().getAsInt();

		if (ceilingY.isPresent()) {
			DripstoneHelper.generatePointedDripstone(world, pos.withY(ceilingY.getAsInt() - 1), Direction.DOWN, finalStalactiteHeight, isMerged);
		}

		if (effectiveFloorY.isPresent()) {
			DripstoneHelper.generatePointedDripstone(world, pos.withY(effectiveFloorY.getAsInt() + 1), Direction.UP, finalStalagmiteHeight, isMerged);
		}
	}

	private boolean isLava(WorldView world, BlockPos pos) {
		return world.getBlockState(pos).isOf(Blocks.LAVA);
	}

	/**
	 * Вычисляет высоту сталактита/сталагмита с учётом плотности и расстояния от центра.
	 * Возвращает 0, если случайное значение превышает плотность.
	 */
	private int getHeight(
		Random random,
		int localX,
		int localZ,
		float density,
		int maxHeight,
		DripstoneClusterFeatureConfig config
	) {
		if (random.nextFloat() > density) {
			return 0;
		}

		int distFromCenter = Math.abs(localX) + Math.abs(localZ);
		float heightBias = (float) MathHelper.clampedMap(
			distFromCenter,
			0.0,
			config.maxDistanceFromCenterAffectingHeightBias,
			maxHeight / 2.0,
			0.0
		);
		return (int) clampedGaussian(random, 0.0F, maxHeight, heightBias, config.heightDeviation);
	}

	private boolean canWaterSpawn(StructureWorldAccess world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);

		if (state.isOf(Blocks.WATER) || state.isOf(Blocks.DRIPSTONE_BLOCK) || state.isOf(Blocks.POINTED_DRIPSTONE)) {
			return false;
		}

		if (world.getBlockState(pos.up()).getFluidState().isIn(FluidTags.WATER)) {
			return false;
		}

		for (Direction direction : Direction.Type.HORIZONTAL) {
			if (!isStoneOrWater(world, pos.offset(direction))) {
				return false;
			}
		}

		return isStoneOrWater(world, pos.down());
	}

	private boolean isStoneOrWater(WorldAccess world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		return state.isIn(BlockTags.BASE_STONE_OVERWORLD) || state.getFluidState().isIn(FluidTags.WATER);
	}

	private void placeDripstoneBlocks(StructureWorldAccess world, BlockPos pos, int height, Direction direction) {
		BlockPos.Mutable mutable = pos.mutableCopy();

		for (int layer = 0; layer < height; layer++) {
			if (!DripstoneHelper.generateDripstoneBlock(world, mutable)) {
				return;
			}

			mutable.move(direction);
		}
	}

	/**
	 * Вычисляет вероятность появления сталактита/сталагмита в позиции.
	 * Вероятность растёт от краёв к центру кластера.
	 */
	private double dripstoneChance(
		int radiusX,
		int radiusZ,
		int localX,
		int localZ,
		DripstoneClusterFeatureConfig config
	) {
		int distToEdgeX = radiusX - Math.abs(localX);
		int distToEdgeZ = radiusZ - Math.abs(localZ);
		int minDistToEdge = Math.min(distToEdgeX, distToEdgeZ);
		return MathHelper.clampedMap(
			minDistToEdge,
			0.0F,
			config.maxDistanceFromCenterAffectingChanceOfDripstoneColumn,
			config.chanceOfDripstoneColumnAtMaxDistanceFromCenter,
			1.0F
		);
	}

	private static float clampedGaussian(Random random, float min, float max, float mean, float deviation) {
		return ClampedNormalFloatProvider.get(random, mean, deviation, min, max);
	}
}
