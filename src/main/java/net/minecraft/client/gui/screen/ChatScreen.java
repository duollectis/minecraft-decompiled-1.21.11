package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * Экран чата. Обрабатывает ввод сообщений, историю чата, автодополнение команд и черновики.
 */
@Environment(EnvType.CLIENT)
public class ChatScreen extends Screen {

	public static final double SHIFT_SCROLL_AMOUNT = 7.0;
	private static final int KEY_BACKSPACE = 259;
	private static final int KEY_DOWN = 264;
	private static final int KEY_UP = 265;
	private static final int KEY_PAGE_UP = 266;
	private static final int KEY_PAGE_DOWN = 267;
	private static final int CHAT_FIELD_HEIGHT = 12;
	private static final int CHAT_FIELD_MARGIN = 4;
	private static final int CHAT_FIELD_BOTTOM_OFFSET = 12;
	private static final int SUGGESTION_LINES = 10;
	private static final int SUGGESTION_BACKGROUND_COLOR = -805306368;
	private static final Text USAGE_TEXT = Text.translatable("chat_screen.usage");

	private String chatLastMessage = "";
	private int messageHistoryIndex = -1;
	protected TextFieldWidget chatField;
	protected String originalChatText;
	protected boolean draft;
	protected CloseReason closeReason = CloseReason.INTERRUPTED;
	ChatInputSuggestor chatInputSuggestor;

	public ChatScreen(String text, boolean draft) {
		super(Text.translatable("chat_screen.title"));
		this.originalChatText = text;
		this.draft = draft;
	}

	@Override
	protected void init() {
		messageHistoryIndex = client.inGameHud.getChatHud().getMessageHistory().size();
		chatField = new TextFieldWidget(
			client.advanceValidatingTextRenderer,
			CHAT_FIELD_MARGIN,
			height - CHAT_FIELD_BOTTOM_OFFSET,
			width - CHAT_FIELD_MARGIN,
			CHAT_FIELD_HEIGHT,
			Text.translatable("chat.editBox")
		) {
			@Override
			protected MutableText getNarrationMessage() {
				return super.getNarrationMessage().append(chatInputSuggestor.getNarration());
			}
		};
		chatField.setMaxLength(256);
		chatField.setDrawsBackground(false);
		chatField.setText(originalChatText);
		chatField.setChangedListener(this::onChatFieldUpdate);
		chatField.addFormatter(this::format);
		chatField.setFocusUnlocked(false);
		addDrawableChild(chatField);
		chatInputSuggestor = new ChatInputSuggestor(
			client,
			this,
			chatField,
			textRenderer,
			false,
			false,
			1,
			SUGGESTION_LINES,
			true,
			SUGGESTION_BACKGROUND_COLOR
		);
		chatInputSuggestor.setCanLeave(false);
		chatInputSuggestor.setWindowActive(false);
		chatInputSuggestor.refresh();
	}

	@Override
	protected void setInitialFocus() {
		setInitialFocus(chatField);
	}

	@Override
	public void resize(int width, int height) {
		originalChatText = chatField.getText();
		init(width, height);
	}

	@Override
	public void close() {
		closeReason = CloseReason.INTENTIONAL;
		super.close();
	}

	@Override
	public void removed() {
		client.inGameHud.getChatHud().resetScroll();
		originalChatText = chatField.getText();
		if (shouldNotSaveDraft() || StringUtils.isBlank(originalChatText)) {
			client.inGameHud.getChatHud().discardDraft();
		} else if (!draft) {
			client.inGameHud.getChatHud().saveDraft(originalChatText);
		}
	}

	protected boolean shouldNotSaveDraft() {
		return closeReason != CloseReason.INTERRUPTED
			&& (closeReason != CloseReason.INTENTIONAL || !client.options.getChatDrafts().getValue());
	}

