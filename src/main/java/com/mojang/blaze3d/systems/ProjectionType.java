package com.mojang.blaze3d.systems;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
/**
 * {@code ProjectionType}.
 */
public enum ProjectionType {
	PERSPECTIVE(VertexSorter.BY_DISTANCE, (matrix, direction) -> matrix.scale(1.0F - direction / 4096.0F)),
	ORTHOGRAPHIC(VertexSorter.BY_Z, (matrix, direction) -> matrix.translate(0.0F, 0.0F, direction / 512.0F));

	private final VertexSorter vertexSorter;
	private final ProjectionType.Applier applier;

	private ProjectionType(final VertexSorter vertexSorter, final ProjectionType.Applier applier) {
		this.vertexSorter = vertexSorter;
		this.applier = applier;
	}

	public VertexSorter getVertexSorter() {
		return this.vertexSorter;
	}

	public void apply(Matrix4f matrix, float direction) {
		this.applier.apply(matrix, direction);
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code Applier}.
	 */
	interface Applier {

		void apply(Matrix4f matrix, float direction);
	}
}
