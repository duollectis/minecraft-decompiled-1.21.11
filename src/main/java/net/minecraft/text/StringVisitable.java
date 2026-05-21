package net.minecraft.text;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.Unit;

import java.util.List;
import java.util.Optional;

/**
 * {@code StringVisitable}.
 */
public interface StringVisitable {

	Optional<Unit> TERMINATE_VISIT = Optional.of(Unit.INSTANCE);

	StringVisitable EMPTY = new StringVisitable() {
		@Override
		public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
			return Optional.empty();
		}

		@Override
		public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> styledVisitor, Style style) {
			return Optional.empty();
		}
	};

	<T> Optional<T> visit(StringVisitable.Visitor<T> visitor);

	<T> Optional<T> visit(StringVisitable.StyledVisitor<T> styledVisitor, Style style);

	static StringVisitable plain(String string) {
		return new StringVisitable() {
			@Override
			public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
				return visitor.accept(string);
			}

			@Override
			public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> styledVisitor, Style style) {
				return styledVisitor.accept(style, string);
			}
		};
	}

	static StringVisitable styled(String string, Style style) {
		return new StringVisitable() {
			@Override
			public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
				return visitor.accept(string);
			}

			@Override
			public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> styledVisitor, Style outerStyle) {
				return styledVisitor.accept(style.withParent(outerStyle), string);
			}
		};
	}

	static StringVisitable concat(StringVisitable... visitables) {
		return concat(ImmutableList.copyOf(visitables));
	}

	static StringVisitable concat(List<? extends StringVisitable> visitables) {
		return new StringVisitable() {
			@Override
			public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
				for (StringVisitable stringVisitable : visitables) {
					Optional<T> optional = stringVisitable.visit(visitor);
					if (optional.isPresent()) {
						return optional;
					}
				}

				return Optional.empty();
			}

			@Override
			public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> styledVisitor, Style style) {
				for (StringVisitable stringVisitable : visitables) {
					Optional<T> optional = stringVisitable.visit(styledVisitor, style);
					if (optional.isPresent()) {
						return optional;
					}
				}

				return Optional.empty();
			}
		};
	}

	default String getString() {
		StringBuilder stringBuilder = new StringBuilder();
		this.visit(string -> {
			stringBuilder.append(string);
			return Optional.empty();
		});
		return stringBuilder.toString();
	}

	/**
	 * {@code StyledVisitor}.
	 */
	public interface StyledVisitor<T> {

		Optional<T> accept(Style style, String asString);
	}

	/**
	 * {@code Visitor}.
	 */
	public interface Visitor<T> {

		Optional<T> accept(String asString);
	}
}
