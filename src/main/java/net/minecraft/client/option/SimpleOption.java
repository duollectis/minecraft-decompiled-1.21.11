package net.minecraft.client.option;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Updatable;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.OptionSliderWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.IntStream;

/**
 * Типизированная настройка игры с поддержкой сериализации, валидации и создания виджета.
 * <p>
 * Каждая настройка имеет текущее значение, дефолтное значение, {@link Callbacks} для
 * создания виджета и валидации, а также {@link TooltipFactory} для подсказок.
 * Изменение значения через {@link #setValue} автоматически вызывает {@code changeCallback}
 * и валидирует новое значение через {@link Callbacks#validate}.
 *
 * @param <T> тип значения настройки
 */
@Environment(EnvType.CLIENT)
public final class SimpleOption<T> {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final SimpleOption.PotentialValuesBasedCallbacks<Boolean> BOOLEAN =
			new SimpleOption.PotentialValuesBasedCallbacks<>(ImmutableList.of(Boolean.TRUE, Boolean.FALSE), Codec.BOOL);

	public static final SimpleOption.ValueTextGetter<Boolean> BOOLEAN_TEXT_GETTER =
			(optionText, value) -> value ? ScreenTexts.ON : ScreenTexts.OFF;

	private final SimpleOption.TooltipFactory<T> tooltipFactory;
	final Function<T, Text> textGetter;
	private final SimpleOption.Callbacks<T> callbacks;
	private final Codec<T> codec;
	private final T defaultValue;
	private final Consumer<T> changeCallback;
	final Text text;
	private T value;

	public static SimpleOption<Boolean> ofBoolean(String key, boolean defaultValue, Consumer<Boolean> changeCallback) {
		return ofBoolean(key, emptyTooltip(), defaultValue, changeCallback);
	}

	public static SimpleOption<Boolean> ofBoolean(String key, boolean defaultValue) {
		return ofBoolean(key, emptyTooltip(), defaultValue, value -> {});
	}

	public static SimpleOption<Boolean> ofBoolean(
			String key,
			SimpleOption.TooltipFactory<Boolean> tooltipFactory,
			boolean defaultValue
	) {
		return ofBoolean(key, tooltipFactory, defaultValue, value -> {});
	}

	public static SimpleOption<Boolean> ofBoolean(
			String key,
			SimpleOption.TooltipFactory<Boolean> tooltipFactory,
			boolean defaultValue,
			Consumer<Boolean> changeCallback
	) {
		return ofBoolean(key, tooltipFactory, BOOLEAN_TEXT_GETTER, defaultValue, changeCallback);
	}

	public static SimpleOption<Boolean> ofBoolean(
			String key,
			SimpleOption.TooltipFactory<Boolean> tooltipFactory,
			SimpleOption.ValueTextGetter<Boolean> valueTextGetter,
			boolean defaultValue,
			Consumer<Boolean> changeCallback
	) {
		return new SimpleOption<>(key, tooltipFactory, valueTextGetter, BOOLEAN, defaultValue, changeCallback);
	}

	public SimpleOption(
			String key,
			SimpleOption.TooltipFactory<T> tooltipFactory,
			SimpleOption.ValueTextGetter<T> valueTextGetter,
			SimpleOption.Callbacks<T> callbacks,
			T defaultValue,
			Consumer<T> changeCallback
	) {
		this(key, tooltipFactory, valueTextGetter, callbacks, callbacks.codec(), defaultValue, changeCallback);
	}

	public SimpleOption(
			String key,
			SimpleOption.TooltipFactory<T> tooltipFactory,
			SimpleOption.ValueTextGetter<T> valueTextGetter,
			SimpleOption.Callbacks<T> callbacks,
			Codec<T> codec,
			T defaultValue,
			Consumer<T> changeCallback
	) {
		text = Text.translatable(key);
		this.tooltipFactory = tooltipFactory;
		textGetter = val -> valueTextGetter.toString(text, val);
		this.callbacks = callbacks;
		this.codec = codec;
		this.defaultValue = defaultValue;
		this.changeCallback = changeCallback;
		value = this.defaultValue;
	}

	public static <T> SimpleOption.TooltipFactory<T> emptyTooltip() {
		return value -> null;
	}

