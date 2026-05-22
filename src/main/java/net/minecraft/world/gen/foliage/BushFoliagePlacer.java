package net.minecraft.world.gen.foliage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

/**
 * Размещает листву куста: радиус каждого слоя уменьшается линейно
 * по мере подъёма, создавая округлую форму куста.
 */
public class BushFoliagePlacer extends BlobFoliagePlacer {

	public static final MapCodec<BushFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> createCodec(instance).apply(instance, BushFoliagePlacer::new)
	);

	public BushFoliagePlacer(IntProvider radius, IntProvider offset, int height) {
		super(radius, offset, height);
	}

	@Override
	protected FoliagePlacerType<?> getType() {
		return FoliagePlacerType.BUSH_FOLIAGE_PLACER;
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
			int layerRadius = radius + treeNode.getFoliageRadius() - 1 - dy;
			generateSquare(world, placer, random, config, treeNode.getCenter(), layerRadius, dy, treeNode.isGiantTrunk());
		}
	}

	@Override
	protected boolean isInvalidForLeaves(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
		return dx == radius && dz == radius && random.nextInt(2) == 0;
	}
}
