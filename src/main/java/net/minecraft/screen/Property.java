package net.minecraft.screen;

/**
 * {@code Property}.
 */
public abstract class Property {

	private int oldValue;

	/**
	 * Create.
	 *
	 * @param delegate delegate
	 * @param index index
	 *
	 * @return Property — результат операции
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
	 * Create.
	 *
	 * @param array array
	 * @param index index
	 *
	 * @return Property — результат операции
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
	 * Create.
	 *
	 * @return Property — результат операции
	 */
	public static Property create() {
		return new Property() {
			private int value;

			@Override
			public int get() {
				return this.value;
			}

			@Override
			public void set(int value) {
				this.value = value;
			}
		};
	}

	/**
	 * Get.
	 *
	 * @return int — 
	 */
	public abstract int get();

	/**
	 * Set.
	 *
	 * @param value value
	 */
	public abstract void set(int value);

	public boolean hasChanged() {
		int i = this.get();
		boolean bl = i != this.oldValue;
		this.oldValue = i;
		return bl;
	}
}