	private void onChatFieldUpdate(String chatText) {
		chatInputSuggestor.setWindowActive(true);
		chatInputSuggestor.refresh();
		draft = false;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (chatInputSuggestor.keyPressed(input)) {
			return true;
		}

		if (draft && input.key() == KEY_BACKSPACE) {
			chatField.setText("");
			draft = false;
			return true;
		}

		if (super.keyPressed(input)) {
			return true;
		}

		if (input.isEnter()) {
			sendMessage(chatField.getText(), true);
			closeReason = CloseReason.DONE;
			client.setScreen(null);
			return true;
		}

		ChatHud chatHud = client.inGameHud.getChatHud();
		switch (input.key()) {
			case KEY_DOWN -> setChatFromHistory(1);
			case KEY_UP -> setChatFromHistory(-1);
			case KEY_PAGE_UP -> chatHud.scroll(chatHud.getVisibleLineCount() - 1);
			case KEY_PAGE_DOWN -> chatHud.scroll(-chatHud.getVisibleLineCount() + 1);
			default -> { return false; }
		}

		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		verticalAmount = MathHelper.clamp(verticalAmount, -1.0, 1.0);
		if (chatInputSuggestor.mouseScrolled(verticalAmount)) {
			return true;
		}

		if (!client.isShiftPressed()) {
			verticalAmount *= SHIFT_SCROLL_AMOUNT;
		}

		client.inGameHud.getChatHud().scroll((int) verticalAmount);
		return true;
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (chatInputSuggestor.mouseClicked(click)) {
			return true;
		}

		if (click.button() == 0) {
			int scaledHeight = client.getWindow().getScaledHeight();
			boolean inserting = shouldInsert();
			DrawnTextConsumer.ClickHandler clickHandler =
				new DrawnTextConsumer.ClickHandler(getTextRenderer(), (int) click.x(), (int) click.y())
					.insert(inserting);
			client.inGameHud.getChatHud().render(clickHandler, scaledHeight, client.inGameHud.getTicks(), true);
			Style style = clickHandler.getStyle();
			if (style != null && handleClickEvent(style, inserting)) {
				originalChatText = chatField.getText();
				return true;
			}
		}

		return super.mouseClicked(click, doubled);
	}

	private boolean shouldInsert() {
		return client.isShiftPressed();
	}

	private boolean handleClickEvent(Style style, boolean insert) {
		ClickEvent clickEvent = style.getClickEvent();
		if (insert) {
			if (style.getInsertion() != null) {
				insertText(style.getInsertion(), false);
			}

			return false;
		}

		if (clickEvent == null) {
			return false;
		}

		if (clickEvent instanceof ClickEvent.Custom custom && custom.id().equals(ChatHud.EXPAND_CHAT_QUEUE_ID)) {
			MessageHandler messageHandler = client.getMessageHandler();
			if (messageHandler.getUnprocessedMessageCount() != 0L) {
				messageHandler.process();
			}
		} else {
			handleClickEvent(clickEvent, client, this);
		}

		return true;
	}

	@Override
	public void insertText(String text, boolean override) {
		if (override) {
			chatField.setText(text);
		} else {
			chatField.write(text);
		}
	}

	public void setChatFromHistory(int offset) {
		int newIndex = messageHistoryIndex + offset;
		int historySize = client.inGameHud.getChatHud().getMessageHistory().size();
		newIndex = MathHelper.clamp(newIndex, 0, historySize);

		if (newIndex == messageHistoryIndex) {
			return;
		}

		if (newIndex == historySize) {
			messageHistoryIndex = historySize;
			chatField.setText(chatLastMessage);
			return;
		}

		if (messageHistoryIndex == historySize) {
			chatLastMessage = chatField.getText();
		}

		chatField.setText(client.inGameHud.getChatHud().getMessageHistory().get(newIndex));
		chatInputSuggestor.setWindowActive(false);
		messageHistoryIndex = newIndex;
	}

	private @Nullable OrderedText format(String text, int firstCharacterIndex) {
		return draft
			? OrderedText.styledForwardsVisitedString(text, Style.EMPTY.withColor(Formatting.GRAY).withItalic(true))
			: null;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.fill(2, height - 14, width - 2, height - 2, client.options.getTextBackgroundColor(Integer.MIN_VALUE));
		client.inGameHud
			.getChatHud()
			.render(context, textRenderer, client.inGameHud.getTicks(), mouseX, mouseY, true, shouldInsert());
		super.render(context, mouseX, mouseY, deltaTicks);
		chatInputSuggestor.render(context, mouseX, mouseY);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean keepOpenThroughPortal() {
		return true;
	}

	@Override
	protected void addScreenNarrations(NarrationMessageBuilder messageBuilder) {
		messageBuilder.put(NarrationPart.TITLE, getTitle());
		messageBuilder.put(NarrationPart.USAGE, USAGE_TEXT);
		String text = chatField.getText();
		if (!text.isEmpty()) {
			messageBuilder.nextMessage().put(NarrationPart.TITLE, Text.translatable("chat_screen.message", text));
		}
	}

	public void sendMessage(String chatText, boolean addToHistory) {
		chatText = normalize(chatText);
		if (chatText.isEmpty()) {
			return;
		}

		if (addToHistory) {
			client.inGameHud.getChatHud().addToMessageHistory(chatText);
		}

		if (chatText.startsWith("/")) {
			client.player.networkHandler.sendChatCommand(chatText.substring(1));
		} else {
			client.player.networkHandler.sendChatMessage(chatText);
		}
	}

	public String normalize(String chatText) {
		return StringHelper.truncateChat(StringUtils.normalizeSpace(chatText.trim()));
	}

	@Environment(EnvType.CLIENT)
	protected enum CloseReason {
		INTENTIONAL,
		INTERRUPTED,
		DONE;
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	public interface Factory<T extends ChatScreen> {

		T create(String string, boolean draft);
	}
}
