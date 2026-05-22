package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует каплю глоустоуна, свисающую с потолка Нижнего мира.
 * Алгоритм: случайным образом выбирает 1500 позиций вокруг начальной точки
 * и размещает глоустоун там, где ровно один соседний блок уже является глоустоуном,
 * создавая органичную форму капли.
 */
public class GlowstoneBlobFeature extends Feature<DefaultFeatureConfig> {

	private static final int SPREAD_ATTEMPTS = 1500;

	public GlowstoneBlobFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();

		if (world.isAir(origin) == false) {
			return false;
		}

		BlockState above = world.getBlockState(origin.up());

		if (!above.isOf(Blocks.NETHERRACK)
			&& !above.isOf(Blocks.BASALT)
			&& !above.isOf(Blocks.BLACKSTONE)
		) {
			return false;
		}

		world.setBlockState(origin, Blocks.GLOWSTONE.getDefaultState(), 2);

		for (int attempt = 0; attempt < SPREAD_ATTEMPTS; attempt++) {
			BlockPos candidate = origin.add(
				random.nextInt(8) - random.nextInt(8),
				-random.nextInt(12),
				random.nextInt(8) - random.nextInt(8)
			);

			if (world.getBlockState(candidate).isAir() == false) {
				continue;
			}

			int glowstoneNeighbors = 0;

			for (Direction direction : Direction.values()) {
				if (world.getBlockState(candidate.offset(direction)).isOf(Blocks.GLOWSTONE)) {
					glowstoneNeighbors++;
				}

				if (glowstoneNeighbors > 1) {
					break;
				}
			}

			if (glowstoneNeighbors == 1) {
				world.setBlockState(candidate, Blocks.GLOWSTONE.getDefaultState(), 2);
			}
		}

		return true;
	}
}
