package net.minecraft.world.attribute;

import com.mojang.serialization.DataResult;
import net.minecraft.util.math.MathHelper;

/**
 * Валидатор значений атрибута окружения. Проверяет допустимость значения
 * и при необходимости зажимает его в допустимый диапазон.
 *
 * @param <Value> тип проверяемого значения
 */
public interface AttributeValidator<Value> {

	/** Валидатор вероятности: допустимый диапазон [0.0; 1.0]. */
	AttributeValidator<Float> PROBABILITY = ranged(0.0F, 1.0F);

	/** Валидатор неотрицательного числа: допустимый диапазон [0.0; +∞). */
	AttributeValidator<Float> NON_NEGATIVE_FLOAT = ranged(0.0F, Float.POSITIVE_INFINITY);

	/**
	 * Создаёт валидатор, принимающий любые значения без ограничений.
	 *
	 * @param <Value> тип значения
	 * @return валидатор-пропускатель
	 */
	static <Value> AttributeValidator<Value> all() {
		return new AttributeValidator<>() {
			@Override
			public DataResult<Value> validate(Value value) {
				return DataResult.success(value);
			}

			@Override
			public Value clamp(Value value) {
				return value;
			}
		};
	}

	/**
	 * Создаёт валидатор с диапазоном допустимых значений [min; max].
	 * При валидации возвращает ошибку, если значение вне диапазона.
	 * При зажатии использует {@link MathHelper#clamp}.
	 *
	 * @param min нижняя граница диапазона (включительно)
	 * @param max верхняя граница диапазона (включительно)
	 * @return валидатор с диапазоном
	 */
	static AttributeValidator<Float> ranged(float min, float max) {
		return new AttributeValidator<>() {
			@Override
			public DataResult<Float> validate(Float value) {
				return value >= min && value <= max
					? DataResult.success(value)
					: DataResult.error(() -> value + " is not in range [" + min + "; " + max + "]");
			}

			@Override
			public Float clamp(Float value) {
				return value >= min && value <= max ? value : MathHelper.clamp(value, min, max);
			}
		};
	}

	/**
	 * Проверяет значение на допустимость.
	 *
	 * @param value проверяемое значение
	 * @return {@link DataResult#success} если значение допустимо, иначе {@link DataResult#error}
	 */
	DataResult<Value> validate(Value value);

	/**
	 * Зажимает значение в допустимый диапазон.
	 *
	 * @param value исходное значение
	 * @return значение, гарантированно находящееся в допустимом диапазоне
	 */
	Value clamp(Value value);
}
