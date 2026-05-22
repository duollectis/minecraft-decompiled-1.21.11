package net.minecraft.network.message;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

/**
 * Серверный валидатор скользящего окна подтверждений чат-сообщений.
 * Отслеживает последние {@code size} сообщений и проверяет корректность
 * битовой маски подтверждений, присылаемой клиентом в каждом пакете.
 */
public class AcknowledgmentValidator {

	private final int size;
	private final ObjectList<AcknowledgedMessage> messages = new ObjectArrayList();
	private @Nullable MessageSignatureData lastSignature;

	public AcknowledgmentValidator(int size) {
		this.size = size;

		for (int index = 0; index < size; index++) {
			messages.add(null);
		}
	}

	public void addPending(MessageSignatureData signature) {
		if (signature.equals(lastSignature)) {
			return;
		}

		messages.add(new AcknowledgedMessage(signature, true));
		lastSignature = signature;
	}

	public int getMessageCount() {
		return messages.size();
	}

	/**
	 * Удаляет из начала очереди {@code index} сообщений, сдвигая окно вперёд.
	 * Допустимый сдвиг — не более {@code messages.size() - size}.
	 *
	 * @param index количество сообщений для удаления
	 * @throws ValidationException если сдвиг выходит за допустимые границы
	 */
	public void removeUntil(int index) throws ValidationException {
		int maxAllowed = messages.size() - size;

		if (index < 0 || index > maxAllowed) {
			throw new ValidationException(
					"Advanced last seen window by " + index + " messages, but expected at most " + maxAllowed
			);
		}

		messages.removeElements(0, index);
	}

	/**
	 * Проверяет пакет подтверждений от клиента и возвращает список подтверждённых подписей.
	 * Алгоритм:
	 * <ol>
	 *   <li>Сдвигает окно на {@code acknowledgment.offset()} позиций.</li>
	 *   <li>Для каждого бита маски проверяет согласованность с внутренним состоянием.</li>
	 *   <li>Сверяет контрольную сумму для защиты от рассинхронизации.</li>
	 * </ol>
	 *
	 * @param acknowledgment пакет подтверждений от клиента
	 * @return список подписей подтверждённых сообщений
	 * @throws ValidationException при любом нарушении протокола
	 */
	public LastSeenMessageList validate(LastSeenMessageList.Acknowledgment acknowledgment)
	throws ValidationException {
		removeUntil(acknowledgment.offset());

		if (acknowledgment.acknowledged().length() > size) {
			throw new ValidationException(
					"Last seen update contained " + acknowledgment.acknowledged().length()
							+ " messages, but maximum window size is " + size
			);
		}

		ObjectList<MessageSignatureData> signatures = new ObjectArrayList(acknowledgment.acknowledged().cardinality());

		for (int slotIndex = 0; slotIndex < size; slotIndex++) {
			boolean isAcknowledged = acknowledgment.acknowledged().get(slotIndex);
			AcknowledgedMessage msg = messages.get(slotIndex);

			if (isAcknowledged) {
				if (msg == null) {
					throw new ValidationException(
							"Last seen update acknowledged unknown or previously ignored message at index " + slotIndex
					);
				}

				messages.set(slotIndex, msg.unmarkAsPending());
				signatures.add(msg.signature());
			} else {
				if (msg != null && !msg.pending()) {
					throw new ValidationException(
							"Last seen update ignored previously acknowledged message at index " + slotIndex
									+ " and signature " + msg.signature()
					);
				}

				messages.set(slotIndex, null);
			}
		}

		LastSeenMessageList lastSeenMessages = new LastSeenMessageList(signatures);

		if (!acknowledgment.checksumEquals(lastSeenMessages)) {
			throw new ValidationException(
					"Checksum mismatch on last seen update: the client and server must have desynced"
			);
		}

		return lastSeenMessages;
	}

	public static class ValidationException extends Exception {

		public ValidationException(String message) {
			super(message);
		}
	}
}
