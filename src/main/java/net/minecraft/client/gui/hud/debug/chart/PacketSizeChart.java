package net.minecraft.client.gui.hud.debug.chart;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.log.MultiValueDebugSampleLog;

import java.util.Locale;

/**
 * График размера сетевых пакетов. Ось Y — логарифмическая шкала до 1 МиБ/с.
 */
@Environment(EnvType.CLIENT)
public class PacketSizeChart extends DebugChart {

	private static final int INCOMING_COLOR = -16711681;
	private static final int OUTGOING_COLOR = -6250241;
	private static final int ERROR_COLOR = -65536;
	private static final int ONE_KILOBYTE = 1024;
	private static final int ONE_MEGABYTE = 1048576;
	private static final int MAX_PACKET_SIZE = 1048576;

	/** Пороговое значение для перехода к единицам МиБ/с. */
	private static final double MEGABYTE_THRESHOLD = 1048576.0;

	/** Пороговое значение для перехода к единицам КиБ/с. */
	private static final double KILOBYTE_THRESHOLD = 1024.0;

	/** Коэффициент перевода байт/тик → байт/с (20 тиков в секунду). */
	private static final double TICKS_PER_SECOND = 20.0;

	public PacketSizeChart(TextRenderer textRenderer, MultiValueDebugSampleLog log) {
		super(textRenderer, log);
	}

	@Override
	protected void renderThresholds(DrawContext context, int x, int width, int height) {
		drawSizeBar(context, x, width, height, 64);
		drawSizeBar(context, x, width, height, ONE_KILOBYTE);
		drawSizeBar(context, x, width, height, 16384);
		drawBorderedText(context, formatBytesPerSecond(MEGABYTE_THRESHOLD), x + 1, height - calculateHeight(MEGABYTE_THRESHOLD) + 1);
	}

	private void drawSizeBar(DrawContext context, int x, int width, int height, int bytes) {
		drawSizeBar(context, x, width, height - calculateHeight(bytes), formatBytesPerSecond(bytes));
	}

	private void drawSizeBar(DrawContext context, int x, int width, int y, String label) {
		drawBorderedText(context, label, x + 1, y + 1);
		context.drawHorizontalLine(x, x + width - 1, y, -1);
	}

	@Override
	protected String format(double value) {
		return formatBytesPerSecond(toBytesPerSecond(value));
	}

	private static String formatBytesPerSecond(double value) {
		if (value >= MEGABYTE_THRESHOLD) {
			return String.format(Locale.ROOT, "%.1f MiB/s", value / MEGABYTE_THRESHOLD);
		}

		return value >= KILOBYTE_THRESHOLD
			? String.format(Locale.ROOT, "%.1f KiB/s", value / KILOBYTE_THRESHOLD)
			: String.format(Locale.ROOT, "%d B/s", MathHelper.floor(value));
	}

	@Override
	protected int getHeight(double value) {
		return calculateHeight(toBytesPerSecond(value));
	}

	private static int calculateHeight(double value) {
		return (int) Math.round(Math.log(value + 1.0) * 60.0 / Math.log(MEGABYTE_THRESHOLD));
	}

	@Override
	protected int getColor(long value) {
		return getColor(toBytesPerSecond(value), 0.0, INCOMING_COLOR, 8192.0, OUTGOING_COLOR, 1.048576E7, ERROR_COLOR);
	}

	private static double toBytesPerSecond(double bytesPerTick) {
		return bytesPerTick * TICKS_PER_SECOND;
	}
}
