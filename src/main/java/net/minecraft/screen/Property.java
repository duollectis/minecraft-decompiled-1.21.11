package net.minecraft.screen;

/**
 * Отслеживаемое целочисленное свойство экрана, поддерживающее обнаружение изменений.
 * <p>
 * Используется для синхронизации числовых данных (прогресс плавки, заряд маяка и т.д.)
 * между сервером и клиентом через {@link ScreenHandler}. Хранит предыдущее значение
 * для определения факта изменения без лишних сетевых пакетов.
 */
public abstract class Property {

	private int previousValue;

	/**
	 * Создаёт свойство, делегирующее чтение/запись в {@link PropertyDelegate} по индексу.
	 *
	 * @param delegate делегат, хранящий массив свойств
	 * @param index    индекс конкретного свойства в делегате
	 * @return новый экземпляр {@code Property}
	 */
	public static Property create(PropertyDelegate delegate, int index) {
		return new Property() {
			@Override
			public int get() {
				return delegate.get(index);
			}

			@Override
			public void set(int value) {
				delegate.set(index, value);
			}
		};
	}

	/**
	 * Создаёт свойство, делегирующее чтение/запись в примитивный массив по индексу.
	 *
	 * @param array массив значений
	 * @param index индекс элемента в массиве
	 * @return новый экземпляр {@code Property}
	 */
	public static Property create(int[] array, int index) {
		return new Property() {
			@Override
			public int get() {
				return array[index];
			}

			@Override
			public void set(int value) {
				array[index] = value;
			}
		};
	}

	/**
	 * Создаёт автономное свойство с собственным хранилищем значения.
	 *
	 * @return новый экземпляр {@code Property}
	 */
	public static Property create() {
		return new Property() {
			private int value;

			@Override
			public int get() {
				return value;
			}

			@Override
			public void set(int value) {
				this.value = value;
			}
		};
	}

	public abstract int get();

	public abstract void set(int value);

	/**
	 * Проверяет, изменилось ли значение с момента последнего вызова этого метода.
	 * Побочный эффект: обновляет внутреннее «предыдущее значение» при каждом вызове.
	 *
	 * @return {@code true} если значение изменилось
	 */
	public boolean hasChanged() {
		int current = get();
		boolean changed = current != previousValue;
		previousValue = current;
		return changed;
	}
}
