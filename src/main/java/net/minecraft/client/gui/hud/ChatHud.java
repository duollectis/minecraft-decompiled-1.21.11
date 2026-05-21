package net.minecraft.client.gui.hud;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.Alignment;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.cursor.StandardCursors;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nullables;
import net.minecraft.util.collection.ArrayListDeque;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
/**
 * {@code ChatHud}.
 */
public class ChatHud {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_MESSAGES = 100;
	private static final int PADDING = 4;
	private static final int OFFSET_FROM_BOTTOM = 40;
	private static final int MAX_WIDTH = 210;
	private static final int REMOVAL_QUEUE_TICKS = 60;
	private static final Text
			DELETED_MARKER_TEXT =
			Text.translatable("chat.deleted_marker").formatted(Formatting.GRAY, Formatting.ITALIC);
	public static final int LINE_HEIGHT = 8;
	public static final Identifier EXPAND_CHAT_QUEUE_ID = Identifier.ofVanilla("internal/expand_chat_queue");
	private static final Style CHAT_QUEUE_STYLE = Style.EMPTY
			.withClickEvent(new ClickEvent.Custom(EXPAND_CHAT_QUEUE_ID, Optional.empty()))
			.withHoverEvent(new HoverEvent.ShowText(Text.translatable("chat.queue.tooltip")));
	final MinecraftClient client;
	private final ArrayListDeque<String> messageHistory = new ArrayListDeque<>(100);
	private final List<ChatHudLine> messages = Lists.newArrayList();
	private final List<ChatHudLine.Visible> visibleMessages = Lists.newArrayList();
	private int scrolledLines;
	private boolean hasUnreadNewMessages;
	private ChatHud.@Nullable Draft draft;
	private @Nullable ChatScreen screen;
	private final List<ChatHud.RemovalQueuedMessage> removalQueue = new ArrayList<>();

	public ChatHud(MinecraftClient client) {
		this.client = client;
		this.messageHistory.addAll(client.getCommandHistoryManager().getHistory());
	}

	/**
	 * Выполняет тик обновления для removal queue if exists.
	 */
	public void tickRemovalQueueIfExists() {
		if (!this.removalQueue.isEmpty()) {
			this.tickRemovalQueue();
		}
	}

	private int forEachVisibleLine(ChatHud.OpacityRule opacityRule, ChatHud.LineConsumer lineConsumer) {
		int i = this.getVisibleLineCount();
		int j = 0;

		for (int k = Math.min(this.visibleMessages.size() - this.scrolledLines, i) - 1; k >= 0; k--) {
			int l = k + this.scrolledLines;
			ChatHudLine.Visible visible = this.visibleMessages.get(l);
			float f = opacityRule.calculate(visible);
			if (f > 1.0E-5F) {
				j++;
				lineConsumer.accept(visible, k, f);
			}
		}

		return j;
	}

	public void render(
			DrawContext context,
			TextRenderer textRenderer,
			int currentTick,
			int mouseX,
			int mouseY,
			boolean interactable,
			boolean bl
	) {
		context.getMatrices().pushMatrix();
		this.render(
				(ChatHud.Backend) (interactable ? new ChatHud.Interactable(context, textRenderer, mouseX, mouseY, bl)
				                                : new ChatHud.Hud(context)
				),
				context.getScaledWindowHeight(),
				currentTick,
				interactable
		);
		context.getMatrices().popMatrix();
	}

	/**
	 * Render.
	 *
	 * @param textConsumer text consumer
	 * @param windowHeight window height
	 * @param currentTick current tick
	 * @param expanded expanded
	 */
	public void render(DrawnTextConsumer textConsumer, int windowHeight, int currentTick, boolean expanded) {
		this.render(new ChatHud.Forwarder(textConsumer), windowHeight, currentTick, expanded);
	}

