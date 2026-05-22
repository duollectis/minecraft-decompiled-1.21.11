package net.minecraft.util.profiler;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Активная реализация {@link Recorder}: записывает профиль за {@link #MAX_DURATION_IN_SECONDS} секунд,
 * собирает отклонения сэмплеров и по завершении сбрасывает дамп через {@link RecordDumper}.
 */
public class DebugRecorder implements Recorder {

	public static final int MAX_DURATION_IN_SECONDS = 10;

	private static @Nullable Consumer<Path> globalDumpConsumer = null;

	private final Map<Sampler, List<Deviation>> deviations = new Object2ObjectOpenHashMap<>();
	private final TickTimeTracker timeTracker;
	private final Executor dumpExecutor;
	private final RecordDumper dumper;
	private final Consumer<ProfileResult> resultConsumer;
	private final Consumer<Path> dumpConsumer;
	private final SamplerSource samplerSource;
	private final LongSupplier timeGetter;
	private final long endTime;

	private int ticks;
	private ReadableProfiler profiler;
	private volatile boolean stopping;
	private Set<Sampler> samplers = ImmutableSet.of();

	private DebugRecorder(
		SamplerSource samplerSource,
		LongSupplier timeGetter,
		Executor dumpExecutor,
		RecordDumper dumper,
		Consumer<ProfileResult> resultConsumer,
		Consumer<Path> dumpConsumer
	) {
		this.samplerSource = samplerSource;
		this.timeGetter = timeGetter;
		this.timeTracker = new TickTimeTracker(timeGetter, () -> ticks, () -> false);
		this.dumpExecutor = dumpExecutor;
		this.dumper = dumper;
		this.resultConsumer = resultConsumer;
		this.dumpConsumer = globalDumpConsumer == null ? dumpConsumer : dumpConsumer.andThen(globalDumpConsumer);
		this.endTime = timeGetter.getAsLong() + TimeUnit.NANOSECONDS.convert(MAX_DURATION_IN_SECONDS, TimeUnit.SECONDS);
		this.profiler = new ProfilerSystem(timeGetter, () -> ticks, () -> true);
		this.timeTracker.enable();
	}

	public static DebugRecorder of(
		SamplerSource source,
		LongSupplier timeGetter,
		Executor dumpExecutor,
		RecordDumper dumper,
		Consumer<ProfileResult> resultConsumer,
		Consumer<Path> dumpConsumer
	) {
		return new DebugRecorder(source, timeGetter, dumpExecutor, dumper, resultConsumer, dumpConsumer);
	}

	@Override
	public synchronized void stop() {
		if (isActive()) {
			stopping = true;
		}
	}

	@Override
	public synchronized void forceStop() {
		if (isActive()) {
			profiler = DummyProfiler.INSTANCE;
			resultConsumer.accept(EmptyProfileResult.INSTANCE);
			forceStop(samplers);
		}
	}

	@Override
	public void startTick() {
		checkState();
		samplers = samplerSource.getSamplers(() -> profiler);

		for (Sampler sampler : samplers) {
			sampler.start();
		}

		ticks++;
	}

	@Override
	public void endTick() {
		checkState();

		if (ticks == 0) {
			return;
		}

		for (Sampler sampler : samplers) {
			sampler.sample(ticks);

			if (sampler.hasDeviated()) {
				Deviation deviation = new Deviation(Instant.now(), ticks, profiler.getResult());
				deviations.computeIfAbsent(sampler, s -> Lists.newArrayList()).add(deviation);
			}
		}

		if (!stopping && timeGetter.getAsLong() <= endTime) {
			profiler = new ProfilerSystem(timeGetter, () -> ticks, () -> true);
			return;
		}

		stopping = false;
		ProfileResult result = timeTracker.getResult();
		profiler = DummyProfiler.INSTANCE;
		resultConsumer.accept(result);
		dump(result);
	}

	@Override
	public boolean isActive() {
		return timeTracker.isActive();
	}

	@Override
	public Profiler getProfiler() {
		return Profiler.union(timeTracker.getProfiler(), profiler);
	}

	private void checkState() {
		if (!isActive()) {
			throw new IllegalStateException("Not started!");
		}
	}

	private void dump(ProfileResult result) {
		HashSet<Sampler> snapshot = new HashSet<>(samplers);
		dumpExecutor.execute(() -> {
			Path path = dumper.createDump(snapshot, deviations, result);
			forceStop(snapshot);
			dumpConsumer.accept(path);
		});
	}

	private void forceStop(Collection<Sampler> toStop) {
		for (Sampler sampler : toStop) {
			sampler.stop();
		}

		deviations.clear();
		timeTracker.disable();
	}

	public static void setGlobalDumpConsumer(Consumer<Path> consumer) {
		globalDumpConsumer = consumer;
	}
}
