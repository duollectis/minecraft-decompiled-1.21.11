package net.minecraft.client.gui.widget;

import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.Updatable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Список виджетов настроек игры. Отображает опции попарно в две колонки,
 * поддерживает заголовки секций и применение отложенных значений слайдеров.
 * Используется на экранах настроек {@link GameOptionsScreen}.
 */
@Environment(EnvType.CLIENT)
public class OptionListWidget extends ElementListWidget<OptionListWidget.Component> {

	private static final int OPTION_WIDTH = 310;
	private static final int OPTION_HEIGHT = 25;

	private final GameOptionsScreen optionsScreen;

	public OptionListWidget(MinecraftClient client, int width, GameOptionsScreen optionsScreen) {
		super(client, width, optionsScreen.layout.getContentHeight(), optionsScreen.layout.getHeaderHeight(), OPTION_HEIGHT);
		centerListVertically = false;
		this.optionsScreen = optionsScreen;
	}

	public void addSingleOptionEntry(SimpleOption<?> option) {
		addEntry(OptionListWidget.WidgetEntry.create(client.options, option, optionsScreen));
	}

	public void addAll(SimpleOption<?>... options) {
		for (int index = 0; index < options.length; index += 2) {
			SimpleOption<?> second = index < options.length - 1 ? options[index + 1] : null;
			addEntry(OptionListWidget.WidgetEntry.create(client.options, options[index], second, optionsScreen));
		}
	}

	public void addAll(List<ClickableWidget> widgets) {
		for (int index = 0; index < widgets.size(); index += 2) {
			addWidgetEntry(widgets.get(index), index < widgets.size() - 1 ? widgets.get(index + 1) : null);
		}
	}

	public void addWidgetEntry(ClickableWidget firstWidget, @Nullable ClickableWidget secondWidget) {
		addEntry(OptionListWidget.WidgetEntry.create(firstWidget, secondWidget, optionsScreen));
	}

	public void addWidgetEntry(
			ClickableWidget firstWidget,
			SimpleOption<?> option,
			@Nullable ClickableWidget secondWidget
	) {
		addEntry(OptionListWidget.WidgetEntry.create(firstWidget, option, secondWidget, optionsScreen));
	}

	public void addHeader(Text title) {
		int lineHeight = 9;
		int topPadding = children().isEmpty() ? 0 : lineHeight * 2;
		addEntry(new OptionListWidget.Header(optionsScreen, title, topPadding), topPadding + lineHeight + 4);
	}

	@Override
	public int getRowWidth() {
		return OPTION_WIDTH;
	}

	public @Nullable ClickableWidget getWidgetFor(SimpleOption<?> option) {
		for (OptionListWidget.Component component : children()) {
			if (component instanceof OptionListWidget.WidgetEntry widgetEntry) {
				ClickableWidget widget = widgetEntry.getWidgetFor(option);
				if (widget != null) {
					return widget;
				}
			}
		}

		return null;
	}

	/**
	 * Применяет все отложенные значения слайдеров, которые ещё не были подтверждены.
	 * Вызывается при закрытии экрана настроек.
	 */
	public void applyAllPendingValues() {
		for (OptionListWidget.Component component : children()) {
			if (!(component instanceof OptionListWidget.WidgetEntry widgetEntry)) {
				continue;
			}

			for (OptionListWidget.OptionAssociatedWidget associated : widgetEntry.widgets) {
				if (associated.optionInstance() != null
						&& associated.widget() instanceof SimpleOption.OptionSliderWidgetImpl<?> slider) {
					slider.applyPendingValue();
				}
			}
		}
	}

