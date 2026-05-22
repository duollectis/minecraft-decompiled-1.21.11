package net.minecraft.world.gen.root;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Размещатель корней мангрового дерева — генерирует разветвлённую корневую систему,
 * растущую вниз и в стороны от ствола, с поддержкой грязных корней в болотных блоках.
 */
public class MangroveRootPlacer extends RootPlacer {

	public static final int MAX_ROOT_WIDTH = 8;
	public static final int MAX_ROOT_LENGTH = 15;
	public static final MapCodec<MangroveRootPlacer> CODEC = RecordCodecBuilder.mapCodec(
		instance -> createCodecParts(instance)
			.and(MangroveRootPlacement.CODEC
				.fieldOf("mangrove_root_placement")
				.forGetter(placer -> placer.mangroveRootPlacement))
			.apply(instance, MangroveRootPlacer::new)
	);
	private final MangroveRootPlacement mangroveRootPlacement;

	public MangroveRootPlacer(
		IntProvider trunkOffsetY,
		BlockStateProvider rootProvider,
		Optional<AboveRootPlacement> aboveRootPlacement,
		MangroveRootPlacement mangroveRootPlacement
	) {
		super(trunkOffsetY, rootProvider, aboveRootPlacement);
		this.mangroveRootPlacement = mangroveRootPlacement;
	}

	@Override
	public boolean generate(
		TestableWorld world,
		BiConsumer<BlockPos, BlockState> replacer,
		Random random,
		BlockPos pos,
		BlockPos trunkPos,
		TreeFeatureConfig config
	) {
		List<BlockPos> rootPositions = Lists.newArrayList();
		BlockPos.Mutable mutable = pos.mutableCopy();

		while (mutable.getY() < trunkPos.getY()) {
			if (!canGrowThrough(world, mutable)) {
				return false;
			}

			mutable.move(Direction.UP);
		}

		rootPositions.add(trunkPos.down());

		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos neighbor = trunkPos.offset(direction);
			List<BlockPos> offshootPositions = Lists.newArrayList();

			if (!canGrow(world, random, neighbor, direction, trunkPos, offshootPositions, 0)) {
				return false;
			}

			rootPositions.addAll(offshootPositions);
			rootPositions.add(trunkPos.offset(direction));
		}

		for (BlockPos rootPos : rootPositions) {
			placeRoots(world, replacer, random, rootPos, config);
		}

		return true;
	}

	private boolean canGrow(
		TestableWorld world,
		Random random,
		BlockPos pos,
		Direction direction,
		BlockPos origin,
		List<BlockPos> offshootPositions,
		int rootLength
	) {
		int maxLength = mangroveRootPlacement.maxRootLength();

		if (rootLength == maxLength || offshootPositions.size() > maxLength) {
			return false;
		}

		for (BlockPos offshoot : getOffshootPositions(pos, direction, random, origin)) {
			if (!canGrowThrough(world, offshoot)) {
				continue;
			}

			offshootPositions.add(offshoot);

			if (!canGrow(world, random, offshoot, direction, origin, offshootPositions, rootLength + 1)) {
				return false;
			}
		}

		return true;
	}

	protected List<BlockPos> getOffshootPositions(BlockPos pos, Direction direction, Random random, BlockPos origin) {
		BlockPos below = pos.down();
		BlockPos forward = pos.offset(direction);
		int distFromOrigin = pos.getManhattanDistance(origin);
		int maxWidth = mangroveRootPlacement.maxRootWidth();
		float skewChance = mangroveRootPlacement.randomSkewChance();

		if (distFromOrigin > maxWidth - 3 && distFromOrigin <= maxWidth) {
			return random.nextFloat() < skewChance
				? List.of(below, forward.down())
				: List.of(below);
		}

		if (distFromOrigin > maxWidth) {
			return List.of(below);
		}

		if (random.nextFloat() < skewChance) {
			return List.of(below);
		}

		return random.nextBoolean() ? List.of(forward) : List.of(below);
	}

	@Override
	protected boolean canGrowThrough(TestableWorld world, BlockPos pos) {
		return super.canGrowThrough(world, pos)
			|| world.testBlockState(pos, state -> state.isIn(mangroveRootPlacement.canGrowThrough()));
	}

	@Override
	protected void placeRoots(
		TestableWorld world,
		BiConsumer<BlockPos, BlockState> replacer,
		Random random,
		BlockPos pos,
		TreeFeatureConfig config
	) {
		if (world.testBlockState(pos, state -> state.isIn(mangroveRootPlacement.muddyRootsIn()))) {
			BlockState muddyState = mangroveRootPlacement.muddyRootsProvider().get(random, pos);
			replacer.accept(pos, applyWaterlogging(world, pos, muddyState));
		} else {
			super.placeRoots(world, replacer, random, pos, config);
		}
	}

	@Override
	protected RootPlacerType<?> getType() {
		return RootPlacerType.MANGROVE_ROOT_PLACER;
	}
}
