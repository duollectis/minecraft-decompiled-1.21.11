package net.minecraft.client.gui.screen;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.command.CommandSource;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Управляет автодополнением команд и чата в текстовом поле.
 * Отображает выпадающее окно подсказок, подсвечивает синтаксис команд
 * и обрабатывает навигацию по списку предложений.
 */
@Environment(EnvType.CLIENT)
public class ChatInputSuggestor {

	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
	private static final Style ERROR_STYLE = Style.EMPTY.withColor(Formatting.RED);
	private static final Style INFO_STYLE = Style.EMPTY.withColor(Formatting.GRAY);
	private static final int SUGGESTION_TEXT_COLOR = -5592406;
	private static final int SUGGESTION_LINE_HEIGHT = 12;
	private static final int NON_CHAT_Y_OFFSET = 72;
	private static final List<Style> HIGHLIGHT_STYLES = Stream
			.of(Formatting.AQUA, Formatting.YELLOW, Formatting.GREEN, Formatting.LIGHT_PURPLE, Formatting.GOLD)
			.map(Style.EMPTY::withColor)
			.collect(ImmutableList.toImmutableList());
	final MinecraftClient client;
	private final Screen owner;
	final TextFieldWidget textField;
	final TextRenderer textRenderer;
	private final boolean slashOptional;
	private final boolean suggestingWhenEmpty;
	final int inWindowIndexOffset;
	final int maxSuggestionSize;
	final boolean chatScreenSized;
	final int color;
	private final List<OrderedText> messages = Lists.newArrayList();
	private int x;
	private int width;
	private @Nullable ParseResults<ClientCommandSource> parse;
	private @Nullable CompletableFuture<Suggestions> pendingSuggestions;
	private ChatInputSuggestor.@Nullable SuggestionWindow window;
	private boolean windowActive;
	boolean completingSuggestions;
	private boolean canLeave = true;

	public ChatInputSuggestor(
			MinecraftClient client,
			Screen owner,
			TextFieldWidget textField,
			TextRenderer textRenderer,
			boolean slashOptional,
			boolean suggestingWhenEmpty,
			int inWindowIndexOffset,
			int maxSuggestionSize,
			boolean chatScreenSized,
			int color
	) {
		this.client = client;
		this.owner = owner;
		this.textField = textField;
		this.textRenderer = textRenderer;
		this.slashOptional = slashOptional;
		this.suggestingWhenEmpty = suggestingWhenEmpty;
		this.inWindowIndexOffset = inWindowIndexOffset;
		this.maxSuggestionSize = maxSuggestionSize;
		this.chatScreenSized = chatScreenSized;
		this.color = color;
		textField.addFormatter(this::provideRenderText);
	}

	public void setWindowActive(boolean windowActive) {
		this.windowActive = windowActive;
		if (!windowActive) {
			this.window = null;
		}
	}

	public void setCanLeave(boolean canLeave) {
		this.canLeave = canLeave;
	}

	public boolean keyPressed(KeyInput input) {
		boolean hasWindow = window != null;

		if (hasWindow && window.keyPressed(input)) {
			return true;
		}

		if (owner.getFocused() != textField || !input.isTab() || canLeave && !hasWindow) {
			return false;
		}

		show(true);
		return true;
	}

	public boolean mouseScrolled(double amount) {
		return window != null && window.mouseScrolled(MathHelper.clamp(amount, -1.0, 1.0));
	}

	public boolean mouseClicked(Click click) {
		return window != null && window.mouseClicked((int) click.x(), (int) click.y());
	}

	/**
	 * Открывает окно подсказок на основе уже вычисленных {@code pendingSuggestions}.
	 * Вычисляет оптимальную позицию X, чтобы окно не выходило за границы поля ввода.
	 *
	 * @param narrateFirstSuggestion если {@code true}, первая подсказка будет озвучена нарратором
	 */
	public void show(boolean narrateFirstSuggestion) {
		if (pendingSuggestions == null || !pendingSuggestions.isDone()) {
			return;
		}

		Suggestions suggestions = pendingSuggestions.join();

		if (suggestions.isEmpty()) {
			return;
		}

		int maxWidth = 0;

		for (Suggestion suggestion : suggestions.getList()) {
			maxWidth = Math.max(maxWidth, textRenderer.getWidth(suggestion.getText()));
		}

		int windowX = MathHelper.clamp(
				textField.getCharacterX(suggestions.getRange().getStart()),
				0,
				textField.getCharacterX(0) + textField.getInnerWidth() - maxWidth
		);
		int windowY = chatScreenSized ? owner.height - SUGGESTION_LINE_HEIGHT : NON_CHAT_Y_OFFSET;
		window = new SuggestionWindow(
				windowX,
				windowY,
				maxWidth,
				sortSuggestions(suggestions),
				narrateFirstSuggestion
		);
	}

