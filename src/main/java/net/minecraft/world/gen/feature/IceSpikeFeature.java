package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует ледяной шип из упакованного льда на снежной поверхности.
 * Форма — конус с убывающим радиусом снизу вверх. С малым шансом
 * генерирует очень высокий шип (поднятый на 10–40 блоков вверх).
 * Также формирует зеркальное отражение шипа вниз для создания корней.
 */
public class IceSpikeFeature extends Feature<DefaultFeatureConfig> {

	private static final int MIN_Y_ABOVE_BOTTOM = 2;
	private static final int TALL_SPIKE_CHANCE = 60;
	private static final int TALL_SPIKE_LIFT_MIN = 10;
	private static final int TALL_SPIKE_LIFT_EXTRA = 30;
	private static final int ROOT_DEPTH_LIMIT = 50;
	private static final int ROOT_SKIP_CHANCE = 5;

	public IceSpikeFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		BlockPos pos = context.getOrigin();
		Random random = context.getRandom();
		StructureWorldAccess world = context.getWorld();

		while (world.isAir(pos) && pos.getY() > world.getBottomY() + MIN_Y_ABOVE_BOTTOM) {
			pos = pos.down();
		}

		if (world.getBlockState(pos).isOf(Blocks.SNOW_BLOCK) == false) {
			return false;
		}

		pos = pos.up(random.nextInt(4));

		int spikeHeight = random.nextInt(4) + 7;
		int spikeRadius = spikeHeight / 4 + random.nextInt(2);

		if (spikeRadius > 1 && random.nextInt(TALL_SPIKE_CHANCE) == 0) {
			pos = pos.up(TALL_SPIKE_LIFT_MIN + random.nextInt(TALL_SPIKE_LIFT_EXTRA));
		}

		for (int dy = 0; dy < spikeHeight; dy++) {
			float radiusF = (1.0F - (float) dy / spikeHeight) * spikeRadius;
			int radius = MathHelper.ceil(radiusF);

			for (int dx = -radius; dx <= radius; dx++) {
				float edgeX = MathHelper.abs(dx) - 0.25F;

				for (int dz = -radius; dz <= radius; dz++) {
					float edgeZ = MathHelper.abs(dz) - 0.25F;
					boolean isCenter = dx == 0 && dz == 0;
					boolean withinRadius = edgeX * edgeX + edgeZ * edgeZ <= radiusF * radiusF;
					boolean isEdge = dx == -radius || dx == radius || dz == -radius || dz == radius;
					boolean skipEdge = isEdge && random.nextFloat() > 0.75F;

					if (!isCenter && !withinRadius) {
						continue;
					}

					if (skipEdge) {
						continue;
					}

					BlockState above = world.getBlockState(pos.add(dx, dy, dz));

					if (above.isAir() || isSoil(above) || above.isOf(Blocks.SNOW_BLOCK) || above.isOf(Blocks.ICE)) {
						setBlockState(world, pos.add(dx, dy, dz), Blocks.PACKED_ICE.getDefaultState());
					}

					if (dy != 0 && radius > 1) {
						BlockState below = world.getBlockState(pos.add(dx, -dy, dz));

						if (below.isAir() || isSoil(below) || below.isOf(Blocks.SNOW_BLOCK) || below.isOf(Blocks.ICE)) {
							setBlockState(world, pos.add(dx, -dy, dz), Blocks.PACKED_ICE.getDefaultState());
						}
					}
				}
			}
		}

		int rootRadius = Math.min(Math.max(spikeRadius - 1, 0), 1);

		for (int dx = -rootRadius; dx <= rootRadius; dx++) {
			for (int dz = -rootRadius; dz <= rootRadius; dz++) {
				BlockPos rootPos = pos.add(dx, -1, dz);
				int skipInterval = ROOT_DEPTH_LIMIT;

				if (Math.abs(dx) == 1 && Math.abs(dz) == 1) {
					skipInterval = random.nextInt(ROOT_SKIP_CHANCE);
				}

				while (rootPos.getY() > ROOT_DEPTH_LIMIT) {
					BlockState state = world.getBlockState(rootPos);

					if (!state.isAir()
						&& !isSoil(state)
						&& !state.isOf(Blocks.SNOW_BLOCK)
						&& !state.isOf(Blocks.ICE)
						&& !state.isOf(Blocks.PACKED_ICE)
					) {
						break;
					}

					setBlockState(world, rootPos, Blocks.PACKED_ICE.getDefaultState());
					rootPos = rootPos.down();

					if (--skipInterval <= 0) {
						rootPos = rootPos.down(random.nextInt(5) + 1);
						skipInterval = random.nextInt(ROOT_SKIP_CHANCE);
					}
				}
			}
		}

		return true;
	}
}
