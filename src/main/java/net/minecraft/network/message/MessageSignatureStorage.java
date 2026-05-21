package net.minecraft.network.message;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

/**
 * Класс message signature storage.
 */
public class MessageSignatureStorage {

	public static final int MISSING = -1;
	private static final int MAX_ENTRIES = 128;
	private final @Nullable MessageSignatureData[] signatures;

	public MessageSignatureStorage(int maxEntries) {
		this.signatures = new MessageSignatureData[maxEntries];
	}

	/**
	 * Create.
	 *
	 * @return MessageSignatureStorage — результат операции
	 */
	public static MessageSignatureStorage create() {
		return new MessageSignatureStorage(128);
	}

	/**
	 * Index of.
	 *
	 * @param signature signature
	 *
	 * @return int — результат операции
	 */
	public int indexOf(MessageSignatureData signature) {
		for (int i = 0; i < this.signatures.length; i++) {
			if (signature.equals(this.signatures[i])) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Get.
	 *
	 * @param index index
	 *
	 * @return @Nullable MessageSignatureData — 
	 */
	public @Nullable MessageSignatureData get(int index) {
		return this.signatures[index];
	}

	/**
	 * Add.
	 *
	 * @param body body
	 * @param signature signature
	 */
	public void add(MessageBody body, @Nullable MessageSignatureData signature) {
		List<MessageSignatureData> list = body.lastSeenMessages().entries();
		ArrayDeque<MessageSignatureData> arrayDeque = new ArrayDeque<>(list.size() + 1);
		arrayDeque.addAll(list);
		if (signature != null) {
			arrayDeque.add(signature);
		}

		this.addFrom(arrayDeque);
	}

	@VisibleForTesting
	void addFrom(List<MessageSignatureData> signatures) {
		this.addFrom(new ArrayDeque<>(signatures));
	}

	private void addFrom(ArrayDeque<MessageSignatureData> deque) {
		Set<MessageSignatureData> set = new ObjectOpenHashSet(deque);

		for (int i = 0; !deque.isEmpty() && i < this.signatures.length; i++) {
			MessageSignatureData messageSignatureData = this.signatures[i];
			this.signatures[i] = deque.removeLast();
			if (messageSignatureData != null && !set.contains(messageSignatureData)) {
				deque.addFirst(messageSignatureData);
			}
		}
	}
}
