package net.minecraft.util.profiler;

import com.mojang.jtracy.Plot;
import com.mojang.jtracy.TracyClient;
import com.mojang.jtracy.Zone;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import org.slf4j.Logger;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Реализация {@link Profiler}, интегрирующаяся с профайлером Tracy через JTracy.
 * В режиме разработки ({@link SharedConstants#isDevelopment}) дополнительно
 * захватывает имя метода, файл и номер строки вызывающего кода через StackWalker.
 */
public class TracyProfiler implements Profiler {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final StackWalker STACK_WALKER = StackWalker.getInstance(Set.of(Option.RETAIN_CLASS_REFERENCE), 5);

	private final List<Zone> zones = new ArrayList<>();
	private final Map<String, TracyProfiler.Marker> markers = new HashMap<>();
	private final String threadName = Thread.currentThread().getName();

	@Override
	public void startTick() {
	}

	@Override
	public void endTick() {
		for (TracyProfiler.Marker marker : markers.values()) {
			marker.setCount(0);
		}
	}

	@Override
	public void push(String location) {
		String methodName = "";
		String fileName = "";
		int lineNumber = 0;

		if (SharedConstants.isDevelopment) {
			Optional<StackFrame> frame = STACK_WALKER.walk(stream -> stream
				.filter(f -> f.getDeclaringClass() != TracyProfiler.class
					&& f.getDeclaringClass() != Profiler.UnionProfiler.class)
				.findFirst()
			);

			if (frame.isPresent()) {
				StackFrame stackFrame = frame.get();
				methodName = stackFrame.getMethodName();
				fileName = stackFrame.getFileName();
				lineNumber = stackFrame.getLineNumber();
			}
		}

		Zone zone = TracyClient.beginZone(location, methodName, fileName, lineNumber);
		zones.add(zone);
	}

	@Override
	public void push(Supplier<String> locationGetter) {
		push(locationGetter.get());
	}

	@Override
	public void pop() {
		if (zones.isEmpty()) {
			LOGGER.error("Tried to pop one too many times! Mismatched push() and pop()?");
			return;
		}

		Zone zone = zones.removeLast();
		zone.close();
	}

	@Override
	public void swap(String location) {
		pop();
		push(location);
	}

	@Override
	public void swap(Supplier<String> locationGetter) {
		pop();
		push(locationGetter.get());
	}

	@Override
	public void markSampleType(SampleType type) {
	}

	@Override
	public void visit(String marker, int num) {
		markers
			.computeIfAbsent(marker, name -> new TracyProfiler.Marker(threadName + " " + name))
			.increment(num);
	}

	@Override
	public void visit(Supplier<String> markerGetter, int num) {
		visit(markerGetter.get(), num);
	}

	private Zone getCurrentZone() {
		return zones.getLast();
	}

	@Override
	public void addZoneText(String label) {
		getCurrentZone().addText(label);
	}

	@Override
	public void addZoneValue(long value) {
		getCurrentZone().addValue(value);
	}

	@Override
	public void setZoneColor(int color) {
		getCurrentZone().setColor(color);
	}

	/**
	 * Обёртка над Tracy Plot: накапливает счётчик посещений маркера за тик
	 * и сбрасывает его в ноль при {@link #endTick()}.
	 */
	static final class Marker {

		private final Plot plot;
		private int count;

		Marker(String name) {
			this.plot = TracyClient.createPlot(name);
			this.count = 0;
		}

		void setCount(int count) {
			this.count = count;
			plot.setValue(count);
		}

		void increment(int delta) {
			setCount(count + delta);
		}
	}
}
