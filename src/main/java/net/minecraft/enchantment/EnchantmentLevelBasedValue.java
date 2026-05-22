package net.minecraft.enchantment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Интерфейс для вычисления числового значения, зависящего от уровня зачарования.
 * Реализации охватывают константы, линейные функции, дроби, степени и таблицы поиска.
 */
public interface EnchantmentLevelBasedValue {

	/**
	 * Базовый кодек, использующий диспетчеризацию по типу из реестра.
	 * Не обрабатывает сокращённую форму константы — для этого используйте {@link #CODEC}.
	 */
	Codec<EnchantmentLevelBasedValue> BASE_CODEC = Registries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE
			.getCodec()
			.dispatch(EnchantmentLevelBasedValue::getCodec, codec -> codec);

	/**
	 * Полный кодек: числовой литерал десериализуется как {@link Constant},
	 * все остальные типы — через {@link #BASE_CODEC}.
	 */
	Codec<EnchantmentLevelBasedValue> CODEC = Codec.either(Constant.CODEC, BASE_CODEC)
			.xmap(
					either -> either.map(type -> type, type -> type),
					type -> type instanceof Constant constant ? Either.left(constant) : Either.right(type)
			);

	/**
	 * Регистрирует все встроенные типы в реестре и возвращает тип по умолчанию.
	 */
	static MapCodec<? extends EnchantmentLevelBasedValue> registerAndGetDefault(
			Registry<MapCodec<? extends EnchantmentLevelBasedValue>> registry
	) {
		Registry.register(registry, "clamped", Clamped.CODEC);
		Registry.register(registry, "fraction", Fraction.CODEC);
		Registry.register(registry, "levels_squared", LevelsSquared.CODEC);
		Registry.register(registry, "linear", Linear.CODEC);
		Registry.register(registry, "exponent", Exponent.CODEC);
		return Registry.register(registry, "lookup", Lookup.CODEC);
	}

	static Constant constant(float value) {
		return new Constant(value);
	}

	static Linear linear(float base, float perLevelAboveFirst) {
		return new Linear(base, perLevelAboveFirst);
	}

	static Linear linear(float base) {
		return linear(base, base);
	}

	static Lookup lookup(List<Float> values, EnchantmentLevelBasedValue fallback) {
		return new Lookup(values, fallback);
	}

	/** Вычисляет значение для указанного уровня зачарования. */
	float getValue(int level);

	MapCodec<? extends EnchantmentLevelBasedValue> getCodec();

	/**
	 * Ограничивает результат другого вычислителя в диапазоне [min, max].
	 */
	record Clamped(EnchantmentLevelBasedValue value, float min, float max) implements EnchantmentLevelBasedValue {

		public static final MapCodec<Clamped> CODEC = RecordCodecBuilder
				.<Clamped>mapCodec(instance -> instance.group(
						EnchantmentLevelBasedValue.CODEC.fieldOf("value").forGetter(Clamped::value),
						Codec.FLOAT.fieldOf("min").forGetter(Clamped::min),
						Codec.FLOAT.fieldOf("max").forGetter(Clamped::max)
				).apply(instance, Clamped::new))
				.validate(type -> type.max <= type.min
						? DataResult.error(() -> "Max must be larger than min, min: " + type.min + ", max: " + type.max)
						: DataResult.success(type)
				);

		@Override
		public float getValue(int level) {
			return MathHelper.clamp(value.getValue(level), min, max);
		}

		@Override
		public MapCodec<Clamped> getCodec() {
			return CODEC;
		}
	}

	/**
	 * Возвращает фиксированное значение независимо от уровня.
	 * В JSON может быть записана как числовой литерал.
	 */
	record Constant(float value) implements EnchantmentLevelBasedValue {

		public static final Codec<Constant> CODEC =
				Codec.FLOAT.xmap(Constant::new, Constant::value);

		public static final MapCodec<Constant> TYPE_CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(Codec.FLOAT.fieldOf("value").forGetter(Constant::value))
						.apply(instance, Constant::new)
		);

