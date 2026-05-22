package net.minecraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.enums.BambooLeaves;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

/**
 * Блок бамбука — быстрорастущее растение с ограниченной высотой {@link #MAX_BAMBOO_HEIGHT}.
 * <p>
 * Хранит три свойства состояния:
 * <ul>
 *   <li>{@link #AGE} — возраст стебля (0 = молодой, 1 = зрелый); влияет на толщину ствола.</li>
 *   <li>{@link #LEAVES} — тип листьев на данном сегменте (NONE / SMALL / LARGE).</li>
 *   <li>{@link #STAGE} — стадия роста (0 = растёт, 1 = достиг предела).</li>
 * </ul>
 * Рост происходит случайно при {@link #randomTick} и принудительно через костяную муку
 * ({@link Fertilizable}). При каждом новом сегменте пересчитываются листья двух нижних
 * сегментов, чтобы сохранить визуальную пирамиду листвы.
 */
public class BambooBlock extends Block implements Fertilizable {

	public static final MapCodec<BambooBlock> CODEC = createCodec(BambooBlock::new);
	private static final VoxelShape SMALL_LEAVES_SHAPE = Block.createColumnShape(6.0, 0.0, 16.0);
	private static final VoxelShape LARGE_LEAVES_SHAPE = Block.createColumnShape(10.0, 0.0, 16.0);
	private static final VoxelShape NO_LEAVES_SHAPE = Block.createColumnShape(3.0, 0.0, 16.0);
	/** Минимальный уровень освещения, необходимый для случайного роста. */
	private static final int MIN_GROWTH_LIGHT = 9;
	/** Вероятность 1/3 для случайного тика роста. */
	private static final int GROWTH_RANDOM_BOUND = 3;
	/** Высота, начиная с которой верхушка может получить статус STAGE_MATURE со случайностью. */
	private static final int MATURE_STAGE_HEIGHT_THRESHOLD = 11;
	/** Вероятность получить STAGE_MATURE при высоте >= {@link #MATURE_STAGE_HEIGHT_THRESHOLD}. */
	private static final float MATURE_STAGE_CHANCE = 0.25f;
	public static final IntProperty AGE = Properties.AGE_1;
	public static final EnumProperty<BambooLeaves> LEAVES = Properties.BAMBOO_LEAVES;
	public static final IntProperty STAGE = Properties.STAGE;
	public static final int MAX_BAMBOO_HEIGHT = 16;
	public static final int AGE_YOUNG = 0;
	public static final int AGE_MATURE = 1;
	public static final int STAGE_GROWING = 0;
	public static final int STAGE_MATURE = 1;

	@Override
	public MapCodec<BambooBlock> getCodec() {
		return CODEC;
	}

