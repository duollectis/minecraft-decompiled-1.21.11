package net.minecraft.world.attribute.timeline;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.Easing;

import java.util.List;

/**
 * Функция сглаживания (easing) для интерполяции между ключевыми кадрами таймлайна.
 * Принимает нормализованный параметр {@code x ∈ [0; 1]} и возвращает
 * преобразованное значение в том же диапазоне.
 * <p>
 * Поддерживает именованные типы (зарегистрированные через {@link #register})
 * и произвольные кривые Безье ({@link CubicBezier}).
 */
public interface EasingType {

	Codecs.IdMapper<String, EasingType> EASING_TYPES_BY_NAME = new Codecs.IdMapper<>();

	/**
	 * Codec: именованный тип сериализуется как строка,
	 * кривая Безье — как объект с полем {@code cubic_bezier}.
	 */
	Codec<EasingType> CODEC = Codec.either(EASING_TYPES_BY_NAME.getCodec(Codec.STRING), CubicBezier.CODEC)
		.xmap(
			Either::unwrap,
			easing -> easing instanceof CubicBezier cubicBezier ? Either.right(cubicBezier) : Either.left(easing)
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

	/** Регистрирует именованный тип сглаживания. */
	static EasingType register(String name, EasingType easingType) {
		EASING_TYPES_BY_NAME.put(name, easingType);
		return easingType;
	}

	/**
	 * Создаёт кубическую кривую Безье с четырьмя контрольными точками.
	 *
	 * @param x1 X первой контрольной точки [0; 1]
	 * @param y1 Y первой контрольной точки
	 * @param x2 X второй контрольной точки [0; 1]
	 * @param y2 Y второй контрольной точки
	 */
	static EasingType cubicBezier(float x1, float y1, float x2, float y2) {
		return new CubicBezier(new CubicBezierControlPoints(x1, y1, x2, y2));
	}

	/**
	 * Создаёт симметричную кубическую кривую Безье.
	 * Вторая точка зеркально отражается: {@code (1-x1, 1-y1)}.
	 */
	static EasingType cubicBezierSymmetric(float x1, float y1) {
		return cubicBezier(x1, y1, 1.0F - x1, 1.0F - y1);
	}

	/**
	 * Применяет функцию сглаживания к нормализованному параметру.
	 *
	 * @param x нормализованный параметр [0; 1]
	 * @return преобразованное значение
	 */
	float apply(float x);

	/**
	 * Кубическая кривая Безье как функция сглаживания.
	 * Использует метод Ньютона для нахождения параметра t по значению x.
	 */
	final class CubicBezier implements EasingType {

		public static final Codec<CubicBezier> CODEC = RecordCodecBuilder.create(
			instance -> instance
				.group(CubicBezierControlPoints.CODEC.fieldOf("cubic_bezier").forGetter(easing -> easing.controlPoints))
				.apply(instance, CubicBezier::new)
		);

		/** Максимальное число итераций метода Ньютона для нахождения t по x. */
		private static final int MAX_NEWTON_ITERATIONS = 4;

		private final CubicBezierControlPoints controlPoints;
		private final Parameters xParams;
		private final Parameters yParams;

		public CubicBezier(CubicBezierControlPoints controlPoints) {
			this.controlPoints = controlPoints;
			xParams = computeParameters(controlPoints.x1, controlPoints.x2);
			yParams = computeParameters(controlPoints.y1, controlPoints.y2);
		}

		private static Parameters computeParameters(float z1, float z2) {
			return new Parameters(
				3.0F * z1 - 3.0F * z2 + 1.0F,
				-6.0F * z1 + 3.0F * z2,
				3.0F * z1
			);
		}

		/**
		 * Вычисляет значение кривой для параметра {@code x}.
		 * Сначала находит t через метод Ньютона по x-компоненте,
		 * затем вычисляет y-компоненту по найденному t.
		 */
		@Override
		public float apply(float x) {
			float t = x;

			for (int iteration = 0; iteration < MAX_NEWTON_ITERATIONS; iteration++) {
				float derivative = xParams.derivative(t);
				if (derivative < 1.0E-5F) {
					break;
				}

				float error = xParams.apply(t) - x;
				t -= error / derivative;
			}

			return yParams.apply(t);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof CubicBezier bezier && controlPoints.equals(bezier.controlPoints);
		}

		@Override
		public int hashCode() {
			return controlPoints.hashCode();
		}

		@Override
		public String toString() {
			return "CubicBezier("
				+ controlPoints.x1 + ", "
				+ controlPoints.y1 + ", "
				+ controlPoints.x2 + ", "
				+ controlPoints.y2 + ")";
		}

		/**
		 * Коэффициенты кубического полинома {@code ((a*t + b)*t + c)*t}.
		 * Используется для вычисления x(t) и y(t) кривой Безье.
		 */
		record Parameters(float a, float b, float c) {

			/** Вычисляет значение полинома: {@code ((a*t + b)*t + c)*t}. */
			public float apply(float t) {
				return ((a * t + b) * t + c) * t;
			}

			/** Вычисляет производную полинома: {@code (3*a*t + 2*b)*t + c}. */
			public float derivative(float t) {
				return (3.0F * a * t + 2.0F * b) * t + c;
			}
		}
	}

	/**
	 * Четыре контрольные точки кубической кривой Безье.
	 * Координаты x1 и x2 должны быть в диапазоне [0; 1].
	 */
	record CubicBezierControlPoints(float x1, float y1, float x2, float y2) {

		public static final Codec<CubicBezierControlPoints> CODEC = Codec.FLOAT
			.listOf(4, 4)
			.xmap(
				points -> new CubicBezierControlPoints(points.get(0), points.get(1), points.get(2), points.get(3)),
				points -> List.of(points.x1, points.y1, points.x2, points.y2)
			)
			.validate(CubicBezierControlPoints::validate);

		private DataResult<CubicBezierControlPoints> validate() {
			if (x1 < 0.0F || x1 > 1.0F) {
				return DataResult.error(() -> "x1 must be in range [0; 1]");
			}

			return x2 >= 0.0F && x2 <= 1.0F
				? DataResult.success(this)
				: DataResult.error(() -> "x2 must be in range [0; 1]");
		}
	}
}
