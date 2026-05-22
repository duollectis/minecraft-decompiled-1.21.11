package net.minecraft.world.gen.trunk;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Алгоритм размещения ствола гигантского джунглевого дерева.
 * Расширяет {@link GiantTrunkPlacer}, добавляя горизонтальные ветви-выступы,
 * которые спирально расходятся от ствола на разных высотах.
 */
public class MegaJungleTrunkPlacer extends GiantTrunkPlacer {

	public static final MapCodec<MegaJungleTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillTrunkPlacerFields(instance).apply(instance, MegaJungleTrunkPlacer::new)
	);

	public MegaJungleTrunkPlacer(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
		super(baseHeight, firstRandomHeight, secondRandomHeight);
	}

	@Override
	protected TrunkPlacerType<?> getType() {
		return TrunkPlacerType.MEGA_JUNGLE_TRUNK_PLACER;
	}

	@Override
	public List<FoliagePlacer.TreeNode> generate(
			TestableWorld world,
			BiConsumer<BlockPos, BlockState> replacer,
			Random random,
			int height,
			BlockPos startPos,
			TreeFeatureConfig config
	) {
		List<FoliagePlacer.TreeNode> nodes = Lists.newArrayList();
		nodes.addAll(super.generate(world, replacer, random, height, startPos, config));

		for (int branchY = height - 2 - random.nextInt(4); branchY > height / 2; branchY -= 2 + random.nextInt(4)) {
			float angle = random.nextFloat() * (float) (Math.PI * 2);
			int endX = 0;
			int endZ = 0;

			for (int step = 0; step < 5; step++) {
				endX = (int) (1.5F + MathHelper.cos(angle) * step);
				endZ = (int) (1.5F + MathHelper.sin(angle) * step);
				getAndSetState(world, replacer, random, startPos.add(endX, branchY - 3 + step / 2, endZ), config);
			}

			nodes.add(new FoliagePlacer.TreeNode(startPos.add(endX, branchY, endZ), -2, false));
		}

		return nodes;
	}
}
