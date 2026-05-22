package net.minecraft.client.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Состояние тултипа виджета: управляет задержкой отображения и выбором позиционера.
 * Тултип показывается только после истечения {@link #delay} с момента,
 * когда виджет стал hovered или focused. При смене состояния таймер сбрасывается.
 *
 * <p>Позиционер выбирается автоматически:
 * {@link FocusedTooltipPositioner} — при клавиатурном фокусе без наведения мыши,
 * {@link WidgetTooltipPositioner} — во всех остальных случаях.</p>
 */
@Environment(EnvType.CLIENT)
public class TooltipState {

	private @Nullable Tooltip tooltip;
	private Duration delay = Duration.ZERO;
	private long renderCheckTime;
	private boolean lastShouldRender;

	public void setDelay(Duration delay) {
		this.delay = delay;
	}

	public void setTooltip(@Nullable Tooltip tooltip) {
		this.tooltip = tooltip;
	}

	public @Nullable Tooltip getTooltip() {
		return tooltip;
	}

	/**
	 * Отрисовывает тултип, если виджет hovered или focused (клавиатура),
	 * и прошла задержка {@link #delay} с момента активации.
	 *
	 * @param hovered        {@code true} — курсор мыши над виджетом
	 * @param focused        {@code true} — виджет имеет фокус
	 * @param navigationFocus прямоугольник виджета для позиционирования тултипа
	 */
	public void render(
		DrawContext context,
		int mouseX,
		int mouseY,
		boolean hovered,
		boolean focused,
		ScreenRect navigationFocus
	) {
		if (tooltip == null) {
			lastShouldRender = false;
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		boolean shouldRender = hovered || (focused && client.getNavigationType().isKeyboard());

		if (shouldRender != lastShouldRender) {
			if (shouldRender) {
				renderCheckTime = Util.getMeasuringTimeMs();
			}

			lastShouldRender = shouldRender;
		}

		if (shouldRender && Util.getMeasuringTimeMs() - renderCheckTime > delay.toMillis()) {
			context.drawTooltip(
				client.textRenderer,
				tooltip.getLines(client),
				createPositioner(navigationFocus, hovered, focused),
				mouseX,
				mouseY,
				focused
			);
		}
	}

	public void appendNarrations(NarrationMessageBuilder builder) {
		if (tooltip != null) {
			tooltip.appendNarrations(builder);
		}
	}

	private TooltipPositioner createPositioner(ScreenRect focus, boolean hovered, boolean focused) {
		return !hovered && focused && MinecraftClient.getInstance().getNavigationType().isKeyboard()
			? new FocusedTooltipPositioner(focus)
			: new WidgetTooltipPositioner(focus);
	}
}
