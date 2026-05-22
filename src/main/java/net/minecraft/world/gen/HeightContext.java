package net.minecraft.world.gen;

import net.minecraft.world.HeightLimitView;
import net.minecraft.world.gen.chunk.ChunkGenerator;

/**
 * Контекст высот для генерации мира.
 * Вычисляет пересечение допустимых высот мира и генератора чанков.
 */
public class HeightContext {

	private final int minY;
	private final int height;

	public HeightContext(ChunkGenerator generator, HeightLimitView world) {
		minY = Math.max(world.getBottomY(), generator.getMinimumY());
		height = Math.min(world.getHeight(), generator.getWorldHeight());
	}

	public int getMinY() {
		return minY;
	}

	public int getHeight() {
		return height;
	}
}
