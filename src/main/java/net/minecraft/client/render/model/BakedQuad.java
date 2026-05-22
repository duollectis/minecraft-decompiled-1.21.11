package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.joml.Vector3fc;

/**
 * Запечённый четырёхугольник (quad) блочной модели.
 * Хранит 4 вершины с позициями и упакованными UV-координатами, а также метаданные
 * для рендеринга: индекс тинта, направление грани, спрайт, затенение и эмиссию света.
 */
@Environment(EnvType.CLIENT)
public record BakedQuad(
		Vector3fc position0,
		Vector3fc position1,
		Vector3fc position2,
		Vector3fc position3,
		long packedUV0,
		long packedUV1,
		long packedUV2,
		long packedUV3,
		int tintIndex,
		Direction face,
		Sprite sprite,
		boolean shade,
		int lightEmission
) {

	public static final int NUM_VERTICES = 4;
	private static final int NO_TINT = -1;

	public boolean hasTint() {
		return tintIndex != NO_TINT;
	}

	public Vector3fc getPosition(int index) {
		return switch (index) {
			case 0 -> position0;
			case 1 -> position1;
			case 2 -> position2;
			case 3 -> position3;
			default -> throw new IndexOutOfBoundsException(index);
		};
	}

	public long getTexcoords(int index) {
		return switch (index) {
			case 0 -> packedUV0;
			case 1 -> packedUV1;
			case 2 -> packedUV2;
			case 3 -> packedUV3;
			default -> throw new IndexOutOfBoundsException(index);
		};
	}
}
