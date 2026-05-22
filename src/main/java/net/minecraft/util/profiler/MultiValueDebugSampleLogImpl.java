package net.minecraft.util.profiler;

import net.minecraft.util.profiler.log.ArrayDebugSampleLog;
import net.minecraft.util.profiler.log.MultiValueDebugSampleLog;

/**
 * Кольцевой буфер многомерных отладочных сэмплов фиксированного размера {@link #LOG_SIZE}.
 * Каждый элемент — массив значений по измерениям (dimensions).
 */
public class MultiValueDebugSampleLogImpl extends ArrayDebugSampleLog implements MultiValueDebugSampleLog {

	public static final int LOG_SIZE = 240;

	private final long[][] multiValues;
	private int start;
	private int length;

	public MultiValueDebugSampleLogImpl(int dimensions) {
		this(dimensions, new long[dimensions]);
	}

	public MultiValueDebugSampleLogImpl(int dimensions, long[] defaults) {
		super(dimensions, defaults);
		this.multiValues = new long[LOG_SIZE][dimensions];
	}

	@Override
	protected void onPush() {
		int writeIndex = wrap(start + length);
		System.arraycopy(values, 0, multiValues[writeIndex], 0, values.length);

		if (length < LOG_SIZE) {
			length++;
		}
		else {
			start = wrap(start + 1);
		}
	}

	@Override
	public int getDimension() {
		return multiValues.length;
	}

	@Override
	public int getLength() {
		return length;
	}

	@Override
	public long get(int index) {
		return get(index, 0);
	}

	@Override
	public long get(int index, int dimension) {
		if (index < 0 || index >= length) {
			throw new IndexOutOfBoundsException(index + " out of bounds for length " + length);
		}

		long[] row = multiValues[wrap(start + index)];

		if (dimension < 0 || dimension >= row.length) {
			throw new IndexOutOfBoundsException(dimension + " out of bounds for dimensions " + row.length);
		}

		return row[dimension];
	}

	private int wrap(int index) {
		return index % LOG_SIZE;
	}

	@Override
	public void clear() {
		start = 0;
		length = 0;
	}
}
