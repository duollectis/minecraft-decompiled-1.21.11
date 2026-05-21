package net.minecraft.util.shape;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;

/**
 * {@code FractionalDoubleList}.
 */
public class FractionalDoubleList extends AbstractDoubleList {

	private final int sectionCount;

	public FractionalDoubleList(int sectionCount) {
		if (sectionCount <= 0) {
			throw new IllegalArgumentException("Need at least 1 part");
		}
		else {
			this.sectionCount = sectionCount;
		}
	}

	public double getDouble(int position) {
		return (double) position / this.sectionCount;
	}

	/**
	 * Size.
	 *
	 * @return int — результат операции
	 */
	public int size() {
		return this.sectionCount + 1;
	}
}