	public static <T> SimpleOption.TooltipFactory<T> constantTooltip(Text text) {
		return value -> Tooltip.of(text);
	}

	public ClickableWidget createWidget(GameOptions options) {
		return createWidget(options, 0, 0, 150);
	}

	public ClickableWidget createWidget(GameOptions options, int x, int y, int width) {
		return createWidget(options, x, y, width, val -> {});
	}

	public ClickableWidget createWidget(GameOptions options, int x, int y, int width, Consumer<T> changeCallback) {
		return callbacks.getWidgetCreator(tooltipFactory, options, x, y, width, changeCallback).apply(this);
	}

	public T getValue() {
		return value;
	}

	public Codec<T> getCodec() {
		return codec;
	}

	@Override
	public String toString() {
		return text.getString();
	}

	/**
	 * Устанавливает новое значение настройки с валидацией через {@link Callbacks#validate}.
	 * Если значение невалидно — используется {@link #defaultValue} с логированием ошибки.
	 * Если клиент запущен — вызывает {@code changeCallback} только при реальном изменении значения.
	 */
	public void setValue(T newValue) {
		T validated = callbacks.validate(newValue).orElseGet(() -> {
			LOGGER.error("Illegal option value {} for {}", newValue, text.getString());
			return defaultValue;
		});

		if (!MinecraftClient.getInstance().isRunning()) {
			value = validated;
			return;
		}

		if (!Objects.equals(value, validated)) {
			value = validated;
			changeCallback.accept(value);
		}
	}

	public SimpleOption.Callbacks<T> getCallbacks() {
		return callbacks;
	}

	/**
	 * Реализация {@link CyclingCallbacks} с поддержкой альтернативного набора значений,
	 * активируемого по условию {@code altCondition}. Используется, например, для
	 * переключения между наборами опций в зависимости от состояния игры.
	 */
	@Environment(EnvType.CLIENT)
	public record AlternateValuesSupportingCyclingCallbacks<T>(
			List<T> values,
			List<T> altValues,
			BooleanSupplier altCondition,
			SimpleOption.CyclingCallbacks.ValueSetter<T> valueSetter,
			Codec<T> codec
	) implements SimpleOption.CyclingCallbacks<T> {

		@Override
		public CyclingButtonWidget.Values<T> getValues() {
			return CyclingButtonWidget.Values.of(altCondition, values, altValues);
		}

		@Override
		public Optional<T> validate(T value) {
			return (altCondition.getAsBoolean() ? altValues : values).contains(value)
					? Optional.of(value)
					: Optional.empty();
		}
	}

	/**
	 * Базовый контракт для всех типов настроек: создание виджета, валидация значения и кодек.
	 */
	@Environment(EnvType.CLIENT)
	interface Callbacks<T> {

		Function<SimpleOption<T>, ClickableWidget> getWidgetCreator(
				SimpleOption.TooltipFactory<T> tooltipFactory,
				GameOptions gameOptions,
				int x,
				int y,
				int width,
				Consumer<T> changeCallback
		);

		Optional<T> validate(T value);

		Codec<T> codec();
	}

	/**
	 * Реализация {@link SliderCallbacks} для категориальных значений, отображаемых
	 * на слайдере. Позиция слайдера линейно отображается на индекс в списке {@code values}.
	 */
	@Environment(EnvType.CLIENT)
	public record CategoricalSliderCallbacks<T>(
			List<T> values,
			Codec<T> codec
	) implements SimpleOption.SliderCallbacks<T> {

		@Override
		public double toSliderProgress(T value) {
			if (value == values.getFirst()) {
				return 0.0;
			}

			return value == values.getLast()
					? 1.0
					: MathHelper.map((double) values.indexOf(value), 0.0, (double) (values.size() - 1), 0.0, 1.0);
		}

		@Override
		public Optional<T> getNext(T value) {
			int index = values.indexOf(value);
			return Optional.of(values.get(MathHelper.clamp(index + 1, 0, values.size() - 1)));
		}

		@Override
		public Optional<T> getPrevious(T value) {
			int index = values.indexOf(value);
			return Optional.of(values.get(MathHelper.clamp(index - 1, 0, values.size() - 1)));
		}

		@Override
		public T toValue(double sliderProgress) {
			double clamped = sliderProgress >= 1.0 ? 0.99999F : sliderProgress;
			int index = MathHelper.floor(MathHelper.map(clamped, 0.0, 1.0, 0.0, (double) values.size()));
			return values.get(MathHelper.clamp(index, 0, values.size() - 1));
		}

		@Override
		public Optional<T> validate(T value) {
			int index = values.indexOf(value);
			return index > -1 ? Optional.of(value) : Optional.empty();
		}
	}

