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
 * Запись last seen message list.
 */
public record LastSeenMessageList(List<MessageSignatureData> entries) {

	public static final Codec<LastSeenMessageList>
			CODEC =
			MessageSignatureData.CODEC.listOf().xmap(LastSeenMessageList::new, LastSeenMessageList::entries);
	public static LastSeenMessageList EMPTY = new LastSeenMessageList(List.of());
	public static final int MAX_ENTRIES = 20;

	/**
	 * Обновляет signatures.
	 *
	 * @param updater updater
	 */
	public void updateSignatures(SignatureUpdatable.SignatureUpdater updater) throws SignatureException {
		updater.update(Ints.toByteArray(this.entries.size()));

		for (MessageSignatureData messageSignatureData : this.entries) {
			updater.update(messageSignatureData.data());
		}
	}

	public LastSeenMessageList.Indexed pack(MessageSignatureStorage storage) {
		return new LastSeenMessageList.Indexed(this.entries
				.stream()
				.map(signature -> signature.pack(storage))
				.toList());
	}

	/**
	 * Вычисляет checksum.
	 *
	 * @return byte — результат операции
	 */
	public byte calculateChecksum() {
		int i = 1;

		for (MessageSignatureData messageSignatureData : this.entries) {
			i = 31 * i + messageSignatureData.calculateChecksum();
		}

		byte b = (byte) i;
		return b == 0 ? 1 : b;
	}

	/**
	 * Запись acknowledgment.
	 */
	public record Acknowledgment(int offset, BitSet acknowledged, byte checksum) {

		public static final byte NO_CHECKSUM = 0;

		public Acknowledgment(PacketByteBuf buf) {
			this(buf.readVarInt(), buf.readBitSet(20), buf.readByte());
		}

		/**
		 * Write.
		 *
		 * @param buf buf
		 */
		public void write(PacketByteBuf buf) {
			buf.writeVarInt(this.offset);
			buf.writeBitSet(this.acknowledged, 20);
			buf.writeByte(this.checksum);
		}

		/**
		 * Проверяет sum equals.
		 *
		 * @param lastSeenMessages last seen messages
		 *
		 * @return boolean — результат операции
		 */
		public boolean checksumEquals(LastSeenMessageList lastSeenMessages) {
			return this.checksum == 0 || this.checksum == lastSeenMessages.calculateChecksum();
		}
	}

	/**
	 * Запись indexed.
	 */
	public record Indexed(List<MessageSignatureData.Indexed> buf) {

		public static final LastSeenMessageList.Indexed EMPTY = new LastSeenMessageList.Indexed(List.of());

		public Indexed(PacketByteBuf buf) {
			this(buf.<MessageSignatureData.Indexed, ArrayList<MessageSignatureData.Indexed>>readCollection(
					PacketByteBuf.getMaxValidator(ArrayList::new, 20),
					MessageSignatureData.Indexed::fromBuf
			));
		}

		/**
		 * Write.
		 *
		 * @param buf buf
		 */
		public void write(PacketByteBuf buf) {
			buf.writeCollection(this.buf, MessageSignatureData.Indexed::write);
		}

		/**
		 * Unpack.
		 *
		 * @param storage storage
		 *
		 * @return Optional — результат операции
		 */
		public Optional<LastSeenMessageList> unpack(MessageSignatureStorage storage) {
			List<MessageSignatureData> list = new ArrayList<>(this.buf.size());

			for (MessageSignatureData.Indexed indexed : this.buf) {
				Optional<MessageSignatureData> optional = indexed.getSignature(storage);
				if (optional.isEmpty()) {
					return Optional.empty();
				}

				list.add(optional.get());
			}

			return Optional.of(new LastSeenMessageList(list));
		}
	}
}
