package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * Базовый класс для головного стебля многоблочных растений (верхушка водоросли, лозы и т.п.).
 * Управляет возрастом роста {@link #AGE}, вероятностью роста и логикой удобрения.
 */
public abstract class AbstractPlantStemBlock extends AbstractPlantPartBlock implements Fertilizable {

	public static final IntProperty AGE = Properties.AGE_25;
	public static final int MAX_AGE = 25;

	private final double growthChance;

	protected AbstractPlantStemBlock(
		AbstractBlock.Settings settings,
		Direction growthDirection,
		VoxelShape outlineShape,
		boolean tickWater,
		double growthChance
	) {
		super(settings, growthDirection, outlineShape, tickWater);
		this.growthChance = growthChance;
		setDefaultState(stateManager.getDefaultState().with(AGE, 0));
	}

	@Override
	protected abstract MapCodec<? extends AbstractPlantStemBlock> getCodec();

	@Override
	public BlockState getRandomGrowthState(Random random) {
		return getDefaultState().with(AGE, random.nextInt(MAX_AGE));
	}

	@Override
	protected boolean hasRandomTicks(BlockState state) {
		return state.get(AGE) < MAX_AGE;
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(AGE) >= MAX_AGE || random.nextDouble() >= growthChance) {
			return;
		}

		BlockPos growPos = pos.offset(growthDirection);

		if (chooseStemState(world.getBlockState(growPos))) {
			world.setBlockState(growPos, age(state, world.random));
		}
	}

	/**
	 * Увеличивает возраст стебля на один шаг.
	 * Переопределяется подклассами для нестандартной логики старения.
	 */
	protected BlockState age(BlockState state, Random random) {
		return state.cycle(AGE);
	}

	public BlockState withMaxAge(BlockState state) {
		return state.with(AGE, MAX_AGE);
	}

	public boolean hasMaxAge(BlockState state) {
		return state.get(AGE) == MAX_AGE;
	}

	/**
	 * Копирует свойства состояния при переходе от одного блока к другому.
	 * По умолчанию возвращает целевое состояние без изменений.
	 */
	protected BlockState copyState(BlockState from, BlockState to) {
		return to;
	}

	@Override
	protected BlockState getStateForNeighborUpdate(
		BlockState state,
		WorldView world,
		ScheduledTickView tickView,
		BlockPos pos,
		Direction direction,
		BlockPos neighborPos,
		BlockState neighborState,
		Random random
	) {
		if (direction == growthDirection.getOpposite()) {
			if (!state.canPlaceAt(world, pos)) {
				tickView.scheduleBlockTick(pos, this, 1);
			} else {
				BlockState aboveState = world.getBlockState(pos.offset(growthDirection));

				if (aboveState.isOf(this) || aboveState.isOf(getPlant())) {
					return copyState(state, getPlant().getDefaultState());
				}
			}
		}

		if (direction == growthDirection && (neighborState.isOf(this) || neighborState.isOf(getPlant()))) {
			return copyState(state, getPlant().getDefaultState());
		}

		if (tickWater) {
			tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}

		return super.getStateForNeighborUpdate(
			state,
			world,
			tickView,
			pos,
			direction,
			neighborPos,
			neighborState,
			random
		);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		return chooseStemState(world.getBlockState(pos.offset(growthDirection)));
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	/**
	 * Выполняет рост стебля при использовании костяной муки:
	 * последовательно заполняет блоки в направлении роста, увеличивая возраст.
	 */
	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		BlockPos growPos = pos.offset(growthDirection);
		int age = Math.min(state.get(AGE) + 1, MAX_AGE);
		int growthLength = getGrowthLength(random);

		for (int step = 0; step < growthLength && chooseStemState(world.getBlockState(growPos)); step++) {
			world.setBlockState(growPos, state.with(AGE, age));
			growPos = growPos.offset(growthDirection);
			age = Math.min(age + 1, MAX_AGE);
		}
	}

	protected abstract int getGrowthLength(Random random);

	/**
	 * Определяет, подходит ли соседний блок для продолжения роста стебля.
	 * Обычно проверяет, является ли блок воздухом или водой.
	 */
	protected abstract boolean chooseStemState(BlockState state);

	@Override
	protected AbstractPlantStemBlock getStem() {
		return this;
	}
}
