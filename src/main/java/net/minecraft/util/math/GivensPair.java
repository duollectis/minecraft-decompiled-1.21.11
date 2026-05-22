package net.minecraft.util.math;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

/**
 * Пара Гивенса (sin(θ/2), cos(θ/2)) — компактное представление угла вращения.
 * Используется в SVD-разложении аффинных трансформаций для извлечения компонент вращения.
 */
public record GivensPair(float sinHalf, float cosHalf) {

	/**
	 * Нормализует вектор (a, b) и возвращает его как пару Гивенса.
	 * Применяется для стабилизации итераций SVD.
	 */
	public static GivensPair normalize(float a, float b) {
		float invLen = Math.invsqrt(a * a + b * b);
		return new GivensPair(invLen * a, invLen * b);
	}

	public static GivensPair fromAngle(float radians) {
		float sin = Math.sin(radians / 2.0F);
		float cos = Math.cosFromSin(sin, radians / 2.0F);
		return new GivensPair(sin, cos);
	}

	public GivensPair negateSin() {
		return new GivensPair(-sinHalf, cosHalf);
	}

	public Quaternionf setXRotation(Quaternionf quaternionf) {
		return quaternionf.set(sinHalf, 0.0F, 0.0F, cosHalf);
	}

	public Quaternionf setYRotation(Quaternionf quaternionf) {
		return quaternionf.set(0.0F, sinHalf, 0.0F, cosHalf);
	}

	public Quaternionf setZRotation(Quaternionf quaternionf) {
		return quaternionf.set(0.0F, 0.0F, sinHalf, cosHalf);
	}

	public float cosDouble() {
		return cosHalf * cosHalf - sinHalf * sinHalf;
	}

	public float sinDouble() {
		return 2.0F * sinHalf * cosHalf;
	}

	public Matrix3f setRotationX(Matrix3f matrix3f) {
		matrix3f.m01 = 0.0F;
		matrix3f.m02 = 0.0F;
		matrix3f.m10 = 0.0F;
		matrix3f.m20 = 0.0F;
		float cos = cosDouble();
		float sin = sinDouble();
		matrix3f.m11 = cos;
		matrix3f.m22 = cos;
		matrix3f.m12 = sin;
		matrix3f.m21 = -sin;
		matrix3f.m00 = 1.0F;
		return matrix3f;
	}

	public Matrix3f setRotationY(Matrix3f matrix3f) {
		matrix3f.m01 = 0.0F;
		matrix3f.m10 = 0.0F;
		matrix3f.m12 = 0.0F;
		matrix3f.m21 = 0.0F;
		float cos = cosDouble();
		float sin = sinDouble();
		matrix3f.m00 = cos;
		matrix3f.m22 = cos;
		matrix3f.m02 = -sin;
		matrix3f.m20 = sin;
		matrix3f.m11 = 1.0F;
		return matrix3f;
	}

	public Matrix3f setRotationZ(Matrix3f matrix3f) {
		matrix3f.m02 = 0.0F;
		matrix3f.m12 = 0.0F;
		matrix3f.m20 = 0.0F;
		matrix3f.m21 = 0.0F;
		float cos = cosDouble();
		float sin = sinDouble();
		matrix3f.m00 = cos;
		matrix3f.m11 = cos;
		matrix3f.m01 = sin;
		matrix3f.m10 = -sin;
		matrix3f.m22 = 1.0F;
		return matrix3f;
	}
}
