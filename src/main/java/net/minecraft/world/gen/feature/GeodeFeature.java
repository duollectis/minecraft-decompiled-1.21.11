package net.minecraft.world.gen.feature;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BuddingAmethystBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.List;
import java.util.function.Predicate;

/**
 * Генерирует геоду — полую сферическую структуру из нескольких концентрических слоёв:
 * внешний камень → средний слой → внутренний слой → заполнение (воздух/кристаллы).
 * Использует шум Перлина для органичной деформации формы.
 * Случайно генерирует трещину, открывающую внутренность геоды наружу.
 */
public class GeodeFeature extends Feature<GeodeFeatureConfig> {

	private static final Direction[] DIRECTIONS = Direction.values();
	private static final int NOISE_OCTAVE = -4;
	private static final int CRACK_DIRECTION_COUNT = 4;

	public GeodeFeature(Codec<GeodeFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<GeodeFeatureConfig> context) {
		GeodeFeatureConfig config = context.getConfig();
		Random random = context.getRandom();
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();

		int minOffset = config.minGenOffset;
		int maxOffset = config.maxGenOffset;
		List<Pair<BlockPos, Integer>> distributionPoints = Lists.newLinkedList();
		int pointCount = config.distributionPoints.get(random);

		ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(world.getSeed()));
		DoublePerlinNoiseSampler noiseSampler = DoublePerlinNoiseSampler.create(chunkRandom, NOISE_OCTAVE, 1.0);
		List<BlockPos> crackPoints = Lists.newLinkedList();

		double pointDensity = (double) pointCount / config.outerWallDistance.getMax();
		GeodeLayerThicknessConfig thickness = config.layerThicknessConfig;
		GeodeLayerConfig layers = config.layerConfig;
		GeodeCrackConfig crack = config.crackConfig;

		double fillingThreshold = 1.0 / Math.sqrt(thickness.filling);
		double innerThreshold = 1.0 / Math.sqrt(thickness.innerLayer + pointDensity);
		double middleThreshold = 1.0 / Math.sqrt(thickness.middleLayer + pointDensity);
		double outerThreshold = 1.0 / Math.sqrt(thickness.outerLayer + pointDensity);
		double crackThreshold = 1.0 / Math.sqrt(
			crack.baseCrackSize + random.nextDouble() / 2.0 + (pointCount > 3 ? pointDensity : 0.0)
		);
		boolean hasCrack = random.nextFloat() < crack.generateCrackChance;
		int invalidCount = 0;

		for (int idx = 0; idx < pointCount; idx++) {
			int ox = config.outerWallDistance.get(random);
			int oy = config.outerWallDistance.get(random);
			int oz = config.outerWallDistance.get(random);
			BlockPos point = origin.add(ox, oy, oz);
			BlockState pointState = world.getBlockState(point);

			if (pointState.isAir() || pointState.isIn(layers.invalidBlocks)) {
				if (++invalidCount > config.invalidBlocksThreshold) {
					return false;
				}
			}

			distributionPoints.add(Pair.of(point, config.pointOffset.get(random)));
		}

		if (hasCrack) {
			int crackDir = random.nextInt(CRACK_DIRECTION_COUNT);
			int crackSpread = pointCount * 2 + 1;

			if (crackDir == 0) {
				crackPoints.add(origin.add(crackSpread, 7, 0));
				crackPoints.add(origin.add(crackSpread, 5, 0));
				crackPoints.add(origin.add(crackSpread, 1, 0));
			} else if (crackDir == 1) {
				crackPoints.add(origin.add(0, 7, crackSpread));
				crackPoints.add(origin.add(0, 5, crackSpread));
				crackPoints.add(origin.add(0, 1, crackSpread));
			} else if (crackDir == 2) {
				crackPoints.add(origin.add(crackSpread, 7, crackSpread));
				crackPoints.add(origin.add(crackSpread, 5, crackSpread));
				crackPoints.add(origin.add(crackSpread, 1, crackSpread));
			} else {
				crackPoints.add(origin.add(0, 7, 0));
				crackPoints.add(origin.add(0, 5, 0));
				crackPoints.add(origin.add(0, 1, 0));
			}
		}

		List<BlockPos> crystalPlacements = Lists.newArrayList();
		Predicate<BlockState> canReplace = notInBlockTagPredicate(config.layerConfig.cannotReplace);

		for (BlockPos candidate : BlockPos.iterate(origin.add(minOffset, minOffset, minOffset), origin.add(maxOffset, maxOffset, maxOffset))) {
			double noise = noiseSampler.sample(candidate.getX(), candidate.getY(), candidate.getZ()) * config.noiseMultiplier;
			double distSum = 0.0;
			double crackDistSum = 0.0;

			for (Pair<BlockPos, Integer> point : distributionPoints) {
				distSum += MathHelper.inverseSqrt(
					candidate.getSquaredDistance((Vec3i) point.getFirst()) + ((Integer) point.getSecond()).intValue()
				) + noise;
			}

			for (BlockPos crackPoint : crackPoints) {
				crackDistSum += MathHelper.inverseSqrt(
					candidate.getSquaredDistance(crackPoint) + crack.crackPointOffset
				) + noise;
			}

			if (distSum < outerThreshold) {
				continue;
			}

			if (hasCrack && crackDistSum >= crackThreshold && distSum < fillingThreshold) {
				setBlockStateIf(world, candidate, Blocks.AIR.getDefaultState(), canReplace);

				for (Direction direction : DIRECTIONS) {
					BlockPos neighbor = candidate.offset(direction);
					FluidState fluidState = world.getFluidState(neighbor);

					if (fluidState.isEmpty() == false) {
						world.scheduleFluidTick(neighbor, fluidState.getFluid(), 0);
					}
				}
			} else if (distSum >= fillingThreshold) {
				setBlockStateIf(world, candidate, layers.fillingProvider.get(random, candidate), canReplace);
			} else if (distSum >= innerThreshold) {
				boolean useAlternate = random.nextFloat() < config.useAlternateLayer0Chance;
				BlockState innerState = useAlternate
					? layers.alternateInnerLayerProvider.get(random, candidate)
					: layers.innerLayerProvider.get(random, candidate);

				setBlockStateIf(world, candidate, innerState, canReplace);

				if ((!config.placementsRequireLayer0Alternate || useAlternate)
					&& random.nextFloat() < config.usePotentialPlacementsChance
				) {
					crystalPlacements.add(candidate.toImmutable());
				}
			} else if (distSum >= middleThreshold) {
				setBlockStateIf(world, candidate, layers.middleLayerProvider.get(random, candidate), canReplace);
			} else if (distSum >= outerThreshold) {
				setBlockStateIf(world, candidate, layers.outerLayerProvider.get(random, candidate), canReplace);
			}
		}

		List<BlockState> innerBlocks = layers.innerBlocks;

		for (BlockPos placement : crystalPlacements) {
			BlockState crystal = Util.getRandom(innerBlocks, random);

			for (Direction direction : DIRECTIONS) {
				if (crystal.contains(Properties.FACING)) {
					crystal = crystal.with(Properties.FACING, direction);
				}

				BlockPos neighbor = placement.offset(direction);
				BlockState neighborState = world.getBlockState(neighbor);

				if (crystal.contains(Properties.WATERLOGGED)) {
					crystal = crystal.with(Properties.WATERLOGGED, neighborState.getFluidState().isStill());
				}

				if (BuddingAmethystBlock.canGrowIn(neighborState)) {
					setBlockStateIf(world, neighbor, crystal, canReplace);
					break;
				}
			}
		}

		return true;
	}
}
