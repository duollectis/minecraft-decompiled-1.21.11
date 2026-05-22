package net.minecraft.util.shape;

import it.unimi.dsi.fastutil.doubles.DoubleList;

/**
 * Список пар координат для вычисления пересечений форм вокселей.
 * Используется в алгоритмах слияния {@link net.minecraft.util.shape.VoxelShape}
 * для итерации по парам смежных координат вдоль одной оси.
 */
interface PairList {

	DoubleList getPairs();

	boolean forEachPair(PairList.Consumer predicate);

	int size();

	public interface Consumer {

		boolean merge(int x, int y, int index);
	}
}
