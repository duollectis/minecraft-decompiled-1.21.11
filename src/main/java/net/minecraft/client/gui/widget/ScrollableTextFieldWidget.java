package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
/**
 * {@code ScrollableTextFieldWidget}.
 */
public abstract class ScrollableTextFieldWidget extends ScrollableWidget {

	private static final ButtonTextures TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("widget/text_field"), Identifier.ofVanilla("widget/text_field_highlighted")
	);
	private static final int PADDING = 4;
	public static final int MIN_HEIGHT = 8;
	private boolean hasBackground = true;
	private boolean hasOverlay = true;

	public ScrollableTextFieldWidget(int i, int j, int k, int l, Text text) {
		super(i, j, k, l, text);
	}

	public ScrollableTextFieldWidget(
			int x,
			int y,
			int width,
			int height,
			Text message,
			boolean hasBackground,
			boolean hasOverlay
	) {
		this(x, y, width, height, message);
		this.hasBackground = hasBackground;
		this.hasOverlay = hasOverlay;
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		boolean bl = this.checkScrollbarDragged(click);
		return super.mouseClicked(click, doubled) || bl;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		boolean bl = input.isUp();
		boolean bl2 = input.isDown();
		if (bl || bl2) {
			double d = this.getScrollY();
			this.setScrollY(this.getScrollY() + (bl ? -1 : 1) * this.getDeltaYPerScroll());
			if (d != this.getScrollY()) {
				return true;
			}
		}

		return super.keyPressed(input);
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (this.visible) {
			if (this.hasBackground) {
				this.drawBox(context);
			}

			context.enableScissor(
					this.getX() + 1,
					this.getY() + 1,
					this.getX() + this.width - 1,
					this.getY() + this.height - 1
			);
			context.getMatrices().pushMatrix();
			context.getMatrices().translate(0.0F, (float) (-this.getScrollY()));
			this.renderContents(context, mouseX, mouseY, deltaTicks);
			context.getMatrices().popMatrix();
			context.disableScissor();
			this.drawScrollbar(context, mouseX, mouseY);
			if (this.hasOverlay) {
				this.renderOverlay(context);
			}
		}
	}

	protected void renderOverlay(DrawContext context) {
	}

	protected int getTextMargin() {
		return 4;
	}

	protected int getPadding() {
		return this.getTextMargin() * 2;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.active && this.visible && mouseX >= this.getX() && mouseY >= this.getY()
				&& mouseX < this.getRight() + 6 && mouseY < this.getBottom();
	}

	@Override
	protected int getScrollbarX() {
		return this.getRight();
	}

	@Override
	protected int getContentsHeightWithPadding() {
		return this.getContentsHeight() + this.getPadding();
	}

	protected void drawBox(DrawContext context) {
		this.draw(context, this.getX(), this.getY(), this.getWidth(), this.getHeight());
	}

	protected void draw(DrawContext context, int x, int y, int width, int height) {
		Identifier identifier = TEXTURES.get(this.isInteractable(), this.isFocused());
		context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, x, y, width, height);
	}

	protected boolean isVisible(int textTop, int textBottom) {
		return textBottom - this.getScrollY() >= this.getY()
				&& textTop - this.getScrollY() <= this.getY() + this.height;
	}

	protected abstract int getContentsHeight();

	protected abstract void renderContents(DrawContext context, int mouseX, int mouseY, float deltaTicks);

	protected int getTextX() {
		return this.getX() + this.getTextMargin();
	}

	protected int getTextY() {
		return this.getY() + this.getTextMargin();
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}
}
