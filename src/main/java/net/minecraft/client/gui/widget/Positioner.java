package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Интерфейс позиционера виджета в лейауте: задаёт отступы (margins) и
 * относительное выравнивание (0.0 = начало, 0.5 = центр, 1.0 = конец).
 * Все методы возвращают {@code this} для fluent-цепочек.
 *
 * <p>Создаётся через {@link #create()}, конкретная реализация — {@link Impl}.</p>
 */
@Environment(EnvType.CLIENT)
public interface Positioner {

	Positioner margin(int value);

	Positioner margin(int x, int y);

	Positioner margin(int left, int top, int right, int bottom);

	Positioner marginLeft(int marginLeft);

	Positioner marginTop(int marginTop);

	Positioner marginRight(int marginRight);

	Positioner marginBottom(int marginBottom);

	Positioner marginX(int marginX);

	Positioner marginY(int marginY);

	Positioner relative(float x, float y);

	Positioner relativeX(float relativeX);

	Positioner relativeY(float relativeY);

	default Positioner alignLeft() {
		return relativeX(0.0F);
	}

	default Positioner alignHorizontalCenter() {
		return relativeX(0.5F);
	}

	default Positioner alignRight() {
		return relativeX(1.0F);
	}

	default Positioner alignTop() {
		return relativeY(0.0F);
	}

	default Positioner alignVerticalCenter() {
		return relativeY(0.5F);
	}

	default Positioner alignBottom() {
		return relativeY(1.0F);
	}

	Positioner copy();

	Positioner.Impl toImpl();

	static Positioner create() {
		return new Positioner.Impl();
	}

	/**
	 * Конкретная реализация позиционера с изменяемыми полями отступов и выравнивания.
	 * Поля публичны для прямого доступа из лейаут-движка без лишних вызовов геттеров.
	 */
	@Environment(EnvType.CLIENT)
	class Impl implements Positioner {

		public int marginLeft;
		public int marginTop;
		public int marginRight;
		public int marginBottom;
		public float relativeX;
		public float relativeY;

		public Impl() {
		}

		public Impl(Positioner.Impl original) {
			marginLeft = original.marginLeft;
			marginTop = original.marginTop;
			marginRight = original.marginRight;
			marginBottom = original.marginBottom;
			relativeX = original.relativeX;
			relativeY = original.relativeY;
		}

		public Positioner.Impl margin(int value) {
			return margin(value, value);
		}

		public Positioner.Impl margin(int x, int y) {
			return marginX(x).marginY(y);
		}

		public Positioner.Impl margin(int left, int top, int right, int bottom) {
			return marginLeft(left).marginRight(right).marginTop(top).marginBottom(bottom);
		}

		public Positioner.Impl marginLeft(int value) {
			marginLeft = value;
			return this;
		}

		public Positioner.Impl marginTop(int value) {
			marginTop = value;
			return this;
		}

		public Positioner.Impl marginRight(int value) {
			marginRight = value;
			return this;
		}

		public Positioner.Impl marginBottom(int value) {
			marginBottom = value;
			return this;
		}

		public Positioner.Impl marginX(int value) {
			return marginLeft(value).marginRight(value);
		}

		public Positioner.Impl marginY(int value) {
			return marginTop(value).marginBottom(value);
		}

		public Positioner.Impl relative(float x, float y) {
			relativeX = x;
			relativeY = y;
			return this;
		}

		public Positioner.Impl relativeX(float value) {
			relativeX = value;
			return this;
		}

		public Positioner.Impl relativeY(float value) {
			relativeY = value;
			return this;
		}

		public Positioner.Impl copy() {
			return new Positioner.Impl(this);
		}

		@Override
		public Positioner.Impl toImpl() {
			return this;
		}
	}
}
