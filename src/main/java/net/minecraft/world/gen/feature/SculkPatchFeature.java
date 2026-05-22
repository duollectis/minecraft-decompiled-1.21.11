package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SculkShriekerBlock;
import net.minecraft.block.SculkSpreadable;
import net.minecraft.block.entity.SculkSpreadManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Генерирует патч скалка: распространяет скалк через {@link SculkSpreadManager},
 * затем опционально размещает катализатор и редкие визжалки вокруг точки генерации.
 */
public class SculkPatchFeature extends Feature<SculkPatchFeatureConfig> {

	public SculkPatchFeature(Codec<SculkPatchFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean generate(FeatureContext<SculkPatchFeatureConfig> context) {
		StructureWorldAccess world = context.getWorld();
		BlockPos origin = context.getOrigin();

		if (!canGenerate(world, origin)) {
			return false;
		}

		SculkPatchFeatureConfig config = context.getConfig();
		Random random = context.getRandom();
		SculkSpreadManager spreadManager = SculkSpreadManager.createWorldGen();
		int totalRounds = config.spreadRounds() + config.growthRounds();

		for (int round = 0; round < totalRounds; round++) {
			for (int charge = 0; charge < config.chargeCount(); charge++) {
				spreadManager.spread(origin, config.amountPerCharge());
			}

			boolean isSpreading = round < config.spreadRounds();

			for (int attempt = 0; attempt < config.spreadAttempts(); attempt++) {
				spreadManager.tick(world, origin, random, isSpreading);
			}

			spreadManager.clearCursors();
		}

		BlockPos below = origin.down();

		if (random.nextFloat() <= config.catalystChance()
			&& world.getBlockState(below).isFullCube(world, below)
		) {
			world.setBlockState(origin, Blocks.SCULK_CATALYST.getDefaultState(), 3);
		}

		int rareCount = config.extraRareGrowths().get(random);

		for (int idx = 0; idx < rareCount; idx++) {
			BlockPos shriekerPos = origin.add(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
			BlockPos shriekerBelow = shriekerPos.down();

			if (world.getBlockState(shriekerPos).isAir()
				&& world.getBlockState(shriekerBelow).isSideSolidFullSquare(world, shriekerBelow, Direction.UP)
			) {
				world.setBlockState(
					shriekerPos,
					Blocks.SCULK_SHRIEKER.getDefaultState().with(SculkShriekerBlock.CAN_SUMMON, true),
					3
				);
			}
		}

		return true;
	}

	private boolean canGenerate(WorldAccess world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);

		if (state.getBlock() instanceof SculkSpreadable) {
			return true;
		}

		if (state.isAir() || (state.isOf(Blocks.WATER) && state.getFluidState().isStill())) {
			return Direction
				.stream()
				.map(pos::offset)
				.anyMatch(neighbor -> world.getBlockState(neighbor).isFullCube(world, neighbor));
		}

		return false;
	}
}
