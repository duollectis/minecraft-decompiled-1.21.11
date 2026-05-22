package net.minecraft.world.gen.foliage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

/**
 * Размещает листву большого дуба: крайние слои имеют увеличенный радиус,
 * форма листвы определяется круговым расстоянием (Fancy Oak).
 */
public class LargeOakFoliagePlacer extends BlobFoliagePlacer {

	public static final MapCodec<LargeOakFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> createCodec(instance).apply(instance, LargeOakFoliagePlacer::new)
	);

	public LargeOakFoliagePlacer(IntProvider radius, IntProvider offset, int height) {
		super(radius, offset, height);
	}

	@Override
	protected FoliagePlacerType<?> getType() {
		return FoliagePlacerType.FANCY_FOLIAGE_PLACER;
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
		for (int dy = offset; dy >= offset - foliageHeight; dy--) {
			int layerRadius = radius + (dy != offset && dy != offset - foliageHeight ? 1 : 0);
			generateSquare(world, placer, random, config, treeNode.getCenter(), layerRadius, dy, treeNode.isGiantTrunk());
		}
	}

	@Override
	protected boolean isInvalidForLeaves(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
		return MathHelper.square(dx + 0.5F) + MathHelper.square(dz + 0.5F) > radius * radius;
	}
}
