package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует вертикальную колонну из нескольких слоёв блоков с настраиваемой высотой каждого слоя. */
public class BlockColumnFeature extends Feature<BlockColumnFeatureConfig> {

	public BlockColumnFeature(Codec<BlockColumnFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<BlockColumnFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockColumnFeatureConfig config = context.getConfig();
		Random random = context.getRandom();
		int layerCount = config.layers().size();
		int[] layerHeights = new int[layerCount];
		int totalHeight = 0;

		for (int idx = 0; idx < layerCount; idx++) {
			layerHeights[idx] = config.layers().get(idx).height().get(random);
			totalHeight += layerHeights[idx];
		}

		if (totalHeight == 0) {
			return false;
		}

		BlockPos.Mutable pos = context.getOrigin().mutableCopy();
		BlockPos.Mutable checkPos = pos.mutableCopy().move(config.direction());

		for (int step = 0; step < totalHeight; step++) {
			if (!config.allowedPlacement().test(world, checkPos)) {
				adjustLayerHeights(layerHeights, totalHeight, step, config.prioritizeTip());
				break;
			}

			checkPos.move(config.direction());
		}

		for (int layerIdx = 0; layerIdx < layerCount; layerIdx++) {
			int height = layerHeights[layerIdx];

			if (height == 0) {
				continue;
			}

			BlockColumnFeatureConfig.Layer layer = config.layers().get(layerIdx);

			for (int block = 0; block < height; block++) {
				world.setBlockState(pos, layer.state().get(random, pos), 2);
				pos.move(config.direction());
			}
		}

		return true;
	}

	private static void adjustLayerHeights(
			int[] layerHeights,
			int expectedHeight,
			int actualHeight,
			boolean prioritizeTip
	) {
		int remaining = expectedHeight - actualHeight;
		int step = prioritizeTip ? 1 : -1;
		int start = prioritizeTip ? 0 : layerHeights.length - 1;
		int end = prioritizeTip ? layerHeights.length : -1;

		for (int idx = start; idx != end && remaining > 0; idx += step) {
			int current = layerHeights[idx];
			int cut = Math.min(current, remaining);
			remaining -= cut;
			layerHeights[idx] -= cut;
		}
	}
}
