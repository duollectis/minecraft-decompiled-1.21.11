package net.minecraft.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.MathHelper;

/**
 * Предикат движения сущности. Проверяет скорость по осям, горизонтальную,
 * вертикальную скорость и дистанцию падения.
 */
public record MovementPredicate(
		NumberRange.DoubleRange x,
		NumberRange.DoubleRange y,
		NumberRange.DoubleRange z,
		NumberRange.DoubleRange speed,
		NumberRange.DoubleRange horizontalSpeed,
		NumberRange.DoubleRange verticalSpeed,
		NumberRange.DoubleRange fallDistance
) {

	public static final Codec<MovementPredicate> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					NumberRange.DoubleRange.CODEC.optionalFieldOf("x", NumberRange.DoubleRange.ANY).forGetter(MovementPredicate::x),
					NumberRange.DoubleRange.CODEC.optionalFieldOf("y", NumberRange.DoubleRange.ANY).forGetter(MovementPredicate::y),
					NumberRange.DoubleRange.CODEC.optionalFieldOf("z", NumberRange.DoubleRange.ANY).forGetter(MovementPredicate::z),
					NumberRange.DoubleRange.CODEC
							.optionalFieldOf("speed", NumberRange.DoubleRange.ANY)
							.forGetter(MovementPredicate::speed),
					NumberRange.DoubleRange.CODEC
							.optionalFieldOf("horizontal_speed", NumberRange.DoubleRange.ANY)
							.forGetter(MovementPredicate::horizontalSpeed),
					NumberRange.DoubleRange.CODEC
							.optionalFieldOf("vertical_speed", NumberRange.DoubleRange.ANY)
							.forGetter(MovementPredicate::verticalSpeed),
					NumberRange.DoubleRange.CODEC
							.optionalFieldOf("fall_distance", NumberRange.DoubleRange.ANY)
							.forGetter(MovementPredicate::fallDistance)
			).apply(instance, MovementPredicate::new)
	);

	public static MovementPredicate speed(NumberRange.DoubleRange speed) {
		return new MovementPredicate(
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				speed,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY
		);
	}

	public static MovementPredicate horizontalSpeed(NumberRange.DoubleRange horizontalSpeed) {
		return new MovementPredicate(
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				horizontalSpeed,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY
		);
	}

	public static MovementPredicate verticalSpeed(NumberRange.DoubleRange verticalSpeed) {
		return new MovementPredicate(
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				verticalSpeed,
				NumberRange.DoubleRange.ANY
		);
	}

	public static MovementPredicate fallDistance(NumberRange.DoubleRange fallDistance) {
		return new MovementPredicate(
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				NumberRange.DoubleRange.ANY,
				fallDistance
		);
	}

	public boolean test(double x, double y, double z, double fallDistance) {
		if (!this.x.test(x) || !this.y.test(y) || !this.z.test(z)) {
			return false;
		}

		if (!speed.testSqrt(MathHelper.squaredMagnitude(x, y, z))) {
			return false;
		}

		if (!horizontalSpeed.testSqrt(MathHelper.squaredHypot(x, z))) {
			return false;
		}

		if (!verticalSpeed.test(Math.abs(y))) {
			return false;
		}

		return this.fallDistance.test(fallDistance);
	}
}
