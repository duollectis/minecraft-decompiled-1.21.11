package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallSeagrassBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.ProbabilityConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует морскую траву (обычную или высокую) на дне океана.
 * Вероятность появления высокой травы задаётся через {@link ProbabilityConfig}.
 */
public class SeagrassFeature extends Feature<ProbabilityConfig> {

	public SeagrassFeature(Codec<ProbabilityConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<ProbabilityConfig> context) {
		Random random = context.getRandom();
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		ProbabilityConfig config = context.getConfig();

		int dx = random.nextInt(8) - random.nextInt(8);
		int dz = random.nextInt(8) - random.nextInt(8);
		int floorY = world.getTopY(Heightmap.Type.OCEAN_FLOOR, origin.getX() + dx, origin.getZ() + dz);
		BlockPos pos = new BlockPos(origin.getX() + dx, floorY, origin.getZ() + dz);

		if (!world.getBlockState(pos).isOf(Blocks.WATER)) {
			return false;
		}

		boolean isTall = random.nextDouble() < config.probability;
		BlockState seagrassState = isTall
			? Blocks.TALL_SEAGRASS.getDefaultState()
			: Blocks.SEAGRASS.getDefaultState();

		if (!seagrassState.canPlaceAt(world, pos)) {
			return false;
		}

		if (isTall) {
			BlockPos above = pos.up();

			if (!world.getBlockState(above).isOf(Blocks.WATER)) {
				return false;
			}

			world.setBlockState(pos, seagrassState, 2);
			world.setBlockState(above, seagrassState.with(TallSeagrassBlock.HALF, DoubleBlockHalf.UPPER), 2);
		} else {
			world.setBlockState(pos, seagrassState, 2);
		}

		return true;
	}
}
