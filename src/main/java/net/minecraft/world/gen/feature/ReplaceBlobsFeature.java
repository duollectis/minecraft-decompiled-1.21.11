package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.jspecify.annotations.Nullable;

/**
 * Заменяет сферический объём блоков целевого типа на другой блок.
 * Сначала ищет целевой блок вниз от начальной позиции, затем
 * итерирует блоки в эллипсоиде с радиусами из конфига.
 */
public class ReplaceBlobsFeature extends Feature<ReplaceBlobsFeatureConfig> {

	public ReplaceBlobsFeature(Codec<ReplaceBlobsFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<ReplaceBlobsFeatureConfig> context) {
		ReplaceBlobsFeatureConfig config = context.getConfig();
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		Block targetBlock = config.target.getBlock();

		BlockPos center = moveDownToTarget(
			world,
			context.getOrigin()
				.mutableCopy()
				.clamp(Direction.Axis.Y, world.getBottomY() + 1, world.getTopYInclusive()),
			targetBlock
		);

		if (center == null) {
			return false;
		}

		int rx = config.getRadius().get(random);
		int ry = config.getRadius().get(random);
		int rz = config.getRadius().get(random);
		int maxRadius = Math.max(rx, Math.max(ry, rz));
		boolean placed = false;

		for (BlockPos candidate : BlockPos.iterateOutwards(center, rx, ry, rz)) {
			if (candidate.getManhattanDistance(center) > maxRadius) {
				break;
			}

			if (world.getBlockState(candidate).isOf(targetBlock)) {
				setBlockState(world, candidate, config.state);
				placed = true;
			}
		}

		return placed;
	}

	private static @Nullable BlockPos moveDownToTarget(WorldAccess world, BlockPos.Mutable mutablePos, Block target) {
		while (mutablePos.getY() > world.getBottomY() + 1) {
			if (world.getBlockState(mutablePos).isOf(target)) {
				return mutablePos;
			}

			mutablePos.move(Direction.DOWN);
		}

		return null;
	}
}
