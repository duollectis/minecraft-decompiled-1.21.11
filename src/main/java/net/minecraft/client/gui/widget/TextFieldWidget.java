package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Однострочное текстовое поле ввода с поддержкой выделения, курсора, подсказки-плейсхолдера
 * и кастомного форматирования через {@link Formatter}.
 * <p>
 * Поддерживает операции буфера обмена (копировать/вставить/вырезать), навигацию по словам
 * (Ctrl+стрелки), выделение мышью (одиночный клик — позиция, двойной — слово).
 */
@Environment(EnvType.CLIENT)
public class TextFieldWidget extends ClickableWidget {

	// GLFW keycodes для клавиш навигации и редактирования
	private static final int KEY_BACKSPACE = 259;
	private static final int KEY_DELETE = 261;
	private static final int KEY_RIGHT = 262;
	private static final int KEY_LEFT = 263;
	private static final int KEY_DOWN = 264;
	private static final int KEY_UP = 265;
	private static final int KEY_PAGE_UP = 266;
	private static final int KEY_PAGE_DOWN = 267;
	private static final int KEY_HOME = 268;
	private static final int KEY_END = 269;

	private static final int BACKGROUND_PADDING = 4;
	private static final int TEXT_HEIGHT = 8;
	private static final int CURSOR_HEIGHT_PADDING = 1;
	private static final int SUGGESTION_COLOR = -8355712;
	private static final int SPACE_CHAR = 32;

