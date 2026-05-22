package net.minecraft.world.gen.foliage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

/**
 * Размещает листву тёмного дуба: широкая многоуровневая крона,
 * для гигантских стволов добавляется дополнительный верхний слой.
 */
public class DarkOakFoliagePlacer extends FoliagePlacer {

	public static final MapCodec<DarkOakFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillFoliagePlacerFields(instance).apply(instance, DarkOakFoliagePlacer::new)
	);

	public DarkOakFoliagePlacer(IntProvider radius, IntProvider offset) {
		super(radius, offset);
	}

	@Override
	protected FoliagePlacerType<?> getType() {
		return FoliagePlacerType.DARK_OAK_FOLIAGE_PLACER;
	}

	@Override
	protected void generate(
			TestableWorld world,
			FoliagePlacer.BlockPlacer placer,
			Random random,
			TreeFeatureConfig config,
			int trunkHeight,
			FoliagePlacer.TreeNode treeNode,
			int foliageHeight,
			int radius,
			int offset
	) {
		BlockPos center = treeNode.getCenter().up(offset);
		boolean isGiantTrunk = treeNode.isGiantTrunk();

		if (isGiantTrunk) {
			generateSquare(world, placer, random, config, center, radius + 2, -1, isGiantTrunk);
			generateSquare(world, placer, random, config, center, radius + 3, 0, isGiantTrunk);
			generateSquare(world, placer, random, config, center, radius + 2, 1, isGiantTrunk);

			if (random.nextBoolean()) {
				generateSquare(world, placer, random, config, center, radius, 2, isGiantTrunk);
			}
		} else {
			generateSquare(world, placer, random, config, center, radius + 2, -1, isGiantTrunk);
			generateSquare(world, placer, random, config, center, radius + 1, 0, isGiantTrunk);
		}
	}

	@Override
	public int getRandomHeight(Random random, int trunkHeight, TreeFeatureConfig config) {
		return 4;
	}

	@Override
	protected boolean isPositionInvalid(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
		boolean isOuterCornerAtTop = y == 0
				&& giantTrunk
				&& (dx == -radius || dx >= radius)
				&& (dz == -radius || dz >= radius);

		return isOuterCornerAtTop || super.isPositionInvalid(random, dx, y, dz, radius, giantTrunk);
	}

	@Override
	protected boolean isInvalidForLeaves(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
		if (y == -1 && !giantTrunk) {
			return dx == radius && dz == radius;
		}

		return y == 1 && dx + dz > radius * 2 - 2;
	}
}
