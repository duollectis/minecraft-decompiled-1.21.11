package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;

import java.util.List;
import java.util.stream.Stream;

/** Генерирует коралл в форме когтя: центральный стебель с 2–3 боковыми ветвями, изгибающимися вверх. */
public class CoralClawFeature extends CoralFeature {

	public CoralClawFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	protected boolean generateCoral(WorldAccess world, Random random, BlockPos pos, BlockState state) {
		if (!generateCoralPiece(world, random, pos, state)) {
			return false;
		}

		Direction mainDir = Direction.Type.HORIZONTAL.random(random);
		int branchCount = random.nextInt(2) + 2;
		List<Direction> branches = Util.copyShuffled(
				Stream.of(mainDir, mainDir.rotateYClockwise(), mainDir.rotateYCounterclockwise()),
				random
		);

		for (Direction branchDir : branches.subList(0, branchCount)) {
			BlockPos.Mutable mutable = pos.mutableCopy();
			int stemLength = random.nextInt(2) + 1;
			mutable.move(branchDir);

			Direction growDir;
			int clawLength;

			if (branchDir == mainDir) {
				growDir = mainDir;
				clawLength = random.nextInt(3) + 2;
			} else {
				mutable.move(Direction.UP);
				Direction[] options = new Direction[]{branchDir, Direction.UP};
				growDir = Util.getRandom(options, random);
				clawLength = random.nextInt(3) + 3;
			}

			for (int step = 0; step < stemLength && generateCoralPiece(world, random, mutable, state); step++) {
				mutable.move(growDir);
			}

			mutable.move(growDir.getOpposite());
			mutable.move(Direction.UP);

			for (int step = 0; step < clawLength; step++) {
				mutable.move(mainDir);

				if (!generateCoralPiece(world, random, mutable, state)) {
					break;
				}

				if (random.nextFloat() < 0.25F) {
					mutable.move(Direction.UP);
				}
			}
		}

		return true;
	}
}
