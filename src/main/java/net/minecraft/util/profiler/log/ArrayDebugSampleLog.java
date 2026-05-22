package net.minecraft.util.profiler.log;

/**
 * Базовая реализация {@link DebugSampleLog} на основе массива значений.
 * Хранит текущие значения по измерениям и сбрасывает их к значениям по умолчанию после каждого push.
 */
public abstract class ArrayDebugSampleLog implements DebugSampleLog {

	protected final long[] defaults;
	protected final long[] values;

	protected ArrayDebugSampleLog(int size, long[] defaults) {
		if (defaults.length != size) {
			throw new IllegalArgumentException("defaults have incorrect length of " + defaults.length);
		}

		this.values = new long[size];
		this.defaults = defaults;
	}

	@Override
	public void set(long[] newValues) {
		System.arraycopy(newValues, 0, values, 0, newValues.length);
		onPush();
		clearValues();
	}

	@Override
	public void push(long value) {
		values[0] = value;
		onPush();
		clearValues();
	}

	@Override
	public void push(long value, int column) {
		if (column < 1 || column >= values.length) {
			throw new IndexOutOfBoundsException(column + " out of bounds for dimensions " + values.length);
		}

		values[column] = value;
	}

	protected abstract void onPush();

	protected void clearValues() {
		System.arraycopy(defaults, 0, values, 0, defaults.length);
	}
}
