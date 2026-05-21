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
 * Запись message signature data.
 */
public record MessageSignatureData(byte[] data) {

	public static final Codec<MessageSignatureData>
			CODEC =
			Codecs.BASE_64.xmap(MessageSignatureData::new, MessageSignatureData::data);
	public static final int SIZE = 256;

	public MessageSignatureData(byte[] data) {
		Preconditions.checkState(data.length == 256, "Invalid message signature size");
		this.data = data;
	}

	/**
	 * From buf.
	 *
	 * @param buf buf
	 *
	 * @return MessageSignatureData — результат операции
	 */
	public static MessageSignatureData fromBuf(PacketByteBuf buf) {
		byte[] bs = new byte[256];
		buf.readBytes(bs);
		return new MessageSignatureData(bs);
	}

	/**
	 * Write.
	 *
	 * @param buf buf
	 * @param signature signature
	 */
	public static void write(PacketByteBuf buf, MessageSignatureData signature) {
		buf.writeBytes(signature.data);
	}

	/**
	 * Verify.
	 *
	 * @param verifier verifier
	 * @param updatable updatable
	 *
	 * @return boolean — результат операции
	 */
	public boolean verify(SignatureVerifier verifier, SignatureUpdatable updatable) {
		return verifier.validate(updatable, this.data);
	}

	/**
	 * To byte buffer.
	 *
	 * @return ByteBuffer — результат операции
	 */
	public ByteBuffer toByteBuffer() {
		return ByteBuffer.wrap(this.data);
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof MessageSignatureData messageSignatureData && Arrays.equals(
				this.data,
				messageSignatureData.data
		);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.data);
	}

	@Override
	public String toString() {
		return Base64.getEncoder().encodeToString(this.data);
	}

	/**
	 * To string.
	 *
	 * @param signature signature
	 *
	 * @return String — результат операции
	 */
	public static String toString(@Nullable MessageSignatureData signature) {
		return signature == null ? "<no signature>" : signature.toString();
	}

	public MessageSignatureData.Indexed pack(MessageSignatureStorage storage) {
		int i = storage.indexOf(this);
		return i != -1 ? new MessageSignatureData.Indexed(i) : new MessageSignatureData.Indexed(this);
	}

	/**
	 * Вычисляет checksum.
	 *
	 * @return int — результат операции
	 */
	public int calculateChecksum() {
		return Arrays.hashCode(this.data);
	}

	/**
	 * Запись indexed.
	 */
	public record Indexed(int id, @Nullable MessageSignatureData fullSignature) {

		public static final int MISSING_ID = -1;

		public Indexed(MessageSignatureData signature) {
			this(-1, signature);
		}

		public Indexed(int id) {
			this(id, null);
		}

		public static MessageSignatureData.Indexed fromBuf(PacketByteBuf buf) {
			int i = buf.readVarInt() - 1;
			return i == -1 ? new MessageSignatureData.Indexed(MessageSignatureData.fromBuf(buf))
			               : new MessageSignatureData.Indexed(i);
		}

		/**
		 * Write.
		 *
		 * @param buf buf
		 * @param indexed indexed
		 */
		public static void write(PacketByteBuf buf, MessageSignatureData.Indexed indexed) {
			buf.writeVarInt(indexed.id() + 1);
			if (indexed.fullSignature() != null) {
				MessageSignatureData.write(buf, indexed.fullSignature());
			}
		}

		public Optional<MessageSignatureData> getSignature(MessageSignatureStorage storage) {
			return this.fullSignature != null ? Optional.of(this.fullSignature)
			                                  : Optional.ofNullable(storage.get(this.id));
		}
	}
}
