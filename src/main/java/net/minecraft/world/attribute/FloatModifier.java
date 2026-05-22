package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.Interpolator;
import net.minecraft.util.math.MathHelper;

/**
 * Модификатор числовых (float) атрибутов окружения.
 * Определяет набор стандартных арифметических операций над значениями атрибутов.
 *
 * @param <Argument> тип аргумента операции
 */
public interface FloatModifier<Argument> extends EnvironmentAttributeModifier<Float, Argument> {

	/**
	 * Alpha-blend: плавно смешивает текущее значение с целевым по коэффициенту alpha.
	 * Формула: {@code lerp(alpha, current, target)}.
	 */
	FloatModifier<BlendArgument> ALPHA_BLEND = new FloatModifier<>() {
		@Override
		public Float apply(Float current, BlendArgument argument) {
			return MathHelper.lerp(argument.alpha(), current, argument.value());
		}

		@Override
		public Codec<BlendArgument> argumentCodec(EnvironmentAttribute<Float> attribute) {
			return BlendArgument.CODEC;
		}

		@Override
		public Interpolator<BlendArgument> argumentKeyframeLerp(EnvironmentAttribute<Float> attribute) {
			return (t, a, b) -> new BlendArgument(
				MathHelper.lerp(t, a.value(), b.value()),
				MathHelper.lerp(t, a.alpha(), b.alpha())
			);
		}
	};

	/** Сложение: {@code current + argument}. */
	FloatModifier<Float> ADD = (Binary) Float::sum;

	/** Вычитание: {@code current - argument}. */
	FloatModifier<Float> SUBTRACT = (Binary) (current, argument) -> current - argument;

	/** Умножение: {@code current * argument}. */
	FloatModifier<Float> MULTIPLY = (Binary) (current, argument) -> current * argument;

	/** Минимум: {@code min(current, argument)}. */
	FloatModifier<Float> MINIMUM = (Binary) Math::min;

	/** Максимум: {@code max(current, argument)}. */
	FloatModifier<Float> MAXIMUM = (Binary) Math::max;

	/**
	 * Бинарный float-модификатор: принимает два float-значения.
	 * Codec аргумента — {@link Codec#FLOAT}, интерполятор — линейный.
	 */
	@FunctionalInterface
	interface Binary extends FloatModifier<Float> {

		@Override
		default Codec<Float> argumentCodec(EnvironmentAttribute<Float> attribute) {
			return Codec.FLOAT;
		}

		@Override
		default Interpolator<Float> argumentKeyframeLerp(EnvironmentAttribute<Float> attribute) {
			return Interpolator.ofFloat();
		}
	}
}