	/**
	 * Реализация {@link Callbacks} для настроек с циклическим переключением значений.
	 * Создаёт {@link CyclingButtonWidget} с поддержкой тултипов и кастомного {@link ValueSetter}.
	 */
	@Environment(EnvType.CLIENT)
	interface CyclingCallbacks<T> extends SimpleOption.Callbacks<T> {

		CyclingButtonWidget.Values<T> getValues();

		default SimpleOption.CyclingCallbacks.ValueSetter<T> valueSetter() {
			return SimpleOption::setValue;
		}

		@Override
		default Function<SimpleOption<T>, ClickableWidget> getWidgetCreator(
				SimpleOption.TooltipFactory<T> tooltipFactory,
				GameOptions gameOptions,
				int x,
				int y,
				int width,
				Consumer<T> changeCallback
		) {
			return option -> CyclingButtonWidget.<T>builder(option.textGetter, option::getValue)
					.values(getValues())
					.tooltip(tooltipFactory)
					.build(
							x, y, width, 20, option.text, (button, value) -> {
								valueSetter().set(option, value);
								gameOptions.write();
								changeCallback.accept(value);
							}
					);
		}

		@Environment(EnvType.CLIENT)
		interface ValueSetter<T> {

			void set(SimpleOption<T> option, T value);
		}
	}

	/**
	 * Слайдер для значений типа {@code Double} в диапазоне [0.0, 1.0].
	 * Поддерживает модификацию через {@link #withModifier} для маппинга на произвольный тип.
	 */
	@Environment(EnvType.CLIENT)
	public enum DoubleSliderCallbacks implements SimpleOption.SliderCallbacks<Double> {
		INSTANCE;

		@Override
		public Optional<Double> validate(Double value) {
			return value >= 0.0 && value <= 1.0 ? Optional.of(value) : Optional.empty();
		}

		@Override
		public double toSliderProgress(Double value) {
			return value;
		}

		@Override
		public Double toValue(double sliderProgress) {
			return sliderProgress;
		}

		public <R> SimpleOption.SliderCallbacks<R> withModifier(
				DoubleFunction<? extends R> sliderProgressValueToValue,
				ToDoubleFunction<? super R> valueToSliderProgressValue
		) {
			return new SimpleOption.SliderCallbacks<R>() {
				@Override
				public Optional<R> validate(R value) {
					return DoubleSliderCallbacks.this
							.validate(valueToSliderProgressValue.applyAsDouble(value))
							.map(sliderProgressValueToValue::apply);
				}

				@Override
				public double toSliderProgress(R value) {
					return DoubleSliderCallbacks.this.toSliderProgress(valueToSliderProgressValue.applyAsDouble(value));
				}

				@Override
				public R toValue(double sliderProgress) {
					return (R) sliderProgressValueToValue.apply(DoubleSliderCallbacks.this.toValue(sliderProgress));
				}

				@Override
				public Codec<R> codec() {
					return DoubleSliderCallbacks.this
							.codec()
							.xmap(sliderProgressValueToValue::apply, valueToSliderProgressValue::applyAsDouble);
				}
			};
		}

		@Override
		public Codec<Double> codec() {
			return Codec.withAlternative(Codec.doubleRange(0.0, 1.0), Codec.BOOL, value -> value ? 1.0 : 0.0);
		}
	}

	/**
	 * Базовый интерфейс для целочисленных слайдеров с диапазоном [{@link #minInclusive}, {@link #maxInclusive}].
	 * Предоставляет дефолтные реализации конвертации между значением и позицией слайдера.
	 */
	@Environment(EnvType.CLIENT)
	interface IntSliderCallbacks extends SimpleOption.SliderCallbacks<Integer> {

