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
 * {@code Track}.
 */
public record Track<T>(List<Keyframe<T>> keyframes, EasingType easingType) {

	public Track(List<Keyframe<T>> keyframes, EasingType easingType) {
		if (keyframes.isEmpty()) {
			throw new IllegalArgumentException("Track has no keyframes");
		}
		else {
			this.keyframes = keyframes;
			this.easingType = easingType;
		}
	}

	/**
	 * Создаёт codec.
	 *
	 * @param valueCodec value codec
	 *
	 * @return MapCodec> — результат операции
	 */
	public static <T> MapCodec<Track<T>> createCodec(Codec<T> valueCodec) {
		Codec<List<Keyframe<T>>> codec = Keyframe.createCodec(valueCodec).listOf().validate(Track::validateKeyframes);
		return RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						                    codec.fieldOf("keyframes").forGetter(Track::keyframes),
						                    EasingType.CODEC.optionalFieldOf("ease", EasingType.LINEAR).forGetter(Track::easingType)
				                    )
				                    .apply(instance, Track::new)
		);
	}

	static <T> DataResult<List<Keyframe<T>>> validateKeyframes(List<Keyframe<T>> keyframes) {
		if (keyframes.isEmpty()) {
			return DataResult.error(() -> "Keyframes must not be empty");
		}
		else if (!Comparators.isInOrder(keyframes, Comparator.comparingInt(Keyframe::ticks))) {
			return DataResult.error(() -> "Keyframes must be ordered by ticks field");
		}
		else {
			if (keyframes.size() > 1) {
				int i = 0;
				int j = keyframes.getLast().ticks();

				for (Keyframe<T> keyframe : keyframes) {
					if (keyframe.ticks() == j) {
						if (++i > 2) {
							return DataResult.error(() -> "More than 2 keyframes on same tick: " + keyframe.ticks());
						}
					}
					else {
						i = 0;
					}

					j = keyframe.ticks();
				}
			}

			return DataResult.success(keyframes);
		}
	}

	/**
	 * Валидирует keyframes in period.
	 *
	 * @param track track
	 * @param period period
	 *
	 * @return DataResult> — результат операции
	 */
	public static DataResult<Track<?>> validateKeyframesInPeriod(Track<?> track, int period) {
		for (Keyframe<?> keyframe : track.keyframes()) {
			int i = keyframe.ticks();
			if (i < 0 || i > period) {
				return DataResult.error(() -> "Keyframe at tick " + keyframe.ticks() + " must be in range [0; " + period
						+ "]");
			}
		}

		return DataResult.success(track);
	}

	/**
	 * Создаёт evaluator.
	 *
	 * @param period period
	 * @param interpolator interpolator
	 *
	 * @return TrackEvaluator — результат операции
	 */
	public TrackEvaluator<T> createEvaluator(Optional<Integer> period, Interpolator<T> interpolator) {
		return new TrackEvaluator<>(this, period, interpolator);
	}

	/**
	 * {@code Builder}.
	 */
	public static class Builder<T> {

		private final com.google.common.collect.ImmutableList.Builder<Keyframe<T>> keyframes = ImmutableList.builder();
		private EasingType easingType = EasingType.LINEAR;

		public Track.Builder<T> keyframe(int ticks, T value) {
			this.keyframes.add(new Keyframe(ticks, value));
			return this;
		}

		public Track.Builder<T> easingType(EasingType easingType) {
			this.easingType = easingType;
			return this;
		}

		/**
		 * Build.
		 *
		 * @return Track — результат операции
		 */
		public Track<T> build() {
			List<Keyframe<T>> list = (List<Keyframe<T>>) Track.validateKeyframes(this.keyframes.build()).getOrThrow();
			return new Track<>(list, this.easingType);
		}
	}
}
