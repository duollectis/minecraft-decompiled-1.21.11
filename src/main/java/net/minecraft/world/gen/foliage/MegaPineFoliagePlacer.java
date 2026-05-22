package net.minecraft.world.gen.foliage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

/**
 * Размещает листву мегасосны: конусообразная крона, радиус каждого слоя
 * вычисляется пропорционально расстоянию от вершины.
 */
public class MegaPineFoliagePlacer extends FoliagePlacer {

	public static final MapCodec<MegaPineFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillFoliagePlacerFields(instance)
					.and(IntProvider
							.createValidatingCodec(0, 24)
							.fieldOf("crown_height")
							.forGetter(placer -> placer.crownHeight))
					.apply(instance, MegaPineFoliagePlacer::new)
	);
	private final IntProvider crownHeight;

	public MegaPineFoliagePlacer(IntProvider radius, IntProvider offset, IntProvider crownHeight) {
		super(radius, offset);
		this.crownHeight = crownHeight;
	}

	@Override
	protected FoliagePlacerType<?> getType() {
		return FoliagePlacerType.MEGA_PINE_FOLIAGE_PLACER;
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
		int prevRadius = 0;

		for (int worldY = center.getY() - foliageHeight + offset; worldY <= center.getY() + offset; worldY++) {
			int distFromTop = center.getY() - worldY;
			int baseLayerRadius = radius + treeNode.getFoliageRadius()
					+ MathHelper.floor((float) distFromTop / foliageHeight * 3.5F);

			// Нечётные слои с тем же радиусом получают +1 для визуального разнообразия
			int layerRadius = distFromTop > 0 && baseLayerRadius == prevRadius && (worldY & 1) == 0
					? baseLayerRadius + 1
					: baseLayerRadius;

			generateSquare(
					world,
					placer,
					random,
					config,
					new BlockPos(center.getX(), worldY, center.getZ()),
					layerRadius,
					0,
					treeNode.isGiantTrunk()
			);
			prevRadius = baseLayerRadius;
		}
	}

	@Override
	public int getRandomHeight(Random random, int trunkHeight, TreeFeatureConfig config) {
		return this.crownHeight.get(random);
	}

	@Override
	protected boolean isInvalidForLeaves(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
		return dx + dz >= 7 || dx * dx + dz * dz > radius * radius;
	}
}
