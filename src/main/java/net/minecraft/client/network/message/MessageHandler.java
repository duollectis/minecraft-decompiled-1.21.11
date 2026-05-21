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

@Environment(EnvType.CLIENT)
public class MessageHandler {

	private static final Text
			VALIDATION_ERROR_TEXT =
			Text.translatable("chat.validation_error").formatted(Formatting.RED, Formatting.ITALIC);
	private final MinecraftClient client;
	private final Deque<MessageHandler.ProcessableMessage> delayedMessages = Queues.newArrayDeque();
	private long chatDelay;
	private long lastProcessTime;

	public MessageHandler(MinecraftClient client) {
		this.client = client;
	}

	public void processDelayedMessages() {
		if (this.client.isPaused()) {
			if (this.chatDelay > 0L) {
				this.lastProcessTime += 50L;
			}
		}
		else {
			if (this.chatDelay == 0L) {
				if (!this.delayedMessages.isEmpty()) {
					this.processAll();
				}
			}
			else {
				MessageHandler.ProcessableMessage processableMessage;
				if (Util.getMeasuringTimeMs() >= this.lastProcessTime + this.chatDelay) {
					do {
						processableMessage = this.delayedMessages.poll();
					}
					while (processableMessage != null && !processableMessage.accept());
				}
			}
		}
	}

	public void setChatDelay(double chatDelay) {
		long l = (long) (chatDelay * 1000.0);
		if (l == 0L && this.chatDelay > 0L && !this.client.isPaused()) {
			this.processAll();
		}

		this.chatDelay = l;
	}

	public void process() {
		this.delayedMessages.remove().accept();
	}

	public long getUnprocessedMessageCount() {
		return this.delayedMessages.size();
	}

	public void processAll() {
		this.delayedMessages.forEach(MessageHandler.ProcessableMessage::accept);
		this.delayedMessages.clear();
		this.lastProcessTime = 0L;
	}

	public boolean removeDelayedMessage(MessageSignatureData signature) {
		return this.delayedMessages.removeIf(message -> signature.equals(message.signature()));
	}

	private boolean shouldDelay() {
		return this.chatDelay > 0L && Util.getMeasuringTimeMs() < this.lastProcessTime + this.chatDelay;
	}

	private void process(@Nullable MessageSignatureData signature, BooleanSupplier processor) {
		if (this.shouldDelay()) {
			this.delayedMessages.add(new MessageHandler.ProcessableMessage(signature, processor));
		}
		else {
			processor.getAsBoolean();
		}
	}

	public void onChatMessage(SignedMessage message, GameProfile sender, MessageType.Parameters params) {
		boolean bl = this.client.options.getOnlyShowSecureChat().getValue();
		SignedMessage signedMessage = bl ? message.withoutUnsigned() : message;
		Text text = params.applyChatDecoration(signedMessage.getContent());
		Instant instant = Instant.now();
		this.process(
				message.signature(), () -> {
					boolean bl2 = this.processChatMessageInternal(params, message, text, sender, bl, instant);
					ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.getNetworkHandler();
					if (clientPlayNetworkHandler != null && message.signature() != null) {
						clientPlayNetworkHandler.acknowledge(message.signature(), bl2);
					}

					return bl2;
				}
		);
	}

	public void onUnverifiedMessage(
			UUID sender,
			@Nullable MessageSignatureData signature,
			MessageType.Parameters parameters
	) {
		this.process(
				null, () -> {
					ClientPlayNetworkHandler clientPlayNetworkHandler = this.client.getNetworkHandler();
					if (clientPlayNetworkHandler != null && signature != null) {
						clientPlayNetworkHandler.acknowledge(signature, false);
					}

					if (this.client.shouldBlockMessages(sender)) {
						return false;
					}
					else {
						Text text = parameters.applyChatDecoration(VALIDATION_ERROR_TEXT);
						this.client.inGameHud.getChatHud().addMessage(text, null, MessageIndicator.chatError());
						this.client
								.getNarratorManager()
								.narrate(parameters.applyNarrationDecoration(VALIDATION_ERROR_TEXT));
						this.lastProcessTime = Util.getMeasuringTimeMs();
						return true;
					}
				}
		);
	}

