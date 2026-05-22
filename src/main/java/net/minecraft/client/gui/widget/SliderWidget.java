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

/**
 * Абстрактный виджет слайдера с поддержкой мыши, клавиатуры и нарративного описания.
 * Подклассы реализуют {@link #updateMessage()} и {@link #applyValue()} для конкретной логики.
 */
@Environment(EnvType.CLIENT)
public abstract class SliderWidget extends ClickableWidget.InactivityIndicatingWidget {

	private static final Identifier TEXTURE = Identifier.ofVanilla("widget/slider");
	private static final Identifier HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("widget/slider_highlighted");
	private static final Identifier HANDLE_TEXTURE = Identifier.ofVanilla("widget/slider_handle");
	private static final Identifier HANDLE_HIGHLIGHTED_TEXTURE = Identifier.ofVanilla("widget/slider_handle_highlighted");

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
		return isInteractable() && isFocused() && !sliderFocused ? HIGHLIGHTED_TEXTURE : TEXTURE;
	}

	private Identifier getHandleTexture() {
		return isInteractable() && (hovered || sliderFocused) ? HANDLE_HIGHLIGHTED_TEXTURE : HANDLE_TEXTURE;
	}

	@Override
	protected MutableText getNarrationMessage() {
		return Text.translatable("gui.narrate.slider", getMessage());
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, getNarrationMessage());

		if (active) {
			if (isFocused()) {
				String usageKey = sliderFocused
						? "narration.slider.usage.focused"
						: "narration.slider.usage.focused.keyboard_cannot_change_value";
				builder.put(NarrationPart.USAGE, Text.translatable(usageKey));
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
				getTexture(),
				getX(),
				getY(),
				getWidth(),
				getHeight(),
				ColorHelper.getWhite(alpha)
		);
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				getHandleTexture(),
				getX() + (int) (value * (width - HANDLE_WIDTH)),
				getY(),
				HANDLE_WIDTH,
				getHeight(),
				ColorHelper.getWhite(alpha)
		);
		drawTextWithMargin(context.getHoverListener(this, DrawContext.HoverType.NONE), getMessage(), TEXT_MARGIN);

		if (isHovered()) {
			context.setCursor(dragging ? StandardCursors.RESIZE_EW : StandardCursors.POINTING_HAND);
		}
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		dragging = active;
		setValueFromMouse(click);
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (focused) {
			GuiNavigationType navType = MinecraftClient.getInstance().getNavigationType();
			if (navType == GuiNavigationType.MOUSE || navType == GuiNavigationType.KEYBOARD_TAB) {
				sliderFocused = true;
			}
		}
		else {
			sliderFocused = false;
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (input.isEnterOrSpace()) {
			sliderFocused = !sliderFocused;
			return true;
		}

		if (sliderFocused) {
			boolean isLeft = input.isLeft();
			boolean isRight = input.isRight();
			if (isLeft || isRight) {
				float direction = isLeft ? -1.0F : 1.0F;
				setValue(value + direction / (width - HANDLE_WIDTH));
				return true;
			}
		}

		return false;
	}

	private void setValueFromMouse(Click click) {
		setValue((click.x() - (getX() + HANDLE_HALF_WIDTH)) / (width - HANDLE_WIDTH));
	}

	protected void setValue(double value) {
		double previous = this.value;
		this.value = MathHelper.clamp(value, 0.0, 1.0);

		if (previous != this.value) {
			applyValue();
		}

		updateMessage();
	}

	@Override
	protected void onDrag(Click click, double offsetX, double offsetY) {
		setValueFromMouse(click);
		super.onDrag(click, offsetX, offsetY);
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	@Override
	public void onRelease(Click click) {
		dragging = false;
		super.playDownSound(MinecraftClient.getInstance().getSoundManager());
	}

	protected abstract void updateMessage();

	protected abstract void applyValue();
}
