package net.minecraft.util;

import org.jspecify.annotations.Nullable;

/**
 * Фильтр типов: позволяет безопасно приводить объект базового типа {@code B}
 * к подтипу {@code T} без непроверяемых приведений в вызывающем коде.
 *
 * @param <B> базовый тип
 * @param <T> целевой подтип
 */
public interface TypeFilter<B, T extends B> {

	/**
	 * Создаёт фильтр на основе {@link Class#isInstance}: принимает объекты,
	 * являющиеся экземплярами {@code cls} или его подклассов.
	 */
	static <B, T extends B> TypeFilter<B, T> instanceOf(Class<T> cls) {
		return new TypeFilter<>() {
			@Override
			@SuppressWarnings("unchecked")
			public @Nullable T downcast(B obj) {
				return cls.isInstance(obj) ? (T) obj : null;
			}

			@Override
			public Class<? extends B> getBaseClass() {
				return cls;
			}
		};
	}

	/**
	 * Создаёт фильтр на основе точного совпадения класса: принимает только объекты,
	 * класс которых в точности равен {@code cls} (без учёта подклассов).
	 */
	static <B, T extends B> TypeFilter<B, T> equals(Class<T> cls) {
		return new TypeFilter<>() {
			@Override
			@SuppressWarnings("unchecked")
			public @Nullable T downcast(B obj) {
				return cls.equals(obj.getClass()) ? (T) obj : null;
			}

			@Override
			public Class<? extends B> getBaseClass() {
				return cls;
			}
		};
	}

	/**
	 * Пытается привести {@code obj} к типу {@code T}.
	 *
	 * @param obj объект базового типа
	 * @return объект типа {@code T} или {@code null}, если приведение невозможно
	 */
	@Nullable T downcast(B obj);

	Class<? extends B> getBaseClass();
}
