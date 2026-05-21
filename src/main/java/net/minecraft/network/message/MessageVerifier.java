package net.minecraft.network.message;

import com.mojang.logging.LogUtils;
import net.minecraft.network.encryption.SignatureVerifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.BooleanSupplier;

@FunctionalInterface
/**
 * Интерфейс message verifier.
 */
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

	public static class Impl implements MessageVerifier {

		private final SignatureVerifier signatureVerifier;
		private final BooleanSupplier expirationChecker;
		private @Nullable SignedMessage lastVerifiedMessage;
		private boolean lastMessageVerified = true;

		public Impl(SignatureVerifier signatureVerifier, BooleanSupplier expirationChecker) {
			this.signatureVerifier = signatureVerifier;
			this.expirationChecker = expirationChecker;
		}

		private boolean verifyPrecedingSignature(SignedMessage message) {
			if (message.equals(this.lastVerifiedMessage)) {
				return true;
			}
			else if (this.lastVerifiedMessage != null && !message.link().linksTo(this.lastVerifiedMessage.link())) {
				LOGGER.error(
						"Received out-of-order chat message from {}: expected index > {} for session {}, but was {} for session {}",
						new Object[]{
								message.getSender(),
								this.lastVerifiedMessage.link().index(),
								this.lastVerifiedMessage.link().sessionId(),
								message.link().index(),
								message.link().sessionId()
						}
				);
				return false;
			}
			else {
				return true;
			}
		}

		private boolean verify(SignedMessage message) {
			if (this.expirationChecker.getAsBoolean()) {
				LOGGER.error(
						"Received message with expired profile public key from {} with session {}",
						message.getSender(),
						message.link().sessionId()
				);
				return false;
			}
			else if (!message.verify(this.signatureVerifier)) {
				LOGGER.error(
						"Received message with invalid signature (is the session wrong, or signature cache out of sync?): {}",
						SignedMessage.toString(message)
				);
				return false;
			}
			else {
				return this.verifyPrecedingSignature(message);
			}
		}

		@Override
		public @Nullable SignedMessage ensureVerified(SignedMessage message) {
			this.lastMessageVerified = this.lastMessageVerified && this.verify(message);
			if (!this.lastMessageVerified) {
				return null;
			}
			else {
				this.lastVerifiedMessage = message;
				return message;
			}
		}
	}
}
