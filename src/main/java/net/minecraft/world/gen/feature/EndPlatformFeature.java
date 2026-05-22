package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует стартовую платформу из обсидиана в Крае (5×5 блоков) с очисткой воздуха над ней. */
public class EndPlatformFeature extends Feature<DefaultFeatureConfig> {

	public EndPlatformFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		generate(context.getWorld(), context.getOrigin(), false);
		return true;
	}

	/**
	 * Генерирует или восстанавливает стартовую платформу Края.
	 * При {@code breakBlocks = true} разрушает существующие блоки с дропом предметов перед заменой.
	 */
	public static void generate(ServerWorldAccess world, BlockPos pos, boolean breakBlocks) {
		BlockPos.Mutable mutable = pos.mutableCopy();

		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = -1; dy < 3; dy++) {
					BlockPos blockPos = mutable.set(pos).move(dz, dy, dx);
					Block block = dy == -1 ? Blocks.OBSIDIAN : Blocks.AIR;

					if (world.getBlockState(blockPos).isOf(block)) {
						continue;
					}

					if (breakBlocks) {
						world.breakBlock(blockPos, true, null);
					}

					world.setBlockState(blockPos, block.getDefaultState(), 3);
				}
			}
		}
	}
}
