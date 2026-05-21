package net.minecraft.network.message;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

import java.util.BitSet;
import java.util.Objects;

/**
 * Класс last seen messages collector.
 */
public class LastSeenMessagesCollector {

	private final @Nullable AcknowledgedMessage[] acknowledgedMessages;
	private int nextIndex;
	private int messageCount;
	private @Nullable MessageSignatureData lastAdded;

	public LastSeenMessagesCollector(int size) {
		this.acknowledgedMessages = new AcknowledgedMessage[size];
	}

	/**
	 * Add.
	 *
	 * @param signature signature
	 * @param displayed displayed
	 *
	 * @return boolean — результат операции
	 */
	public boolean add(MessageSignatureData signature, boolean displayed) {
		if (Objects.equals(signature, this.lastAdded)) {
			return false;
		}
		else {
			this.lastAdded = signature;
			this.add(displayed ? new AcknowledgedMessage(signature, true) : null);
			return true;
		}
	}

	private void add(@Nullable AcknowledgedMessage message) {
		int i = this.nextIndex;
		this.nextIndex = (i + 1) % this.acknowledgedMessages.length;
		this.messageCount++;
		this.acknowledgedMessages[i] = message;
	}

	/**
	 * Remove.
	 *
	 * @param signature signature
	 */
	public void remove(MessageSignatureData signature) {
		for (int i = 0; i < this.acknowledgedMessages.length; i++) {
			AcknowledgedMessage acknowledgedMessage = this.acknowledgedMessages[i];
			if (acknowledgedMessage != null && acknowledgedMessage.pending()
					&& signature.equals(acknowledgedMessage.signature())) {
				this.acknowledgedMessages[i] = null;
				break;
			}
		}
	}

	/**
	 * Сбрасывает message count.
	 *
	 * @return int — результат операции
	 */
	public int resetMessageCount() {
		int i = this.messageCount;
		this.messageCount = 0;
		return i;
	}

	public LastSeenMessagesCollector.LastSeenMessages collect() {
		int i = this.resetMessageCount();
		BitSet bitSet = new BitSet(this.acknowledgedMessages.length);
		ObjectList<MessageSignatureData> objectList = new ObjectArrayList(this.acknowledgedMessages.length);

		for (int j = 0; j < this.acknowledgedMessages.length; j++) {
			int k = (this.nextIndex + j) % this.acknowledgedMessages.length;
			AcknowledgedMessage acknowledgedMessage = this.acknowledgedMessages[k];
			if (acknowledgedMessage != null) {
				bitSet.set(j, true);
				objectList.add(acknowledgedMessage.signature());
				this.acknowledgedMessages[k] = acknowledgedMessage.unmarkAsPending();
			}
		}

		LastSeenMessageList lastSeenMessageList = new LastSeenMessageList(objectList);
		LastSeenMessageList.Acknowledgment
				acknowledgment =
				new LastSeenMessageList.Acknowledgment(i, bitSet, lastSeenMessageList.calculateChecksum());
		return new LastSeenMessagesCollector.LastSeenMessages(lastSeenMessageList, acknowledgment);
	}

	public int getMessageCount() {
		return this.messageCount;
	}

	/**
	 * Запись last seen messages.
	 */
	public record LastSeenMessages(LastSeenMessageList lastSeen, LastSeenMessageList.Acknowledgment update) {
	}
}
