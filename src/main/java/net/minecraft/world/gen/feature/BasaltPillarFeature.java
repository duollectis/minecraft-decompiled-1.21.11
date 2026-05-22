package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Генерирует вертикальный столб базальта с боковыми ответвлениями и основанием из разбросанных блоков. */
public class BasaltPillarFeature extends Feature<DefaultFeatureConfig> {

	public BasaltPillarFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		BlockPos origin = context.getOrigin();
		StructureWorldAccess world = context.getWorld();
		Random random = context.getRandom();

		if (!world.isAir(origin) || world.isAir(origin.up())) {
			return false;
		}

		BlockPos.Mutable pos = origin.mutableCopy();
		BlockPos.Mutable neighbor = origin.mutableCopy();
		boolean northActive = true;
		boolean southActive = true;
		boolean westActive = true;
		boolean eastActive = true;

		while (world.isAir(pos)) {
			if (world.isOutOfHeightLimit(pos)) {
				return true;
			}

			world.setBlockState(pos, Blocks.BASALT.getDefaultState(), 2);
			northActive = northActive && stopOrPlaceBasalt(world, random, neighbor.set(pos, Direction.NORTH));
			southActive = southActive && stopOrPlaceBasalt(world, random, neighbor.set(pos, Direction.SOUTH));
			westActive = westActive && stopOrPlaceBasalt(world, random, neighbor.set(pos, Direction.WEST));
			eastActive = eastActive && stopOrPlaceBasalt(world, random, neighbor.set(pos, Direction.EAST));
			pos.move(Direction.DOWN);
		}

		pos.move(Direction.UP);
		tryPlaceBasalt(world, random, neighbor.set(pos, Direction.NORTH));
		tryPlaceBasalt(world, random, neighbor.set(pos, Direction.SOUTH));
		tryPlaceBasalt(world, random, neighbor.set(pos, Direction.WEST));
		tryPlaceBasalt(world, random, neighbor.set(pos, Direction.EAST));
		pos.move(Direction.DOWN);

		BlockPos.Mutable basePos = new BlockPos.Mutable();

		for (int dx = -3; dx < 4; dx++) {
			for (int dz = -3; dz < 4; dz++) {
				int spread = MathHelper.abs(dx) * MathHelper.abs(dz);

				if (random.nextInt(10) < 10 - spread) {
					basePos.set(pos.add(dx, 0, dz));
					int searchDepth = 3;

					while (world.isAir(neighbor.set(basePos, Direction.DOWN))) {
						basePos.move(Direction.DOWN);

						if (--searchDepth <= 0) {
							break;
						}
					}

					if (!world.isAir(neighbor.set(basePos, Direction.DOWN))) {
						world.setBlockState(basePos, Blocks.BASALT.getDefaultState(), 2);
					}
				}
			}
		}

		return true;
	}

	private void tryPlaceBasalt(WorldAccess world, Random random, BlockPos pos) {
		if (random.nextBoolean()) {
			world.setBlockState(pos, Blocks.BASALT.getDefaultState(), 2);
		}
	}

	private boolean stopOrPlaceBasalt(WorldAccess world, Random random, BlockPos pos) {
		if (random.nextInt(10) == 0) {
			return false;
		}

		world.setBlockState(pos, Blocks.BASALT.getDefaultState(), 2);
		return true;
	}
}
