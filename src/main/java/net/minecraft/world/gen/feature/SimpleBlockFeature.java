package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaleMossCarpetBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Размещает одиночный блок из {@link SimpleBlockFeatureConfig#toPlace()} в точке генерации.
 * Поддерживает двухблочные растения ({@link TallPlantBlock}) и бледный мох ({@link PaleMossCarpetBlock}).
 */
public class SimpleBlockFeature extends Feature<SimpleBlockFeatureConfig> {

	public SimpleBlockFeature(Codec<SimpleBlockFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<SimpleBlockFeatureConfig> context) {
		SimpleBlockFeatureConfig config = context.getConfig();
		StructureWorldAccess world = context.getWorld();
		BlockPos pos = context.getOrigin();
		BlockState state = config.toPlace().get(context.getRandom(), pos);

		if (!state.canPlaceAt(world, pos)) {
			return false;
		}

		if (state.getBlock() instanceof TallPlantBlock) {
			if (!world.isAir(pos.up())) {
				return false;
			}

			TallPlantBlock.placeAt(world, state, pos, 2);
		} else if (state.getBlock() instanceof PaleMossCarpetBlock) {
			PaleMossCarpetBlock.placeAt(world, pos, world.getRandom(), 2);
		} else {
			world.setBlockState(pos, state, 2);
		}

		if (config.scheduleTick()) {
			world.scheduleBlockTick(pos, world.getBlockState(pos).getBlock(), 1);
		}

		return true;
	}
}
