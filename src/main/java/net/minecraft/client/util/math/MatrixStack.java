package net.minecraft.client.util.math;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MatrixUtil;
import net.minecraft.util.math.Vec3d;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Стек матриц трансформации для иерархического рендеринга.
 * Каждый элемент стека содержит пару матриц: позиционную (model) и нормальную.
 */
@Environment(EnvType.CLIENT)
public class MatrixStack {

	private static final int INITIAL_CAPACITY = 16;

	private final List<Entry> stack = new ArrayList<>(INITIAL_CAPACITY);
	private int stackDepth;

	public MatrixStack() {
		stack.add(new Entry());
	}

	public void translate(double x, double y, double z) {
		translate((float) x, (float) y, (float) z);
	}

	public void translate(float x, float y, float z) {
		peek().translate(x, y, z);
	}

	public void translate(Vec3d vec) {
		translate(vec.x, vec.y, vec.z);
	}

	public void scale(float x, float y, float z) {
		peek().scale(x, y, z);
	}

	public void multiply(Quaternionfc quaternion) {
		peek().rotate(quaternion);
	}

	public void multiply(Quaternionfc quaternion, float originX, float originY, float originZ) {
		peek().rotateAround(quaternion, originX, originY, originZ);
	}

	public void push() {
		Entry current = peek();
		stackDepth++;
		if (stackDepth >= stack.size()) {
			stack.add(current.copy());
		} else {
			stack.get(stackDepth).copy(current);
		}
	}

	public void pop() {
		if (stackDepth == 0) {
			throw new NoSuchElementException();
		}

		stackDepth--;
	}

	public Entry peek() {
		return stack.get(stackDepth);
	}

	public boolean isEmpty() {
		return stackDepth == 0;
	}

	public void loadIdentity() {
		peek().loadIdentity();
	}

	public void multiplyPositionMatrix(Matrix4fc matrix) {
		peek().multiplyPositionMatrix(matrix);
	}

	@Environment(EnvType.CLIENT)
	public static final class Entry {

		private final Matrix4f positionMatrix = new Matrix4f();
		private final Matrix3f normalMatrix = new Matrix3f();
		private boolean canSkipNormalization = true;

		private void computeNormal() {
			normalMatrix.set(positionMatrix).invert().transpose();
			canSkipNormalization = false;
		}

		public void copy(Entry source) {
			positionMatrix.set(source.positionMatrix);
			normalMatrix.set(source.normalMatrix);
			canSkipNormalization = source.canSkipNormalization;
		}

		public Matrix4f getPositionMatrix() {
			return positionMatrix;
		}

		public Matrix3f getNormalMatrix() {
			return normalMatrix;
		}

		public Vector3f transformNormal(Vector3fc vec, Vector3f dest) {
			return transformNormal(vec.x(), vec.y(), vec.z(), dest);
		}

		public Vector3f transformNormal(float x, float y, float z, Vector3f dest) {
			Vector3f transformed = normalMatrix.transform(x, y, z, dest);
			return canSkipNormalization ? transformed : transformed.normalize();
		}

		public Matrix4f translate(float x, float y, float z) {
			return positionMatrix.translate(x, y, z);
		}

		/**
		 * Масштабирует матрицы позиции и нормалей.
		 * Для равномерного масштабирования нормали корректируются только при отрицательных компонентах.
		 * Для неравномерного — применяется обратная матрица масштаба и отключается оптимизация нормализации.
		 */
		public void scale(float x, float y, float z) {
			positionMatrix.scale(x, y, z);
			if (Math.abs(x) == Math.abs(y) && Math.abs(y) == Math.abs(z)) {
				if (x < 0.0F || y < 0.0F || z < 0.0F) {
					normalMatrix.scale(Math.signum(x), Math.signum(y), Math.signum(z));
				}
			} else {
				normalMatrix.scale(1.0F / x, 1.0F / y, 1.0F / z);
				canSkipNormalization = false;
			}
		}

		public void rotate(Quaternionfc quaternion) {
			positionMatrix.rotate(quaternion);
			normalMatrix.rotate(quaternion);
		}

		public void rotateAround(Quaternionfc quaternion, float originX, float originY, float originZ) {
			positionMatrix.rotateAround(quaternion, originX, originY, originZ);
			normalMatrix.rotate(quaternion);
		}

		public void loadIdentity() {
			positionMatrix.identity();
			normalMatrix.identity();
			canSkipNormalization = true;
		}

		/**
		 * Умножает матрицу позиции на переданную матрицу и обновляет матрицу нормалей.
		 * Для трансляций нормали не меняются; для ортонормальных матриц применяется прямое умножение;
		 * для остальных — пересчёт через инверсию-транспонирование.
		 */
		public void multiplyPositionMatrix(Matrix4fc matrix) {
			positionMatrix.mul(matrix);
			if (MatrixUtil.isTranslation(matrix)) {
				return;
			}

			if (MatrixUtil.isOrthonormal(matrix)) {
				normalMatrix.mul(new Matrix3f(matrix));
			} else {
				computeNormal();
			}
		}

		public Entry copy() {
			Entry copy = new Entry();
			copy.copy(this);
			return copy;
		}
	}
}