		int minInclusive();

		int maxInclusive();

		default Optional<Integer> getNext(Integer integer) {
			return Optional.of(integer + 1);
		}

		default Optional<Integer> getPrevious(Integer integer) {
			return Optional.of(integer - 1);
		}

		default double toSliderProgress(Integer integer) {
			if (integer == minInclusive()) {
				return 0.0;
			}

			return integer == maxInclusive()
					? 1.0
					: MathHelper.map(integer.intValue() + 0.5, (double) minInclusive(), maxInclusive() + 1.0, 0.0, 1.0);
		}

		default Integer toValue(double sliderProgress) {
			double clamped = sliderProgress >= 1.0 ? 0.99999F : sliderProgress;
			return MathHelper.floor(MathHelper.map(clamped, 0.0, 1.0, (double) minInclusive(), maxInclusive() + 1.0));
		}

		default <R> SimpleOption.SliderCallbacks<R> withModifier(
				IntFunction<? extends R> sliderProgressValueToValue,
				ToIntFunction<? super R> valueToSliderProgressValue,
				boolean supportsCycling
		) {
			return new SimpleOption.SliderCallbacks<R>() {
				@Override
				public Optional<R> validate(R value) {
					return IntSliderCallbacks.this
							.validate(valueToSliderProgressValue.applyAsInt(value))
							.map(sliderProgressValueToValue::apply);
				}

				@Override
				public double toSliderProgress(R value) {
					return IntSliderCallbacks.this.toSliderProgress(valueToSliderProgressValue.applyAsInt(value));
				}

				@Override
				public Optional<R> getNext(R value) {
					if (!supportsCycling) {
						return Optional.empty();
					}

					int intValue = valueToSliderProgressValue.applyAsInt(value);
					return (Optional<R>) Optional.of(
							sliderProgressValueToValue.apply(IntSliderCallbacks.this.validate(intValue + 1).orElse(intValue))
					);
				}

				@Override
				public Optional<R> getPrevious(R value) {
					if (!supportsCycling) {
						return Optional.empty();
					}

					int intValue = valueToSliderProgressValue.applyAsInt(value);
					return (Optional<R>) Optional.of(
							sliderProgressValueToValue.apply(IntSliderCallbacks.this.validate(intValue - 1).orElse(intValue))
					);
				}

				@Override
				public R toValue(double sliderProgress) {
					return (R) sliderProgressValueToValue.apply(IntSliderCallbacks.this.toValue(sliderProgress));
				}

				@Override
				public Codec<R> codec() {
					return IntSliderCallbacks.this
							.codec()
							.xmap(sliderProgressValueToValue::apply, valueToSliderProgressValue::applyAsInt);
				}
			};
		}
	}

	/**
	 * Реализация {@link CyclingCallbacks} с ленивой загрузкой списка значений.
	 * Используется для настроек, чей список значений зависит от состояния игры
	 * (например, список аудиоустройств).
	 */
	@Environment(EnvType.CLIENT)
	public record LazyCyclingCallbacks<T>(
			Supplier<List<T>> values,
			Function<T, Optional<T>> validateValue,
			Codec<T> codec
	) implements SimpleOption.CyclingCallbacks<T> {

		@Override
		public Optional<T> validate(T value) {
			return validateValue.apply(value);
		}

		@Override
		public CyclingButtonWidget.Values<T> getValues() {
			return CyclingButtonWidget.Values.of(values.get());
		}
	}

