package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.List;

/**
 * Генерирует многогранный рост (например, светящийся лишайник или мох)
 * на поверхностях блоков. Сначала пробует разместить в начальной точке,
 * затем ищет подходящую позицию в радиусе {@code searchRange} по каждому направлению.
 */
public class MultifaceGrowthFeature extends Feature<MultifaceGrowthFeatureConfig> {

	public MultifaceGrowthFeature(Codec<MultifaceGrowthFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<MultifaceGrowthFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();
		MultifaceGrowthFeatureConfig config = context.getConfig();

		if (!isAirOrWater(world.getBlockState(origin))) {
			return false;
		}

		List<Direction> directions = config.shuffleDirections(random);

		if (generate(world, origin, world.getBlockState(origin), config, random, directions)) {
			return true;
		}

		BlockPos.Mutable mutable = origin.mutableCopy();

		for (Direction direction : directions) {
			mutable.set(origin);
			List<Direction> searchDirs = config.shuffleDirections(random, direction.getOpposite());

			for (int step = 0; step < config.searchRange; step++) {
				mutable.set(origin, direction);
				BlockState state = world.getBlockState(mutable);

				if (!isAirOrWater(state) && !state.isOf(config.block)) {
					break;
				}

				if (generate(world, mutable, state, config, random, searchDirs)) {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean generate(
		StructureWorldAccess world,
		BlockPos pos,
		BlockState state,
		MultifaceGrowthFeatureConfig config,
		Random random,
		List<Direction> directions
	) {
		BlockPos.Mutable mutable = pos.mutableCopy();

		for (Direction direction : directions) {
			BlockState neighbor = world.getBlockState(mutable.set(pos, direction));

			if (!neighbor.isIn(config.canPlaceOn)) {
				continue;
			}

			BlockState placed = config.block.withDirection(state, world, pos, direction);

			if (placed == null) {
				return false;
			}

			world.setBlockState(pos, placed, 3);
			world.getChunk(pos).markBlockForPostProcessing(pos);

			if (random.nextFloat() < config.spreadChance) {
				config.block.getGrower().grow(placed, world, pos, direction, random, true);
			}

			return true;
		}

		return false;
	}

	private static boolean isAirOrWater(BlockState state) {
		return state.isAir() || state.isOf(Blocks.WATER);
	}
}
