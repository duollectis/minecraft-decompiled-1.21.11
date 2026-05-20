package net.minecraft.client.option;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
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

@Environment(EnvType.CLIENT)
public final class SimpleOption<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final SimpleOption.PotentialValuesBasedCallbacks<Boolean> BOOLEAN = new SimpleOption.PotentialValuesBasedCallbacks<>(
      ImmutableList.of(Boolean.TRUE, Boolean.FALSE), Codec.BOOL
   );
   public static final SimpleOption.ValueTextGetter<Boolean> BOOLEAN_TEXT_GETTER = (optionText, value) -> value ? ScreenTexts.ON : ScreenTexts.OFF;
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

   public static SimpleOption<Boolean> ofBoolean(String key, SimpleOption.TooltipFactory<Boolean> tooltipFactory, boolean defaultValue) {
      return ofBoolean(key, tooltipFactory, defaultValue, value -> {});
   }

   public static SimpleOption<Boolean> ofBoolean(
      String key, SimpleOption.TooltipFactory<Boolean> tooltipFactory, boolean defaultValue, Consumer<Boolean> changeCallback
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
      this.text = Text.translatable(key);
      this.tooltipFactory = tooltipFactory;
      this.textGetter = value -> valueTextGetter.toString(this.text, value);
      this.callbacks = callbacks;
      this.codec = codec;
      this.defaultValue = defaultValue;
      this.changeCallback = changeCallback;
      this.value = this.defaultValue;
   }

   public static <T> SimpleOption.TooltipFactory<T> emptyTooltip() {
      return value -> null;
   }

   public static <T> SimpleOption.TooltipFactory<T> constantTooltip(Text text) {
      return value -> Tooltip.of(text);
   }

   public ClickableWidget createWidget(GameOptions options) {
      return this.createWidget(options, 0, 0, 150);
   }

   public ClickableWidget createWidget(GameOptions options, int x, int y, int width) {
      return this.createWidget(options, x, y, width, value -> {});
   }

   public ClickableWidget createWidget(GameOptions options, int x, int y, int width, Consumer<T> changeCallback) {
      return this.callbacks.getWidgetCreator(this.tooltipFactory, options, x, y, width, changeCallback).apply(this);
   }

   public T getValue() {
      return this.value;
   }

   public Codec<T> getCodec() {
      return this.codec;
   }

   @Override
   public String toString() {
      return this.text.getString();
   }

   public void setValue(T value) {
      T object = this.callbacks.validate(value).orElseGet(() -> {
         LOGGER.error("Illegal option value {} for {}", value, this.text.getString());
         return this.defaultValue;
      });
      if (!MinecraftClient.getInstance().isRunning()) {
         this.value = object;
      } else {
         if (!Objects.equals(this.value, object)) {
            this.value = object;
            this.changeCallback.accept(this.value);
         }
      }
   }

   public SimpleOption.Callbacks<T> getCallbacks() {
      return this.callbacks;
   }

   @Environment(EnvType.CLIENT)
   public record AlternateValuesSupportingCyclingCallbacks<T>(
      List<T> values, List<T> altValues, BooleanSupplier altCondition, SimpleOption.CyclingCallbacks.ValueSetter<T> valueSetter, Codec<T> codec
   ) implements SimpleOption.CyclingCallbacks<T> {
      @Override
      public CyclingButtonWidget.Values<T> getValues() {
         return CyclingButtonWidget.Values.of(this.altCondition, this.values, this.altValues);
      }

      @Override
      public Optional<T> validate(T value) {
         return (this.altCondition.getAsBoolean() ? this.altValues : this.values).contains(value) ? Optional.of(value) : Optional.empty();
      }
   }

   @Environment(EnvType.CLIENT)
   interface Callbacks<T> {
      Function<SimpleOption<T>, ClickableWidget> getWidgetCreator(
         SimpleOption.TooltipFactory<T> tooltipFactory, GameOptions gameOptions, int x, int y, int width, Consumer<T> changeCallback
      );

      Optional<T> validate(T value);

      Codec<T> codec();
   }

   @Environment(EnvType.CLIENT)
   public record CategoricalSliderCallbacks<T>(List<T> values, Codec<T> codec) implements SimpleOption.SliderCallbacks<T> {
      @Override
      public double toSliderProgress(T value) {
         if (value == this.values.getFirst()) {
            return 0.0;
         } else {
            return value == this.values.getLast() ? 1.0 : MathHelper.map((double)this.values.indexOf(value), 0.0, (double)(this.values.size() - 1), 0.0, 1.0);
         }
      }

      @Override
      public Optional<T> getNext(T value) {
         int i = this.values.indexOf(value);
         int j = MathHelper.clamp(i + 1, 0, this.values.size() - 1);
         return Optional.of(this.values.get(j));
      }

      @Override
      public Optional<T> getPrevious(T value) {
         int i = this.values.indexOf(value);
         int j = MathHelper.clamp(i - 1, 0, this.values.size() - 1);
         return Optional.of(this.values.get(j));
      }

      @Override
      public T toValue(double sliderProgress) {
         if (sliderProgress >= 1.0) {
            sliderProgress = 0.99999F;
         }

         int i = MathHelper.floor(MathHelper.map(sliderProgress, 0.0, 1.0, 0.0, (double)this.values.size()));
         return this.values.get(MathHelper.clamp(i, 0, this.values.size() - 1));
      }

      @Override
      public Optional<T> validate(T value) {
         int i = this.values.indexOf(value);
         return i > -1 ? Optional.of(value) : Optional.empty();
      }
   }

   @Environment(EnvType.CLIENT)
   interface CyclingCallbacks<T> extends SimpleOption.Callbacks<T> {
      CyclingButtonWidget.Values<T> getValues();

      default SimpleOption.CyclingCallbacks.ValueSetter<T> valueSetter() {
         return SimpleOption::setValue;
      }

      @Override
      default Function<SimpleOption<T>, ClickableWidget> getWidgetCreator(
         SimpleOption.TooltipFactory<T> tooltipFactory, GameOptions gameOptions, int x, int y, int width, Consumer<T> changeCallback
      ) {
         return option -> CyclingButtonWidget.<T>builder(option.textGetter, option::getValue)
            .values(this.getValues())
            .tooltip(tooltipFactory)
            .build(x, y, width, 20, option.text, (button, value) -> {
               this.valueSetter().set(option, value);
               gameOptions.write();
               changeCallback.accept(value);
            });
      }

      @Environment(EnvType.CLIENT)
      public interface ValueSetter<T> {
         void set(SimpleOption<T> option, T value);
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum DoubleSliderCallbacks implements SimpleOption.SliderCallbacks<Double> {
      INSTANCE;

      public Optional<Double> validate(Double double_) {
         return double_ >= 0.0 && double_ <= 1.0 ? Optional.of(double_) : Optional.empty();
      }

      public double toSliderProgress(Double double_) {
         return double_;
      }

      public Double toValue(double d) {
         return d;
      }

      public <R> SimpleOption.SliderCallbacks<R> withModifier(
         DoubleFunction<? extends R> sliderProgressValueToValue, ToDoubleFunction<? super R> valueToSliderProgressValue
      ) {
         return new SimpleOption.SliderCallbacks<R>() {
            @Override
            public Optional<R> validate(R value) {
               return DoubleSliderCallbacks.this.validate(valueToSliderProgressValue.applyAsDouble(value)).map(sliderProgressValueToValue::apply);
            }

            @Override
            public double toSliderProgress(R value) {
               return DoubleSliderCallbacks.this.toSliderProgress(valueToSliderProgressValue.applyAsDouble(value));
            }

            @Override
            public R toValue(double sliderProgress) {
               return (R)sliderProgressValueToValue.apply(DoubleSliderCallbacks.this.toValue(sliderProgress));
            }

            @Override
            public Codec<R> codec() {
               return DoubleSliderCallbacks.this.codec().xmap(sliderProgressValueToValue::apply, valueToSliderProgressValue::applyAsDouble);
            }
         };
      }

      @Override
      public Codec<Double> codec() {
         return Codec.withAlternative(Codec.doubleRange(0.0, 1.0), Codec.BOOL, value -> value ? 1.0 : 0.0);
      }
   }

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
         if (integer == this.minInclusive()) {
            return 0.0;
         } else {
            return integer == this.maxInclusive()
               ? 1.0
               : MathHelper.map(integer.intValue() + 0.5, (double)this.minInclusive(), this.maxInclusive() + 1.0, 0.0, 1.0);
         }
      }

      default Integer toValue(double d) {
         if (d >= 1.0) {
            d = 0.99999F;
         }

         return MathHelper.floor(MathHelper.map(d, 0.0, 1.0, (double)this.minInclusive(), this.maxInclusive() + 1.0));
      }

      default <R> SimpleOption.SliderCallbacks<R> withModifier(
         IntFunction<? extends R> sliderProgressValueToValue, ToIntFunction<? super R> valueToSliderProgressValue, boolean bl
      ) {
         return new SimpleOption.SliderCallbacks<R>() {
            @Override
            public Optional<R> validate(R value) {
               return IntSliderCallbacks.this.validate(valueToSliderProgressValue.applyAsInt(value)).map(sliderProgressValueToValue::apply);
            }

            @Override
            public double toSliderProgress(R value) {
               return IntSliderCallbacks.this.toSliderProgress(valueToSliderProgressValue.applyAsInt(value));
            }

            @Override
            public Optional<R> getNext(R value) {
               if (!bl) {
                  return Optional.empty();
               } else {
                  int i = valueToSliderProgressValue.applyAsInt(value);
                  return (Optional<R>)Optional.of(sliderProgressValueToValue.apply(IntSliderCallbacks.this.validate(i + 1).orElse(i)));
               }
            }

            @Override
            public Optional<R> getPrevious(R value) {
               if (!bl) {
                  return Optional.empty();
               } else {
                  int i = valueToSliderProgressValue.applyAsInt(value);
                  return (Optional<R>)Optional.of(sliderProgressValueToValue.apply(IntSliderCallbacks.this.validate(i - 1).orElse(i)));
               }
            }

            @Override
            public R toValue(double sliderProgress) {
               return (R)sliderProgressValueToValue.apply(IntSliderCallbacks.this.toValue(sliderProgress));
            }

            @Override
            public Codec<R> codec() {
               return IntSliderCallbacks.this.codec().xmap(sliderProgressValueToValue::apply, valueToSliderProgressValue::applyAsInt);
            }
         };
      }
   }

   @Environment(EnvType.CLIENT)
   public record LazyCyclingCallbacks<T>(Supplier<List<T>> values, Function<T, Optional<T>> validateValue, Codec<T> codec)
      implements SimpleOption.CyclingCallbacks<T> {
      @Override
      public Optional<T> validate(T value) {
         return this.validateValue.apply(value);
      }

      @Override
      public CyclingButtonWidget.Values<T> getValues() {
         return CyclingButtonWidget.Values.of(this.values.get());
      }
   }

   @Environment(EnvType.CLIENT)
   public record MaxSuppliableIntCallbacks(int minInclusive, IntSupplier maxSupplier, int encodableMaxInclusive)
      implements SimpleOption.IntSliderCallbacks,
      SimpleOption.TypeChangeableCallbacks<Integer> {
      public Optional<Integer> validate(Integer integer) {
         return Optional.of(MathHelper.clamp(integer, this.minInclusive(), this.maxInclusive()));
      }

      @Override
      public int maxInclusive() {
         return this.maxSupplier.getAsInt();
      }

      @Override
      public Codec<Integer> codec() {
         return Codec.INT
            .validate(
               value -> {
                  int i = this.encodableMaxInclusive + 1;
                  return value.compareTo(this.minInclusive) >= 0 && value.compareTo(i) <= 0
                     ? DataResult.success(value)
                     : DataResult.error(() -> "Value " + value + " outside of range [" + this.minInclusive + ":" + i + "]", value);
               }
            );
      }

      @Override
      public boolean isCycling() {
         return true;
      }

      @Override
      public CyclingButtonWidget.Values<Integer> getValues() {
         return CyclingButtonWidget.Values.of(IntStream.range(this.minInclusive, this.maxInclusive() + 1).boxed().toList());
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class OptionSliderWidgetImpl<N> extends OptionSliderWidget implements Updatable {
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
         this.updateMessage();
      }

      @Override
      protected void updateMessage() {
         this.setMessage(this.option.textGetter.apply(this.callbacks.toValue(this.value)));
         this.setTooltip(this.tooltipFactory.apply(this.callbacks.toValue(this.value)));
      }

      @Override
      protected void applyValue() {
         if (this.shouldApplyImmediately) {
            this.applyPendingValue();
         } else {
            this.timeToApply = Util.getMeasuringTimeMs() + 600L;
         }
      }

      public void applyPendingValue() {
         N object = this.callbacks.toValue(this.value);
         if (!Objects.equals(object, this.option.getValue())) {
            this.option.setValue(object);
            this.changeCallback.accept(this.option.getValue());
         }
      }

      @Override
      public void update() {
         if (this.value != this.callbacks.toSliderProgress(this.option.getValue())) {
            this.value = this.callbacks.toSliderProgress(this.option.getValue());
            this.timeToApply = null;
            this.updateMessage();
         }
      }

      @Override
      public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
         super.renderWidget(context, mouseX, mouseY, deltaTicks);
         if (this.timeToApply != null && Util.getMeasuringTimeMs() >= this.timeToApply) {
            this.timeToApply = null;
            this.applyPendingValue();
            this.update();
         }
      }

      @Override
      public void onRelease(Click click) {
         super.onRelease(click);
         if (this.shouldApplyImmediately) {
            this.update();
         }
      }

      @Override
      public boolean keyPressed(KeyInput input) {
         if (input.isEnterOrSpace()) {
            this.sliderFocused = !this.sliderFocused;
            return true;
         } else {
            if (this.sliderFocused) {
               boolean bl = input.isLeft();
               boolean bl2 = input.isRight();
               if (bl) {
                  Optional<N> optional = this.callbacks.getPrevious(this.callbacks.toValue(this.value));
                  if (optional.isPresent()) {
                     this.setValue(this.callbacks.toSliderProgress(optional.get()));
                     return true;
                  }
               }

               if (bl2) {
                  Optional<N> optional = this.callbacks.getNext(this.callbacks.toValue(this.value));
                  if (optional.isPresent()) {
                     this.setValue(this.callbacks.toSliderProgress(optional.get()));
                     return true;
                  }
               }

               if (bl || bl2) {
                  float f = bl ? -1.0F : 1.0F;
                  this.setValue(this.value + f / (this.width - 8));
                  return true;
               }
            }

            return false;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public record PotentialValuesBasedCallbacks<T>(List<T> values, Codec<T> codec) implements SimpleOption.CyclingCallbacks<T> {
      @Override
      public Optional<T> validate(T value) {
         return this.values.contains(value) ? Optional.of(value) : Optional.empty();
      }

      @Override
      public CyclingButtonWidget.Values<T> getValues() {
         return CyclingButtonWidget.Values.of(this.values);
      }
   }

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
         SimpleOption.TooltipFactory<T> tooltipFactory, GameOptions gameOptions, int x, int y, int width, Consumer<T> changeCallback
      ) {
         return option -> new SimpleOption.OptionSliderWidgetImpl<>(
            gameOptions, x, y, width, 20, option, this, tooltipFactory, changeCallback, this.applyValueImmediately()
         );
      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   public interface TooltipFactory<T> {
      @Nullable Tooltip apply(T value);
   }

   @Environment(EnvType.CLIENT)
   interface TypeChangeableCallbacks<T> extends SimpleOption.CyclingCallbacks<T>, SimpleOption.SliderCallbacks<T> {
      boolean isCycling();

      @Override
      default Function<SimpleOption<T>, ClickableWidget> getWidgetCreator(
         SimpleOption.TooltipFactory<T> tooltipFactory, GameOptions gameOptions, int x, int y, int width, Consumer<T> changeCallback
      ) {
         return this.isCycling()
            ? SimpleOption.CyclingCallbacks.super.getWidgetCreator(tooltipFactory, gameOptions, x, y, width, changeCallback)
            : SimpleOption.SliderCallbacks.super.getWidgetCreator(tooltipFactory, gameOptions, x, y, width, changeCallback);
      }
   }

   @Environment(EnvType.CLIENT)
   public record ValidatingIntSliderCallbacks(int minInclusive, int maxInclusive, boolean applyValueImmediately) implements SimpleOption.IntSliderCallbacks {
      public ValidatingIntSliderCallbacks(int minInclusive, int maxInclusive) {
         this(minInclusive, maxInclusive, true);
      }

      public Optional<Integer> validate(Integer integer) {
         return integer.compareTo(this.minInclusive()) >= 0 && integer.compareTo(this.maxInclusive()) <= 0 ? Optional.of(integer) : Optional.empty();
      }

      @Override
      public Codec<Integer> codec() {
         return Codec.intRange(this.minInclusive, this.maxInclusive + 1);
      }
   }

   @Environment(EnvType.CLIENT)
   public interface ValueTextGetter<T> {
      Text toString(Text optionText, T value);
   }
}
