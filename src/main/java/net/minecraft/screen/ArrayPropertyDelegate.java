package net.minecraft.screen;

/**
 * Простейшая реализация {@link PropertyDelegate} на основе примитивного массива {@code int[]}.
 * <p>
 * Используется как заглушка на клиентской стороне или в тестах, когда реальная
 * блок-сущность недоступна, но экрану нужен делегат корректного размера.
 */
public class ArrayPropertyDelegate implements PropertyDelegate {

	private final int[] data;

	public ArrayPropertyDelegate(int size) {
		data = new int[size];
	}

	@Override
	public int get(int index) {
		return data[index];
	}

	@Override
	public void set(int index, int value) {
		data[index] = value;
	}

	@Override
	public int size() {
		return data.length;
	}
}
