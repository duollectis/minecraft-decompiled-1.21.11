package com.mojang.blaze3d.systems;

import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.Vec3fArray;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Сортировщик вершин для корректного рендеринга полупрозрачных объектов.
 * Возвращает массив индексов, упорядоченных по убыванию ключа сортировки
 * (дальние объекты рисуются первыми — painter's algorithm).
 */
@Environment(EnvType.CLIENT)
public interface VertexSorter {

	/** Сортировка по расстоянию от начала координат (0, 0, 0). */
	VertexSorter BY_DISTANCE = byDistance(0.0F, 0.0F, 0.0F);

	/** Сортировка по отрицательной Z-координате (дальние по Z — первыми). */
	VertexSorter BY_Z = of(vec -> -vec.z());

	/** Создаёт сортировщик по расстоянию от заданной точки. */
	static VertexSorter byDistance(float originX, float originY, float originZ) {
		return byDistance(new Vector3f(originX, originY, originZ));
	}

	/** Создаёт сортировщик по расстоянию от заданного вектора-источника. */
	static VertexSorter byDistance(Vector3fc origin) {
		return of(origin::distanceSquared);
	}

	/**
	 * Создаёт сортировщик на основе произвольной функции вычисления ключа.
	 * Вершины сортируются по убыванию ключа (наибольший ключ — первый).
	 *
	 * @param mapper функция, вычисляющая числовой ключ сортировки для каждой вершины
	 */
	static VertexSorter of(SortKeyMapper mapper) {
		return vectors -> {
			Vector3f temp = new Vector3f();
			int count = vectors.size();
			float[] keys = new float[count];
			int[] indices = new int[count];

			for (int index = 0; index < count; index++) {
				keys[index] = mapper.apply(vectors.get(index, temp));
				indices[index] = index;
			}

			IntArrays.mergeSort(indices, (a, b) -> Floats.compare(keys[b], keys[a]));
			return indices;
		};
	}

	/**
	 * Сортирует массив центров квадов и возвращает массив индексов в порядке рендеринга.
	 *
	 * @param vectors массив центров квадов
	 * @return массив индексов квадов, упорядоченных для корректного alpha blending
	 */
	int[] sort(Vec3fArray vectors);

	/** Функция вычисления ключа сортировки для одной вершины. */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	interface SortKeyMapper {

		float apply(Vector3f vec);
	}
}
