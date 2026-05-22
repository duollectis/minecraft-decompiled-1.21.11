package net.minecraft.world.gen.trunk;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Алгоритм размещения гигантского ствола дерева (гигантская ель/секвойя).
 * Строит широкий ствол 2×2 блока, устанавливая все четыре колонны на каждом уровне высоты.
 * На последнем уровне размещается только одна колонна (для формирования вершины).
 */
public class GiantTrunkPlacer extends TrunkPlacer {

	public static final MapCodec<GiantTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillTrunkPlacerFields(instance).apply(instance, GiantTrunkPlacer::new)
	);

	public GiantTrunkPlacer(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
		super(baseHeight, firstRandomHeight, secondRandomHeight);
	}

	@Override
	protected TrunkPlacerType<?> getType() {
		return TrunkPlacerType.GIANT_TRUNK_PLACER;
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
		BlockPos below = startPos.down();
		setToDirt(world, replacer, random, below, config);
		setToDirt(world, replacer, random, below.east(), config);
		setToDirt(world, replacer, random, below.south(), config);
		setToDirt(world, replacer, random, below.south().east(), config);
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int y = 0; y < height; y++) {
			setLog(world, replacer, random, mutable, config, startPos, 0, y, 0);

			if (y < height - 1) {
				setLog(world, replacer, random, mutable, config, startPos, 1, y, 0);
				setLog(world, replacer, random, mutable, config, startPos, 1, y, 1);
				setLog(world, replacer, random, mutable, config, startPos, 0, y, 1);
			}
		}

		return ImmutableList.of(new FoliagePlacer.TreeNode(startPos.up(height), 0, true));
	}

	private void setLog(
			TestableWorld world,
			BiConsumer<BlockPos, BlockState> replacer,
			Random random,
			BlockPos.Mutable tmpPos,
			TreeFeatureConfig config,
			BlockPos startPos,
			int dx,
			int dy,
			int dz
	) {
		tmpPos.set(startPos, dx, dy, dz);
		trySetState(world, replacer, random, tmpPos, config);
	}
}
