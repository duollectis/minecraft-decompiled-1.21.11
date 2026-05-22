package net.minecraft.util.profiler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.util.Util;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Основная реализация {@link ReadableProfiler}, накапливающая иерархические замеры времени
 * по стеку push/pop. Хранит {@link LocatedInfo} для каждого уникального пути секции.
 */
public class ProfilerSystem implements ReadableProfiler {

	private static final long TIMEOUT_NANOSECONDS = Duration.ofMillis(100L).toNanos();
	private static final Logger LOGGER = LogUtils.getLogger();

	private final List<String> path = Lists.newArrayList();
	private final LongList timeList = new LongArrayList();
	private final Map<String, ProfilerSystem.LocatedInfo> locationInfos = Maps.newHashMap();
	private final IntSupplier endTickGetter;
	private final LongSupplier timeGetter;
	private final long startTime;
	private final int startTick;
	private final BooleanSupplier timeoutDisabled;
	private final Set<Pair<String, SampleType>> sampleTypes = new ObjectArraySet<>();

	private String fullPath = "";
	private boolean tickStarted;
	private ProfilerSystem.@Nullable LocatedInfo currentInfo;

	public ProfilerSystem(LongSupplier timeGetter, IntSupplier tickGetter, BooleanSupplier timeoutDisabled) {
		this.startTime = timeGetter.getAsLong();
		this.timeGetter = timeGetter;
		this.startTick = tickGetter.getAsInt();
		this.endTickGetter = tickGetter;
		this.timeoutDisabled = timeoutDisabled;
	}

	@Override
	public void startTick() {
		if (tickStarted) {
			LOGGER.error("Profiler tick already started - missing endTick()?");
			return;
		}

		tickStarted = true;
		fullPath = "";
		path.clear();
		push("root");
	}

	@Override
	public void endTick() {
		if (!tickStarted) {
			LOGGER.error("Profiler tick already ended - missing startTick()?");
			return;
		}

		pop();
		tickStarted = false;

		if (!fullPath.isEmpty()) {
			LOGGER.error(
				"Profiler tick ended before path was fully popped (remainder: '{}'). Mismatched push/pop?",
				LogUtils.defer(() -> ProfileResult.getHumanReadableName(fullPath))
			);
		}
	}

	@Override
	public void push(String location) {
		if (!tickStarted) {
			LOGGER.error(
				"Cannot push '{}' to profiler if profiler tick hasn't started - missing startTick()?",
				location
			);
			return;
		}

		if (!fullPath.isEmpty()) {
			fullPath = fullPath + "\u001e";
		}

		fullPath = fullPath + location;
		path.add(fullPath);
		timeList.add(Util.getMeasuringTimeNano());
		currentInfo = null;
	}

	@Override
	public void push(Supplier<String> locationGetter) {
		push(locationGetter.get());
	}

	@Override
	public void markSampleType(SampleType type) {
		sampleTypes.add(Pair.of(fullPath, type));
	}

	@Override
	public void pop() {
		if (!tickStarted) {
			LOGGER.error("Cannot pop from profiler if profiler tick hasn't started - missing startTick()?");
			return;
		}

		if (timeList.isEmpty()) {
			LOGGER.error("Tried to pop one too many times! Mismatched push() and pop()?");
			return;
		}

		long now = Util.getMeasuringTimeNano();
		long startTime = timeList.removeLong(timeList.size() - 1);
		path.removeLast();
		long elapsed = now - startTime;

		ProfilerSystem.LocatedInfo info = getCurrentInfo();
		info.totalTime += elapsed;
		info.visits++;
		info.maxTime = Math.max(info.maxTime, elapsed);
		info.minTime = Math.min(info.minTime, elapsed);

		if (elapsed > TIMEOUT_NANOSECONDS && !timeoutDisabled.getAsBoolean()) {
			LOGGER.warn(
				"Something's taking too long! '{}' took aprox {} ms",
				LogUtils.defer(() -> ProfileResult.getHumanReadableName(fullPath)),
				LogUtils.defer(() -> elapsed / 1000000.0)
			);
		}

		fullPath = path.isEmpty() ? "" : path.getLast();
		currentInfo = null;
	}

	@Override
	public void swap(String location) {
		pop();
		push(location);
	}

	@Override
	public void swap(Supplier<String> locationGetter) {
		pop();
		push(locationGetter);
	}

	private ProfilerSystem.LocatedInfo getCurrentInfo() {
		if (currentInfo == null) {
			currentInfo = locationInfos.computeIfAbsent(fullPath, k -> new ProfilerSystem.LocatedInfo());
		}

		return currentInfo;
	}

	@Override
	public void visit(String marker, int num) {
		getCurrentInfo().counts.addTo(marker, num);
	}

	@Override
	public void visit(Supplier<String> markerGetter, int num) {
		getCurrentInfo().counts.addTo(markerGetter.get(), num);
	}

	@Override
	public ProfileResult getResult() {
		return new ProfileResultImpl(
			locationInfos,
			startTime,
			startTick,
			timeGetter.getAsLong(),
			endTickGetter.getAsInt()
		);
	}

	@Override
	public ProfilerSystem.@Nullable LocatedInfo getInfo(String name) {
		return locationInfos.get(name);
	}

	@Override
	public Set<Pair<String, SampleType>> getSampleTargets() {
		return sampleTypes;
	}

	/**
	 * Накопленная статистика одной секции профайлера: суммарное, максимальное,
	 * минимальное время и счётчики посещений маркеров.
	 */
	public static class LocatedInfo implements ProfileLocationInfo {

		long maxTime = Long.MIN_VALUE;
		long minTime = Long.MAX_VALUE;
		long totalTime;
		long visits;
		final Object2LongOpenHashMap<String> counts = new Object2LongOpenHashMap<>();

		@Override
		public long getTotalTime() {
			return totalTime;
		}

		@Override
		public long getMaxTime() {
			return maxTime;
		}

		@Override
		public long getVisitCount() {
			return visits;
		}

		@Override
		public Object2LongMap<String> getCounts() {
			return Object2LongMaps.unmodifiable(counts);
		}
	}
}
