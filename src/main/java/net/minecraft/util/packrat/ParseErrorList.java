package net.minecraft.util.packrat;

import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Коллектор ошибок разбора, отслеживающий только ошибки на максимальной позиции курсора.
 * Это соответствует принципу «наиболее продвинутой ошибки» (furthest failure heuristic),
 * который даёт наиболее полезные сообщения об ошибках в PEG-парсерах.
 */
public interface ParseErrorList<S> {

	void add(int cursor, Suggestable<S> suggestions, Object reason);

	default void add(int cursor, Object reason) {
		add(cursor, Suggestable.empty(), reason);
	}

	void setCursor(int cursor);

	/**
	 * Стандартная реализация с динамически расширяемым массивом записей.
	 * Хранит только ошибки на максимальной позиции курсора.
	 */
	class Impl<S> implements ParseErrorList<S> {

		private static final int INITIAL_CAPACITY = 16;

		private @Nullable Entry<S>[] errors = new Entry[INITIAL_CAPACITY];
		private int topIndex;
		private int cursor = -1;

		private void moveCursor(int newCursor) {
			if (newCursor > cursor) {
				cursor = newCursor;
				topIndex = 0;
			}
		}

		@Override
		public void setCursor(int cursor) {
			moveCursor(cursor);
		}

		@Override
		public void add(int cursor, Suggestable<S> suggestions, Object reason) {
			moveCursor(cursor);

			if (cursor == this.cursor) {
				addEntry(suggestions, reason);
			}
		}

		private void addEntry(Suggestable<S> suggestions, Object reason) {
			int capacity = errors.length;

			if (topIndex >= capacity) {
				int newCapacity = Util.nextCapacity(capacity, topIndex + 1);
				Entry<S>[] expanded = new Entry[newCapacity];
				System.arraycopy(errors, 0, expanded, 0, capacity);
				errors = expanded;
			}

			int index = topIndex++;
			Entry<S> entry = errors[index];

			if (entry == null) {
				entry = new Entry<>();
				errors[index] = entry;
			}

			entry.suggestions = suggestions;
			entry.reason = reason;
		}

		public List<ParseError<S>> getErrors() {
			if (topIndex == 0) {
				return List.of();
			}

			List<ParseError<S>> result = new ArrayList<>(topIndex);

			for (int i = 0; i < topIndex; i++) {
				Entry<S> entry = errors[i];
				result.add(new ParseError<>(cursor, entry.suggestions, entry.reason));
			}

			return result;
		}

		public int getCursor() {
			return cursor;
		}

		static class Entry<S> {

			Suggestable<S> suggestions = Suggestable.empty();
			Object reason = "empty";
		}
	}

	/**
	 * Реализация-заглушка, которая игнорирует все ошибки.
	 * Используется в режиме подавления ошибок при lookahead-проверках.
	 */
	class Noop<S> implements ParseErrorList<S> {

		@Override
		public void add(int cursor, Suggestable<S> suggestions, Object reason) {
		}

		@Override
		public void setCursor(int cursor) {
		}
	}
}
