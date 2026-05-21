package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code NarratedMultilineTextWidget}.
 */
public class NarratedMultilineTextWidget extends MultilineTextWidget {

	public static final int DEFAULT_MARGIN = 4;
	private final int margin;
	private final int customWidth;
	private final boolean alwaysShowBorders;
	private final NarratedMultilineTextWidget.BackgroundRendering backgroundRendering;

	NarratedMultilineTextWidget(
			Text text,
			TextRenderer textRenderer,
			int margin,
			int customWidth,
			NarratedMultilineTextWidget.BackgroundRendering backgroundRendering,
			boolean alwaysShowBorders
	) {
		super(text, textRenderer);
		this.active = true;
		this.margin = margin;
		this.customWidth = customWidth;
		this.alwaysShowBorders = alwaysShowBorders;
		this.backgroundRendering = backgroundRendering;
		this.updateWidth();
		this.updateHeight();
		this.setCentered(true);
	}

	@Override
	protected void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, this.getMessage());
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int
				i =
				this.alwaysShowBorders && !this.isFocused() ? ColorHelper.withAlpha(this.alpha, -6250336)
				                                            : ColorHelper.getWhite(this.alpha);
		switch (this.backgroundRendering) {
			case ALWAYS:
				context.fill(
						this.getX() + 1,
						this.getY(),
						this.getRight(),
						this.getBottom(),
						ColorHelper.toAlpha(this.alpha)
				);
				break;
			case ON_FOCUS:
				if (this.isFocused()) {
					context.fill(
							this.getX() + 1,
							this.getY(),
							this.getRight(),
							this.getBottom(),
							ColorHelper.toAlpha(this.alpha)
					);
				}
			case NEVER:
		}

		if (this.isFocused() || this.alwaysShowBorders) {
			context.drawStrokedRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight(), i);
		}

		super.renderWidget(context, mouseX, mouseY, deltaTicks);
	}

	@Override
	protected int getTextX() {
		return this.getX() + this.margin;
	}

	@Override
	protected int getTextY() {
		return super.getTextY() + this.margin;
	}

	@Override
	public MultilineTextWidget setMaxWidth(int maxWidth) {
		return super.setMaxWidth(maxWidth - this.margin * 2);
	}

	@Override
	public int getWidth() {
		return this.width;
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	public int getMArgin() {
		return this.margin;
	}

	public void updateWidth() {
		if (this.customWidth != -1) {
			this.setWidth(this.customWidth);
			this.setMaxWidth(this.customWidth);
		}
		else {
			this.setWidth(this.getTextRenderer().getWidth(this.getMessage()) + this.margin * 2);
		}
	}

	public void updateHeight() {
		int i = 9 * this.getTextRenderer().wrapLines(this.getMessage(), super.getWidth()).size();
		this.setHeight(i + this.margin * 2);
	}

	@Override
	public void setMessage(Text message) {
		this.message = message;
		int i;
		if (this.customWidth != -1) {
			i = this.customWidth;
		}
		else {
			i = this.getTextRenderer().getWidth(message) + this.margin * 2;
		}

		this.setWidth(i);
		this.updateHeight();
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	public static NarratedMultilineTextWidget.Builder builder(Text text, TextRenderer textRenderer) {
		return new NarratedMultilineTextWidget.Builder(text, textRenderer);
	}

	public static NarratedMultilineTextWidget.Builder builder(Text text, TextRenderer textRenderer, int margin) {
		return new NarratedMultilineTextWidget.Builder(text, textRenderer, margin);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code BackgroundRendering}.
	 */
	public static enum BackgroundRendering {
		ALWAYS,
		ON_FOCUS,
		NEVER;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Builder}.
	 */
	public static class Builder {

		private final Text text;
		private final TextRenderer textRenderer;
		private final int margin;
		private int customWidth = -1;
		private boolean alwaysShowBorders = true;
		private NarratedMultilineTextWidget.BackgroundRendering
				backgroundRendering =
				NarratedMultilineTextWidget.BackgroundRendering.ALWAYS;

		Builder(Text text, TextRenderer textRenderer) {
			this(text, textRenderer, 4);
		}

		Builder(Text text, TextRenderer textRenderer, int margin) {
			this.text = text;
			this.textRenderer = textRenderer;
			this.margin = margin;
		}

		public NarratedMultilineTextWidget.Builder width(int width) {
			this.customWidth = width;
			return this;
		}

		public NarratedMultilineTextWidget.Builder innerWidth(int width) {
			this.customWidth = width + this.margin * 2;
			return this;
		}

		public NarratedMultilineTextWidget.Builder alwaysShowBorders(boolean alwaysShowBorders) {
			this.alwaysShowBorders = alwaysShowBorders;
			return this;
		}

		public NarratedMultilineTextWidget.Builder backgroundRendering(NarratedMultilineTextWidget.BackgroundRendering backgroundRendering) {
			this.backgroundRendering = backgroundRendering;
			return this;
		}

		public NarratedMultilineTextWidget build() {
			return new NarratedMultilineTextWidget(
					this.text,
					this.textRenderer,
					this.margin,
					this.customWidth,
					this.backgroundRendering,
					this.alwaysShowBorders
			);
		}
	}
}
