package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует кучу блоков в форме эллипса вокруг точки происхождения, опираясь на поверхность под ней. */
public class BlockPileFeature extends Feature<BlockPileFeatureConfig> {

	public BlockPileFeature(Codec<BlockPileFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<BlockPileFeatureConfig> context) {
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		BlockPileFeatureConfig config = context.getConfig();

		if (origin.getY() < world.getBottomY() + 5) {
			return false;
		}

		int radiusX = 2 + random.nextInt(2);
		int radiusZ = 2 + random.nextInt(2);

		for (BlockPos pos : BlockPos.iterate(origin.add(-radiusX, 0, -radiusZ), origin.add(radiusX, 1, radiusZ))) {
			int dx = origin.getX() - pos.getX();
			int dz = origin.getZ() - pos.getZ();
			float distSq = dx * dx + dz * dz;

			if (distSq <= random.nextFloat() * 10.0F - random.nextFloat() * 6.0F
					|| random.nextFloat() < 0.031F) {
				addPileBlock(world, pos, random, config);
			}
		}

		return true;
	}

	private boolean canPlace(WorldAccess world, BlockPos pos, Random random) {
		BlockPos below = pos.down();
		BlockState belowState = world.getBlockState(below);
		return belowState.isOf(Blocks.DIRT_PATH)
				? random.nextBoolean()
				: belowState.isSideSolidFullSquare(world, below, Direction.UP);
	}

	private void addPileBlock(WorldAccess world, BlockPos pos, Random random, BlockPileFeatureConfig config) {
		if (world.isAir(pos) && canPlace(world, pos, random)) {
			world.setBlockState(pos, config.stateProvider.get(random, pos), 260);
		}
	}
}
