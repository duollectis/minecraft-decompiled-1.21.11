package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.MushroomBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

/**
 * Генерирует огромный коричневый гриб с плоской широкой шляпкой.
 * Шляпка формируется как квадратный диск с закруглёнными углами,
 * где боковые грани блоков шляпки ориентированы наружу.
 */
public class HugeBrownMushroomFeature extends HugeMushroomFeature {

	private static final int CAP_MIN_Y = 3;

	public HugeBrownMushroomFeature(Codec<HugeMushroomFeatureConfig> codec) {
		super(codec);
	}

	@Override
	protected void generateCap(
		WorldAccess world,
		Random random,
		BlockPos start,
		int y,
		BlockPos.Mutable mutable,
		HugeMushroomFeatureConfig config
	) {
		int radius = config.foliageRadius;

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				boolean onWestEdge = dx == -radius;
				boolean onEastEdge = dx == radius;
				boolean onNorthEdge = dz == -radius;
				boolean onSouthEdge = dz == radius;
				boolean onXEdge = onWestEdge || onEastEdge;
				boolean onZEdge = onNorthEdge || onSouthEdge;

				if (onXEdge && onZEdge) {
					continue;
				}

				mutable.set(start, dx, y, dz);

				boolean facingWest = onWestEdge || (onZEdge && dx == 1 - radius);
				boolean facingEast = onEastEdge || (onZEdge && dx == radius - 1);
				boolean facingNorth = onNorthEdge || (onXEdge && dz == 1 - radius);
				boolean facingSouth = onSouthEdge || (onXEdge && dz == radius - 1);

				BlockState capState = config.capProvider.get(random, start);

				if (capState.contains(MushroomBlock.WEST)
					&& capState.contains(MushroomBlock.EAST)
					&& capState.contains(MushroomBlock.NORTH)
					&& capState.contains(MushroomBlock.SOUTH)
				) {
					capState = capState
						.with(MushroomBlock.WEST, facingWest)
						.with(MushroomBlock.EAST, facingEast)
						.with(MushroomBlock.NORTH, facingNorth)
						.with(MushroomBlock.SOUTH, facingSouth);
				}

				generateStem(world, mutable, capState);
			}
		}
	}

	@Override
	protected int getCapSize(int i, int j, int capSize, int y) {
		return y <= CAP_MIN_Y ? 0 : capSize;
	}
}
