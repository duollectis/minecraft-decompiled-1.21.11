package net.minecraft.world.gen.chunk;

import net.minecraft.block.BlockState;

/**
 * Абстракция вертикального столбца блоков в чанке.
 * Используется при генерации поверхности и применении правил материалов.
 */
public interface BlockColumn {

	BlockState getState(int y);

	void setState(int y, BlockState state);
}
