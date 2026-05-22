package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.treedecorator.TreeDecorator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Генерирует упавшее дерево: пень на месте происхождения и горизонтальный ствол в случайном направлении. */
public class FallenTreeFeature extends Feature<FallenTreeFeatureConfig> {

	private static final int MIN_LOG_OFFSET = 1;
	private static final int LOG_PLACEMENT_OFFSET = 2;
	private static final int MAX_GROUND_SEARCH_STEPS = 5;
	private static final int MAX_UNSUPPORTED_LOG_BLOCKS = 2;
	private static final int STUMP_OFFSET = 2;

	public FallenTreeFeature(Codec<FallenTreeFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<FallenTreeFeatureConfig> context) {
		generate(context.getConfig(), context.getOrigin(), context.getWorld(), context.getRandom());
		return true;
	}

	private void generate(FallenTreeFeatureConfig config, BlockPos pos, StructureWorldAccess world, Random random) {
		generateStump(config, world, random, pos.mutableCopy());
		Direction direction = Direction.Type.HORIZONTAL.random(random);
		int logLength = config.logLength.get(random) - LOG_PLACEMENT_OFFSET;
		BlockPos.Mutable logStart = pos.offset(direction, LOG_PLACEMENT_OFFSET + random.nextInt(MIN_LOG_OFFSET)).mutableCopy();
		moveToGroundPos(world, logStart);

		if (canPlaceLog(world, logLength, logStart, direction)) {
			generateLog(config, world, random, logLength, logStart, direction);
		}
	}

	private void moveToGroundPos(StructureWorldAccess world, BlockPos.Mutable pos) {
		pos.move(Direction.UP, 1);

		for (int step = 0; step < MAX_GROUND_SEARCH_STEPS + 1; step++) {
			if (canReplaceAndHasSolidBelow(world, pos)) {
				return;
			}

			pos.move(Direction.DOWN);
		}
	}

	private void generateStump(
			FallenTreeFeatureConfig config,
			StructureWorldAccess world,
			Random random,
			BlockPos.Mutable pos
	) {
		BlockPos stumpPos = setBlockStateAndGetPos(config, world, random, pos, Function.identity());
		applyDecorators(world, random, Set.of(stumpPos), config.stumpDecorators);
	}

	private boolean canPlaceLog(StructureWorldAccess world, int length, BlockPos.Mutable pos, Direction direction) {
		int unsupportedCount = 0;

		for (int step = 0; step < length; step++) {
			if (!TreeFeature.canReplace(world, pos)) {
				return false;
			}

			if (!isSolidBelow(world, pos)) {
				if (++unsupportedCount > MAX_UNSUPPORTED_LOG_BLOCKS) {
					return false;
				}
			} else {
				unsupportedCount = 0;
			}

			pos.move(direction);
		}

		pos.move(direction.getOpposite(), length);
		return true;
	}

	private void generateLog(
			FallenTreeFeatureConfig config,
			StructureWorldAccess world,
			Random random,
			int length,
			BlockPos.Mutable pos,
			Direction direction
	) {
		Set<BlockPos> logPositions = new HashSet<>();

		for (int step = 0; step < length; step++) {
			logPositions.add(setBlockStateAndGetPos(config, world, random, pos, createAxisApplier(direction)));
			pos.move(direction);
		}

		applyDecorators(world, random, logPositions, config.logDecorators);
	}

	private boolean canReplaceAndHasSolidBelow(WorldAccess world, BlockPos pos) {
		return TreeFeature.canReplace(world, pos) && isSolidBelow(world, pos);
	}

	private boolean isSolidBelow(WorldAccess world, BlockPos pos) {
		return world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos, Direction.UP);
	}

	private BlockPos setBlockStateAndGetPos(
			FallenTreeFeatureConfig config,
			StructureWorldAccess world,
			Random random,
			BlockPos.Mutable pos,
			Function<BlockState, BlockState> stateFunction
	) {
		world.setBlockState(pos, stateFunction.apply(config.trunkProvider.get(random, pos)), 3);
		markBlocksAboveForPostProcessing(world, pos);
		return pos.toImmutable();
	}

	private void applyDecorators(
			StructureWorldAccess world,
			Random random,
			Set<BlockPos> positions,
			List<TreeDecorator> decorators
	) {
		if (decorators.isEmpty()) {
			return;
		}

		TreeDecorator.Generator generator = new TreeDecorator.Generator(
				world,
				createStatePlacer(world),
				random,
				positions,
				Set.of(),
				Set.of()
		);
		decorators.forEach(decorator -> decorator.generate(generator));
	}

	private BiConsumer<BlockPos, BlockState> createStatePlacer(StructureWorldAccess world) {
		return (pos, state) -> world.setBlockState(pos, state, 19);
	}

	private static Function<BlockState, BlockState> createAxisApplier(Direction direction) {
		return state -> state.withIfExists(PillarBlock.AXIS, direction.getAxis());
	}
}
