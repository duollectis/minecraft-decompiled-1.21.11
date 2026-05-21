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

@Environment(EnvType.CLIENT)
/**
 * {@code ChatScreen}.
 */
public class ChatScreen extends Screen {

	public static final double SHIFT_SCROLL_AMOUNT = 7.0;
	private static final Text USAGE_TEXT = Text.translatable("chat_screen.usage");
	private String chatLastMessage = "";
	private int messageHistoryIndex = -1;
	protected TextFieldWidget chatField;
	protected String originalChatText;
	protected boolean draft;
	protected ChatScreen.CloseReason closeReason = ChatScreen.CloseReason.INTERRUPTED;
	ChatInputSuggestor chatInputSuggestor;

	public ChatScreen(String text, boolean draft) {
		super(Text.translatable("chat_screen.title"));
		this.originalChatText = text;
		this.draft = draft;
	}

	@Override
	protected void init() {
		this.messageHistoryIndex = this.client.inGameHud.getChatHud().getMessageHistory().size();
		this.chatField = new TextFieldWidget(
				this.client.advanceValidatingTextRenderer,
				4,
				this.height - 12,
				this.width - 4,
				12,
				Text.translatable("chat.editBox")
		) {
			@Override
			protected MutableText getNarrationMessage() {
				return super.getNarrationMessage().append(ChatScreen.this.chatInputSuggestor.getNarration());
			}
		};
		this.chatField.setMaxLength(256);
		this.chatField.setDrawsBackground(false);
		this.chatField.setText(this.originalChatText);
		this.chatField.setChangedListener(this::onChatFieldUpdate);
		this.chatField.addFormatter(this::format);
		this.chatField.setFocusUnlocked(false);
		this.addDrawableChild(this.chatField);
		this.chatInputSuggestor =
				new ChatInputSuggestor(
						this.client,
						this,
						this.chatField,
						this.textRenderer,
						false,
						false,
						1,
						10,
						true,
						-805306368
				);
		this.chatInputSuggestor.setCanLeave(false);
		this.chatInputSuggestor.setWindowActive(false);
		this.chatInputSuggestor.refresh();
	}

	@Override
	protected void setInitialFocus() {
		this.setInitialFocus(this.chatField);
	}

	@Override
	public void resize(int width, int height) {
		this.originalChatText = this.chatField.getText();
		this.init(width, height);
	}

	@Override
	public void close() {
		this.closeReason = ChatScreen.CloseReason.INTENTIONAL;
		super.close();
	}

	@Override
	public void removed() {
		this.client.inGameHud.getChatHud().resetScroll();
		this.originalChatText = this.chatField.getText();
		if (this.shouldNotSaveDraft() || StringUtils.isBlank(this.originalChatText)) {
			this.client.inGameHud.getChatHud().discardDraft();
		}
		else if (!this.draft) {
			this.client.inGameHud.getChatHud().saveDraft(this.originalChatText);
		}
	}

	/**
	 * Определяет, следует ли not save draft.
	 *
	 * @return boolean — результат операции
	 */
	protected boolean shouldNotSaveDraft() {
		return this.closeReason != ChatScreen.CloseReason.INTERRUPTED
				&& (this.closeReason != ChatScreen.CloseReason.INTENTIONAL || !this.client.options
				.getChatDrafts()
				.getValue()
		);
	}

