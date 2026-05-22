package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Виджет-контейнер, позиционирующий дочерние элементы внутри себя
 * с помощью относительных координат через {@link Positioner}.
 * <p>
 * По умолчанию все добавленные виджеты центрируются (relative 0.5, 0.5).
 * Размер контейнера автоматически вычисляется как максимум из {@code minWidth}/{@code minHeight}
 * и размеров всех дочерних элементов.
 */
@Environment(EnvType.CLIENT)
public class SimplePositioningWidget extends WrapperWidget {

	private final List<SimplePositioningWidget.Element> elements = new ArrayList<>();
	private int minWidth;
	private int minHeight;
	private final Positioner mainPositioner = Positioner.create().relative(0.5F, 0.5F);

	public SimplePositioningWidget() {
		this(0, 0, 0, 0);
	}

	public SimplePositioningWidget(int width, int height) {
		this(0, 0, width, height);
	}

	public SimplePositioningWidget(int x, int y, int width, int height) {
		super(x, y, width, height);
		setDimensions(width, height);
	}

	public SimplePositioningWidget setDimensions(int minWidth, int minHeight) {
		return setMinWidth(minWidth).setMinHeight(minHeight);
	}

	public SimplePositioningWidget setMinHeight(int minHeight) {
		this.minHeight = minHeight;
		return this;
	}

	public SimplePositioningWidget setMinWidth(int minWidth) {
		this.minWidth = minWidth;
		return this;
	}

	public Positioner copyPositioner() {
		return mainPositioner.copy();
	}

	public Positioner getMainPositioner() {
		return mainPositioner;
	}

	@Override
	public void refreshPositions() {
		super.refreshPositions();
		int computedWidth = minWidth;
		int computedHeight = minHeight;

		for (SimplePositioningWidget.Element element : elements) {
			computedWidth = Math.max(computedWidth, element.getWidth());
			computedHeight = Math.max(computedHeight, element.getHeight());
		}

		for (SimplePositioningWidget.Element element : elements) {
			element.setX(getX(), computedWidth);
			element.setY(getY(), computedHeight);
		}

		width = computedWidth;
		height = computedHeight;
	}

	public <T extends Widget> T add(T widget) {
		return add(widget, copyPositioner());
	}

	public <T extends Widget> T add(T widget, Positioner positioner) {
		elements.add(new SimplePositioningWidget.Element(widget, positioner));
		return widget;
	}

	public <T extends Widget> T add(T widget, Consumer<Positioner> callback) {
		return add(widget, Util.make(copyPositioner(), callback));
	}

	@Override
	public void forEachElement(Consumer<Widget> consumer) {
		elements.forEach(element -> consumer.accept(element.widget));
	}

	public static void setPos(Widget widget, int left, int top, int right, int bottom) {
		setPos(widget, left, top, right, bottom, 0.5F, 0.5F);
	}

	public static void setPos(Widget widget, ScreenRect rect) {
		setPos(widget, rect.position().x(), rect.position().y(), rect.width(), rect.height());
	}

	public static void setPos(Widget widget, ScreenRect rect, float relativeX, float relativeY) {
		setPos(widget, rect.getLeft(), rect.getTop(), rect.width(), rect.height(), relativeX, relativeY);
	}

	public static void setPos(
			Widget widget,
			int left,
			int top,
			int right,
			int bottom,
			float relativeX,
			float relativeY
	) {
		setPos(left, right, widget.getWidth(), widget::setX, relativeX);
		setPos(top, bottom, widget.getHeight(), widget::setY, relativeY);
	}

	/**
	 * Вычисляет позицию виджета внутри диапазона {@code [low, low + high - length]} по относительной
	 * координате {@code relative} (0.0 = начало, 1.0 = конец) и передаёт результат в {@code setter}.
	 */
	public static void setPos(int low, int high, int length, Consumer<Integer> setter, float relative) {
		int offset = (int) MathHelper.lerp(relative, 0.0F, (float) (high - length));
		setter.accept(low + offset);
	}

	@Environment(EnvType.CLIENT)
	static class Element extends WrapperWidget.WrappedElement {

		protected Element(Widget widget, Positioner positioner) {
			super(widget, positioner);
		}
	}
}
