package net.minecraft.block.piston;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

/**
 * Вычисляет, какие блоки должны быть перемещены или уничтожены при срабатывании поршня.
 * <p>
 * Алгоритм обходит цепочку слипающихся блоков (слизь, мёд) рекурсивно через
 * {@link #tryMove} и {@link #tryMoveAdjacentBlock}, накапливая результаты
 * в {@link #movedBlocks} и {@link #brokenBlocks}.
 */
public class PistonHandler {

	public static final int MAX_MOVABLE_BLOCKS = 12;

	private final World world;
	private final BlockPos posFrom;
	private final boolean retracted;
	private final BlockPos posTo;
	private final Direction motionDirection;
	private final List<BlockPos> movedBlocks = Lists.newArrayList();
	private final List<BlockPos> brokenBlocks = Lists.newArrayList();
	private final Direction pistonDirection;

	public PistonHandler(World world, BlockPos pos, Direction dir, boolean retracted) {
		this.world = world;
		this.posFrom = pos;
		this.pistonDirection = dir;
		this.retracted = retracted;

		if (retracted) {
			this.motionDirection = dir;
			this.posTo = pos.offset(dir);
		} else {
			this.motionDirection = dir.getOpposite();
			this.posTo = pos.offset(dir, 2);
		}
	}

	/**
	 * Вычисляет список блоков для перемещения и уничтожения при выдвижении/втягивании поршня.
	 * <p>
	 * Сначала проверяет целевой блок на перемещаемость, затем рекурсивно обходит
	 * все слипающиеся соседние блоки. Возвращает {@code false}, если движение невозможно
	 * (например, блок заблокирован или превышен лимит {@link #MAX_MOVABLE_BLOCKS}).
	 *
	 * @return {@code true} если движение поршня возможно
	 */
	public boolean calculatePush() {
		movedBlocks.clear();
		brokenBlocks.clear();

		BlockState targetState = world.getBlockState(posTo);

		if (PistonBlock.isMovable(targetState, world, posTo, motionDirection, false, pistonDirection) == false) {
			if (retracted && targetState.getPistonBehavior() == PistonBehavior.DESTROY) {
				brokenBlocks.add(posTo);
				return true;
			}

			return false;
		}

		if (tryMove(posTo, motionDirection) == false) {
			return false;
		}

		for (int index = 0; index < movedBlocks.size(); index++) {
			BlockPos movedPos = movedBlocks.get(index);

			if (isBlockSticky(world.getBlockState(movedPos)) && tryMoveAdjacentBlock(movedPos) == false) {
				return false;
			}
		}

		return true;
	}

	public Direction getMotionDirection() {
		return motionDirection;
	}

	public List<BlockPos> getMovedBlocks() {
		return movedBlocks;
	}

	public List<BlockPos> getBrokenBlocks() {
		return brokenBlocks;
	}

	private static boolean isBlockSticky(BlockState state) {
		return state.isOf(Blocks.SLIME_BLOCK) || state.isOf(Blocks.HONEY_BLOCK);
	}

	/**
	 * Проверяет, должны ли два соседних блока «слипаться» при движении поршня.
	 * <p>
	 * Слизь и мёд не слипаются друг с другом — это намеренное игровое правило,
	 * позволяющее строить механизмы с раздельным движением.
	 */
	private static boolean isAdjacentBlockStuck(BlockState state, BlockState adjacentState) {
		if (state.isOf(Blocks.HONEY_BLOCK) && adjacentState.isOf(Blocks.SLIME_BLOCK)) {
			return false;
		}

		if (state.isOf(Blocks.SLIME_BLOCK) && adjacentState.isOf(Blocks.HONEY_BLOCK)) {
			return false;
		}

		return isBlockSticky(state) || isBlockSticky(adjacentState);
	}

