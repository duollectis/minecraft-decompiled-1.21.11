package net.minecraft.client.network.message;

import com.google.common.collect.Queues;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.session.report.log.ChatLog;
import net.minecraft.client.session.report.log.ReceivedMessage;
import net.minecraft.network.message.FilterMask;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Deque;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Обработчик входящих сообщений чата на стороне клиента.
 * <p>Поддерживает задержку отображения сообщений ({@link #setChatDelay}),
 * очередь отложенных сообщений и верификацию подписей через {@link MessageTrustStatus}.
 * Сообщения от заблокированных или скрытых игроков фильтруются.
 */
@Environment(EnvType.CLIENT)
public class MessageHandler {

	private static final Text VALIDATION_ERROR_TEXT =
			Text.translatable("chat.validation_error").formatted(Formatting.RED, Formatting.ITALIC);

	private final MinecraftClient client;
	private final Deque<MessageHandler.ProcessableMessage> delayedMessages = Queues.newArrayDeque();
	private long chatDelay;
	private long lastProcessTime;

	/**
	 * @param client клиент Minecraft
	 */
	public MessageHandler(MinecraftClient client) {
		this.client = client;
	}

	/**
	 * Обрабатывает отложенные сообщения в очереди.
	 * Если игра на паузе и задержка активна — сдвигает время последней обработки.
	 * Если задержка истекла — извлекает и обрабатывает одно сообщение из очереди.
	 */
	public void processDelayedMessages() {
		if (client.isPaused()) {
			if (chatDelay > 0L) {
				lastProcessTime += 50L;
			}

			return;
		}

		if (chatDelay == 0L) {
			if (delayedMessages.isEmpty() == false) {
				processAll();
			}

			return;
		}

		if (Util.getMeasuringTimeMs() < lastProcessTime + chatDelay) {
			return;
		}

		ProcessableMessage message;
		do {
			message = delayedMessages.poll();
		}
		while (message != null && message.accept() == false);
	}

	/**
	 * Устанавливает задержку отображения сообщений чата.
	 * При сбросе задержки до нуля немедленно обрабатывает все отложенные сообщения.
	 *
	 * @param chatDelay задержка в секундах
	 */
	public void setChatDelay(double chatDelay) {
		long delayMs = (long) (chatDelay * 1000.0);

		if (delayMs == 0L && this.chatDelay > 0L && client.isPaused() == false) {
			processAll();
		}

		this.chatDelay = delayMs;
	}

	/**
	 * Принудительно обрабатывает первое сообщение из очереди.
	 */
	public void process() {
		delayedMessages.remove().accept();
	}

	/**
	 * Возвращает количество необработанных сообщений в очереди.
	 *
	 * @return размер очереди
	 */
	public long getUnprocessedMessageCount() {
		return delayedMessages.size();
	}

	/**
	 * Немедленно обрабатывает все отложенные сообщения и очищает очередь.
	 */
	public void processAll() {
		delayedMessages.forEach(ProcessableMessage::accept);
		delayedMessages.clear();
		lastProcessTime = 0L;
	}

	/**
	 * Удаляет отложенное сообщение с указанной подписью из очереди.
	 *
	 * @param signature подпись сообщения для удаления
	 * @return {@code true} если сообщение было найдено и удалено
	 */
	public boolean removeDelayedMessage(MessageSignatureData signature) {
		return delayedMessages.removeIf(message -> signature.equals(message.signature()));
	}

	/**
	 * Обрабатывает подписанное сообщение чата от игрока.
	 * Верифицирует подпись, применяет фильтры и добавляет в HUD и лог.
	 *
	 * @param message сообщение с подписью
	 * @param sender  профиль отправителя
	 * @param params  параметры типа сообщения (декорации, нарратор)
	 */
	public void onChatMessage(SignedMessage message, GameProfile sender, MessageType.Parameters params) {
		boolean onlySecure = client.options.getOnlyShowSecureChat().getValue();
		SignedMessage displayMessage = onlySecure ? message.withoutUnsigned() : message;
		Text decorated = params.applyChatDecoration(displayMessage.getContent());
		Instant receivedAt = Instant.now();

		process(
				message.signature(), () -> {
					boolean accepted = processChatMessageInternal(params, message, decorated, sender, onlySecure, receivedAt);
					ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

					if (networkHandler != null && message.signature() != null) {
						networkHandler.acknowledge(message.signature(), accepted);
					}

					return accepted;
				}
		);
	}

	/**
	 * Обрабатывает сообщение с нарушенной или отсутствующей подписью.
	 * Отображает индикатор ошибки валидации вместо содержимого.
	 *
	 * @param sender     UUID отправителя
	 * @param signature  подпись сообщения или {@code null}
	 * @param parameters параметры типа сообщения
	 */
	public void onUnverifiedMessage(
			UUID sender,
			@Nullable MessageSignatureData signature,
			MessageType.Parameters parameters
	) {
		process(
				null, () -> {
					ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();

					if (networkHandler != null && signature != null) {
						networkHandler.acknowledge(signature, false);
					}

					if (client.shouldBlockMessages(sender)) {
						return false;
					}

					Text decorated = parameters.applyChatDecoration(VALIDATION_ERROR_TEXT);
					client.inGameHud.getChatHud().addMessage(decorated, null, MessageIndicator.chatError());
					client.getNarratorManager().narrate(parameters.applyNarrationDecoration(VALIDATION_ERROR_TEXT));
					lastProcessTime = Util.getMeasuringTimeMs();
					return true;
				}
		);
	}

	/**
	 * Обрабатывает системное сообщение без профиля отправителя (серверные объявления и т.д.).
	 *
	 * @param content содержимое сообщения
	 * @param params  параметры типа сообщения
	 */
	public void onProfilelessMessage(Text content, MessageType.Parameters params) {
		Instant receivedAt = Instant.now();

		process(
				null, () -> {
					Text decorated = params.applyChatDecoration(content);
					client.inGameHud.getChatHud().addMessage(decorated);
					narrate(params, content);
					addToChatLog(decorated, receivedAt);
					lastProcessTime = Util.getMeasuringTimeMs();
					return true;
				}
		);
	}

	/**
	 * Обрабатывает игровое сообщение (системное или оверлей).
	 * Фильтрует сообщения от заблокированных игроков по имени в тексте.
	 *
	 * @param message сообщение для отображения
	 * @param overlay {@code true} для отображения в оверлее (над хотбаром)
	 */
	public void onGameMessage(Text message, boolean overlay) {
		boolean hideMatchedNames = client.options.getHideMatchedNames().getValue();

		if (hideMatchedNames && client.shouldBlockMessages(extractSender(message))) {
			return;
		}

		if (overlay) {
			client.inGameHud.setOverlayMessage(message, false);
			client.getNarratorManager().narrateSystemMessage(message);
		}
		else {
			client.inGameHud.getChatHud().addMessage(message);
			addToChatLog(message, Instant.now());
			client.getNarratorManager().narrate(message);
		}
	}

	private boolean shouldDelay() {
		return chatDelay > 0L && Util.getMeasuringTimeMs() < lastProcessTime + chatDelay;
	}

	private void process(@Nullable MessageSignatureData signature, BooleanSupplier processor) {
		if (shouldDelay()) {
			delayedMessages.add(new ProcessableMessage(signature, processor));
		}
		else {
			processor.getAsBoolean();
		}
	}

	private boolean processChatMessageInternal(
			MessageType.Parameters params,
			SignedMessage message,
			Text decorated,
			GameProfile sender,
			boolean onlyShowSecureChat,
			Instant receptionTimestamp
	) {
		MessageTrustStatus trustStatus = getStatus(message, decorated, receptionTimestamp);

		if (onlyShowSecureChat && trustStatus.isInsecure()) {
			return false;
		}

		if (client.shouldBlockMessages(message.getSender()) || message.isFullyFiltered()) {
			return false;
		}

		MessageIndicator indicator = trustStatus.createIndicator(message);
		MessageSignatureData messageSignature = message.signature();
		FilterMask filterMask = message.filterMask();

		if (filterMask.isPassThrough()) {
			client.inGameHud.getChatHud().addMessage(decorated, messageSignature, indicator);
			narrate(params, message.getContent());
		}
		else {
			Text filtered = filterMask.getFilteredText(message.getSignedContent());

			if (filtered != null) {
				client.inGameHud.getChatHud().addMessage(params.applyChatDecoration(filtered), messageSignature, indicator);
				narrate(params, filtered);
			}
		}

		addToChatLog(message, sender, trustStatus);
		lastProcessTime = Util.getMeasuringTimeMs();
		return true;
	}

	private void narrate(MessageType.Parameters params, Text message) {
		client.getNarratorManager().narrateChatMessage(params.applyNarrationDecoration(message));
	}

	private MessageTrustStatus getStatus(SignedMessage message, Text decorated, Instant receptionTimestamp) {
		return isAlwaysTrusted(message.getSender())
				? MessageTrustStatus.SECURE
				: MessageTrustStatus.getStatus(message, decorated, receptionTimestamp);
	}

	private void addToChatLog(SignedMessage message, GameProfile gameProfile, MessageTrustStatus trustStatus) {
		ChatLog chatLog = client.getAbuseReportContext().getChatLog();
		chatLog.add(ReceivedMessage.of(gameProfile, message, trustStatus));
	}

	private void addToChatLog(Text message, Instant timestamp) {
		ChatLog chatLog = client.getAbuseReportContext().getChatLog();
		chatLog.add(ReceivedMessage.of(message, timestamp));
	}

	private UUID extractSender(Text text) {
		String plain = TextVisitFactory.removeFormattingCodes(text);
		String name = StringUtils.substringBetween(plain, "<", ">");
		return name == null ? Util.NIL_UUID : client.getSocialInteractionsManager().getUuid(name);
	}

	private boolean isAlwaysTrusted(UUID sender) {
		if (client.isInSingleplayer() == false || client.player == null) {
			return false;
		}

		return client.player.getGameProfile().id().equals(sender);
	}

	/**
	 * Сообщение в очереди отложенной обработки.
	 */
	@Environment(EnvType.CLIENT)
	record ProcessableMessage(@Nullable MessageSignatureData signature, BooleanSupplier handler) {

		public boolean accept() {
			return handler.getAsBoolean();
		}
	}
}
