package net.minecraft.world.biome;

/**
 * Функциональный интерфейс для вычисления цвета биома в заданной позиции.
 * Используется клиентской стороной для блендинга цветов между соседними биомами.
 */
@FunctionalInterface
public interface ColorResolver {

	int getColor(Biome biome, double x, double z);
}
