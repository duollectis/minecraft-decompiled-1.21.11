package net.minecraft.client.render.model.json;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MatrixUtil;
import org.joml.*;
import org.joml.Math;

@Environment(EnvType.CLIENT)
/**
 * {@code ModelRotation}.
 */
public record ModelRotation(Vector3fc origin, ModelRotation.RotationValue value, boolean rescale, Matrix4fc transform) {

	public ModelRotation(Vector3fc vector3fc, ModelRotation.RotationValue arg, boolean bl) {
		this(vector3fc, arg, bl, computeTransform(arg, bl));
	}

	private static Matrix4f computeTransform(ModelRotation.RotationValue arg, boolean bl) {
		Matrix4f matrix4f = arg.toMatrix();
		if (bl && !MatrixUtil.isIdentity(matrix4f)) {
			Vector3fc vector3fc = computeRescaleVector(matrix4f);
			matrix4f.scale(vector3fc);
		}

		return matrix4f;
	}

	private static Vector3fc computeRescaleVector(Matrix4fc matrix4fc) {
		Vector3f vector3f = new Vector3f();
		float f = computeAxisScale(matrix4fc, Direction.Axis.X, vector3f);
		float g = computeAxisScale(matrix4fc, Direction.Axis.Y, vector3f);
		float h = computeAxisScale(matrix4fc, Direction.Axis.Z, vector3f);
		return vector3f.set(f, g, h);
	}

	private static float computeAxisScale(Matrix4fc matrix4fc, Direction.Axis axis, Vector3f vector3f) {
		Vector3f vector3f2 = vector3f.set(axis.getPositiveDirection().getFloatVector());
		Vector3f vector3f3 = matrix4fc.transformDirection(vector3f2);
		float f = Math.abs(vector3f3.x);
		float g = Math.abs(vector3f3.y);
		float h = Math.abs(vector3f3.z);
		float i = Math.max(Math.max(f, g), h);
		return 1.0F / i;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code EulerRotation}.
	 */
	public record EulerRotation(float x, float y, float z) implements ModelRotation.RotationValue {

		@Override
		public Matrix4f toMatrix() {
			return new Matrix4f()
					.rotationZYX(
							this.z * (float) (java.lang.Math.PI / 180.0),
							this.y * (float) (java.lang.Math.PI / 180.0),
							this.x * (float) (java.lang.Math.PI / 180.0)
					);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code RotationValue}.
	 */
	public interface RotationValue {

		Matrix4f toMatrix();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code AxisAngleRotation}.
	 */
	public record AxisAngleRotation(Direction.Axis axis, float angle) implements ModelRotation.RotationValue {

		@Override
		public Matrix4f toMatrix() {
			Matrix4f matrix4f = new Matrix4f();
			if (this.angle == 0.0F) {
				return matrix4f;
			}
			else {
				Vector3fc vector3fc = this.axis.getPositiveDirection().getFloatVector();
				matrix4f.rotation(this.angle * (float) (java.lang.Math.PI / 180.0), vector3fc);
				return matrix4f;
			}
		}
	}
}
