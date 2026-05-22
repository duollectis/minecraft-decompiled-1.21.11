package net.minecraft.world.attribute;

import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Функция модификации значения атрибута окружения.
 * Sealed-иерархия из трёх вариантов: константная, временная и позиционная.
 * <p>
 * Функции применяются последовательно в {@link WorldEnvironmentAttributeAccess}
 * для вычисления итогового значения атрибута.
 *
 * @param <Value> тип значения атрибута
 */
public sealed interface EnvironmentAttributeFunction<Value>
	permits EnvironmentAttributeFunction.Constant,
		EnvironmentAttributeFunction.TimeBased,
		EnvironmentAttributeFunction.Positional {

	/**
	 * Константная функция — не зависит ни от времени, ни от позиции.
	 * Вычисляется один раз и кешируется.
	 *
	 * @param <Value> тип значения атрибута
	 */
	@FunctionalInterface
	non-sealed interface Constant<Value> extends EnvironmentAttributeFunction<Value> {

		/**
		 * Применяет константную модификацию к значению атрибута.
		 *
		 * @param value текущее значение
		 * @return модифицированное значение
		 */
		Value applyConstant(Value value);
	}

	/**
	 * Временная функция — зависит от игрового времени (тиков).
	 * Пересчитывается каждый тик.
	 *
	 * @param <Value> тип значения атрибута
	 */
	@FunctionalInterface
	non-sealed interface TimeBased<Value> extends EnvironmentAttributeFunction<Value> {

		/**
		 * Применяет временну́ю модификацию к значению атрибута.
		 *
		 * @param value текущее значение
		 * @param time текущий возраст (тики с момента создания)
		 * @return модифицированное значение
		 */
		Value applyTimeBased(Value value, int time);
	}

	/**
	 * Позиционная функция — зависит от координат в мире.
	 * Пересчитывается при каждом запросе с позицией.
	 *
	 * @param <Value> тип значения атрибута
	 */
	@FunctionalInterface
	non-sealed interface Positional<Value> extends EnvironmentAttributeFunction<Value> {

		/**
		 * Применяет позиционную модификацию к значению атрибута.
		 *
		 * @param value текущее значение
		 * @param pos позиция в мире
		 * @param weightedAttributeList пул взвешенных атрибутов биомов для интерполяции, или {@code null}
		 * @return модифицированное значение
		 */
		Value applyPositional(Value value, Vec3d pos, @Nullable WeightedAttributeList weightedAttributeList);
	}
}
