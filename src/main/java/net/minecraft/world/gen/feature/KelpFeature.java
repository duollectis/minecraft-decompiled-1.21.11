package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.KelpBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует колонну ламинарии (kelp) на дне океана.
 * Высота колонны — от 1 до 10 блоков. Верхний блок — {@code KELP} с
 * случайным возрастом 20–23, остальные — {@code KELP_PLANT}.
 * Если на пути встречается препятствие, пытается завершить колонну
 * на блок ниже последней допустимой позиции.
 */
public class KelpFeature extends Feature<DefaultFeatureConfig> {

	private static final int MAX_KELP_HEIGHT = 10;
	private static final int KELP_AGE_MIN = 20;
	private static final int KELP_AGE_RANGE = 4;

	public KelpFeature(Codec<DefaultFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();
		Random random = context.getRandom();

		int floorY = world.getTopY(Heightmap.Type.OCEAN_FLOOR, origin.getX(), origin.getZ());
		BlockPos pos = new BlockPos(origin.getX(), floorY, origin.getZ());

		if (world.getBlockState(pos).isOf(Blocks.WATER) == false) {
			return false;
		}

		BlockState kelpTop = Blocks.KELP.getDefaultState();
		BlockState kelpBody = Blocks.KELP_PLANT.getDefaultState();
		int height = 1 + random.nextInt(MAX_KELP_HEIGHT);
		int placed = 0;

		for (int step = 0; step <= height; step++) {
			if (world.getBlockState(pos).isOf(Blocks.WATER)
				&& world.getBlockState(pos.up()).isOf(Blocks.WATER)
				&& kelpBody.canPlaceAt(world, pos)
			) {
				if (step == height) {
					world.setBlockState(pos, kelpTop.with(KelpBlock.AGE, KELP_AGE_MIN + random.nextInt(KELP_AGE_RANGE)), 2);
					placed++;
				} else {
					world.setBlockState(pos, kelpBody, 2);
				}
			} else if (step > 0) {
				BlockPos below = pos.down();

				if (kelpTop.canPlaceAt(world, below) && !world.getBlockState(below.down()).isOf(Blocks.KELP)) {
					world.setBlockState(below, kelpTop.with(KelpBlock.AGE, KELP_AGE_MIN + random.nextInt(KELP_AGE_RANGE)), 2);
					placed++;
				}

				break;
			}

			pos = pos.up();
		}

		return placed > 0;
	}
}
