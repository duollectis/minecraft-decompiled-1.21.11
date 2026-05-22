package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.util.math.Divider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Виджет-контейнер, который выравнивает дочерние элементы вдоль одной оси (горизонтальной или вертикальной),
 * равномерно распределяя оставшееся пространство между ними с помощью {@link Divider}.
 * Размер по перпендикулярной оси подстраивается под наибольший дочерний элемент.
 */
@Environment(EnvType.CLIENT)
public class AxisGridWidget extends WrapperWidget {

	private final AxisGridWidget.DisplayAxis axis;
	private final List<AxisGridWidget.Element> elements = new ArrayList<>();
	private final Positioner mainPositioner = Positioner.create();

	public AxisGridWidget(int width, int height, AxisGridWidget.DisplayAxis axis) {
		this(0, 0, width, height, axis);
	}

	public AxisGridWidget(int x, int y, int width, int height, AxisGridWidget.DisplayAxis axis) {
		super(x, y, width, height);
		this.axis = axis;
	}

	@Override
	public void refreshPositions() {
		super.refreshPositions();
		if (elements.isEmpty()) {
			return;
		}

		int totalSameAxis = 0;
		int maxOtherAxis = axis.getOtherAxisLength(this);

		for (AxisGridWidget.Element element : elements) {
			totalSameAxis += axis.getSameAxisLength(element);
			maxOtherAxis = Math.max(maxOtherAxis, axis.getOtherAxisLength(element));
		}

		int remaining = axis.getSameAxisLength(this) - totalSameAxis;
		int cursor = axis.getSameAxisCoordinate(this);
		Iterator<AxisGridWidget.Element> iterator = elements.iterator();
		AxisGridWidget.Element first = iterator.next();
		axis.setSameAxisCoordinate(first, cursor);
		cursor += axis.getSameAxisLength(first);

		if (elements.size() >= 2) {
			Divider divider = new Divider(remaining, elements.size() - 1);
			while (divider.hasNext()) {
				cursor += divider.nextInt();
				AxisGridWidget.Element next = iterator.next();
				axis.setSameAxisCoordinate(next, cursor);
				cursor += axis.getSameAxisLength(next);
			}
		}

		int otherOrigin = axis.getOtherAxisCoordinate(this);
		for (AxisGridWidget.Element element : elements) {
			axis.setOtherAxisCoordinate(element, otherOrigin, maxOtherAxis);
		}

		switch (axis) {
			case HORIZONTAL -> this.height = maxOtherAxis;
			case VERTICAL -> this.width = maxOtherAxis;
		}
	}

	@Override
	public void forEachElement(Consumer<Widget> consumer) {
		elements.forEach(element -> consumer.accept(element.widget));
	}

	public Positioner copyPositioner() {
		return mainPositioner.copy();
	}

	public Positioner getMainPositioner() {
		return mainPositioner;
	}

	public <T extends Widget> T add(T widget) {
		return add(widget, copyPositioner());
	}

	public <T extends Widget> T add(T widget, Positioner positioner) {
		elements.add(new AxisGridWidget.Element(widget, positioner));
		return widget;
	}

	public <T extends Widget> T add(T widget, Consumer<Positioner> callback) {
		return add(widget, Util.make(copyPositioner(), callback));
	}

	/**
	 * Ось, вдоль которой выравниваются дочерние элементы.
	 * Методы {@code getSameAxisLength} / {@code getOtherAxisLength} абстрагируют
	 * работу с шириной/высотой в зависимости от выбранной оси.
	 */
	@Environment(EnvType.CLIENT)
	public enum DisplayAxis {
		HORIZONTAL,
		VERTICAL;

		int getSameAxisLength(Widget widget) {
			return switch (this) {
				case HORIZONTAL -> widget.getWidth();
				case VERTICAL -> widget.getHeight();
			};
		}

		int getSameAxisLength(AxisGridWidget.Element element) {
			return switch (this) {
				case HORIZONTAL -> element.getWidth();
				case VERTICAL -> element.getHeight();
			};
		}

		int getOtherAxisLength(Widget widget) {
			return switch (this) {
				case HORIZONTAL -> widget.getHeight();
				case VERTICAL -> widget.getWidth();
			};
		}

		int getOtherAxisLength(AxisGridWidget.Element element) {
			return switch (this) {
				case HORIZONTAL -> element.getHeight();
				case VERTICAL -> element.getWidth();
			};
		}

		void setSameAxisCoordinate(AxisGridWidget.Element element, int position) {
			switch (this) {
				case HORIZONTAL -> element.setX(position, element.getWidth());
				case VERTICAL -> element.setY(position, element.getHeight());
			}
		}

		void setOtherAxisCoordinate(AxisGridWidget.Element element, int origin, int size) {
			switch (this) {
				case HORIZONTAL -> element.setY(origin, size);
				case VERTICAL -> element.setX(origin, size);
			}
		}

		int getSameAxisCoordinate(Widget widget) {
			return switch (this) {
				case HORIZONTAL -> widget.getX();
				case VERTICAL -> widget.getY();
			};
		}

		int getOtherAxisCoordinate(Widget widget) {
			return switch (this) {
				case HORIZONTAL -> widget.getY();
				case VERTICAL -> widget.getX();
			};
		}
	}

	@Environment(EnvType.CLIENT)
	static class Element extends WrapperWidget.WrappedElement {

		protected Element(Widget widget, Positioner positioner) {
			super(widget, positioner);
		}
	}
}
