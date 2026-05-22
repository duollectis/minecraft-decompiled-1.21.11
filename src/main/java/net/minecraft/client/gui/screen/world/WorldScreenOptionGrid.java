package net.minecraft.client.gui.screen.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Сетка переключателей опций мира (структуры, бонусный сундук и т.д.).
 * Каждая опция — кнопка ON/OFF с текстовой меткой и опциональным описанием.
 */
@Environment(EnvType.CLIENT)
class WorldScreenOptionGrid {

	private static final int BUTTON_WIDTH = 44;
	private final List<WorldScreenOptionGrid.Option> options;
	private final LayoutWidget layout;

	WorldScreenOptionGrid(List<WorldScreenOptionGrid.Option> options, LayoutWidget layout) {
		this.options = options;
		this.layout = layout;
	}

	public LayoutWidget getLayout() {
		return layout;
	}

	public void refresh() {
		options.forEach(WorldScreenOptionGrid.Option::refresh);
	}

	public static WorldScreenOptionGrid.Builder builder(int width) {
		return new WorldScreenOptionGrid.Builder(width);
	}

	@Environment(EnvType.CLIENT)
	public static class Builder {

		final int width;
		private final List<WorldScreenOptionGrid.OptionBuilder> options = new ArrayList<>();
		int marginLeft;
		int rowSpacing = 4;
		int rows;
		Optional<WorldScreenOptionGrid.TooltipBoxDisplay> tooltipBoxDisplay = Optional.empty();

		public Builder(int width) {
			this.width = width;
		}

		void incrementRows() {
			this.rows++;
		}

		public WorldScreenOptionGrid.OptionBuilder add(Text text, BooleanSupplier getter, Consumer<Boolean> setter) {
			WorldScreenOptionGrid.OptionBuilder
					optionBuilder =
					new WorldScreenOptionGrid.OptionBuilder(text, getter, setter, BUTTON_WIDTH);
			this.options.add(optionBuilder);
			return optionBuilder;
		}

		public WorldScreenOptionGrid.Builder marginLeft(int marginLeft) {
			this.marginLeft = marginLeft;
			return this;
		}

		public WorldScreenOptionGrid.Builder setRowSpacing(int rowSpacing) {
			this.rowSpacing = rowSpacing;
			return this;
		}

		public WorldScreenOptionGrid build() {
			GridWidget gridWidget = new GridWidget().setRowSpacing(this.rowSpacing);
			gridWidget.add(EmptyWidget.ofWidth(this.width - BUTTON_WIDTH), 0, 0);
			gridWidget.add(EmptyWidget.ofWidth(BUTTON_WIDTH), 0, 1);
			List<WorldScreenOptionGrid.Option> list = new ArrayList<>();
			this.rows = 0;

			for (WorldScreenOptionGrid.OptionBuilder optionBuilder : this.options) {
				list.add(optionBuilder.build(this, gridWidget, 0));
			}

			gridWidget.refreshPositions();
			WorldScreenOptionGrid worldScreenOptionGrid = new WorldScreenOptionGrid(list, gridWidget);
			worldScreenOptionGrid.refresh();
			return worldScreenOptionGrid;
		}

		public WorldScreenOptionGrid.Builder withTooltipBox(int maxInfoRows, boolean alwaysMaxHeight) {
			this.tooltipBoxDisplay =
					Optional.of(new WorldScreenOptionGrid.TooltipBoxDisplay(maxInfoRows, alwaysMaxHeight));
			return this;
		}
	}

	@Environment(EnvType.CLIENT)
	record Option(CyclingButtonWidget<Boolean> button, BooleanSupplier getter, @Nullable BooleanSupplier toggleable) {

		public void refresh() {
			button.setValue(getter.getAsBoolean());
			if (toggleable != null) {
				button.active = toggleable.getAsBoolean();
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static class OptionBuilder {

		private final Text text;
		private final BooleanSupplier getter;
		private final Consumer<Boolean> setter;
		private @Nullable Text tooltip;
		private @Nullable BooleanSupplier toggleable;
		private final int buttonWidth;

		OptionBuilder(Text text, BooleanSupplier getter, Consumer<Boolean> setter, int buttonWidth) {
			this.text = text;
			this.getter = getter;
			this.setter = setter;
			this.buttonWidth = buttonWidth;
		}

		public WorldScreenOptionGrid.OptionBuilder toggleable(BooleanSupplier toggleable) {
			this.toggleable = toggleable;
			return this;
		}

		public WorldScreenOptionGrid.OptionBuilder tooltip(Text tooltip) {
			this.tooltip = tooltip;
			return this;
		}

		WorldScreenOptionGrid.Option build(WorldScreenOptionGrid.Builder gridBuilder, GridWidget gridWidget, int row) {
			gridBuilder.incrementRows();
			TextWidget textWidget = new TextWidget(text, MinecraftClient.getInstance().textRenderer);
			gridWidget.add(
					textWidget,
					gridBuilder.rows,
					row,
					gridWidget.copyPositioner().relative(0.0F, 0.5F).marginLeft(gridBuilder.marginLeft)
			);
			Optional<WorldScreenOptionGrid.TooltipBoxDisplay> tooltipBoxOpt = gridBuilder.tooltipBoxDisplay;
			CyclingButtonWidget.Builder<Boolean> builder = CyclingButtonWidget.onOffBuilder(getter.getAsBoolean());
			builder.omitKeyText();
			boolean hasTooltipOnly = tooltip != null && tooltipBoxOpt.isEmpty();

			if (hasTooltipOnly) {
				Tooltip tooltipWidget = Tooltip.of(tooltip);
				builder.tooltip(value -> tooltipWidget);
			}

			if (tooltip != null && !hasTooltipOnly) {
				builder.narration(button -> ScreenTexts.joinSentences(
						text,
						button.getGenericNarrationMessage(),
						tooltip
				));
			}
			else {
				builder.narration(button -> ScreenTexts.joinSentences(text, button.getGenericNarrationMessage()));
			}

			CyclingButtonWidget<Boolean> cyclingButton = builder.build(
					0, 0, buttonWidth, 20, Text.empty(), (button, value) -> setter.accept(value)
			);
			if (toggleable != null) {
				cyclingButton.active = toggleable.getAsBoolean();
			}

			gridWidget.add(cyclingButton, gridBuilder.rows, row + 1, gridWidget.copyPositioner().alignRight());
			if (tooltip != null) {
				tooltipBoxOpt.ifPresent(tooltipBoxDisplay -> {
					Text tooltipText = tooltip.copy().formatted(Formatting.GRAY);
					TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
					MultilineTextWidget multilineTextWidget = new MultilineTextWidget(tooltipText, textRenderer);
					multilineTextWidget.setMaxWidth(gridBuilder.width - gridBuilder.marginLeft - buttonWidth);
					multilineTextWidget.setMaxRows(tooltipBoxDisplay.maxInfoRows());
					gridBuilder.incrementRows();
					int bottomPadding = tooltipBoxDisplay.alwaysMaxHeight
							? 9 * tooltipBoxDisplay.maxInfoRows - multilineTextWidget.getHeight()
							: 0;
					gridWidget.add(
							multilineTextWidget,
							gridBuilder.rows,
							row,
							gridWidget.copyPositioner().marginTop(-gridBuilder.rowSpacing).marginBottom(bottomPadding)
					);
				});
			}

			return new WorldScreenOptionGrid.Option(cyclingButton, getter, toggleable);
		}
	}

	@Environment(EnvType.CLIENT)
	record TooltipBoxDisplay(int maxInfoRows, boolean alwaysMaxHeight) {
	}
}