	/**
	 * Реализация {@link IntSliderCallbacks} с динамическим максимумом через {@code maxSupplier}.
	 * Также реализует {@link TypeChangeableCallbacks} для переключения между слайдером и кнопкой.
	 * Используется для настроек типа «дальность прорисовки», где максимум зависит от RAM.
	 */
	@Environment(EnvType.CLIENT)
	public record MaxSuppliableIntCallbacks(int minInclusive, IntSupplier maxSupplier, int encodableMaxInclusive)
			implements SimpleOption.IntSliderCallbacks, SimpleOption.TypeChangeableCallbacks<Integer> {

		@Override
		public Optional<Integer> validate(Integer integer) {
			return Optional.of(MathHelper.clamp(integer, minInclusive(), maxInclusive()));
		}

		@Override
		public int maxInclusive() {
			return maxSupplier.getAsInt();
		}

		@Override
		public Codec<Integer> codec() {
			return Codec.INT.validate(value -> {
				int upperBound = encodableMaxInclusive + 1;
				return value.compareTo(minInclusive) >= 0 && value.compareTo(upperBound) <= 0
						? DataResult.success(value)
						: DataResult.error(
								() -> "Value " + value + " outside of range [" + minInclusive + ":" + upperBound + "]",
								value
						);
			});
		}

		@Override
		public boolean isCycling() {
			return true;
		}

		@Override
		public CyclingButtonWidget.Values<Integer> getValues() {
			return CyclingButtonWidget.Values.of(
					IntStream.range(minInclusive, maxInclusive() + 1).boxed().toList()
			);
		}
	}

	/**
	 * Виджет слайдера для настройки типа {@link SimpleOption}.
	 * Поддерживает отложенное применение значения (через {@code timeToApply}) для
	 * настроек, где немедленное применение дорого (например, перестройка чанков).
	 */
	@Environment(EnvType.CLIENT)
	public static final class OptionSliderWidgetImpl<N> extends OptionSliderWidget implements Updatable {

		private static final long APPLY_DELAY_MS = 600L;

		private final SimpleOption<N> option;
		private final SimpleOption.SliderCallbacks<N> callbacks;
		private final SimpleOption.TooltipFactory<N> tooltipFactory;
		private final Consumer<N> changeCallback;
		private @Nullable Long timeToApply;
		private final boolean shouldApplyImmediately;

		OptionSliderWidgetImpl(
				GameOptions options,
				int x,
				int y,
				int width,
				int height,
				SimpleOption<N> option,
				SimpleOption.SliderCallbacks<N> callbacks,
				SimpleOption.TooltipFactory<N> tooltipFactory,
				Consumer<N> changeCallback,
				boolean shouldApplyImmediately
		) {
			super(options, x, y, width, height, callbacks.toSliderProgress(option.getValue()));
			this.option = option;
			this.callbacks = callbacks;
			this.tooltipFactory = tooltipFactory;
			this.changeCallback = changeCallback;
			this.shouldApplyImmediately = shouldApplyImmediately;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(option.textGetter.apply(callbacks.toValue(value)));
			setTooltip(tooltipFactory.apply(callbacks.toValue(value)));
		}

		@Override
		protected void applyValue() {
			if (shouldApplyImmediately) {
				applyPendingValue();
			}
			else {
				timeToApply = Util.getMeasuringTimeMs() + APPLY_DELAY_MS;
			}
		}

		public void applyPendingValue() {
			N newValue = callbacks.toValue(value);

			if (!Objects.equals(newValue, option.getValue())) {
				option.setValue(newValue);
				changeCallback.accept(option.getValue());
			}
		}

		@Override
		public void update() {
			if (value == callbacks.toSliderProgress(option.getValue())) {
				return;
			}

			value = callbacks.toSliderProgress(option.getValue());
			timeToApply = null;
			updateMessage();
		}

		@Override
		public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
			super.renderWidget(context, mouseX, mouseY, deltaTicks);

			if (timeToApply != null && Util.getMeasuringTimeMs() >= timeToApply) {
				timeToApply = null;
				applyPendingValue();
				update();
			}
		}