		@Override
		public float getValue(int level) {
			return value;
		}

		@Override
		public MapCodec<Constant> getCodec() {
			return TYPE_CODEC;
		}
	}

	/**
	 * Вычисляет {@code base ^ power}, где оба операнда зависят от уровня.
	 */
	record Exponent(EnchantmentLevelBasedValue base, EnchantmentLevelBasedValue power)
			implements EnchantmentLevelBasedValue {

		public static final MapCodec<Exponent> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						EnchantmentLevelBasedValue.CODEC.fieldOf("base").forGetter(Exponent::base),
						EnchantmentLevelBasedValue.CODEC.fieldOf("power").forGetter(Exponent::power)
				).apply(instance, Exponent::new)
		);

		@Override
		public float getValue(int level) {
			return (float) Math.pow(base.getValue(level), power.getValue(level));
		}

		@Override
		public MapCodec<Exponent> getCodec() {
			return CODEC;
		}
	}

	/**
	 * Вычисляет дробь {@code numerator / denominator}.
	 * Если знаменатель равен нулю, возвращает 0.
	 */
	record Fraction(EnchantmentLevelBasedValue numerator, EnchantmentLevelBasedValue denominator)
			implements EnchantmentLevelBasedValue {

		public static final MapCodec<Fraction> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						EnchantmentLevelBasedValue.CODEC.fieldOf("numerator").forGetter(Fraction::numerator),
						EnchantmentLevelBasedValue.CODEC.fieldOf("denominator").forGetter(Fraction::denominator)
				).apply(instance, Fraction::new)
		);

		@Override
		public float getValue(int level) {
			float denom = denominator.getValue(level);
			return denom == 0.0F ? 0.0F : numerator.getValue(level) / denom;
		}

		@Override
		public MapCodec<Fraction> getCodec() {
			return CODEC;
		}
	}

	/**
	 * Вычисляет {@code level² + added}.
	 */
	record LevelsSquared(float added) implements EnchantmentLevelBasedValue {

		public static final MapCodec<LevelsSquared> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance
						.group(Codec.FLOAT.fieldOf("added").forGetter(LevelsSquared::added))
						.apply(instance, LevelsSquared::new)
		);

		@Override
		public float getValue(int level) {
			return MathHelper.square(level) + added;
		}

		@Override
		public MapCodec<LevelsSquared> getCodec() {
			return CODEC;
		}
	}

	/**
	 * Линейная функция: {@code base + perLevelAboveFirst * (level - 1)}.
	 */
	record Linear(float base, float perLevelAboveFirst) implements EnchantmentLevelBasedValue {

		public static final MapCodec<Linear> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Codec.FLOAT.fieldOf("base").forGetter(Linear::base),
						Codec.FLOAT.fieldOf("per_level_above_first").forGetter(Linear::perLevelAboveFirst)
				).apply(instance, Linear::new)
		);

		@Override
		public float getValue(int level) {
			return base + perLevelAboveFirst * (level - 1);
		}

		@Override
		public MapCodec<Linear> getCodec() {
			return CODEC;
		}
	}

	/**
	 * Таблица поиска: возвращает {@code values[level - 1]} для уровней в пределах списка,
	 * иначе делегирует в {@code fallback}.
	 */
	record Lookup(List<Float> values, EnchantmentLevelBasedValue fallback) implements EnchantmentLevelBasedValue {

		public static final MapCodec<Lookup> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Codec.FLOAT.listOf().fieldOf("values").forGetter(Lookup::values),
						EnchantmentLevelBasedValue.CODEC.fieldOf("fallback").forGetter(Lookup::fallback)
				).apply(instance, Lookup::new)
		);

		@Override
		public float getValue(int level) {
			return level <= values.size() ? values.get(level - 1) : fallback.getValue(level);
		}

		@Override
		public MapCodec<Lookup> getCodec() {
			return CODEC;
		}
	}
}
