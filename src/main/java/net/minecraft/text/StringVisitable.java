package net.minecraft.text;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс для объектов, содержимое которых можно обойти как последовательность строк.
 * Поддерживает два режима обхода: без стиля ({@link Visitor}) и со стилем ({@link StyledVisitor}).
 * Обход прерывается досрочно, если посетитель возвращает непустой {@link Optional}.
 */
public interface StringVisitable {

	/** Сигнальное значение для досрочного прекращения обхода. */
	Optional<Unit> TERMINATE_VISIT = Optional.of(Unit.INSTANCE);

	/** Пустой посетитель, не содержащий никакого текста. */
	StringVisitable EMPTY = new StringVisitable() {
		@Override
		public <T> Optional<T> visit(Visitor<T> visitor) {
			return Optional.empty();
		}

		@Override
		public <T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style style) {
			return Optional.empty();
		}
	};

	/**
	 * Обходит строковое содержимое без учёта стиля.
	 *
	 * @param visitor посетитель, получающий строковые фрагменты
	 * @return непустой {@link Optional}, если обход был прерван посетителем
	 */
	<T> Optional<T> visit(Visitor<T> visitor);

	/**
	 * Обходит строковое содержимое с учётом стиля.
	 *
	 * @param styledVisitor посетитель, получающий стиль и строковые фрагменты
	 * @param style         базовый стиль для обхода
	 * @return непустой {@link Optional}, если обход был прерван посетителем
	 */
	<T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style style);

	/**
	 * Создаёт {@link StringVisitable} из простой строки без стиля.
	 */
	static StringVisitable plain(String string) {
		return new StringVisitable() {
			@Override
			public <T> Optional<T> visit(Visitor<T> visitor) {
				return visitor.accept(string);
			}

			@Override
			public <T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style style) {
				return styledVisitor.accept(style, string);
			}
		};
	}

	/**
	 * Создаёт {@link StringVisitable} из строки с заданным стилем.
	 * При обходе со стилем переданный {@code style} объединяется с внешним через {@link Style#withParent}.
	 */
	static StringVisitable styled(String string, Style style) {
		return new StringVisitable() {
			@Override
			public <T> Optional<T> visit(Visitor<T> visitor) {
				return visitor.accept(string);
			}

			@Override
			public <T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style outerStyle) {
				return styledVisitor.accept(style.withParent(outerStyle), string);
			}
		};
	}

	/**
	 * Конкатенирует несколько {@link StringVisitable} в один последовательный обход.
	 */
	static StringVisitable concat(StringVisitable... visitables) {
		return concat(ImmutableList.copyOf(visitables));
	}

	/**
	 * Конкатенирует список {@link StringVisitable} в один последовательный обход.
	 * Обход прекращается, как только один из элементов вернёт непустой результат.
	 */
	static StringVisitable concat(List<? extends StringVisitable> visitables) {
		return new StringVisitable() {
			@Override
			public <T> Optional<T> visit(Visitor<T> visitor) {
				for (StringVisitable visitable : visitables) {
					Optional<T> result = visitable.visit(visitor);

					if (result.isPresent()) {
						return result;
					}
				}

				return Optional.empty();
			}

			@Override
			public <T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style style) {
				for (StringVisitable visitable : visitables) {
					Optional<T> result = visitable.visit(styledVisitor, style);

					if (result.isPresent()) {
						return result;
					}
				}

				return Optional.empty();
			}
		};
	}

	default String getString() {
		StringBuilder builder = new StringBuilder();

		visit(string -> {
			builder.append(string);
			return Optional.empty();
		});

		return builder.toString();
	}

	/**
	 * Посетитель, получающий строковые фрагменты с учётом стиля.
	 */
	interface StyledVisitor<T> {

		Optional<T> accept(Style style, String asString);
	}

	/**
	 * Посетитель, получающий строковые фрагменты без учёта стиля.
	 */
	interface Visitor<T> {

		Optional<T> accept(String asString);
	}
}