	public boolean isOpen() {
		return window != null;
	}

	public Text getSuggestionUsageNarrationText() {
		boolean isCycling = window != null && window.completed;
		return isCycling
				? (canLeave
						? Text.translatable("narration.suggestion.usage.cycle.hidable")
						: Text.translatable("narration.suggestion.usage.cycle.fixed"))
				: (canLeave
						? Text.translatable("narration.suggestion.usage.fill.hidable")
						: Text.translatable("narration.suggestion.usage.fill.fixed"));
	}

	public void clearWindow() {
		window = null;
	}

	private List<Suggestion> sortSuggestions(Suggestions suggestions) {
		String textBeforeCursor = textField.getText().substring(0, textField.getCursor());
		int wordStart = getStartOfCurrentWord(textBeforeCursor);
		String currentWord = textBeforeCursor.substring(wordStart).toLowerCase(Locale.ROOT);
		List<Suggestion> matching = Lists.newArrayList();
		List<Suggestion> nonMatching = Lists.newArrayList();

		for (Suggestion suggestion : suggestions.getList()) {
			boolean startsWithWord = suggestion.getText().startsWith(currentWord)
					|| suggestion.getText().startsWith("minecraft:" + currentWord);

			if (startsWithWord) {
				matching.add(suggestion);
			} else {
				nonMatching.add(suggestion);
			}
		}

		matching.addAll(nonMatching);
		return matching;
	}

	/**
	 * Обновляет состояние автодополнения: перепарсивает команду, вычисляет новые подсказки
	 * и обновляет подсветку синтаксиса в текстовом поле.
	 */
	public void refresh() {
		String inputText = textField.getText();

		if (parse != null && !parse.getReader().getString().equals(inputText)) {
			parse = null;
		}

		if (!completingSuggestions) {
			textField.setSuggestion(null);
			window = null;
		}

		messages.clear();
		StringReader reader = new StringReader(inputText);
		boolean startsWithSlash = reader.canRead() && reader.peek() == '/';

		if (startsWithSlash) {
			reader.skip();
		}

		boolean isCommand = slashOptional || startsWithSlash;
		int cursorPos = textField.getCursor();

		if (isCommand) {
			CommandDispatcher<ClientCommandSource> dispatcher = client.player.networkHandler.getCommandDispatcher();

			if (parse == null) {
				parse = dispatcher.parse(reader, client.player.networkHandler.getCommandSource());
			}

			int minCursor = suggestingWhenEmpty ? reader.getCursor() : 1;

			if (cursorPos >= minCursor && (window == null || !completingSuggestions)) {
				pendingSuggestions = dispatcher.getCompletionSuggestions(parse, cursorPos);
				pendingSuggestions.thenRun(() -> {
					if (pendingSuggestions.isDone()) {
						showCommandSuggestions();
					}
				});
			}
		} else {
			String textBeforeCursor = inputText.substring(0, cursorPos);
			int wordStart = getStartOfCurrentWord(textBeforeCursor);
			Collection<String> chatSuggestions = client.player.networkHandler.getCommandSource().getChatSuggestions();
			pendingSuggestions = CommandSource.suggestMatching(
					chatSuggestions,
					new SuggestionsBuilder(textBeforeCursor, wordStart)
			);
		}
	}

	private static int getStartOfCurrentWord(String input) {
		if (Strings.isNullOrEmpty(input)) {
			return 0;
		}

		int lastWordStart = 0;
		Matcher matcher = WHITESPACE_PATTERN.matcher(input);

		while (matcher.find()) {
			lastWordStart = matcher.end();
		}

		return lastWordStart;
	}

	private static OrderedText formatException(CommandSyntaxException exception) {
		Text text = Texts.toText(exception.getRawMessage());
		String context = exception.getContext();
		return context == null
				? text.asOrderedText()
				: Text.translatable("command.context.parse_error", text, exception.getCursor(), context)
						.asOrderedText();
	}

