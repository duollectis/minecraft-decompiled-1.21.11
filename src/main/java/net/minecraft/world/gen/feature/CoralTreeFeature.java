package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

import java.util.List;

/** Генерирует коралл в форме дерева: вертикальный ствол с несколькими горизонтальными ветвями, изгибающимися вверх. */
public class CoralTreeFeature extends CoralFeature {

	public CoralTreeFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	protected boolean generateCoral(WorldAccess world, Random random, BlockPos pos, BlockState state) {
		BlockPos.Mutable mutable = pos.mutableCopy();
		int trunkHeight = random.nextInt(3) + 1;

		for (int step = 0; step < trunkHeight; step++) {
			if (!generateCoralPiece(world, random, mutable, state)) {
				return true;
			}

			mutable.move(Direction.UP);
		}

		BlockPos trunkTop = mutable.toImmutable();
		int branchCount = random.nextInt(3) + 2;
		List<Direction> directions = Direction.Type.HORIZONTAL.getShuffled(random);

		for (Direction branchDir : directions.subList(0, branchCount)) {
			mutable.set(trunkTop);
			mutable.move(branchDir);
			int branchLength = random.nextInt(5) + 2;
			int straightCount = 0;

			for (int step = 0; step < branchLength && generateCoralPiece(world, random, mutable, state); step++) {
				straightCount++;
				mutable.move(Direction.UP);

				if (step == 0 || straightCount >= 2 && random.nextFloat() < 0.25F) {
					mutable.move(branchDir);
					straightCount = 0;
				}
			}
		}

		return true;
	}
}
