package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Выбирает случайную фичу из списка {@link SimpleRandomFeatureConfig#features}
 * и генерирует её в точке вызова.
 */
public class SimpleRandomFeature extends Feature<SimpleRandomFeatureConfig> {

	public SimpleRandomFeature(Codec<SimpleRandomFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<SimpleRandomFeatureConfig> context) {
		Random random = context.getRandom();
		SimpleRandomFeatureConfig config = context.getConfig();
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		ChunkGenerator generator = context.getGenerator();

		int index = random.nextInt(config.features.size());
		PlacedFeature chosen = config.features.get(index).value();

		return chosen.generateUnregistered(world, generator, random, origin);
	}
}
