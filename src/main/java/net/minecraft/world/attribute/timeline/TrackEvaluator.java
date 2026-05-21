package net.minecraft.world.attribute.timeline;

import net.minecraft.util.math.Interpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code TrackEvaluator}.
 */
public class TrackEvaluator<T> {

	private final Optional<Integer> period;
	private final Interpolator<T> interpolator;
	private final List<TrackEvaluator.Segment<T>> segments;

	TrackEvaluator(Track<T> track, Optional<Integer> period, Interpolator<T> interpolator) {
		this.period = period;
		this.interpolator = interpolator;
		this.segments = convertToSegments(track, period);
	}

	private static <T> List<TrackEvaluator.Segment<T>> convertToSegments(Track<T> track, Optional<Integer> period) {
		List<Keyframe<T>> list = track.keyframes();
		if (list.size() == 1) {
			T object = list.getFirst().value();
			return List.of(new TrackEvaluator.Segment<>(EasingType.CONSTANT, object, 0, object, 0));
		}
		else {
			List<TrackEvaluator.Segment<T>> list2 = new ArrayList<>();
			if (period.isPresent()) {
				Keyframe<T> keyframe = list.getFirst();
				Keyframe<T> keyframe2 = list.getLast();
				list2.add(new TrackEvaluator.Segment<>(
						track,
						keyframe2,
						keyframe2.ticks() - period.get(),
						keyframe,
						keyframe.ticks()
				));
				addSegmentsOfKeyframe(track, list, list2);
				list2.add(new TrackEvaluator.Segment<>(
						track,
						keyframe2,
						keyframe2.ticks(),
						keyframe,
						keyframe.ticks() + period.get()
				));
			}
			else {
				addSegmentsOfKeyframe(track, list, list2);
			}

			return List.copyOf(list2);
		}
	}

	private static <T> void addSegmentsOfKeyframe(
			Track<T> track,
			List<Keyframe<T>> keyframes,
			List<TrackEvaluator.Segment<T>> segmentsOut
	) {
		for (int i = 0; i < keyframes.size() - 1; i++) {
			Keyframe<T> keyframe = keyframes.get(i);
			Keyframe<T> keyframe2 = keyframes.get(i + 1);
			segmentsOut.add(new TrackEvaluator.Segment<>(
					track,
					keyframe,
					keyframe.ticks(),
					keyframe2,
					keyframe2.ticks()
			));
		}
	}

	public T get(long time) {
		long l = this.periodize(time);
		TrackEvaluator.Segment<T> segment = this.getSegmentForTime(l);
		if (l <= segment.fromTicks) {
			return segment.fromValue;
		}
		else if (l >= segment.toTicks) {
			return segment.toValue;
		}
		else {
			float f = (float) (l - segment.fromTicks) / (segment.toTicks - segment.fromTicks);
			float g = segment.easing.apply(f);
			return this.interpolator.apply(g, segment.fromValue, segment.toValue);
		}
	}

	private TrackEvaluator.Segment<T> getSegmentForTime(long time) {
		for (TrackEvaluator.Segment<T> segment : this.segments) {
			if (time < segment.toTicks) {
				return segment;
			}
		}

		return this.segments.getLast();
	}

	private long periodize(long time) {
		return this.period.isPresent() ? Math.floorMod(time, this.period.get()) : time;
	}

	/**
	 * {@code Segment}.
	 */
	record Segment<T>(EasingType easing, T fromValue, int fromTicks, T toValue, int toTicks) {

		public Segment(Track<T> track, Keyframe<T> fromKeyframe, int fromTicks, Keyframe<T> toKeyframe, int toTicks) {
			this(track.easingType(), fromKeyframe.value(), fromTicks, toKeyframe.value(), toTicks);
		}
	}
}