	public BambooBlock(AbstractBlock.Settings settings) {
		super(settings);
		setDefaultState(stateManager
			.getDefaultState()
			.with(AGE, AGE_YOUNG)
			.with(LEAVES, BambooLeaves.NONE)
			.with(STAGE, STAGE_GROWING)
		);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AGE, LEAVES, STAGE);
	}

	@Override
	protected boolean isTransparent(BlockState state) {
		return true;
	}

	@Override
	protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		VoxelShape shape = state.get(LEAVES) == BambooLeaves.LARGE ? LARGE_LEAVES_SHAPE : SMALL_LEAVES_SHAPE;
		return shape.offset(state.getModelOffset(pos));
	}

	@Override
	protected boolean canPathfindThrough(BlockState state, NavigationType type) {
		return false;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return NO_LEAVES_SHAPE.offset(state.getModelOffset(pos));
	}

	@Override
	protected boolean isShapeFullCube(BlockState state, BlockView world, BlockPos pos) {
		return false;
	}

	/**
	 * Определяет начальное состояние при размещении бамбука игроком.
	 * <p>
	 * Логика выбора возраста:
	 * <ul>
	 *   <li>Под блоком — росток бамбука: возраст 0.</li>
	 *   <li>Под блоком — взрослый бамбук: наследует его возраст (≥1 → 1).</li>
	 *   <li>Иначе: смотрит на блок сверху; если там бамбук — берёт его возраст,
	 *       иначе размещает росток.</li>
	 * </ul>
	 */
	@Override
	public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
		if (ctx.getWorld().getFluidState(ctx.getBlockPos()).isEmpty() == false) {
			return null;
		}

		BlockState below = ctx.getWorld().getBlockState(ctx.getBlockPos().down());

		if (below.isIn(BlockTags.BAMBOO_PLANTABLE_ON) == false) {
			return null;
		}

		if (below.isOf(Blocks.BAMBOO_SAPLING)) {
			return getDefaultState().with(AGE, AGE_YOUNG);
		}

		if (below.isOf(Blocks.BAMBOO)) {
			int inheritedAge = below.get(AGE) > 0 ? AGE_MATURE : AGE_YOUNG;
			return getDefaultState().with(AGE, inheritedAge);
		}

		BlockState above = ctx.getWorld().getBlockState(ctx.getBlockPos().up());
		return above.isOf(Blocks.BAMBOO)
			? getDefaultState().with(AGE, above.get(AGE))
			: Blocks.BAMBOO_SAPLING.getDefaultState();
	}

	@Override
	protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.canPlaceAt(world, pos) == false) {
			world.breakBlock(pos, true);
		}
	}

	@Override
	protected boolean hasRandomTicks(BlockState state) {
		return state.get(STAGE) == STAGE_GROWING;
	}

	@Override
	protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		if (state.get(STAGE) != STAGE_GROWING) {
			return;
		}

		if (random.nextInt(GROWTH_RANDOM_BOUND) != 0) {
			return;
		}

		BlockPos above = pos.up();

		if (world.isAir(above) == false || world.getBaseLightLevel(above, 0) < MIN_GROWTH_LIGHT) {
			return;
		}

		int totalHeight = countBambooBelow(world, pos) + 1;

		if (totalHeight < MAX_BAMBOO_HEIGHT) {
			updateLeaves(state, world, pos, random, totalHeight);
		}
	}

	@Override
	protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
		return world.getBlockState(pos.down()).isIn(BlockTags.BAMBOO_PLANTABLE_ON);
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
		if (state.canPlaceAt(world, pos) == false) {
			tickView.scheduleBlockTick(pos, this, 1);
		}

		return direction == Direction.UP
			&& neighborState.isOf(Blocks.BAMBOO)
			&& neighborState.get(AGE) > state.get(AGE)
			? state.cycle(AGE)
			: super.getStateForNeighborUpdate(
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
	public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
		int aboveCount = countBambooAbove(world, pos);
		int belowCount = countBambooBelow(world, pos);
		return aboveCount + belowCount + 1 < MAX_BAMBOO_HEIGHT
			&& world.getBlockState(pos.up(aboveCount)).get(STAGE) != STAGE_MATURE;
	}

	@Override
	public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
		return true;
	}

	/**
	 * Принудительно наращивает бамбук на 1–2 сегмента вверх (костяная мука).
	 * Каждый новый сегмент добавляется поверх текущей верхушки, пока не достигнут
	 * {@link #MAX_BAMBOO_HEIGHT} или верхушка не помечена как {@link #STAGE_MATURE}.
	 */
	@Override
	public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
		int aboveCount = countBambooAbove(world, pos);
		int belowCount = countBambooBelow(world, pos);
		int totalHeight = aboveCount + belowCount + 1;
		int growthSteps = 1 + random.nextInt(2);

		for (int step = 0; step < growthSteps; step++) {
			BlockPos topPos = pos.up(aboveCount);
			BlockState topState = world.getBlockState(topPos);

			if (totalHeight >= MAX_BAMBOO_HEIGHT
				|| topState.get(STAGE) == STAGE_MATURE
				|| world.isAir(topPos.up()) == false
			) {
				return;
			}

			updateLeaves(topState, world, topPos, random, totalHeight);
			aboveCount++;
			totalHeight++;
		}
	}

	/**
	 * Размещает новый сегмент бамбука над {@code pos} и пересчитывает листья
	 * двух нижних сегментов, поддерживая визуальную пирамиду листвы:
	 * верхний сегмент получает LARGE, средний — SMALL, нижний — NONE.
	 *
	 * @param state текущее состояние сегмента, над которым растём
	 * @param world мир
	 * @param pos позиция текущего верхнего сегмента
	 * @param random генератор случайных чисел
	 * @param height суммарная высота колонны бамбука после добавления нового сегмента
	 */
	protected void updateLeaves(BlockState state, World world, BlockPos pos, Random random, int height) {
		BlockState oneBelow = world.getBlockState(pos.down());
		BlockPos twoBelowPos = pos.down(2);
		BlockState twoBelow = world.getBlockState(twoBelowPos);

		BambooLeaves newLeaves = BambooLeaves.NONE;

		if (height >= 1) {
			if (oneBelow.isOf(Blocks.BAMBOO) == false || oneBelow.get(LEAVES) == BambooLeaves.NONE) {
				newLeaves = BambooLeaves.SMALL;
			} else {
				newLeaves = BambooLeaves.LARGE;

				if (twoBelow.isOf(Blocks.BAMBOO)) {
					world.setBlockState(pos.down(), oneBelow.with(LEAVES, BambooLeaves.SMALL), 3);
					world.setBlockState(twoBelowPos, twoBelow.with(LEAVES, BambooLeaves.NONE), 3);
				}
			}
		}

		// Возраст нового сегмента: зрелый, если текущий зрелый или под ним нет бамбука
		int newAge = (state.get(AGE) == AGE_MATURE || twoBelow.isOf(Blocks.BAMBOO)) ? AGE_MATURE : AGE_YOUNG;
		// Верхушка помечается как STAGE_MATURE при достижении предельной высоты или случайно при высоте ≥ 11
		int newStage = (height >= MATURE_STAGE_HEIGHT_THRESHOLD && random.nextFloat() < MATURE_STAGE_CHANCE)
			|| height == MAX_BAMBOO_HEIGHT - 1
			? STAGE_MATURE
			: STAGE_GROWING;

		world.setBlockState(
			pos.up(),
			getDefaultState().with(AGE, newAge).with(LEAVES, newLeaves).with(STAGE, newStage),
			3
		);
	}

	/**
	 * Считает количество непрерывных блоков бамбука строго выше {@code pos}.
	 *
	 * @param world мир
	 * @param pos начальная позиция (не включается в счёт)
	 * @return количество сегментов бамбука выше
	 */
	protected int countBambooAbove(BlockView world, BlockPos pos) {
		int count = 0;

		while (count < MAX_BAMBOO_HEIGHT && world.getBlockState(pos.up(count + 1)).isOf(Blocks.BAMBOO)) {
			count++;
		}

		return count;
	}

	/**
	 * Считает количество непрерывных блоков бамбука строго ниже {@code pos}.
	 *
	 * @param world мир
	 * @param pos начальная позиция (не включается в счёт)
	 * @return количество сегментов бамбука ниже
	 */
	protected int countBambooBelow(BlockView world, BlockPos pos) {
		int count = 0;

		while (count < MAX_BAMBOO_HEIGHT && world.getBlockState(pos.down(count + 1)).isOf(Blocks.BAMBOO)) {
			count++;
		}

		return count;
	}
}
