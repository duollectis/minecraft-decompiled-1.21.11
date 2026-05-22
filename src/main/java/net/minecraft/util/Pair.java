package net.minecraft.util;

/**
 * Изменяемая пара значений произвольных типов.
 * Используется там, где нужно передать два связанных значения без создания отдельного класса.
 *
 * @param <A> тип левого значения
 * @param <B> тип правого значения
 */
public class Pair<A, B> {

	private A left;
	private B right;

	public Pair(A left, B right) {
		this.left = left;
		this.right = right;
	}

	public A getLeft() {
		return left;
	}

	public void setLeft(A left) {
		this.left = left;
	}

	public B getRight() {
		return right;
	}

	public void setRight(B right) {
		this.right = right;
	}
}
