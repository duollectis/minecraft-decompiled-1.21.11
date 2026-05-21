package net.minecraft.network.encryption;

import net.minecraft.network.message.MessageChain;

import java.util.UUID;

/**
 * Запись client player session.
 */
public record ClientPlayerSession(UUID sessionId, PlayerKeyPair keyPair) {

	/**
	 * Create.
	 *
	 * @param keyPair key pair
	 *
	 * @return ClientPlayerSession — результат операции
	 */
	public static ClientPlayerSession create(PlayerKeyPair keyPair) {
		return new ClientPlayerSession(UUID.randomUUID(), keyPair);
	}

	public MessageChain.Packer createPacker(UUID sender) {
		return new MessageChain(sender, this.sessionId).getPacker(Signer.create(
				this.keyPair.privateKey(),
				"SHA256withRSA"
		));
	}

	/**
	 * To public session.
	 *
	 * @return PublicPlayerSession — результат операции
	 */
	public PublicPlayerSession toPublicSession() {
		return new PublicPlayerSession(this.sessionId, this.keyPair.publicKey());
	}
}
