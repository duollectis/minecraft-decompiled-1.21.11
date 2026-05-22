package net.minecraft.network.encryption;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.TextifiedException;
import net.minecraft.util.dynamic.Codecs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Публичный ключ игрока с подписью от Mojang Services, используемый для верификации сообщений чата.
 */
public record PlayerPublicKey(PlayerPublicKey.PublicKeyData data) {

	public static final Text EXPIRED_PUBLIC_KEY_TEXT = Text.translatable("multiplayer.disconnect.expired_public_key");
	private static final Text INVALID_PUBLIC_KEY_SIGNATURE_TEXT =
			Text.translatable("multiplayer.disconnect.invalid_public_key_signature");
	public static final Duration EXPIRATION_GRACE_PERIOD = Duration.ofHours(8L);
	public static final Codec<PlayerPublicKey> CODEC =
			PlayerPublicKey.PublicKeyData.CODEC.xmap(PlayerPublicKey::new, PlayerPublicKey::data);

	/**
	 * Верифицирует подпись публичного ключа через Mojang Services и возвращает {@link PlayerPublicKey}.
	 *
	 * @throws PublicKeyException если подпись недействительна
	 */
	public static PlayerPublicKey verifyAndDecode(
			SignatureVerifier servicesSignatureVerifier,
			UUID playerUuid,
			PlayerPublicKey.PublicKeyData publicKeyData
	) throws PlayerPublicKey.PublicKeyException {
		if (!publicKeyData.verifyKey(servicesSignatureVerifier, playerUuid)) {
			throw new PlayerPublicKey.PublicKeyException(INVALID_PUBLIC_KEY_SIGNATURE_TEXT);
		}

		return new PlayerPublicKey(publicKeyData);
	}

	public SignatureVerifier createSignatureInstance() {
		return SignatureVerifier.create(data.key, "SHA256withRSA");
	}

	/**
	 * Данные публичного ключа: время истечения, сам ключ и подпись Mojang Services.
	 */
	public record PublicKeyData(Instant expiresAt, PublicKey key, byte[] keySignature) {

		private static final int KEY_SIGNATURE_MAX_SIZE = 4096;
		private static final int UUID_BYTES = 16;
		private static final int EPOCH_MILLIS_BYTES = 8;
		private static final int HEADER_SIZE = UUID_BYTES + EPOCH_MILLIS_BYTES;

		public static final Codec<PlayerPublicKey.PublicKeyData> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
						Codecs.INSTANT.fieldOf("expires_at").forGetter(PlayerPublicKey.PublicKeyData::expiresAt),
						NetworkEncryptionUtils.RSA_PUBLIC_KEY_CODEC
								.fieldOf("key")
								.forGetter(PlayerPublicKey.PublicKeyData::key),
						Codecs.BASE_64.fieldOf("signature_v2").forGetter(PlayerPublicKey.PublicKeyData::keySignature)
				).apply(instance, PlayerPublicKey.PublicKeyData::new)
		);

		public PublicKeyData(PacketByteBuf buf) {
			this(buf.readInstant(), buf.readPublicKey(), buf.readByteArray(KEY_SIGNATURE_MAX_SIZE));
		}

		public void write(PacketByteBuf buf) {
			buf.writeInstant(expiresAt);
			buf.writePublicKey(key);
			buf.writeByteArray(keySignature);
		}

		boolean verifyKey(SignatureVerifier servicesSignatureVerifier, UUID playerUuid) {
			return servicesSignatureVerifier.validate(toSerializedString(playerUuid), keySignature);
		}

		/**
		 * Сериализует данные ключа в байтовый массив для верификации подписи:
		 * UUID (16 байт) + expiresAt (8 байт) + encoded public key.
		 */
		private byte[] toSerializedString(UUID playerUuid) {
			byte[] encodedKey = key.getEncoded();
			byte[] result = new byte[HEADER_SIZE + encodedKey.length];
			ByteBuffer.wrap(result)
					.order(ByteOrder.BIG_ENDIAN)
					.putLong(playerUuid.getMostSignificantBits())
					.putLong(playerUuid.getLeastSignificantBits())
					.putLong(expiresAt.toEpochMilli())
					.put(encodedKey);
			return result;
		}

		public boolean isExpired() {
			return expiresAt.isBefore(Instant.now());
		}

		public boolean isExpired(Duration gracePeriod) {
			return expiresAt.plus(gracePeriod).isBefore(Instant.now());
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof PlayerPublicKey.PublicKeyData publicKeyData)) {
				return false;
			}

			return expiresAt.equals(publicKeyData.expiresAt)
					&& key.equals(publicKeyData.key)
					&& Arrays.equals(keySignature, publicKeyData.keySignature);
		}
	}

	public static class PublicKeyException extends TextifiedException {

		public PublicKeyException(Text text) {
			super(text);
		}
	}
}
