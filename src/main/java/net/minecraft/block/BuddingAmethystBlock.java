package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * Блок зарождающегося аметиста. Случайно выращивает кристаллы аметиста
 * на соседних блоках, проходя через стадии: малый → средний → большой → кластер.
 */
public class BuddingAmethystBlock extends AmethystBlock {

	public static final MapCodec<BuddingAmethystBlock> CODEC = createCodec(BuddingAmethystBlock::new);
	public static final int GROW_CHANCE = 5;
	private static final Direction[] DIRECTIONS = Direction.values();

	@Override
	public MapCodec<BuddingAmethystBlock> getCodec() {
		return CODEC;
	}

	public BuddingAmethystBlock(AbstractBlock.Settings settings) {
		super(settings);
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (random.nextInt(GROW_CHANCE) != 0) {
			return;
		}

		Direction direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
		BlockPos neighborPos = pos.offset(direction);
		BlockState neighborState = world.getBlockState(neighborPos);
		Block nextStage = resolveNextGrowthStage(neighborState, direction);

		if (nextStage == null) {
			return;
		}

		BlockState newState = nextStage.getDefaultState()
			.with(AmethystClusterBlock.FACING, direction)
			.with(AmethystClusterBlock.WATERLOGGED, neighborState.getFluidState().getFluid() == Fluids.WATER);

		world.setBlockState(neighborPos, newState);
	}

	/**
	 * Определяет следующую стадию роста кристалла аметиста для соседнего блока.
	 * Возвращает {@code null}, если рост невозможен.
	 */
	private static Block resolveNextGrowthStage(BlockState neighborState, Direction direction) {
		if (canGrowIn(neighborState)) {
			return Blocks.SMALL_AMETHYST_BUD;
		}

		if (neighborState.isOf(Blocks.SMALL_AMETHYST_BUD)
			&& neighborState.get(AmethystClusterBlock.FACING) == direction
		) {
			return Blocks.MEDIUM_AMETHYST_BUD;
		}

		if (neighborState.isOf(Blocks.MEDIUM_AMETHYST_BUD)
			&& neighborState.get(AmethystClusterBlock.FACING) == direction
		) {
			return Blocks.LARGE_AMETHYST_BUD;
		}

		if (neighborState.isOf(Blocks.LARGE_AMETHYST_BUD)
			&& neighborState.get(AmethystClusterBlock.FACING) == direction
		) {
			return Blocks.AMETHYST_CLUSTER;
		}

		return null;
	}

	/**
	 * Проверяет, может ли кристалл аметиста вырасти в данном блоке.
	 * Допустимы воздух и полный блок стоячей воды (уровень 8).
	 */
	public static boolean canGrowIn(BlockState state) {
		return state.isAir() || state.isOf(Blocks.WATER) && state.getFluidState().getLevel() == 8;
	}
}
