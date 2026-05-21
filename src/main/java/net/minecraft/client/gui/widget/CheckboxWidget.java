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

@Environment(EnvType.CLIENT)
/**
 * {@code CheckboxWidget}.
 */
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
		this.checked = !this.checked;
		this.callback.onValueChange(this, this.checked);
	}

	public boolean isChecked() {
		return this.checked;
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, this.getNarrationMessage());
		if (this.active) {
			if (this.isFocused()) {
				builder.put(
						NarrationPart.USAGE,
						Text.translatable(this.checked ? "narration.checkbox.usage.focused.uncheck"
						                               : "narration.checkbox.usage.focused.check")
				);
			}
			else {
				builder.put(
						NarrationPart.USAGE,
						Text.translatable(this.checked ? "narration.checkbox.usage.hovered.uncheck"
						                               : "narration.checkbox.usage.hovered.check")
				);
			}
		}
	}

	@Override
	public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		TextRenderer textRenderer = minecraftClient.textRenderer;
		Identifier identifier;
		if (this.checked) {
			identifier = this.isFocused() ? SELECTED_HIGHLIGHTED_TEXTURE : SELECTED_TEXTURE;
		}
		else {
			identifier = this.isFocused() ? HIGHLIGHTED_TEXTURE : TEXTURE;
		}

		int i = getCheckboxSize(textRenderer);
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				identifier,
				this.getX(),
				this.getY(),
				i,
				i,
				ColorHelper.getWhite(this.alpha)
		);
		int j = this.getX() + i + 4;
		int k = this.getY() + i / 2 - this.textWidget.getHeight() / 2;
		this.textWidget.setPosition(j, k);
		this.textWidget.draw(context.getHoverListener(this, DrawContext.HoverType.fromTooltip(this.isHovered())));
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Builder}.
	 */
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

		/**
		 * Build.
		 *
		 * @return CheckboxWidget — результат операции
		 */
		public CheckboxWidget build() {
			CheckboxWidget.Callback callback = this.option == null ? this.callback : (checkbox, checked) -> {
				this.option.setValue(checked);
				this.callback.onValueChange(checkbox, checked);
			};
			CheckboxWidget
					checkboxWidget =
					new CheckboxWidget(
							this.x,
							this.y,
							this.maxWidth,
							this.message,
							this.textRenderer,
							this.checked,
							callback
					);
			checkboxWidget.setTooltip(this.tooltip);
			return checkboxWidget;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Callback}.
	 */
	public interface Callback {

		CheckboxWidget.Callback EMPTY = (checkbox, checked) -> {};

		void onValueChange(CheckboxWidget checkbox, boolean checked);
	}
}
