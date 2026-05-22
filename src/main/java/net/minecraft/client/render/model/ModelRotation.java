package net.minecraft.client.render.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.AffineTransformations;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.DirectionTransformation;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.EnumMap;
import java.util.Map;

/**
 * Вращение блочной модели на основе {@link DirectionTransformation}.
 * Предварительно вычисляет матрицы трансформации для всех направлений граней,
 * а также инвертированные матрицы для UV-блокировки.
 */
@Environment(EnvType.CLIENT)
public class ModelRotation implements ModelBakeSettings {

	private static final Map<DirectionTransformation, ModelRotation> BY_DIRECTION_TRANSFORMATION = Util.mapEnum(
			DirectionTransformation.class, ModelRotation::new
	);

	public static final ModelRotation IDENTITY = fromDirectionTransformation(DirectionTransformation.IDENTITY);

	final DirectionTransformation directionTransformation;
	final AffineTransformation rotation;
	final Map<Direction, Matrix4fc> faces = new EnumMap<>(Direction.class);
	final Map<Direction, Matrix4fc> invertedFaces = new EnumMap<>(Direction.class);

	private final ModelRotation.UVModel uvModel = new ModelRotation.UVModel(this);

	private ModelRotation(DirectionTransformation directionTransformation) {
		this.directionTransformation = directionTransformation;
		rotation = directionTransformation != DirectionTransformation.IDENTITY
				? new AffineTransformation(new Matrix4f(directionTransformation.getMatrix()))
				: AffineTransformation.identity();

		for (Direction direction : Direction.values()) {
			Matrix4fc matrix = AffineTransformations.getTransformed(rotation, direction).getMatrix();
			faces.put(direction, matrix);
			invertedFaces.put(direction, matrix.invertAffine(new Matrix4f()));
		}
	}

	@Override
	public AffineTransformation getRotation() {
		return rotation;
	}

	public static ModelRotation fromDirectionTransformation(DirectionTransformation directionTransformation) {
		return BY_DIRECTION_TRANSFORMATION.get(directionTransformation);
	}

	public ModelBakeSettings getUVModel() {
		return uvModel;
	}

	@Override
	public String toString() {
		return "simple[" + directionTransformation.asString() + "]";
	}

	/**
	 * Вариант настроек запекания с UV-блокировкой: использует предварительно
	 * вычисленные матрицы трансформации граней для корректного выравнивания текстур
	 * при вращении блока.
	 */
	@Environment(EnvType.CLIENT)
	record UVModel(ModelRotation parent) implements ModelBakeSettings {

		@Override
		public AffineTransformation getRotation() {
			return parent.rotation;
		}

		@Override
		public Matrix4fc forward(Direction facing) {
			return parent.faces.getOrDefault(facing, TRANSFORM_NONE);
		}

		@Override
		public Matrix4fc reverse(Direction facing) {
			return parent.invertedFaces.getOrDefault(facing, TRANSFORM_NONE);
		}

		@Override
		public String toString() {
			return "uvLocked[" + parent.directionTransformation.asString() + "]";
		}
	}
}
