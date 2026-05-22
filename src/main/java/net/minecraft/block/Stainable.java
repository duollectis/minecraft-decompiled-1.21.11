package net.minecraft.block;

import net.minecraft.util.DyeColor;

/**
 * Маркерный интерфейс для блоков, которые имеют цвет краски ({@link net.minecraft.util.DyeColor}).
 */
public interface Stainable {

	DyeColor getColor();
}
