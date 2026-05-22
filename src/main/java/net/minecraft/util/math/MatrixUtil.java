package net.minecraft.util.math;

import org.apache.commons.lang3.tuple.Triple;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Утилитарный класс для операций над матрицами JOML.
 * Содержит реализацию SVD-разложения через итерации Якоби с парами Гивенса,
 * а также вспомогательные методы для проверки свойств матриц.
 */
public class MatrixUtil {

	// Котангенс π/8 — порог для выбора между аппроксимацией и точным значением в итерации Якоби
	private static final float COT_PI_OVER_8 = 3.0F + 2.0F * Math.sqrt(2.0F);
	private static final GivensPair SIN_COS_PI_OVER_8 = GivensPair.fromAngle((float) (java.lang.Math.PI / 4));

	// Флаги свойств матрицы из JOML Matrix4fc
	private static final int PROPERTY_IDENTITY = 4;
	private static final int PROPERTY_TRANSLATION = 8;
	private static final int PROPERTY_ORTHONORMAL = 16;

	private MatrixUtil() {
	}

	public static Matrix4f scale(Matrix4f matrix, float scalar) {
		return matrix.set(
				matrix.m00() * scalar,
				matrix.m01() * scalar,
				matrix.m02() * scalar,
				matrix.m03() * scalar,
				matrix.m10() * scalar,
				matrix.m11() * scalar,
				matrix.m12() * scalar,
				matrix.m13() * scalar,
				matrix.m20() * scalar,
				matrix.m21() * scalar,
				matrix.m22() * scalar,
				matrix.m23() * scalar,
				matrix.m30() * scalar,
				matrix.m31() * scalar,
				matrix.m32() * scalar,
				matrix.m33() * scalar
		);
	}

	private static GivensPair approximateGivensQuaternion(float a11, float a12, float a22) {
		float diff = 2.0F * (a11 - a22);
		return COT_PI_OVER_8 * a12 * a12 < diff * diff
				? GivensPair.normalize(a12, diff)
				: SIN_COS_PI_OVER_8;
	}

	private static GivensPair qrGivensQuaternion(float a1, float a2) {
		float hyp = (float) java.lang.Math.hypot(a1, a2);
		float s = hyp > 1.0E-6F ? a2 : 0.0F;
		float c = Math.abs(a1) + Math.max(hyp, 1.0E-6F);

		if (a1 < 0.0F) {
			float tmp = s;
			s = c;
			c = tmp;
		}

		return GivensPair.normalize(s, c);
	}

	private static void conjugate(Matrix3f X, Matrix3f A) {
		X.mul(A);
		A.transpose();
		A.mul(X);
		X.set(A);
	}

	private static void applyJacobiIteration(
			Matrix3f AtA,
			Matrix3f rotation,
			Quaternionf tempQuat,
			Quaternionf accumQuat
	) {
		if (AtA.m01 * AtA.m01 + AtA.m10 * AtA.m10 > 1.0E-6F) {
			GivensPair pair = approximateGivensQuaternion(AtA.m00, 0.5F * (AtA.m01 + AtA.m10), AtA.m11);
			accumQuat.mul(pair.setZRotation(tempQuat));
			conjugate(AtA, pair.setRotationZ(rotation));
		}

		if (AtA.m02 * AtA.m02 + AtA.m20 * AtA.m20 > 1.0E-6F) {
			GivensPair pair = approximateGivensQuaternion(AtA.m00, 0.5F * (AtA.m02 + AtA.m20), AtA.m22).negateSin();
			accumQuat.mul(pair.setYRotation(tempQuat));
			conjugate(AtA, pair.setRotationY(rotation));
		}

		if (AtA.m12 * AtA.m12 + AtA.m21 * AtA.m21 > 1.0E-6F) {
			GivensPair pair = approximateGivensQuaternion(AtA.m11, 0.5F * (AtA.m12 + AtA.m21), AtA.m22);
			accumQuat.mul(pair.setXRotation(tempQuat));
			conjugate(AtA, pair.setRotationX(rotation));
		}
	}

	/**
	 * Вычисляет правое вращение SVD через итерации Якоби на матрице A^T·A.
	 * Каждая итерация обнуляет внедиагональные элементы через повороты Гивенса.
	 */
	public static Quaternionf applyJacobiIterations(Matrix3f AtA, int numJacobiIterations) {
		Quaternionf result = new Quaternionf();
		Matrix3f rotation = new Matrix3f();
		Quaternionf tempQuat = new Quaternionf();

		for (int i = 0; i < numJacobiIterations; i++) {
			applyJacobiIteration(AtA, rotation, tempQuat, result);
		}

		result.normalize();
		return result;
	}

	/**
	 * SVD-разложение матрицы 3×3: A = U · Σ · V^T.
	 * Возвращает тройку (U, Σ, V), где U и V — кватернионы вращения, Σ — вектор сингулярных значений.
	 */
	public static Triple<Quaternionf, Vector3f, Quaternionf> svdDecompose(Matrix3f A) {
		Matrix3f AtA = new Matrix3f(A);
		AtA.transpose();
		AtA.mul(A);

		Quaternionf rightRotation = applyJacobiIterations(AtA, 5);
		boolean firstSingularNearZero = AtA.m00 < 1.0E-6;
		boolean secondSingularNearZero = AtA.m11 < 1.0E-6;

		Matrix3f rotatedA = A.rotate(rightRotation);
		Quaternionf leftRotation = new Quaternionf();
		Quaternionf tempQuat = new Quaternionf();

		GivensPair pair = firstSingularNearZero
				? qrGivensQuaternion(rotatedA.m11, -rotatedA.m10)
				: qrGivensQuaternion(rotatedA.m00, rotatedA.m01);

		Matrix3f step1 = new Matrix3f();
		leftRotation.mul(pair.setZRotation(tempQuat));
		pair.setRotationZ(step1).transpose().mul(rotatedA);

		pair = firstSingularNearZero
				? qrGivensQuaternion(step1.m22, -step1.m20)
				: qrGivensQuaternion(step1.m00, step1.m02);
		pair = pair.negateSin();

		Matrix3f step2 = new Matrix3f();
		leftRotation.mul(pair.setYRotation(tempQuat));
		pair.setRotationY(step2).transpose().mul(step1);

		pair = secondSingularNearZero
				? qrGivensQuaternion(step2.m22, -step2.m21)
				: qrGivensQuaternion(step2.m11, step2.m12);

		Matrix3f step3 = new Matrix3f();
		leftRotation.mul(pair.setXRotation(tempQuat));
		pair.setRotationX(step3).transpose().mul(step2);

		Vector3f singularValues = new Vector3f(step3.m00, step3.m11, step3.m22);
		return Triple.of(leftRotation, singularValues, rightRotation.conjugate());
	}

	private static boolean isPropertyBitSet(Matrix4fc matrix, int property) {
		return (matrix.properties() & property) != 0;
	}

	public static boolean hasProperty(Matrix4fc matrix, int property) {
		if (isPropertyBitSet(matrix, property)) {
			return true;
		}

		if (matrix instanceof Matrix4f matrix4f) {
			matrix4f.determineProperties();
			return isPropertyBitSet(matrix, property);
		}

		return false;
	}

	public static boolean isIdentity(Matrix4fc matrix) {
		return hasProperty(matrix, PROPERTY_IDENTITY);
	}

	public static boolean isTranslation(Matrix4fc matrix) {
		return hasProperty(matrix, PROPERTY_TRANSLATION);
	}

	public static boolean isOrthonormal(Matrix4fc matrix) {
		return hasProperty(matrix, PROPERTY_ORTHONORMAL);
	}
}
