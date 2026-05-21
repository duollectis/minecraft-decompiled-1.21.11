package net.minecraft.network.encryption;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.message.MessageChain;
import net.minecraft.network.message.MessageVerifier;

import java.time.Duration;
import java.util.UUID;

/**
 * Запись public player session.
 */
public record PublicPlayerSession(UUID sessionId, PlayerPublicKey publicKeyData) {

	/**
	 * Создаёт verifier.
	 *
	 * @param gracePeriod grace period
	 *
	 * @return MessageVerifier — результат операции
	 */
	public MessageVerifier createVerifier(Duration gracePeriod) {
		return new MessageVerifier.Impl(
				this.publicKeyData.createSignatureInstance(),
				() -> this.publicKeyData.data().isExpired(gracePeriod)
		);
	}

	public MessageChain.Unpacker createUnpacker(UUID sender) {
		return new MessageChain(sender, this.sessionId).getUnpacker(this.publicKeyData);
	}

	public PublicPlayerSession.Serialized toSerialized() {
		return new PublicPlayerSession.Serialized(this.sessionId, this.publicKeyData.data());
	}

	public boolean isKeyExpired() {
		return this.publicKeyData.data().isExpired();
	}

	/**
	 * Запись serialized.
	 */
	public record Serialized(UUID sessionId, PlayerPublicKey.PublicKeyData publicKeyData) {

		public static PublicPlayerSession.Serialized fromBuf(PacketByteBuf buf) {
			return new PublicPlayerSession.Serialized(buf.readUuid(), new PlayerPublicKey.PublicKeyData(buf));
		}

		/**
		 * Write.
		 *
		 * @param buf buf
		 * @param serialized serialized
		 */
		public static void write(PacketByteBuf buf, PublicPlayerSession.Serialized serialized) {
			buf.writeUuid(serialized.sessionId);
			serialized.publicKeyData.write(buf);
		}

		public PublicPlayerSession toSession(GameProfile gameProfile, SignatureVerifier servicesSignatureVerifier)
		throws PlayerPublicKey.PublicKeyException {
			return new PublicPlayerSession(
					this.sessionId,
					PlayerPublicKey.verifyAndDecode(servicesSignatureVerifier, gameProfile.id(), this.publicKeyData)
			);
		}
	}
}
