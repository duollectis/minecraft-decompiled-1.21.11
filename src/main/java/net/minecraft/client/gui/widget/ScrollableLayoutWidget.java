package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.screen.ScreenTexts;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Виджет-обёртка над произвольным {@link LayoutWidget}, добавляющий вертикальную прокрутку.
 * <p>
 * Внутренний {@link Container} ограничивает видимую область по высоте и рисует полосу прокрутки.
 * Ширина контейнера автоматически расширяется на {@link #SCROLLBAR_WIDTH} пикселей, чтобы
 * оставить место для скроллбара, не перекрывая содержимое.
 */
@Environment(EnvType.CLIENT)
public class ScrollableLayoutWidget implements LayoutWidget {

	private static final int SCROLLBAR_WIDTH = 4;
	/** Смещение содержимого по X, чтобы скроллбар не перекрывал виджеты. */
	private static final int CONTENT_X_OFFSET = 10;
	private static final double SCROLL_SPEED = 10.0;

	final LayoutWidget layout;
	private final ScrollableLayoutWidget.Container container;
	private int width;
	private int height;

	public ScrollableLayoutWidget(MinecraftClient client, LayoutWidget layout, int height) {
		this.layout = layout;
		this.container = new ScrollableLayoutWidget.Container(client, 0, height);
	}

	public void setWidth(int width) {
		this.width = width;
		container.setWidth(Math.max(layout.getWidth(), width));
	}

	public void setHeight(int height) {
		this.height = height;
		container.setHeight(Math.min(layout.getHeight(), height));
		container.refreshScroll();
	}

	@Override
	public void refreshPositions() {
		layout.refreshPositions();
		int layoutWidth = layout.getWidth();
		container.setWidth(Math.max(layoutWidth + CONTENT_X_OFFSET * 2, width));
		container.setHeight(Math.min(layout.getHeight(), height));
		container.refreshScroll();
	}

	@Override
	public void forEachElement(Consumer<Widget> consumer) {
		consumer.accept(container);
	}

	@Override
	public void setX(int x) {
		container.setX(x);
	}

	@Override
	public void setY(int y) {
		container.setY(y);
	}

	@Override
	public int getX() {
		return container.getX();
	}

	@Override
	public int getY() {
		return container.getY();
	}

	@Override
	public int getWidth() {
		return container.getWidth();
	}

	@Override
	public int getHeight() {
		return container.getHeight();
	}

	/**
	 * Внутренний контейнер, реализующий прокрутку содержимого {@link LayoutWidget}.
	 * <p>
	 * При фокусировке дочернего элемента через клавиатуру автоматически прокручивает
	 * область видимости так, чтобы сфокусированный виджет оказался в поле зрения.
	 */
	@Environment(EnvType.CLIENT)
	class Container extends ContainerWidget {

		private final MinecraftClient client;
		private final List<ClickableWidget> children = new ArrayList<>();

		public Container(final MinecraftClient client, final int width, final int height) {
			super(0, 0, width, height, ScreenTexts.EMPTY);
			this.client = client;
			ScrollableLayoutWidget.this.layout.forEachChild(children::add);
		}

		@Override
		protected int getContentsHeightWithPadding() {
			return ScrollableLayoutWidget.this.layout.getHeight();
		}

		@Override
		protected double getDeltaYPerScroll() {
			return SCROLL_SPEED;
		}

		@Override
		protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			context.enableScissor(getX(), getY(), getX() + width, getY() + height);

			for (ClickableWidget child : children) {
				child.render(context, mouseX, mouseY, deltaTicks);
			}

			context.disableScissor();
			drawScrollbar(context, mouseX, mouseY);
		}

		@Override
		protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		}

		@Override
		public ScreenRect getBorder(NavigationDirection direction) {
			return new ScreenRect(getX(), getY(), width, getContentsHeightWithPadding());
		}

		/**
		 * При фокусировке дочернего элемента через клавиатуру прокручивает список,
		 * чтобы сфокусированный виджет оказался в видимой области.
		 */
		@Override
		public void setFocused(@Nullable Element focused) {
			super.setFocused(focused);

			if (focused == null || !client.getNavigationType().isKeyboard()) {
				return;
			}

			ScreenRect containerRect = getNavigationFocus();
			ScreenRect focusedRect = focused.getNavigationFocus();
			int topDelta = focusedRect.getTop() - containerRect.getTop();
			int bottomDelta = focusedRect.getBottom() - containerRect.getBottom();

			if (topDelta < 0) {
				setScrollY(getScrollY() + topDelta - CONTENT_X_OFFSET);
			}
			else if (bottomDelta > 0) {
				setScrollY(getScrollY() + bottomDelta + CONTENT_X_OFFSET);
			}
		}

		@Override
		public void setX(int x) {
			super.setX(x);
			// Смещаем содержимое вправо, чтобы скроллбар не перекрывал виджеты
			ScrollableLayoutWidget.this.layout.setX(x + CONTENT_X_OFFSET);
		}

		@Override
		public void setY(int y) {
			super.setY(y);
			ScrollableLayoutWidget.this.layout.setY(y - (int) getScrollY());
		}

		@Override
		public void setScrollY(double scrollY) {
			super.setScrollY(scrollY);
			ScrollableLayoutWidget.this.layout.setY(getNavigationFocus().getTop() - (int) getScrollY());
		}

		@Override
		public List<? extends Element> children() {
			return children;
		}

		@Override
		public Collection<? extends Selectable> getNarratedParts() {
			return children;
		}
	}
}
