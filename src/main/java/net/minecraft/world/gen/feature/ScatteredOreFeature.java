package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует рассеянную руду: блоки размещаются случайно вокруг точки генерации
 * с нарастающим разбросом (до {@link #MAX_SPREAD} блоков).
 */
public class ScatteredOreFeature extends Feature<OreFeatureConfig> {

	private static final int MAX_SPREAD = 7;

	ScatteredOreFeature(Codec<OreFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<OreFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		OreFeatureConfig config = context.getConfig();
		BlockPos origin = context.getOrigin();
		int count = random.nextInt(config.size + 1);
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int placed = 0; placed < count; placed++) {
			int spread = Math.min(placed, MAX_SPREAD);
			setPos(mutable, random, origin, spread);
			BlockState existing = world.getBlockState(mutable);

			for (OreFeatureConfig.Target target : config.targets) {
				if (OreFeature.shouldPlace(existing, world::getBlockState, random, config, target, mutable)) {
					world.setBlockState(mutable, target.state, 2);
					break;
				}
			}
		}

		return true;
	}

	private void setPos(BlockPos.Mutable mutable, Random random, BlockPos origin, int spread) {
		mutable.set(origin, getSpread(random, spread), getSpread(random, spread), getSpread(random, spread));
	}

	private int getSpread(Random random, int spread) {
		return Math.round((random.nextFloat() - random.nextFloat()) * spread);
	}
}
