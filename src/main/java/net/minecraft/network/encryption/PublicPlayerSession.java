package net.minecraft.network.encryption;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.message.MessageChain;
import net.minecraft.network.message.MessageVerifier;

import java.time.Duration;
import java.util.UUID;

/**
 * Публичная сессия игрока: идентификатор сессии и публичный ключ для верификации сообщений чата.
 */
public record PublicPlayerSession(UUID sessionId, PlayerPublicKey publicKeyData) {

	/**
	 * Создаёт {@link MessageVerifier} для проверки подписей входящих сообщений чата.
	 *
	 * @param gracePeriod период отсрочки после истечения ключа, в течение которого он ещё принимается
	 */
	public MessageVerifier createVerifier(Duration gracePeriod) {
		return new MessageVerifier.Impl(
				publicKeyData.createSignatureInstance(),
				() -> publicKeyData.data().isExpired(gracePeriod)
		);
	}

	public MessageChain.Unpacker createUnpacker(UUID sender) {
		return new MessageChain(sender, sessionId).getUnpacker(publicKeyData);
	}

	public PublicPlayerSession.Serialized toSerialized() {
		return new PublicPlayerSession.Serialized(sessionId, publicKeyData.data());
	}

	public boolean isKeyExpired() {
		return publicKeyData.data().isExpired();
	}

	/**
	 * Сериализованное представление сессии для передачи по сети.
	 */
	public record Serialized(UUID sessionId, PlayerPublicKey.PublicKeyData publicKeyData) {

		public static PublicPlayerSession.Serialized fromBuf(PacketByteBuf buf) {
			return new PublicPlayerSession.Serialized(buf.readUuid(), new PlayerPublicKey.PublicKeyData(buf));
		}

		public static void write(PacketByteBuf buf, PublicPlayerSession.Serialized serialized) {
			buf.writeUuid(serialized.sessionId);
			serialized.publicKeyData.write(buf);
		}

		public PublicPlayerSession toSession(GameProfile gameProfile, SignatureVerifier servicesSignatureVerifier)
		throws PlayerPublicKey.PublicKeyException {
			return new PublicPlayerSession(
					sessionId,
					PlayerPublicKey.verifyAndDecode(servicesSignatureVerifier, gameProfile.id(), publicKeyData)
			);
		}
	}
}
