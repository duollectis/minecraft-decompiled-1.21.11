package net.minecraft.util.profiler;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import net.minecraft.SharedConstants;
import net.minecraft.util.crash.ReportType;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Снимок результатов профилирования за один тик или диапазон тиков.
 * Хранит иерархию секций, счётчики посещений и поддерживает сохранение в текстовый файл.
 */
public class ProfileResultImpl implements ProfileResult {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final ProfileLocationInfo EMPTY_INFO = new ProfileLocationInfo() {
		@Override
		public long getTotalTime() {
			return 0L;
		}

		@Override
		public long getMaxTime() {
			return 0L;
		}

		@Override
		public long getVisitCount() {
			return 0L;
		}

		@Override
		public Object2LongMap<String> getCounts() {
			return Object2LongMaps.emptyMap();
		}
	};

	private static final Splitter PATH_SPLITTER = Splitter.on('\u001e');
	private static final Comparator<Entry<String, ProfileResultImpl.CounterInfo>> COUNTER_COMPARATOR =
		Entry.<String, ProfileResultImpl.CounterInfo>comparingByValue(
			Comparator.comparingLong(info -> info.totalTime)
		).reversed();

	private final Map<String, ? extends ProfileLocationInfo> locationInfos;
	private final long startTime;
	private final int startTick;
	private final long endTime;
	private final int endTick;
	private final int tickDuration;

	public ProfileResultImpl(
		Map<String, ? extends ProfileLocationInfo> locationInfos,
		long startTime,
		int startTick,
		long endTime,
		int endTick
	) {
		this.locationInfos = locationInfos;
		this.startTime = startTime;
		this.startTick = startTick;
		this.endTime = endTime;
		this.endTick = endTick;
		this.tickDuration = endTick - startTick;
	}

	private ProfileLocationInfo getInfo(String path) {
		ProfileLocationInfo info = locationInfos.get(path);
		return info != null ? info : EMPTY_INFO;
	}

	/**
	 * Возвращает список замеров дочерних секций относительно указанного пути.
	 * Первым элементом всегда идёт сам родительский узел с 100% долей.
	 */
	@Override
	public List<ProfilerTiming> getTimings(String parentPath) {
		String originalPath = parentPath;
		ProfileLocationInfo rootInfo = getInfo("root");
		long rootTotalTime = rootInfo.getTotalTime();

		ProfileLocationInfo parentInfo = getInfo(parentPath);
		long parentTotalTime = parentInfo.getTotalTime();
		long parentVisitCount = parentInfo.getVisitCount();

		List<ProfilerTiming> timings = Lists.newArrayList();

		if (!parentPath.isEmpty()) {
			parentPath = parentPath + "\u001e";
		}

		long childrenTotalTime = 0L;

		for (String key : locationInfos.keySet()) {
			if (isSubpath(parentPath, key)) {
				childrenTotalTime += getInfo(key).getTotalTime();
			}
		}

		float unaccountedTime = (float) childrenTotalTime;

		if (childrenTotalTime < parentTotalTime) {
			childrenTotalTime = parentTotalTime;
		}

		if (rootTotalTime < childrenTotalTime) {
			rootTotalTime = childrenTotalTime;
		}

		for (String key : locationInfos.keySet()) {
			if (isSubpath(parentPath, key)) {
				ProfileLocationInfo childInfo = getInfo(key);
				long childTime = childInfo.getTotalTime();
				double parentPercent = childTime * 100.0 / childrenTotalTime;
				double totalPercent = childTime * 100.0 / rootTotalTime;
				String childName = key.substring(parentPath.length());
				timings.add(new ProfilerTiming(childName, parentPercent, totalPercent, childInfo.getVisitCount()));
			}
		}

		if ((float) childrenTotalTime > unaccountedTime) {
			double unspecifiedParent = ((float) childrenTotalTime - unaccountedTime) * 100.0 / childrenTotalTime;
			double unspecifiedTotal = ((float) childrenTotalTime - unaccountedTime) * 100.0 / rootTotalTime;
			timings.add(new ProfilerTiming("unspecified", unspecifiedParent, unspecifiedTotal, parentVisitCount));
		}

		Collections.sort(timings);
		timings.add(0, new ProfilerTiming(originalPath, 100.0, childrenTotalTime * 100.0 / rootTotalTime, parentVisitCount));
		return timings;
	}

	private static boolean isSubpath(String parent, String path) {
		return path.length() > parent.length()
			&& path.startsWith(parent)
			&& path.indexOf(30, parent.length() + 1) < 0;
	}

	private Map<String, ProfileResultImpl.CounterInfo> setupCounters() {
		Map<String, ProfileResultImpl.CounterInfo> counters = Maps.newTreeMap();
		locationInfos.forEach((location, info) -> {
			Object2LongMap<String> counts = info.getCounts();
			if (counts.isEmpty()) {
				return;
			}

			List<String> pathParts = PATH_SPLITTER.splitToList(location);
			counts.forEach((marker, count) -> counters
				.computeIfAbsent(marker, k -> new ProfileResultImpl.CounterInfo())
				.add(pathParts.iterator(), count));
		});
		return counters;
	}

	@Override
	public long getStartTime() {
		return startTime;
	}

	@Override
	public int getStartTick() {
		return startTick;
	}

	@Override
	public long getEndTime() {
		return endTime;
	}

