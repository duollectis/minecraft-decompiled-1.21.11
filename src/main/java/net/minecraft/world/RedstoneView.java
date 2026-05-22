package net.minecraft.world;

import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Представление мира для работы с редстоун-сигналами.
 * Предоставляет методы для получения мощности сигнала от блоков
 * во всех направлениях.
 */
public interface RedstoneView extends BlockView {

	/** Максимальная мощность редстоун-сигнала. */
	int MAX_REDSTONE_POWER = 15;

	Direction[] DIRECTIONS = Direction.values();

	default int getStrongRedstonePower(BlockPos pos, Direction direction) {
		return getBlockState(pos).getStrongRedstonePower(this, pos, direction);
	}

	/**
	 * Возвращает максимальную сильную мощность редстоун-сигнала,
	 * получаемую блоком в позиции {@code pos} со всех сторон.
	 * Прерывает поиск досрочно при достижении максимума (15).
	 */
	default int getReceivedStrongRedstonePower(BlockPos pos) {
		int power = 0;

		power = Math.max(power, getStrongRedstonePower(pos.down(), Direction.DOWN));
		if (power >= MAX_REDSTONE_POWER) {
			return power;
		}

		power = Math.max(power, getStrongRedstonePower(pos.up(), Direction.UP));
		if (power >= MAX_REDSTONE_POWER) {
			return power;
		}

		power = Math.max(power, getStrongRedstonePower(pos.north(), Direction.NORTH));
		if (power >= MAX_REDSTONE_POWER) {
			return power;
		}

		power = Math.max(power, getStrongRedstonePower(pos.south(), Direction.SOUTH));
		if (power >= MAX_REDSTONE_POWER) {
			return power;
		}

		power = Math.max(power, getStrongRedstonePower(pos.west(), Direction.WEST));
		if (power >= MAX_REDSTONE_POWER) {
			return power;
		}

		return Math.max(power, getStrongRedstonePower(pos.east(), Direction.EAST));
	}

	default int getEmittedRedstonePower(BlockPos pos, Direction direction, boolean onlyFromGate) {
		BlockState blockState = getBlockState(pos);

		if (onlyFromGate) {
			return AbstractRedstoneGateBlock.isRedstoneGate(blockState)
				? getStrongRedstonePower(pos, direction)
				: 0;
		}

		if (blockState.isOf(Blocks.REDSTONE_BLOCK)) {
			return MAX_REDSTONE_POWER;
		}

		if (blockState.isOf(Blocks.REDSTONE_WIRE)) {
			return blockState.get(RedstoneWireBlock.POWER);
		}

		return blockState.emitsRedstonePower() ? getStrongRedstonePower(pos, direction) : 0;
	}

	default boolean isEmittingRedstonePower(BlockPos pos, Direction direction) {
		return getEmittedRedstonePower(pos, direction) > 0;
	}

	default int getEmittedRedstonePower(BlockPos pos, Direction direction) {
		BlockState blockState = getBlockState(pos);
		int weakPower = blockState.getWeakRedstonePower(this, pos, direction);
		return blockState.isSolidBlock(this, pos) ? Math.max(weakPower, getReceivedStrongRedstonePower(pos)) : weakPower;
	}

	default boolean isReceivingRedstonePower(BlockPos pos) {
		return getEmittedRedstonePower(pos.down(), Direction.DOWN) > 0
			|| getEmittedRedstonePower(pos.up(), Direction.UP) > 0
			|| getEmittedRedstonePower(pos.north(), Direction.NORTH) > 0
			|| getEmittedRedstonePower(pos.south(), Direction.SOUTH) > 0
			|| getEmittedRedstonePower(pos.west(), Direction.WEST) > 0
			|| getEmittedRedstonePower(pos.east(), Direction.EAST) > 0;
	}

	default int getReceivedRedstonePower(BlockPos pos) {
		int maxPower = 0;

		for (Direction direction : DIRECTIONS) {
			int power = getEmittedRedstonePower(pos.offset(direction), direction);

			if (power >= MAX_REDSTONE_POWER) {
				return MAX_REDSTONE_POWER;
			}

			if (power > maxPower) {
				maxPower = power;
			}
		}

		return maxPower;
	}
}
