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

/**
 * Менеджер выделения и редактирования текста в полях ввода.
 * Обрабатывает навигацию курсором, выделение, копирование, вставку и удаление.
 */
@Environment(EnvType.CLIENT)
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
		putCursorAtEnd();
	}

	public static Supplier<String> makeClipboardGetter(MinecraftClient client) {
		return () -> getClipboard(client);
	}

	public static String getClipboard(MinecraftClient client) {
		return Formatting.strip(client.keyboard.getClipboard().replaceAll("\\r", ""));
	}

	public static Consumer<String> makeClipboardSetter(MinecraftClient client) {
		return clipboardText -> setClipboard(client, clipboardText);
	}

	public static void setClipboard(MinecraftClient client, String clipboard) {
		client.keyboard.setClipboard(clipboard);
	}

	public boolean insert(CharInput input) {
		if (input.isValidChar()) {
			insert(stringGetter.get(), input.asString());
		}

		return true;
	}

	/**
	 * Обрабатывает специальные клавиши редактирования: выделение, копирование, вставку, удаление, навигацию.
	 *
	 * @param input событие нажатия клавиши
	 * @return {@code true}, если клавиша была обработана
	 */
	public boolean handleSpecialKey(KeyInput input) {
		if (input.isSelectAll()) {
			selectAll();
			return true;
		}

		if (input.isCopy()) {
			copy();
			return true;
		}

		if (input.isPaste()) {
			paste();
			return true;
		}

		if (input.isCut()) {
			cut();
			return true;
		}

		SelectionType selectionType = input.hasCtrlOrCmd() ? SelectionType.WORD : SelectionType.CHARACTER;

		if (input.key() == InputUtil.GLFW_KEY_BACKSPACE) {
			delete(-1, selectionType);
			return true;
		}

		if (input.key() == InputUtil.GLFW_KEY_DELETE) {
			delete(1, selectionType);
			return true;
		}

		if (input.isLeft()) {
			moveCursor(-1, input.hasShift(), selectionType);
			return true;
		}

		if (input.isRight()) {
			moveCursor(1, input.hasShift(), selectionType);
			return true;
		}

		if (input.key() == InputUtil.GLFW_KEY_HOME) {
			moveCursorToStart(input.hasShift());
			return true;
		}

		if (input.key() == InputUtil.GLFW_KEY_END) {
			moveCursorToEnd(input.hasShift());
			return true;
		}

		return false;
	}

	private int clampCursorPosition(int pos) {
		return MathHelper.clamp(pos, 0, stringGetter.get().length());
	}

	private void insert(String currentText, String insertion) {
		String text = selectionEnd != selectionStart ? deleteSelectedText(currentText) : currentText;
		selectionStart = MathHelper.clamp(selectionStart, 0, text.length());
		String newText = new StringBuilder(text).insert(selectionStart, insertion).toString();
		if (!stringFilter.test(newText)) {
			return;
		}

		stringSetter.accept(newText);
		selectionEnd = selectionStart = Math.min(newText.length(), selectionStart + insertion.length());
	}

	public void insert(String text) {
		insert(stringGetter.get(), text);
	}

	private void updateSelectionRange(boolean shiftDown) {
		if (!shiftDown) {
			selectionEnd = selectionStart;
		}
	}

	public void moveCursor(int offset, boolean shiftDown, SelectionType selectionType) {
		switch (selectionType) {
			case CHARACTER -> moveCursor(offset, shiftDown);
			case WORD -> moveCursorPastWord(offset, shiftDown);
		}
	}

	public void moveCursor(int offset) {
		moveCursor(offset, false);
	}

	public void moveCursor(int offset, boolean shiftDown) {
		selectionStart = Util.moveCursor(stringGetter.get(), selectionStart, offset);
		updateSelectionRange(shiftDown);
	}

	public void moveCursorPastWord(int offset) {
		moveCursorPastWord(offset, false);
	}

	public void moveCursorPastWord(int offset, boolean shiftDown) {
		selectionStart = TextHandler.moveCursorByWords(stringGetter.get(), offset, selectionStart, true);
		updateSelectionRange(shiftDown);
	}

	public void delete(int offset, SelectionType selectionType) {
		switch (selectionType) {
			case CHARACTER -> delete(offset);
			case WORD -> deleteWord(offset);
		}
	}

	public void deleteWord(int offset) {
		int targetPos = TextHandler.moveCursorByWords(stringGetter.get(), offset, selectionStart, true);
		delete(targetPos - selectionStart);
	}

	public void delete(int offset) {
		String text = stringGetter.get();
		if (text.isEmpty()) {
			return;
		}

		String newText;
		if (selectionEnd != selectionStart) {
			newText = deleteSelectedText(text);
		} else {
			int targetPos = Util.moveCursor(text, selectionStart, offset);
			int deleteFrom = Math.min(targetPos, selectionStart);
			int deleteTo = Math.max(targetPos, selectionStart);
			newText = new StringBuilder(text).delete(deleteFrom, deleteTo).toString();
			if (offset < 0) {
				selectionEnd = selectionStart = deleteFrom;
			}
		}

		stringSetter.accept(newText);
	}

	public void cut() {
		String text = stringGetter.get();
		clipboardSetter.accept(getSelectedText(text));
		stringSetter.accept(deleteSelectedText(text));
	}

	public void paste() {
		insert(stringGetter.get(), clipboardGetter.get());
		selectionEnd = selectionStart;
	}

	public void copy() {
		clipboardSetter.accept(getSelectedText(stringGetter.get()));
	}

	public void selectAll() {
		selectionEnd = 0;
		selectionStart = stringGetter.get().length();
	}

	private String getSelectedText(String text) {
		int from = Math.min(selectionStart, selectionEnd);
		int to = Math.max(selectionStart, selectionEnd);
		return text.substring(from, to);
	}

	private String deleteSelectedText(String text) {
		if (selectionEnd == selectionStart) {
			return text;
		}

		int from = Math.min(selectionStart, selectionEnd);
		int to = Math.max(selectionStart, selectionEnd);
		String result = text.substring(0, from) + text.substring(to);
		selectionEnd = selectionStart = from;
		return result;
	}

	public void moveCursorToStart() {
		moveCursorToStart(false);
	}

	public void moveCursorToStart(boolean shiftDown) {
		selectionStart = 0;
		updateSelectionRange(shiftDown);
	}

	public void putCursorAtEnd() {
		moveCursorToEnd(false);
	}

	public void moveCursorToEnd(boolean shiftDown) {
		selectionStart = stringGetter.get().length();
		updateSelectionRange(shiftDown);
	}

	public int getSelectionStart() {
		return selectionStart;
	}

	public void moveCursorTo(int position) {
		moveCursorTo(position, true);
	}

	public void moveCursorTo(int position, boolean shiftDown) {
		selectionStart = clampCursorPosition(position);
		updateSelectionRange(shiftDown);
	}

	public int getSelectionEnd() {
		return selectionEnd;
	}

	public void setSelectionEnd(int pos) {
		selectionEnd = clampCursorPosition(pos);
	}

	public void setSelection(int start, int end) {
		int textLength = stringGetter.get().length();
		selectionStart = MathHelper.clamp(start, 0, textLength);
		selectionEnd = MathHelper.clamp(end, 0, textLength);
	}

	public boolean isSelecting() {
		return selectionStart != selectionEnd;
	}

	@Environment(EnvType.CLIENT)
	public enum SelectionType {
		CHARACTER,
		WORD
	}
}
