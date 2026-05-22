package net.minecraft.network.message;

import com.google.common.primitives.Ints;
import com.mojang.serialization.Codec;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.encryption.SignatureUpdatable;

import java.security.SignatureException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

/**
 * Список подписей последних просмотренных сообщений, используемый для цепочки подписей чата.
 * Передаётся в теле каждого подписанного сообщения для верификации порядка сообщений.
 */
public record LastSeenMessageList(List<MessageSignatureData> entries) {

	public static final Codec<LastSeenMessageList> CODEC = MessageSignatureData.CODEC
			.listOf()
			.xmap(LastSeenMessageList::new, LastSeenMessageList::entries);

	public static LastSeenMessageList EMPTY = new LastSeenMessageList(List.of());
	public static final int MAX_ENTRIES = 20;

	/**
	 * Добавляет подписи всех записей в обновляемый объект подписи.
	 * Сначала записывается размер списка, затем байты каждой подписи.
	 *
	 * @param updater объект для накопления данных подписи
	 * @throws SignatureException при ошибке криптографической операции
	 */
	public void updateSignatures(SignatureUpdatable.SignatureUpdater updater) throws SignatureException {
		updater.update(Ints.toByteArray(entries.size()));

		for (MessageSignatureData signature : entries) {
			updater.update(signature.data());
		}
	}

	public LastSeenMessageList.Indexed pack(MessageSignatureStorage storage) {
		return new LastSeenMessageList.Indexed(
				entries.stream()
						.map(signature -> signature.pack(storage))
						.toList()
		);
	}

	/**
	 * Вычисляет однобайтовую контрольную сумму списка для быстрой проверки синхронизации.
	 * Никогда не возвращает {@code 0} — заменяет его на {@code 1}, чтобы отличить от «нет суммы».
	 *
	 * @return ненулевой байт контрольной суммы
	 */
	public byte calculateChecksum() {
		int hash = 1;

		for (MessageSignatureData signature : entries) {
			hash = 31 * hash + signature.calculateChecksum();
		}

		byte checksum = (byte) hash;
		return checksum == 0 ? 1 : checksum;
	}

	/**
	 * Пакет подтверждения от клиента: смещение окна, битовая маска подтверждённых сообщений
	 * и контрольная сумма для проверки синхронизации.
	 */
	public record Acknowledgment(int offset, BitSet acknowledged, byte checksum) {

		public static final byte NO_CHECKSUM = 0;

		public Acknowledgment(PacketByteBuf buf) {
			this(buf.readVarInt(), buf.readBitSet(MAX_ENTRIES), buf.readByte());
		}

		public void write(PacketByteBuf buf) {
			buf.writeVarInt(offset);
			buf.writeBitSet(acknowledged, MAX_ENTRIES);
			buf.writeByte(checksum);
		}

		/**
		 * Проверяет контрольную сумму. Значение {@link #NO_CHECKSUM} ({@code 0}) трактуется
		 * как «проверка отключена» и всегда считается корректным.
		 *
		 * @param lastSeenMessages список сообщений для сравнения
		 * @return {@code true} если сумма совпадает или равна {@link #NO_CHECKSUM}
		 */
		public boolean checksumEquals(LastSeenMessageList lastSeenMessages) {
			return checksum == NO_CHECKSUM || checksum == lastSeenMessages.calculateChecksum();
		}
	}

	/**
	 * Компактное представление списка, где уже известные серверу подписи заменены индексами
	 * в хранилище {@link MessageSignatureStorage}.
	 */
	public record Indexed(List<MessageSignatureData.Indexed> buf) {

		public static final LastSeenMessageList.Indexed EMPTY = new LastSeenMessageList.Indexed(List.of());

		public Indexed(PacketByteBuf buf) {
			this(buf.<MessageSignatureData.Indexed, ArrayList<MessageSignatureData.Indexed>>readCollection(
					PacketByteBuf.getMaxValidator(ArrayList::new, MAX_ENTRIES),
					MessageSignatureData.Indexed::fromBuf
			));
		}

		public void write(PacketByteBuf buf) {
			buf.writeCollection(this.buf, MessageSignatureData.Indexed::write);
		}

		/**
		 * Разворачивает компактный список обратно в полные подписи, используя хранилище.
		 *
		 * @param storage хранилище известных подписей
		 * @return полный список подписей, или {@link Optional#empty()} если хотя бы одна подпись не найдена
		 */
		public Optional<LastSeenMessageList> unpack(MessageSignatureStorage storage) {
			List<MessageSignatureData> signatures = new ArrayList<>(this.buf.size());

			for (MessageSignatureData.Indexed indexed : this.buf) {
				Optional<MessageSignatureData> signature = indexed.getSignature(storage);

				if (signature.isEmpty()) {
					return Optional.empty();
				}

				signatures.add(signature.get());
			}

			return Optional.of(new LastSeenMessageList(signatures));
		}
	}
}
