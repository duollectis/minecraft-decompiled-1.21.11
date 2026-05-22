package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Consumer;

/**
 * Невидимый виджет-заглушка, занимающий пространство в лейауте без отображения.
 * Используется для создания отступов и выравнивания в {@link LayoutWidget}.
 * Фабричные методы {@link #ofWidth} и {@link #ofHeight} удобны для горизонтальных
 * и вертикальных разделителей соответственно.
 */
@Environment(EnvType.CLIENT)
public class EmptyWidget implements Widget {

	private int x;
	private int y;
	private final int width;
	private final int height;

	public EmptyWidget(int width, int height) {
		this(0, 0, width, height);
	}

	public EmptyWidget(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	/** Создаёт горизонтальный разделитель заданной ширины с нулевой высотой. */
	public static EmptyWidget ofWidth(int width) {
		return new EmptyWidget(width, 0);
	}

	/** Создаёт вертикальный разделитель заданной высоты с нулевой шириной. */
	public static EmptyWidget ofHeight(int height) {
		return new EmptyWidget(0, height);
	}

	@Override
	public void setX(int x) {
		this.x = x;
	}

	@Override
	public void setY(int y) {
		this.y = y;
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void forEachChild(Consumer<ClickableWidget> consumer) {
	}
}
