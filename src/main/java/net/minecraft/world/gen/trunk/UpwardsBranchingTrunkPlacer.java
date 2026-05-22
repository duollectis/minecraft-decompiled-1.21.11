package net.minecraft.world.gen.trunk;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Алгоритм размещения ствола с восходящими ветвями (мангровое дерево).
 * Строит прямой ствол и с заданной вероятностью на каждом уровне генерирует
 * боковую ветвь, которая растёт вверх и в сторону на несколько шагов.
 */
public class UpwardsBranchingTrunkPlacer extends TrunkPlacer {

	public static final MapCodec<UpwardsBranchingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillTrunkPlacerFields(instance)
					.and(
							instance.group(
									IntProvider.POSITIVE_CODEC
											.fieldOf("extra_branch_steps")
											.forGetter(trunkPlacer -> trunkPlacer.extraBranchSteps),
									Codec
											.floatRange(0.0F, 1.0F)
											.fieldOf("place_branch_per_log_probability")
											.forGetter(trunkPlacer -> trunkPlacer.placeBranchPerLogProbability),
									IntProvider.NON_NEGATIVE_CODEC
											.fieldOf("extra_branch_length")
											.forGetter(trunkPlacer -> trunkPlacer.extraBranchLength),
									RegistryCodecs
											.entryList(RegistryKeys.BLOCK)
											.fieldOf("can_grow_through")
											.forGetter(trunkPlacer -> trunkPlacer.canGrowThrough)
							)
					)
					.apply(instance, UpwardsBranchingTrunkPlacer::new)
	);
	private final IntProvider extraBranchSteps;
	private final float placeBranchPerLogProbability;
	private final IntProvider extraBranchLength;
	private final RegistryEntryList<Block> canGrowThrough;

	public UpwardsBranchingTrunkPlacer(
			int baseHeight,
			int firstRandomHeight,
			int secondRandomHeight,
			IntProvider extraBranchSteps,
			float placeBranchPerLogProbability,
			IntProvider extraBranchLength,
			RegistryEntryList<Block> canGrowThrough
	) {
		super(baseHeight, firstRandomHeight, secondRandomHeight);
		this.extraBranchSteps = extraBranchSteps;
		this.placeBranchPerLogProbability = placeBranchPerLogProbability;
		this.extraBranchLength = extraBranchLength;
		this.canGrowThrough = canGrowThrough;
	}

	@Override
	protected TrunkPlacerType<?> getType() {
		return TrunkPlacerType.UPWARDS_BRANCHING_TRUNK_PLACER;
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
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int y = 0; y < height; y++) {
			int worldY = startPos.getY() + y;

			if (getAndSetState(world, replacer, random, mutable.set(startPos.getX(), worldY, startPos.getZ()), config)
					&& y < height - 1
					&& random.nextFloat() < placeBranchPerLogProbability
			) {
				Direction branchDir = Direction.Type.HORIZONTAL.random(random);
				int branchLen = extraBranchLength.get(random);
				int startOffset = Math.max(0, branchLen - extraBranchLength.get(random) - 1);
				int branchSteps = extraBranchSteps.get(random);
				generateExtraBranch(world, replacer, random, height, config, nodes, mutable, worldY, branchDir, startOffset, branchSteps);
			}

			if (y == height - 1) {
				nodes.add(new FoliagePlacer.TreeNode(mutable.set(startPos.getX(), worldY + 1, startPos.getZ()), 0, false));
			}
		}

		return nodes;
	}

	private void generateExtraBranch(
			TestableWorld world,
			BiConsumer<BlockPos, BlockState> replacer,
			Random random,
			int height,
			TreeFeatureConfig config,
			List<FoliagePlacer.TreeNode> nodes,
			BlockPos.Mutable pos,
			int baseY,
			Direction direction,
			int startOffset,
			int steps
	) {
		int topY = baseY + startOffset;
		int currentX = pos.getX();
		int currentZ = pos.getZ();
		int offset = startOffset;

		while (offset < height && steps > 0) {
			if (offset >= 1) {
				int worldY = baseY + offset;
				currentX += direction.getOffsetX();
				currentZ += direction.getOffsetZ();
				topY = worldY;

				if (getAndSetState(world, replacer, random, pos.set(currentX, worldY, currentZ), config)) {
					topY = worldY + 1;
				}

				nodes.add(new FoliagePlacer.TreeNode(pos.toImmutable(), 0, false));
			}

			offset++;
			steps--;
		}

		if (topY - baseY > 1) {
			BlockPos branchTop = new BlockPos(currentX, topY, currentZ);
			nodes.add(new FoliagePlacer.TreeNode(branchTop, 0, false));
			nodes.add(new FoliagePlacer.TreeNode(branchTop.down(2), 0, false));
		}
	}

	@Override
	protected boolean canReplace(TestableWorld world, BlockPos pos) {
		return super.canReplace(world, pos) || world.testBlockState(pos, state -> state.isIn(canGrowThrough));
	}
}
