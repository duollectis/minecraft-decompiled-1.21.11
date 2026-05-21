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

@Environment(EnvType.CLIENT)
/**
 * {@code TextFieldWidget}.
 */
public class TextFieldWidget extends ClickableWidget {

	private static final ButtonTextures TEXTURES = new ButtonTextures(
			Identifier.ofVanilla("widget/text_field"), Identifier.ofVanilla("widget/text_field_highlighted")
	);
	public static final int CURSOR_DIRECTION_LEFT = -1;
	public static final int CURSOR_DIRECTION_RIGHT = 1;
	private static final int BORDER_THICKNESS = 1;
	private static final String HORIZONTAL_CURSOR = "_";
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
	private int editableColor = -2039584;
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
			this.setText(copyFrom.getText());
		}

		this.updateTextPosition();
	}

	public void setChangedListener(Consumer<String> changedListener) {
		this.changedListener = changedListener;
	}

	public void addFormatter(TextFieldWidget.Formatter formatter) {
		this.formatters.add(formatter);
	}

	@Override
	protected MutableText getNarrationMessage() {
		Text text = this.getMessage();
		return Text.translatable("gui.narrate.editBox", text, this.text);
	}

	public void setText(String text) {
		if (this.textPredicate.test(text)) {
			if (text.length() > this.maxLength) {
				this.text = text.substring(0, this.maxLength);
			}
			else {
				this.text = text;
			}

			this.setCursorToEnd(false);
			this.setSelectionEnd(this.selectionStart);
			this.onChanged(text);
		}
	}

	public String getText() {
		return this.text;
	}

	public String getSelectedText() {
		int i = Math.min(this.selectionStart, this.selectionEnd);
		int j = Math.max(this.selectionStart, this.selectionEnd);
		return this.text.substring(i, j);
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		this.updateTextPosition();
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		this.updateTextPosition();
	}

	public void setTextPredicate(Predicate<String> textPredicate) {
		this.textPredicate = textPredicate;
	}

	public void write(String text) {
		int i = Math.min(this.selectionStart, this.selectionEnd);
		int j = Math.max(this.selectionStart, this.selectionEnd);
		int k = this.maxLength - this.text.length() - (i - j);
		if (k > 0) {
			String string = StringHelper.stripInvalidChars(text);
			int l = string.length();
			if (k < l) {
				if (Character.isHighSurrogate(string.charAt(k - 1))) {
					k--;
				}

				string = string.substring(0, k);
				l = k;
			}

			String string2 = new StringBuilder(this.text).replace(i, j, string).toString();
			if (this.textPredicate.test(string2)) {
				this.text = string2;
				this.setSelectionStart(i + l);
				this.setSelectionEnd(this.selectionStart);
				this.onChanged(this.text);
			}
		}
	}

	private void onChanged(String newText) {
		if (this.changedListener != null) {
			this.changedListener.accept(newText);
		}

		this.updateTextPosition();
	}

	private void erase(int offset, boolean words) {
		if (words) {
			this.eraseWords(offset);
		}
		else {
			this.eraseCharacters(offset);
		}
	}

	public void eraseWords(int wordOffset) {
		if (!this.text.isEmpty()) {
			if (this.selectionEnd != this.selectionStart) {
				this.write("");
			}
			else {
				this.eraseCharactersTo(this.getWordSkipPosition(wordOffset));
			}
		}
	}

	public void eraseCharacters(int characterOffset) {
		this.eraseCharactersTo(this.getCursorPosWithOffset(characterOffset));
	}

	public void eraseCharactersTo(int position) {
		if (!this.text.isEmpty()) {
			if (this.selectionEnd != this.selectionStart) {
				this.write("");
			}
			else {
				int i = Math.min(position, this.selectionStart);
				int j = Math.max(position, this.selectionStart);
				if (i != j) {
					String string = new StringBuilder(this.text).delete(i, j).toString();
					if (this.textPredicate.test(string)) {
						this.text = string;
						this.setCursor(i, false);
					}
				}
			}
		}
	}

	public int getWordSkipPosition(int wordOffset) {
		return this.getWordSkipPosition(wordOffset, this.getCursor());
	}

	private int getWordSkipPosition(int wordOffset, int cursorPosition) {
		return this.getWordSkipPosition(wordOffset, cursorPosition, true);
	}

	private int getWordSkipPosition(int wordOffset, int cursorPosition, boolean skipOverSpaces) {
		int i = cursorPosition;
		boolean bl = wordOffset < 0;
		int j = Math.abs(wordOffset);

		for (int k = 0; k < j; k++) {
			if (!bl) {
				int l = this.text.length();
				i = this.text.indexOf(32, i);
				if (i == -1) {
					i = l;
				}
				else {
					while (skipOverSpaces && i < l && this.text.charAt(i) == ' ') {
						i++;
					}
				}
			}
			else {
				while (skipOverSpaces && i > 0 && this.text.charAt(i - 1) == ' ') {
					i--;
				}

				while (i > 0 && this.text.charAt(i - 1) != ' ') {
					i--;
				}
			}
		}

		return i;
	}

	public void moveCursor(int offset, boolean shiftKeyPressed) {
		this.setCursor(this.getCursorPosWithOffset(offset), shiftKeyPressed);
	}

	private int getCursorPosWithOffset(int offset) {
		return Util.moveCursor(this.text, this.selectionStart, offset);
	}

	public void setCursor(int cursor, boolean select) {
		this.setSelectionStart(cursor);
		if (!select) {
			this.setSelectionEnd(this.selectionStart);
		}

		this.onChanged(this.text);
	}

	public void setSelectionStart(int cursor) {
		this.selectionStart = MathHelper.clamp(cursor, 0, this.text.length());
		this.updateFirstCharacterIndex(this.selectionStart);
	}

	public void setCursorToStart(boolean shiftKeyPressed) {
		this.setCursor(0, shiftKeyPressed);
	}

	public void setCursorToEnd(boolean shiftKeyPressed) {
		this.setCursor(this.text.length(), shiftKeyPressed);
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.isInteractable() && this.isFocused()) {
			switch (input.key()) {
				case 259:
					if (this.editable) {
						this.erase(-1, input.hasCtrlOrCmd());
					}

					return true;
				case 260:
				case 264:
				case 265:
				case 266:
				case 267:
				default:
					if (input.isSelectAll()) {
						this.setCursorToEnd(false);
						this.setSelectionEnd(0);
						return true;
					}
					else if (input.isCopy()) {
						MinecraftClient.getInstance().keyboard.setClipboard(this.getSelectedText());
						return true;
					}
					else if (input.isPaste()) {
						if (this.isEditable()) {
							this.write(MinecraftClient.getInstance().keyboard.getClipboard());
						}

						return true;
					}
					else {
						if (input.isCut()) {
							MinecraftClient.getInstance().keyboard.setClipboard(this.getSelectedText());
							if (this.isEditable()) {
								this.write("");
							}

							return true;
						}

						return false;
					}
				case 261:
					if (this.editable) {
						this.erase(1, input.hasCtrlOrCmd());
					}

					return true;
				case 262:
					if (input.hasCtrlOrCmd()) {
						this.setCursor(this.getWordSkipPosition(1), input.hasShift());
					}
					else {
						this.moveCursor(1, input.hasShift());
					}

					return true;
				case 263:
					if (input.hasCtrlOrCmd()) {
						this.setCursor(this.getWordSkipPosition(-1), input.hasShift());
					}
					else {
						this.moveCursor(-1, input.hasShift());
					}

					return true;
				case 268:
					this.setCursorToStart(input.hasShift());
					return true;
				case 269:
					this.setCursorToEnd(input.hasShift());
					return true;
			}
		}
		else {
			return false;
		}
	}

	public boolean isActive() {
		return this.isInteractable() && this.isFocused() && this.isEditable();
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (!this.isActive()) {
			return false;
		}
		else if (input.isValidChar()) {
			if (this.editable) {
				this.write(input.asString());
			}

			return true;
		}
		else {
			return false;
		}
	}

	private int calculateCursorPos(Click click) {
		int i = Math.min(MathHelper.floor(click.x()) - this.textX, this.getInnerWidth());
		String string = this.text.substring(this.firstCharacterIndex);
		return this.firstCharacterIndex + this.textRenderer.trimToWidth(string, i).length();
	}

	private void selectWord(Click click) {
		int i = this.calculateCursorPos(click);
		int j = this.getWordSkipPosition(-1, i);
		int k = this.getWordSkipPosition(1, i);
		this.setCursor(j, false);
		this.setCursor(k, true);
	}

	@Override
	public void onClick(Click click, boolean doubled) {
		if (doubled) {
			this.selectWord(click);
		}
		else {
			this.setCursor(this.calculateCursorPos(click), click.hasShift());
		}
	}

	@Override
	protected void onDrag(Click click, double offsetX, double offsetY) {
		this.setCursor(this.calculateCursorPos(click), true);
	}

	@Override
	public void playDownSound(SoundManager soundManager) {
	}

	@Override
	public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (this.isVisible()) {
			if (this.drawsBackground()) {
				Identifier identifier = TEXTURES.get(this.isInteractable(), this.isFocused());
				context.drawGuiTexture(
						RenderPipelines.GUI_TEXTURED,
						identifier,
						this.getX(),
						this.getY(),
						this.getWidth(),
						this.getHeight()
				);
			}

			int i = this.editable ? this.editableColor : this.uneditableColor;
			int j = this.selectionStart - this.firstCharacterIndex;
			String
					string =
					this.textRenderer.trimToWidth(this.text.substring(this.firstCharacterIndex), this.getInnerWidth());
			boolean bl = j >= 0 && j <= string.length();
			boolean
					bl2 =
					this.isFocused() && (Util.getMeasuringTimeMs() - this.lastSwitchFocusTime) / 300L % 2L == 0L && bl;
			int k = this.textX;
			int l = MathHelper.clamp(this.selectionEnd - this.firstCharacterIndex, 0, string.length());
			if (!string.isEmpty()) {
				String string2 = bl ? string.substring(0, j) : string;
				OrderedText orderedText = this.format(string2, this.firstCharacterIndex);
				context.drawText(this.textRenderer, orderedText, k, this.textY, i, this.textShadow);
				k += this.textRenderer.getWidth(orderedText) + 1;
			}

			boolean bl3 = this.selectionStart < this.text.length() || this.text.length() >= this.getMaxLength();
			int m = k;
			if (!bl) {
				m = j > 0 ? this.textX + this.width : this.textX;
			}
			else if (bl3) {
				m = k - 1;
				k--;
			}

			if (!string.isEmpty() && bl && j < string.length()) {
				context.drawText(
						this.textRenderer,
						this.format(string.substring(j), this.selectionStart),
						k,
						this.textY,
						i,
						this.textShadow
				);
			}

			if (this.placeholder != null && string.isEmpty() && !this.isFocused()) {
				context.drawTextWithShadow(this.textRenderer, this.placeholder, k, this.textY, i);
			}

			if (!bl3 && this.suggestion != null) {
				context.drawText(this.textRenderer, this.suggestion, m - 1, this.textY, -8355712, this.textShadow);
			}

			if (l != j) {
				int n = this.textX + this.textRenderer.getWidth(string.substring(0, l));
				context.drawSelection(
						Math.min(m, this.getX() + this.width),
						this.textY - 1,
						Math.min(n - 1, this.getX() + this.width),
						this.textY + 1 + 9,
						this.invertSelectionBackground
				);
			}

			if (bl2) {
				if (bl3) {
					context.fill(m, this.textY - 1, m + 1, this.textY + 1 + 9, i);
				}
				else {
					context.drawText(this.textRenderer, "_", m, this.textY, i, this.textShadow);
				}
			}

			if (this.isHovered()) {
				context.setCursor(this.isEditable() ? StandardCursors.IBEAM : StandardCursors.NOT_ALLOWED);
			}
		}
	}

	private OrderedText format(String string, int firstCharacterIndex) {
		for (TextFieldWidget.Formatter formatter : this.formatters) {
			OrderedText orderedText = formatter.format(string, firstCharacterIndex);
			if (orderedText != null) {
				return orderedText;
			}
		}

		return OrderedText.styledForwardsVisitedString(string, Style.EMPTY);
	}

	private void updateTextPosition() {
		if (this.textRenderer != null) {
			String
					string =
					this.textRenderer.trimToWidth(this.text.substring(this.firstCharacterIndex), this.getInnerWidth());
			this.textX =
					this.getX() + (this.isCentered() ? (this.getWidth() - this.textRenderer.getWidth(string)) / 2
					                                 : (this.drawsBackground ? 4 : 0)
					);
			this.textY = this.drawsBackground ? this.getY() + (this.height - 8) / 2 : this.getY();
		}
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
		if (this.text.length() > maxLength) {
			this.text = this.text.substring(0, maxLength);
			this.onChanged(this.text);
		}
	}

	private int getMaxLength() {
		return this.maxLength;
	}

	public int getCursor() {
		return this.selectionStart;
	}

	public boolean drawsBackground() {
		return this.drawsBackground;
	}

	public void setDrawsBackground(boolean drawsBackground) {
		this.drawsBackground = drawsBackground;
		this.updateTextPosition();
	}

	public void setEditableColor(int editableColor) {
		this.editableColor = editableColor;
	}

	public void setUneditableColor(int uneditableColor) {
		this.uneditableColor = uneditableColor;
	}

	@Override
	public void setFocused(boolean focused) {
		if (this.focusUnlocked || focused) {
			super.setFocused(focused);
			if (focused) {
				this.lastSwitchFocusTime = Util.getMeasuringTimeMs();
			}
		}
	}

	private boolean isEditable() {
		return this.editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	private boolean isCentered() {
		return this.centered;
	}

	public void setCentered(boolean centered) {
		this.centered = centered;
		this.updateTextPosition();
	}

	public void setTextShadow(boolean textShadow) {
		this.textShadow = textShadow;
	}

	public void setInvertSelectionBackground(boolean invertSelectionBackground) {
		this.invertSelectionBackground = invertSelectionBackground;
	}

	public int getInnerWidth() {
		return this.drawsBackground() ? this.width - 8 : this.width;
	}

	public void setSelectionEnd(int index) {
		this.selectionEnd = MathHelper.clamp(index, 0, this.text.length());
		this.updateFirstCharacterIndex(this.selectionEnd);
	}

	private void updateFirstCharacterIndex(int cursor) {
		if (this.textRenderer != null) {
			this.firstCharacterIndex = Math.min(this.firstCharacterIndex, this.text.length());
			int i = this.getInnerWidth();
			String string = this.textRenderer.trimToWidth(this.text.substring(this.firstCharacterIndex), i);
			int j = string.length() + this.firstCharacterIndex;
			if (cursor == this.firstCharacterIndex) {
				this.firstCharacterIndex =
						this.firstCharacterIndex - this.textRenderer.trimToWidth(this.text, i, true).length();
			}

			if (cursor > j) {
				this.firstCharacterIndex += cursor - j;
			}
			else if (cursor <= this.firstCharacterIndex) {
				this.firstCharacterIndex = this.firstCharacterIndex - (this.firstCharacterIndex - cursor);
			}

			this.firstCharacterIndex = MathHelper.clamp(this.firstCharacterIndex, 0, this.text.length());
		}
	}

	public void setFocusUnlocked(boolean focusUnlocked) {
		this.focusUnlocked = focusUnlocked;
	}

	public boolean isVisible() {
		return this.visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void setSuggestion(@Nullable String suggestion) {
		this.suggestion = suggestion;
	}

	public int getCharacterX(int index) {
		return index > this.text.length() ? this.getX()
		                                  : this.getX() + this.textRenderer.getWidth(this.text.substring(0, index));
	}

	@Override
	public void appendClickableNarrations(NarrationMessageBuilder builder) {
		builder.put(NarrationPart.TITLE, this.getNarrationMessage());
	}

	public void setPlaceholder(Text placeholder) {
		boolean bl = placeholder.getStyle().equals(Style.EMPTY);
		this.placeholder = (Text) (bl ? placeholder.copy().fillStyle(PLACEHOLDER_STYLE) : placeholder);
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code Formatter}.
	 */
	public interface Formatter {

		@Nullable OrderedText format(String string, int firstCharacterIndex);
	}
}
