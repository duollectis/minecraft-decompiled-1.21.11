package net.minecraft.network.message;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Отправляемое сообщение чата. Два варианта: подписанное ({@link Chat}) и без профиля ({@link Profileless}).
 */
public interface SentMessage {

	Text content();

	void send(ServerPlayerEntity sender, boolean filterMaskEnabled, MessageType.Parameters params);

	static SentMessage of(SignedMessage message) {
		return message.isSenderMissing()
				? new SentMessage.Profileless(message.getContent())
				: new SentMessage.Chat(message);
	}

	/**
	 * Подписанное сообщение с профилем игрока. Применяет маску фильтрации перед отправкой.
	 */
	record Chat(SignedMessage message) implements SentMessage {

		@Override
		public Text content() {
			return message.getContent();
		}

		@Override
		public void send(ServerPlayerEntity sender, boolean filterMaskEnabled, MessageType.Parameters params) {
			SignedMessage filtered = message.withFilterMaskEnabled(filterMaskEnabled);

			if (!filtered.isFullyFiltered()) {
				sender.networkHandler.sendChatMessage(filtered, params);
			}
		}
	}

	/**
	 * Сообщение без профиля игрока (анонимное или системное).
	 */
	record Profileless(Text content) implements SentMessage {

		@Override
		public void send(ServerPlayerEntity sender, boolean filterMaskEnabled, MessageType.Parameters params) {
			sender.networkHandler.sendProfilelessChatMessage(content, params);
		}
	}
}
