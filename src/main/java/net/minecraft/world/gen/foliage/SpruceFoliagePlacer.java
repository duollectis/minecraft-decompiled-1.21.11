package net.minecraft.world.gen.foliage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

/**
 * Размещает листву ели: радиус нарастает снизу вверх слоями,
 * создавая характерную пирамидальную форму ёлки.
 */
public class SpruceFoliagePlacer extends FoliagePlacer {

	public static final MapCodec<SpruceFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillFoliagePlacerFields(instance)
					.and(IntProvider
							.createValidatingCodec(0, 24)
							.fieldOf("trunk_height")
							.forGetter(placer -> placer.trunkHeight))
					.apply(instance, SpruceFoliagePlacer::new)
	);
	private final IntProvider trunkHeight;

	public SpruceFoliagePlacer(IntProvider radius, IntProvider offset, IntProvider trunkHeight) {
		super(radius, offset);
		this.trunkHeight = trunkHeight;
	}

	@Override
	protected FoliagePlacerType<?> getType() {
		return FoliagePlacerType.SPRUCE_FOLIAGE_PLACER;
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
		int currentRadius = random.nextInt(2);
		int maxRadius = 1;
		int minRadius = 0;

		for (int dy = offset; dy >= -foliageHeight; dy--) {
			generateSquare(world, placer, random, config, center, currentRadius, dy, treeNode.isGiantTrunk());

			if (currentRadius >= maxRadius) {
				currentRadius = minRadius;
				minRadius = 1;
				maxRadius = Math.min(maxRadius + 1, radius + treeNode.getFoliageRadius());
			} else {
				currentRadius++;
			}
		}
	}

	@Override
	public int getRandomHeight(Random random, int trunkHeight, TreeFeatureConfig config) {
		return Math.max(4, trunkHeight - this.trunkHeight.get(random));
	}

	@Override
	protected boolean isInvalidForLeaves(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
		return dx == radius && dz == radius && radius > 0;
	}
}
