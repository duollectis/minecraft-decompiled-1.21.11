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

@Environment(EnvType.CLIENT)
/**
 * {@code ModelRotation}.
 */
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
		if (directionTransformation != DirectionTransformation.IDENTITY) {
			this.rotation = new AffineTransformation(new Matrix4f(directionTransformation.getMatrix()));
		}
		else {
			this.rotation = AffineTransformation.identity();
		}

		for (Direction direction : Direction.values()) {
			Matrix4fc matrix4fc = AffineTransformations.getTransformed(this.rotation, direction).getMatrix();
			this.faces.put(direction, matrix4fc);
			this.invertedFaces.put(direction, matrix4fc.invertAffine(new Matrix4f()));
		}
	}

	@Override
	public AffineTransformation getRotation() {
		return this.rotation;
	}

	/**
	 * From direction transformation.
	 *
	 * @param directionTransformation direction transformation
	 *
	 * @return ModelRotation — результат операции
	 */
	public static ModelRotation fromDirectionTransformation(DirectionTransformation directionTransformation) {
		return BY_DIRECTION_TRANSFORMATION.get(directionTransformation);
	}

	public ModelBakeSettings getUVModel() {
		return this.uvModel;
	}

	@Override
	public String toString() {
		return "simple[" + this.directionTransformation.asString() + "]";
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code UVModel}.
	 */
	record UVModel(ModelRotation parent) implements ModelBakeSettings {

		@Override
		public AffineTransformation getRotation() {
			return this.parent.rotation;
		}

		@Override
		public Matrix4fc forward(Direction facing) {
			return this.parent.faces.getOrDefault(facing, TRANSFORM_NONE);
		}

		@Override
		public Matrix4fc reverse(Direction facing) {
			return this.parent.invertedFaces.getOrDefault(facing, TRANSFORM_NONE);
		}

		@Override
		public String toString() {
			return "uvLocked[" + this.parent.directionTransformation.asString() + "]";
		}
	}
}
