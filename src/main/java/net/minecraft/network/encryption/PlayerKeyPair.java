package net.minecraft.network.encryption;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.Codecs;

import java.security.PrivateKey;
import java.time.Instant;

/**
 * Пара ключей игрока: приватный ключ RSA, публичный ключ с подписью и время обновления.
 */
public record PlayerKeyPair(PrivateKey privateKey, PlayerPublicKey publicKey, Instant refreshedAfter) {

	public static final Codec<PlayerKeyPair> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
					NetworkEncryptionUtils.RSA_PRIVATE_KEY_CODEC
							.fieldOf("private_key")
							.forGetter(PlayerKeyPair::privateKey),
					PlayerPublicKey.CODEC.fieldOf("public_key").forGetter(PlayerKeyPair::publicKey),
					Codecs.INSTANT.fieldOf("refreshed_after").forGetter(PlayerKeyPair::refreshedAfter)
			).apply(instance, PlayerKeyPair::new)
	);

	public boolean needsRefreshing() {
		return refreshedAfter.isBefore(Instant.now());
	}
}
