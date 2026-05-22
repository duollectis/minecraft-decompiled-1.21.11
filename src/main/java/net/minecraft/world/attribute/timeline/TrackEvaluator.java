package net.minecraft.world.attribute.timeline;

import net.minecraft.util.math.Interpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Вычислитель значений анимационного трека в произвольный момент времени.
 *
 * <p>При создании трек преобразуется в список {@link Segment сегментов} — пар соседних
 * ключевых кадров. Для периодических шкал добавляются граничные сегменты, обеспечивающие
 * плавный переход через границу периода (wrap-around).
 *
 * <p>Метод {@link #get(long)} находит нужный сегмент бинарным обходом и вычисляет
 * интерполированное значение с учётом {@link EasingType} трека.
 *
 * @param <T> тип значения ключевых кадров
 */
public class TrackEvaluator<T> {

	private final Optional<Integer> period;
	private final Interpolator<T> interpolator;
	private final List<Segment<T>> segments;

	TrackEvaluator(Track<T> track, Optional<Integer> period, Interpolator<T> interpolator) {
		this.period = period;
		this.interpolator = interpolator;
		this.segments = convertToSegments(track, period);
	}

	/**
	 * Преобразует трек в список сегментов интерполяции.
	 *
	 * <p>Для периодических треков добавляются два дополнительных сегмента:
	 * один перед первым кадром (от последнего кадра предыдущего периода) и
	 * один после последнего кадра (до первого кадра следующего периода).
	 * Это обеспечивает корректную интерполяцию на границах периода.
	 */
	private static <T> List<Segment<T>> convertToSegments(Track<T> track, Optional<Integer> period) {
		List<Keyframe<T>> keyframes = track.keyframes();

		if (keyframes.size() == 1) {
			T singleValue = keyframes.getFirst().value();
			return List.of(new Segment<>(EasingType.CONSTANT, singleValue, 0, singleValue, 0));
		}

		List<Segment<T>> result = new ArrayList<>();

		if (period.isPresent()) {
			int periodLength = period.get();
			Keyframe<T> first = keyframes.getFirst();
			Keyframe<T> last = keyframes.getLast();

			// Сегмент wrap-around: от последнего кадра предыдущего периода до первого кадра текущего
			result.add(new Segment<>(track, last, last.ticks() - periodLength, first, first.ticks()));
			addSegmentsOfKeyframes(track, keyframes, result);
			// Сегмент wrap-around: от последнего кадра текущего периода до первого кадра следующего
			result.add(new Segment<>(track, last, last.ticks(), first, first.ticks() + periodLength));
		} else {
			addSegmentsOfKeyframes(track, keyframes, result);
		}

		return List.copyOf(result);
	}

	private static <T> void addSegmentsOfKeyframes(
			Track<T> track,
			List<Keyframe<T>> keyframes,
			List<Segment<T>> segmentsOut
	) {
		for (int i = 0; i < keyframes.size() - 1; i++) {
			Keyframe<T> from = keyframes.get(i);
			Keyframe<T> to = keyframes.get(i + 1);
			segmentsOut.add(new Segment<>(track, from, from.ticks(), to, to.ticks()));
		}
	}

	/**
	 * Вычисляет значение трека в заданный момент времени.
	 *
	 * <p>Если время совпадает с границей сегмента, возвращается граничное значение без
	 * интерполяции. Иначе вычисляется нормализованный прогресс {@code t ∈ (0, 1)},
	 * к которому применяется {@link EasingType}, а затем — интерполятор.
	 *
	 * @param time абсолютное время в тиках
	 * @return интерполированное значение атрибута
	 */
	public T get(long time) {
		long normalizedTime = periodize(time);
		Segment<T> segment = getSegmentForTime(normalizedTime);

		if (normalizedTime <= segment.fromTicks) {
			return segment.fromValue;
		}

		if (normalizedTime >= segment.toTicks) {
			return segment.toValue;
		}

		float progress = (float) (normalizedTime - segment.fromTicks) / (segment.toTicks - segment.fromTicks);
		float easedProgress = segment.easing.apply(progress);

		return interpolator.apply(easedProgress, segment.fromValue, segment.toValue);
	}

	private Segment<T> getSegmentForTime(long time) {
		for (Segment<T> segment : segments) {
			if (time < segment.toTicks) {
				return segment;
			}
		}

		return segments.getLast();
	}

	private long periodize(long time) {
		return period.isPresent() ? Math.floorMod(time, period.get()) : time;
	}

	/**
	 * Сегмент интерполяции между двумя соседними ключевыми кадрами.
	 *
	 * @param <T> тип значения кадров
	 */
	record Segment<T>(EasingType easing, T fromValue, int fromTicks, T toValue, int toTicks) {

		Segment(Track<T> track, Keyframe<T> from, int fromTicks, Keyframe<T> to, int toTicks) {
			this(track.easingType(), from.value(), fromTicks, to.value(), toTicks);
		}
	}
}
