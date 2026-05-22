package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует пятно голубого льда под поверхностью воды, прикреплённое к соседнему упакованному льду. */
public class BlueIceFeature extends Feature<DefaultFeatureConfig> {

	public BlueIceFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	private static final int SPREAD_ITERATIONS = 200;
	private static final int BASE_SPREAD_RADIUS = 3;

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();

		if (origin.getY() > world.getSeaLevel() - 1) {
			return false;
		}

		if (!world.getBlockState(origin).isOf(Blocks.WATER)
				&& !world.getBlockState(origin.down()).isOf(Blocks.WATER)) {
			return false;
		}

		boolean hasPackedIceNeighbor = false;

		for (Direction direction : Direction.values()) {
			if (direction != Direction.DOWN && world.getBlockState(origin.offset(direction)).isOf(Blocks.PACKED_ICE)) {
				hasPackedIceNeighbor = true;
				break;
			}
		}

		if (!hasPackedIceNeighbor) {
			return false;
		}

		world.setBlockState(origin, Blocks.BLUE_ICE.getDefaultState(), 2);

		for (int iter = 0; iter < SPREAD_ITERATIONS; iter++) {
			int dy = random.nextInt(5) - random.nextInt(6);
			int radius = BASE_SPREAD_RADIUS;

			if (dy < 2) {
				radius += dy / 2;
			}

			if (radius < 1) {
				continue;
			}

			BlockPos candidate = origin.add(
					random.nextInt(radius) - random.nextInt(radius),
					dy,
					random.nextInt(radius) - random.nextInt(radius)
			);
			BlockState candidateState = world.getBlockState(candidate);

			if (!candidateState.isAir()
					&& !candidateState.isOf(Blocks.WATER)
					&& !candidateState.isOf(Blocks.PACKED_ICE)
					&& !candidateState.isOf(Blocks.ICE)) {
				continue;
			}

			for (Direction neighbor : Direction.values()) {
				if (world.getBlockState(candidate.offset(neighbor)).isOf(Blocks.BLUE_ICE)) {
					world.setBlockState(candidate, Blocks.BLUE_ICE.getDefaultState(), 2);
					break;
				}
			}
		}

		return true;
	}
}
