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
 * {@code Sampler}.
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

	/**
	 * Create.
	 *
	 * @param name name
	 * @param type type
	 * @param retriever retriever
	 *
	 * @return Sampler — результат операции
	 */
	public static Sampler create(String name, SampleType type, DoubleSupplier retriever) {
		return new Sampler(name, type, retriever, null, null);
	}

	/**
	 * Create.
	 *
	 * @param name name
	 * @param type type
	 * @param context context
	 * @param retriever retriever
	 *
	 * @return Sampler — результат операции
	 */
	public static <T> Sampler create(String name, SampleType type, T context, ToDoubleFunction<T> retriever) {
		return builder(name, type, retriever, context).build();
	}

	public static <T> Sampler.Builder<T> builder(
			String name,
			SampleType type,
			ToDoubleFunction<T> retriever,
			T context
	) {
		if (retriever == null) {
			throw new IllegalStateException();
		}
		else {
			return new Sampler.Builder<>(name, type, retriever, context);
		}
	}

	/**
	 * Start.
	 */
	public void start() {
		if (!this.active) {
			throw new IllegalStateException("Not running");
		}
		else {
			if (this.startAction != null) {
				this.startAction.run();
			}
		}
	}

	/**
	 * Sample.
	 *
	 * @param tick tick
	 */
	public void sample(int tick) {
		this.ensureActive();
		this.currentSample = this.retriever.getAsDouble();
		this.valueBuffer.writeDouble(this.currentSample);
		this.ticksBuffer.writeInt(tick);
	}

	/**
	 * Stop.
	 */
	public void stop() {
		this.ensureActive();
		this.valueBuffer.release();
		this.ticksBuffer.release();
		this.active = false;
	}

	private void ensureActive() {
		if (!this.active) {
			throw new IllegalStateException(String.format(
					Locale.ROOT,
					"Sampler for metric %s not started!",
					this.name
			));
		}
	}

	public DoubleSupplier getRetriever() {
		return this.retriever;
	}

	public String getName() {
		return this.name;
	}

	public SampleType getType() {
		return this.type;
	}

	public Sampler.Data collectData() {
		Int2DoubleMap int2DoubleMap = new Int2DoubleOpenHashMap();
		int i = Integer.MIN_VALUE;
		int j = Integer.MIN_VALUE;

		while (this.valueBuffer.isReadable(8)) {
			int k = this.ticksBuffer.readInt();
			if (i == Integer.MIN_VALUE) {
				i = k;
			}

			int2DoubleMap.put(k, this.valueBuffer.readDouble());
			j = k;
		}

		return new Sampler.Data(i, j, int2DoubleMap);
	}

	public boolean hasDeviated() {
		return this.deviationChecker != null && this.deviationChecker.check(this.currentSample);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		else if (o != null && this.getClass() == o.getClass()) {
			Sampler sampler = (Sampler) o;
			return this.name.equals(sampler.name) && this.type.equals(sampler.type);
		}
		else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	/**
	 * {@code Builder}.
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
			this.startAction = () -> action.accept(this.context);
			return this;
		}

		public Sampler.Builder<T> deviationChecker(Sampler.DeviationChecker deviationChecker) {
			this.deviationChecker = deviationChecker;
			return this;
		}

		/**
		 * Build.
		 *
		 * @return Sampler — результат операции
		 */
		public Sampler build() {
			return new Sampler(this.name, this.type, this.timeGetter, this.startAction, this.deviationChecker);
		}
	}

	/**
	 * {@code Data}.
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
			return this.values.get(tick);
		}

		public int getStartTick() {
			return this.startTick;
		}

		public int getEndTick() {
			return this.endTick;
		}
	}

	/**
	 * {@code DeviationChecker}.
	 */
	public interface DeviationChecker {

		boolean check(double value);
	}

	/**
	 * {@code RatioDeviationChecker}.
	 */
	public static class RatioDeviationChecker implements Sampler.DeviationChecker {

		private final float threshold;
		private double lastValue = Double.MIN_VALUE;

		public RatioDeviationChecker(float threshold) {
			this.threshold = threshold;
		}

		@Override
		public boolean check(double value) {
			boolean bl;
			if (this.lastValue != Double.MIN_VALUE && !(value <= this.lastValue)) {
				bl = (value - this.lastValue) / this.lastValue >= this.threshold;
			}
			else {
				bl = false;
			}

			this.lastValue = value;
			return bl;
		}
	}
}
