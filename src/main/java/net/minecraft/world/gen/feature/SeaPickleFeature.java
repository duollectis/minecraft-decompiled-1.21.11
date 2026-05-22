package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SeaPickleBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.CountConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует кластеры морских огурцов на дне океана.
 * Количество попыток размещения задаётся через {@link CountConfig}.
 */
public class SeaPickleFeature extends Feature<CountConfig> {

	public SeaPickleFeature(Codec<CountConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<CountConfig> context) {
		Random random = context.getRandom();
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		int count = context.getConfig().getCount().get(random);
		int placed = 0;

		for (int attempt = 0; attempt < count; attempt++) {
			int dx = random.nextInt(8) - random.nextInt(8);
			int dz = random.nextInt(8) - random.nextInt(8);
			int floorY = world.getTopY(Heightmap.Type.OCEAN_FLOOR, origin.getX() + dx, origin.getZ() + dz);
			BlockPos pos = new BlockPos(origin.getX() + dx, floorY, origin.getZ() + dz);
			BlockState pickleState = Blocks.SEA_PICKLE.getDefaultState()
				.with(SeaPickleBlock.PICKLES, random.nextInt(4) + 1);

			if (world.getBlockState(pos).isOf(Blocks.WATER) && pickleState.canPlaceAt(world, pos)) {
				world.setBlockState(pos, pickleState, 2);
				placed++;
			}
		}

		return placed > 0;
	}
}
