package net.minecraft.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Box;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Усечённая пирамида видимости (view frustum) для отсечения невидимых объектов.
 * Использует JOML {@link FrustumIntersection} для быстрой проверки пересечения AABB с фрустумом.
 * Поддерживает смещение позиции камеры и расширение охватывающего бокса для корректного
 * отсечения чанков с учётом погрешностей позиционирования.
 */
@Environment(EnvType.CLIENT)
public class Frustum {

	/** Масштаб отступа при расширении охватывающего бокса в {@link #coverBoxAroundSetPosition}. */
	public static final int RECESSION_SCALE = 4;

	private final FrustumIntersection frustumIntersection = new FrustumIntersection();
	private final Matrix4f positionProjectionMatrix = new Matrix4f();
	private Vector4f recession;
	private double x;
	private double y;
	private double z;

	public Frustum(Matrix4f positionMatrix, Matrix4f projectionMatrix) {
		init(positionMatrix, projectionMatrix);
	}

	public Frustum(Frustum frustum) {
		frustumIntersection.set(frustum.positionProjectionMatrix);
		positionProjectionMatrix.set(frustum.positionProjectionMatrix);
		x = frustum.x;
		y = frustum.y;
		z = frustum.z;
		recession = frustum.recession;
	}

	/** Смещает позицию фрустума вдоль вектора рецессии на заданное расстояние. */
	public Frustum offset(float distance) {
		x = x + recession.x * distance;
		y = y + recession.y * distance;
		z = z + recession.z * distance;
		return this;
	}

	/**
	 * Сдвигает позицию фрустума так, чтобы бокс заданного размера вокруг текущей позиции
	 * полностью попадал в фрустум. Используется для корректного отсечения чанков.
	 *
	 * @param boxSize размер охватывающего бокса в блоках
	 * @return {@code this} для цепочки вызовов
	 */
	public Frustum coverBoxAroundSetPosition(int boxSize) {
		double minX = Math.floor(x / boxSize) * boxSize;
		double minY = Math.floor(y / boxSize) * boxSize;
		double minZ = Math.floor(z / boxSize) * boxSize;
		double maxX = Math.ceil(x / boxSize) * boxSize;
		double maxY = Math.ceil(y / boxSize) * boxSize;

		for (double maxZ = Math.ceil(z / boxSize) * boxSize;
				frustumIntersection.intersectAab(
						(float) (minX - x),
						(float) (minY - y),
						(float) (minZ - z),
						(float) (maxX - x),
						(float) (maxY - y),
						(float) (maxZ - z)
				) != -2;
				z = z - recession.z() * RECESSION_SCALE
		) {
			x = x - recession.x() * RECESSION_SCALE;
			y = y - recession.y() * RECESSION_SCALE;
		}

		return this;
	}

	public void setPosition(double cameraX, double cameraY, double cameraZ) {
		x = cameraX;
		y = cameraY;
		z = cameraZ;
	}

	private void init(Matrix4f positionMatrix, Matrix4f projectionMatrix) {
		projectionMatrix.mul(positionMatrix, positionProjectionMatrix);
		frustumIntersection.set(positionProjectionMatrix);
		recession = positionProjectionMatrix.transformTranspose(new Vector4f(0.0F, 0.0F, 1.0F, 0.0F));
	}

	public boolean isVisible(Box box) {
		int result = intersectAab(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
		return result == -2 || result == -1;
	}

	public int intersectAab(BlockBox box) {
		return intersectAab(
				box.getMinX(),
				box.getMinY(),
				box.getMinZ(),
				box.getMaxX() + 1,
				box.getMaxY() + 1,
				box.getMaxZ() + 1
		);
	}

	private int intersectAab(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		float localMinX = (float) (minX - x);
		float localMinY = (float) (minY - y);
		float localMinZ = (float) (minZ - z);
		float localMaxX = (float) (maxX - x);
		float localMaxY = (float) (maxY - y);
		float localMaxZ = (float) (maxZ - z);
		return frustumIntersection.intersectAab(localMinX, localMinY, localMinZ, localMaxX, localMaxY, localMaxZ);
	}

	public boolean intersectPoint(double x, double y, double z) {
		return frustumIntersection.testPoint((float) (x - this.x), (float) (y - this.y), (float) (z - this.z));
	}

	/**
	 * Возвращает 8 угловых точек фрустума в мировом пространстве.
	 * Вычисляется через обратную матрицу проекции-вида из NDC-координат.
	 *
	 * @return массив из 8 точек в однородных координатах (после деления на w)
	 */
	public Vector4f[] getBoundaryPoints() {
		Vector4f[] corners = new Vector4f[]{
				new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F),
				new Vector4f(1.0F, -1.0F, -1.0F, 1.0F),
				new Vector4f(1.0F, 1.0F, -1.0F, 1.0F),
				new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F),
				new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F),
				new Vector4f(1.0F, -1.0F, 1.0F, 1.0F),
				new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
				new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F)
		};
		Matrix4f inverseMatrix = positionProjectionMatrix.invert(new Matrix4f());

		for (int cornerIndex = 0; cornerIndex < 8; cornerIndex++) {
			inverseMatrix.transform(corners[cornerIndex]);
			corners[cornerIndex].div(corners[cornerIndex].w());
		}

		return corners;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}
}