	private void onChatFieldUpdate(String chatText) {
		this.chatInputSuggestor.setWindowActive(true);
		this.chatInputSuggestor.refresh();
		this.draft = false;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.chatInputSuggestor.keyPressed(input)) {
			return true;
		}
		else if (this.draft && input.key() == 259) {
			this.chatField.setText("");
			this.draft = false;
			return true;
		}
		else if (super.keyPressed(input)) {
			return true;
		}
		else if (input.isEnter()) {
			this.sendMessage(this.chatField.getText(), true);
			this.closeReason = ChatScreen.CloseReason.DONE;
			this.client.setScreen(null);
			return true;
		}
		else {
			switch (input.key()) {
				case 264:
					this.setChatFromHistory(1);
					break;
				case 265:
					this.setChatFromHistory(-1);
					break;
				case 266:
					this.client.inGameHud
							.getChatHud()
							.scroll(this.client.inGameHud.getChatHud().getVisibleLineCount() - 1);
					break;
				case 267:
					this.client.inGameHud
							.getChatHud()
							.scroll(-this.client.inGameHud.getChatHud().getVisibleLineCount() + 1);
					break;
				default:
					return false;
			}

			return true;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		verticalAmount = MathHelper.clamp(verticalAmount, -1.0, 1.0);
		if (this.chatInputSuggestor.mouseScrolled(verticalAmount)) {
			return true;
		}
		else {
			if (!this.client.isShiftPressed()) {
				verticalAmount *= 7.0;
			}

			this.client.inGameHud.getChatHud().scroll((int) verticalAmount);
			return true;
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (this.chatInputSuggestor.mouseClicked(click)) {
			return true;
		}
		else {
			if (click.button() == 0) {
				int i = this.client.getWindow().getScaledHeight();
				DrawnTextConsumer.ClickHandler
						clickHandler =
						new DrawnTextConsumer.ClickHandler(this.getTextRenderer(), (int) click.x(), (int) click.y())
								.insert(this.shouldInsert());
				this.client.inGameHud.getChatHud().render(clickHandler, i, this.client.inGameHud.getTicks(), true);
				Style style = clickHandler.getStyle();
				if (style != null && this.handleClickEvent(style, this.shouldInsert())) {
					this.originalChatText = this.chatField.getText();
					return true;
				}
			}

			return super.mouseClicked(click, doubled);
		}
	}

	private boolean shouldInsert() {
		return this.client.isShiftPressed();
	}

	private boolean handleClickEvent(Style style, boolean insert) {
		ClickEvent clickEvent = style.getClickEvent();
		if (insert) {
			if (style.getInsertion() != null) {
				this.insertText(style.getInsertion(), false);
			}
		}
		else if (clickEvent != null) {
			if (clickEvent instanceof ClickEvent.Custom custom && custom.id().equals(ChatHud.EXPAND_CHAT_QUEUE_ID)) {
				MessageHandler messageHandler = this.client.getMessageHandler();
				if (messageHandler.getUnprocessedMessageCount() != 0L) {
					messageHandler.process();
				}
			}
			else {
				handleClickEvent(clickEvent, this.client, this);
			}

			return true;
		}

		return false;
	}

	@Override
	public void insertText(String text, boolean override) {
		if (override) {
			this.chatField.setText(text);
		}
		else {
			this.chatField.write(text);
		}
	}

	public void setChatFromHistory(int offset) {
		int i = this.messageHistoryIndex + offset;
		int j = this.client.inGameHud.getChatHud().getMessageHistory().size();
		i = MathHelper.clamp(i, 0, j);
		if (i != this.messageHistoryIndex) {
			if (i == j) {
				this.messageHistoryIndex = j;
				this.chatField.setText(this.chatLastMessage);
			}
			else {
				if (this.messageHistoryIndex == j) {
					this.chatLastMessage = this.chatField.getText();
				}

				this.chatField.setText(this.client.inGameHud.getChatHud().getMessageHistory().get(i));
				this.chatInputSuggestor.setWindowActive(false);
				this.messageHistoryIndex = i;
			}
		}
	}

	private @Nullable OrderedText format(String string, int firstCharacterIndex) {
		return this.draft ? OrderedText.styledForwardsVisitedString(
				string,
				Style.EMPTY.withColor(Formatting.GRAY).withItalic(true)
		) : null;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		context.fill(
				2,
				this.height - 14,
				this.width - 2,
				this.height - 2,
				this.client.options.getTextBackgroundColor(Integer.MIN_VALUE)
		);
		this.client.inGameHud
				.getChatHud()
				.render(
						context,
						this.textRenderer,
						this.client.inGameHud.getTicks(),
						mouseX,
						mouseY,
						true,
						this.shouldInsert()
				);
		super.render(context, mouseX, mouseY, deltaTicks);
		this.chatInputSuggestor.render(context, mouseX, mouseY);
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
		messageBuilder.put(NarrationPart.TITLE, this.getTitle());
		messageBuilder.put(NarrationPart.USAGE, USAGE_TEXT);
		String string = this.chatField.getText();
		if (!string.isEmpty()) {
			messageBuilder.nextMessage().put(NarrationPart.TITLE, Text.translatable("chat_screen.message", string));
		}
	}

	/**
	 * Отправляет message.
	 *
	 * @param chatText chat text
	 * @param addToHistory add to history
	 */
	public void sendMessage(String chatText, boolean addToHistory) {
		chatText = this.normalize(chatText);
		if (!chatText.isEmpty()) {
			if (addToHistory) {
				this.client.inGameHud.getChatHud().addToMessageHistory(chatText);
			}

			if (chatText.startsWith("/")) {
				this.client.player.networkHandler.sendChatCommand(chatText.substring(1));
			}
			else {
				this.client.player.networkHandler.sendChatMessage(chatText);
			}
		}
	}

	/**
	 * Normalize.
	 *
	 * @param chatText chat text
	 *
	 * @return String — результат операции
	 */
	public String normalize(String chatText) {
		return StringHelper.truncateChat(StringUtils.normalizeSpace(chatText.trim()));
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code CloseReason}.
	 */
	protected static enum CloseReason {
		INTENTIONAL,
		INTERRUPTED,
		DONE;
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code Factory}.
	 */
	public interface Factory<T extends ChatScreen> {

		T create(String string, boolean draft);
	}
}
