package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует стартовую платформу для режима «Пустота» (Void).
 * Платформа размещается в чанках, находящихся не далее 1 чанка от стартового.
 * Центральный блок — булыжник, остальные — камень.
 */
public class VoidStartPlatformFeature extends Feature<DefaultFeatureConfig> {

	private static final BlockPos START_BLOCK = new BlockPos(8, 3, 8);
	private static final ChunkPos START_CHUNK = new ChunkPos(START_BLOCK);
	private static final int CHUNK_RADIUS = 1;
	private static final int PLATFORM_RADIUS = 16;

	public VoidStartPlatformFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	private static int getChebyshevDistance(int x1, int z1, int x2, int z2) {
		return Math.max(Math.abs(x1 - x2), Math.abs(z1 - z2));
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		ChunkPos chunkPos = new ChunkPos(context.getOrigin());

		if (getChebyshevDistance(chunkPos.x, chunkPos.z, START_CHUNK.x, START_CHUNK.z) > CHUNK_RADIUS) {
			return true;
		}

		BlockPos center = START_BLOCK.withY(context.getOrigin().getY() + START_BLOCK.getY());
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
			for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
				if (getChebyshevDistance(center.getX(), center.getZ(), x, z) > PLATFORM_RADIUS) {
					continue;
				}

				mutable.set(x, center.getY(), z);

				if (mutable.equals(center)) {
					world.setBlockState(mutable, Blocks.COBBLESTONE.getDefaultState(), 2);
				} else {
					world.setBlockState(mutable, Blocks.STONE.getDefaultState(), 2);
				}
			}
		}

		return true;
	}
}