	public void update(SimpleOption<?> simpleOption) {
		for (OptionListWidget.Component component : children()) {
			if (!(component instanceof OptionListWidget.WidgetEntry widgetEntry)) {
				continue;
			}

			for (OptionListWidget.OptionAssociatedWidget associated : widgetEntry.widgets) {
				if (associated.optionInstance() == simpleOption
						&& associated.widget() instanceof Updatable updatable) {
					updatable.update();
					return;
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	protected abstract static class Component extends ElementListWidget.Entry<OptionListWidget.Component> {
	}

	@Environment(EnvType.CLIENT)
	protected static class Header extends OptionListWidget.Component {

		private final Screen parent;
		private final int yOffset;
		private final TextWidget title;

		protected Header(Screen parent, Text title, int yOffset) {
			this.parent = parent;
			this.yOffset = yOffset;
			this.title = new TextWidget(title, parent.getTextRenderer());
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return List.of(title);
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			title.setPosition(parent.width / 2 - 155, getContentY() + yOffset);
			title.render(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public List<? extends Element> children() {
			return List.of(title);
		}
	}

	/**
	 * Запись, связывающая виджет с конкретной опцией {@link SimpleOption}.
	 * Используется для поиска виджета по опции и применения отложенных значений.
	 */
	@Environment(EnvType.CLIENT)
	public record OptionAssociatedWidget(ClickableWidget widget, @Nullable SimpleOption<?> optionInstance) {

		public OptionAssociatedWidget(ClickableWidget widget) {
			this(widget, null);
		}
	}

	@Environment(EnvType.CLIENT)
	protected static class WidgetEntry extends OptionListWidget.Component {

		private static final int WIDGET_X_SPACING = 160;

		final List<OptionListWidget.OptionAssociatedWidget> widgets;
		private final Screen screen;

		private WidgetEntry(List<OptionListWidget.OptionAssociatedWidget> widgets, Screen screen) {
			this.widgets = widgets;
			this.screen = screen;
		}

		public static OptionListWidget.WidgetEntry create(GameOptions options, SimpleOption<?> option, Screen screen) {
			return new OptionListWidget.WidgetEntry(
					List.of(new OptionListWidget.OptionAssociatedWidget(
							option.createWidget(options, 0, 0, OPTION_WIDTH),
							option
					)),
					screen
			);
		}

		public static OptionListWidget.WidgetEntry create(
				ClickableWidget firstWidget,
				@Nullable ClickableWidget secondWidget,
				Screen screen
		) {
			return secondWidget == null
					? new OptionListWidget.WidgetEntry(
							List.of(new OptionListWidget.OptionAssociatedWidget(firstWidget)),
							screen
					)
					: new OptionListWidget.WidgetEntry(
							List.of(
									new OptionListWidget.OptionAssociatedWidget(firstWidget),
									new OptionListWidget.OptionAssociatedWidget(secondWidget)
							),
							screen
					);
		}

		public static OptionListWidget.WidgetEntry create(
				ClickableWidget firstWidget,
				SimpleOption<?> option,
				@Nullable ClickableWidget secondWidget,
				Screen screen
		) {
			return secondWidget == null
					? new OptionListWidget.WidgetEntry(
							List.of(new OptionListWidget.OptionAssociatedWidget(firstWidget, option)),
							screen
					)
					: new OptionListWidget.WidgetEntry(
							List.of(
									new OptionListWidget.OptionAssociatedWidget(firstWidget, option),
									new OptionListWidget.OptionAssociatedWidget(secondWidget)
							),
							screen
					);
		}

		public static OptionListWidget.WidgetEntry create(
				GameOptions options,
				SimpleOption<?> firstOption,
				@Nullable SimpleOption<?> secondOption,
				GameOptionsScreen screen
		) {
			ClickableWidget firstWidget = firstOption.createWidget(options);
			return secondOption == null
					? new OptionListWidget.WidgetEntry(
							List.of(new OptionListWidget.OptionAssociatedWidget(firstWidget, firstOption)),
							screen
					)
					: new OptionListWidget.WidgetEntry(
							List.of(
									new OptionListWidget.OptionAssociatedWidget(firstWidget, firstOption),
									new OptionListWidget.OptionAssociatedWidget(secondOption.createWidget(options), secondOption)
							),
							screen
					);
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int xOffset = 0;
			int baseX = screen.width / 2 - 155;

			for (OptionListWidget.OptionAssociatedWidget associated : widgets) {
				associated.widget().setPosition(baseX + xOffset, getContentY());
				associated.widget().render(context, mouseX, mouseY, deltaTicks);
				xOffset += WIDGET_X_SPACING;
			}
		}

		@Override
		public List<? extends Element> children() {
			return Lists.transform(widgets, OptionListWidget.OptionAssociatedWidget::widget);
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return Lists.transform(widgets, OptionListWidget.OptionAssociatedWidget::widget);
		}

		public @Nullable ClickableWidget getWidgetFor(SimpleOption<?> option) {
			for (OptionListWidget.OptionAssociatedWidget associated : widgets) {
				if (associated.optionInstance == option) {
					return associated.widget();
				}
			}

			return null;
		}
	}
}
