package net.minecraft.client.gui.hud.debug.chart;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.profiler.log.MultiValueDebugSampleLog;

import java.util.Locale;

@Environment(EnvType.CLIENT)
/**
 * {@code RenderingChart}.
 */
public class RenderingChart extends DebugChart {

	private static final int TARGET_FPS = 30;
	private static final double TARGET_FRAME_TIME_MS = 33.333333333333336;

	public RenderingChart(TextRenderer textRenderer, MultiValueDebugSampleLog multiValueDebugSampleLog) {
		super(textRenderer, multiValueDebugSampleLog);
	}

	@Override
	protected void renderThresholds(DrawContext context, int x, int width, int height) {
		this.drawBorderedText(context, "30 FPS", x + 1, height - 60 + 1);
		this.drawBorderedText(context, "60 FPS", x + 1, height - 30 + 1);
		context.drawHorizontalLine(x, x + width - 1, height - 30, -1);
		int i = MinecraftClient.getInstance().options.getMaxFps().getValue();
		if (i > 0 && i <= 250) {
			context.drawHorizontalLine(x, x + width - 1, height - this.getHeight(1.0E9 / i) - 1, -16711681);
		}
	}

	@Override
	protected String format(double value) {
		return String.format(Locale.ROOT, "%d ms", (int) Math.round(toMillisecondsPerFrame(value)));
	}

	@Override
	protected int getHeight(double value) {
		return (int) Math.round(toMillisecondsPerFrame(value) * 60.0 / 33.333333333333336);
	}

	@Override
	protected int getColor(long value) {
		return this.getColor(toMillisecondsPerFrame(value), 0.0, -16711936, 28.0, -256, 56.0, -65536);
	}

	private static double toMillisecondsPerFrame(double nanosecondsPerFrame) {
		return nanosecondsPerFrame / 1000000.0;
	}
}
