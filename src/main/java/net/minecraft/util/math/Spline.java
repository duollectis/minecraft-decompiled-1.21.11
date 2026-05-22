package net.minecraft.util.math;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.function.ToFloatFunction;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Кубический сплайн для интерполяции float-значений по произвольной функции координаты.
 * Поддерживает сериализацию через Codec и обход узлов через {@link Visitor}.
 */
public interface Spline<C, I extends ToFloatFunction<C>> extends ToFloatFunction<C> {

	@Debug
	String getDebugString();

	Spline<C, I> apply(Spline.Visitor<I> visitor);

	/**
	 * Создаёт рекурсивный Codec для сплайна, поддерживающий как константные значения,
	 * так и полноценные многоточечные реализации с производными.
	 *
	 * @param locationFunctionCodec кодек для функции координаты
	 * @return кодек для {@code Spline<C, I>}
	 */
	static <C, I extends ToFloatFunction<C>> Codec<Spline<C, I>> createCodec(Codec<I> locationFunctionCodec) {
		MutableObject<Codec<Spline<C, I>>> mutableObject = new MutableObject<>();

		record Serialized<C, I extends ToFloatFunction<C>>(float location, Spline<C, I> value, float derivative) {
		}

		Codec<Serialized<C, I>> pointCodec = RecordCodecBuilder.create(
			instance -> instance.group(
				Codec.FLOAT.fieldOf("location").forGetter(Serialized::location),
				Codec.lazyInitialized(mutableObject).fieldOf("value").forGetter(Serialized::value),
				Codec.FLOAT.fieldOf("derivative").forGetter(Serialized::derivative)
			).apply(instance, (location, value, derivative) -> new Serialized<>(location, value, derivative))
		);

		Codec<Spline.Implementation<C, I>> implCodec = RecordCodecBuilder.create(
			instance -> instance.group(
				locationFunctionCodec.fieldOf("coordinate").forGetter(Spline.Implementation::locationFunction),
				Codecs.nonEmptyList(pointCodec.listOf())
					.fieldOf("points")
					.forGetter(
						(Spline.Implementation<C, I> spline) -> IntStream
							.range(0, spline.locations().length)
							.mapToObj(index -> new Serialized<C, I>(
								spline.locations()[index],
								(Spline<C, I>) spline.values().get(index),
								spline.derivatives()[index]
							))
							.toList()
					)
			).apply(instance, (locationFunction, splines) -> {
				float[] locations = new float[splines.size()];
				ImmutableList.Builder<Spline<C, I>> builder = ImmutableList.builder();
				float[] derivatives = new float[splines.size()];

				for (int i = 0; i < splines.size(); i++) {
					Serialized<C, I> serialized = (Serialized<C, I>) splines.get(i);
					locations[i] = serialized.location();
					builder.add(serialized.value());
					derivatives[i] = serialized.derivative();
				}

				return Spline.Implementation.build((I) locationFunction, locations, builder.build(), derivatives);
			})
		);

		mutableObject.setValue(
			Codec.either(Codec.FLOAT, implCodec)
				.xmap(
					either -> (Spline) either.map(Spline.FixedFloatFunction::new, spline -> spline),
					spline -> spline instanceof Spline.FixedFloatFunction<C, I> fixedFloatFunction
						? Either.left(fixedFloatFunction.value())
						: Either.right((Spline.Implementation) spline)
				)
		);

		return (Codec<Spline<C, I>>) mutableObject.get();
	}

	static <C, I extends ToFloatFunction<C>> Spline<C, I> fixedFloatFunction(float value) {
		return new Spline.FixedFloatFunction<>(value);
	}

	static <C, I extends ToFloatFunction<C>> Spline.Builder<C, I> builder(I locationFunction) {
		return new Spline.Builder<>(locationFunction);
	}

	static <C, I extends ToFloatFunction<C>> Spline.Builder<C, I> builder(
		I locationFunction,
		ToFloatFunction<Float> amplifier
	) {
		return new Spline.Builder<>(locationFunction, amplifier);
	}

	/**
	 * Строитель многоточечного сплайна. Точки должны добавляться в порядке возрастания координаты.
	 */
	final class Builder<C, I extends ToFloatFunction<C>> {

		private final I locationFunction;
		private final ToFloatFunction<Float> amplifier;
		private final FloatList locations = new FloatArrayList();
		private final List<Spline<C, I>> values = Lists.newArrayList();
		private final FloatList derivatives = new FloatArrayList();

		protected Builder(I locationFunction) {
			this(locationFunction, ToFloatFunction.IDENTITY);
		}

