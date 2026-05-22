package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.function.Predicate;

/**
 * Генерирует дерево с корневой системой: сначала ищет подходящую позицию
 * для дерева вверх по колонне, затем генерирует корни вдоль ствола
 * и свисающие корни вокруг основания.
 */
public class RootSystemFeature extends Feature<RootSystemFeatureConfig> {

	public RootSystemFeature(Codec<RootSystemFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<RootSystemFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();

		if (world.getBlockState(origin).isAir() == false) {
			return false;
		}

		Random random = context.getRandom();
		RootSystemFeatureConfig config = context.getConfig();
		BlockPos.Mutable mutable = origin.mutableCopy();

		if (generateTreeAndRoots(world, context.getGenerator(), config, random, mutable, origin)) {
			generateHangingRoots(world, config, random, origin, mutable);
		}

		return true;
	}

	private static boolean hasSpaceForTree(StructureWorldAccess world, RootSystemFeatureConfig config, BlockPos pos) {
		BlockPos.Mutable mutable = pos.mutableCopy();

		for (int step = 1; step <= config.requiredVerticalSpaceForTree; step++) {
			mutable.move(Direction.UP);
			BlockState state = world.getBlockState(mutable);

			if (!isAirOrWater(state, step, config.allowedVerticalWaterForTree)) {
				return false;
			}
		}

		return true;
	}

	private static boolean isAirOrWater(BlockState state, int height, int allowedWaterHeight) {
		if (state.isAir()) {
			return true;
		}

		return height + 1 <= allowedWaterHeight && state.getFluidState().isIn(FluidTags.WATER);
	}

	private static boolean generateTreeAndRoots(
		StructureWorldAccess world,
		ChunkGenerator generator,
		RootSystemFeatureConfig config,
		Random random,
		BlockPos.Mutable mutablePos,
		BlockPos origin
	) {
		for (int step = 0; step < config.maxRootColumnHeight; step++) {
			mutablePos.move(Direction.UP);

			if (!config.predicate.test(world, mutablePos) || !hasSpaceForTree(world, config, mutablePos)) {
				continue;
			}

			BlockPos below = mutablePos.down();

			if (world.getFluidState(below).isIn(FluidTags.LAVA) || !world.getBlockState(below).isSolid()) {
				return false;
			}

			if (config.feature.value().generateUnregistered(world, generator, random, mutablePos)) {
				generateRootsColumn(origin, origin.getY() + step, world, config, random);
				return true;
			}
		}

		return false;
	}

	private static void generateRootsColumn(
		BlockPos origin,
		int maxY,
		StructureWorldAccess world,
		RootSystemFeatureConfig config,
		Random random
	) {
		int x = origin.getX();
		int z = origin.getZ();
		BlockPos.Mutable mutable = origin.mutableCopy();

		for (int y = origin.getY(); y < maxY; y++) {
			generateRoots(world, config, random, x, z, mutable.set(x, y, z));
		}
	}

	private static void generateRoots(
		StructureWorldAccess world,
		RootSystemFeatureConfig config,
		Random random,
		int baseX,
		int baseZ,
		BlockPos.Mutable mutablePos
	) {
		int radius = config.rootRadius;
		Predicate<BlockState> canReplace = state -> state.isIn(config.rootReplaceable);

		for (int attempt = 0; attempt < config.rootPlacementAttempts; attempt++) {
			mutablePos.set(
				mutablePos,
				random.nextInt(radius) - random.nextInt(radius),
				0,
				random.nextInt(radius) - random.nextInt(radius)
			);

			if (canReplace.test(world.getBlockState(mutablePos))) {
				world.setBlockState(mutablePos, config.rootStateProvider.get(random, mutablePos), 2);
			}

			mutablePos.setX(baseX);
			mutablePos.setZ(baseZ);
		}
	}

	private static void generateHangingRoots(
		StructureWorldAccess world,
		RootSystemFeatureConfig config,
		Random random,
		BlockPos origin,
		BlockPos.Mutable mutablePos
	) {
		int radius = config.hangingRootRadius;
		int verticalSpan = config.hangingRootVerticalSpan;

		for (int attempt = 0; attempt < config.hangingRootPlacementAttempts; attempt++) {
			mutablePos.set(
				origin,
				random.nextInt(radius) - random.nextInt(radius),
				random.nextInt(verticalSpan) - random.nextInt(verticalSpan),
				random.nextInt(radius) - random.nextInt(radius)
			);

			if (world.isAir(mutablePos) == false) {
				continue;
			}

			BlockState hangingState = config.hangingRootStateProvider.get(random, mutablePos);

			if (hangingState.canPlaceAt(world, mutablePos)
				&& world.getBlockState(mutablePos.up()).isSideSolidFullSquare(world, mutablePos, Direction.DOWN)
			) {
				world.setBlockState(mutablePos, hangingState, 2);
			}
		}
	}
}
