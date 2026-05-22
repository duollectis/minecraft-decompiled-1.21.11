package net.minecraft.client.gui.hud.debug.chart;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.profiler.ServerTickType;
import net.minecraft.util.profiler.log.MultiValueDebugSampleLog;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * График времени серверного тика. Столбцы разбиты на три слоя:
 * основной тик сервера, запланированные задачи и прочие задачи.
 */
@Environment(EnvType.CLIENT)
public class TickChart extends DebugChart {

	private static final int TICK_SERVER_COLOR = -6745839;
	private static final int SCHEDULED_TASKS_COLOR = -4548257;
	private static final int OTHER_TASKS_COLOR = -10547572;

	private static final int COLOR_GOOD = -16711936;
	private static final int COLOR_MEDIUM = -256;
	private static final int COLOR_BAD = -65536;

	private final Supplier<Float> millisPerTickSupplier;

	public TickChart(TextRenderer textRenderer, MultiValueDebugSampleLog log, Supplier<Float> millisPerTickSupplier) {
		super(textRenderer, log);
		this.millisPerTickSupplier = millisPerTickSupplier;
	}

	@Override
	protected void renderThresholds(DrawContext context, int x, int width, int height) {
		float tps = (float) TimeHelper.SECOND_IN_MILLIS / millisPerTickSupplier.get();
		drawBorderedText(context, String.format(Locale.ROOT, "%.1f TPS", tps), x + 1, height - CHART_HEIGHT + 1);
	}

	@Override
	protected void drawOverlayBar(DrawContext context, int y, int x, int index) {
		long tickServerNs = log.get(index, ServerTickType.TICK_SERVER_METHOD.ordinal());
		int tickServerHeight = getHeight(tickServerNs);
		context.fill(x, y - tickServerHeight, x + 1, y, TICK_SERVER_COLOR);

		long scheduledNs = log.get(index, ServerTickType.SCHEDULED_TASKS.ordinal());
		int scheduledHeight = getHeight(scheduledNs);
		context.fill(x, y - tickServerHeight - scheduledHeight, x + 1, y - tickServerHeight, SCHEDULED_TASKS_COLOR);

		long otherNs = log.get(index) - log.get(index, ServerTickType.IDLE.ordinal()) - tickServerNs - scheduledNs;
		int otherHeight = getHeight(otherNs);
		context.fill(x, y - otherHeight - scheduledHeight - tickServerHeight, x + 1, y - scheduledHeight - tickServerHeight, OTHER_TASKS_COLOR);
	}

	@Override
	protected long get(int index) {
		return log.get(index) - log.get(index, ServerTickType.IDLE.ordinal());
	}

	@Override
	protected String format(double value) {
		return String.format(Locale.ROOT, "%d ms", (int) Math.round(toMillisecondsPerTick(value)));
	}

	@Override
	protected int getHeight(double value) {
		return (int) Math.round(toMillisecondsPerTick(value) * CHART_HEIGHT / millisPerTickSupplier.get().floatValue());
	}

	@Override
	protected int getColor(long value) {
		float msPerTick = millisPerTickSupplier.get();
		return getColor(toMillisecondsPerTick(value), msPerTick, COLOR_GOOD, msPerTick * 1.125, COLOR_MEDIUM, msPerTick * 1.25, COLOR_BAD);
	}

	private static double toMillisecondsPerTick(double nanosecondsPerTick) {
		return nanosecondsPerTick / 1_000_000.0;
	}
}
