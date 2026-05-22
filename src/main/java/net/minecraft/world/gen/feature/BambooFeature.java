package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BambooLeaves;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.ProbabilityConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует заросли бамбука с опциональным кругом подзола вокруг основания. */
public class BambooFeature extends Feature<ProbabilityConfig> {

	private static final BlockState BAMBOO = Blocks.BAMBOO
			.getDefaultState()
			.with(BambooBlock.AGE, 1)
			.with(BambooBlock.LEAVES, BambooLeaves.NONE)
			.with(BambooBlock.STAGE, 0);
	private static final BlockState BAMBOO_TOP_1 = BAMBOO.with(BambooBlock.LEAVES, BambooLeaves.LARGE).with(BambooBlock.STAGE, 1);
	private static final BlockState BAMBOO_TOP_2 = BAMBOO.with(BambooBlock.LEAVES, BambooLeaves.LARGE);
	private static final BlockState BAMBOO_TOP_3 = BAMBOO.with(BambooBlock.LEAVES, BambooLeaves.SMALL);

	public BambooFeature(Codec<ProbabilityConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<ProbabilityConfig> context) {
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		ProbabilityConfig config = context.getConfig();
		BlockPos.Mutable pos = origin.mutableCopy();
		BlockPos.Mutable soilPos = origin.mutableCopy();

		if (!world.isAir(pos)) {
			return false;
		}

		if (!Blocks.BAMBOO.getDefaultState().canPlaceAt(world, pos)) {
			return false;
		}

		int height = random.nextInt(12) + 5;
		if (random.nextFloat() < config.probability) {
			int radius = random.nextInt(4) + 1;

			for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
				for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
					int dx = x - origin.getX();
					int dz = z - origin.getZ();
					if (dx * dx + dz * dz <= radius * radius) {
						soilPos.set(x, world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z) - 1, z);
						if (isSoil(world.getBlockState(soilPos))) {
							world.setBlockState(soilPos, Blocks.PODZOL.getDefaultState(), 2);
						}
					}
				}
			}
		}

		for (int stalk = 0; stalk < height && world.isAir(pos); stalk++) {
			world.setBlockState(pos, BAMBOO, 2);
			pos.move(Direction.UP, 1);
		}

		if (pos.getY() - origin.getY() >= 3) {
			world.setBlockState(pos, BAMBOO_TOP_1, 2);
			world.setBlockState(pos.move(Direction.DOWN, 1), BAMBOO_TOP_2, 2);
			world.setBlockState(pos.move(Direction.DOWN, 1), BAMBOO_TOP_3, 2);
		}

		return true;
	}
}
