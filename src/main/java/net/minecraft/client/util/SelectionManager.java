package net.minecraft.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
/**
 * {@code SelectionManager}.
 */
public class SelectionManager {

	private final Supplier<String> stringGetter;
	private final Consumer<String> stringSetter;
	private final Supplier<String> clipboardGetter;
	private final Consumer<String> clipboardSetter;
	private final Predicate<String> stringFilter;
	private int selectionStart;
	private int selectionEnd;

	public SelectionManager(
			Supplier<String> stringGetter,
			Consumer<String> stringSetter,
			Supplier<String> clipboardGetter,
			Consumer<String> clipboardSetter,
			Predicate<String> stringFilter
	) {
		this.stringGetter = stringGetter;
		this.stringSetter = stringSetter;
		this.clipboardGetter = clipboardGetter;
		this.clipboardSetter = clipboardSetter;
		this.stringFilter = stringFilter;
		this.putCursorAtEnd();
	}

	/**
	 * Make clipboard getter.
	 *
	 * @param client client
	 *
	 * @return Supplier — результат операции
	 */
	public static Supplier<String> makeClipboardGetter(MinecraftClient client) {
		return () -> getClipboard(client);
	}

	public static String getClipboard(MinecraftClient client) {
		return Formatting.strip(client.keyboard.getClipboard().replaceAll("\\r", ""));
	}

	/**
	 * Make clipboard setter.
	 *
	 * @param client client
	 *
	 * @return Consumer — результат операции
	 */
	public static Consumer<String> makeClipboardSetter(MinecraftClient client) {
		return clipboardString -> setClipboard(client, clipboardString);
	}

	public static void setClipboard(MinecraftClient client, String clipboard) {
		client.keyboard.setClipboard(clipboard);
	}

	/**
	 * Insert.
	 *
	 * @param input input
	 *
	 * @return boolean — результат операции
	 */
	public boolean insert(CharInput input) {
		if (input.isValidChar()) {
			this.insert(this.stringGetter.get(), input.asString());
		}

		return true;
	}

	/**
	 * Обрабатывает special key.
	 *
	 * @param input input
	 *
	 * @return boolean — результат операции
	 */
	public boolean handleSpecialKey(KeyInput input) {
		if (input.isSelectAll()) {
			this.selectAll();
			return true;
		}
		else if (input.isCopy()) {
			this.copy();
			return true;
		}
		else if (input.isPaste()) {
			this.paste();
			return true;
		}
		else if (input.isCut()) {
			this.cut();
			return true;
		}
		else {
			SelectionManager.SelectionType
					selectionType =
					input.hasCtrlOrCmd() ? SelectionManager.SelectionType.WORD
					                     : SelectionManager.SelectionType.CHARACTER;
			if (input.key() == 259) {
				this.delete(-1, selectionType);
				return true;
			}
			else {
				if (input.key() == 261) {
					this.delete(1, selectionType);
				}
				else {
					if (input.isLeft()) {
						this.moveCursor(-1, input.hasShift(), selectionType);
						return true;
					}

					if (input.isRight()) {
						this.moveCursor(1, input.hasShift(), selectionType);
						return true;
					}

					if (input.key() == 268) {
						this.moveCursorToStart(input.hasShift());
						return true;
					}

					if (input.key() == 269) {
						this.moveCursorToEnd(input.hasShift());
						return true;
					}
				}

				return false;
			}
		}
	}

	private int clampCursorPosition(int pos) {
		return MathHelper.clamp(pos, 0, this.stringGetter.get().length());
	}

	private void insert(String string, String insertion) {
		if (this.selectionEnd != this.selectionStart) {
			string = this.deleteSelectedText(string);
		}

		this.selectionStart = MathHelper.clamp(this.selectionStart, 0, string.length());
		String string2 = new StringBuilder(string).insert(this.selectionStart, insertion).toString();
		if (this.stringFilter.test(string2)) {
			this.stringSetter.accept(string2);
			this.selectionEnd =
			this.selectionStart = Math.min(string2.length(), this.selectionStart + insertion.length());
		}
	}

	/**
	 * Insert.
	 *
	 * @param string string
	 */
	public void insert(String string) {
		this.insert(this.stringGetter.get(), string);
	}

	private void updateSelectionRange(boolean shiftDown) {
		if (!shiftDown) {
			this.selectionEnd = this.selectionStart;
		}
	}

	/**
	 * Перемещает cursor.
	 *
	 * @param offset offset
	 * @param shiftDown shift down
	 * @param selectionType selection type
	 */
	public void moveCursor(int offset, boolean shiftDown, SelectionManager.SelectionType selectionType) {
		switch (selectionType) {
			case CHARACTER:
				this.moveCursor(offset, shiftDown);
				break;
			case WORD:
				this.moveCursorPastWord(offset, shiftDown);
		}
	}

	/**
	 * Перемещает cursor.
	 *
	 * @param offset offset
	 */
	public void moveCursor(int offset) {
		this.moveCursor(offset, false);
	}

	/**
	 * Перемещает cursor.
	 *
	 * @param offset offset
	 * @param shiftDown shift down
	 */
	public void moveCursor(int offset, boolean shiftDown) {
		this.selectionStart = Util.moveCursor(this.stringGetter.get(), this.selectionStart, offset);
		this.updateSelectionRange(shiftDown);
	}

