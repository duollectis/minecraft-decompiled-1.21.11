package net.minecraft.util.packrat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.chars.CharList;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Фабрика терминальных символов (литералов) для PEG-парсера.
 * Предоставляет готовые {@link Term} для сопоставления строк и символов.
 */
public interface Literals {

	static Term<StringReader> string(String string) {
		return new StringLiteral(string);
	}

	/**
	 * Создаёт терминал, принимающий ровно один конкретный символ.
	 */
	static Term<StringReader> character(char c) {
		return new CharacterLiteral(CharList.of(c)) {
			@Override
			protected boolean accepts(char ch) {
				return ch == c;
			}
		};
	}

	/**
	 * Создаёт терминал, принимающий один из двух символов.
	 */
	static Term<StringReader> character(char c1, char c2) {
		return new CharacterLiteral(CharList.of(c1, c2)) {
			@Override
			protected boolean accepts(char ch) {
				return ch == c1 || ch == c2;
			}
		};
	}

	static StringReader createReader(String string, int cursor) {
		StringReader reader = new StringReader(string);
		reader.setCursor(cursor);
		return reader;
	}

	/**
	 * Терминал, сопоставляющий один символ из допустимого набора.
	 */
	abstract class CharacterLiteral implements Term<StringReader> {

		private final CursorExceptionType<CommandSyntaxException> exception;
		private final Suggestable<StringReader> suggestions;

		public CharacterLiteral(CharList values) {
			String joined = values.intStream()
					.mapToObj(Character::toString)
					.collect(Collectors.joining("|"));

			exception = CursorExceptionType.create(
					CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(),
					joined
			);
			suggestions = state -> values.intStream().mapToObj(Character::toString);
		}

		@Override
		public boolean matches(ParsingState<StringReader> state, ParseResults results, Cut cut) {
			state.getReader().skipWhitespace();

			int cursor = state.getCursor();

			if (state.getReader().canRead() && accepts(state.getReader().read())) {
				return true;
			}

			state.getErrors().add(cursor, suggestions, exception);
			return false;
		}

		protected abstract boolean accepts(char c);
	}

	/**
	 * Терминал, сопоставляющий конкретную строку.
	 */
	final class StringLiteral implements Term<StringReader> {

		private final String value;
		private final CursorExceptionType<CommandSyntaxException> exception;
		private final Suggestable<StringReader> suggestions;

		public StringLiteral(String value) {
			this.value = value;
			exception = CursorExceptionType.create(
					CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(),
					value
			);
			suggestions = state -> Stream.of(value);
		}

		@Override
		public boolean matches(ParsingState<StringReader> state, ParseResults results, Cut cut) {
			state.getReader().skipWhitespace();

			int cursor = state.getCursor();
			String token = state.getReader().readUnquotedString();

			if (token.equals(value)) {
				return true;
			}

			state.getErrors().add(cursor, suggestions, exception);
			return false;
		}

		@Override
		public String toString() {
			return "terminal[" + value + "]";
		}
	}
}
