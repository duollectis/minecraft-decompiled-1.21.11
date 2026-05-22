package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.MushroomBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

/**
 * Генерирует огромный красный гриб с куполообразной шляпкой.
 * Шляпка формируется в нижних 3 уровнях как кольцо (только края),
 * а верхний уровень — полный диск. Боковые грани ориентированы наружу.
 */
public class HugeRedMushroomFeature extends HugeMushroomFeature {

	public HugeRedMushroomFeature(Codec<HugeMushroomFeatureConfig> codec) {
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
		int innerRadius = config.foliageRadius - 2;

		for (int dy = y - 3; dy <= y; dy++) {
			int outerRadius = dy < y ? config.foliageRadius : config.foliageRadius - 1;

			for (int dx = -outerRadius; dx <= outerRadius; dx++) {
				for (int dz = -outerRadius; dz <= outerRadius; dz++) {
					boolean onWestEdge = dx == -outerRadius;
					boolean onEastEdge = dx == outerRadius;
					boolean onNorthEdge = dz == -outerRadius;
					boolean onSouthEdge = dz == outerRadius;
					boolean onXEdge = onWestEdge || onEastEdge;
					boolean onZEdge = onNorthEdge || onSouthEdge;
					boolean isTopLevel = dy >= y;
					boolean isRingOnly = onXEdge != onZEdge;

					if (!isTopLevel && !isRingOnly) {
						continue;
					}

					mutable.set(start, dx, dy, dz);
					BlockState capState = config.capProvider.get(random, start);

					if (capState.contains(MushroomBlock.WEST)
						&& capState.contains(MushroomBlock.EAST)
						&& capState.contains(MushroomBlock.NORTH)
						&& capState.contains(MushroomBlock.SOUTH)
						&& capState.contains(MushroomBlock.UP)
					) {
						capState = capState
							.with(MushroomBlock.UP, dy >= y - 1)
							.with(MushroomBlock.WEST, dx < -innerRadius)
							.with(MushroomBlock.EAST, dx > innerRadius)
							.with(MushroomBlock.NORTH, dz < -innerRadius)
							.with(MushroomBlock.SOUTH, dz > innerRadius);
					}

					generateStem(world, mutable, capState);
				}
			}
		}
	}

	@Override
	protected int getCapSize(int i, int j, int capSize, int y) {
		if ((y < j && y >= j - 3) || y == j) {
			return capSize;
		}

		return 0;
	}
}