		protected Builder(I locationFunction, ToFloatFunction<Float> amplifier) {
			this.locationFunction = locationFunction;
			this.amplifier = amplifier;
		}

		public Spline.Builder<C, I> add(float location, float value) {
			return addPoint(location, new Spline.FixedFloatFunction<>(amplifier.apply(value)), 0.0F);
		}

		public Spline.Builder<C, I> add(float location, float value, float derivative) {
			return addPoint(location, new Spline.FixedFloatFunction<>(amplifier.apply(value)), derivative);
		}

		public Spline.Builder<C, I> add(float location, Spline<C, I> value) {
			return addPoint(location, value, 0.0F);
		}

		private Spline.Builder<C, I> addPoint(float location, Spline<C, I> value, float derivative) {
			if (!locations.isEmpty() && location <= locations.getFloat(locations.size() - 1)) {
				throw new IllegalArgumentException("Please register points in ascending order");
			}

			locations.add(location);
			values.add(value);
			derivatives.add(derivative);

			return this;
		}

		public Spline<C, I> build() {
			if (locations.isEmpty()) {
				throw new IllegalStateException("No elements added");
			}

			return Spline.Implementation.build(
				locationFunction,
				locations.toFloatArray(),
				ImmutableList.copyOf(values),
				derivatives.toFloatArray()
			);
		}
	}

	/**
	 * Константная функция, всегда возвращающая одно и то же значение.
	 */
	@Debug
	record FixedFloatFunction<C, I extends ToFloatFunction<C>>(float value) implements Spline<C, I> {

		@Override
		public float apply(C x) {
			return value;
		}

		@Override
		public String getDebugString() {
			return String.format(Locale.ROOT, "k=%.3f", value);
		}

		@Override
		public float min() {
			return value;
		}

		@Override
		public float max() {
			return value;
		}

		@Override
		public Spline<C, I> apply(Spline.Visitor<I> visitor) {
			return this;
		}
	}

