package net.minecraft.util;

/**
 * Итератор по всем блокам внутри кубоида (прямоугольного параллелепипеда).
 * <p>
 * Перебирает блоки в порядке X → Y → Z. Для каждого шага предоставляет
 * абсолютные координаты текущего блока и количество граничных осей,
 * по которым блок находится на краю кубоида.
 * <p>
 * Использование:
 * <pre>{@code
 * CuboidBlockIterator iter = new CuboidBlockIterator(x1, y1, z1, x2, y2, z2);
 * while (iter.step()) {
 *     doSomething(iter.getX(), iter.getY(), iter.getZ());
 * }
 * }</pre>
 */
public class CuboidBlockIterator {

	public static final int EDGE_COORD_NONE = 0;
	public static final int EDGE_COORD_ONE = 1;
	public static final int EDGE_COORD_TWO = 2;
	public static final int EDGE_COORD_THREE = 3;

	private final int startX;
	private final int startY;
	private final int startZ;
	private final int sizeX;
	private final int sizeY;
	private final int sizeZ;
	private final int totalSize;
	private int blocksIterated;
	private int x;
	private int y;
	private int z;

	public CuboidBlockIterator(int startX, int startY, int startZ, int endX, int endY, int endZ) {
		this.startX = startX;
		this.startY = startY;
		this.startZ = startZ;
		this.sizeX = endX - startX + 1;
		this.sizeY = endY - startY + 1;
		this.sizeZ = endZ - startZ + 1;
		this.totalSize = sizeX * sizeY * sizeZ;
	}

	/**
	 * Переходит к следующему блоку в кубоиде.
	 *
	 * @return {@code true} если следующий блок существует, {@code false} если итерация завершена
	 */
	public boolean step() {
		if (blocksIterated == totalSize) {
			return false;
		}

		x = blocksIterated % sizeX;
		int flat = blocksIterated / sizeX;
		y = flat % sizeY;
		z = flat / sizeY;
		blocksIterated++;

		return true;
	}

	public int getX() {
		return startX + x;
	}

	public int getY() {
		return startY + y;
	}

	public int getZ() {
		return startZ + z;
	}

	/**
	 * Возвращает количество осей, по которым текущий блок находится на краю кубоида.
	 * Значение от 0 (внутренний блок) до 3 (угловой блок).
	 *
	 * @return количество граничных осей (0–3)
	 */
	public int getEdgeCoordinatesCount() {
		int edgeCount = 0;

		if (x == 0 || x == sizeX - 1) {
			edgeCount++;
		}

		if (y == 0 || y == sizeY - 1) {
			edgeCount++;
		}

		if (z == 0 || z == sizeZ - 1) {
			edgeCount++;
		}

		return edgeCount;
	}
}
