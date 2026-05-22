package net.minecraft.util.profiler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.ToDoubleFunction;

/**
 * Сэмплер метрики: периодически снимает значение через {@link DoubleSupplier}
 * и накапливает пары (тик → значение) в буферах Netty для последующего дампа.
 */
public class Sampler {

	private final String name;
	private final SampleType type;
	private final DoubleSupplier retriever;
	private final ByteBuf ticksBuffer;
	private final ByteBuf valueBuffer;
	private volatile boolean active;
	private final @Nullable Runnable startAction;
	public final Sampler.@Nullable DeviationChecker deviationChecker;
	private double currentSample;

	protected Sampler(
		String name,
		SampleType type,
		DoubleSupplier retriever,
		@Nullable Runnable startAction,
		Sampler.@Nullable DeviationChecker deviationChecker
	) {
		this.name = name;
		this.type = type;
		this.startAction = startAction;
		this.retriever = retriever;
		this.deviationChecker = deviationChecker;
		this.valueBuffer = ByteBufAllocator.DEFAULT.buffer();
		this.ticksBuffer = ByteBufAllocator.DEFAULT.buffer();
		this.active = true;
	}

	public static Sampler create(String name, SampleType type, DoubleSupplier retriever) {
		return new Sampler(name, type, retriever, null, null);
	}

	public static <T> Sampler create(String name, SampleType type, T context, ToDoubleFunction<T> retriever) {
		return builder(name, type, retriever, context).build();
	}

	public static <T> Sampler.Builder<T> builder(String name, SampleType type, ToDoubleFunction<T> retriever, T context) {
		if (retriever == null) {
			throw new IllegalStateException();
		}

		return new Sampler.Builder<>(name, type, retriever, context);
	}

	public void start() {
		if (!active) {
			throw new IllegalStateException("Not running");
		}

		if (startAction != null) {
			startAction.run();
		}
	}

	public void sample(int tick) {
		ensureActive();
		currentSample = retriever.getAsDouble();
		valueBuffer.writeDouble(currentSample);
		ticksBuffer.writeInt(tick);
	}

	public void stop() {
		ensureActive();
		valueBuffer.release();
		ticksBuffer.release();
		active = false;
	}

	private void ensureActive() {
		if (!active) {
			throw new IllegalStateException(String.format(Locale.ROOT, "Sampler for metric %s not started!", name));
		}
	}

	public DoubleSupplier getRetriever() {
		return retriever;
	}

	public String getName() {
		return name;
	}

	public SampleType getType() {
		return type;
	}

	/**
	 * Собирает накопленные данные из буферов в карту тик→значение.
	 * После вызова буферы остаются в состоянии «прочитано».
	 */
	public Sampler.Data collectData() {
		Int2DoubleMap values = new Int2DoubleOpenHashMap();
		int startTick = Integer.MIN_VALUE;
		int endTick = Integer.MIN_VALUE;

		while (valueBuffer.isReadable(8)) {
			int tick = ticksBuffer.readInt();

			if (startTick == Integer.MIN_VALUE) {
				startTick = tick;
			}

			values.put(tick, valueBuffer.readDouble());
			endTick = tick;
		}

		return new Sampler.Data(startTick, endTick, values);
	}

	public boolean hasDeviated() {
		return deviationChecker != null && deviationChecker.check(currentSample);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Sampler sampler = (Sampler) o;
		return name.equals(sampler.name) && type.equals(sampler.type);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	/**
	 * Строитель сэмплера с поддержкой стартового действия и детектора отклонений.
	 */
	public static class Builder<T> {

		private final String name;
		private final SampleType type;
		private final DoubleSupplier timeGetter;
		private final T context;
		private @Nullable Runnable startAction;
		private Sampler.@Nullable DeviationChecker deviationChecker;

		public Builder(String name, SampleType type, ToDoubleFunction<T> timeFunction, T context) {
			this.name = name;
			this.type = type;
			this.timeGetter = () -> timeFunction.applyAsDouble(context);
			this.context = context;
		}

		public Sampler.Builder<T> startAction(Consumer<T> action) {
			startAction = () -> action.accept(context);
			return this;
		}

		public Sampler.Builder<T> deviationChecker(Sampler.DeviationChecker checker) {
			deviationChecker = checker;
			return this;
		}

		public Sampler build() {
			return new Sampler(name, type, timeGetter, startAction, deviationChecker);
		}
	}

	/**
	 * Снимок данных сэмплера: карта тик→значение с диапазоном тиков.
	 */
	public static class Data {

		private final Int2DoubleMap values;
		private final int startTick;
		private final int endTick;

		public Data(int startTick, int endTick, Int2DoubleMap values) {
			this.startTick = startTick;
			this.endTick = endTick;
			this.values = values;
		}

		public double getValue(int tick) {
			return values.get(tick);
		}

		public int getStartTick() {
			return startTick;
		}

		public int getEndTick() {
			return endTick;
		}
	}

	/**
	 * Стратегия определения аномального отклонения значения сэмплера.
	 */
	public interface DeviationChecker {

		boolean check(double value);
	}

	/**
	 * Детектор отклонений на основе отношения: срабатывает, если значение выросло
	 * относительно предыдущего более чем на заданный порог.
	 */
	public static class RatioDeviationChecker implements Sampler.DeviationChecker {

		private final float threshold;
		private double lastValue = Double.MIN_VALUE;

		public RatioDeviationChecker(float threshold) {
			this.threshold = threshold;
		}

		@Override
		public boolean check(double value) {
			if (lastValue == Double.MIN_VALUE || value <= lastValue) {
				lastValue = value;
				return false;
			}

			boolean deviated = (value - lastValue) / lastValue >= threshold;
			lastValue = value;
			return deviated;
		}
	}
}
