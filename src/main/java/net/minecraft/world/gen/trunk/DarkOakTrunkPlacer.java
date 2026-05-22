package net.minecraft.world.gen.trunk;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Алгоритм размещения ствола тёмного дуба.
 * Строит широкий ствол 2×2 блока, который на определённой высоте начинает смещаться
 * в случайном горизонтальном направлении. Дополнительно генерирует боковые ветви
 * вокруг вершины для создания характерной раскидистой кроны.
 */
public class DarkOakTrunkPlacer extends TrunkPlacer {

	public static final MapCodec<DarkOakTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillTrunkPlacerFields(instance).apply(instance, DarkOakTrunkPlacer::new)
	);

	public DarkOakTrunkPlacer(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
		super(baseHeight, firstRandomHeight, secondRandomHeight);
	}

	@Override
	protected TrunkPlacerType<?> getType() {
		return TrunkPlacerType.DARK_OAK_TRUNK_PLACER;
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
		BlockPos below = startPos.down();
		setToDirt(world, replacer, random, below, config);
		setToDirt(world, replacer, random, below.east(), config);
		setToDirt(world, replacer, random, below.south(), config);
		setToDirt(world, replacer, random, below.south().east(), config);

		Direction leanDir = Direction.Type.HORIZONTAL.random(random);
		int leanStartY = height - random.nextInt(4);
		int leanSteps = 2 - random.nextInt(3);
		int originX = startPos.getX();
		int originY = startPos.getY();
		int originZ = startPos.getZ();
		int currentX = originX;
		int currentZ = originZ;
		int topY = originY + height - 1;

		for (int y = 0; y < height; y++) {
			if (y >= leanStartY && leanSteps > 0) {
				currentX += leanDir.getOffsetX();
				currentZ += leanDir.getOffsetZ();
				leanSteps--;
			}

			BlockPos column = new BlockPos(currentX, originY + y, currentZ);

			if (TreeFeature.isAirOrLeaves(world, column)) {
				getAndSetState(world, replacer, random, column, config);
				getAndSetState(world, replacer, random, column.east(), config);
				getAndSetState(world, replacer, random, column.south(), config);
				getAndSetState(world, replacer, random, column.east().south(), config);
			}
		}

		nodes.add(new FoliagePlacer.TreeNode(new BlockPos(currentX, topY, currentZ), 0, true));

		for (int dx = -1; dx <= 2; dx++) {
			for (int dz = -1; dz <= 2; dz++) {
				if ((dx < 0 || dx > 1 || dz < 0 || dz > 1) && random.nextInt(3) <= 0) {
					int branchLen = random.nextInt(3) + 2;

					for (int step = 0; step < branchLen; step++) {
						getAndSetState(world, replacer, random, new BlockPos(originX + dx, topY - step - 1, originZ + dz), config);
					}

					nodes.add(new FoliagePlacer.TreeNode(new BlockPos(originX + dx, topY, originZ + dz), 0, false));
				}
			}
		}

		return nodes;
	}
}
