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

@Environment(EnvType.CLIENT)
/**
 * {@code TooltipState}.
 */
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
		return this.tooltip;
	}

	public void render(
			DrawContext context,
			int mouseX,
			int mouseY,
			boolean hovered,
			boolean focused,
			ScreenRect navigationFocus
	) {
		if (this.tooltip == null) {
			this.lastShouldRender = false;
		}
		else {
			MinecraftClient minecraftClient = MinecraftClient.getInstance();
			boolean bl = hovered || focused && minecraftClient.getNavigationType().isKeyboard();
			if (bl != this.lastShouldRender) {
				if (bl) {
					this.renderCheckTime = Util.getMeasuringTimeMs();
				}

				this.lastShouldRender = bl;
			}

			if (bl && Util.getMeasuringTimeMs() - this.renderCheckTime > this.delay.toMillis()) {
				context.drawTooltip(
						minecraftClient.textRenderer,
						this.tooltip.getLines(minecraftClient),
						this.createPositioner(navigationFocus, hovered, focused),
						mouseX,
						mouseY,
						focused
				);
			}
		}
	}

	private TooltipPositioner createPositioner(ScreenRect focus, boolean hovered, boolean focused) {
		return (TooltipPositioner) (
				!hovered && focused && MinecraftClient.getInstance().getNavigationType().isKeyboard()
				? new FocusedTooltipPositioner(focus)
				: new WidgetTooltipPositioner(focus)
		);
	}

	public void appendNarrations(NarrationMessageBuilder builder) {
		if (this.tooltip != null) {
			this.tooltip.appendNarrations(builder);
		}
	}
}
