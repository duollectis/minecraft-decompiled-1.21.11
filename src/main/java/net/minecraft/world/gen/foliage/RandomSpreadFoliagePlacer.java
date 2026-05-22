package net.minecraft.world.gen.foliage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

/**
 * Размещает листву случайным разбросом: каждый лист помещается
 * в случайную позицию в пределах заданного радиуса и высоты.
 */
public class RandomSpreadFoliagePlacer extends FoliagePlacer {

	public static final MapCodec<RandomSpreadFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillFoliagePlacerFields(instance)
					.and(
							instance.group(
									IntProvider
											.createValidatingCodec(1, 512)
											.fieldOf("foliage_height")
											.forGetter(placer -> placer.foliageHeight),
									Codec
											.intRange(0, 256)
											.fieldOf("leaf_placement_attempts")
											.forGetter(placer -> placer.leafPlacementAttempts)
							)
					)
					.apply(instance, RandomSpreadFoliagePlacer::new)
	);
	private final IntProvider foliageHeight;
	private final int leafPlacementAttempts;

	public RandomSpreadFoliagePlacer(
			IntProvider radius,
			IntProvider offset,
			IntProvider foliageHeight,
			int leafPlacementAttempts
	) {
		super(radius, offset);
		this.foliageHeight = foliageHeight;
		this.leafPlacementAttempts = leafPlacementAttempts;
	}

	@Override
	protected FoliagePlacerType<?> getType() {
		return FoliagePlacerType.RANDOM_SPREAD_FOLIAGE_PLACER;
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
		BlockPos center = treeNode.getCenter();
		BlockPos.Mutable mutable = center.mutableCopy();

		for (int attempt = 0; attempt < this.leafPlacementAttempts; attempt++) {
			mutable.set(
					center,
					random.nextInt(radius) - random.nextInt(radius),
					random.nextInt(foliageHeight) - random.nextInt(foliageHeight),
					random.nextInt(radius) - random.nextInt(radius)
			);
			placeFoliageBlock(world, placer, random, config, mutable);
		}
	}

	@Override
	public int getRandomHeight(Random random, int trunkHeight, TreeFeatureConfig config) {
		return this.foliageHeight.get(random);
	}

	@Override
	protected boolean isInvalidForLeaves(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
		return false;
	}
}
