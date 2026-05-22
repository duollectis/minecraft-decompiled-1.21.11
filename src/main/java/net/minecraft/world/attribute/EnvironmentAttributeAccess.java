package net.minecraft.world.attribute;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * Интерфейс доступа к значениям атрибутов окружения.
 * Предоставляет методы для получения значений как глобально,
 * так и с учётом позиции в мире.
 */
public interface EnvironmentAttributeAccess {

	/**
	 * Реализация по умолчанию, всегда возвращающая дефолтное значение атрибута.
	 * Используется как заглушка там, где нет реального источника атрибутов.
	 */
	EnvironmentAttributeAccess DEFAULT = new EnvironmentAttributeAccess() {
		@Override
		public <Value> Value getAttributeValue(EnvironmentAttribute<Value> attribute) {
			return attribute.getDefaultValue();
		}

		@Override
		public <Value> Value getAttributeValue(
			EnvironmentAttribute<Value> attribute,
			Vec3d pos,
			@Nullable WeightedAttributeList pool
		) {
			return attribute.getDefaultValue();
		}
	};

	/**
	 * Возвращает глобальное значение атрибута без учёта позиции.
	 * Для позиционных атрибутов в режиме разработки бросает исключение.
	 *
	 * @param attribute запрашиваемый атрибут
	 * @return текущее значение атрибута
	 */
	<Value> Value getAttributeValue(EnvironmentAttribute<Value> attribute);

	/**
	 * Возвращает значение атрибута для заданной позиции блока.
	 *
	 * @param attribute запрашиваемый атрибут
	 * @param pos позиция блока в мире
	 * @return значение атрибута в данной позиции
	 */
	default <Value> Value getAttributeValue(EnvironmentAttribute<Value> attribute, BlockPos pos) {
		return getAttributeValue(attribute, Vec3d.ofCenter(pos));
	}

	/**
	 * Возвращает значение атрибута для заданной позиции в мире.
	 *
	 * @param attribute запрашиваемый атрибут
	 * @param pos позиция в мире
	 * @return значение атрибута в данной позиции
	 */
	default <Value> Value getAttributeValue(EnvironmentAttribute<Value> attribute, Vec3d pos) {
		return getAttributeValue(attribute, pos, null);
	}

	/**
	 * Возвращает значение атрибута для заданной позиции с опциональным пулом
	 * взвешенных атрибутов для интерполяции между биомами.
	 *
	 * @param attribute запрашиваемый атрибут
	 * @param pos позиция в мире
	 * @param pool пул взвешенных атрибутов биомов для интерполяции, или {@code null}
	 * @return значение атрибута
	 */
	<Value> Value getAttributeValue(
		EnvironmentAttribute<Value> attribute,
		Vec3d pos,
		@Nullable WeightedAttributeList pool
	);
}
