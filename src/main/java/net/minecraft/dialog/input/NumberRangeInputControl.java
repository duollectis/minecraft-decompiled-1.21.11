package net.minecraft.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.dialog.type.Dialog;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;

/**
 * Элемент управления вводом числового значения через слайдер.
 * <p>
 * Поддерживает дискретный шаг ({@link RangeInfo#step}) и начальное значение.
 * Метка форматируется через ключ перевода {@link #labelFormat}.
 *
 * @param width       ширина слайдера в пикселях
 * @param label       текст метки
 * @param labelFormat ключ перевода для форматирования метки со значением
 * @param rangeInfo   параметры диапазона значений
 */
public record NumberRangeInputControl(
	int width,
	Text label,
	String labelFormat,
	RangeInfo rangeInfo
) implements InputControl {

	public static final MapCodec<NumberRangeInputControl> CODEC = RecordCodecBuilder.<NumberRangeInputControl>mapCodec(
		instance -> instance.group(
			Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(NumberRangeInputControl::width),
			TextCodecs.CODEC.fieldOf("label").forGetter(NumberRangeInputControl::label),
			Codec.STRING.optionalFieldOf("label_format", "options.generic_value").forGetter(NumberRangeInputControl::labelFormat),
			RangeInfo.CODEC.forGetter(NumberRangeInputControl::rangeInfo)
		).apply(instance, NumberRangeInputControl::new)
	);

	@Override
	public MapCodec<NumberRangeInputControl> getCodec() {
		return CODEC;
	}

	/**
	 * Форматирует метку с текущим значением слайдера.
	 *
	 * @param value текущее строковое значение
	 * @return отформатированный текст метки
	 */
	public Text getFormattedLabel(String value) {
		return Text.translatable(labelFormat, label, value);
	}

	/**
	 * Параметры диапазона числового слайдера.
	 *
	 * @param start   начало диапазона
	 * @param end     конец диапазона
	 * @param initial опциональное начальное значение (по умолчанию — середина диапазона)
	 * @param step    опциональный дискретный шаг
	 */
	public record RangeInfo(float start, float end, Optional<Float> initial, Optional<Float> step) {

		public static final MapCodec<RangeInfo> CODEC = RecordCodecBuilder.<RangeInfo>mapCodec(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("start").forGetter(RangeInfo::start),
				Codec.FLOAT.fieldOf("end").forGetter(RangeInfo::end),
				Codec.FLOAT.optionalFieldOf("initial").forGetter(RangeInfo::initial),
				Codecs.POSITIVE_FLOAT.optionalFieldOf("step").forGetter(RangeInfo::step)
			).apply(instance, RangeInfo::new)
		).validate(rangeInfo -> {
			if (rangeInfo.initial.isEmpty()) {
				return DataResult.success(rangeInfo);
			}

			double initialValue = rangeInfo.initial.get().floatValue();
			double minValue = Math.min(rangeInfo.start, rangeInfo.end);
			double maxValue = Math.max(rangeInfo.start, rangeInfo.end);

			return initialValue < minValue || initialValue > maxValue
				? DataResult.error(() -> "Initial value " + initialValue + " is outside of range [" + minValue + ", " + maxValue + "]")
				: DataResult.success(rangeInfo);
		});

		/**
		 * Преобразует прогресс слайдера (0.0–1.0) в значение диапазона с учётом шага.
		 *
		 * @param sliderProgress прогресс слайдера от 0.0 до 1.0
		 * @return значение в диапазоне, выровненное по шагу если задан
		 */
		public float sliderProgressToValue(float sliderProgress) {
			float interpolated = MathHelper.lerp(sliderProgress, start, end);

			if (step.isEmpty()) {
				return interpolated;
			}

			float stepSize = step.get();
			float origin = getInitialValue();
			float delta = interpolated - origin;
			int steps = Math.round(delta / stepSize);
			float snapped = origin + steps * stepSize;

			return isValueOutOfRange(snapped)
				? origin + (steps - MathHelper.sign(steps)) * stepSize
				: snapped;
		}

		private boolean isValueOutOfRange(float value) {
			float progress = valueToSliderProgress(value);
			return progress < 0.0 || progress > 1.0;
		}

		private float getInitialValue() {
			return initial.isPresent() ? initial.get() : (start + end) / 2.0F;
		}

		/**
		 * Возвращает начальный прогресс слайдера (0.0–1.0) для начального значения.
		 *
		 * @return начальный прогресс слайдера
		 */
		public float getInitialSliderProgress() {
			return valueToSliderProgress(getInitialValue());
		}

		private float valueToSliderProgress(float value) {
			return start == end ? 0.5F : MathHelper.getLerpProgress(value, start, end);
		}
	}
}