	private void showCommandSuggestions() {
		boolean hasUnknownArgument = false;

		if (textField.getCursor() == textField.getText().length()) {
			if (pendingSuggestions.join().isEmpty() && !parse.getExceptions().isEmpty()) {
				int literalIncorrectCount = 0;

				for (Entry<CommandNode<ClientCommandSource>, CommandSyntaxException> entry : parse.getExceptions().entrySet()) {
					CommandSyntaxException syntaxException = entry.getValue();

					if (syntaxException.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect()) {
						literalIncorrectCount++;
					} else {
						messages.add(formatException(syntaxException));
					}
				}

				if (literalIncorrectCount > 0) {
					messages.add(formatException(
							CommandSyntaxException.BUILT_IN_EXCEPTIONS
									.dispatcherUnknownArgument()
									.createWithContext(parse.getReader())
					));
				}
			} else if (parse.getReader().canRead()) {
				hasUnknownArgument = true;
			}
		}

		x = 0;
		width = owner.width;

		if (messages.isEmpty() && !showUsages(Formatting.GRAY) && hasUnknownArgument) {
			messages.add(formatException(CommandManager.getException(parse)));
		}

		window = null;

		if (windowActive && client.options.getAutoSuggestions().getValue()) {
			show(false);
		}
	}

	private boolean showUsages(Formatting formatting) {
		CommandContextBuilder<ClientCommandSource> contextBuilder = parse.getContext();
		SuggestionContext<ClientCommandSource> suggestionContext = contextBuilder.findSuggestionContext(textField.getCursor());
		Map<CommandNode<ClientCommandSource>, String> usageMap = client
				.player
				.networkHandler
				.getCommandDispatcher()
				.getSmartUsage(suggestionContext.parent, client.player.networkHandler.getCommandSource());
		List<OrderedText> usageTexts = Lists.newArrayList();
		int maxWidth = 0;
		Style style = Style.EMPTY.withColor(formatting);

		for (Entry<CommandNode<ClientCommandSource>, String> entry : usageMap.entrySet()) {
			if (entry.getKey() instanceof LiteralCommandNode) {
				continue;
			}

			usageTexts.add(OrderedText.styledForwardsVisitedString(entry.getValue(), style));
			maxWidth = Math.max(maxWidth, textRenderer.getWidth(entry.getValue()));
		}

		if (usageTexts.isEmpty()) {
			return false;
		}

		messages.addAll(usageTexts);
		x = MathHelper.clamp(
				textField.getCharacterX(suggestionContext.startPos),
				0,
				textField.getCharacterX(0) + textField.getInnerWidth() - maxWidth
		);
		width = maxWidth;
		return true;
	}

	private @Nullable OrderedText provideRenderText(String original, int firstCharacterIndex) {
		return parse != null ? highlight(parse, original, firstCharacterIndex) : null;
	}

	static @Nullable String getSuggestionSuffix(String original, String suggestion) {
		return suggestion.startsWith(original) ? suggestion.substring(original.length()) : null;
	}

	/**
	 * Подсвечивает аргументы команды разными цветами из {@link #HIGHLIGHT_STYLES},
	 * а нераспознанные части — красным цветом ошибки.
	 */
	private static OrderedText highlight(
			ParseResults<ClientCommandSource> parse,
			String original,
			int firstCharacterIndex
	) {
		List<OrderedText> parts = Lists.newArrayList();
		int cursor = 0;
		int styleIndex = -1;
		CommandContextBuilder<ClientCommandSource> lastChild = parse.getContext().getLastChild();

		for (ParsedArgument<ClientCommandSource, ?> argument : lastChild.getArguments().values()) {
			if (++styleIndex >= HIGHLIGHT_STYLES.size()) {
				styleIndex = 0;
			}

			int argStart = Math.max(argument.getRange().getStart() - firstCharacterIndex, 0);

			if (argStart >= original.length()) {
				break;
			}

			int argEnd = Math.min(argument.getRange().getEnd() - firstCharacterIndex, original.length());

			if (argEnd > 0) {
				parts.add(OrderedText.styledForwardsVisitedString(original.substring(cursor, argStart), INFO_STYLE));
				parts.add(OrderedText.styledForwardsVisitedString(original.substring(argStart, argEnd), HIGHLIGHT_STYLES.get(styleIndex)));
				cursor = argEnd;
			}
		}

		if (parse.getReader().canRead()) {
			int errorStart = Math.max(parse.getReader().getCursor() - firstCharacterIndex, 0);

			if (errorStart < original.length()) {
				int errorEnd = Math.min(errorStart + parse.getReader().getRemainingLength(), original.length());
				parts.add(OrderedText.styledForwardsVisitedString(original.substring(cursor, errorStart), INFO_STYLE));
				parts.add(OrderedText.styledForwardsVisitedString(original.substring(errorStart, errorEnd), ERROR_STYLE));
				cursor = errorEnd;
			}
		}

		parts.add(OrderedText.styledForwardsVisitedString(original.substring(cursor), INFO_STYLE));
		return OrderedText.concat(parts);
	}

