package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;

import java.util.function.Consumer;

/**
 * Многострочный редактируемый текстовый виджет с поддержкой прокрутки, выделения текста,
 * мигающего курсора и отображения плейсхолдера при пустом содержимом.
 * Делегирует всю логику редактирования в {@link EditBox}.
 */
@Environment(EnvType.CLIENT)
public class EditBoxWidget extends ScrollableTextFieldWidget {

	private static final int CURSOR_PADDING = 1;
	private static final int CURSOR_COLOR = -3092272;
	private static final String UNDERSCORE = "_";
	private static final int UNFOCUSED_BOX_TEXT_COLOR = ColorHelper.withAlpha(204, -2039584);
	private static final int CURSOR_BLINK_INTERVAL = 300;
	private static final int LINE_HEIGHT = 9;

	private final TextRenderer textRenderer;
	private final Text placeholder;
	private final EditBox editBox;
	private final int textColor;
	private final boolean textShadow;
	private final int cursorColor;
	private long lastSwitchFocusTime = Util.getMeasuringTimeMs();

	EditBoxWidget(
			TextRenderer textRenderer,
			int x,
			int y,
			int width,
			int height,
			Text placeholder,
			Text message,
			int textColor,
			boolean textShadow,
			int cursorColor,
			boolean hasBackground,
			boolean hasOverlay
	) {
		super(x, y, width, height, message, hasBackground, hasOverlay);
		this.textRenderer = textRenderer;
		this.textShadow = textShadow;
		this.textColor = textColor;
		this.cursorColor = cursorColor;
		this.placeholder = placeholder;
		this.editBox = new EditBox(textRenderer, width - getPadding());
		this.editBox.setCursorChangeListener(this::onCursorChange);
	}

	public void setMaxLength(int maxLength) {
		editBox.setMaxLength(maxLength);
	}

	public void setMaxLines(int maxLines) {
		editBox.setMaxLines(maxLines);
	}

	public void setChangeListener(Consumer<String> changeListener) {
		editBox.setChangeListener(changeListener);
	}

	public void setText(String text) {
		setText(text, false);
	}

	public void setText(String text, boolean allowOverflow) {
		editBox.setText(text, allowOverflow);
	}

