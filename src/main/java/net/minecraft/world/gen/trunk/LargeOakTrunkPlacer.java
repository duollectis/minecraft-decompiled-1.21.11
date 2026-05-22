package net.minecraft.world.gen.trunk;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Алгоритм размещения ствола большого дуба (fancy oak).
 * Использует алгоритм на основе золотого сечения для генерации органично выглядящих
 * ветвей, расходящихся от ствола под случайными углами. Каждая ветвь проверяется
 * на проходимость перед размещением, а её наклон определяется константой {@code BRANCH_SLOPE_FACTOR}.
 */
public class LargeOakTrunkPlacer extends TrunkPlacer {

	public static final MapCodec<LargeOakTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillTrunkPlacerFields(instance).apply(instance, LargeOakTrunkPlacer::new)
	);
	private static final double GOLDEN_RATIO = 0.618;
	private static final double BRANCH_COUNT_FACTOR = 1.382;
	private static final double BRANCH_SLOPE_FACTOR = 0.381;
	private static final double BRANCH_SPREAD_FACTOR = 0.328;

	public LargeOakTrunkPlacer(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
		super(baseHeight, firstRandomHeight, secondRandomHeight);
	}

	@Override
	protected TrunkPlacerType<?> getType() {
		return TrunkPlacerType.FANCY_TRUNK_PLACER;
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
		int totalHeight = height + 2;
		int trunkTopY = MathHelper.floor(totalHeight * GOLDEN_RATIO);
		setToDirt(world, replacer, random, startPos.down(), config);
		int branchesPerLevel = Math.min(1, MathHelper.floor(BRANCH_COUNT_FACTOR + Math.pow(1.0 * totalHeight / 13.0, 2.0)));
		int crownY = startPos.getY() + trunkTopY;
		int startLevel = totalHeight - 5;
		List<LargeOakTrunkPlacer.BranchPosition> branchPositions = Lists.newArrayList();
		branchPositions.add(new LargeOakTrunkPlacer.BranchPosition(startPos.up(startLevel), crownY));

		for (; startLevel >= 0; startLevel--) {
			float radius = shouldGenerateBranch(totalHeight, startLevel);

			if (radius < 0.0F) {
				continue;
			}

			for (int branch = 0; branch < branchesPerLevel; branch++) {
				double spread = 1.0 * radius * (random.nextFloat() + BRANCH_SPREAD_FACTOR);
				double angle = random.nextFloat() * 2.0F * Math.PI;
				double offsetX = spread * Math.sin(angle) + 0.5;
				double offsetZ = spread * Math.cos(angle) + 0.5;
				BlockPos branchTip = startPos.add(MathHelper.floor(offsetX), startLevel - 1, MathHelper.floor(offsetZ));
				BlockPos checkEnd = branchTip.up(5);

				if (!makeOrCheckBranch(world, replacer, random, branchTip, checkEnd, false, config)) {
					continue;
				}

				int dx = startPos.getX() - branchTip.getX();
				int dz = startPos.getZ() - branchTip.getZ();
				double slopedY = branchTip.getY() - Math.sqrt(dx * dx + dz * dz) * BRANCH_SLOPE_FACTOR;
				int attachY = slopedY > crownY ? crownY : (int) slopedY;
				BlockPos attachPos = new BlockPos(startPos.getX(), attachY, startPos.getZ());

				if (makeOrCheckBranch(world, replacer, random, attachPos, branchTip, false, config)) {
					branchPositions.add(new LargeOakTrunkPlacer.BranchPosition(branchTip, attachPos.getY()));
				}
			}
		}

		makeOrCheckBranch(world, replacer, random, startPos, startPos.up(trunkTopY), true, config);
		makeBranches(world, replacer, random, totalHeight, startPos, branchPositions, config);
		List<FoliagePlacer.TreeNode> foliageNodes = Lists.newArrayList();

		for (LargeOakTrunkPlacer.BranchPosition branchPos : branchPositions) {
			if (isHighEnough(totalHeight, branchPos.getEndY() - startPos.getY())) {
				foliageNodes.add(branchPos.node);
			}
		}

		return foliageNodes;
	}

	private boolean makeOrCheckBranch(
			TestableWorld world,
			BiConsumer<BlockPos, BlockState> replacer,
			Random random,
			BlockPos startPos,
			BlockPos branchPos,
			boolean make,
			TreeFeatureConfig config
	) {
		if (!make && Objects.equals(startPos, branchPos)) {
			return true;
		}

		BlockPos delta = branchPos.add(-startPos.getX(), -startPos.getY(), -startPos.getZ());
		int longest = getLongestSide(delta);
		float stepX = (float) delta.getX() / longest;
		float stepY = (float) delta.getY() / longest;
		float stepZ = (float) delta.getZ() / longest;

		for (int step = 0; step <= longest; step++) {
			BlockPos current = startPos.add(
					MathHelper.floor(0.5F + step * stepX),
					MathHelper.floor(0.5F + step * stepY),
					MathHelper.floor(0.5F + step * stepZ)
			);

			if (make) {
				getAndSetState(
						world, replacer, random, current, config,
						state -> state.withIfExists(PillarBlock.AXIS, getLogAxis(startPos, current))
				);
			} else if (!canReplaceOrIsLog(world, current)) {
				return false;
			}
		}

		return true;
	}

	private int getLongestSide(BlockPos offset) {
		return Math.max(MathHelper.abs(offset.getX()), Math.max(MathHelper.abs(offset.getY()), MathHelper.abs(offset.getZ())));
	}

	private Direction.Axis getLogAxis(BlockPos branchStart, BlockPos branchEnd) {
		int absX = Math.abs(branchEnd.getX() - branchStart.getX());
		int absZ = Math.abs(branchEnd.getZ() - branchStart.getZ());
		int maxHorizontal = Math.max(absX, absZ);

		if (maxHorizontal == 0) {
			return Direction.Axis.Y;
		}

		return absX == maxHorizontal ? Direction.Axis.X : Direction.Axis.Z;
	}

	private boolean isHighEnough(int treeHeight, int height) {
		return height >= treeHeight * 0.2;
	}

	private void makeBranches(
			TestableWorld world,
			BiConsumer<BlockPos, BlockState> replacer,
			Random random,
			int treeHeight,
			BlockPos startPos,
			List<LargeOakTrunkPlacer.BranchPosition> branchPositions,
			TreeFeatureConfig config
	) {
		for (LargeOakTrunkPlacer.BranchPosition branchPos : branchPositions) {
			int attachY = branchPos.getEndY();
			BlockPos attachPos = new BlockPos(startPos.getX(), attachY, startPos.getZ());

			if (!attachPos.equals(branchPos.node.getCenter()) && isHighEnough(treeHeight, attachY - startPos.getY())) {
				makeOrCheckBranch(world, replacer, random, attachPos, branchPos.node.getCenter(), true, config);
			}
		}
	}

	private static float shouldGenerateBranch(int treeHeight, int height) {
		if (height < treeHeight * 0.3F) {
			return -1.0F;
		}

		float halfHeight = treeHeight / 2.0F;
		float distFromCenter = halfHeight - height;
		float circleRadius = MathHelper.sqrt(halfHeight * halfHeight - distFromCenter * distFromCenter);

		if (distFromCenter == 0.0F) {
			circleRadius = halfHeight;
		} else if (Math.abs(distFromCenter) >= halfHeight) {
			return 0.0F;
		}

		return circleRadius * 0.5F;
	}

	static class BranchPosition {

		final FoliagePlacer.TreeNode node;
		private final int endY;

		public BranchPosition(BlockPos pos, int endY) {
			this.node = new FoliagePlacer.TreeNode(pos, 0, false);
			this.endY = endY;
		}

		public int getEndY() {
			return endY;
		}
	}
}
