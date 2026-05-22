package net.minecraft.network.message;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.encryption.SignatureUpdatable;
import net.minecraft.network.encryption.SignatureVerifier;
import net.minecraft.util.dynamic.Codecs;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Криптографическая подпись чат-сообщения — массив из {@value #SIZE} байт RSA-подписи.
 * Используется для верификации подлинности и порядка сообщений в цепочке.
 */
public record MessageSignatureData(byte[] data) {

	public static final Codec<MessageSignatureData> CODEC = Codecs.BASE_64
			.xmap(MessageSignatureData::new, MessageSignatureData::data);

	public static final int SIZE = 256;

	public MessageSignatureData(byte[] data) {
		Preconditions.checkState(data.length == SIZE, "Invalid message signature size");
		this.data = data;
	}

	public static MessageSignatureData fromBuf(PacketByteBuf buf) {
		byte[] bytes = new byte[SIZE];
		buf.readBytes(bytes);
		return new MessageSignatureData(bytes);
	}

	public static void write(PacketByteBuf buf, MessageSignatureData signature) {
		buf.writeBytes(signature.data);
	}

	public boolean verify(SignatureVerifier verifier, SignatureUpdatable updatable) {
		return verifier.validate(updatable, data);
	}

	public ByteBuffer toByteBuffer() {
		return ByteBuffer.wrap(data);
	}

	@Override
	public boolean equals(Object o) {
		return this == o
				|| o instanceof MessageSignatureData other && Arrays.equals(data, other.data);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(data);
	}

	@Override
	public String toString() {
		return Base64.getEncoder().encodeToString(data);
	}

	public static String toString(@Nullable MessageSignatureData signature) {
		return signature == null ? "<no signature>" : signature.toString();
	}

	/**
	 * Упаковывает подпись в компактный индексированный вид.
	 * Если подпись уже есть в хранилище, сохраняется только её индекс.
	 *
	 * @param storage хранилище известных подписей
	 * @return индексированное представление подписи
	 */
	public MessageSignatureData.Indexed pack(MessageSignatureStorage storage) {
		int storageIndex = storage.indexOf(this);
		return storageIndex != MessageSignatureData.Indexed.MISSING_ID
				? new MessageSignatureData.Indexed(storageIndex)
				: new MessageSignatureData.Indexed(this);
	}

	public int calculateChecksum() {
		return Arrays.hashCode(data);
	}

	/**
	 * Компактное представление подписи для передачи по сети.
	 * Если подпись известна серверу, хранится только её индекс в хранилище.
	 * Иначе — полные байты подписи.
	 */
	public record Indexed(int id, @Nullable MessageSignatureData fullSignature) {

		public static final int MISSING_ID = -1;

		public Indexed(MessageSignatureData signature) {
			this(MISSING_ID, signature);
		}

		public Indexed(int id) {
			this(id, null);
		}

		/**
		 * Читает индексированную подпись из буфера.
		 * Протокол: VarInt (id + 1), где 0 означает «полная подпись следует».
		 *
		 * @param buf буфер для чтения
		 * @return индексированная подпись
		 */
		public static MessageSignatureData.Indexed fromBuf(PacketByteBuf buf) {
			int encodedId = buf.readVarInt() - 1;
			return encodedId == MISSING_ID
					? new MessageSignatureData.Indexed(MessageSignatureData.fromBuf(buf))
					: new MessageSignatureData.Indexed(encodedId);
		}

		/**
		 * Записывает индексированную подпись в буфер.
		 * Если полная подпись присутствует, записывает 0 + байты подписи.
		 * Иначе записывает (id + 1).
		 *
		 * @param buf     буфер для записи
		 * @param indexed индексированная подпись
		 */
		public static void write(PacketByteBuf buf, MessageSignatureData.Indexed indexed) {
			buf.writeVarInt(indexed.id() + 1);

			if (indexed.fullSignature() != null) {
				MessageSignatureData.write(buf, indexed.fullSignature());
			}
		}

		public Optional<MessageSignatureData> getSignature(MessageSignatureStorage storage) {
			return fullSignature != null
					? Optional.of(fullSignature)
					: Optional.ofNullable(storage.get(id));
		}
	}
}
