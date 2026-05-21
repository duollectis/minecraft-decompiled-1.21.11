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

@Environment(EnvType.CLIENT)
/**
 * {@code OptionListWidget}.
 */
public class OptionListWidget extends ElementListWidget<OptionListWidget.Component> {

	private static final int OPTION_WIDTH = 310;
	private static final int OPTION_HEIGHT = 25;
	private final GameOptionsScreen optionsScreen;

	public OptionListWidget(MinecraftClient client, int width, GameOptionsScreen optionsScreen) {
		super(client, width, optionsScreen.layout.getContentHeight(), optionsScreen.layout.getHeaderHeight(), 25);
		this.centerListVertically = false;
		this.optionsScreen = optionsScreen;
	}

	public void addSingleOptionEntry(SimpleOption<?> option) {
		this.addEntry(OptionListWidget.WidgetEntry.create(this.client.options, option, this.optionsScreen));
	}

	public void addAll(SimpleOption<?>... options) {
		for (int i = 0; i < options.length; i += 2) {
			SimpleOption<?> simpleOption = i < options.length - 1 ? options[i + 1] : null;
			this.addEntry(OptionListWidget.WidgetEntry.create(
					this.client.options,
					options[i],
					simpleOption,
					this.optionsScreen
			));
		}
	}

	public void addAll(List<ClickableWidget> widgets) {
		for (int i = 0; i < widgets.size(); i += 2) {
			this.addWidgetEntry(widgets.get(i), i < widgets.size() - 1 ? widgets.get(i + 1) : null);
		}
	}

	public void addWidgetEntry(ClickableWidget firstWidget, @Nullable ClickableWidget secondWidget) {
		this.addEntry(OptionListWidget.WidgetEntry.create(firstWidget, secondWidget, this.optionsScreen));
	}

	public void addWidgetEntry(
			ClickableWidget firstWidget,
			SimpleOption<?> option,
			@Nullable ClickableWidget secondWidget
	) {
		this.addEntry(OptionListWidget.WidgetEntry.create(firstWidget, option, secondWidget, this.optionsScreen));
	}

	public void addHeader(Text title) {
		int i = 9;
		int j = this.children().isEmpty() ? 0 : i * 2;
		this.addEntry(new OptionListWidget.Header(this.optionsScreen, title, j), j + i + 4);
	}

	@Override
	public int getRowWidth() {
		return 310;
	}

	public @Nullable ClickableWidget getWidgetFor(SimpleOption<?> option) {
		for (OptionListWidget.Component component : this.children()) {
			if (component instanceof OptionListWidget.WidgetEntry widgetEntry) {
				ClickableWidget clickableWidget = widgetEntry.getWidgetFor(option);
				if (clickableWidget != null) {
					return clickableWidget;
				}
			}
		}

		return null;
	}

	public void applyAllPendingValues() {
		for (OptionListWidget.Component component : this.children()) {
			if (component instanceof OptionListWidget.WidgetEntry widgetEntry) {
				for (OptionListWidget.OptionAssociatedWidget optionAssociatedWidget : widgetEntry.widgets) {
					if (optionAssociatedWidget.optionInstance() != null
							&& optionAssociatedWidget.widget() instanceof SimpleOption.OptionSliderWidgetImpl<?> optionSliderWidgetImpl) {
						optionSliderWidgetImpl.applyPendingValue();
					}
				}
			}
		}
	}

	public void update(SimpleOption<?> simpleOption) {
		for (OptionListWidget.Component component : this.children()) {
			if (component instanceof OptionListWidget.WidgetEntry widgetEntry) {
				for (OptionListWidget.OptionAssociatedWidget optionAssociatedWidget : widgetEntry.widgets) {
					if (optionAssociatedWidget.optionInstance() == simpleOption
							&& optionAssociatedWidget.widget() instanceof Updatable updatable) {
						updatable.update();
						return;
					}
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Component}.
	 */
	protected abstract static class Component extends ElementListWidget.Entry<OptionListWidget.Component> {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Header}.
	 */
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
			return List.of(this.title);
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			this.title.setPosition(this.parent.width / 2 - 155, this.getContentY() + this.yOffset);
			this.title.render(context, mouseX, mouseY, deltaTicks);
		}

		@Override
		public List<? extends Element> children() {
			return List.of(this.title);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code OptionAssociatedWidget}.
	 */
	public record OptionAssociatedWidget(ClickableWidget widget, @Nullable SimpleOption<?> optionInstance) {

		public OptionAssociatedWidget(ClickableWidget widget) {
			this(widget, null);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code WidgetEntry}.
	 */
	protected static class WidgetEntry extends OptionListWidget.Component {

		final List<OptionListWidget.OptionAssociatedWidget> widgets;
		private final Screen screen;
		private static final int WIDGET_X_SPACING = 160;

		private WidgetEntry(List<OptionListWidget.OptionAssociatedWidget> widgets, Screen screen) {
			this.widgets = widgets;
			this.screen = screen;
		}

		public static OptionListWidget.WidgetEntry create(GameOptions options, SimpleOption<?> option, Screen screen) {
			return new OptionListWidget.WidgetEntry(
					List.of(new OptionListWidget.OptionAssociatedWidget(
							option.createWidget(
									options,
									0,
									0,
									310
							), option
					)), screen
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
					       ), screen
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
					List.of(new OptionListWidget.OptionAssociatedWidget(
							firstWidget,
							option
					)), screen
			)
			       : new OptionListWidget.WidgetEntry(
					       List.of(
							       new OptionListWidget.OptionAssociatedWidget(firstWidget, option),
							       new OptionListWidget.OptionAssociatedWidget(secondWidget)
					       ), screen
			       );
		}

		public static OptionListWidget.WidgetEntry create(
				GameOptions options,
				SimpleOption<?> firstOption,
				@Nullable SimpleOption<?> secondOption,
				GameOptionsScreen screen
		) {
			ClickableWidget clickableWidget = firstOption.createWidget(options);
			return secondOption == null
			       ? new OptionListWidget.WidgetEntry(
					List.of(new OptionListWidget.OptionAssociatedWidget(
							clickableWidget,
							firstOption
					)), screen
			)
			       : new OptionListWidget.WidgetEntry(
					       List.of(
							       new OptionListWidget.OptionAssociatedWidget(clickableWidget, firstOption),
							       new OptionListWidget.OptionAssociatedWidget(
									       secondOption.createWidget(options),
									       secondOption
							       )
					       ),
					       screen
			       );
		}

		@Override
		public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			int i = 0;
			int j = this.screen.width / 2 - 155;

			for (OptionListWidget.OptionAssociatedWidget optionAssociatedWidget : this.widgets) {
				optionAssociatedWidget.widget().setPosition(j + i, this.getContentY());
				optionAssociatedWidget.widget().render(context, mouseX, mouseY, deltaTicks);
				i += 160;
			}
		}

		@Override
		public List<? extends Element> children() {
			return Lists.transform(this.widgets, OptionListWidget.OptionAssociatedWidget::widget);
		}

		@Override
		public List<? extends Selectable> selectableChildren() {
			return Lists.transform(this.widgets, OptionListWidget.OptionAssociatedWidget::widget);
		}

		public @Nullable ClickableWidget getWidgetFor(SimpleOption<?> option) {
			for (OptionListWidget.OptionAssociatedWidget optionAssociatedWidget : this.widgets) {
				if (optionAssociatedWidget.optionInstance == option) {
					return optionAssociatedWidget.widget();
				}
			}

			return null;
		}
	}
}
