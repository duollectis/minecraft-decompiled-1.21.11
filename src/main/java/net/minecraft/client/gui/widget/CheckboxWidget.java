package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;

/**
 * Виджет флажка (checkbox) с поддержкой многострочного текста, тултипа и привязки к {@link SimpleOption}.
 */
@Environment(EnvType.CLIENT)
public class CheckboxWidget extends PressableWidget {

	private static final Identifier
			SELECTED_HIGHLIGHTED_TEXTURE =
			Identifier.ofVanilla("widget/checkbox_selected_highlighted");
	private static final Identifier SELECTED_TEXTURE = Identifier.ofVanilla("widget/checkbox_selected");
	private static final Identifier HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("widget/checkbox_highlighted");
	private static final Identifier TEXTURE = Identifier.ofVanilla("widget/checkbox");
	private static final int CHECKBOX_PADDING = 4;
	private static final int CHECKBOX_SIZE = 8;
	private boolean checked;
	private final CheckboxWidget.Callback callback;
	private final MultilineTextWidget textWidget;

	CheckboxWidget(
			int x,
			int y,
			int maxWidth,
			Text message,
			TextRenderer textRenderer,
			boolean checked,
			CheckboxWidget.Callback callback
	) {
		super(x, y, 0, 0, message);
		this.textWidget = new MultilineTextWidget(message, textRenderer);
		this.textWidget.setMaxRows(2);
		this.width = this.setMaxWidth(maxWidth, textRenderer);
		this.height = this.calculateHeight(textRenderer);
		this.checked = checked;
		this.callback = callback;
	}

	public int setMaxWidth(int max, TextRenderer textRenderer) {
		this.width = this.calculateWidth(max, this.getMessage(), textRenderer);
		this.textWidget.setMaxWidth(this.width);
		return this.width;
	}

	private int calculateWidth(int max, Text text, TextRenderer textRenderer) {
		return Math.min(calculateWidth(text, textRenderer), max);
	}

	private int calculateHeight(TextRenderer textRenderer) {
		return Math.max(getCheckboxSize(textRenderer), this.textWidget.getHeight());
	}

	static int calculateWidth(Text text, TextRenderer textRenderer) {
		return getCheckboxSize(textRenderer) + 4 + textRenderer.getWidth(text);
	}

	public static CheckboxWidget.Builder builder(Text text, TextRenderer textRenderer) {
		return new CheckboxWidget.Builder(text, textRenderer);
	}

	public static int getCheckboxSize(TextRenderer textRenderer) {
		return 9 + 8;
	}

	@Override
	public void onPress(AbstractInput input) {
		checked = !checked;
		callback.onValueChange(this, checked);
	}

	public boolean isChecked() {
		return checked;
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, getNarrationMessage());

		if (active) {
			String usageKey = isFocused()
					? (checked ? "narration.checkbox.usage.focused.uncheck" : "narration.checkbox.usage.focused.check")
					: (checked ? "narration.checkbox.usage.hovered.uncheck" : "narration.checkbox.usage.hovered.check");
			builder.put(NarrationPart.USAGE, Text.translatable(usageKey));
		}
	}

	@Override
	public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
		Identifier texture = checked
				? (isFocused() ? SELECTED_HIGHLIGHTED_TEXTURE : SELECTED_TEXTURE)
				: (isFocused() ? HIGHLIGHTED_TEXTURE : TEXTURE);

		int checkboxSize = getCheckboxSize(textRenderer);
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				texture,
				getX(),
				getY(),
				checkboxSize,
				checkboxSize,
				ColorHelper.getWhite(alpha)
		);

		int textX = getX() + checkboxSize + CHECKBOX_PADDING;
		int textY = getY() + checkboxSize / 2 - textWidget.getHeight() / 2;
		textWidget.setPosition(textX, textY);
		textWidget.draw(context.getHoverListener(this, DrawContext.HoverType.fromTooltip(isHovered())));
	}

	/**
	 * Строитель для создания экземпляров {@link CheckboxWidget} с заданными параметрами.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final Text message;
		private final TextRenderer textRenderer;
		private int maxWidth;
		private int x = 0;
		private int y = 0;
		private CheckboxWidget.Callback callback = CheckboxWidget.Callback.EMPTY;
		private boolean checked = false;
		private @Nullable SimpleOption<Boolean> option = null;
		private @Nullable Tooltip tooltip = null;

		Builder(Text message, TextRenderer textRenderer) {
			this.message = message;
			this.textRenderer = textRenderer;
			this.maxWidth = CheckboxWidget.calculateWidth(message, textRenderer);
		}

		public CheckboxWidget.Builder pos(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public CheckboxWidget.Builder callback(CheckboxWidget.Callback callback) {
			this.callback = callback;
			return this;
		}

		public CheckboxWidget.Builder checked(boolean checked) {
			this.checked = checked;
			this.option = null;
			return this;
		}

		public CheckboxWidget.Builder option(SimpleOption<Boolean> option) {
			this.option = option;
			this.checked = option.getValue();
			return this;
		}

		public CheckboxWidget.Builder tooltip(Tooltip tooltip) {
			this.tooltip = tooltip;
			return this;
		}

		public CheckboxWidget.Builder maxWidth(int maxWidth) {
			this.maxWidth = maxWidth;
			return this;
		}

		public CheckboxWidget build() {
			CheckboxWidget.Callback resolvedCallback = option == null
					? callback
					: (checkbox, checked) -> {
						option.setValue(checked);
						callback.onValueChange(checkbox, checked);
					};
			CheckboxWidget checkboxWidget = new CheckboxWidget(
					x,
					y,
					maxWidth,
					message,
					textRenderer,
					checked,
					resolvedCallback
			);
			checkboxWidget.setTooltip(tooltip);
			return checkboxWidget;
		}
	}

	@Environment(EnvType.CLIENT)
	public interface Callback {

		CheckboxWidget.Callback EMPTY = (checkbox, checked) -> {};

		void onValueChange(CheckboxWidget checkbox, boolean checked);
	}
}
