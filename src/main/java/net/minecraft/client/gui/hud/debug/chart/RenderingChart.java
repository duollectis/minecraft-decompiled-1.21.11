package net.minecraft.client.gui.hud.debug.chart;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.profiler.log.MultiValueDebugSampleLog;

import java.util.Locale;

/**
 * График времени рендеринга кадра. Пороговые линии — 30 и 60 FPS,
 * а также пользовательский лимит FPS.
 */
@Environment(EnvType.CLIENT)
public class RenderingChart extends DebugChart {

	private static final int TARGET_FPS_LOW = 30;
	private static final int TARGET_FPS_HIGH = 60;

	/** Время кадра при 30 FPS в миллисекундах. */
	private static final double TARGET_FRAME_TIME_MS = 1000.0 / TARGET_FPS_LOW;

	/** Максимальный лимит FPS, при котором рисуется пользовательская линия. */
	private static final int MAX_CUSTOM_FPS_LIMIT = 250;

	private static final int COLOR_GOOD = -16711936;
	private static final int COLOR_MEDIUM = -256;
	private static final int COLOR_BAD = -65536;
	private static final int COLOR_FPS_LIMIT_LINE = -16711681;

	public RenderingChart(TextRenderer textRenderer, MultiValueDebugSampleLog log) {
		super(textRenderer, log);
	}

	@Override
	protected void renderThresholds(DrawContext context, int x, int width, int height) {
		drawBorderedText(context, "30 FPS", x + 1, height - CHART_HEIGHT + 1);
		drawBorderedText(context, "60 FPS", x + 1, height - TARGET_FPS_LOW + 1);
		context.drawHorizontalLine(x, x + width - 1, height - TARGET_FPS_LOW, -1);

		int fpsLimit = MinecraftClient.getInstance().options.getMaxFps().getValue();

		if (fpsLimit > 0 && fpsLimit <= MAX_CUSTOM_FPS_LIMIT) {
			context.drawHorizontalLine(x, x + width - 1, height - getHeight(1.0E9 / fpsLimit) - 1, COLOR_FPS_LIMIT_LINE);
		}
	}

	@Override
	protected String format(double value) {
		return String.format(Locale.ROOT, "%d ms", (int) Math.round(toMillisecondsPerFrame(value)));
	}

	@Override
	protected int getHeight(double value) {
		return (int) Math.round(toMillisecondsPerFrame(value) * CHART_HEIGHT / TARGET_FRAME_TIME_MS);
	}

	@Override
	protected int getColor(long value) {
		return getColor(toMillisecondsPerFrame(value), 0.0, COLOR_GOOD, 28.0, COLOR_MEDIUM, 56.0, COLOR_BAD);
	}

	private static double toMillisecondsPerFrame(double nanosecondsPerFrame) {
		return nanosecondsPerFrame / 1_000_000.0;
	}
}
