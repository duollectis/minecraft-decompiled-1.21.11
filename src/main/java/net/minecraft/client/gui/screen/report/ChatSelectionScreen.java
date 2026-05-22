package net.minecraft.client.gui.screen.report;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.gui.navigation.NavigationDirection;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.message.MessageTrustStatus;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ChatAbuseReport;
import net.minecraft.client.session.report.MessagesListAdder;
import net.minecraft.client.session.report.log.ReceivedMessage;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.Nullables;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Экран выбора сообщений чата для включения в жалобу на игрока.
 * Отображает историю чата с возможностью отметить конкретные сообщения нарушителя.
 * Поддерживает подгрузку более старых сообщений при прокрутке вверх.
 */
@Environment(EnvType.CLIENT)
public class ChatSelectionScreen extends Screen {

	static final Identifier CHECKMARK_ICON_TEXTURE = Identifier.ofVanilla("icon/checkmark");
	private static final Text TITLE_TEXT = Text.translatable("gui.chatSelection.title");
	private static final Text CONTEXT_TEXT = Text.translatable("gui.chatSelection.context");
	private static final int ENTRY_HEIGHT = 16;

	private final @Nullable Screen parent;
	private final AbuseReportContext reporter;
	private ButtonWidget doneButton;
	private MultilineText contextMessage;
	private ChatSelectionScreen.@Nullable SelectionListWidget selectionList;
	final ChatAbuseReport.Builder report;
	private final Consumer<ChatAbuseReport.Builder> newReportConsumer;
	private MessagesListAdder listAdder;

	public ChatSelectionScreen(
			@Nullable Screen parent,
			AbuseReportContext reporter,
			ChatAbuseReport.Builder report,
			Consumer<ChatAbuseReport.Builder> newReportConsumer
	) {
		super(TITLE_TEXT);
		this.parent = parent;
		this.reporter = reporter;
		this.report = report.copy();
		this.newReportConsumer = newReportConsumer;
	}

	@Override
	protected void init() {
		listAdder = new MessagesListAdder(reporter, this::isSentByReportedPlayer);
		contextMessage = MultilineText.create(textRenderer, CONTEXT_TEXT, width - ENTRY_HEIGHT);

		int contextHeight = (contextMessage.getLineCount() + 1) * 9;
		selectionList = addDrawableChild(new SelectionListWidget(client, contextHeight));

		addDrawableChild(ButtonWidget
				.builder(ScreenTexts.BACK, button -> close())
				.dimensions(width / 2 - 155, height - 32, 150, 20)
				.build());

		doneButton = addDrawableChild(ButtonWidget.builder(
				ScreenTexts.DONE, button -> {
					newReportConsumer.accept(report);
					close();
				}
		).dimensions(width / 2 - 155 + 160, height - 32, 150, 20).build());

		setDoneButtonActivation();
		addMessages();
		selectionList.setScrollY(selectionList.getMaxScrollY());
	}

	private boolean isSentByReportedPlayer(ReceivedMessage message) {
		return message.isSentFrom(report.getReportedPlayerUuid());
	}

	private void addMessages() {
		int displayCount = selectionList.getDisplayedItemCount();
		listAdder.add(displayCount, selectionList);
	}

	void addMoreMessages() {
		addMessages();
	}

	void setDoneButtonActivation() {
		doneButton.active = !report.getSelectedMessages().isEmpty();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);

