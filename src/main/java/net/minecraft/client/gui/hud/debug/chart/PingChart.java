package net.minecraft.client.gui.hud.debug.chart;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.profiler.log.MultiValueDebugSampleLog;

import java.util.Locale;

/**
 * График пинга (задержки сети). Максимальная шкала — {@value #MAX_PING_MS} мс.
 */
@Environment(EnvType.CLIENT)
public class PingChart extends DebugChart {

	private static final int MAX_PING_MS = 500;
	private static final int GOOD_PING_MS = 250;

	private static final int COLOR_GOOD = -16711936;
	private static final int COLOR_MEDIUM = -256;
	private static final int COLOR_BAD = -65536;

	public PingChart(TextRenderer textRenderer, MultiValueDebugSampleLog log) {
		super(textRenderer, log);
	}

	@Override
	protected void renderThresholds(DrawContext context, int x, int width, int height) {
		drawBorderedText(context, "500 ms", x + 1, height - CHART_HEIGHT + 1);
	}

	@Override
	protected String format(double value) {
		return String.format(Locale.ROOT, "%d ms", (int) Math.round(value));
	}

	@Override
	protected int getHeight(double value) {
		return (int) Math.round(value * CHART_HEIGHT / MAX_PING_MS);
	}

	@Override
	protected int getColor(long value) {
		return getColor(value, 0.0, COLOR_GOOD, GOOD_PING_MS, COLOR_MEDIUM, MAX_PING_MS, COLOR_BAD);
	}
}
