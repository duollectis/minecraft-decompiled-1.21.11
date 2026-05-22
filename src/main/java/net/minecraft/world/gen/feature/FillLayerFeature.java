package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Заполняет горизонтальный слой 16×16 блоков заданным состоянием блока
 * на фиксированной высоте относительно нижней границы мира.
 */
public class FillLayerFeature extends Feature<FillLayerFeatureConfig> {

	private static final int CHUNK_SIZE = 16;

	public FillLayerFeature(Codec<FillLayerFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<FillLayerFeatureConfig> context) {
		BlockPos origin = context.getOrigin();
		FillLayerFeatureConfig config = context.getConfig();
		StructureWorldAccess world = context.getWorld();
		BlockPos.Mutable pos = new BlockPos.Mutable();
		int targetY = world.getBottomY() + config.height;

		for (int dx = 0; dx < CHUNK_SIZE; dx++) {
			for (int dz = 0; dz < CHUNK_SIZE; dz++) {
				int x = origin.getX() + dx;
				int z = origin.getZ() + dz;
				pos.set(x, targetY, z);

				if (world.getBlockState(pos).isAir()) {
					world.setBlockState(pos, config.state, 2);
				}
			}
		}

		return true;
	}
}