	public void render(DrawContext context, int mouseX, int mouseY) {
		if (!tryRenderWindow(context, mouseX, mouseY)) {
			renderMessages(context);
		}
	}

	public boolean tryRenderWindow(DrawContext context, int mouseX, int mouseY) {
		if (window == null) {
			return false;
		}

		window.render(context, mouseX, mouseY);
		return true;
	}

	public void renderMessages(DrawContext context) {
		for (int index = 0; index < messages.size(); index++) {
			int lineY = chatScreenSized
					? owner.height - 14 - 13 - SUGGESTION_LINE_HEIGHT * index
					: NON_CHAT_Y_OFFSET + SUGGESTION_LINE_HEIGHT * index;

			context.fill(x - 1, lineY, x + width + 1, lineY + SUGGESTION_LINE_HEIGHT, color);
			context.drawTextWithShadow(textRenderer, messages.get(index), x, lineY + 2, -1);
		}
	}

	public Text getNarration() {
		return window != null
				? ScreenTexts.LINE_BREAK.copy().append(window.getNarration())
				: ScreenTexts.EMPTY;
	}

	/**
	 * Выпадающее окно с отфильтрованным и отсортированным списком подсказок.
	 * Поддерживает прокрутку, выбор мышью и клавиатурой, а также отображение
	 * индикаторов прокрутки (пунктирные линии сверху/снизу).
	 */
	@Environment(EnvType.CLIENT)
	public class SuggestionWindow {

		private final Rect2i area;
		private final String typedText;
		private final List<Suggestion> suggestions;
		private int inWindowIndex;
		private int selection;
		private Vec2f mouse = Vec2f.ZERO;
		boolean completed;
		private int lastNarrationIndex;

		SuggestionWindow(
				final int x,
				final int y,
				final int width,
				final List<Suggestion> suggestions,
				final boolean narrateFirstSuggestion
		) {
			int areaX = x - (textField.drawsBackground() ? 0 : 1);
			int visibleCount = Math.min(suggestions.size(), maxSuggestionSize);
			int areaY = chatScreenSized
					? y - 3 - visibleCount * SUGGESTION_LINE_HEIGHT
					: y - (textField.drawsBackground() ? 1 : 0);
			area = new Rect2i(areaX, areaY, width + 1, visibleCount * SUGGESTION_LINE_HEIGHT);
			typedText = textField.getText();
			lastNarrationIndex = narrateFirstSuggestion ? -1 : 0;
			this.suggestions = suggestions;
			select(0);
		}

		public void render(DrawContext context, int mouseX, int mouseY) {
			int visibleCount = Math.min(suggestions.size(), maxSuggestionSize);
			boolean hasScrollUp = inWindowIndex > 0;
			boolean hasScrollDown = suggestions.size() > inWindowIndex + visibleCount;
			boolean hasScroll = hasScrollUp || hasScrollDown;
			boolean mouseMoved = mouse.x != mouseX || mouse.y != mouseY;

			if (mouseMoved) {
				mouse = new Vec2f(mouseX, mouseY);
			}

			if (hasScroll) {
				context.fill(area.getX(), area.getY() - 1, area.getX() + area.getWidth(), area.getY(), color);
				context.fill(
						area.getX(),
						area.getY() + area.getHeight(),
						area.getX() + area.getWidth(),
						area.getY() + area.getHeight() + 1,
						color
				);

				if (hasScrollUp) {
					for (int px = 0; px < area.getWidth(); px++) {
						if (px % 2 == 0) {
							context.fill(area.getX() + px, area.getY() - 1, area.getX() + px + 1, area.getY(), -1);
						}
					}
				}

				if (hasScrollDown) {
					for (int px = 0; px < area.getWidth(); px++) {
						if (px % 2 == 0) {
							context.fill(
									area.getX() + px,
									area.getY() + area.getHeight(),
									area.getX() + px + 1,
									area.getY() + area.getHeight() + 1,
									-1
							);
						}
					}
				}
			}

			boolean mouseOverSuggestion = false;

			for (int row = 0; row < visibleCount; row++) {
				Suggestion suggestion = suggestions.get(row + inWindowIndex);
				int rowY = area.getY() + SUGGESTION_LINE_HEIGHT * row;

				context.fill(area.getX(), rowY, area.getX() + area.getWidth(), rowY + SUGGESTION_LINE_HEIGHT, color);

				boolean mouseOverRow = mouseX > area.getX()
						&& mouseX < area.getX() + area.getWidth()
						&& mouseY > rowY
						&& mouseY < rowY + SUGGESTION_LINE_HEIGHT;

				if (mouseOverRow) {
					if (mouseMoved) {
						select(row + inWindowIndex);
					}

					mouseOverSuggestion = true;
				}

				int textColor = row + inWindowIndex == selection ? -256 : SUGGESTION_TEXT_COLOR;
				context.drawTextWithShadow(textRenderer, suggestion.getText(), area.getX() + 1, rowY + 2, textColor);
			}

			if (mouseOverSuggestion) {
				Message tooltip = suggestions.get(selection).getTooltip();

				if (tooltip != null) {
					context.drawTooltip(textRenderer, Texts.toText(tooltip), mouseX, mouseY);
				}
			}

			if (area.contains(mouseX, mouseY)) {
				context.setCursor(StandardCursors.POINTING_HAND);
			}
		}