	public void onProfilelessMessage(Text content, MessageType.Parameters params) {
		Instant instant = Instant.now();
		this.process(
				null, () -> {
					Text text2 = params.applyChatDecoration(content);
					this.client.inGameHud.getChatHud().addMessage(text2);
					this.narrate(params, content);
					this.addToChatLog(text2, instant);
					this.lastProcessTime = Util.getMeasuringTimeMs();
					return true;
				}
		);
	}

	private boolean processChatMessageInternal(
			MessageType.Parameters params,
			SignedMessage message,
			Text decorated,
			GameProfile sender,
			boolean onlyShowSecureChat,
			Instant receptionTimestamp
	) {
		MessageTrustStatus messageTrustStatus = this.getStatus(message, decorated, receptionTimestamp);
		if (onlyShowSecureChat && messageTrustStatus.isInsecure()) {
			return false;
		}
		else if (!this.client.shouldBlockMessages(message.getSender()) && !message.isFullyFiltered()) {
			MessageIndicator messageIndicator = messageTrustStatus.createIndicator(message);
			MessageSignatureData messageSignatureData = message.signature();
			FilterMask filterMask = message.filterMask();
			if (filterMask.isPassThrough()) {
				this.client.inGameHud.getChatHud().addMessage(decorated, messageSignatureData, messageIndicator);
				this.narrate(params, message.getContent());
			}
			else {
				Text text = filterMask.getFilteredText(message.getSignedContent());
				if (text != null) {
					this.client.inGameHud
							.getChatHud()
							.addMessage(params.applyChatDecoration(text), messageSignatureData, messageIndicator);
					this.narrate(params, text);
				}
			}

			this.addToChatLog(message, sender, messageTrustStatus);
			this.lastProcessTime = Util.getMeasuringTimeMs();
			return true;
		}
		else {
			return false;
		}
	}

	private void narrate(MessageType.Parameters params, Text message) {
		this.client.getNarratorManager().narrateChatMessage(params.applyNarrationDecoration(message));
	}

	private MessageTrustStatus getStatus(SignedMessage message, Text decorated, Instant receptionTimestamp) {
		return this.isAlwaysTrusted(message.getSender()) ? MessageTrustStatus.SECURE : MessageTrustStatus.getStatus(
				message,
				decorated,
				receptionTimestamp
		);
	}

	private void addToChatLog(SignedMessage message, GameProfile gameProfile, MessageTrustStatus messageTrustStatus) {
		ChatLog chatLog = this.client.getAbuseReportContext().getChatLog();
		chatLog.add(ReceivedMessage.of(gameProfile, message, messageTrustStatus));
	}

	private void addToChatLog(Text message, Instant timestamp) {
		ChatLog chatLog = this.client.getAbuseReportContext().getChatLog();
		chatLog.add(ReceivedMessage.of(message, timestamp));
	}

	public void onGameMessage(Text message, boolean overlay) {
		if (!this.client.options.getHideMatchedNames().getValue()
				|| !this.client.shouldBlockMessages(this.extractSender(message))) {
			if (overlay) {
				this.client.inGameHud.setOverlayMessage(message, false);
				this.client.getNarratorManager().narrateSystemMessage(message);
			}
			else {
				this.client.inGameHud.getChatHud().addMessage(message);
				this.addToChatLog(message, Instant.now());
				this.client.getNarratorManager().narrate(message);
			}
		}
	}

	private UUID extractSender(Text text) {
		String string = TextVisitFactory.removeFormattingCodes(text);
		String string2 = StringUtils.substringBetween(string, "<", ">");
		return string2 == null ? Util.NIL_UUID : this.client.getSocialInteractionsManager().getUuid(string2);
	}

	private boolean isAlwaysTrusted(UUID sender) {
		if (this.client.isInSingleplayer() && this.client.player != null) {
			UUID uUID = this.client.player.getGameProfile().id();
			return uUID.equals(sender);
		}
		else {
			return false;
		}
	}

	@Environment(EnvType.CLIENT)
	record ProcessableMessage(@Nullable MessageSignatureData signature, BooleanSupplier handler) {

		public boolean accept() {
			return this.handler.getAsBoolean();
		}
	}
}
