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

@Environment(EnvType.CLIENT)
/**
 * {@code TextIconButtonWidget}.
 */
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
			this.setTooltip(Tooltip.of(tooltip));
		}

		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.texture = textures;
	}

	/**
	 * Draw icon.
	 *
	 * @param context context
	 * @param x x
	 * @param y y
	 */
	protected void drawIcon(DrawContext context, int x, int y) {
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				this.texture.get(this.isInteractable(), this.isSelected()),
				x,
				y,
				this.textureWidth,
				this.textureHeight,
				this.alpha
		);
	}

	public static TextIconButtonWidget.Builder builder(
			net.minecraft.text.Text text,
			ButtonWidget.PressAction onPress,
			boolean hideLabel
	) {
		return new TextIconButtonWidget.Builder(text, onPress, hideLabel);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Builder}.
	 */
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

		/**
		 * Build.
		 *
		 * @return TextIconButtonWidget — результат операции
		 */
		public TextIconButtonWidget build() {
			if (this.texture == null) {
				throw new IllegalStateException("Sprite not set");
			}
			else {
				return (TextIconButtonWidget) (this.hideText
				                               ? new TextIconButtonWidget.IconOnly(
						this.width,
						this.height,
						this.text,
						this.textureWidth,
						this.textureHeight,
						this.texture,
						this.onPress,
						this.tooltip,
						this.narrationSupplier
				)
				                               : new TextIconButtonWidget.WithText(
						                               this.width,
						                               this.height,
						                               this.text,
						                               this.textureWidth,
						                               this.textureHeight,
						                               this.texture,
						                               this.onPress,
						                               this.tooltip,
						                               this.narrationSupplier
				                               )
				);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code IconOnly}.
	 */
	public static class IconOnly extends TextIconButtonWidget {

		@SuppressWarnings("NullableProblems")
		protected IconOnly(
				int i,
				int j,
				net.minecraft.text.Text text,
				int k,
				int l,
				ButtonTextures buttonTextures,
				ButtonWidget.PressAction pressAction,
				net.minecraft.text.Text text2,
				ButtonWidget.@Nullable NarrationSupplier narrationSupplier
		) {
			super(i, j, text, k, l, buttonTextures, pressAction, text2, narrationSupplier);
		}

		@Override
		public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			this.drawButton(context);
			int i = this.getX() + this.getWidth() / 2 - this.textureWidth / 2;
			int j = this.getY() + this.getHeight() / 2 - this.textureHeight / 2;
			this.drawIcon(context, i, j);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code WithText}.
	 */
	public static class WithText extends TextIconButtonWidget {

		@SuppressWarnings("NullableProblems")
		protected WithText(
				int i,
				int j,
				net.minecraft.text.Text text,
				int k,
				int l,
				ButtonTextures buttonTextures,
				ButtonWidget.PressAction pressAction,
				net.minecraft.text.Text text2,
				ButtonWidget.@Nullable NarrationSupplier narrationSupplier
		) {
			super(i, j, text, k, l, buttonTextures, pressAction, text2, narrationSupplier);
		}

		@Override
		public void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			this.drawButton(context);
			int i = this.getX() + 2;
			int j = this.getX() + this.getWidth() - this.textureWidth - 4;
			int k = this.getX() + this.getWidth() / 2;
			DrawnTextConsumer drawnTextConsumer = context.getHoverListener(this, DrawContext.HoverType.NONE);
			drawnTextConsumer.marqueedText(this.getMessage(), k, i, j, this.getY(), this.getY() + this.getHeight());
			int l = this.getX() + this.getWidth() - this.textureWidth - 2;
			int m = this.getY() + this.getHeight() / 2 - this.textureHeight / 2;
			this.drawIcon(context, l, m);
		}
	}
}
