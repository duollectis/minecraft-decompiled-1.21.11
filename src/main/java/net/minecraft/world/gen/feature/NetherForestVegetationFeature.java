package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует растительность нижнемирового леса (грибы, корни и т.д.)
 * случайным образом в пределах заданного радиуса вокруг начальной точки.
 * Требует, чтобы блок под начальной точкой был нилиумом.
 */
public class NetherForestVegetationFeature extends Feature<NetherForestVegetationFeatureConfig> {

	public NetherForestVegetationFeature(Codec<NetherForestVegetationFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<NetherForestVegetationFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		NetherForestVegetationFeatureConfig config = context.getConfig();
		Random random = context.getRandom();

		BlockState baseState = world.getBlockState(origin.down());

		if (!baseState.isIn(BlockTags.NYLIUM)) {
			return false;
		}

		int originY = origin.getY();

		if (originY < world.getBottomY() + 1 || originY + 1 > world.getTopYInclusive()) {
			return false;
		}

		int placed = 0;
		int attempts = config.spreadWidth * config.spreadWidth;

		for (int attempt = 0; attempt < attempts; attempt++) {
			BlockPos candidate = origin.add(
				random.nextInt(config.spreadWidth) - random.nextInt(config.spreadWidth),
				random.nextInt(config.spreadHeight) - random.nextInt(config.spreadHeight),
				random.nextInt(config.spreadWidth) - random.nextInt(config.spreadWidth)
			);
			BlockState candidateState = config.stateProvider.get(random, candidate);

			if (world.isAir(candidate)
				&& candidate.getY() > world.getBottomY()
				&& candidateState.canPlaceAt(world, candidate)
			) {
				world.setBlockState(candidate, candidateState, 2);
				placed++;
			}
		}

		return placed > 0;
	}
}
