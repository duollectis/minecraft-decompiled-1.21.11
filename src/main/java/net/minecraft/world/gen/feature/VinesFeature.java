package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.block.VineBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Размещает один блок лозы в точке генерации, если рядом есть подходящая стена.
 * Перебирает все направления (кроме DOWN) и ставит лозу, прикреплённую к первой найденной стене.
 */
public class VinesFeature extends Feature<DefaultFeatureConfig> {

	public VinesFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();

		if (!world.isAir(origin)) {
			return false;
		}

		for (Direction direction : Direction.values()) {
			if (direction == Direction.DOWN) {
				continue;
			}

			if (!VineBlock.shouldConnectTo(world, origin.offset(direction), direction)) {
				continue;
			}

			world.setBlockState(
				origin,
				Blocks.VINE.getDefaultState().with(VineBlock.getFacingProperty(direction), true),
				2
			);
			return true;
		}

		return false;
	}
}
