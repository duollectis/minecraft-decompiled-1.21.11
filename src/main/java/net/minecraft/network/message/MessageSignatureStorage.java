package net.minecraft.network.message;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

/**
 * Кольцевое хранилище последних известных подписей сообщений.
 * Позволяет передавать подписи по сети в компактном виде (индекс вместо 256 байт).
 * Максимальный размер — {@value #MAX_ENTRIES} записей.
 */
public class MessageSignatureStorage {

	public static final int MISSING = -1;
	private static final int MAX_ENTRIES = 128;

	private final @Nullable MessageSignatureData[] signatures;

	public MessageSignatureStorage(int maxEntries) {
		signatures = new MessageSignatureData[maxEntries];
	}

	public static MessageSignatureStorage create() {
		return new MessageSignatureStorage(MAX_ENTRIES);
	}

	public int indexOf(MessageSignatureData signature) {
		for (int index = 0; index < signatures.length; index++) {
			if (signature.equals(signatures[index])) {
				return index;
			}
		}

		return MISSING;
	}

	public @Nullable MessageSignatureData get(int index) {
		return signatures[index];
	}

	/**
	 * Добавляет подписи из тела сообщения и саму подпись сообщения в хранилище.
	 * Подписи из {@code lastSeenMessages} добавляются первыми, затем подпись самого сообщения.
	 *
	 * @param body      тело сообщения с последними просмотренными подписями
	 * @param signature подпись самого сообщения, или {@code null} для неподписанных
	 */
	public void add(MessageBody body, @Nullable MessageSignatureData signature) {
		List<MessageSignatureData> lastSeen = body.lastSeenMessages().entries();
		ArrayDeque<MessageSignatureData> deque = new ArrayDeque<>(lastSeen.size() + 1);
		deque.addAll(lastSeen);

		if (signature != null) {
			deque.add(signature);
		}

		addFrom(deque);
	}

	@VisibleForTesting
	void addFrom(List<MessageSignatureData> newSignatures) {
		addFrom(new ArrayDeque<>(newSignatures));
	}

	/**
	 * Заполняет хранилище из очереди, вытесняя старые записи.
	 * Уже присутствующие в очереди подписи не дублируются — они перемещаются в начало.
	 *
	 * @param deque очередь новых подписей (последние добавленные — в конце)
	 */
	private void addFrom(ArrayDeque<MessageSignatureData> deque) {
		Set<MessageSignatureData> inQueue = new ObjectOpenHashSet(deque);

		for (int index = 0; !deque.isEmpty() && index < signatures.length; index++) {
			MessageSignatureData displaced = signatures[index];
			signatures[index] = deque.removeLast();

			if (displaced != null && !inQueue.contains(displaced)) {
				deque.addFirst(displaced);
			}
		}
	}
}