	private void render(ChatHud.Backend drawer, int windowHeight, int currentTick, boolean expanded) {
		if (!this.isChatHidden()) {
			int i = this.visibleMessages.size();
			if (i > 0) {
				Profiler profiler = Profilers.get();
				profiler.push("chat");
				float f = (float) this.getChatScale();
				int j = MathHelper.ceil(this.getWidth() / f);
				final int k = MathHelper.floor((windowHeight - 40) / f);
				final float g = this.client.options.getChatOpacity().getValue().floatValue() * 0.9F + 0.1F;
				float h = this.client.options.getTextBackgroundOpacity().getValue().floatValue();
				final int l = 9;
				int m = 8;
				double d = this.client.options.getChatLineSpacing().getValue();
				final int n = (int) (l * (d + 1.0));
				final int o = (int) Math.round(8.0 * (d + 1.0) - 4.0 * d);
				long p = this.client.getMessageHandler().getUnprocessedMessageCount();
				ChatHud.OpacityRule
						opacityRule =
						expanded ? ChatHud.OpacityRule.CONSTANT : ChatHud.OpacityRule.timeBased(currentTick);
				drawer.updatePose(pose -> {
					pose.scale(f, f);
					pose.translate(4.0F, 0.0F);
				});
				this.forEachVisibleLine(
						opacityRule, (line, y, opacity) -> {
							int lx = k - y * n;
							int mx = lx - n;
							drawer.fill(-4, mx, j + 4 + 4, lx, ColorHelper.toAlpha(opacity * h));
						}
				);
				if (p > 0L) {
					drawer.fill(-2, k, j + 4, k + l, ColorHelper.toAlpha(h));
				}

				int q = this.forEachVisibleLine(
						opacityRule, new ChatHud.LineConsumer() {
							boolean styledCurrentLine;

							@Override
							public void accept(ChatHudLine.Visible visible, int ix, float fx) {
								int jx = k - ix * n;
								int kx = jx - n;
								int lx = jx - o;
								boolean bl = drawer.text(lx, fx * g, visible.content());
								this.styledCurrentLine |= bl;
								boolean bl2;
								if (visible.endOfEntry()) {
									bl2 = this.styledCurrentLine;
									this.styledCurrentLine = false;
								}
								else {
									bl2 = false;
								}

								MessageIndicator messageIndicator = visible.indicator();
								if (messageIndicator != null) {
									drawer.indicator(-4, kx, -2, jx, fx * g, messageIndicator);
									if (messageIndicator.icon() != null) {
										int mx = visible.getWidth(ChatHud.this.client.textRenderer);
										int nx = lx + l;
										drawer.indicatorIcon(mx, nx, bl2, messageIndicator, messageIndicator.icon());
									}
								}
							}
						}
				);
				if (p > 0L) {
					int r = k + l;
					Text text = Text.translatable("chat.queue", p).setStyle(CHAT_QUEUE_STYLE);
					drawer.text(r - 8, 0.5F * g, text.asOrderedText());
				}

				if (expanded) {
					int r = i * n;
					int s = q * n;
					int t = this.scrolledLines * s / i - k;
					int u = s * s / r;
					if (r != s) {
						int v = t > 0 ? 170 : 96;
						int w = this.hasUnreadNewMessages ? 13382451 : 3355562;
						int x = j + 4;
						drawer.fill(x, -t, x + 2, -t - u, ColorHelper.withAlpha(v, w));
						drawer.fill(x + 2, -t, x + 1, -t - u, ColorHelper.withAlpha(v, 13421772));
					}
				}

				profiler.pop();
			}
		}
	}

	private boolean isChatHidden() {
		return this.client.options.getChatVisibility().getValue() == ChatVisibility.HIDDEN;
	}

	/**
	 * Clear.
	 *
	 * @param clearHistory clear history
	 */
	public void clear(boolean clearHistory) {
		this.client.getMessageHandler().processAll();
		this.removalQueue.clear();
		this.visibleMessages.clear();
		this.messages.clear();
		if (clearHistory) {
			this.messageHistory.clear();
			this.messageHistory.addAll(this.client.getCommandHistoryManager().getHistory());
		}
	}

	/**
	 * Добавляет message.
	 *
	 * @param message message
	 */
	public void addMessage(Text message) {
		this.addMessage(
				message,
				null,
				this.client.isConnectedToLocalServer() ? MessageIndicator.singlePlayer() : MessageIndicator.system()
		);
	}

