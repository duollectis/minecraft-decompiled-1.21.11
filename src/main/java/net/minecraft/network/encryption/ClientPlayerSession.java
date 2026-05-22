package net.minecraft.network.encryption;

import net.minecraft.network.message.MessageChain;

import java.util.UUID;

/**
 * Сессия игрока на стороне клиента: содержит уникальный идентификатор сессии
 * и пару ключей для подписи сообщений чата.
 */
public record ClientPlayerSession(UUID sessionId, PlayerKeyPair keyPair) {

	public static ClientPlayerSession create(PlayerKeyPair keyPair) {
		return new ClientPlayerSession(UUID.randomUUID(), keyPair);
	}

	/**
	 * Создаёт {@link MessageChain.Packer} для подписи исходящих сообщений чата
	 * от имени указанного отправителя.
	 */
	public MessageChain.Packer createPacker(UUID sender) {
		return new MessageChain(sender, sessionId).getPacker(Signer.create(
				keyPair.privateKey(),
				"SHA256withRSA"
		));
	}

	public PublicPlayerSession toPublicSession() {
		return new PublicPlayerSession(sessionId, keyPair.publicKey());
	}
}
