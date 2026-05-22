package net.minecraft.world.chunk;

/**
 * Слушатель переполнения палитры. Вызывается, когда палитра не может вместить
 * новый элемент и требует расширения (увеличения числа бит на элемент).
 */
public interface PaletteResizeListener<T> {

	/**
	 * Вызывается при переполнении палитры.
	 *
	 * @param newBits новое количество бит, необходимое для хранения
	 * @param object  объект, который не удалось добавить
	 * @return новый индекс объекта после расширения
	 */
	int onResize(int newBits, T object);

	/** Возвращает слушатель, который всегда бросает исключение — для неизменяемых палитр. */
	static <T> PaletteResizeListener<T> throwing() {
		return (newBits, object) -> {
			throw new IllegalArgumentException(
				"Unexpected palette resize, bits = " + newBits + ", added value = " + object
			);
		};
	}
}
