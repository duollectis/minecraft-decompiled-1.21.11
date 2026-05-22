package net.minecraft.world.gen.trunk;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiConsumer;

/**
 * Алгоритм размещения раздвоенного ствола дерева (акация).
 * Строит основной ствол, который смещается в случайном направлении, а затем
 * генерирует вторую ветвь в другом случайном направлении, начинающуюся ниже вершины.
 */
public class ForkingTrunkPlacer extends TrunkPlacer {

	public static final MapCodec<ForkingTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillTrunkPlacerFields(instance).apply(instance, ForkingTrunkPlacer::new)
	);

	public ForkingTrunkPlacer(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
		super(baseHeight, firstRandomHeight, secondRandomHeight);
	}

	@Override
	protected TrunkPlacerType<?> getType() {
		return TrunkPlacerType.FORKING_TRUNK_PLACER;
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
		List<FoliagePlacer.TreeNode> nodes = Lists.newArrayList();
		Direction mainDir = Direction.Type.HORIZONTAL.random(random);
		int leanStartY = height - random.nextInt(4) - 1;
		int leanSteps = 3 - random.nextInt(3);
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int currentX = startPos.getX();
		int currentZ = startPos.getZ();
		OptionalInt topY = OptionalInt.empty();

		for (int y = 0; y < height; y++) {
			int worldY = startPos.getY() + y;

			if (y >= leanStartY && leanSteps > 0) {
				currentX += mainDir.getOffsetX();
				currentZ += mainDir.getOffsetZ();
				leanSteps--;
			}

			if (getAndSetState(world, replacer, random, mutable.set(currentX, worldY, currentZ), config)) {
				topY = OptionalInt.of(worldY + 1);
			}
		}

		if (topY.isPresent()) {
			nodes.add(new FoliagePlacer.TreeNode(new BlockPos(currentX, topY.getAsInt(), currentZ), 1, false));
		}

		currentX = startPos.getX();
		currentZ = startPos.getZ();
		Direction forkDir = Direction.Type.HORIZONTAL.random(random);

		if (forkDir != mainDir) {
			int forkStartY = leanStartY - random.nextInt(2) - 1;
			int forkLen = 1 + random.nextInt(3);
			topY = OptionalInt.empty();

			for (int y = forkStartY; y < height && forkLen > 0; forkLen--) {
				if (y >= 1) {
					int worldY = startPos.getY() + y;
					currentX += forkDir.getOffsetX();
					currentZ += forkDir.getOffsetZ();

					if (getAndSetState(world, replacer, random, mutable.set(currentX, worldY, currentZ), config)) {
						topY = OptionalInt.of(worldY + 1);
					}
				}

				y++;
			}

			if (topY.isPresent()) {
				nodes.add(new FoliagePlacer.TreeNode(new BlockPos(currentX, topY.getAsInt(), currentZ), 0, false));
			}
		}

		return nodes;
	}
}
