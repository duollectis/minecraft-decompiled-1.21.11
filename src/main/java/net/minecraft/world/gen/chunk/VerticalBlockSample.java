package net.minecraft.world.gen.chunk;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/**
 * Реализация {@link BlockColumn} на основе массива состояний блоков.
 * Используется для вертикального сэмплирования столбца чанка.
 */
public final class VerticalBlockSample implements BlockColumn {

	private final int startY;
	private final BlockState[] states;

	public VerticalBlockSample(int startY, BlockState[] states) {
		this.startY = startY;
		this.states = states;
	}

	@Override
	public BlockState getState(int y) {
		int index = y - startY;
		return index >= 0 && index < states.length
			? states[index]
			: Blocks.AIR.getDefaultState();
	}

	@Override
	public void setState(int y, BlockState state) {
		int index = y - startY;

		if (index < 0 || index >= states.length) {
			throw new IllegalArgumentException("Outside of column height: " + y);
		}

		states[index] = state;
	}
}
