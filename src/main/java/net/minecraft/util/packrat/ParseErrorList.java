package net.minecraft.util.packrat;

import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ParseErrorList}.
 */
public interface ParseErrorList<S> {

	void add(int cursor, Suggestable<S> suggestions, Object reason);

	default void add(int cursor, Object reason) {
		this.add(cursor, Suggestable.empty(), reason);
	}

	void setCursor(int cursor);

	/**
	 * {@code Impl}.
	 */
	public static class Impl<S> implements ParseErrorList<S> {

		private ParseErrorList.Impl.@Nullable Entry<S>[] errors = new ParseErrorList.Impl.Entry[16];
		private int topIndex;
		private int cursor = -1;

		private void moveCursor(int cursor) {
			if (cursor > this.cursor) {
				this.cursor = cursor;
				this.topIndex = 0;
			}
		}

		@Override
		public void setCursor(int cursor) {
			this.moveCursor(cursor);
		}

		@Override
		public void add(int cursor, Suggestable<S> suggestions, Object reason) {
			this.moveCursor(cursor);
			if (cursor == this.cursor) {
				this.add(suggestions, reason);
			}
		}

		private void add(Suggestable<S> suggestions, Object reason) {
			int i = this.errors.length;
			if (this.topIndex >= i) {
				int j = Util.nextCapacity(i, this.topIndex + 1);
				ParseErrorList.Impl.Entry<S>[] entrys = new ParseErrorList.Impl.Entry[j];
				System.arraycopy(this.errors, 0, entrys, 0, i);
				this.errors = entrys;
			}

			int j = this.topIndex++;
			ParseErrorList.Impl.Entry<S> entry = this.errors[j];
			if (entry == null) {
				entry = new ParseErrorList.Impl.Entry<>();
				this.errors[j] = entry;
			}

			entry.suggestions = suggestions;
			entry.reason = reason;
		}

		public List<ParseError<S>> getErrors() {
			int i = this.topIndex;
			if (i == 0) {
				return List.of();
			}
			else {
				List<ParseError<S>> list = new ArrayList<>(i);

				for (int j = 0; j < i; j++) {
					ParseErrorList.Impl.Entry<S> entry = this.errors[j];
					list.add(new ParseError<>(this.cursor, entry.suggestions, entry.reason));
				}

				return list;
			}
		}

		public int getCursor() {
			return this.cursor;
		}

		/**
		 * {@code Entry}.
		 */
		static class Entry<S> {

			Suggestable<S> suggestions = Suggestable.empty();
			Object reason = "empty";
		}
	}

	/**
	 * {@code Noop}.
	 */
	public static class Noop<S> implements ParseErrorList<S> {

		@Override
		public void add(int cursor, Suggestable<S> suggestions, Object reason) {
		}

		@Override
		public void setCursor(int cursor) {
		}
	}
}