	public void addMessage(
			Text message,
			@Nullable MessageSignatureData signatureData,
			@Nullable MessageIndicator indicator
	) {
		ChatHudLine chatHudLine = new ChatHudLine(this.client.inGameHud.getTicks(), message, signatureData, indicator);
		this.logChatMessage(chatHudLine);
		this.addVisibleMessage(chatHudLine);
		this.addMessage(chatHudLine);
	}

	private void logChatMessage(ChatHudLine message) {
		String string = message.content().getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
		String string2 = Nullables.map(message.indicator(), MessageIndicator::loggedName);
		if (string2 != null) {
			LOGGER.info("[{}] [CHAT] {}", string2, string);
		}
		else {
			LOGGER.info("[CHAT] {}", string);
		}
	}

	private void addVisibleMessage(ChatHudLine message) {
		int i = MathHelper.floor(this.getWidth() / this.getChatScale());
		List<OrderedText> list = message.breakLines(this.client.textRenderer, i);
		boolean bl = this.isChatFocused();

		for (int j = 0; j < list.size(); j++) {
			OrderedText orderedText = list.get(j);
			if (bl && this.scrolledLines > 0) {
				this.hasUnreadNewMessages = true;
				this.scroll(1);
			}

			boolean bl2 = j == list.size() - 1;
			this.visibleMessages.addFirst(new ChatHudLine.Visible(
					message.creationTick(),
					orderedText,
					message.indicator(),
					bl2
			));
		}

		while (this.visibleMessages.size() > 100) {
			this.visibleMessages.removeLast();
		}
	}

	private void addMessage(ChatHudLine message) {
		this.messages.addFirst(message);

		while (this.messages.size() > 100) {
			this.messages.removeLast();
		}
	}

	private void tickRemovalQueue() {
		int i = this.client.inGameHud.getTicks();
		this.removalQueue.removeIf(message -> i >= message.deletableAfter() ? this.queueForRemoval(message.signature())
		                                                                      == null : false);
	}

	/**
	 * Удаляет message.
	 *
	 * @param signature signature
	 */
	public void removeMessage(MessageSignatureData signature) {
		ChatHud.RemovalQueuedMessage removalQueuedMessage = this.queueForRemoval(signature);
		if (removalQueuedMessage != null) {
			this.removalQueue.add(removalQueuedMessage);
		}
	}

	private ChatHud.@Nullable RemovalQueuedMessage queueForRemoval(MessageSignatureData signature) {
		int i = this.client.inGameHud.getTicks();
		ListIterator<ChatHudLine> listIterator = this.messages.listIterator();

		while (listIterator.hasNext()) {
			ChatHudLine chatHudLine = listIterator.next();
			if (signature.equals(chatHudLine.signature())) {
				int j = chatHudLine.creationTick() + 60;
				if (i >= j) {
					listIterator.set(this.createRemovalMarker(chatHudLine));
					this.refresh();
					return null;
				}

				return new ChatHud.RemovalQueuedMessage(signature, j);
			}
		}

		return null;
	}

	private ChatHudLine createRemovalMarker(ChatHudLine original) {
		return new ChatHudLine(original.creationTick(), DELETED_MARKER_TEXT, null, MessageIndicator.system());
	}

	/**
	 * Reset.
	 */
	public void reset() {
		this.resetScroll();
		this.refresh();
	}

	private void refresh() {
		this.visibleMessages.clear();

		for (ChatHudLine chatHudLine : Lists.reverse(this.messages)) {
			this.addVisibleMessage(chatHudLine);
		}
	}

	public ArrayListDeque<String> getMessageHistory() {
		return this.messageHistory;
	}

	/**
	 * Добавляет to message history.
	 *
	 * @param message message
	 */
	public void addToMessageHistory(String message) {
		if (!message.equals(this.messageHistory.peekLast())) {
			if (this.messageHistory.size() >= 100) {
				this.messageHistory.removeFirst();
			}

			this.messageHistory.addLast(message);
		}

		if (message.startsWith("/")) {
			this.client.getCommandHistoryManager().add(message);
		}
	}

	/**
	 * Сбрасывает scroll.
	 */
	public void resetScroll() {
		this.scrolledLines = 0;
		this.hasUnreadNewMessages = false;
	}

