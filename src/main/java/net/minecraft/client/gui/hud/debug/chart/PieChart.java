package net.minecraft.client.gui.hud.debug.chart;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.profiler.ProfileResult;
import net.minecraft.util.profiler.ProfilerTiming;
import org.jspecify.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Круговая диаграмма профилировщика. Отображает дерево секций профиля
 * с возможностью навигации вглубь по нажатию цифровых клавиш.
 */
@Environment(EnvType.CLIENT)
public class PieChart {

	public static final int PIE_CHART_SIZE = 105;
	public static final int LEGEND_ENTRY_HEIGHT = 10;

	private static final int LEGEND_PADDING = 5;
	private static final int BACKGROUND_COLOR = -1873784752;
	private static final int TEXT_COLOR = -1;

	/** Разделитель уровней пути профиля (ASCII RS — record separator). */
	private static final char PATH_SEPARATOR = '\u001e';

	private final TextRenderer textRenderer;
	private @Nullable ProfileResult profileResult;
	private String currentPath = "root";
	private int bottomMargin = 0;

	public PieChart(TextRenderer textRenderer) {
		this.textRenderer = textRenderer;
	}

	public void setProfileResult(@Nullable ProfileResult profileResult) {
		this.profileResult = profileResult;
	}

	public void setBottomMargin(int bottomMargin) {
		this.bottomMargin = bottomMargin;
	}

	public void render(DrawContext context) {
		if (profileResult == null) {
			return;
		}

		List<ProfilerTiming> timings = profileResult.getTimings(currentPath);
		ProfilerTiming rootTiming = timings.removeFirst();
		DecimalFormat decimalFormat = new DecimalFormat("##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));

		int chartCenterX = context.getScaledWindowWidth() - PIE_CHART_SIZE - LEGEND_ENTRY_HEIGHT;
		int chartLeft = chartCenterX - PIE_CHART_SIZE;
		int chartRight = chartCenterX + PIE_CHART_SIZE;
		int legendHeight = timings.size() * 9;
		int legendBottom = context.getScaledWindowHeight() - bottomMargin - LEGEND_PADDING;
		int legendTop = legendBottom - legendHeight;
		int pieRadius = 62;
		int pieTop = legendTop - pieRadius - LEGEND_PADDING;

		context.fill(chartLeft - LEGEND_PADDING, pieTop - pieRadius - LEGEND_PADDING, chartRight + LEGEND_PADDING, legendBottom + LEGEND_PADDING, BACKGROUND_COLOR);
		context.addProfilerChart(timings, chartLeft, pieTop - pieRadius + LEGEND_ENTRY_HEIGHT, chartRight, pieTop + pieRadius);

		String rootName = ProfileResult.getHumanReadableName(rootTiming.name);
		String headerLabel = "";

		if (!"unspecified".equals(rootName)) {
			headerLabel += "[0] ";
		}

		headerLabel += rootName.isEmpty() ? "ROOT " : rootName + " ";

		int headerY = pieTop - pieRadius;
		context.drawTextWithShadow(textRenderer, headerLabel, chartLeft, headerY, TEXT_COLOR);

		String totalPercent = decimalFormat.format(rootTiming.totalUsagePercentage) + "%";
		context.drawTextWithShadow(textRenderer, totalPercent, chartRight - textRenderer.getWidth(totalPercent), headerY, TEXT_COLOR);

		for (int entryIndex = 0; entryIndex < timings.size(); entryIndex++) {
			ProfilerTiming entry = timings.get(entryIndex);
			int entryY = legendTop + entryIndex * 9;

			String entryLabel = "unspecified".equals(entry.name)
				? "[?] " + entry.name
				: "[" + (entryIndex + 1) + "] " + entry.name;

			context.drawTextWithShadow(textRenderer, entryLabel, chartLeft, entryY, entry.getColor());

			String parentPercent = decimalFormat.format(entry.parentSectionUsagePercentage) + "%";
			context.drawTextWithShadow(textRenderer, parentPercent, chartRight - 50 - textRenderer.getWidth(parentPercent), entryY, entry.getColor());

			String totalEntryPercent = decimalFormat.format(entry.totalUsagePercentage) + "%";
			context.drawTextWithShadow(textRenderer, totalEntryPercent, chartRight - textRenderer.getWidth(totalEntryPercent), entryY, entry.getColor());
		}
	}

	/**
	 * Навигация по дереву профиля. Индекс 0 — переход на уровень выше,
	 * остальные — переход в дочернюю секцию по номеру из легенды.
	 */
	public void select(int index) {
		if (profileResult == null) {
			return;
		}

		List<ProfilerTiming> timings = profileResult.getTimings(currentPath);

		if (timings.isEmpty()) {
			return;
		}

		ProfilerTiming rootTiming = timings.remove(0);

		if (index == 0) {
			if (!rootTiming.name.isEmpty()) {
				int separatorIndex = currentPath.lastIndexOf(PATH_SEPARATOR);
				if (separatorIndex >= 0) {
					currentPath = currentPath.substring(0, separatorIndex);
				}
			}
		} else {
			int childIndex = index - 1;
			if (childIndex < timings.size() && !"unspecified".equals(timings.get(childIndex).name)) {
				if (!currentPath.isEmpty()) {
					currentPath += PATH_SEPARATOR;
				}

				currentPath += timings.get(childIndex).name;
			}
		}
	}
}
