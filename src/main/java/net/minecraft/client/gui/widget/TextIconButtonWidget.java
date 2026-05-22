package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Абстрактная кнопка с иконкой-текстурой. Конкретные реализации:
 * {@link IconOnly} — только иконка, {@link WithText} — иконка + текст.
 */
@Environment(EnvType.CLIENT)
public abstract class TextIconButtonWidget extends ButtonWidget {

	protected final ButtonTextures texture;
	protected final int textureWidth;
	protected final int textureHeight;

	@SuppressWarnings("NullableProblems")
	TextIconButtonWidget(
			int width,
			int height,
			net.minecraft.text.Text message,
			int textureWidth,
			int textureHeight,
			ButtonTextures textures,
			ButtonWidget.PressAction onPress,
			net.minecraft.text.Text tooltip,
			ButtonWidget.@Nullable NarrationSupplier narrationSupplier
	) {
		super(
				0,
				0,
				width,
				height,
				message,
				onPress,
				narrationSupplier == null ? DEFAULT_NARRATION_SUPPLIER : narrationSupplier
		);
		if (tooltip != null) {
			setTooltip(Tooltip.of(tooltip));
		}

		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		texture = textures;
	}

	protected void drawIcon(DrawContext context, int x, int y) {
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				texture.get(isInteractable(), isSelected()),
				x,
				y,
				textureWidth,
				textureHeight,
				alpha
		);
	}

	public static TextIconButtonWidget.Builder builder(
			net.minecraft.text.Text text,
			ButtonWidget.PressAction onPress,
			boolean hideLabel
	) {
		return new TextIconButtonWidget.Builder(text, onPress, hideLabel);
	}

	/**
	 * Строитель для создания экземпляров {@link TextIconButtonWidget}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private final net.minecraft.text.Text text;
		private final ButtonWidget.PressAction onPress;
		private final boolean hideText;
		private int width = 150;
		private int height = 20;
		private @Nullable ButtonTextures texture;
		private int textureWidth;
		private int textureHeight;
		private net.minecraft.text.Text tooltip;
		private ButtonWidget.@Nullable NarrationSupplier narrationSupplier;

		public Builder(net.minecraft.text.Text text, ButtonWidget.PressAction onPress, boolean hideText) {
			this.text = text;
			this.onPress = onPress;
			this.hideText = hideText;
		}

		public TextIconButtonWidget.Builder width(int width) {
			this.width = width;
			return this;
		}

		public TextIconButtonWidget.Builder dimension(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public TextIconButtonWidget.Builder texture(Identifier texture, int width, int height) {
			this.texture = new ButtonTextures(texture);
			this.textureWidth = width;
			this.textureHeight = height;
			return this;
		}

		public TextIconButtonWidget.Builder texture(ButtonTextures texture, int width, int height) {
			this.texture = texture;
			this.textureWidth = width;
			this.textureHeight = height;
			return this;
		}

		public TextIconButtonWidget.Builder useTextAsTooltip() {
			this.tooltip = this.text;
			return this;
		}

		public TextIconButtonWidget.Builder narration(ButtonWidget.NarrationSupplier narrationSupplier) {
			this.narrationSupplier = narrationSupplier;
			return this;
		}

		public TextIconButtonWidget build() {
			if (texture == null) {
				throw new IllegalStateException("Sprite not set");
			}

			return hideText
					? new TextIconButtonWidget.IconOnly(
							width,
							height,
							text,
							textureWidth,
							textureHeight,
							texture,
							onPress,
							tooltip,
							narrationSupplier
					)
					: new TextIconButtonWidget.WithText(
							width,
							height,
							text,
							textureWidth,
							textureHeight,
							texture,
							onPress,
							tooltip,
							narrationSupplier
					);
		}
	}

	@Environment(EnvType.CLIENT)
	public static class IconOnly extends TextIconButtonWidget {

		@SuppressWarnings("NullableProblems")
		protected IconOnly(
				int width,
				int height,
				net.minecraft.text.Text text,
				int textureWidth,
				int textureHeight,
				ButtonTextures buttonTextures,
				ButtonWidget.PressAction pressAction,
				net.minecraft.text.Text tooltip,
				ButtonWidget.@Nullable NarrationSupplier narrationSupplier
		) {
			super(width, height, text, textureWidth, textureHeight, buttonTextures, pressAction, tooltip, narrationSupplier);
		}

		@Override
		public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			drawButton(context);
			int iconX = getX() + getWidth() / 2 - textureWidth / 2;
			int iconY = getY() + getHeight() / 2 - textureHeight / 2;
			drawIcon(context, iconX, iconY);
		}
	}

	@Environment(EnvType.CLIENT)
	public static class WithText extends TextIconButtonWidget {

		@SuppressWarnings("NullableProblems")
		protected WithText(
				int width,
				int height,
				net.minecraft.text.Text text,
				int textureWidth,
				int textureHeight,
				ButtonTextures buttonTextures,
				ButtonWidget.PressAction pressAction,
				net.minecraft.text.Text tooltip,
				ButtonWidget.@Nullable NarrationSupplier narrationSupplier
		) {
			super(width, height, text, textureWidth, textureHeight, buttonTextures, pressAction, tooltip, narrationSupplier);
		}

		@Override
		public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			drawButton(context);
			int textStart = getX() + 2;
			int textEnd = getX() + getWidth() - textureWidth - 4;
			int textCenter = getX() + getWidth() / 2;
			DrawnTextConsumer textConsumer = context.getHoverListener(this, DrawContext.HoverType.NONE);
			textConsumer.marqueedText(getMessage(), textCenter, textStart, textEnd, getY(), getY() + getHeight());

			int iconX = getX() + getWidth() - textureWidth - 2;
			int iconY = getY() + getHeight() / 2 - textureHeight / 2;
			drawIcon(context, iconX, iconY);
		}
	}
}
