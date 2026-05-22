package net.minecraft.fluid;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.block.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Базовый класс для всех текучих жидкостей (вода, лава).
 *
 * <p>Реализует полную физику растекания: вычисление вектора скорости потока,
 * алгоритм поиска кратчайшего пути вниз ({@link #getMinFlowDownDistance}),
 * распространение по горизонтали ({@link #flowToSides}) и вниз ({@link #tryFlow}).
 * Кэширует результаты проверки проходимости граней в потокобезопасном
 * {@link ThreadLocal}-кэше {@link #FLOW_CACHE} для оптимизации.</p>
 */
public abstract class FlowableFluid extends Fluid {

	public static final BooleanProperty FALLING = Properties.FALLING;
	public static final IntProperty LEVEL = Properties.LEVEL_1_8;

	/** Максимальный размер кэша проходимости граней на поток. */
	private static final int FLOW_CACHE_SIZE = 200;

	/**
	 * Потокобезопасный кэш результатов {@link #receivesFlow}.
	 * Ключ — группа соседних состояний блоков и направление, значение — байт (0/1/127=miss).
	 * Ограничен {@link #FLOW_CACHE_SIZE} записями с вытеснением по LRU.
	 */
	private static final ThreadLocal<Object2ByteLinkedOpenHashMap<NeighborGroup>> FLOW_CACHE =
		ThreadLocal.withInitial(() -> {
			Object2ByteLinkedOpenHashMap<NeighborGroup> cache =
				new Object2ByteLinkedOpenHashMap<>(FLOW_CACHE_SIZE) {
					@Override
					protected void rehash(int newSize) {
					}
				};
			cache.defaultReturnValue((byte) 127);
			return cache;
		});

	private final Map<FluidState, VoxelShape> shapeCache = Maps.newIdentityHashMap();

	@Override
	protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
		builder.add(FALLING);
	}

	/**
	 * Вычисляет вектор скорости потока жидкости в позиции {@code pos}.
	 *
	 * <p>Алгоритм суммирует горизонтальные составляющие разницы высот со всеми
	 * соседними блоками жидкости того же типа. Если жидкость падает и поток
	 * заблокирован с боков, добавляется сильная нисходящая составляющая (-6 по Y),
	 * имитирующая «прилипание» к стене водопада.</p>
	 */
	@Override
	public Vec3d getVelocity(BlockView world, BlockPos pos, FluidState state) {
		double velocityX = 0.0;
		double velocityZ = 0.0;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (Direction direction : Direction.Type.HORIZONTAL) {
			mutable.set(pos, direction);
			FluidState neighborFluid = world.getFluidState(mutable);

			if (!isEmptyOrThis(neighborFluid)) {
				continue;
			}

			float neighborHeight = neighborFluid.getHeight();
			float heightDiff = 0.0F;

			if (neighborHeight == 0.0F) {
				if (!world.getBlockState(mutable).blocksMovement()) {
					BlockPos below = mutable.down();
					FluidState belowFluid = world.getFluidState(below);
					if (isEmptyOrThis(belowFluid)) {
						neighborHeight = belowFluid.getHeight();
						if (neighborHeight > 0.0F) {
							// Жидкость падает вниз — учитываем разницу с «дном» водопада
							heightDiff = state.getHeight() - (neighborHeight - 0.8888889F);
						}
					}
				}
			} else if (neighborHeight > 0.0F) {
				heightDiff = state.getHeight() - neighborHeight;
			}

			if (heightDiff != 0.0F) {
				velocityX += direction.getOffsetX() * heightDiff;
				velocityZ += direction.getOffsetZ() * heightDiff;
			}
		}

		Vec3d velocity = new Vec3d(velocityX, 0.0, velocityZ);

		if (state.get(FALLING)) {
			for (Direction direction : Direction.Type.HORIZONTAL) {
				mutable.set(pos, direction);
				if (isFlowBlocked(world, mutable, direction) || isFlowBlocked(world, mutable.up(), direction)) {
					velocity = velocity.normalize().add(0.0, -6.0, 0.0);
					break;
				}
			}
		}

		return velocity.normalize();
	}

	private boolean isEmptyOrThis(FluidState state) {
		return state.isEmpty() || state.getFluid().matchesType(this);
	}

	/**
	 * Проверяет, заблокирован ли поток жидкости через грань {@code direction} в позиции {@code pos}.
	 * Лёд не блокирует поток, несмотря на то что является твёрдым блоком.
	 */
	protected boolean isFlowBlocked(BlockView world, BlockPos pos, Direction direction) {
		BlockState blockState = world.getBlockState(pos);
		FluidState fluidState = world.getFluidState(pos);

		if (fluidState.getFluid().matchesType(this)) {
			return false;
		}

		if (direction == Direction.UP) {
			return true;
		}

		return blockState.getBlock() instanceof IceBlock
			? false
			: blockState.isSideSolidFullSquare(world, pos, direction);
	}

	/**
	 * Пытается распространить жидкость вниз, а при невозможности — по горизонтали.
	 *
	 * <p>Если ниже есть свободное место и жидкость может туда вытечь, она течёт вниз.
	 * При наличии ≥3 соседних источников дополнительно вызывается {@link #flowToSides}.
	 * Если вниз течь нельзя, жидкость распространяется только по горизонтали.</p>
	 */
	protected void tryFlow(ServerWorld world, BlockPos fluidPos, BlockState blockState, FluidState fluidState) {
		if (fluidState.isEmpty()) {
			return;
		}

		BlockPos below = fluidPos.down();
		BlockState belowBlock = world.getBlockState(below);
		FluidState belowFluid = belowBlock.getFluidState();

		if (!canFlowThrough(world, fluidPos, blockState, Direction.DOWN, below, belowBlock, belowFluid)) {
			if (fluidState.isStill() || !canFlowDownTo(world, fluidPos, blockState, below, belowBlock)) {
				flowToSides(world, fluidPos, fluidState, blockState);
			}

			return;
		}

		FluidState updatedBelow = getUpdatedState(world, below, belowBlock);
		Fluid updatedFluid = updatedBelow.getFluid();

		if (belowFluid.canBeReplacedWith(world, below, updatedFluid, Direction.DOWN)
			&& canFillWithFluid(world, below, belowBlock, updatedFluid)
		) {
			flow(world, below, belowBlock, Direction.DOWN, updatedBelow);

			if (countNeighboringSources(world, fluidPos) >= 3) {
				flowToSides(world, fluidPos, fluidState, blockState);
			}

			return;
		}

		if (fluidState.isStill() || !canFlowDownTo(world, fluidPos, blockState, below, belowBlock)) {
			flowToSides(world, fluidPos, fluidState, blockState);
		}
	}

	/**
	 * Распространяет жидкость по горизонтальным направлениям.
	 * Уровень уменьшается на {@link #getLevelDecreasePerBlock} за каждый блок.
	 * Если жидкость падает, горизонтальный уровень принудительно равен 7.
	 */
	private void flowToSides(ServerWorld world, BlockPos pos, FluidState fluidState, BlockState blockState) {
		int spreadLevel = fluidState.get(FALLING) ? 7 : fluidState.getLevel() - getLevelDecreasePerBlock(world);

		if (spreadLevel <= 0) {
			return;
		}

		Map<Direction, FluidState> spread = getSpread(world, pos, blockState);

		for (Entry<Direction, FluidState> entry : spread.entrySet()) {
			Direction direction = entry.getKey();
			FluidState spreadState = entry.getValue();
			BlockPos target = pos.offset(direction);
			flow(world, target, world.getBlockState(target), direction, spreadState);
		}
	}

	/**
	 * Вычисляет обновлённое состояние жидкости для позиции {@code pos} на основе соседей.
	 *
	 * <p>Логика:
	 * <ol>
	 *   <li>Если сверху течёт та же жидкость — возвращает падающее состояние уровня 8.</li>
	 *   <li>Если ≥2 соседних источника и мир бесконечный — создаёт новый источник.</li>
	 *   <li>Иначе — уменьшает максимальный уровень соседей на {@link #getLevelDecreasePerBlock}.</li>
	 * </ol>
	 * </p>
	 */
	protected FluidState getUpdatedState(ServerWorld world, BlockPos pos, BlockState state) {
		int maxNeighborLevel = 0;
		int stillNeighborCount = 0;
		BlockPos.Mutable mutable = new BlockPos.Mutable();

		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos neighborPos = mutable.set(pos, direction);
			BlockState neighborBlock = world.getBlockState(neighborPos);
			FluidState neighborFluid = neighborBlock.getFluidState();

			if (!neighborFluid.getFluid().matchesType(this)) {
				continue;
			}

			if (!receivesFlow(direction, world, pos, state, neighborPos, neighborBlock)) {
				continue;
			}

			if (neighborFluid.isStill()) {
				stillNeighborCount++;
			}

			maxNeighborLevel = Math.max(maxNeighborLevel, neighborFluid.getLevel());
		}

		if (stillNeighborCount >= 2 && isInfinite(world)) {
			BlockState belowBlock = world.getBlockState(mutable.set(pos, Direction.DOWN));
			FluidState belowFluid = belowBlock.getFluidState();

			if (belowBlock.isSolid() || isMatchingAndStill(belowFluid)) {
				return getStill(false);
			}
		}

		BlockPos above = mutable.set(pos, Direction.UP);
		BlockState aboveBlock = world.getBlockState(above);
		FluidState aboveFluid = aboveBlock.getFluidState();

		if (!aboveFluid.isEmpty()
			&& aboveFluid.getFluid().matchesType(this)
			&& receivesFlow(Direction.UP, world, pos, state, above, aboveBlock)
		) {
			return getFlowing(8, true);
		}

		int resultLevel = maxNeighborLevel - getLevelDecreasePerBlock(world);
		return resultLevel <= 0 ? Fluids.EMPTY.getDefaultState() : getFlowing(resultLevel, false);
	}

	/**
	 * Проверяет, может ли жидкость перетечь из {@code fromPos} в {@code pos} через грань {@code face}.
	 *
	 * <p>Использует потокобезопасный LRU-кэш {@link #FLOW_CACHE} для блоков без динамических границ.
	 * Кэш не применяется для блоков с {@code hasDynamicBounds() == true}, так как их форма
	 * может меняться в зависимости от контекста.</p>
	 */
	private static boolean receivesFlow(
		Direction face,
		BlockView world,
		BlockPos pos,
		BlockState state,
		BlockPos fromPos,
		BlockState fromState
	) {
		if (SharedConstants.DISABLE_LIQUID_SPREADING) {
			return false;
		}

		if (SharedConstants.ONLY_GENERATE_HALF_THE_WORLD && fromPos.getZ() < 0) {
			return false;
		}

		VoxelShape fromShape = fromState.getCollisionShape(world, fromPos);
		if (fromShape == VoxelShapes.fullCube()) {
			return false;
		}

		VoxelShape selfShape = state.getCollisionShape(world, pos);
		if (selfShape == VoxelShapes.fullCube()) {
			return false;
		}

		if (selfShape == VoxelShapes.empty() && fromShape == VoxelShapes.empty()) {
			return true;
		}

		// Кэш применяется только для блоков со статическими границами
		Object2ByteLinkedOpenHashMap<NeighborGroup> cache =
			(!state.getBlock().hasDynamicBounds() && !fromState.getBlock().hasDynamicBounds())
				? FLOW_CACHE.get()
				: null;

		NeighborGroup key = null;

		if (cache != null) {
			key = new NeighborGroup(state, fromState, face);
			byte cached = cache.getAndMoveToFirst(key);
			if (cached != 127) {
				return cached != 0;
			}
		}

		boolean canFlow = !VoxelShapes.adjacentSidesCoverSquare(selfShape, fromShape, face);

		if (cache != null) {
			if (cache.size() == FLOW_CACHE_SIZE) {
				cache.removeLastByte();
			}

			cache.putAndMoveToFirst(key, (byte) (canFlow ? 1 : 0));
		}

		return canFlow;
	}

	/** @return жидкость в текущем (flowing) состоянии. */
	public abstract Fluid getFlowing();

	/** @return состояние текущей жидкости с заданным уровнем и флагом падения. */
	public FluidState getFlowing(int level, boolean falling) {
		return getFlowing().getDefaultState().with(LEVEL, level).with(FALLING, falling);
	}

	/** @return жидкость в стоячем (still/source) состоянии. */
	public abstract Fluid getStill();

	/** @return состояние стоячей жидкости с заданным флагом падения. */
	public FluidState getStill(boolean falling) {
		return getStill().getDefaultState().with(FALLING, falling);
	}

	/**
	 * @return {@code true}, если данная жидкость может создавать бесконечные источники
	 *         (например, вода при включённом правиле {@code waterSourceConversion}).
	 */
	protected abstract boolean isInfinite(ServerWorld world);

	/**
	 * Выполняет фактическое размещение жидкости в позиции {@code pos}.
	 * Если блок реализует {@link FluidFillable}, делегирует заполнение ему.
	 * Иначе разрушает существующий блок и ставит блок жидкости.
	 */
	protected void flow(WorldAccess world, BlockPos pos, BlockState state, Direction direction, FluidState fluidState) {
		if (state.getBlock() instanceof FluidFillable fluidFillable) {
			fluidFillable.tryFillWithFluid(world, pos, state, fluidState);
			return;
		}

		if (!state.isAir()) {
			beforeBreakingBlock(world, pos, state);
		}

		// Флаг 3 = BLOCK_UPDATE | SEND_TO_CLIENTS
		world.setBlockState(pos, fluidState.getBlockState(), 3);
	}

	/** Вызывается перед разрушением блока при вытеснении его жидкостью. */
	protected abstract void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state);

	/**
	 * Рекурсивно ищет минимальное расстояние до ближайшего «провала» вниз
	 * при горизонтальном распространении жидкости.
	 *
	 * <p>Используется в {@link #getSpread} для выбора направлений с наименьшим
	 * расстоянием до падения — жидкость предпочитает течь туда, где быстрее упадёт вниз.</p>
	 *
	 * @param depth текущая глубина рекурсии (начинается с 1)
	 * @param excludeDirection направление, откуда пришли (исключается из обхода)
	 * @return минимальное расстояние до провала или 1000, если провала нет в радиусе
	 */
	protected int getMinFlowDownDistance(
		WorldView world,
		BlockPos pos,
		int depth,
		Direction excludeDirection,
		BlockState state,
		SpreadCache spreadCache
	) {
		int minDistance = 1000;

		for (Direction direction : Direction.Type.HORIZONTAL) {
			if (direction == excludeDirection) {
				continue;
			}

			BlockPos neighbor = pos.offset(direction);
			BlockState neighborBlock = spreadCache.getBlockState(neighbor);
			FluidState neighborFluid = neighborBlock.getFluidState();

			if (!canFlowThrough(world, getFlowing(), pos, state, direction, neighbor, neighborBlock, neighborFluid)) {
				continue;
			}

			if (spreadCache.canFlowDownTo(neighbor)) {
				return depth;
			}

			if (depth < getMaxFlowDistance(world)) {
				int subDistance = getMinFlowDownDistance(
					world, neighbor, depth + 1, direction.getOpposite(), neighborBlock, spreadCache
				);
				if (subDistance < minDistance) {
					minDistance = subDistance;
				}
			}
		}

		return minDistance;
	}

	boolean canFlowDownTo(BlockView world, BlockPos pos, BlockState state, BlockPos fromPos, BlockState fromState) {
		if (!receivesFlow(Direction.DOWN, world, pos, state, fromPos, fromState)) {
			return false;
		}

		return fromState.getFluidState().getFluid().matchesType(this)
			|| canFill(world, fromPos, fromState, getFlowing());
	}

	private boolean canFlowThrough(
		BlockView world,
		Fluid fluid,
		BlockPos pos,
		BlockState state,
		Direction face,
		BlockPos fromPos,
		BlockState fromState,
		FluidState fluidState
	) {
		return canFlowThrough(world, pos, state, face, fromPos, fromState, fluidState)
			&& canFillWithFluid(world, fromPos, fromState, fluid);
	}

	private boolean canFlowThrough(
		BlockView world,
		BlockPos pos,
		BlockState state,
		Direction face,
		BlockPos fromPos,
		BlockState fromState,
		FluidState fluidState
	) {
		return !isMatchingAndStill(fluidState)
			&& canFill(fromState)
			&& receivesFlow(face, world, pos, state, fromPos, fromState);
	}

	private boolean isMatchingAndStill(FluidState state) {
		return state.getFluid().matchesType(this) && state.isStill();
	}

	/** @return максимальное расстояние горизонтального распространения жидкости в блоках. */
	protected abstract int getMaxFlowDistance(WorldView world);

	/** @return количество соседних стоячих источников той же жидкости по горизонтали. */
	private int countNeighboringSources(WorldView world, BlockPos pos) {
		int count = 0;

		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos neighbor = pos.offset(direction);
			FluidState neighborFluid = world.getFluidState(neighbor);

			if (isMatchingAndStill(neighborFluid)) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Вычисляет карту направлений, в которые жидкость должна растечься.
	 *
	 * <p>Для каждого горизонтального направления определяет минимальное расстояние
	 * до «провала» вниз. Жидкость течёт только в направления с минимальным расстоянием,
	 * имитируя поведение реальной воды, которая ищет кратчайший путь вниз.</p>
	 */
	protected Map<Direction, FluidState> getSpread(ServerWorld world, BlockPos pos, BlockState state) {
		int minDownDistance = 1000;
		Map<Direction, FluidState> result = Maps.newEnumMap(Direction.class);
		SpreadCache spreadCache = null;

		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos neighbor = pos.offset(direction);
			BlockState neighborBlock = world.getBlockState(neighbor);
			FluidState neighborFluid = neighborBlock.getFluidState();

			if (!canFlowThrough(world, pos, state, direction, neighbor, neighborBlock, neighborFluid)) {
				continue;
			}

			FluidState updatedState = getUpdatedState(world, neighbor, neighborBlock);

			if (!canFillWithFluid(world, neighbor, neighborBlock, updatedState.getFluid())) {
				continue;
			}

			if (spreadCache == null) {
				spreadCache = new SpreadCache(world, pos);
			}

			int downDistance = spreadCache.canFlowDownTo(neighbor)
				? 0
				: getMinFlowDownDistance(world, neighbor, 1, direction.getOpposite(), neighborBlock, spreadCache);

			if (downDistance < minDownDistance) {
				result.clear();
				minDownDistance = downDistance;
			}

			if (downDistance <= minDownDistance
				&& neighborFluid.canBeReplacedWith(world, neighbor, updatedState.getFluid(), direction)
			) {
				result.put(direction, updatedState);
			}
		}

		return result;
	}

	/**
	 * Проверяет, может ли блок быть заполнен жидкостью (без учёта конкретного типа жидкости).
	 * Исключает двери, знаки, лестницы, сахарный тростник, порталы и т.д.
	 */
	private static boolean canFill(BlockState state) {
		Block block = state.getBlock();

		if (block instanceof FluidFillable) {
			return true;
		}

		if (state.blocksMovement()) {
			return false;
		}

		return !(block instanceof DoorBlock)
			&& !state.isIn(BlockTags.SIGNS)
			&& !state.isOf(Blocks.LADDER)
			&& !state.isOf(Blocks.SUGAR_CANE)
			&& !state.isOf(Blocks.BUBBLE_COLUMN)
			&& !state.isOf(Blocks.NETHER_PORTAL)
			&& !state.isOf(Blocks.END_PORTAL)
			&& !state.isOf(Blocks.END_GATEWAY)
			&& !state.isOf(Blocks.STRUCTURE_VOID);
	}

	private static boolean canFill(BlockView world, BlockPos pos, BlockState state, Fluid fluid) {
		return canFill(state) && canFillWithFluid(world, pos, state, fluid);
	}

	private static boolean canFillWithFluid(BlockView world, BlockPos pos, BlockState state, Fluid fluid) {
		return state.getBlock() instanceof FluidFillable fluidFillable
			? fluidFillable.canFillWithFluid(null, world, pos, state, fluid)
			: true;
	}

	/** @return на сколько единиц уменьшается уровень жидкости за каждый горизонтальный блок. */
	protected abstract int getLevelDecreasePerBlock(WorldView world);

	/**
	 * @return задержка следующего тика жидкости в игровых тиках.
	 *         По умолчанию равна {@link #getTickRate}. Лава переопределяет для замедления при подъёме.
	 */
	protected int getNextTickDelay(World world, BlockPos pos, FluidState oldState, FluidState newState) {
		return getTickRate(world);
	}

	/**
	 * Обрабатывает запланированный тик жидкости.
	 *
	 * <p>Пересчитывает состояние жидкости. Если оно изменилось — обновляет блок и планирует
	 * следующий тик. Затем вызывает {@link #tryFlow} для распространения.</p>
	 */
	@Override
	public void onScheduledTick(ServerWorld world, BlockPos pos, BlockState blockState, FluidState fluidState) {
		if (!fluidState.isStill()) {
			FluidState updatedFluid = getUpdatedState(world, pos, world.getBlockState(pos));
			int tickDelay = getNextTickDelay(world, pos, fluidState, updatedFluid);

			if (updatedFluid.isEmpty()) {
				fluidState = updatedFluid;
				blockState = Blocks.AIR.getDefaultState();
				world.setBlockState(pos, blockState, 3);
			} else if (updatedFluid != fluidState) {
				fluidState = updatedFluid;
				blockState = updatedFluid.getBlockState();
				world.setBlockState(pos, blockState, 3);
				world.scheduleFluidTick(pos, updatedFluid.getFluid(), tickDelay);
			}
		}

		tryFlow(world, pos, blockState, fluidState);
	}

	/**
	 * Конвертирует уровень жидкости в числовое значение для {@link FluidBlock#LEVEL}.
	 * Стоячая жидкость → 0, текущая → (8 - level), падающая → (8 - level) + 8.
	 */
	protected static int getBlockStateLevel(FluidState state) {
		return state.isStill()
			? 0
			: 8 - Math.min(state.getLevel(), 8) + (state.get(FALLING) ? 8 : 0);
	}

	private static boolean isFluidAboveEqual(FluidState state, BlockView world, BlockPos pos) {
		return state.getFluid().matchesType(world.getFluidState(pos.up()).getFluid());
	}

	@Override
	public float getHeight(FluidState state, BlockView world, BlockPos pos) {
		return isFluidAboveEqual(state, world, pos) ? 1.0F : state.getHeight();
	}

	@Override
	public float getHeight(FluidState state) {
		return state.getLevel() / 9.0F;
	}

	@Override
	public abstract int getLevel(FluidState state);

	/**
	 * Возвращает форму жидкости для рендеринга и коллизий.
	 * Если над блоком та же жидкость и уровень максимальный — возвращает полный куб.
	 * Иначе — вычисляет форму по высоте и кэширует результат.
	 */
	@Override
	public VoxelShape getShape(FluidState state, BlockView world, BlockPos pos) {
		return state.getLevel() == FluidState.MAX_AMOUNT && isFluidAboveEqual(state, world, pos)
			? VoxelShapes.fullCube()
			: shapeCache.computeIfAbsent(
				state,
				s -> VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, s.getHeight(world, pos), 1.0)
			);
	}

	// -------------------------------------------------------------------------
	// Вложенные классы
	// -------------------------------------------------------------------------

	/**
	 * Ключ кэша {@link #FLOW_CACHE}: тройка (состояние блока, состояние соседа, направление).
	 * Использует идентичность объектов (==) вместо equals для максимальной скорости.
	 */
	record NeighborGroup(BlockState self, BlockState other, Direction facing) {

		@Override
		public boolean equals(Object obj) {
			return obj instanceof NeighborGroup other
				&& self == other.self
				&& this.other == other.other
				&& facing == other.facing;
		}

		@Override
		public int hashCode() {
			int hash = System.identityHashCode(self);
			hash = 31 * hash + System.identityHashCode(other);
			return 31 * hash + facing.hashCode();
		}
	}

	/**
		* Кэш состояний блоков и возможности стекания вниз для одного цикла распространения жидкости.
		*
		* <p>Позиции кодируются в {@code short} относительно стартовой позиции,
		* что позволяет избежать создания лишних объектов {@link BlockPos} при обходе соседей.</p>
		*/
	protected class SpreadCache {

		private final BlockView world;
		private final BlockPos startPos;
		private final Short2ObjectMap<BlockState> stateCache = new Short2ObjectOpenHashMap<>();
		private final Short2BooleanMap flowDownCache = new Short2BooleanOpenHashMap();

		SpreadCache(BlockView world, BlockPos startPos) {
			this.world = world;
			this.startPos = startPos;
		}

		public BlockState getBlockState(BlockPos pos) {
			return getBlockState(pos, pack(pos));
		}

		private BlockState getBlockState(BlockPos pos, short packed) {
			return stateCache.computeIfAbsent(packed, key -> world.getBlockState(pos));
		}

		public boolean canFlowDownTo(BlockPos pos) {
			return flowDownCache.computeIfAbsent(pack(pos), packed -> {
				BlockState blockState = getBlockState(pos, packed);
				BlockPos below = pos.down();
				BlockState belowState = world.getBlockState(below);
				return FlowableFluid.this.canFlowDownTo(world, pos, blockState, below, belowState);
			});
		}

		/**
		 * Упаковывает смещение позиции относительно стартовой в {@code short}.
		 * Диапазон смещений: [-128, 127] по X и Z.
		 */
		private short pack(BlockPos pos) {
			int dx = pos.getX() - startPos.getX();
			int dz = pos.getZ() - startPos.getZ();
			return (short) ((dx + 128 & 0xFF) << 8 | dz + 128 & 0xFF);
		}
	}
}
