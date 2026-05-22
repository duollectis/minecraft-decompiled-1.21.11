package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует небольшой каменный валун в лесу: 3 итерации случайных эллипсоидов,
 * каждый раз смещая центр вниз и в сторону, создавая органичную форму.
 */
public class ForestRockFeature extends Feature<SingleStateFeatureConfig> {

	private static final int BOULDER_ITERATIONS = 3;
	private static final int MIN_Y_OFFSET_FROM_BOTTOM = 3;

	public ForestRockFeature(Codec<SingleStateFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<SingleStateFeatureConfig> context) {
		BlockPos pos = context.getOrigin();
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		SingleStateFeatureConfig config = context.getConfig();

		while (pos.getY() > world.getBottomY() + MIN_Y_OFFSET_FROM_BOTTOM) {
			if (world.isAir(pos.down())) {
				pos = pos.down();
				continue;
			}

			BlockState below = world.getBlockState(pos.down());

			if (isSoil(below) || isStone(below)) {
				break;
			}

			pos = pos.down();
		}

		if (pos.getY() <= world.getBottomY() + MIN_Y_OFFSET_FROM_BOTTOM) {
			return false;
		}

		for (int iteration = 0; iteration < BOULDER_ITERATIONS; iteration++) {
			int rx = random.nextInt(2);
			int ry = random.nextInt(2);
			int rz = random.nextInt(2);
			float radius = (rx + ry + rz) * 0.333F + 0.5F;

			for (BlockPos candidate : BlockPos.iterate(pos.add(-rx, -ry, -rz), pos.add(rx, ry, rz))) {
				if (candidate.getSquaredDistance(pos) <= radius * radius) {
					world.setBlockState(candidate, config.state, 3);
				}
			}

			pos = pos.add(-1 + random.nextInt(2), -random.nextInt(2), -1 + random.nextInt(2));
		}

		return true;
	}
}
