package net.minecraft.util.collection;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Двумерный массив значений, ограниченный прямоугольной областью в координатах чанков.
 * Используется для кэширования данных региона вокруг центральной точки.
 */
public class BoundedRegionArray<T> {

	private final int minX;
	private final int minZ;
	private final int maxX;
	private final int maxZ;
	private final Object[] array;

	public static <T> BoundedRegionArray<T> create(
		int centerX,
		int centerZ,
		int radius,
		Getter<T> getter
	) {
		int startX = centerX - radius;
		int startZ = centerZ - radius;
		int diameter = 2 * radius + 1;
		return new BoundedRegionArray<>(startX, startZ, diameter, diameter, getter);
	}

	private BoundedRegionArray(int minX, int minZ, int maxX, int maxZ, Getter<T> getter) {
		this.minX = minX;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxZ = maxZ;
		array = new Object[maxX * maxZ];

		for (int x = minX; x < minX + maxX; x++) {
			for (int z = minZ; z < minZ + maxZ; z++) {
				array[toIndex(x, z)] = getter.get(x, z);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void forEach(Consumer<T> callback) {
		for (Object element : array) {
			callback.accept((T) element);
		}
	}

	@SuppressWarnings("unchecked")
	public T get(int x, int z) {
		if (!isWithinBounds(x, z)) {
			throw new IllegalArgumentException("Requested out of range value (" + x + "," + z + ") from " + this);
		}

		return (T) array[toIndex(x, z)];
	}

	public boolean isWithinBounds(int x, int z) {
		int relX = x - minX;
		int relZ = z - minZ;
		return relX >= 0 && relX < maxX && relZ >= 0 && relZ < maxZ;
	}

	@Override
	public String toString() {
		return String.format(
			Locale.ROOT,
			"StaticCache2D[%d, %d, %d, %d]",
			minX,
			minZ,
			minX + maxX,
			minZ + maxZ
		);
	}

	private int toIndex(int x, int z) {
		return (x - minX) * maxZ + (z - minZ);
	}

	/** Поставщик значений для заполнения массива при создании. */
	@FunctionalInterface
	public interface Getter<T> {

		T get(int x, int z);
	}
}