		DrawnTextConsumer textConsumer = context.getTextConsumer();
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, -1);

		AbuseReportLimits limits = reporter.getSender().getLimits();
		int selectedCount = report.getSelectedMessages().size();
		int maxCount = limits.maxReportedMessageCount();
		Text selectionText = Text.translatable("gui.chatSelection.selected", selectedCount, maxCount);
		context.drawCenteredTextWithShadow(textRenderer, selectionText, width / 2, 26, -1);

		int contextY = selectionList.getContextMessageY();
		contextMessage.draw(Alignment.CENTER, width / 2, contextY, 9, textConsumer);
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	@Override
	public Text getNarratedTitle() {
		return ScreenTexts.joinSentences(super.getNarratedTitle(), CONTEXT_TEXT);
	}

	/**
	 * Виджет списка сообщений чата с поддержкой выбора и подгрузки истории.
	 * При прокрутке к началу автоматически запрашивает более старые сообщения.
	 */
	@Environment(EnvType.CLIENT)
	public class SelectionListWidget
			extends AlwaysSelectedEntryListWidget<ChatSelectionScreen.SelectionListWidget.Entry>
			implements MessagesListAdder.MessagesList {

		public static final int ENTRY_HEIGHT = 16;

		private ChatSelectionScreen.SelectionListWidget.@Nullable SenderEntryPair lastSenderEntryPair;

		public SelectionListWidget(final MinecraftClient client, final int contextMessagesHeight) {
			super(
					client,
					ChatSelectionScreen.this.width,
					ChatSelectionScreen.this.height - contextMessagesHeight - 80,
					40,
					ENTRY_HEIGHT
			);
		}

		@Override
		public void setScrollY(double scrollY) {
			double previousScrollY = getScrollY();
			super.setScrollY(scrollY);

			if (getMaxScrollY() > 1.0E-5F
					&& scrollY <= 1.0E-5F
					&& !MathHelper.approximatelyEquals(scrollY, previousScrollY)
			) {
				ChatSelectionScreen.this.addMoreMessages();
			}
		}

		@Override
		public void addMessage(int index, ReceivedMessage.ChatMessage message) {
			boolean fromReportedPlayer = message.isSentFrom(ChatSelectionScreen.this.report.getReportedPlayerUuid());
			MessageTrustStatus trustStatus = message.trustStatus();
			MessageIndicator indicator = trustStatus.createIndicator(message.message());

			Entry entry = new MessageEntry(
					index, message.getContent(), message.getNarration(), indicator, fromReportedPlayer, true
			);
			addEntryToTop(entry);
			addSenderEntry(message, fromReportedPlayer);
		}

		private void addSenderEntry(ReceivedMessage.ChatMessage message, boolean fromReportedPlayer) {
			Entry senderEntry = new SenderEntry(
					message.profile(), message.getHeadingText(), fromReportedPlayer
			);
			addEntryToTop(senderEntry);

			SenderEntryPair newPair = new SenderEntryPair(message.getSenderUuid(), senderEntry);
			if (lastSenderEntryPair != null && lastSenderEntryPair.senderEquals(newPair)) {
				removeEntryWithoutScrolling(lastSenderEntryPair.entry());
			}

			lastSenderEntryPair = newPair;
		}

		@Override
		public void addText(Text text) {
			addEntryToTop(new SeparatorEntry());
			addEntryToTop(new TextEntry(text));
			addEntryToTop(new SeparatorEntry());
			lastSenderEntryPair = null;
		}

		@Override
		public int getRowWidth() {
			return Math.min(350, width - 50);
		}

		public int getDisplayedItemCount() {
			return MathHelper.ceilDiv(height, ENTRY_HEIGHT);
		}

		protected void renderEntry(
				DrawContext drawContext,
				int mouseX,
				int mouseY,
				float deltaTicks,
				Entry entry
		) {
			if (shouldHighlight(entry)) {
				boolean isSelected = getSelectedOrNull() == entry;
				int highlightColor = isFocused() && isSelected ? -1 : -8355712;
				drawSelectionHighlight(drawContext, entry, highlightColor);
			}

			entry.render(drawContext, mouseX, mouseY, getHoveredEntry() == entry, deltaTicks);
		}

		private boolean shouldHighlight(Entry entry) {
			if (!entry.canSelect()) {
				return false;
			}

			boolean isSelected = getSelectedOrNull() == entry;
			boolean nothingSelected = getSelectedOrNull() == null;
			boolean isHovered = getHoveredEntry() == entry;

			return isSelected || (nothingSelected && isHovered && entry.isHighlightedOnHover());
		}

		protected ChatSelectionScreen.SelectionListWidget.@Nullable Entry getNeighboringEntry(
				NavigationDirection navigationDirection
		) {
			return getNeighboringEntry(navigationDirection, Entry::canSelect);
		}

		public void setSelected(ChatSelectionScreen.SelectionListWidget.@Nullable Entry entry) {
			super.setSelected(entry);
			Entry upperNeighbor = getNeighboringEntry(NavigationDirection.UP);
			if (upperNeighbor == null) {
				ChatSelectionScreen.this.addMoreMessages();
			}
		}

		@Override
		public boolean keyPressed(KeyInput input) {
			Entry selected = getSelectedOrNull();
			return selected != null && selected.keyPressed(input) ? true : super.keyPressed(input);
		}

		public int getContextMessageY() {
			return getBottom() + 9;
		}

		/**
		 * Базовый класс записи в списке выбора сообщений.
		 */
		@Environment(EnvType.CLIENT)
		public abstract static class Entry extends AlwaysSelectedEntryListWidget.Entry<ChatSelectionScreen.SelectionListWidget.Entry> {

			@Override
			public Text getNarration() {
				return ScreenTexts.EMPTY;
			}

			public boolean isSelected() {
				return false;
			}

			public boolean canSelect() {
				return false;
			}

			public boolean isHighlightedOnHover() {
				return canSelect();
			}

			@Override
			public boolean mouseClicked(Click click, boolean doubled) {
				return canSelect();
			}
		}

		/**
		 * Запись сообщения чата с поддержкой выбора, отображением галочки и индикатора доверия.
		 */
		@Environment(EnvType.CLIENT)
		public class MessageEntry extends ChatSelectionScreen.SelectionListWidget.Entry {

			private static final int CHECKMARK_WIDTH = 9;
			private static final int CHECKMARK_HEIGHT = 8;
			private static final int CHAT_MESSAGE_LEFT_MARGIN = 11;
			private static final int INDICATOR_LEFT_MARGIN = 4;

			private final int index;
			private final StringVisitable truncatedContent;
			private final Text narration;
			private final @Nullable List<OrderedText> fullContent;
			private final MessageIndicator.@Nullable Icon indicatorIcon;
			private final @Nullable List<OrderedText> originalContent;
			private final boolean fromReportedPlayer;
			private final boolean isChatMessage;

			public MessageEntry(
					final int index,
					final Text message,
					final @Nullable Text narration,
					final MessageIndicator indicator,
					final boolean fromReportedPlayer,
					final boolean isChatMessage
			) {
				this.index = index;
				this.indicatorIcon = Nullables.map(indicator, MessageIndicator::icon);
				this.originalContent = indicator != null && indicator.text() != null
						? ChatSelectionScreen.this.textRenderer.wrapLines(
								indicator.text(),
								SelectionListWidget.this.getRowWidth()
						)
						: null;
				this.fromReportedPlayer = fromReportedPlayer;
				this.isChatMessage = isChatMessage;

				int availableWidth = getTextWidth()
						- ChatSelectionScreen.this.textRenderer.getWidth(ScreenTexts.ELLIPSIS);
				StringVisitable trimmed = ChatSelectionScreen.this.textRenderer.trimToWidth(message, availableWidth);

				if (message != trimmed) {
					this.truncatedContent = StringVisitable.concat(trimmed, ScreenTexts.ELLIPSIS);
					this.fullContent = ChatSelectionScreen.this.textRenderer.wrapLines(
							message,
							SelectionListWidget.this.getRowWidth()
					);
				}
				else {
					this.truncatedContent = message;
					this.fullContent = null;
				}

				this.narration = narration;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				if (isSelected() && fromReportedPlayer) {
					drawCheckmark(context, getContentY(), getContentX(), getContentHeight());
				}

				int textX = getContentX() + getIndent();
				int textY = getContentY() + 1 + (getContentHeight() - 9) / 2;
				context.drawTextWithShadow(
						ChatSelectionScreen.this.textRenderer,
						Language.getInstance().reorder(truncatedContent),
						textX,
						textY,
						fromReportedPlayer ? -1 : -1593835521
				);

				if (fullContent != null && hovered) {
					context.drawTooltip(fullContent, mouseX, mouseY);
				}

				int textWidth = ChatSelectionScreen.this.textRenderer.getWidth(truncatedContent);
				renderIndicator(context, textX + textWidth + INDICATOR_LEFT_MARGIN, getContentY(), getContentHeight(), mouseX, mouseY);
			}

			private void renderIndicator(DrawContext context, int x, int y, int entryHeight, int mouseX, int mouseY) {
				if (indicatorIcon == null) {
					return;
				}

				int iconY = y + (entryHeight - indicatorIcon.height) / 2;
				indicatorIcon.draw(context, x, iconY);

				if (originalContent != null
						&& mouseX >= x
						&& mouseX <= x + indicatorIcon.width
						&& mouseY >= iconY
						&& mouseY <= iconY + indicatorIcon.height
				) {
					context.drawTooltip(originalContent, mouseX, mouseY);
				}
			}

			private void drawCheckmark(DrawContext context, int y, int x, int entryHeight) {
				int checkY = y + (entryHeight - CHECKMARK_HEIGHT) / 2;
				context.drawGuiTexture(
						RenderPipelines.GUI_TEXTURED,
						ChatSelectionScreen.CHECKMARK_ICON_TEXTURE,
						x,
						checkY,
						CHECKMARK_WIDTH,
						CHECKMARK_HEIGHT
				);
			}

			private int getTextWidth() {
				int indicatorWidth = indicatorIcon != null ? indicatorIcon.width + INDICATOR_LEFT_MARGIN : 0;
				return SelectionListWidget.this.getRowWidth() - getIndent() - INDICATOR_LEFT_MARGIN - indicatorWidth;
			}

			private int getIndent() {
				return isChatMessage ? CHAT_MESSAGE_LEFT_MARGIN : 0;
			}

			@Override
			public Text getNarration() {
				return isSelected()
						? Text.translatable("narrator.select", narration)
						: narration;
			}

			@Override
			public boolean mouseClicked(Click click, boolean doubled) {
				SelectionListWidget.this.setSelected(null);
				return toggle();
			}

			@Override
			public boolean keyPressed(KeyInput input) {
				return input.isEnterOrSpace() ? toggle() : false;
			}

			@Override
			public boolean isSelected() {
				return ChatSelectionScreen.this.report.isMessageSelected(index);
			}

			@Override
			public boolean canSelect() {
				return true;
			}

			@Override
			public boolean isHighlightedOnHover() {
				return fromReportedPlayer;
			}

			private boolean toggle() {
				if (!fromReportedPlayer) {
					return false;
				}

				ChatSelectionScreen.this.report.toggleMessageSelection(index);
				ChatSelectionScreen.this.setDoneButtonActivation();
				return true;
			}
		}

		/**
		 * Запись отправителя сообщения с аватаром скина и именем.
		 */
		@Environment(EnvType.CLIENT)
		public class SenderEntry extends ChatSelectionScreen.SelectionListWidget.Entry {

			private static final int PLAYER_SKIN_SIZE = 12;
			private static final int ICON_PADDING = 4;

			private final Text headingText;
			private final Supplier<SkinTextures> skinTexturesSupplier;
			private final boolean fromReportedPlayer;

			public SenderEntry(
					final GameProfile gameProfile,
					final Text headingText,
					final boolean fromReportedPlayer
			) {
				this.headingText = headingText;
				this.fromReportedPlayer = fromReportedPlayer;
				this.skinTexturesSupplier =
						SelectionListWidget.this.client.getSkinProvider().supplySkinTextures(gameProfile, true);
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				int skinX = getContentX() - PLAYER_SKIN_SIZE + ICON_PADDING;
				int skinY = getContentY() + (getContentHeight() - PLAYER_SKIN_SIZE) / 2;
				PlayerSkinDrawer.draw(context, skinTexturesSupplier.get(), skinX, skinY, PLAYER_SKIN_SIZE);

				int textY = getContentY() + 1 + (getContentHeight() - 9) / 2;
				context.drawTextWithShadow(
						ChatSelectionScreen.this.textRenderer,
						headingText,
						skinX + PLAYER_SKIN_SIZE + ICON_PADDING,
						textY,
						fromReportedPlayer ? -1 : -1593835521
				);
			}
		}

		@Environment(EnvType.CLIENT)
		record SenderEntryPair(UUID sender, ChatSelectionScreen.SelectionListWidget.Entry entry) {

			public boolean senderEquals(ChatSelectionScreen.SelectionListWidget.SenderEntryPair pair) {
				return pair.sender.equals(sender);
			}
		}

		@Environment(EnvType.CLIENT)
		public static class SeparatorEntry extends ChatSelectionScreen.SelectionListWidget.Entry {

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
			}
		}

		@Environment(EnvType.CLIENT)
		public class TextEntry extends ChatSelectionScreen.SelectionListWidget.Entry {

			private final Text text;

			public TextEntry(final Text text) {
				this.text = text;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				int centerY = getContentMiddleY();
				int rightBound = getContentRightEnd() - 8;
				int textWidth = ChatSelectionScreen.this.textRenderer.getWidth(text);
				int textX = (getContentX() + rightBound - textWidth) / 2;
				int textY = centerY - 9 / 2;
				context.drawTextWithShadow(ChatSelectionScreen.this.textRenderer, text, textX, textY, -6250336);
			}

			@Override
			public Text getNarration() {
				return text;
			}
		}
	}
}