	/**
	 * Scroll.
	 *
	 * @param scroll scroll
	 */
	public void scroll(int scroll) {
		this.scrolledLines += scroll;
		int i = this.visibleMessages.size();
		if (this.scrolledLines > i - this.getVisibleLineCount()) {
			this.scrolledLines = i - this.getVisibleLineCount();
		}

		if (this.scrolledLines <= 0) {
			this.scrolledLines = 0;
			this.hasUnreadNewMessages = false;
		}
	}

	public boolean isChatFocused() {
		return this.client.currentScreen instanceof ChatScreen;
	}

	private int getWidth() {
		return getWidth(this.client.options.getChatWidth().getValue());
	}

	private int getHeight() {
		return getHeight(this.isChatFocused() ? this.client.options.getChatHeightFocused().getValue()
		                                      : this.client.options.getChatHeightUnfocused().getValue());
	}

	private double getChatScale() {
		return this.client.options.getChatScale().getValue();
	}

	public static int getWidth(double widthOption) {
		int i = 320;
		int j = 40;
		return MathHelper.floor(widthOption * 280.0 + 40.0);
	}

	public static int getHeight(double heightOption) {
		int i = 180;
		int j = 20;
		return MathHelper.floor(heightOption * 160.0 + 20.0);
	}

	public static double getDefaultUnfocusedHeight() {
		int i = 180;
		int j = 20;
		return 70.0 / (getHeight(1.0) - 20);
	}

	public int getVisibleLineCount() {
		return this.getHeight() / this.getLineHeight();
	}

	private int getLineHeight() {
		return (int) (9.0 * (this.client.options.getChatLineSpacing().getValue() + 1.0));
	}

	/**
	 * Сохраняет draft.
	 *
	 * @param text text
	 */
	public void saveDraft(String text) {
		boolean bl = text.startsWith("/");
		this.draft = new ChatHud.Draft(text, bl ? ChatHud.ChatMethod.COMMAND : ChatHud.ChatMethod.MESSAGE);
	}

	/**
	 * Discard draft.
	 */
	public void discardDraft() {
		this.draft = null;
	}

	/**
	 * Создаёт screen.
	 *
	 * @param method method
	 * @param factory factory
	 *
	 * @return T — результат операции
	 */
	public <T extends ChatScreen> T createScreen(ChatHud.ChatMethod method, ChatScreen.Factory<T> factory) {
		return this.draft != null && method.shouldKeepDraft(this.draft)
		       ? factory.create(this.draft.text(), true)
		       : factory.create(method.getReplacement(), false);
	}

	@SuppressWarnings("unchecked")
	public void setClientScreen(ChatHud.ChatMethod method, ChatScreen.Factory<?> factory) {
		this.client.setScreen(this.createScreen(
				method,
				(ChatScreen.Factory<ChatScreen>) (ChatScreen.Factory<?>) factory
		));
	}

	public void setScreen() {
		if (this.client.currentScreen instanceof ChatScreen chatScreen) {
			this.screen = chatScreen;
		}
	}

	/**
	 * Удаляет screen.
	 *
	 * @return @Nullable ChatScreen — результат операции
	 */
	public @Nullable ChatScreen removeScreen() {
		ChatScreen chatScreen = this.screen;
		this.screen = null;
		return chatScreen;
	}

	public ChatHud.ChatState toChatState() {
		return new ChatHud.ChatState(
				List.copyOf(this.messages),
				List.copyOf(this.messageHistory),
				List.copyOf(this.removalQueue)
		);
	}

	/**
	 * Restore chat state.
	 *
	 * @param state state
	 */
	public void restoreChatState(ChatHud.ChatState state) {
		this.messageHistory.clear();
		this.messageHistory.addAll(state.messageHistory);
		this.removalQueue.clear();
		this.removalQueue.addAll(state.removalQueue);
		this.messages.clear();
		this.messages.addAll(state.messages);
		this.refresh();
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Backend}.
	 */
	public interface Backend {

		void updatePose(Consumer<Matrix3x2f> transformer);

		void fill(int x1, int y1, int x2, int y2, int color);

		boolean text(int y, float opacity, OrderedText text);

		void indicator(int x1, int y1, int x2, int y2, float opacity, MessageIndicator indicator);

