package net.minecraft.world.gen.foliage;

import com.mojang.datafixers.Products.P2;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

/**
 * Базовый класс для всех плейсеров листвы дерева.
 * Определяет форму кроны через {@link #generate} и фильтрацию позиций через {@link #isInvalidForLeaves}.
 */
public abstract class FoliagePlacer {

	public static final Codec<FoliagePlacer> TYPE_CODEC = Registries.FOLIAGE_PLACER_TYPE
		.getCodec()
		.dispatch(FoliagePlacer::getType, FoliagePlacerType::getCodec);

	protected final IntProvider radius;
	protected final IntProvider offset;

	protected static <P extends FoliagePlacer> P2<Mu<P>, IntProvider, IntProvider> fillFoliagePlacerFields(Instance<P> instance) {
		return instance.group(
			IntProvider.createValidatingCodec(0, 16).fieldOf("radius").forGetter(placer -> placer.radius),
			IntProvider.createValidatingCodec(0, 16).fieldOf("offset").forGetter(placer -> placer.offset)
		);
	}

	public FoliagePlacer(IntProvider radius, IntProvider offset) {
		this.radius = radius;
		this.offset = offset;
	}

	protected abstract FoliagePlacerType<?> getType();

	public void generate(
		TestableWorld world,
		FoliagePlacer.BlockPlacer placer,
		Random random,
		TreeFeatureConfig config,
		int trunkHeight,
		FoliagePlacer.TreeNode treeNode,
		int foliageHeight,
		int radius
	) {
		generate(world, placer, random, config, trunkHeight, treeNode, foliageHeight, radius, getRandomOffset(random));
	}

	protected abstract void generate(
		TestableWorld world,
		FoliagePlacer.BlockPlacer placer,
		Random random,
		TreeFeatureConfig config,
		int trunkHeight,
		FoliagePlacer.TreeNode treeNode,
		int foliageHeight,
		int radius,
		int offset
	);

	public abstract int getRandomHeight(Random random, int trunkHeight, TreeFeatureConfig config);

	public int getRandomRadius(Random random, int baseHeight) {
		return radius.get(random);
	}

	private int getRandomOffset(Random random) {
		return offset.get(random);
	}

	protected abstract boolean isInvalidForLeaves(Random random, int dx, int y, int dz, int radius, boolean giantTrunk);

	protected boolean isPositionInvalid(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
		int effectiveDx;
		int effectiveDz;

		if (giantTrunk) {
			effectiveDx = Math.min(Math.abs(dx), Math.abs(dx - 1));
			effectiveDz = Math.min(Math.abs(dz), Math.abs(dz - 1));
		} else {
			effectiveDx = Math.abs(dx);
			effectiveDz = Math.abs(dz);
		}

		return isInvalidForLeaves(random, effectiveDx, y, effectiveDz, radius, giantTrunk);
	}

	protected void generateSquare(
		TestableWorld world,
		FoliagePlacer.BlockPlacer placer,
		Random random,
		TreeFeatureConfig config,
		BlockPos centerPos,
		int radius,
		int y,
		boolean giantTrunk
	) {
		int extra = giantTrunk ? 1 : 0;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int dx = -radius; dx <= radius + extra; dx++) {
			for (int dz = -radius; dz <= radius + extra; dz++) {
				if (!isPositionInvalid(random, dx, y, dz, radius, giantTrunk)) {
					mutable.set(centerPos, dx, y, dz);
					placeFoliageBlock(world, placer, random, config, mutable);
				}
			}
		}
	}

	/**
	 * Генерирует квадрат листвы с опциональными свисающими листьями по краям.
	 * Свисающие листья размещаются с вероятностью {@code hangingLeavesChance},
	 * а их продолжение — с вероятностью {@code hangingLeavesExtensionChance}.
	 */
	protected final void generateSquareWithHangingLeaves(
		TestableWorld world,
		FoliagePlacer.BlockPlacer placer,
		Random random,
		TreeFeatureConfig config,
		BlockPos centerPos,
		int radius,
		int y,
		boolean giantTrunk,
		float hangingLeavesChance,
		float hangingLeavesExtensionChance
	) {
		generateSquare(world, placer, random, config, centerPos, radius, y, giantTrunk);
		int extra = giantTrunk ? 1 : 0;
		BlockPos origin = centerPos.down();
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (Direction direction : Direction.Type.HORIZONTAL) {
			Direction perpendicular = direction.rotateYClockwise();
			int edgeOffset = perpendicular.getDirection() == Direction.AxisDirection.POSITIVE ? radius + extra : radius;
			mutable.set(centerPos, 0, y - 1, 0).move(perpendicular, edgeOffset).move(direction, -radius);
			int step = -radius;

			while (step < radius + extra) {
				boolean hasLeafAbove = placer.hasPlacedBlock(mutable.move(Direction.UP));
				mutable.move(Direction.DOWN);

				if (hasLeafAbove && placeFoliageBlock(world, placer, random, config, hangingLeavesChance, origin, mutable)) {
					mutable.move(Direction.DOWN);
					placeFoliageBlock(world, placer, random, config, hangingLeavesExtensionChance, origin, mutable);
					mutable.move(Direction.UP);
				}

				step++;
				mutable.move(direction);
			}
		}
	}

	private static boolean placeFoliageBlock(
		TestableWorld world,
		FoliagePlacer.BlockPlacer placer,
		Random random,
		TreeFeatureConfig config,
		float chance,
		BlockPos origin,
		BlockPos.Mutable pos
	) {
		if (pos.getManhattanDistance(origin) >= 7) {
			return false;
		}

		return random.nextFloat() <= chance && placeFoliageBlock(world, placer, random, config, pos);
	}

	protected static boolean placeFoliageBlock(
		TestableWorld world,
		FoliagePlacer.BlockPlacer placer,
		Random random,
		TreeFeatureConfig config,
		BlockPos pos
	) {
		boolean isPersistent = world.testBlockState(pos, state -> state.get(Properties.PERSISTENT, false));

		if (isPersistent || !TreeFeature.canReplace(world, pos)) {
			return false;
		}

		BlockState foliageState = config.foliageProvider.get(random, pos);

		if (foliageState.contains(Properties.WATERLOGGED)) {
			foliageState = foliageState.with(
				Properties.WATERLOGGED,
				world.testFluidState(pos, fluidState -> fluidState.isEqualAndStill(Fluids.WATER))
			);
		}

		placer.placeBlock(pos, foliageState);
		return true;
	}

	public interface BlockPlacer {

		void placeBlock(BlockPos pos, BlockState state);

		boolean hasPlacedBlock(BlockPos pos);
	}

	public static final class TreeNode {

		private final BlockPos center;
		private final int foliageRadius;
		private final boolean giantTrunk;

		public TreeNode(BlockPos center, int foliageRadius, boolean giantTrunk) {
			this.center = center;
			this.foliageRadius = foliageRadius;
			this.giantTrunk = giantTrunk;
		}

		public BlockPos getCenter() {
			return center;
		}

		public int getFoliageRadius() {
			return foliageRadius;
		}

		public boolean isGiantTrunk() {
			return giantTrunk;
		}
	}
}
