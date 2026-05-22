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

/**
 * Базовый класс нажимаемого виджета-кнопки.
 * Реализует стандартную логику нажатия мышью и клавишей Enter/Space,
 * отрисовку фона кнопки с поддержкой состояний (активна/неактивна/выделена)
 * и опциональное переопределение состояния фокуса через {@link #setFocusOverride}.
 *
 * <p>Подклассы обязаны реализовать {@link #onPress} и {@link #drawIcon}.</p>
 */
@Environment(EnvType.CLIENT)
public abstract class PressableWidget extends ClickableWidget.InactivityIndicatingWidget {

	protected static final int TEXT_MARGIN = 2;

	private static final ButtonTextures TEXTURES = new ButtonTextures(
		Identifier.ofVanilla("widget/button"),
		Identifier.ofVanilla("widget/button_disabled"),
		Identifier.ofVanilla("widget/button_highlighted")
	);

	private @Nullable Supplier<Boolean> focusOverride;

	public PressableWidget(int x, int y, int width, int height, Text text) {
		super(x, y, width, height, text);
	}

	/** Вызывается при нажатии кнопки (мышь или клавиша). */
	public abstract void onPress(AbstractInput input);

	@Override
	protected final void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		drawIcon(context, mouseX, mouseY, deltaTicks);
		setCursor(context);
	}

	/** Отрисовывает содержимое кнопки поверх фона. */
	protected abstract void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks);

	protected void drawLabel(DrawnTextConsumer drawer) {
		drawTextWithMargin(drawer, getMessage(), TEXT_MARGIN);
	}

	/**
	 * Отрисовывает стандартный фон кнопки с учётом состояния активности и фокуса.
	 * Если задан {@link #focusOverride}, он используется вместо {@link #isSelected()}.
	 */
	protected final void drawButton(DrawContext context) {
		boolean focused = focusOverride != null ? focusOverride.get() : isSelected();
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURES.get(active, focused),
			getX(),
			getY(),
			getWidth(),
			getHeight(),
			ColorHelper.getWhite(alpha)
		);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		onPress(click);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (!isInteractable()) {
			return false;
		}

		if (input.isEnterOrSpace()) {
			playDownSound(MinecraftClient.getInstance().getSoundManager());
			onPress(input);
			return true;
		}

		return false;
	}

	/**
	 * Устанавливает поставщик состояния фокуса, переопределяющий стандартный {@link #isSelected()}.
	 * Используется для синхронизации визуального выделения кнопки с внешним состоянием.
	 *
	 * @param focusOverride поставщик булевого значения: {@code true} — кнопка выглядит выделенной
	 */
	public void setFocusOverride(Supplier<Boolean> focusOverride) {
		this.focusOverride = focusOverride;
	}
}
