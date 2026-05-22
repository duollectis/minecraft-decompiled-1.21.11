package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

/** Генерирует коралл в форме гриба: полый прямоугольный параллелепипед с открытыми гранями. */
public class CoralMushroomFeature extends CoralFeature {

	public CoralMushroomFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	protected boolean generateCoral(WorldAccess world, Random random, BlockPos pos, BlockState state) {
		int sizeX = random.nextInt(3) + 3;
		int sizeY = random.nextInt(3) + 3;
		int sizeZ = random.nextInt(3) + 3;
		int offsetDown = random.nextInt(3) + 1;
		BlockPos.Mutable mutable = pos.mutableCopy();

		for (int dx = 0; dx <= sizeX; dx++) {
			for (int dy = 0; dy <= sizeY; dy++) {
				for (int dz = 0; dz <= sizeZ; dz++) {
					mutable.set(dx + pos.getX(), dy + pos.getY(), dz + pos.getZ());
					mutable.move(Direction.DOWN, offsetDown);

					boolean onXEdge = dx == 0 || dx == sizeX;
					boolean onYEdge = dy == 0 || dy == sizeY;
					boolean onZEdge = dz == 0 || dz == sizeZ;

					// Пропускаем внутренние блоки и рёбра, оставляя только грани
					if ((onXEdge || onYEdge) && (onZEdge || onYEdge) && (onXEdge || onZEdge)) {
						continue;
					}

					if (!onXEdge && !onYEdge && !onZEdge) {
						continue;
					}

					if (random.nextFloat() < 0.1F) {
						continue;
					}

					generateCoralPiece(world, random, mutable, state);
				}
			}
		}

		return true;
	}
}
