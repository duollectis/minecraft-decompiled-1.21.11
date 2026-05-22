package net.minecraft.util.function;

/**
 * Функциональный интерфейс для отображения объекта типа {@code T} в примитивный {@code float}
 * без упаковки результата.
 */
@FunctionalInterface
public interface Object2FloatFunction<T> {

	float applyAsFloat(T object);
}
