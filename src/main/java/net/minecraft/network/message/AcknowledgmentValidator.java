package net.minecraft.network.message;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

public class AcknowledgmentValidator {

	private final int size;
	private final ObjectList<AcknowledgedMessage> messages = new ObjectArrayList();
	private @Nullable MessageSignatureData lastSignature;

	public AcknowledgmentValidator(int size) {
		this.size = size;

		for (int i = 0; i < size; i++) {
			this.messages.add(null);
		}
	}

	public void addPending(MessageSignatureData signature) {
		if (!signature.equals(this.lastSignature)) {
			this.messages.add(new AcknowledgedMessage(signature, true));
			this.lastSignature = signature;
		}
	}

	public int getMessageCount() {
		return this.messages.size();
	}

	public void removeUntil(int index) throws AcknowledgmentValidator.ValidationException {
		int i = this.messages.size() - this.size;
		if (index >= 0 && index <= i) {
			this.messages.removeElements(0, index);
		}
		else {
			throw new AcknowledgmentValidator.ValidationException(
					"Advanced last seen window by " + index + " messages, but expected at most " + i);
		}
	}

	public LastSeenMessageList validate(LastSeenMessageList.Acknowledgment acknowledgment)
	throws AcknowledgmentValidator.ValidationException {
		this.removeUntil(acknowledgment.offset());
		ObjectList<MessageSignatureData> objectList = new ObjectArrayList(acknowledgment.acknowledged().cardinality());
		if (acknowledgment.acknowledged().length() > this.size) {
			throw new AcknowledgmentValidator.ValidationException(
					"Last seen update contained " + acknowledgment.acknowledged().length()
							+ " messages, but maximum window size is " + this.size
			);
		}
		else {
			for (int i = 0; i < this.size; i++) {
				boolean bl = acknowledgment.acknowledged().get(i);
				AcknowledgedMessage acknowledgedMessage = (AcknowledgedMessage) this.messages.get(i);
				if (bl) {
					if (acknowledgedMessage == null) {
						throw new AcknowledgmentValidator.ValidationException(
								"Last seen update acknowledged unknown or previously ignored message at index " + i);
					}

					this.messages.set(i, acknowledgedMessage.unmarkAsPending());
					objectList.add(acknowledgedMessage.signature());
				}
				else {
					if (acknowledgedMessage != null && !acknowledgedMessage.pending()) {
						throw new AcknowledgmentValidator.ValidationException(
								"Last seen update ignored previously acknowledged message at index " + i
										+ " and signature " + acknowledgedMessage.signature()
						);
					}

					this.messages.set(i, null);
				}
			}

			LastSeenMessageList lastSeenMessageList = new LastSeenMessageList(objectList);
			if (!acknowledgment.checksumEquals(lastSeenMessageList)) {
				throw new AcknowledgmentValidator.ValidationException(
						"Checksum mismatch on last seen update: the client and server must have desynced");
			}
			else {
				return lastSeenMessageList;
			}
		}
	}

	public static class ValidationException extends Exception {

		public ValidationException(String message) {
			super(message);
		}
	}
}