		void indicatorIcon(
				int left,
				int bottom,
				boolean forceDraw,
				MessageIndicator indicator,
				MessageIndicator.Icon icon
		);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ChatMethod}.
	 */
	public static enum ChatMethod {
		MESSAGE("") {
			@Override
			public boolean shouldKeepDraft(ChatHud.Draft draft) {
				return true;
			}
		},
		COMMAND("/") {
			@Override
			public boolean shouldKeepDraft(ChatHud.Draft draft) {
				return this == draft.chatMethod;
			}
		};

		private final String replacement;

		ChatMethod(final String replacement) {
			this.replacement = replacement;
		}

		public String getReplacement() {
			return this.replacement;
		}

		/**
		 * Определяет, следует ли keep draft.
		 *
		 * @param draft draft
		 *
		 * @return boolean — результат операции
		 */
		public abstract boolean shouldKeepDraft(ChatHud.Draft draft);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code ChatState}.
	 */
	public static class ChatState {

		final List<ChatHudLine> messages;
		final List<String> messageHistory;
		final List<ChatHud.RemovalQueuedMessage> removalQueue;

		public ChatState(
				List<ChatHudLine> messages,
				List<String> messageHistory,
				List<ChatHud.RemovalQueuedMessage> removalQueue
		) {
			this.messages = messages;
			this.messageHistory = messageHistory;
			this.removalQueue = removalQueue;
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Draft}.
	 */
	public record Draft(String text, ChatHud.ChatMethod chatMethod) {
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Forwarder}.
	 */
	static class Forwarder implements ChatHud.Backend {

		private final DrawnTextConsumer drawer;

		public Forwarder(DrawnTextConsumer drawer) {
			this.drawer = drawer;
		}

		@Override
		public void updatePose(Consumer<Matrix3x2f> transformer) {
			DrawnTextConsumer.Transformation transformation = this.drawer.getTransformation();
			Matrix3x2f matrix3x2f = new Matrix3x2f(transformation.pose());
			transformer.accept(matrix3x2f);
			this.drawer.setTransformation(transformation.withPose(matrix3x2f));
		}

		@Override
		public void fill(int x1, int y1, int x2, int y2, int color) {
		}

		@Override
		public boolean text(int y, float opacity, OrderedText text) {
			this.drawer.text(Alignment.LEFT, 0, y, text);
			return false;
		}

		@Override
		public void indicator(int x1, int y1, int x2, int y2, float opacity, MessageIndicator indicator) {
		}

		@Override
		public void indicatorIcon(
				int left,
				int bottom,
				boolean forceDraw,
				MessageIndicator indicator,
				MessageIndicator.Icon icon
		) {
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Hud}.
	 */
	static class Hud implements ChatHud.Backend {

		private final DrawContext context;
		private final DrawnTextConsumer textConsumer;
		private DrawnTextConsumer.Transformation transformation;

		public Hud(DrawContext context) {
			this.context = context;
			this.textConsumer = context.getTextConsumer(DrawContext.HoverType.NONE, null);
			this.transformation = this.textConsumer.getTransformation();
		}

		@Override
		public void updatePose(Consumer<Matrix3x2f> transformer) {
			transformer.accept(this.context.getMatrices());
			this.transformation = this.transformation.withPose(new Matrix3x2f(this.context.getMatrices()));
		}

		@Override
		public void fill(int x1, int y1, int x2, int y2, int color) {
			this.context.fill(x1, y1, x2, y2, color);
		}

		@Override
		public boolean text(int y, float opacity, OrderedText text) {
			this.textConsumer.text(Alignment.LEFT, 0, y, this.transformation.withOpacity(opacity), text);
			return false;
		}

		@Override
		public void indicator(int x1, int y1, int x2, int y2, float opacity, MessageIndicator indicator) {
			int i = ColorHelper.withAlpha(opacity, indicator.indicatorColor());
			this.context.fill(x1, y1, x2, y2, i);
		}

		@Override
		public void indicatorIcon(
				int left,
				int bottom,
				boolean forceDraw,
				MessageIndicator indicator,
				MessageIndicator.Icon icon
		) {
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Interactable}.
	 */
	static class Interactable implements ChatHud.Backend, Consumer<Style> {

