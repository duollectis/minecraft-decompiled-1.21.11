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

/** Генерирует дельту (лужу) в Нижнем мире: заполняет область содержимым со смещённым ободком из другого блока. */
public class DeltaFeature extends Feature<DeltaFeatureConfig> {

	private static final ImmutableList<Block> CANNOT_REPLACE_BLOCKS = ImmutableList.of(
			Blocks.BEDROCK,
			Blocks.NETHER_BRICKS,
			Blocks.NETHER_BRICK_FENCE,
			Blocks.NETHER_BRICK_STAIRS,
			Blocks.NETHER_WART,
			Blocks.CHEST,
			Blocks.SPAWNER
	);
	private static final Direction[] DIRECTIONS = Direction.values();
	private static final double DELTA_SCALE = 0.9;

	public DeltaFeature(Codec<DeltaFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DeltaFeatureConfig> context) {
		boolean placed = false;
		Random random = context.getRandom();
		StructureWorldAccess world = context.getWorld();
		DeltaFeatureConfig config = context.getConfig();
		BlockPos origin = context.getOrigin();
		boolean hasRim = random.nextDouble() < DELTA_SCALE;
		int rimOffsetX = hasRim ? config.getRimSize().get(random) : 0;
		int rimOffsetZ = hasRim ? config.getRimSize().get(random) : 0;
		boolean placeRim = hasRim && rimOffsetX != 0 && rimOffsetZ != 0;
		int sizeX = config.getSize().get(random);
		int sizeZ = config.getSize().get(random);
		int maxSize = Math.max(sizeX, sizeZ);

		for (BlockPos pos : BlockPos.iterateOutwards(origin, sizeX, 0, sizeZ)) {
			if (pos.getManhattanDistance(origin) > maxSize) {
				break;
			}

			if (!canPlace(world, pos, config)) {
				continue;
			}

			if (placeRim) {
				placed = true;
				setBlockState(world, pos, config.getRim());
			}

			BlockPos contentsPos = pos.add(rimOffsetX, 0, rimOffsetZ);

			if (canPlace(world, contentsPos, config)) {
				placed = true;
				setBlockState(world, contentsPos, config.getContents());
			}
		}

		return placed;
	}

	private static boolean canPlace(WorldAccess world, BlockPos pos, DeltaFeatureConfig config) {
		BlockState state = world.getBlockState(pos);

		if (state.isOf(config.getContents().getBlock())) {
			return false;
		}

		if (CANNOT_REPLACE_BLOCKS.contains(state.getBlock())) {
			return false;
		}

		for (Direction direction : DIRECTIONS) {
			boolean neighborIsAir = world.getBlockState(pos.offset(direction)).isAir();

			if (neighborIsAir && direction != Direction.UP) {
				return false;
			}

			if (!neighborIsAir && direction == Direction.UP) {
				return false;
			}
		}

		return true;
	}
}
