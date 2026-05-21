package net.minecraft.world.attribute.timeline;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.Easing;

import java.util.List;

/**
 * {@code EasingType}.
 */
public interface EasingType {

	Codecs.IdMapper<String, EasingType> EASING_TYPES_BY_NAME = new Codecs.IdMapper<>();

	Codec<EasingType> CODEC = Codec.either(EASING_TYPES_BY_NAME.getCodec(Codec.STRING), EasingType.CubicBezier.CODEC)
	                               .xmap(
			                               Either::unwrap,
			                               easing -> easing instanceof EasingType.CubicBezier cubicBezier
			                                         ? Either.right(cubicBezier) : Either.left(easing)
	                               );

	EasingType CONSTANT = register("constant", x -> 0.0F);

	EasingType LINEAR = register("linear", x -> x);

	EasingType IN_BACK = register("in_back", Easing::inBack);

	EasingType IN_BOUNCE = register("in_bounce", Easing::inBounce);

	EasingType IN_CIRC = register("in_circ", Easing::inCirc);

	EasingType IN_CUBIC = register("in_cubic", Easing::inCubic);

	EasingType IN_ELASTIC = register("in_elastic", Easing::inElastic);

	EasingType IN_EXPO = register("in_expo", Easing::inExpo);

	EasingType IN_QUAD = register("in_quad", Easing::inQuad);

	EasingType IN_QUART = register("in_quart", Easing::inQuart);

	EasingType IN_QUINT = register("in_quint", Easing::inQuint);

	EasingType IN_SINE = register("in_sine", Easing::inSine);

	EasingType IN_OUT_BACK = register("in_out_back", Easing::inOutBack);

	EasingType IN_OUT_BOUNCE = register("in_out_bounce", Easing::inOutBounce);

	EasingType IN_OUT_CIRC = register("in_out_circ", Easing::inOutCirc);

	EasingType IN_OUT_CUBIC = register("in_out_cubic", Easing::inOutCubic);

	EasingType IN_OUT_ELASTIC = register("in_out_elastic", Easing::inOutElastic);

	EasingType IN_OUT_EXPO = register("in_out_expo", Easing::inOutExpo);

	EasingType IN_OUT_QUAD = register("in_out_quad", Easing::inOutQuad);

	EasingType IN_OUT_QUART = register("in_out_quart", Easing::inOutQuart);

	EasingType IN_OUT_QUINT = register("in_out_quint", Easing::inOutQuint);

	EasingType IN_OUT_SINE = register("in_out_sine", Easing::inOutSine);

	EasingType OUT_BACK = register("out_back", Easing::outBack);

	EasingType OUT_BOUNCE = register("out_bounce", Easing::outBounce);

	EasingType OUT_CIRC = register("out_circ", Easing::outCirc);

	EasingType OUT_CUBIC = register("out_cubic", Easing::outCubic);

	EasingType OUT_ELASTIC = register("out_elastic", Easing::outElastic);

	EasingType OUT_EXPO = register("out_expo", Easing::outExpo);

	EasingType OUT_QUAD = register("out_quad", Easing::outQuad);

	EasingType OUT_QUART = register("out_quart", Easing::outQuart);

	EasingType OUT_QUINT = register("out_quint", Easing::outQuint);

	EasingType OUT_SINE = register("out_sine", Easing::outSine);

	static EasingType register(String name, EasingType easingType) {
		EASING_TYPES_BY_NAME.put(name, easingType);
		return easingType;
	}

	static EasingType cubicBezier(float x1, float y1, float x2, float y2) {
		return new EasingType.CubicBezier(new EasingType.CubicBezierControlPoints(x1, y1, x2, y2));
	}

	static EasingType cubicBezierSymmetric(float x1, float y1) {
		return cubicBezier(x1, y1, 1.0F - x1, 1.0F - y1);
	}

	float apply(float x);

	/**
	 * {@code CubicBezier}.
	 */
	public static final class CubicBezier implements EasingType {

		public static final Codec<EasingType.CubicBezier> CODEC = RecordCodecBuilder.create(
				instance -> instance
						.group(EasingType.CubicBezierControlPoints.CODEC
								.fieldOf("cubic_bezier")
								.forGetter(easing -> easing.controlPoints))
						.apply(instance, EasingType.CubicBezier::new)
		);
		private static final int MAX_NEWTON_ITERATIONS = 4;
		private final EasingType.CubicBezierControlPoints controlPoints;
		private final EasingType.CubicBezier.Parameters xParams;
		private final EasingType.CubicBezier.Parameters yParams;

		public CubicBezier(EasingType.CubicBezierControlPoints controlPoints) {
			this.controlPoints = controlPoints;
			this.xParams = computeParameters(controlPoints.x1, controlPoints.x2);
			this.yParams = computeParameters(controlPoints.y1, controlPoints.y2);
		}

		private static EasingType.CubicBezier.Parameters computeParameters(float z1, float z2) {
			return new EasingType.CubicBezier.Parameters(
					3.0F * z1 - 3.0F * z2 + 1.0F,
					-6.0F * z1 + 3.0F * z2,
					3.0F * z1
			);
		}

		@Override
		public float apply(float f) {
			float g = f;

			for (int i = 0; i < 4; i++) {
				float h = this.xParams.derivative(g);
				if (h < 1.0E-5F) {
					break;
				}

				float j = this.xParams.apply(g) - f;
				g -= j / h;
			}

			return this.yParams.apply(g);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof EasingType.CubicBezier cubicBezier
					&& this.controlPoints.equals(cubicBezier.controlPoints);
		}

		@Override
		public int hashCode() {
			return this.controlPoints.hashCode();
		}

		@Override
		public String toString() {
			return "CubicBezier(" + this.controlPoints.x1 + ", " + this.controlPoints.y1 + ", " + this.controlPoints.x2
					+ ", " + this.controlPoints.y2 + ")";
		}

		/**
		 * {@code Parameters}.
		 */
		record Parameters(float a, float b, float c) {

			/**
			 * Apply.
			 *
			 * @param t t
			 *
			 * @return float — результат операции
			 */
			public float apply(float t) {
				return ((this.a * t + this.b) * t + this.c) * t;
			}

			/**
			 * Derivative.
			 *
			 * @param t t
			 *
			 * @return float — результат операции
			 */
			public float derivative(float t) {
				return (3.0F * this.a * t + 2.0F * this.b) * t + this.c;
			}
		}
	}

	/**
	 * {@code CubicBezierControlPoints}.
	 */
	public record CubicBezierControlPoints(float x1, float y1, float x2, float y2) {

		public static final Codec<EasingType.CubicBezierControlPoints> CODEC = Codec.FLOAT
				.listOf(4, 4)
				.xmap(
						points -> new EasingType.CubicBezierControlPoints(
								(Float) points.get(0),
								(Float) points.get(1),
								(Float) points.get(2),
								(Float) points.get(3)
						),
						points -> List.of(points.x1, points.y1, points.x2, points.y2)
				)
				.validate(EasingType.CubicBezierControlPoints::validate);

		private DataResult<EasingType.CubicBezierControlPoints> validate() {
			if (this.x1 < 0.0F || this.x1 > 1.0F) {
				return DataResult.error(() -> "x1 must be in range [0; 1]");
			}
			else {
				return !(this.x2 < 0.0F) && !(this.x2 > 1.0F) ? DataResult.success(this)
				                                              : DataResult.error(() -> "x2 must be in range [0; 1]");
			}
		}
	}
}
