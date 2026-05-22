package net.minecraft.world.attribute.timeline;

import com.google.common.collect.Comparators;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.Interpolator;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Анимационный трек — упорядоченный список {@link Keyframe ключевых кадров} с заданным
 * типом плавности ({@link EasingType}).
 *
 * <p>Трек должен содержать хотя бы один кадр. Кадры обязаны быть отсортированы по полю
 * {@code ticks} в порядке возрастания. На одном тике допускается не более двух кадров
 * (для задания мгновенного перехода без интерполяции).
 *
 * @param <T> тип значения ключевых кадров
 */
public record Track<T>(List<Keyframe<T>> keyframes, EasingType easingType) {

	public Track(List<Keyframe<T>> keyframes, EasingType easingType) {
		if (keyframes.isEmpty()) {
			throw new IllegalArgumentException("Track has no keyframes");
		}

		this.keyframes = keyframes;
		this.easingType = easingType;
	}

	/**
	 * Создаёт MapCodec для трека с произвольным типом значения.
	 * Список кадров проходит валидацию {@link #validateKeyframes} при десериализации.
	 *
	 * @param <T>        тип значения кадров
	 * @param valueCodec codec для сериализации значений кадров
	 * @return MapCodec, кодирующий трек как объект с полями {@code keyframes} и {@code ease}
	 */
	public static <T> MapCodec<Track<T>> createCodec(Codec<T> valueCodec) {
		Codec<List<Keyframe<T>>> keyframesCodec = Keyframe.createCodec(valueCodec)
				.listOf()
				.validate(Track::validateKeyframes);

		return RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						keyframesCodec.fieldOf("keyframes").forGetter(Track::keyframes),
						EasingType.CODEC.optionalFieldOf("ease", EasingType.LINEAR).forGetter(Track::easingType)
				).apply(instance, Track::new)
		);
	}

	/**
	 * Проверяет корректность списка ключевых кадров:
	 * <ul>
	 *   <li>список не пуст;</li>
	 *   <li>кадры отсортированы по возрастанию тиков;</li>
	 *   <li>на одном тике не более двух кадров.</li>
	 * </ul>
	 *
	 * @param <T>       тип значения кадров
	 * @param keyframes список кадров для проверки
	 * @return {@code DataResult.success} при успехе, иначе — ошибка с описанием нарушения
	 */
	static <T> DataResult<List<Keyframe<T>>> validateKeyframes(List<Keyframe<T>> keyframes) {
		if (keyframes.isEmpty()) {
			return DataResult.error(() -> "Keyframes must not be empty");
		}

		if (!Comparators.isInOrder(keyframes, Comparator.comparingInt(Keyframe::ticks))) {
			return DataResult.error(() -> "Keyframes must be ordered by ticks field");
		}

		if (keyframes.size() > 1) {
			int duplicateCount = 0;
			int lastTick = keyframes.getLast().ticks();

			for (Keyframe<T> keyframe : keyframes) {
				if (keyframe.ticks() == lastTick) {
					if (++duplicateCount > 2) {
						return DataResult.error(() -> "More than 2 keyframes on same tick: " + keyframe.ticks());
					}
				} else {
					duplicateCount = 0;
				}

				lastTick = keyframe.ticks();
			}
		}

		return DataResult.success(keyframes);
	}

	/**
	 * Проверяет, что все ключевые кадры трека укладываются в диапазон {@code [0; period]}.
	 *
	 * @param track  трек для проверки
	 * @param period длина периода в тиках
	 * @return {@code DataResult.success(track)} если все кадры в диапазоне, иначе — ошибка
	 */
	public static DataResult<Track<?>> validateKeyframesInPeriod(Track<?> track, int period) {
		for (Keyframe<?> keyframe : track.keyframes()) {
			int tick = keyframe.ticks();

			if (tick < 0 || tick > period) {
				return DataResult.error(
						() -> "Keyframe at tick " + keyframe.ticks() + " must be in range [0; " + period + "]"
				);
			}
		}

		return DataResult.success(track);
	}

	/**
	 * Создаёт {@link TrackEvaluator} для вычисления значений трека в произвольный момент времени.
	 *
	 * @param period       период шкалы (если задан, время берётся по модулю)
	 * @param interpolator функция интерполяции между значениями кадров
	 * @return готовый к использованию evaluator
	 */
	public TrackEvaluator<T> createEvaluator(Optional<Integer> period, Interpolator<T> interpolator) {
		return new TrackEvaluator<>(this, period, interpolator);
	}

	/**
	 * Строитель {@link Track}.
	 * Позволяет декларативно добавлять ключевые кадры и задавать тип плавности.
	 *
	 * @param <T> тип значения кадров
	 */
	public static class Builder<T> {

		private final ImmutableList.Builder<Keyframe<T>> keyframes = ImmutableList.builder();
		private EasingType easingType = EasingType.LINEAR;

		public Track.Builder<T> keyframe(int ticks, T value) {
			keyframes.add(new Keyframe<>(ticks, value));
			return this;
		}

		public Track.Builder<T> easingType(EasingType easingType) {
			this.easingType = easingType;
			return this;
		}

		@SuppressWarnings("unchecked")
		public Track<T> build() {
			List<Keyframe<T>> validated = (List<Keyframe<T>>) Track.validateKeyframes(keyframes.build()).getOrThrow();
			return new Track<>(validated, easingType);
		}
	}
}
