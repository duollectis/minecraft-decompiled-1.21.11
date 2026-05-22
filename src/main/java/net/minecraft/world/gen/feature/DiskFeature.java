package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует горизонтальный диск из блоков заданного типа вокруг точки происхождения. */
public class DiskFeature extends Feature<DiskFeatureConfig> {

	public DiskFeature(Codec<DiskFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DiskFeatureConfig> context) {
		DiskFeatureConfig config = context.getConfig();
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();
		boolean placed = false;
		int centerY = origin.getY();
		int topY = centerY + config.halfHeight();
		int bottomY = centerY - config.halfHeight() - 1;
		int radius = config.radius().get(random);
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (BlockPos pos : BlockPos.iterate(origin.add(-radius, 0, -radius), origin.add(radius, 0, radius))) {
			int dx = pos.getX() - origin.getX();
			int dz = pos.getZ() - origin.getZ();

			if (dx * dx + dz * dz <= radius * radius) {
				placed |= placeBlock(config, world, random, topY, bottomY, mutable.set(pos));
			}
		}

		return placed;
	}

	protected boolean placeBlock(
			DiskFeatureConfig config,
			StructureWorldAccess world,
			Random random,
			int topY,
			int bottomY,
			BlockPos.Mutable pos
	) {
		boolean placed = false;
		boolean wasPlacedPreviously = false;

		for (int y = topY; y > bottomY; y--) {
			pos.setY(y);

			if (!config.target().test(world, pos)) {
				wasPlacedPreviously = false;
				continue;
			}

			BlockState state = config.stateProvider().getBlockState(world, random, pos);
			world.setBlockState(pos, state, 2);

			if (!wasPlacedPreviously) {
				markBlocksAboveForPostProcessing(world, pos);
			}

			placed = true;
			wasPlacedPreviously = true;
		}

		return placed;
	}
}