	/**
	 * Перемещает cursor past word.
	 *
	 * @param offset offset
	 */
	public void moveCursorPastWord(int offset) {
		this.moveCursorPastWord(offset, false);
	}

	/**
	 * Перемещает cursor past word.
	 *
	 * @param offset offset
	 * @param shiftDown shift down
	 */
	public void moveCursorPastWord(int offset, boolean shiftDown) {
		this.selectionStart = TextHandler.moveCursorByWords(this.stringGetter.get(), offset, this.selectionStart, true);
		this.updateSelectionRange(shiftDown);
	}

	/**
	 * Delete.
	 *
	 * @param offset offset
	 * @param selectionType selection type
	 */
	public void delete(int offset, SelectionManager.SelectionType selectionType) {
		switch (selectionType) {
			case CHARACTER:
				this.delete(offset);
				break;
			case WORD:
				this.deleteWord(offset);
		}
	}

	/**
	 * Delete word.
	 *
	 * @param offset offset
	 */
	public void deleteWord(int offset) {
		int i = TextHandler.moveCursorByWords(this.stringGetter.get(), offset, this.selectionStart, true);
		this.delete(i - this.selectionStart);
	}

	/**
	 * Delete.
	 *
	 * @param offset offset
	 */
	public void delete(int offset) {
		String string = this.stringGetter.get();
		if (!string.isEmpty()) {
			String string2;
			if (this.selectionEnd != this.selectionStart) {
				string2 = this.deleteSelectedText(string);
			}
			else {
				int i = Util.moveCursor(string, this.selectionStart, offset);
				int j = Math.min(i, this.selectionStart);
				int k = Math.max(i, this.selectionStart);
				string2 = new StringBuilder(string).delete(j, k).toString();
				if (offset < 0) {
					this.selectionEnd = this.selectionStart = j;
				}
			}

			this.stringSetter.accept(string2);
		}
	}

	/**
	 * Cut.
	 */
	public void cut() {
		String string = this.stringGetter.get();
		this.clipboardSetter.accept(this.getSelectedText(string));
		this.stringSetter.accept(this.deleteSelectedText(string));
	}

	/**
	 * Paste.
	 */
	public void paste() {
		this.insert(this.stringGetter.get(), this.clipboardGetter.get());
		this.selectionEnd = this.selectionStart;
	}

	/**
	 * Copy.
	 */
	public void copy() {
		this.clipboardSetter.accept(this.getSelectedText(this.stringGetter.get()));
	}

	/**
	 * Select all.
	 */
	public void selectAll() {
		this.selectionEnd = 0;
		this.selectionStart = this.stringGetter.get().length();
	}

	private String getSelectedText(String string) {
		int i = Math.min(this.selectionStart, this.selectionEnd);
		int j = Math.max(this.selectionStart, this.selectionEnd);
		return string.substring(i, j);
	}

	private String deleteSelectedText(String string) {
		if (this.selectionEnd == this.selectionStart) {
			return string;
		}
		else {
			int i = Math.min(this.selectionStart, this.selectionEnd);
			int j = Math.max(this.selectionStart, this.selectionEnd);
			String string2 = string.substring(0, i) + string.substring(j);
			this.selectionEnd = this.selectionStart = i;
			return string2;
		}
	}

	/**
	 * Перемещает cursor to start.
	 */
	public void moveCursorToStart() {
		this.moveCursorToStart(false);
	}

	/**
	 * Перемещает cursor to start.
	 *
	 * @param shiftDown shift down
	 */
	public void moveCursorToStart(boolean shiftDown) {
		this.selectionStart = 0;
		this.updateSelectionRange(shiftDown);
	}

	/**
	 * Put cursor at end.
	 */
	public void putCursorAtEnd() {
		this.moveCursorToEnd(false);
	}

	/**
	 * Перемещает cursor to end.
	 *
	 * @param shiftDown shift down
	 */
	public void moveCursorToEnd(boolean shiftDown) {
		this.selectionStart = this.stringGetter.get().length();
		this.updateSelectionRange(shiftDown);
	}

	public int getSelectionStart() {
		return this.selectionStart;
	}

	/**
	 * Перемещает cursor to.
	 *
	 * @param position position
	 */
	public void moveCursorTo(int position) {
		this.moveCursorTo(position, true);
	}

	/**
	 * Перемещает cursor to.
	 *
	 * @param position position
	 * @param shiftDown shift down
	 */
	public void moveCursorTo(int position, boolean shiftDown) {
		this.selectionStart = this.clampCursorPosition(position);
		this.updateSelectionRange(shiftDown);
	}

	public int getSelectionEnd() {
		return this.selectionEnd;
	}

	public void setSelectionEnd(int pos) {
		this.selectionEnd = this.clampCursorPosition(pos);
	}

	public void setSelection(int start, int end) {
		int i = this.stringGetter.get().length();
		this.selectionStart = MathHelper.clamp(start, 0, i);
		this.selectionEnd = MathHelper.clamp(end, 0, i);
	}

	public boolean isSelecting() {
		return this.selectionStart != this.selectionEnd;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code SelectionType}.
	 */
	public static enum SelectionType {
		CHARACTER,
		WORD;
	}
}
