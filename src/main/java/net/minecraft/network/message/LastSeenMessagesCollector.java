package net.minecraft.network.message;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

import java.util.BitSet;
import java.util.Objects;

/**
 * Кольцевой буфер последних просмотренных сообщений на стороне клиента.
 * Накапливает подтверждения и формирует снимок окна для отправки серверу.
 */
public class LastSeenMessagesCollector {

	private final @Nullable AcknowledgedMessage[] acknowledgedMessages;
	private int nextIndex;
	private int messageCount;
	private @Nullable MessageSignatureData lastAdded;

	public LastSeenMessagesCollector(int size) {
		acknowledgedMessages = new AcknowledgedMessage[size];
	}

	/**
	 * Добавляет подпись сообщения в кольцевой буфер.
	 * Дублирующие подписи (та же, что и последняя добавленная) игнорируются.
	 *
	 * @param signature подпись сообщения
	 * @param displayed {@code true} если сообщение было отображено пользователю
	 * @return {@code true} если подпись была добавлена, {@code false} если проигнорирована как дубликат
	 */
	public boolean add(MessageSignatureData signature, boolean displayed) {
		if (Objects.equals(signature, lastAdded)) {
			return false;
		}

		lastAdded = signature;
		addToBuffer(displayed ? new AcknowledgedMessage(signature, true) : null);
		return true;
	}

	private void addToBuffer(@Nullable AcknowledgedMessage message) {
		int slot = nextIndex;
		nextIndex = (slot + 1) % acknowledgedMessages.length;
		messageCount++;
		acknowledgedMessages[slot] = message;
	}

	public void remove(MessageSignatureData signature) {
		for (int index = 0; index < acknowledgedMessages.length; index++) {
			AcknowledgedMessage msg = acknowledgedMessages[index];

			if (msg != null && msg.pending() && signature.equals(msg.signature())) {
				acknowledgedMessages[index] = null;
				break;
			}
		}
	}

	/**
	 * Сбрасывает счётчик новых сообщений и возвращает его значение.
	 * Используется для формирования поля {@code offset} в пакете подтверждений.
	 *
	 * @return количество сообщений, накопленных с момента последнего сброса
	 */
	public int resetMessageCount() {
		int count = messageCount;
		messageCount = 0;
		return count;
	}

	/**
	 * Формирует снимок текущего состояния окна для отправки серверу.
	 * Обходит кольцевой буфер начиная с {@code nextIndex}, строит битовую маску
	 * и список подписей, затем снимает флаг {@code pending} у всех включённых сообщений.
	 *
	 * @return пара (список подписей, пакет подтверждений)
	 */
	public LastSeenMessagesCollector.LastSeenMessages collect() {
		int offset = resetMessageCount();
		BitSet acknowledged = new BitSet(acknowledgedMessages.length);
		ObjectList<MessageSignatureData> signatures = new ObjectArrayList(acknowledgedMessages.length);

		for (int slot = 0; slot < acknowledgedMessages.length; slot++) {
			int bufferIndex = (nextIndex + slot) % acknowledgedMessages.length;
			AcknowledgedMessage msg = acknowledgedMessages[bufferIndex];

			if (msg != null) {
				acknowledged.set(slot, true);
				signatures.add(msg.signature());
				acknowledgedMessages[bufferIndex] = msg.unmarkAsPending();
			}
		}

		LastSeenMessageList lastSeen = new LastSeenMessageList(signatures);
		LastSeenMessageList.Acknowledgment acknowledgment = new LastSeenMessageList.Acknowledgment(
				offset,
				acknowledged,
				lastSeen.calculateChecksum()
		);

		return new LastSeenMessagesCollector.LastSeenMessages(lastSeen, acknowledgment);
	}

	public int getMessageCount() {
		return messageCount;
	}

	/**
	 * Снимок окна последних просмотренных сообщений вместе с пакетом подтверждений.
	 */
	public record LastSeenMessages(LastSeenMessageList lastSeen, LastSeenMessageList.Acknowledgment update) {
	}
}
