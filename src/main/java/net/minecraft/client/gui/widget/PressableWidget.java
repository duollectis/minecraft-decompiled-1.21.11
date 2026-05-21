package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code PressableWidget}.
 */
public abstract class PressableWidget extends ClickableWidget.InactivityIndicatingWidget {

	protected static final int TEXT_MARGIN = 2;
	private static final ButtonTextures TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("widget/button"),
			Identifier.ofVanilla("widget/button_disabled"),
			Identifier.ofVanilla("widget/button_highlighted")
	);
	private @Nullable Supplier<Boolean> focusOverride;

	public PressableWidget(int i, int j, int k, int l, Text text) {
		super(i, j, k, l, text);
	}

	/**
	 * Обрабатывает событие press.
	 *
	 * @param input input
	 */
	public abstract void onPress(AbstractInput input);

	@Override
	protected final void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		this.drawIcon(context, mouseX, mouseY, deltaTicks);
		this.setCursor(context);
	}

	/**
	 * Draw icon.
	 *
	 * @param context context
	 * @param mouseX mouse x
	 * @param mouseY mouse y
	 * @param deltaTicks delta ticks
	 */
	protected abstract void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks);

	/**
	 * Draw label.
	 *
	 * @param drawer drawer
	 */
	protected void drawLabel(DrawnTextConsumer drawer) {
		this.drawTextWithMargin(drawer, this.getMessage(), 2);
	}

	/**
	 * Draw button.
	 *
	 * @param context context
	 */
	protected final void drawButton(DrawContext context) {
		context.drawGuiTexture(
				RenderPipelines.GUI_TEXTURED,
				TEXTURES.get(this.active, this.focusOverride != null ? this.focusOverride.get() : this.isSelected()),
				this.getX(),
				this.getY(),
				this.getWidth(),
				this.getHeight(),
				ColorHelper.getWhite(this.alpha)
		);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		this.onPress(click);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (!this.isInteractable()) {
			return false;
		}
		else if (input.isEnterOrSpace()) {
			this.playDownSound(MinecraftClient.getInstance().getSoundManager());
			this.onPress(input);
			return true;
		}
		else {
			return false;
		}
	}

	public void setFocusOverride(Supplier<Boolean> focusOverride) {
		this.focusOverride = focusOverride;
	}
}
