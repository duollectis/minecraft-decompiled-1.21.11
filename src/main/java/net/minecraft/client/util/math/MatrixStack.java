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

@Environment(EnvType.CLIENT)
/**
 * {@code MatrixStack}.
 */
public class MatrixStack {

	private final List<MatrixStack.Entry> stack = new ArrayList<>(16);
	private int stackDepth;

	public MatrixStack() {
		this.stack.add(new MatrixStack.Entry());
	}

	/**
	 * Translate.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 */
	public void translate(double x, double y, double z) {
		this.translate((float) x, (float) y, (float) z);
	}

	/**
	 * Translate.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 */
	public void translate(float x, float y, float z) {
		this.peek().translate(x, y, z);
	}

	/**
	 * Translate.
	 *
	 * @param vec vec
	 */
	public void translate(Vec3d vec) {
		this.translate(vec.x, vec.y, vec.z);
	}

	/**
	 * Scale.
	 *
	 * @param x x
	 * @param y y
	 * @param z z
	 */
	public void scale(float x, float y, float z) {
		this.peek().scale(x, y, z);
	}

	/**
	 * Multiply.
	 *
	 * @param quaternion quaternion
	 */
	public void multiply(Quaternionfc quaternion) {
		this.peek().rotate(quaternion);
	}

	/**
	 * Multiply.
	 *
	 * @param quaternion quaternion
	 * @param originX origin x
	 * @param originY origin y
	 * @param originZ origin z
	 */
	public void multiply(Quaternionfc quaternion, float originX, float originY, float originZ) {
		this.peek().rotateAround(quaternion, originX, originY, originZ);
	}

	/**
	 * Push.
	 */
	public void push() {
		MatrixStack.Entry entry = this.peek();
		this.stackDepth++;
		if (this.stackDepth >= this.stack.size()) {
			this.stack.add(entry.copy());
		}
		else {
			this.stack.get(this.stackDepth).copy(entry);
		}
	}

	/**
	 * Pop.
	 */
	public void pop() {
		if (this.stackDepth == 0) {
			throw new NoSuchElementException();
		}
		else {
			this.stackDepth--;
		}
	}

	public MatrixStack.Entry peek() {
		return this.stack.get(this.stackDepth);
	}

	public boolean isEmpty() {
		return this.stackDepth == 0;
	}

	/**
	 * Загружает identity.
	 */
	public void loadIdentity() {
		this.peek().loadIdentity();
	}

	/**
	 * Multiply position matrix.
	 *
	 * @param matrix matrix
	 */
	public void multiplyPositionMatrix(Matrix4fc matrix) {
		this.peek().multiplyPositionMatrix(matrix);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Entry}.
	 */
	public static final class Entry {

		private final Matrix4f positionMatrix = new Matrix4f();
		private final Matrix3f normalMatrix = new Matrix3f();
		private boolean canSkipNormalization = true;

		private void computeNormal() {
			this.normalMatrix.set(this.positionMatrix).invert().transpose();
			this.canSkipNormalization = false;
		}

		/**
		 * Copy.
		 *
		 * @param entry entry
		 */
		public void copy(MatrixStack.Entry entry) {
			this.positionMatrix.set(entry.positionMatrix);
			this.normalMatrix.set(entry.normalMatrix);
			this.canSkipNormalization = entry.canSkipNormalization;
		}

		public Matrix4f getPositionMatrix() {
			return this.positionMatrix;
		}

		public Matrix3f getNormalMatrix() {
			return this.normalMatrix;
		}

		/**
		 * Трансформирует normal.
		 *
		 * @param vec vec
		 * @param dest dest
		 *
		 * @return Vector3f — результат операции
		 */
		public Vector3f transformNormal(Vector3fc vec, Vector3f dest) {
			return this.transformNormal(vec.x(), vec.y(), vec.z(), dest);
		}

		/**
		 * Трансформирует normal.
		 *
		 * @param x x
		 * @param y y
		 * @param z z
		 * @param dest dest
		 *
		 * @return Vector3f — результат операции
		 */
		public Vector3f transformNormal(float x, float y, float z, Vector3f dest) {
			Vector3f vector3f = this.normalMatrix.transform(x, y, z, dest);
			return this.canSkipNormalization ? vector3f : vector3f.normalize();
		}

		/**
		 * Translate.
		 *
		 * @param x x
		 * @param y y
		 * @param z z
		 *
		 * @return Matrix4f — результат операции
		 */
		public Matrix4f translate(float x, float y, float z) {
			return this.positionMatrix.translate(x, y, z);
		}

		/**
		 * Scale.
		 *
		 * @param x x
		 * @param y y
		 * @param z z
		 */
		public void scale(float x, float y, float z) {
			this.positionMatrix.scale(x, y, z);
			if (Math.abs(x) == Math.abs(y) && Math.abs(y) == Math.abs(z)) {
				if (x < 0.0F || y < 0.0F || z < 0.0F) {
					this.normalMatrix.scale(Math.signum(x), Math.signum(y), Math.signum(z));
				}
			}
			else {
				this.normalMatrix.scale(1.0F / x, 1.0F / y, 1.0F / z);
				this.canSkipNormalization = false;
			}
		}

		/**
		 * Rotate.
		 *
		 * @param quaternion quaternion
		 */
		public void rotate(Quaternionfc quaternion) {
			this.positionMatrix.rotate(quaternion);
			this.normalMatrix.rotate(quaternion);
		}

		/**
		 * Rotate around.
		 *
		 * @param quaternion quaternion
		 * @param originX origin x
		 * @param originY origin y
		 * @param originZ origin z
		 */
		public void rotateAround(Quaternionfc quaternion, float originX, float originY, float originZ) {
			this.positionMatrix.rotateAround(quaternion, originX, originY, originZ);
			this.normalMatrix.rotate(quaternion);
		}

		/**
		 * Загружает identity.
		 */
		public void loadIdentity() {
			this.positionMatrix.identity();
			this.normalMatrix.identity();
			this.canSkipNormalization = true;
		}

		/**
		 * Multiply position matrix.
		 *
		 * @param matrix matrix
		 */
		public void multiplyPositionMatrix(Matrix4fc matrix) {
			this.positionMatrix.mul(matrix);
			if (!MatrixUtil.isTranslation(matrix)) {
				if (MatrixUtil.isOrthonormal(matrix)) {
					this.normalMatrix.mul(new Matrix3f(matrix));
				}
				else {
					this.computeNormal();
				}
			}
		}

		public MatrixStack.Entry copy() {
			MatrixStack.Entry entry = new MatrixStack.Entry();
			entry.copy(this);
			return entry;
		}
	}
}