	/**
	 * Рекурсивно пытается включить блок по позиции {@code pos} в список перемещаемых.
	 * <p>
	 * Обходит цепочку слипающихся блоков в направлении, противоположном движению,
	 * затем проверяет блоки по направлению движения до первого воздуха или разрушаемого блока.
	 *
	 * @param pos позиция блока для проверки
	 * @param dir направление, из которого пришёл запрос на перемещение
	 * @return {@code true} если блок и вся его цепочка могут быть перемещены
	 */
	private boolean tryMove(BlockPos pos, Direction dir) {
		BlockState blockState = world.getBlockState(pos);

		if (blockState.isAir()) {
			return true;
		}

		if (PistonBlock.isMovable(blockState, world, pos, motionDirection, false, dir) == false) {
			return true;
		}

		if (pos.equals(posFrom)) {
			return true;
		}

		if (movedBlocks.contains(pos)) {
			return true;
		}

		int chainLength = 1;

		if (chainLength + movedBlocks.size() > MAX_MOVABLE_BLOCKS) {
			return false;
		}

		// Обходим цепочку слипающихся блоков в направлении, противоположном движению
		while (isBlockSticky(blockState)) {
			BlockPos behindPos = pos.offset(motionDirection.getOpposite(), chainLength);
			BlockState prevState = blockState;
			blockState = world.getBlockState(behindPos);

			if (blockState.isAir()
				|| isAdjacentBlockStuck(prevState, blockState) == false
				|| PistonBlock.isMovable(blockState, world, behindPos, motionDirection, false, motionDirection.getOpposite()) == false
				|| behindPos.equals(posFrom)
			) {
				break;
			}

			if (++chainLength + movedBlocks.size() > MAX_MOVABLE_BLOCKS) {
				return false;
			}
		}

		int addedCount = 0;

		for (int offset = chainLength - 1; offset >= 0; offset--) {
			movedBlocks.add(pos.offset(motionDirection.getOpposite(), offset));
			addedCount++;
		}

		int forwardStep = 1;

		while (true) {
			BlockPos forwardPos = pos.offset(motionDirection, forwardStep);
			int existingIndex = movedBlocks.indexOf(forwardPos);

			if (existingIndex > -1) {
				setMovedBlocks(addedCount, existingIndex);

				for (int checkIndex = 0; checkIndex <= existingIndex + addedCount; checkIndex++) {
					BlockPos checkPos = movedBlocks.get(checkIndex);

					if (isBlockSticky(world.getBlockState(checkPos)) && tryMoveAdjacentBlock(checkPos) == false) {
						return false;
					}
				}

				return true;
			}

			blockState = world.getBlockState(forwardPos);

			if (blockState.isAir()) {
				return true;
			}

			if (PistonBlock.isMovable(blockState, world, forwardPos, motionDirection, true, motionDirection) == false
				|| forwardPos.equals(posFrom)
			) {
				return false;
			}

			if (blockState.getPistonBehavior() == PistonBehavior.DESTROY) {
				brokenBlocks.add(forwardPos);
				return true;
			}

			if (movedBlocks.size() >= MAX_MOVABLE_BLOCKS) {
				return false;
			}

			movedBlocks.add(forwardPos);
			addedCount++;
			forwardStep++;
		}
	}

	/**
	 * Переупорядочивает список {@link #movedBlocks} при обнаружении пересечения цепочек.
	 * <p>
	 * Разбивает текущий список на три части и собирает их в правильном порядке:
	 * сначала блоки до точки пересечения, затем хвост новой цепочки, затем середина.
	 *
	 * @param tailSize  количество блоков в хвосте новой цепочки
	 * @param splitPoint индекс точки пересечения в текущем списке
	 */
	private void setMovedBlocks(int tailSize, int splitPoint) {
		List<BlockPos> head = Lists.newArrayList(movedBlocks.subList(0, splitPoint));
		List<BlockPos> tail = Lists.newArrayList(movedBlocks.subList(movedBlocks.size() - tailSize, movedBlocks.size()));
		List<BlockPos> middle = Lists.newArrayList(movedBlocks.subList(splitPoint, movedBlocks.size() - tailSize));

		movedBlocks.clear();
		movedBlocks.addAll(head);
		movedBlocks.addAll(tail);
		movedBlocks.addAll(middle);
	}

	private boolean tryMoveAdjacentBlock(BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);

		for (Direction direction : Direction.values()) {
			if (direction.getAxis() == motionDirection.getAxis()) {
				continue;
			}

			BlockPos adjacentPos = pos.offset(direction);
			BlockState adjacentState = world.getBlockState(adjacentPos);

			if (isAdjacentBlockStuck(adjacentState, blockState) && tryMove(adjacentPos, direction) == false) {
				return false;
			}
		}

		return true;
	}
}
