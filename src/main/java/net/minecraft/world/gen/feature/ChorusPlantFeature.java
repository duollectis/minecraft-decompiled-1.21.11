package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusFlowerBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует растение хоруса на поверхности камня Края, если позиция свободна. */
public class ChorusPlantFeature extends Feature<DefaultFeatureConfig> {

	public ChorusPlantFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();

		if (!world.isAir(origin) || !world.getBlockState(origin.down()).isOf(Blocks.END_STONE)) {
			return false;
		}

		ChorusFlowerBlock.generate(world, origin, random, 8);
		return true;
	}
}
