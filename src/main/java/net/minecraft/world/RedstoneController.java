package net.minecraft.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.block.WireOrientation;
import org.jspecify.annotations.Nullable;

/**
 * Базовый контроллер логики редстоун-провода.
 * Определяет алгоритм расчёта мощности провода с учётом соседних блоков.
 */
public abstract class RedstoneController {

	protected final RedstoneWireBlock wire;

	protected RedstoneController(RedstoneWireBlock wire) {
		this.wire = wire;
	}

	public abstract void update(
		World world,
		BlockPos pos,
		BlockState state,
		@Nullable WireOrientation orientation,
		boolean blockAdded
	);

	protected int getStrongPowerAt(World world, BlockPos pos) {
		return wire.getStrongPower(world, pos);
	}

	protected int getWirePowerAt(BlockPos pos, BlockState state) {
		return state.isOf(wire) ? state.get(RedstoneWireBlock.POWER) : 0;
	}

	/**
	 * Вычисляет мощность провода, получаемую от соседних проводов.
	 * Учитывает провода на одном уровне, сверху (если сосед непрозрачный) и снизу.
	 * Возвращает максимальную мощность минус 1 (затухание сигнала), но не менее 0.
	 *
	 * @param world мир для чтения состояний блоков
	 * @param pos   позиция провода
	 * @return мощность, получаемая от соседних проводов
	 */
	protected int calculateWirePowerAt(World world, BlockPos pos) {
		int maxPower = 0;

		for (Direction direction : Direction.Type.HORIZONTAL) {
			BlockPos neighborPos = pos.offset(direction);
			BlockState neighborState = world.getBlockState(neighborPos);
			maxPower = Math.max(maxPower, getWirePowerAt(neighborPos, neighborState));

			BlockPos abovePos = pos.up();

			if (neighborState.isSolidBlock(world, neighborPos)
				&& !world.getBlockState(abovePos).isSolidBlock(world, abovePos)
			) {
				BlockPos aboveNeighbor = neighborPos.up();
				maxPower = Math.max(maxPower, getWirePowerAt(aboveNeighbor, world.getBlockState(aboveNeighbor)));
			} else if (!neighborState.isSolidBlock(world, neighborPos)) {
				BlockPos belowNeighbor = neighborPos.down();
				maxPower = Math.max(maxPower, getWirePowerAt(belowNeighbor, world.getBlockState(belowNeighbor)));
			}
		}

		return Math.max(0, maxPower - 1);
	}
}