	public String getText() {
		return editBox.getText();
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, Text.translatable("gui.narrate.editBox", getMessage(), getText()));
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		if (doubled) {
			editBox.selectWord();
		} else {
			editBox.setSelecting(click.hasShift());
			moveCursor(click.x(), click.y());
		}
	}

	@Override
	protected void onDrag(Click click, double offsetX, double offsetY) {
		editBox.setSelecting(true);
		moveCursor(click.x(), click.y());
		editBox.setSelecting(click.hasShift());
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		return editBox.handleSpecialKey(input);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (!visible || !isFocused() || !input.isValidChar()) {
			return false;
		}

		editBox.replaceSelection(input.asString());
		return true;
	}

	@Override
	protected void renderContents(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		String text = editBox.getText();
		if (text.isEmpty() && !isFocused()) {
			context.drawWrappedTextWithShadow(
					textRenderer,
					placeholder,
					getTextX(),
					getTextY(),
					width - getPadding(),
					UNFOCUSED_BOX_TEXT_COLOR
			);
			return;
		}

		int cursor = editBox.getCursor();
		boolean blinkOn = isFocused()
				&& (Util.getMeasuringTimeMs() - lastSwitchFocusTime) / CURSOR_BLINK_INTERVAL % 2L == 0L;
		boolean cursorBeforeEnd = cursor < text.length();
		int cursorX = 0;
		int lastLineY = 0;
		int lineY = getTextY();
		boolean cursorDrawn = false;

		for (EditBox.Substring substring : editBox.getLines()) {
			boolean lineVisible = isVisible(lineY, lineY + LINE_HEIGHT);
			int lineX = getTextX();

			if (blinkOn && cursorBeforeEnd && cursor >= substring.beginIndex() && cursor <= substring.endIndex()) {
				if (lineVisible) {
					String beforeCursor = text.substring(substring.beginIndex(), cursor);
					context.drawText(textRenderer, beforeCursor, lineX, lineY, textColor, textShadow);
					cursorX = lineX + textRenderer.getWidth(beforeCursor);
					if (!cursorDrawn) {
						context.fill(cursorX, lineY - 1, cursorX + 1, lineY + 1 + LINE_HEIGHT, cursorColor);
						cursorDrawn = true;
					}

					context.drawText(
							textRenderer,
							text.substring(cursor, substring.endIndex()),
							cursorX,
							lineY,
							textColor,
							textShadow
					);
				}
			} else {
				if (lineVisible) {
					String lineText = text.substring(substring.beginIndex(), substring.endIndex());
					context.drawText(textRenderer, lineText, lineX, lineY, textColor, textShadow);
					cursorX = lineX + textRenderer.getWidth(lineText) - 1;
				}

				lastLineY = lineY;
			}

			lineY += LINE_HEIGHT;
		}

		if (blinkOn && !cursorBeforeEnd && isVisible(lastLineY, lastLineY + LINE_HEIGHT)) {
			context.drawText(textRenderer, UNDERSCORE, cursorX + 1, lastLineY, cursorColor, textShadow);
		}

		if (editBox.hasSelection()) {
			EditBox.Substring selection = editBox.getSelection();
			int selectionX = getTextX();
			lineY = getTextY();

			for (EditBox.Substring line : editBox.getLines()) {
				if (selection.beginIndex() > line.endIndex()) {
					lineY += LINE_HEIGHT;
					continue;
				}

				if (line.beginIndex() > selection.endIndex()) {
					break;
				}

				if (isVisible(lineY, lineY + LINE_HEIGHT)) {
					int selStartOffset = textRenderer.getWidth(
							text.substring(line.beginIndex(), Math.max(selection.beginIndex(), line.beginIndex()))
					);
					int selEndOffset = selection.endIndex() > line.endIndex()
							? width - getTextMargin()
							: textRenderer.getWidth(text.substring(line.beginIndex(), selection.endIndex()));

					context.drawSelection(selectionX + selStartOffset, lineY, selectionX + selEndOffset, lineY + LINE_HEIGHT, true);
				}

				lineY += LINE_HEIGHT;
			}
		}

		if (isHovered()) {
			context.setCursor(StandardCursors.IBEAM);
		}
	}

	@Override
	protected void renderOverlay(DrawContext context) {
		super.renderOverlay(context);
		if (!editBox.hasMaxLength()) {
			return;
		}

		int maxLength = editBox.getMaxLength();
		Text limitText = Text.translatable("gui.multiLineEditBox.character_limit", editBox.getText().length(), maxLength);
		context.drawTextWithShadow(
				textRenderer,
				limitText,
				getX() + width - textRenderer.getWidth(limitText),
				getY() + height + 4,
				-6250336
		);
	}

	@Override
	public int getContentsHeight() {
		return LINE_HEIGHT * editBox.getLineCount();
	}

	@Override
	protected double getDeltaYPerScroll() {
		return LINE_HEIGHT / 2.0;
	}

	private void onCursorChange() {
		double scrollY = getScrollY();
		EditBox.Substring topLine = editBox.getLine((int) (scrollY / LINE_HEIGHT));
		if (editBox.getCursor() <= topLine.beginIndex()) {
			setScrollY(editBox.getCurrentLineIndex() * LINE_HEIGHT);
			return;
		}

		EditBox.Substring bottomLine = editBox.getLine((int) ((scrollY + height) / LINE_HEIGHT) - 1);
		if (editBox.getCursor() > bottomLine.endIndex()) {
			setScrollY(editBox.getCurrentLineIndex() * LINE_HEIGHT - height + LINE_HEIGHT + getPadding());
		}
	}

	private void moveCursor(double mouseX, double mouseY) {
		double relX = mouseX - getX() - getTextMargin();
		double relY = mouseY - getY() - getTextMargin() + getScrollY();
		editBox.moveCursor(relX, relY);
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
		if (focused) {
			lastSwitchFocusTime = Util.getMeasuringTimeMs();
		}
	}

	public static EditBoxWidget.Builder builder() {
		return new EditBoxWidget.Builder();
	}

	/**
	 * Строитель для создания {@link EditBoxWidget} с настраиваемыми параметрами отображения.
	 * Позволяет задать позицию, цвета, тени и фон перед финальным вызовом {@link #build}.
	 */
	@Environment(EnvType.CLIENT)
	public static class Builder {

		private int x;
		private int y;
		private Text placeholder = ScreenTexts.EMPTY;
		private int textColor = -2039584;
		private boolean textShadow = true;
		private int cursorColor = -3092272;
		private boolean hasBackground = true;
		private boolean hasOverlay = true;

		public EditBoxWidget.Builder x(int x) {
			this.x = x;
			return this;
		}

		public EditBoxWidget.Builder y(int y) {
			this.y = y;
			return this;
		}

		public EditBoxWidget.Builder placeholder(Text placeholder) {
			this.placeholder = placeholder;
			return this;
		}

		public EditBoxWidget.Builder textColor(int textColor) {
			this.textColor = textColor;
			return this;
		}

		public EditBoxWidget.Builder textShadow(boolean textShadow) {
			this.textShadow = textShadow;
			return this;
		}

		public EditBoxWidget.Builder cursorColor(int cursorColor) {
			this.cursorColor = cursorColor;
			return this;
		}

		public EditBoxWidget.Builder hasBackground(boolean hasBackground) {
			this.hasBackground = hasBackground;
			return this;
		}

		public EditBoxWidget.Builder hasOverlay(boolean hasOverlay) {
			this.hasOverlay = hasOverlay;
			return this;
		}

		public EditBoxWidget build(TextRenderer textRenderer, int width, int height, Text message) {
			return new EditBoxWidget(
					textRenderer,
					x,
					y,
					width,
					height,
					placeholder,
					message,
					textColor,
					textShadow,
					cursorColor,
					hasBackground,
					hasOverlay
			);
		}
	}
}
