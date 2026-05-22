package net.minecraft.world.gen.trunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Алгоритм размещения ствола вишнёвого дерева.
 * Строит прямой ствол с 1–3 горизонтальными ветвями, расходящимися в противоположных направлениях.
 * Каждая ветвь начинается на случайной высоте ниже вершины и изгибается к конечной точке,
 * чередуя горизонтальные и вертикальные шаги пропорционально разнице высот.
 */
public class CherryTrunkPlacer extends TrunkPlacer {

	private static final Codec<UniformIntProvider> BRANCH_START_OFFSET_FROM_TOP_CODEC = UniformIntProvider.CODEC
			.codec()
			.validate(
					branchStartOffsetFromTop ->
							branchStartOffsetFromTop.getMax() - branchStartOffsetFromTop.getMin() < 1
							? DataResult.error(() -> "Need at least 2 blocks variation for the branch starts to fit both branches")
							: DataResult.success(branchStartOffsetFromTop)
			);
	public static final MapCodec<CherryTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillTrunkPlacerFields(instance)
					.and(
							instance.group(
									IntProvider
											.createValidatingCodec(1, 3)
											.fieldOf("branch_count")
											.forGetter(trunkPlacer -> trunkPlacer.branchCount),
									IntProvider
											.createValidatingCodec(2, 16)
											.fieldOf("branch_horizontal_length")
											.forGetter(trunkPlacer -> trunkPlacer.branchHorizontalLength),
									IntProvider.createValidatingCodec(-16, 0, BRANCH_START_OFFSET_FROM_TOP_CODEC)
									           .fieldOf("branch_start_offset_from_top")
									           .forGetter(trunkPlacer -> trunkPlacer.branchStartOffsetFromTop),
									IntProvider
											.createValidatingCodec(-16, 16)
											.fieldOf("branch_end_offset_from_top")
											.forGetter(trunkPlacer -> trunkPlacer.branchEndOffsetFromTop)
							)
					)
					.apply(instance, CherryTrunkPlacer::new)
	);
	private final IntProvider branchCount;
	private final IntProvider branchHorizontalLength;
	private final UniformIntProvider branchStartOffsetFromTop;
	private final UniformIntProvider secondBranchStartOffsetFromTop;
	private final IntProvider branchEndOffsetFromTop;

	public CherryTrunkPlacer(
			int baseHeight,
			int firstRandomHeight,
			int secondRandomHeight,
			IntProvider branchCount,
			IntProvider branchHorizontalLength,
			UniformIntProvider branchStartOffsetFromTop,
			IntProvider branchEndOffsetFromTop
	) {
		super(baseHeight, firstRandomHeight, secondRandomHeight);
		this.branchCount = branchCount;
		this.branchHorizontalLength = branchHorizontalLength;
		this.branchStartOffsetFromTop = branchStartOffsetFromTop;
		this.secondBranchStartOffsetFromTop =
				UniformIntProvider.create(branchStartOffsetFromTop.getMin(), branchStartOffsetFromTop.getMax() - 1);
		this.branchEndOffsetFromTop = branchEndOffsetFromTop;
	}

	@Override
	protected TrunkPlacerType<?> getType() {
		return TrunkPlacerType.CHERRY_TRUNK_PLACER;
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
		setToDirt(world, replacer, random, startPos.down(), config);
		int firstBranchY = Math.max(0, height - 1 + branchStartOffsetFromTop.get(random));
		int secondBranchY = Math.max(0, height - 1 + secondBranchStartOffsetFromTop.get(random));

		if (secondBranchY >= firstBranchY) {
			secondBranchY++;
		}

		int branchCountValue = branchCount.get(random);
		boolean hasThreeBranches = branchCountValue == 3;
		boolean hasTwoBranches = branchCountValue >= 2;
		int trunkHeight = hasThreeBranches
				? height
				: hasTwoBranches
						? Math.max(firstBranchY, secondBranchY) + 1
						: firstBranchY + 1;

		for (int y = 0; y < trunkHeight; y++) {
			getAndSetState(world, replacer, random, startPos.up(y), config);
		}

		List<FoliagePlacer.TreeNode> nodes = new ArrayList<>();

		if (hasThreeBranches) {
			nodes.add(new FoliagePlacer.TreeNode(startPos.up(trunkHeight), 0, false));
		}

		BlockPos.Mutable mutable = new BlockPos.Mutable();
		Direction direction = Direction.Type.HORIZONTAL.random(random);
		Function<BlockState, BlockState> withAxis = state -> state.withIfExists(PillarBlock.AXIS, direction.getAxis());

		nodes.add(generateBranch(
				world, replacer, random, height, startPos, config,
				withAxis, direction, firstBranchY, firstBranchY < trunkHeight - 1, mutable
		));

		if (hasTwoBranches) {
			nodes.add(generateBranch(
					world, replacer, random, height, startPos, config,
					withAxis, direction.getOpposite(), secondBranchY, secondBranchY < trunkHeight - 1, mutable
			));
		}

		return nodes;
	}

	private FoliagePlacer.TreeNode generateBranch(
			TestableWorld world,
			BiConsumer<BlockPos, BlockState> replacer,
			Random random,
			int height,
			BlockPos startPos,
			TreeFeatureConfig config,
			Function<BlockState, BlockState> withAxisFunction,
			Direction direction,
			int branchStartOffset,
			boolean branchBelowHeight,
			BlockPos.Mutable mutablePos
	) {
		mutablePos.set(startPos).move(Direction.UP, branchStartOffset);
		int endOffsetY = height - 1 + branchEndOffsetFromTop.get(random);
		boolean needsExtraStep = branchBelowHeight || endOffsetY < branchStartOffset;
		int horizontalLen = branchHorizontalLength.get(random) + (needsExtraStep ? 1 : 0);
		BlockPos branchEnd = startPos.offset(direction, horizontalLen).up(endOffsetY);
		int initialSteps = needsExtraStep ? 2 : 1;

		for (int step = 0; step < initialSteps; step++) {
			getAndSetState(world, replacer, random, mutablePos.move(direction), config, withAxisFunction);
		}

		Direction verticalDir = branchEnd.getY() > mutablePos.getY() ? Direction.UP : Direction.DOWN;

		while (true) {
			int distance = mutablePos.getManhattanDistance(branchEnd);

			if (distance == 0) {
				return new FoliagePlacer.TreeNode(branchEnd.up(), 0, false);
			}

			float verticalBias = (float) Math.abs(branchEnd.getY() - mutablePos.getY()) / distance;
			boolean moveVertical = random.nextFloat() < verticalBias;
			mutablePos.move(moveVertical ? verticalDir : direction);
			getAndSetState(
					world, replacer, random, mutablePos, config,
					moveVertical ? Function.identity() : withAxisFunction
			);
		}
	}
}
