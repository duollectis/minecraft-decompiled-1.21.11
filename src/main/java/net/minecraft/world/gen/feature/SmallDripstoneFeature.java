package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.DripstoneHelper;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.Optional;

/**
 * Генерирует небольшой сталактит или сталагмит из друзы (dripstone).
 * Опционально распространяет блоки друзы вокруг основания.
 */
public class SmallDripstoneFeature extends Feature<SmallDripstoneFeatureConfig> {

	public SmallDripstoneFeature(Codec<SmallDripstoneFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<SmallDripstoneFeatureConfig> context) {
		WorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();
		SmallDripstoneFeatureConfig config = context.getConfig();

		Optional<Direction> direction = getDirection(world, origin, random);

		if (direction.isEmpty()) {
			return false;
		}

		BlockPos base = origin.offset(direction.get().getOpposite());
		generateDripstoneBlocks(world, random, base, config);

		boolean canBeTall = random.nextFloat() < config.chanceOfTallerDripstone
			&& DripstoneHelper.canGenerate(world.getBlockState(origin.offset(direction.get())));
		int height = canBeTall ? 2 : 1;

		DripstoneHelper.generatePointedDripstone(world, origin, direction.get(), height, false);
		return true;
	}

	private static Optional<Direction> getDirection(WorldAccess world, BlockPos pos, Random random) {
		boolean canReplaceAbove = DripstoneHelper.canReplace(world.getBlockState(pos.up()));
		boolean canReplaceBelow = DripstoneHelper.canReplace(world.getBlockState(pos.down()));

		if (canReplaceAbove && canReplaceBelow) {
			return Optional.of(random.nextBoolean() ? Direction.DOWN : Direction.UP);
		}

		if (canReplaceAbove) {
			return Optional.of(Direction.DOWN);
		}

		return canReplaceBelow ? Optional.of(Direction.UP) : Optional.empty();
	}

	private static void generateDripstoneBlocks(
		WorldAccess world,
		Random random,
		BlockPos pos,
		SmallDripstoneFeatureConfig config
	) {
		DripstoneHelper.generateDripstoneBlock(world, pos);

		for (Direction direction : Direction.Type.HORIZONTAL) {
			if (random.nextFloat() > config.chanceOfDirectionalSpread) {
				continue;
			}

			BlockPos neighbor = pos.offset(direction);
			DripstoneHelper.generateDripstoneBlock(world, neighbor);

			if (random.nextFloat() > config.chanceOfSpreadRadius2) {
				continue;
			}

			BlockPos neighbor2 = neighbor.offset(Direction.random(random));
			DripstoneHelper.generateDripstoneBlock(world, neighbor2);

			if (random.nextFloat() > config.chanceOfSpreadRadius3) {
				continue;
			}

			BlockPos neighbor3 = neighbor2.offset(Direction.random(random));
			DripstoneHelper.generateDripstoneBlock(world, neighbor3);
		}
	}
}