		private final DrawContext context;
		private final TextRenderer textRenderer;
		private final DrawnTextConsumer drawer;
		private DrawnTextConsumer.Transformation transformation;
		private final int mouseX;
		private final int mouseY;
		private final Vector2f untransformedOffset = new Vector2f();
		private @Nullable Style style;
		private final boolean hasInsertion;

		public Interactable(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, boolean bl) {
			this.context = context;
			this.textRenderer = textRenderer;
			this.drawer = context.getTextConsumer(DrawContext.HoverType.TOOLTIP_AND_CURSOR, this);
			this.mouseX = mouseX;
			this.mouseY = mouseY;
			this.hasInsertion = bl;
			this.transformation = this.drawer.getTransformation();
			this.calculateUntransformedOffset();
		}

		private void calculateUntransformedOffset() {
			this.context
					.getMatrices()
					.invert(new Matrix3x2f())
					.transformPosition(this.mouseX, this.mouseY, this.untransformedOffset);
		}

		@Override
		public void updatePose(Consumer<Matrix3x2f> transformer) {
			transformer.accept(this.context.getMatrices());
			this.transformation = this.transformation.withPose(new Matrix3x2f(this.context.getMatrices()));
			this.calculateUntransformedOffset();
		}

		@Override
		public void fill(int x1, int y1, int x2, int y2, int color) {
			this.context.fill(x1, y1, x2, y2, color);
		}

		/**
		 * Accept.
		 *
		 * @param style style
		 */
		public void accept(Style style) {
			this.style = style;
		}

		@Override
		public boolean text(int y, float opacity, OrderedText text) {
			this.style = null;
			this.drawer.text(Alignment.LEFT, 0, y, this.transformation.withOpacity(opacity), text);
			if (this.hasInsertion && this.style != null && this.style.getInsertion() != null) {
				this.context.setCursor(StandardCursors.POINTING_HAND);
			}

			return this.style != null;
		}

		private boolean isWithinBounds(int left, int top, int right, int bottom) {
			return DrawnTextConsumer.isWithinBounds(
					this.untransformedOffset.x,
					this.untransformedOffset.y,
					left,
					top,
					right,
					bottom
			);
		}

		@Override
		public void indicator(int x1, int y1, int x2, int y2, float opacity, MessageIndicator indicator) {
			int i = ColorHelper.withAlpha(opacity, indicator.indicatorColor());
			this.context.fill(x1, y1, x2, y2, i);
			if (this.isWithinBounds(x1, y1, x2, y2)) {
				this.indicatorTooltip(indicator);
			}
		}

		@Override
		public void indicatorIcon(
				int left,
				int bottom,
				boolean forceDraw,
				MessageIndicator indicator,
				MessageIndicator.Icon icon
		) {
			int i = bottom - icon.height - 1;
			int j = left + icon.width;
			boolean bl = this.isWithinBounds(left, i, j, bottom);
			if (bl) {
				this.indicatorTooltip(indicator);
			}

			if (forceDraw || bl) {
				icon.draw(this.context, left, i);
			}
		}

		private void indicatorTooltip(MessageIndicator indicator) {
			if (indicator.text() != null) {
				this.context.drawOrderedTooltip(
						this.textRenderer,
						this.textRenderer.wrapLines(indicator.text(), 210),
						this.mouseX,
						this.mouseY
				);
			}
		}
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code LineConsumer}.
	 */
	interface LineConsumer {

		void accept(ChatHudLine.Visible line, int y1, float opacity);
	}

	@FunctionalInterface
	@Environment(EnvType.CLIENT)
	/**
	 * {@code OpacityRule}.
	 */
	interface OpacityRule {

		ChatHud.OpacityRule CONSTANT = line -> 1.0F;

		static ChatHud.OpacityRule timeBased(int currentTick) {
			return line -> {
				int j = currentTick - line.addedTime();
				double d = j / 200.0;
				d = 1.0 - d;
				d *= 10.0;
				d = MathHelper.clamp(d, 0.0, 1.0);
				d *= d;
				return (float) d;
			};
		}

		float calculate(ChatHudLine.Visible line);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code RemovalQueuedMessage}.
	 */
	record RemovalQueuedMessage(MessageSignatureData signature, int deletableAfter) {
	}
}
