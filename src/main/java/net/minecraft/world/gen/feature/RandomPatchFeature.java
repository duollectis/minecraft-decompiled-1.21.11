package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует случайный патч растительности или других объектов,
 * многократно пробуя разместить вложенную фичу в случайных позициях
 * в пределах заданного радиуса.
 */
public class RandomPatchFeature extends Feature<RandomPatchFeatureConfig> {

	public RandomPatchFeature(Codec<RandomPatchFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<RandomPatchFeatureConfig> context) {
		RandomPatchFeatureConfig config = context.getConfig();
		Random random = context.getRandom();
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();

		int placed = 0;
		BlockPos.Mutable mutable = new BlockPos.Mutable();
		int xzRange = config.xzSpread() + 1;
		int yRange = config.ySpread() + 1;

		for (int attempt = 0; attempt < config.tries(); attempt++) {
			mutable.set(
				origin,
				random.nextInt(xzRange) - random.nextInt(xzRange),
				random.nextInt(yRange) - random.nextInt(yRange),
				random.nextInt(xzRange) - random.nextInt(xzRange)
			);

			if (config.feature().value().generateUnregistered(world, context.getGenerator(), random, mutable)) {
				placed++;
			}
		}

		return placed > 0;
	}
}
