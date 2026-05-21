package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.navigation.GuiNavigationType;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
/**
 * {@code SliderWidget}.
 */
public abstract class SliderWidget extends ClickableWidget.InactivityIndicatingWidget {

	private static final Identifier TEXTURE = Identifier.ofVanilla("widget/slider");
	private static final Identifier HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("widget/slider_highlighted");
	private static final Identifier HANDLE_TEXTURE = Identifier.ofVanilla("widget/slider_handle");
	private static final Identifier
			HANDLE_HIGHLIGHTED_TEXTURE =
			Identifier.ofVanilla("widget/slider_handle_highlighted");
	protected static final int TEXT_MARGIN = 2;
	public static final int DEFAULT_HEIGHT = 20;
	protected static final int HANDLE_WIDTH = 8;
	private static final int HANDLE_HALF_WIDTH = 4;
	protected double value;
	protected boolean sliderFocused;
	private boolean dragging;

	public SliderWidget(int x, int y, int width, int height, Text text, double value) {
		super(x, y, width, height, text);
		this.value = value;
	}

	private Identifier getTexture() {
		return this.isInteractable() && this.isFocused() && !this.sliderFocused ? HIGHLIGHTED_TEXTURE : TEXTURE;
	}

	private Identifier getHandleTexture() {
		return !this.isInteractable() || !this.hovered && !this.sliderFocused ? HANDLE_TEXTURE
		                                                                      : HANDLE_HIGHLIGHTED_TEXTURE;
	}

	@Override
	protected MutableText getNarrationMessage() {
		return Text.translatable("gui.narrate.slider", this.getMessage());
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, this.getNarrationMessage());
		if (this.active) {
			if (this.isFocused()) {
				if (this.sliderFocused) {
					builder.put(NarrationPart.USAGE, Text.translatable("narration.slider.usage.focused"));
				}
				else {
					builder.put(
							NarrationPart.USAGE,
							Text.translatable("narration.slider.usage.focused.keyboard_cannot_change_value")
					);
				}
			}
			else {
				builder.put(NarrationPart.USAGE, Text.translatable("narration.slider.usage.hovered"));
			}
		}
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				this.getTexture(),
				this.getX(),
				this.getY(),
				this.getWidth(),
				this.getHeight(),
				ColorHelper.getWhite(this.alpha)
		);
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				this.getHandleTexture(),
				this.getX() + (int) (this.value * (this.width - 8)),
				this.getY(),
				8,
				this.getHeight(),
				ColorHelper.getWhite(this.alpha)
		);
		this.drawTextWithMargin(context.getHoverListener(this, DrawContext.HoverType.NONE), this.getMessage(), 2);
		if (this.isHovered()) {
			context.setCursor(this.dragging ? StandardCursors.RESIZE_EW : StandardCursors.POINTING_HAND);
		}
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		this.dragging = this.active;
		this.setValueFromMouse(click);
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (!focused) {
			this.sliderFocused = false;
		}
		else {
			GuiNavigationType guiNavigationType = MinecraftClient.getInstance().getNavigationType();
			if (guiNavigationType == GuiNavigationType.MOUSE || guiNavigationType == GuiNavigationType.KEYBOARD_TAB) {
				this.sliderFocused = true;
			}
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEnterOrSpace()) {
			this.sliderFocused = !this.sliderFocused;
			return true;
		}
		else {
			if (this.sliderFocused) {
				boolean bl = input.isLeft();
				boolean bl2 = input.isRight();
				if (bl || bl2) {
					float f = bl ? -1.0F : 1.0F;
					this.setValue(this.value + f / (this.width - 8));
					return true;
				}
			}

			return false;
		}
	}

	private void setValueFromMouse(Click click) {
		this.setValue((click.x() - (this.getX() + 4)) / (this.width - 8));
	}

	protected void setValue(double value) {
		double d = this.value;
		this.value = MathHelper.clamp(value, 0.0, 1.0);
		if (d != this.value) {
			this.applyValue();
		}

		this.updateMessage();
	}

	@Override
	protected void onDrag(Click click, double offsetX, double offsetY) {
		this.setValueFromMouse(click);
		super.onDrag(click, offsetX, offsetY);
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	@Override
	public void onRelease(Click click) {
		this.dragging = false;
		super.playDownSound(MinecraftClient.getInstance().getSoundManager());
	}

	protected abstract void updateMessage();

	protected abstract void applyValue();
}
