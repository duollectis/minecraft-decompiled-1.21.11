package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Генерирует патч растительности: сначала заменяет грунт под поверхностью
 * на заданный блок, затем размещает растительную фичу поверх каждой позиции.
 */
public class VegetationPatchFeature extends Feature<VegetationPatchFeatureConfig> {

	public VegetationPatchFeature(Codec<VegetationPatchFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<VegetationPatchFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		VegetationPatchFeatureConfig config = context.getConfig();
		Random random = context.getRandom();
		BlockPos origin = context.getOrigin();
		Predicate<BlockState> replaceable = state -> state.isIn(config.replaceable);

		int radiusX = config.horizontalRadius.get(random) + 1;
		int radiusZ = config.horizontalRadius.get(random) + 1;

		Set<BlockPos> groundPositions = placeGroundAndGetPositions(world, config, random, origin, replaceable, radiusX, radiusZ);
		generateVegetation(context, world, config, random, groundPositions, radiusX, radiusZ);
		return !groundPositions.isEmpty();
	}

	/**
	 * Проходит по сетке позиций в радиусе, находит поверхность вдоль заданного направления
	 * и заменяет грунт на нужную глубину. Возвращает множество позиций, где грунт был успешно заменён.
	 */
	protected Set<BlockPos> placeGroundAndGetPositions(
		StructureWorldAccess world,
		VegetationPatchFeatureConfig config,
		Random random,
		BlockPos pos,
		Predicate<BlockState> replaceable,
		int radiusX,
		int radiusZ
	) {
		BlockPos.Mutable mutable = pos.mutableCopy();
		BlockPos.Mutable surfaceCheck = mutable.mutableCopy();
		Direction surfaceDir = config.surface.getDirection();
		Direction intoSurface = surfaceDir.getOpposite();
		Set<BlockPos> result = new HashSet<>();

		for (int dx = -radiusX; dx <= radiusX; dx++) {
			boolean onXEdge = dx == -radiusX || dx == radiusX;

			for (int dz = -radiusZ; dz <= radiusZ; dz++) {
				boolean onZEdge = dz == -radiusZ || dz == radiusZ;
				boolean isCorner = onXEdge && onZEdge;
				boolean isEdge = (onXEdge || onZEdge) && !isCorner;

				if (isCorner) {
					continue;
				}

				if (isEdge && (config.extraEdgeColumnChance == 0.0F || random.nextFloat() > config.extraEdgeColumnChance)) {
					continue;
				}

				mutable.set(pos, dx, 0, dz);

				// Движемся вдоль поверхности, пока не выйдем из воздуха
				for (int step = 0; world.testBlockState(mutable, AbstractBlock.AbstractBlockState::isAir) && step < config.verticalRange; step++) {
					mutable.move(surfaceDir);
				}

				// Движемся обратно, пока не выйдем из твёрдого блока
				for (int step = 0; world.testBlockState(mutable, state -> !state.isAir()) && step < config.verticalRange; step++) {
					mutable.move(intoSurface);
				}

				surfaceCheck.set(mutable, surfaceDir);
				BlockState surfaceState = world.getBlockState(surfaceCheck);

				if (!world.isAir(mutable) || !surfaceState.isSideSolidFullSquare(world, surfaceCheck, intoSurface)) {
					continue;
				}

				int depth = config.depth.get(random)
					+ (config.extraBottomBlockChance > 0.0F && random.nextFloat() < config.extraBottomBlockChance ? 1 : 0);
				BlockPos groundPos = surfaceCheck.toImmutable();

				if (placeGround(world, config, replaceable, random, surfaceCheck, depth)) {
					result.add(groundPos);
				}
			}
		}

		return result;
	}

	protected void generateVegetation(
		FeatureContext<VegetationPatchFeatureConfig> context,
		StructureWorldAccess world,
		VegetationPatchFeatureConfig config,
		Random random,
		Set<BlockPos> positions,
		int radiusX,
		int radiusZ
	) {
		for (BlockPos pos : positions) {
			if (config.vegetationChance > 0.0F && random.nextFloat() < config.vegetationChance) {
				generateVegetationFeature(world, config, context.getGenerator(), random, pos);
			}
		}
	}

	protected boolean generateVegetationFeature(
		StructureWorldAccess world,
		VegetationPatchFeatureConfig config,
		ChunkGenerator generator,
		Random random,
		BlockPos pos
	) {
		return config.vegetationFeature
			.value()
			.generateUnregistered(world, generator, random, pos.offset(config.surface.getDirection().getOpposite()));
	}

	protected boolean placeGround(
		StructureWorldAccess world,
		VegetationPatchFeatureConfig config,
		Predicate<BlockState> replaceable,
		Random random,
		BlockPos.Mutable pos,
		int depth
	) {
		for (int layer = 0; layer < depth; layer++) {
			BlockState groundState = config.groundState.get(random, pos);
			BlockState existing = world.getBlockState(pos);

			if (groundState.isOf(existing.getBlock())) {
				continue;
			}

			if (!replaceable.test(existing)) {
				return layer != 0;
			}

			world.setBlockState(pos, groundState, 2);
			pos.move(config.surface.getDirection());
		}

		return true;
	}
}
