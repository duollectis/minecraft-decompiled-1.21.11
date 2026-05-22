package net.minecraft.network.message;

import com.mojang.logging.LogUtils;
import net.minecraft.network.encryption.SignatureVerifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.BooleanSupplier;

/**
 * Верификатор входящих подписанных сообщений.
 * Проверяет корректность подписи, порядок сообщений и срок действия ключа.
 */
@FunctionalInterface
public interface MessageVerifier {

	Logger LOGGER = LogUtils.getLogger();

	MessageVerifier NO_SIGNATURE = SignedMessage::stripSignature;

	MessageVerifier UNVERIFIED = message -> {
		LOGGER.error(
				"Received chat message from {}, but they have no chat session initialized and secure chat is enforced",
				message.getSender()
		);
		return null;
	};

	@Nullable SignedMessage ensureVerified(SignedMessage message);

	/**
	 * Полная реализация верификатора с отслеживанием состояния цепочки.
	 * Проверяет: срок действия ключа, корректность подписи, порядок сообщений.
	 * После первой ошибки все последующие сообщения отклоняются.
	 */
	class Impl implements MessageVerifier {

		private final SignatureVerifier signatureVerifier;
		private final BooleanSupplier expirationChecker;
		private @Nullable SignedMessage lastVerifiedMessage;
		private boolean lastMessageVerified = true;

		public Impl(SignatureVerifier signatureVerifier, BooleanSupplier expirationChecker) {
			this.signatureVerifier = signatureVerifier;
			this.expirationChecker = expirationChecker;
		}

		private boolean verifyPrecedingSignature(SignedMessage message) {
			if (message.equals(lastVerifiedMessage)) {
				return true;
			}

			if (lastVerifiedMessage != null && !message.link().linksTo(lastVerifiedMessage.link())) {
				LOGGER.error(
						"Received out-of-order chat message from {}: expected index > {} for session {}, but was {} for session {}",
						new Object[]{
								message.getSender(),
								lastVerifiedMessage.link().index(),
								lastVerifiedMessage.link().sessionId(),
								message.link().index(),
								message.link().sessionId()
						}
				);
				return false;
			}

			return true;
		}

		private boolean verify(SignedMessage message) {
			if (expirationChecker.getAsBoolean()) {
				LOGGER.error(
						"Received message with expired profile public key from {} with session {}",
						message.getSender(),
						message.link().sessionId()
				);
				return false;
			}

			if (!message.verify(signatureVerifier)) {
				LOGGER.error(
						"Received message with invalid signature (is the session wrong, or signature cache out of sync?): {}",
						SignedMessage.toString(message)
				);
				return false;
			}

			return verifyPrecedingSignature(message);
		}

		@Override
		public @Nullable SignedMessage ensureVerified(SignedMessage message) {
			lastMessageVerified = lastMessageVerified && verify(message);

			if (!lastMessageVerified) {
				return null;
			}

			lastVerifiedMessage = message;
			return message;
		}
	}
}
