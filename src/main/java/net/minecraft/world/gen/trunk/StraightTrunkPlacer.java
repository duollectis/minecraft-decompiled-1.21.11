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
 * Простейший алгоритм размещения ствола: строит прямой вертикальный ствол заданной высоты
 * и возвращает единственный узел кроны на вершине.
 */
public class StraightTrunkPlacer extends TrunkPlacer {

	public static final MapCodec<StraightTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(
			instance -> fillTrunkPlacerFields(instance).apply(instance, StraightTrunkPlacer::new)
	);

	public StraightTrunkPlacer(int baseHeight, int firstRandomHeight, int secondRandomHeight) {
		super(baseHeight, firstRandomHeight, secondRandomHeight);
	}

	@Override
	protected TrunkPlacerType<?> getType() {
		return TrunkPlacerType.STRAIGHT_TRUNK_PLACER;
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

		for (int y = 0; y < height; y++) {
			getAndSetState(world, replacer, random, startPos.up(y), config);
		}

		return ImmutableList.of(new FoliagePlacer.TreeNode(startPos.up(height), 0, false));
	}
}