	/**
	 * Многоточечная реализация кубического сплайна Эрмита.
	 * Хранит предвычисленные границы min/max для быстрой проверки диапазона.
	 */
	@Debug
	record Implementation<C, I extends ToFloatFunction<C>>(
		I locationFunction,
		float[] locations,
		List<Spline<C, I>> values,
		float[] derivatives,
		float min,
		float max
	) implements Spline<C, I> {

		public Implementation(
			I locationFunction,
			float[] locations,
			List<Spline<C, I>> values,
			float[] derivatives,
			float min,
			float max
		) {
			assertParametersValid(locations, values, derivatives);
			this.locationFunction = locationFunction;
			this.locations = locations;
			this.values = values;
			this.derivatives = derivatives;
			this.min = min;
			this.max = max;
		}

		/**
		 * Строит реализацию сплайна с предвычислением глобальных min/max,
		 * учитывая поведение за пределами диапазона (линейная экстраполяция по производной).
		 */
		static <C, I extends ToFloatFunction<C>> Spline.Implementation<C, I> build(
			I locationFunction,
			float[] locations,
			List<Spline<C, I>> values,
			float[] derivatives
		) {
			assertParametersValid(locations, values, derivatives);
			int lastIndex = locations.length - 1;
			float globalMin = Float.POSITIVE_INFINITY;
			float globalMax = Float.NEGATIVE_INFINITY;
			float funcMin = locationFunction.min();
			float funcMax = locationFunction.max();

			if (funcMin < locations[0]) {
				float sampleMin = sampleOutsideRange(funcMin, locations, values.get(0).min(), derivatives, 0);
				float sampleMax = sampleOutsideRange(funcMin, locations, values.get(0).max(), derivatives, 0);
				globalMin = Math.min(globalMin, Math.min(sampleMin, sampleMax));
				globalMax = Math.max(globalMax, Math.max(sampleMin, sampleMax));
			}

			if (funcMax > locations[lastIndex]) {
				float sampleMin = sampleOutsideRange(funcMax, locations, values.get(lastIndex).min(), derivatives, lastIndex);
				float sampleMax = sampleOutsideRange(funcMax, locations, values.get(lastIndex).max(), derivatives, lastIndex);
				globalMin = Math.min(globalMin, Math.min(sampleMin, sampleMax));
				globalMax = Math.max(globalMax, Math.max(sampleMin, sampleMax));
			}

			for (Spline<C, I> spline : values) {
				globalMin = Math.min(globalMin, spline.min());
				globalMax = Math.max(globalMax, spline.max());
			}

			for (int m = 0; m < lastIndex; m++) {
				float locLeft = locations[m];
				float locRight = locations[m + 1];
				float span = locRight - locLeft;
				Spline<C, I> splineLeft = values.get(m);
				Spline<C, I> splineRight = values.get(m + 1);
				float valLeftMin = splineLeft.min();
				float valLeftMax = splineLeft.max();
				float valRightMin = splineRight.min();
				float valRightMax = splineRight.max();
				float derivLeft = derivatives[m];
				float derivRight = derivatives[m + 1];

				if (derivLeft != 0.0F || derivRight != 0.0F) {
					float scaledLeft = derivLeft * span;
					float scaledRight = derivRight * span;
					float rangeMin = Math.min(valLeftMin, valRightMin);
					float rangeMax = Math.max(valLeftMax, valRightMax);
					float z1 = scaledLeft - valRightMax + valLeftMin;
					float z2 = scaledLeft - valRightMin + valLeftMax;
					float z3 = -scaledRight + valRightMin - valLeftMax;
					float z4 = -scaledRight + valRightMax - valLeftMin;
					globalMin = Math.min(globalMin, rangeMin + 0.25F * Math.min(z1, z3));
					globalMax = Math.max(globalMax, rangeMax + 0.25F * Math.max(z2, z4));
				}
			}

			return new Spline.Implementation<>(locationFunction, locations, values, derivatives, globalMin, globalMax);
		}

		private static float sampleOutsideRange(
			float point,
			float[] locations,
			float value,
			float[] derivatives,
			int index
		) {
			float deriv = derivatives[index];
			return deriv == 0.0F ? value : value + deriv * (point - locations[index]);
		}

		private static <C, I extends ToFloatFunction<C>> void assertParametersValid(
			float[] locations,
			List<Spline<C, I>> values,
			float[] derivatives
		) {
			if (locations.length != values.size() || locations.length != derivatives.length) {
				throw new IllegalArgumentException(
					"All lengths must be equal, got: " + locations.length + " " + values.size() + " " + derivatives.length
				);
			}

			if (locations.length == 0) {
				throw new IllegalArgumentException("Cannot create a multipoint spline with no points");
			}
		}

		@Override
		public float apply(C x) {
			float coord = locationFunction.apply(x);
			int index = findRangeForLocation(locations, coord);
			int lastIndex = locations.length - 1;

			if (index < 0) {
				return sampleOutsideRange(coord, locations, values.get(0).apply(x), derivatives, 0);
			}

			if (index == lastIndex) {
				return sampleOutsideRange(coord, locations, values.get(lastIndex).apply(x), derivatives, lastIndex);
			}

			float locLeft = locations[index];
			float locRight = locations[index + 1];
			float t = (coord - locLeft) / (locRight - locLeft);
			ToFloatFunction<C> funcLeft = (ToFloatFunction<C>) values.get(index);
			ToFloatFunction<C> funcRight = (ToFloatFunction<C>) values.get(index + 1);
			float derivLeft = derivatives[index];
			float derivRight = derivatives[index + 1];
			float valLeft = funcLeft.apply(x);
			float valRight = funcRight.apply(x);
			float p = derivLeft * (locRight - locLeft) - (valRight - valLeft);
			float q = -derivRight * (locRight - locLeft) + (valRight - valLeft);

			return MathHelper.lerp(t, valLeft, valRight) + t * (1.0F - t) * MathHelper.lerp(t, p, q);
		}

		private static int findRangeForLocation(float[] locations, float x) {
			return MathHelper.binarySearch(0, locations.length, i -> x < locations[i]) - 1;
		}

		@VisibleForTesting
		@Override
		public String getDebugString() {
			return "Spline{coordinate="
				+ locationFunction
				+ ", locations="
				+ format(locations)
				+ ", derivatives="
				+ format(derivatives)
				+ ", values="
				+ values.stream().map(Spline::getDebugString).collect(Collectors.joining(", ", "[", "]"))
				+ "}";
		}

		private String format(float[] floats) {
			return "["
				+ IntStream.range(0, floats.length)
					.mapToDouble(index -> floats[index])
					.mapToObj(value -> String.format(Locale.ROOT, "%.3f", value))
					.collect(Collectors.joining(", "))
				+ "]";
		}

		@Override
		public Spline<C, I> apply(Spline.Visitor<I> visitor) {
			return build(
				visitor.visit(locationFunction),
				locations,
				values().stream().map(value -> value.apply(visitor)).toList(),
				derivatives
			);
		}
	}

	interface Visitor<I> {

		I visit(I value);
	}
}
