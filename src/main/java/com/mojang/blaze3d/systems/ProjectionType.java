package com.mojang.blaze3d.systems;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;

/**
 * Тип проекционной матрицы, используемой при рендеринге.
 * Определяет алгоритм сортировки вершин и способ применения смещения глубины.
 */
@Environment(EnvType.CLIENT)
public enum ProjectionType {
	/**
	 * Перспективная проекция — объекты уменьшаются с расстоянием.
	 * Сортировка по расстоянию от камеры, смещение глубины через масштабирование.
	 */
	PERSPECTIVE(VertexSorter.BY_DISTANCE, (matrix, direction) -> matrix.scale(1.0F - direction / 4096.0F)),

	/**
	 * Ортографическая проекция — объекты одинакового размера независимо от расстояния.
	 * Сортировка по Z-координате, смещение глубины через трансляцию.
	 */
	ORTHOGRAPHIC(VertexSorter.BY_Z, (matrix, direction) -> matrix.translate(0.0F, 0.0F, direction / 512.0F));

	private final VertexSorter vertexSorter;
	private final Applier applier;

	ProjectionType(VertexSorter vertexSorter, Applier applier) {
		this.vertexSorter = vertexSorter;
		this.applier = applier;
	}

	public VertexSorter getVertexSorter() {
		return vertexSorter;
	}

	/**
	 * Применяет смещение глубины к матрице проекции.
	 *
	 * @param matrix    матрица проекции для модификации
	 * @param direction величина смещения глубины
	 */
	public void apply(Matrix4f matrix, float direction) {
		applier.apply(matrix, direction);
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	interface Applier {

		void apply(Matrix4f matrix, float direction);
	}
}