	private static final ButtonTextures TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("widget/text_field"), Identifier.ofVanilla("widget/text_field_highlighted")
	);

	public static final int CURSOR_DIRECTION_LEFT = -1;
	public static final int CURSOR_DIRECTION_RIGHT = 1;
	public static final int DEFAULT_EDITABLE_COLOR = -2039584;
	public static final Style PLACEHOLDER_STYLE = Style.EMPTY.withColor(Formatting.DARK_GRAY);
	public static final Style SEARCH_STYLE = Style.EMPTY.withFormatting(Formatting.GRAY, Formatting.ITALIC);

	private static final int CURSOR_BLINK_PERIOD_MS = 300;

	private final TextRenderer textRenderer;
	private String text = "";
	private int maxLength = 32;
	private boolean drawsBackground = true;
	private boolean focusUnlocked = true;
	private boolean editable = true;
	private boolean centered = false;
	private boolean textShadow = true;
	private boolean invertSelectionBackground = true;
	private int firstCharacterIndex;
	private int selectionStart;
	private int selectionEnd;
	private int editableColor = DEFAULT_EDITABLE_COLOR;
	private int uneditableColor = -9408400;
	private @Nullable String suggestion;
	private @Nullable Consumer<String> changedListener;
	private Predicate<String> textPredicate = Objects::nonNull;
	private final List<TextFieldWidget.Formatter> formatters = new ArrayList<>();
	private @Nullable Text placeholder;
	private long lastSwitchFocusTime = Util.getMeasuringTimeMs();
	private int textX;
	private int textY;

	public TextFieldWidget(TextRenderer textRenderer, int width, int height, Text text) {
		this(textRenderer, 0, 0, width, height, text);
	}

	public TextFieldWidget(TextRenderer textRenderer, int x, int y, int width, int height, Text text) {
		this(textRenderer, x, y, width, height, null, text);
	}

	public TextFieldWidget(
			TextRenderer textRenderer,
			int x,
			int y,
			int width,
			int height,
			@Nullable TextFieldWidget copyFrom,
			Text text
	) {
		super(x, y, width, height, text);
		this.textRenderer = textRenderer;
		if (copyFrom != null) {
			setText(copyFrom.getText());
		}

		updateTextPosition();
	}

	public void setChangedListener(Consumer<String> changedListener) {
		this.changedListener = changedListener;
	}

	public void addFormatter(TextFieldWidget.Formatter formatter) {
		formatters.add(formatter);
	}

	@Override
	protected MutableText getNarrationMessage() {
		Text label = getMessage();
		return Text.translatable("gui.narrate.editBox", label, text);
	}

	public void setText(String newText) {
		if (!textPredicate.test(newText)) {
			return;
		}

		text = newText.length() > maxLength ? newText.substring(0, maxLength) : newText;
		setCursorToEnd(false);
		setSelectionEnd(selectionStart);
		onChanged(text);
	}

	public String getText() {
		return text;
	}

	public String getSelectedText() {
		int start = Math.min(selectionStart, selectionEnd);
		int end = Math.max(selectionStart, selectionEnd);
		return text.substring(start, end);
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		updateTextPosition();
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		updateTextPosition();
	}

	public void setTextPredicate(Predicate<String> textPredicate) {
		this.textPredicate = textPredicate;
	}

	/**
	 * Вставляет текст в текущую позицию курсора, заменяя выделение (если есть).
	 * Обрезает вставляемый текст до оставшегося лимита символов и корректно
	 * обрабатывает суррогатные пары Unicode.
	 */
	public void write(String insertText) {
		int selStart = Math.min(selectionStart, selectionEnd);
		int selEnd = Math.max(selectionStart, selectionEnd);
		int remaining = maxLength - text.length() - (selStart - selEnd);

		if (remaining <= 0) {
			return;
		}

		String sanitized = StringHelper.stripInvalidChars(insertText);
		int sanitizedLen = sanitized.length();

		if (remaining < sanitizedLen) {
			if (Character.isHighSurrogate(sanitized.charAt(remaining - 1))) {
				remaining--;
			}

			sanitized = sanitized.substring(0, remaining);
			sanitizedLen = remaining;
		}

		String result = new StringBuilder(text).replace(selStart, selEnd, sanitized).toString();

		if (!textPredicate.test(result)) {
			return;
		}

		text = result;
		setSelectionStart(selStart + sanitizedLen);
		setSelectionEnd(selectionStart);
		onChanged(text);
	}

	private void onChanged(String newText) {
		if (changedListener != null) {
			changedListener.accept(newText);
		}

		updateTextPosition();
	}

	private void erase(int offset, boolean words) {
		if (words) {
			eraseWords(offset);
		}
		else {
			eraseCharacters(offset);
		}
	}

	public void eraseWords(int wordOffset) {
		if (text.isEmpty()) {
			return;
		}

		if (selectionEnd != selectionStart) {
			write("");
		}
		else {
			eraseCharactersTo(getWordSkipPosition(wordOffset));
		}
	}

	public void eraseCharacters(int characterOffset) {
		eraseCharactersTo(getCursorPosWithOffset(characterOffset));
	}

	public void eraseCharactersTo(int position) {
		if (text.isEmpty()) {
			return;
		}

		if (selectionEnd != selectionStart) {
			write("");
			return;
		}

		int start = Math.min(position, selectionStart);
		int end = Math.max(position, selectionStart);

		if (start == end) {
			return;
		}

		String result = new StringBuilder(text).delete(start, end).toString();

		if (!textPredicate.test(result)) {
			return;
		}

		text = result;
		setCursor(start, false);
	}

	public int getWordSkipPosition(int wordOffset) {
		return getWordSkipPosition(wordOffset, getCursor());
	}

	private int getWordSkipPosition(int wordOffset, int cursorPosition) {
		return getWordSkipPosition(wordOffset, cursorPosition, true);
	}

	private int getWordSkipPosition(int wordOffset, int cursorPosition, boolean skipOverSpaces) {
		int pos = cursorPosition;
		boolean movingLeft = wordOffset < 0;
		int steps = Math.abs(wordOffset);

		for (int step = 0; step < steps; step++) {
			if (movingLeft) {
				while (skipOverSpaces && pos > 0 && text.charAt(pos - 1) == ' ') {
					pos--;
				}

				while (pos > 0 && text.charAt(pos - 1) != ' ') {
					pos--;
				}
			}
			else {
				int textLen = text.length();
				pos = text.indexOf(SPACE_CHAR, pos);

				if (pos == -1) {
					pos = textLen;
				}
				else {
					while (skipOverSpaces && pos < textLen && text.charAt(pos) == ' ') {
						pos++;
					}
				}
			}
		}

		return pos;
	}

	public void moveCursor(int offset, boolean shiftKeyPressed) {
		setCursor(getCursorPosWithOffset(offset), shiftKeyPressed);
	}

	private int getCursorPosWithOffset(int offset) {
		return Util.moveCursor(text, selectionStart, offset);
	}

	public void setCursor(int cursor, boolean select) {
		setSelectionStart(cursor);

		if (!select) {
			setSelectionEnd(selectionStart);
		}

		onChanged(text);
	}

	public void setSelectionStart(int cursor) {
		selectionStart = MathHelper.clamp(cursor, 0, text.length());
		updateFirstCharacterIndex(selectionStart);
	}

	public void setCursorToStart(boolean shiftKeyPressed) {
		setCursor(0, shiftKeyPressed);
	}

	public void setCursorToEnd(boolean shiftKeyPressed) {
		setCursor(text.length(), shiftKeyPressed);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (!isInteractable() || !isFocused()) {
			return false;
		}

		switch (input.key()) {
			case KEY_BACKSPACE -> {
				if (editable) {
					erase(-1, input.hasCtrlOrCmd());
				}

				return true;
			}
			case KEY_DELETE -> {
				if (editable) {
					erase(1, input.hasCtrlOrCmd());
				}

				return true;
			}
			case KEY_RIGHT -> {
				if (input.hasCtrlOrCmd()) {
					setCursor(getWordSkipPosition(1), input.hasShift());
				}
				else {
					moveCursor(1, input.hasShift());
				}

				return true;
			}
			case KEY_LEFT -> {
				if (input.hasCtrlOrCmd()) {
					setCursor(getWordSkipPosition(-1), input.hasShift());
				}
				else {
					moveCursor(-1, input.hasShift());
				}

				return true;
			}
			case KEY_HOME -> {
				setCursorToStart(input.hasShift());
				return true;
			}
			case KEY_END -> {
				setCursorToEnd(input.hasShift());
				return true;
			}
			default -> {
				if (input.isSelectAll()) {
					setCursorToEnd(false);
					setSelectionEnd(0);
					return true;
				}

				if (input.isCopy()) {
					MinecraftClient.getInstance().keyboard.setClipboard(getSelectedText());
					return true;
				}

				if (input.isPaste()) {
					if (isEditable()) {
						write(MinecraftClient.getInstance().keyboard.getClipboard());
					}

					return true;
				}

				if (input.isCut()) {
					MinecraftClient.getInstance().keyboard.setClipboard(getSelectedText());

					if (isEditable()) {
						write("");
					}

					return true;
				}

				return false;
			}
		}
	}

	public boolean isActive() {
		return isInteractable() && isFocused() && isEditable();
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (!isActive()) {
			return false;
		}

		if (!input.isValidChar()) {
			return false;
		}

		if (editable) {
			write(input.asString());
		}

		return true;
	}

	private int calculateCursorPos(Click click) {
		int clickOffset = Math.min(MathHelper.floor(click.x()) - textX, getInnerWidth());
		String visibleText = text.substring(firstCharacterIndex);
		return firstCharacterIndex + textRenderer.trimToWidth(visibleText, clickOffset).length();
	}

	private void selectWord(Click click) {
		int clickPos = calculateCursorPos(click);
		int wordStart = getWordSkipPosition(-1, clickPos);
		int wordEnd = getWordSkipPosition(1, clickPos);
		setCursor(wordStart, false);
		setCursor(wordEnd, true);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		if (doubled) {
			selectWord(click);
		}
		else {
			setCursor(calculateCursorPos(click), click.hasShift());
		}
	}

	@Override
	protected void onDrag(Click click, double offsetX, double offsetY) {
		setCursor(calculateCursorPos(click), true);
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (!isVisible()) {
			return;
		}

		if (drawsBackground()) {
			Identifier texture = TEXTURES.get(isInteractable(), isFocused());
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, texture, getX(), getY(), getWidth(), getHeight());
		}

		int textColor = editable ? editableColor : uneditableColor;
		int cursorOffset = selectionStart - firstCharacterIndex;
		String visibleText = textRenderer.trimToWidth(text.substring(firstCharacterIndex), getInnerWidth());
		boolean cursorInView = cursorOffset >= 0 && cursorOffset <= visibleText.length();
		boolean cursorBlinking = isFocused()
				&& (Util.getMeasuringTimeMs() - lastSwitchFocusTime) / CURSOR_BLINK_PERIOD_MS % 2L == 0L
				&& cursorInView;
		int drawX = textX;
		int selectionEndOffset = MathHelper.clamp(selectionEnd - firstCharacterIndex, 0, visibleText.length());

		if (!visibleText.isEmpty()) {
			String beforeCursor = cursorInView ? visibleText.substring(0, cursorOffset) : visibleText;
			OrderedText formattedBefore = format(beforeCursor, firstCharacterIndex);
			context.drawText(textRenderer, formattedBefore, drawX, textY, textColor, textShadow);
			drawX += textRenderer.getWidth(formattedBefore) + 1;
		}

		boolean cursorAtEnd = selectionStart < text.length() || text.length() >= getMaxLength();
		int cursorX = drawX;

		if (!cursorInView) {
			cursorX = cursorOffset > 0 ? textX + width : textX;
		}
		else if (cursorAtEnd) {
			cursorX = drawX - 1;
			drawX--;
		}

		if (!visibleText.isEmpty() && cursorInView && cursorOffset < visibleText.length()) {
			context.drawText(
					textRenderer,
					format(visibleText.substring(cursorOffset), selectionStart),
					drawX,
					textY,
					textColor,
					textShadow
			);
		}

		if (placeholder != null && visibleText.isEmpty() && !isFocused()) {
			context.drawTextWithShadow(textRenderer, placeholder, drawX, textY, textColor);
		}

		if (!cursorAtEnd && suggestion != null) {
			context.drawText(textRenderer, suggestion, cursorX - 1, textY, SUGGESTION_COLOR, textShadow);
		}

		if (selectionEndOffset != cursorOffset) {
			int selectionX = textX + textRenderer.getWidth(visibleText.substring(0, selectionEndOffset));
			context.drawSelection(
					Math.min(cursorX, getX() + width),
					textY - CURSOR_HEIGHT_PADDING,
					Math.min(selectionX - 1, getX() + width),
					textY + CURSOR_HEIGHT_PADDING + TEXT_HEIGHT,
					invertSelectionBackground
			);
		}

		if (cursorBlinking) {
			if (cursorAtEnd) {
				context.fill(cursorX, textY - CURSOR_HEIGHT_PADDING, cursorX + 1, textY + CURSOR_HEIGHT_PADDING + TEXT_HEIGHT, textColor);
			}
			else {
				context.drawText(textRenderer, "_", cursorX, textY, textColor, textShadow);
			}
		}

		if (isHovered()) {
			context.setCursor(isEditable() ? StandardCursors.IBEAM : StandardCursors.NOT_ALLOWED);
		}
	}

	private OrderedText format(String string, int charIndex) {
		for (TextFieldWidget.Formatter formatter : formatters) {
			OrderedText result = formatter.format(string, charIndex);

			if (result != null) {
				return result;
			}
		}

		return OrderedText.styledForwardsVisitedString(string, Style.EMPTY);
	}

	private void updateTextPosition() {
		if (textRenderer == null) {
			return;
		}

		String visibleText = textRenderer.trimToWidth(text.substring(firstCharacterIndex), getInnerWidth());
		textX = getX() + (isCentered()
				? (getWidth() - textRenderer.getWidth(visibleText)) / 2
				: (drawsBackground ? BACKGROUND_PADDING : 0)
		);
		textY = drawsBackground ? getY() + (height - TEXT_HEIGHT) / 2 : getY();
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;

		if (text.length() > maxLength) {
			text = text.substring(0, maxLength);
			onChanged(text);
		}
	}

	private int getMaxLength() {
		return maxLength;
	}

	public int getCursor() {
		return selectionStart;
	}

	public boolean drawsBackground() {
		return drawsBackground;
	}

	public void setDrawsBackground(boolean drawsBackground) {
		this.drawsBackground = drawsBackground;
		updateTextPosition();
	}

	public void setEditableColor(int editableColor) {
		this.editableColor = editableColor;
	}

	public void setUneditableColor(int uneditableColor) {
		this.uneditableColor = uneditableColor;
	}

	@Override
	public void setFocused(boolean focused) {
		if (!focusUnlocked && !focused) {
			return;
		}

		super.setFocused(focused);

		if (focused) {
			lastSwitchFocusTime = Util.getMeasuringTimeMs();
		}
	}

	private boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	private boolean isCentered() {
		return centered;
	}

	public void setCentered(boolean centered) {
		this.centered = centered;
		updateTextPosition();
	}

	public void setTextShadow(boolean textShadow) {
		this.textShadow = textShadow;
	}

	public void setInvertSelectionBackground(boolean invertSelectionBackground) {
		this.invertSelectionBackground = invertSelectionBackground;
	}

	public int getInnerWidth() {
		return drawsBackground() ? width - 8 : width;
	}

	public void setSelectionEnd(int index) {
		selectionEnd = MathHelper.clamp(index, 0, text.length());
		updateFirstCharacterIndex(selectionEnd);
	}

	/**
	 * Обновляет {@code firstCharacterIndex} так, чтобы курсор всегда оставался
	 * в видимой области текстового поля при горизонтальной прокрутке.
	 */
	private void updateFirstCharacterIndex(int cursor) {
		if (textRenderer == null) {
			return;
		}

		firstCharacterIndex = Math.min(firstCharacterIndex, text.length());
		int innerWidth = getInnerWidth();
		String visibleText = textRenderer.trimToWidth(text.substring(firstCharacterIndex), innerWidth);
		int visibleEnd = visibleText.length() + firstCharacterIndex;

		if (cursor == firstCharacterIndex) {
			firstCharacterIndex -= textRenderer.trimToWidth(text, innerWidth, true).length();
		}

		if (cursor > visibleEnd) {
			firstCharacterIndex += cursor - visibleEnd;
		}
		else if (cursor <= firstCharacterIndex) {
			firstCharacterIndex -= firstCharacterIndex - cursor;
		}

		firstCharacterIndex = MathHelper.clamp(firstCharacterIndex, 0, text.length());
	}

	public void setFocusUnlocked(boolean focusUnlocked) {
		this.focusUnlocked = focusUnlocked;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void setSuggestion(@Nullable String suggestion) {
		this.suggestion = suggestion;
	}

	public int getCharacterX(int index) {
		return index > text.length()
				? getX()
				: getX() + textRenderer.getWidth(text.substring(0, index));
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, getNarrationMessage());
	}

	public void setPlaceholder(Text placeholder) {
		boolean hasEmptyStyle = placeholder.getStyle().equals(Style.EMPTY);
		this.placeholder = hasEmptyStyle ? placeholder.copy().fillStyle(PLACEHOLDER_STYLE) : placeholder;
	}

	/**
	 * Функциональный интерфейс для кастомного форматирования текста в поле ввода.
	 * Возвращает {@code null}, если данный форматтер не применим к строке.
	 */
	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface Formatter {

		@Nullable OrderedText format(String string, int firstCharacterIndex);
	}
}
