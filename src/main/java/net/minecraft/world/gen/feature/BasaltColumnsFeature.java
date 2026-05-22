package net.minecraft.world.gen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.jspecify.annotations.Nullable;

/** Генерирует колонны базальта в Нижнем мире, размещая плотные или редкие группы столбов вокруг точки происхождения. */
public class BasaltColumnsFeature extends Feature<BasaltColumnsFeatureConfig> {

	private static final ImmutableList<Block> CANNOT_REPLACE_BLOCKS = ImmutableList.of(
			Blocks.LAVA,
			Blocks.BEDROCK,
			Blocks.MAGMA_BLOCK,
			Blocks.SOUL_SAND,
			Blocks.NETHER_BRICKS,
			Blocks.NETHER_BRICK_FENCE,
			Blocks.NETHER_BRICK_STAIRS,
			Blocks.NETHER_WART,
			Blocks.CHEST,
			Blocks.SPAWNER
	);
	private static final int DENSE_COLUMN_RADIUS = 5;
	private static final int DENSE_COLUMN_ATTEMPTS = 50;
	private static final int SPARSE_COLUMN_RADIUS = 8;
	private static final int SPARSE_COLUMN_ATTEMPTS = 15;

	public BasaltColumnsFeature(Codec<BasaltColumnsFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<BasaltColumnsFeatureConfig> context) {
		int seaLevel = context.getGenerator().getSeaLevel();
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		BasaltColumnsFeatureConfig config = context.getConfig();

		if (!canPlaceAt(world, seaLevel, origin.mutableCopy())) {
			return false;
		}

		int height = config.getHeight().get(random);
		boolean isDense = random.nextFloat() < 0.9F;
		int radius = Math.min(height, isDense ? DENSE_COLUMN_RADIUS : SPARSE_COLUMN_RADIUS);
		int attempts = isDense ? DENSE_COLUMN_ATTEMPTS : SPARSE_COLUMN_ATTEMPTS;
		boolean placed = false;

		for (BlockPos candidate : BlockPos.iterateRandomly(
				random,
				attempts,
				origin.getX() - radius,
				origin.getY(),
				origin.getZ() - radius,
				origin.getX() + radius,
				origin.getY(),
				origin.getZ() + radius
		)) {
			int columnHeight = height - candidate.getManhattanDistance(origin);

			if (columnHeight >= 0) {
				placed |= placeBasaltColumn(world, seaLevel, candidate, columnHeight, config.getReach().get(random));
			}
		}

		return placed;
	}

	private boolean placeBasaltColumn(WorldAccess world, int seaLevel, BlockPos pos, int height, int reach) {
		boolean placed = false;

		for (BlockPos blockPos : BlockPos.iterate(
				pos.getX() - reach,
				pos.getY(),
				pos.getZ() - reach,
				pos.getX() + reach,
				pos.getY(),
				pos.getZ() + reach
		)) {
			int dist = blockPos.getManhattanDistance(pos);
			BlockPos groundPos = isAirOrLavaOcean(world, seaLevel, blockPos)
					? moveDownToGround(world, seaLevel, blockPos.mutableCopy(), dist)
					: moveUpToAir(world, blockPos.mutableCopy(), dist);

			if (groundPos == null) {
				continue;
			}

			int columnHeight = height - dist / 2;

			for (BlockPos.Mutable mutable = groundPos.mutableCopy(); columnHeight >= 0; columnHeight--) {
				if (isAirOrLavaOcean(world, seaLevel, mutable)) {
					setBlockState(world, mutable, Blocks.BASALT.getDefaultState());
					mutable.move(Direction.UP);
					placed = true;
				} else {
					if (!world.getBlockState(mutable).isOf(Blocks.BASALT)) {
						break;
					}

					mutable.move(Direction.UP);
				}
			}
		}

		return placed;
	}

	private static @Nullable BlockPos moveDownToGround(
			WorldAccess world,
			int seaLevel,
			BlockPos.Mutable mutablePos,
			int distance
	) {
		while (mutablePos.getY() > world.getBottomY() + 1 && distance > 0) {
			distance--;
			if (canPlaceAt(world, seaLevel, mutablePos)) {
				return mutablePos;
			}

			mutablePos.move(Direction.DOWN);
		}

		return null;
	}

	private static boolean canPlaceAt(WorldAccess world, int seaLevel, BlockPos.Mutable mutablePos) {
		if (!isAirOrLavaOcean(world, seaLevel, mutablePos)) {
			return false;
		}

		BlockState below = world.getBlockState(mutablePos.move(Direction.DOWN));
		mutablePos.move(Direction.UP);

		return !below.isAir() && !CANNOT_REPLACE_BLOCKS.contains(below.getBlock());
	}

	private static @Nullable BlockPos moveUpToAir(WorldAccess world, BlockPos.Mutable mutablePos, int distance) {
		while (mutablePos.getY() <= world.getTopYInclusive() && distance > 0) {
			distance--;
			BlockState blockState = world.getBlockState(mutablePos);
			if (CANNOT_REPLACE_BLOCKS.contains(blockState.getBlock())) {
				return null;
			}

			if (blockState.isAir()) {
				return mutablePos;
			}

			mutablePos.move(Direction.UP);
		}

		return null;
	}

	private static boolean isAirOrLavaOcean(WorldAccess world, int seaLevel, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		return blockState.isAir() || blockState.isOf(Blocks.LAVA) && pos.getY() <= seaLevel;
	}
}