		public boolean mouseClicked(int x, int y) {
			if (!area.contains(x, y)) {
				return false;
			}

			int clickedIndex = (y - area.getY()) / SUGGESTION_LINE_HEIGHT + inWindowIndex;

			if (clickedIndex >= 0 && clickedIndex < suggestions.size()) {
				select(clickedIndex);
				complete();
			}

			return true;
		}

		public boolean mouseScrolled(double amount) {
			int mouseX = (int) client.mouse.getScaledX(client.getWindow());
			int mouseY = (int) client.mouse.getScaledY(client.getWindow());

			if (!area.contains(mouseX, mouseY)) {
				return false;
			}

			inWindowIndex = MathHelper.clamp(
					(int) (inWindowIndex - amount),
					0,
					Math.max(suggestions.size() - maxSuggestionSize, 0)
			);
			return true;
		}

		public boolean keyPressed(KeyInput input) {
			if (input.isUp()) {
				scroll(-1);
				completed = false;
				return true;
			}

			if (input.isDown()) {
				scroll(1);
				completed = false;
				return true;
			}

			if (input.isTab()) {
				if (completed) {
					scroll(input.hasShift() ? -1 : 1);
				}

				complete();
				return true;
			}

			if (input.isEscape()) {
				clearWindow();
				textField.setSuggestion(null);
				return true;
			}

			return false;
		}

		public void scroll(int offset) {
			select(selection + offset);
			int windowStart = inWindowIndex;
			int windowEnd = inWindowIndex + maxSuggestionSize - 1;

			if (selection < windowStart) {
				inWindowIndex = MathHelper.clamp(
						selection,
						0,
						Math.max(suggestions.size() - maxSuggestionSize, 0)
				);
			} else if (selection > windowEnd) {
				inWindowIndex = MathHelper.clamp(
						selection + inWindowIndexOffset - maxSuggestionSize,
						0,
						Math.max(suggestions.size() - maxSuggestionSize, 0)
				);
			}
		}

		public void select(int index) {
			selection = index;

			if (selection < 0) {
				selection += suggestions.size();
			}

			if (selection >= suggestions.size()) {
				selection -= suggestions.size();
			}

			Suggestion suggestion = suggestions.get(selection);
			textField.setSuggestion(getSuggestionSuffix(textField.getText(), suggestion.apply(typedText)));

			if (lastNarrationIndex != selection) {
				client.getNarratorManager().narrateSystemImmediately(getNarration());
			}
		}

		public void complete() {
			Suggestion suggestion = suggestions.get(selection);
			completingSuggestions = true;
			textField.setText(suggestion.apply(typedText));
			int cursorPos = suggestion.getRange().getStart() + suggestion.getText().length();
			textField.setSelectionStart(cursorPos);
			textField.setSelectionEnd(cursorPos);
			select(selection);
			completingSuggestions = false;
			completed = true;
		}

		Text getNarration() {
			lastNarrationIndex = selection;
			Suggestion suggestion = suggestions.get(selection);
			Message tooltip = suggestion.getTooltip();
			return tooltip != null
					? Text.translatable(
							"narration.suggestion.tooltip",
							selection + 1,
							suggestions.size(),
							suggestion.getText(),
							Text.of(tooltip)
					)
					: Text.translatable(
							"narration.suggestion",
							selection + 1,
							suggestions.size(),
							suggestion.getText()
					);
		}
	}
}
