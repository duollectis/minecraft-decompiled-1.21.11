package net.minecraft.network.message;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public interface SentMessage {

	Text content();

	void send(ServerPlayerEntity sender, boolean filterMaskEnabled, MessageType.Parameters params);

	static SentMessage of(SignedMessage message) {
		return (SentMessage) (message.isSenderMissing() ? new SentMessage.Profileless(message.getContent())
		                                                : new SentMessage.Chat(message)
		);
	}

	public record Chat(SignedMessage message) implements SentMessage {

		@Override
		public Text content() {
			return this.message.getContent();
		}

		@Override
		public void send(ServerPlayerEntity sender, boolean filterMaskEnabled, MessageType.Parameters params) {
			SignedMessage signedMessage = this.message.withFilterMaskEnabled(filterMaskEnabled);
			if (!signedMessage.isFullyFiltered()) {
				sender.networkHandler.sendChatMessage(signedMessage, params);
			}
		}
	}

	public record Profileless(Text content) implements SentMessage {

		@Override
		public void send(ServerPlayerEntity sender, boolean filterMaskEnabled, MessageType.Parameters params) {
			sender.networkHandler.sendProfilelessChatMessage(this.content, params);
		}
	}
}