	@Override
	public int getEndTick() {
		return endTick;
	}

	@Override
	public boolean save(Path path) {
		Writer writer = null;

		try {
			Files.createDirectories(path.getParent());
			writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
			writer.write(asString(getTimeSpan(), getTickSpan()));
			return true;
		}
		catch (Throwable error) {
			LOGGER.error("Could not save profiler results to {}", path, error);
			return false;
		}
		finally {
			IOUtils.closeQuietly(writer);
		}
	}

	protected String asString(long timeSpan, int tickSpan) {
		StringBuilder sb = new StringBuilder();
		ReportType.MINECRAFT_PROFILER_RESULTS.addHeaderAndNugget(sb, List.of());
		sb.append("Version: ").append(SharedConstants.getGameVersion().id()).append('\n');
		sb.append("Time span: ").append(timeSpan / 1000000L).append(" ms\n");
		sb.append("Tick span: ").append(tickSpan).append(" ticks\n");
		sb.append("// This is approximately ")
			.append(String.format(Locale.ROOT, "%.2f", tickSpan / ((float) timeSpan / 1.0E9F)))
			.append(" ticks per second. It should be ")
			.append(20)
			.append(" ticks per second\n\n");
		sb.append("--- BEGIN PROFILE DUMP ---\n\n");
		appendTiming(0, "root", sb);
		sb.append("--- END PROFILE DUMP ---\n\n");

		Map<String, ProfileResultImpl.CounterInfo> counters = setupCounters();
		if (!counters.isEmpty()) {
			sb.append("--- BEGIN COUNTER DUMP ---\n\n");
			appendCounterDump(counters, sb, tickSpan);
			sb.append("--- END COUNTER DUMP ---\n\n");
		}

		return sb.toString();
	}

	@Override
	public String getRootTimings() {
		StringBuilder sb = new StringBuilder();
		appendTiming(0, "root", sb);
		return sb.toString();
	}

	private static StringBuilder indent(StringBuilder sb, int size) {
		sb.append(String.format(Locale.ROOT, "[%02d] ", size));

		for (int i = 0; i < size; i++) {
			sb.append("|   ");
		}

		return sb;
	}

	private void appendTiming(int level, String name, StringBuilder sb) {
		List<ProfilerTiming> timings = getTimings(name);

		ProfileLocationInfo info = locationInfos.get(name);
		Object2LongMap<String> counts = info != null ? info.getCounts() : Object2LongMaps.emptyMap();

		counts.forEach((marker, count) -> indent(sb, level)
			.append('#')
			.append(marker)
			.append(' ')
			.append(count)
			.append('/')
			.append(count / tickDuration)
			.append('\n')
		);

		if (timings.size() < 3) {
			return;
		}

		for (int i = 1; i < timings.size(); i++) {
			ProfilerTiming timing = timings.get(i);
			indent(sb, level)
				.append(timing.name)
				.append('(')
				.append(timing.visitCount)
				.append('/')
				.append(String.format(Locale.ROOT, "%.0f", (float) timing.visitCount / tickDuration))
				.append(')')
				.append(" - ")
				.append(String.format(Locale.ROOT, "%.2f", timing.parentSectionUsagePercentage))
				.append("%/")
				.append(String.format(Locale.ROOT, "%.2f", timing.totalUsagePercentage))
				.append("%\n");

			if ("unspecified".equals(timing.name)) {
				continue;
			}

			try {
				appendTiming(level + 1, name + "\u001e" + timing.name, sb);
			}
			catch (Exception error) {
				sb.append("[[ EXCEPTION ").append(error).append(" ]]");
			}
		}
	}

	private void appendCounter(int depth, String name, ProfileResultImpl.CounterInfo info, int tickSpan, StringBuilder sb) {
		indent(sb, depth)
			.append(name)
			.append(" total:")
			.append(info.selfTime)
			.append('/')
			.append(info.totalTime)
			.append(" average: ")
			.append(info.selfTime / tickSpan)
			.append('/')
			.append(info.totalTime / tickSpan)
			.append('\n');

		info.subCounters
			.entrySet()
			.stream()
			.sorted(COUNTER_COMPARATOR)
			.forEach(entry -> appendCounter(depth + 1, entry.getKey(), entry.getValue(), tickSpan, sb));
	}

	private void appendCounterDump(Map<String, ProfileResultImpl.CounterInfo> counters, StringBuilder sb, int tickSpan) {
		counters.forEach((name, info) -> {
			sb.append("-- Counter: ").append(name).append(" --\n");
			appendCounter(0, "root", info.subCounters.get("root"), tickSpan, sb);
			sb.append("\n\n");
		});
	}

	@Override
	public int getTickSpan() {
		return tickDuration;
	}

	/**
	 * Узел дерева счётчиков: хранит собственное время секции и рекурсивно вложенные счётчики.
	 */
	static class CounterInfo {

		long selfTime;
		long totalTime;
		final Map<String, ProfileResultImpl.CounterInfo> subCounters = Maps.newHashMap();

		public void add(Iterator<String> pathIterator, long time) {
			totalTime += time;

			if (!pathIterator.hasNext()) {
				selfTime += time;
				return;
			}

			subCounters
				.computeIfAbsent(pathIterator.next(), k -> new ProfileResultImpl.CounterInfo())
				.add(pathIterator, time);
		}
	}
}
