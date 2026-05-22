package net.minecraft.world.gen.trunk;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Алгоритм размещения изогнутого ствола дерева.
 * Строит ствол, который на определённой высоте начинает отклоняться в случайном горизонтальном
 * направлении, а затем продолжает расти горизонтально на заданную длину изгиба.
 */
public class BendingTrunkPlacer extends TrunkPlacer {

	public static final MapCodec<BendingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillTrunkPlacerFields(instance)
					.and(
							instance.group(
									Codecs.POSITIVE_INT
											.optionalFieldOf("min_height_for_leaves", 1)
											.forGetter(placer -> placer.minHeightForLeaves),
									IntProvider
											.createValidatingCodec(1, 64)
											.fieldOf("bend_length")
											.forGetter(placer -> placer.bendLength)
							)
					)
					.apply(instance, BendingTrunkPlacer::new)
	);
	private final int minHeightForLeaves;
	private final IntProvider bendLength;

	public BendingTrunkPlacer(
			int baseHeight,
			int firstRandomHeight,
			int secondRandomHeight,
			int minHeightForLeaves,
			IntProvider bendLength
	) {
		super(baseHeight, firstRandomHeight, secondRandomHeight);
		this.minHeightForLeaves = minHeightForLeaves;
		this.bendLength = bendLength;
	}

	@Override
	protected TrunkPlacerType<?> getType() {
		return TrunkPlacerType.BENDING_TRUNK_PLACER;
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
		Direction bendDirection = Direction.Type.HORIZONTAL.random(random);
		int trunkTop = height - 1;
		BlockPos.Mutable mutable = startPos.mutableCopy();
		setToDirt(world, replacer, random, mutable.down(), config);
		List<FoliagePlacer.TreeNode> nodes = Lists.newArrayList();

		for (int y = 0; y <= trunkTop; y++) {
			if (y + 1 >= trunkTop + random.nextInt(2)) {
				mutable.move(bendDirection);
			}

			if (TreeFeature.canReplace(world, mutable)) {
				getAndSetState(world, replacer, random, mutable, config);
			}

			if (y >= minHeightForLeaves) {
				nodes.add(new FoliagePlacer.TreeNode(mutable.toImmutable(), 0, false));
			}

			mutable.move(Direction.UP);
		}

		int bendLen = bendLength.get(random);

		for (int step = 0; step <= bendLen; step++) {
			if (TreeFeature.canReplace(world, mutable)) {
				getAndSetState(world, replacer, random, mutable, config);
			}

			nodes.add(new FoliagePlacer.TreeNode(mutable.toImmutable(), 0, false));
			mutable.move(bendDirection);
		}

		return nodes;
	}
}
