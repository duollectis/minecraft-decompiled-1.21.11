package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует небольшой плавающий остров из камня Края, уменьшающийся по радиусу снизу вверх. */
public class EndIslandFeature extends Feature<DefaultFeatureConfig> {

	public EndIslandFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		BlockPos origin = context.getOrigin();
		float radius = random.nextInt(3) + 4.0F;

		for (int dy = 0; radius > 0.5F; dy--) {
			for (int dx = MathHelper.floor(-radius); dx <= MathHelper.ceil(radius); dx++) {
				for (int dz = MathHelper.floor(-radius); dz <= MathHelper.ceil(radius); dz++) {
					if (dx * dx + dz * dz <= (radius + 1.0F) * (radius + 1.0F)) {
						setBlockState(world, origin.add(dx, dy, dz), Blocks.END_STONE.getDefaultState());
					}
				}
			}

			radius -= random.nextInt(2) + 0.5F;
		}

		return true;
	}
}
