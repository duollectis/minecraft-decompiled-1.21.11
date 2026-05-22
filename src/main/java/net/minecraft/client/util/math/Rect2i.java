package net.minecraft.client.util.math;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Изменяемый прямоугольник в целочисленных координатах экрана.
 * Поддерживает вычисление пересечения с другим прямоугольником на месте.
 */
@Environment(EnvType.CLIENT)
public class Rect2i {

	private int x;
	private int y;
	private int width;
	private int height;

	public Rect2i(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	/**
	 * Вычисляет пересечение этого прямоугольника с другим, изменяя текущий объект.
	 *
	 * @param rect прямоугольник для пересечения
	 * @return {@code this} для цепочки вызовов
	 */
	public Rect2i intersection(Rect2i rect) {
		int thisRight = x + width;
		int thisBottom = y + height;
		int otherRight = rect.getX() + rect.getWidth();
		int otherBottom = rect.getY() + rect.getHeight();
		x = Math.max(x, rect.getX());
		y = Math.max(y, rect.getY());
		width = Math.max(0, Math.min(thisRight, otherRight) - x);
		height = Math.max(0, Math.min(thisBottom, otherBottom) - y);
		return this;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setStartPos(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public boolean contains(int pointX, int pointY) {
		return pointX >= x && pointX <= x + width && pointY >= y && pointY <= y + height;
	}
}