		@Override
		public void onRelease(Click click) {
			super.onRelease(click);

			if (shouldApplyImmediately) {
				update();
			}
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			if (input.isEnterOrSpace()) {
				sliderFocused = !sliderFocused;
				return true;
			}

			if (!sliderFocused) {
				return false;
			}

			boolean movingLeft = input.isLeft();
			boolean movingRight = input.isRight();

			if (movingLeft) {
				Optional<N> previous = callbacks.getPrevious(callbacks.toValue(value));

				if (previous.isPresent()) {
					setValue(callbacks.toSliderProgress(previous.get()));
					return true;
				}
			}

			if (movingRight) {
				Optional<N> next = callbacks.getNext(callbacks.toValue(value));

				if (next.isPresent()) {
					setValue(callbacks.toSliderProgress(next.get()));
					return true;
				}
			}

			if (movingLeft || movingRight) {
				float direction = movingLeft ? -1.0F : 1.0F;
				setValue(value + direction / (width - 8));
				return true;
			}

			return false;
		}
	}

	/**
	 * Реализация {@link CyclingCallbacks} на основе фиксированного списка допустимых значений.
	 */
	@Environment(EnvType.CLIENT)
	public record PotentialValuesBasedCallbacks<T>(
			List<T> values,
			Codec<T> codec
	) implements SimpleOption.CyclingCallbacks<T> {

		@Override
		public Optional<T> validate(T value) {
			return values.contains(value) ? Optional.of(value) : Optional.empty();
		}

		@Override
		public CyclingButtonWidget.Values<T> getValues() {
			return CyclingButtonWidget.Values.of(values);
		}
	}

	/**
	 * Базовый интерфейс для настроек, отображаемых слайдером.
	 * Определяет конвертацию между значением типа {@code T} и позицией слайдера [0.0, 1.0].
	 */
	@Environment(EnvType.CLIENT)
	interface SliderCallbacks<T> extends SimpleOption.Callbacks<T> {

		double toSliderProgress(T value);

		default Optional<T> getNext(T value) {
			return Optional.empty();
		}

		default Optional<T> getPrevious(T value) {
			return Optional.empty();
		}

		T toValue(double sliderProgress);

		default boolean applyValueImmediately() {
			return true;
		}

		@Override
		default Function<SimpleOption<T>, ClickableWidget> getWidgetCreator(
				SimpleOption.TooltipFactory<T> tooltipFactory,
				GameOptions gameOptions,
				int x,
				int y,
				int width,
				Consumer<T> changeCallback
		) {
			return option -> new SimpleOption.OptionSliderWidgetImpl<>(
					gameOptions,
					x,
					y,
					width,
					20,
					option,
					this,
					tooltipFactory,
					changeCallback,
					applyValueImmediately()
			);
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface TooltipFactory<T> {

		@Nullable Tooltip apply(T value);
	}

	/**
	 * Интерфейс для настроек, которые могут отображаться как слайдером, так и кнопкой.
	 * Выбор типа виджета определяется методом {@link #isCycling()}.
	 */
	@Environment(EnvType.CLIENT)
	interface TypeChangeableCallbacks<T> extends SimpleOption.CyclingCallbacks<T>, SimpleOption.SliderCallbacks<T> {

		boolean isCycling();

		@Override
		default Function<SimpleOption<T>, ClickableWidget> getWidgetCreator(
				SimpleOption.TooltipFactory<T> tooltipFactory,
				GameOptions gameOptions,
				int x,
				int y,
				int width,
				Consumer<T> changeCallback
		) {
			return isCycling()
					? SimpleOption.CyclingCallbacks.super.getWidgetCreator(tooltipFactory, gameOptions, x, y, width, changeCallback)
					: SimpleOption.SliderCallbacks.super.getWidgetCreator(tooltipFactory, gameOptions, x, y, width, changeCallback);
		}
	}

	/**
	 * Реализация {@link IntSliderCallbacks} с жёстко заданным диапазоном [{@code minInclusive}, {@code maxInclusive}].
	 */
	@Environment(EnvType.CLIENT)
	public record ValidatingIntSliderCallbacks(
			int minInclusive,
			int maxInclusive,
			boolean applyValueImmediately
	) implements SimpleOption.IntSliderCallbacks {

		public ValidatingIntSliderCallbacks(int minInclusive, int maxInclusive) {
			this(minInclusive, maxInclusive, true);
		}

		@Override
		public Optional<Integer> validate(Integer integer) {
			return integer.compareTo(minInclusive()) >= 0 && integer.compareTo(maxInclusive()) <= 0
					? Optional.of(integer)
					: Optional.empty();
		}

		@Override
		public Codec<Integer> codec() {
			return Codec.intRange(minInclusive, maxInclusive + 1);
		}
	}

	@Environment(EnvType.CLIENT)
	public interface ValueTextGetter<T> {

		Text toString(Text optionText, T value);
	}
}
