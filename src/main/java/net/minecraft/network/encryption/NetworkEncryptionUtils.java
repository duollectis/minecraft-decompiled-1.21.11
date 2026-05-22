package net.minecraft.network.encryption;

import com.google.common.primitives.Longs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import net.minecraft.network.PacketByteBuf;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Base64.Encoder;

/**
 * Утилитарный класс для криптографических операций сетевого протокола Minecraft:
 * генерация ключей AES/RSA, шифрование/дешифрование, кодирование ключей в PEM-формат.
 */
public class NetworkEncryptionUtils {

	private static final String AES = "AES";
	private static final int AES_KEY_LENGTH = 128;
	private static final String RSA = "RSA";
	private static final int RSA_KEY_LENGTH = 1024;
	private static final String ISO_8859_1 = "ISO_8859_1";
	private static final String SHA1 = "SHA-1";
	public static final String SHA256_WITH_RSA = "SHA256withRSA";
	public static final int SHA256_BITS = 256;
	private static final String RSA_PRIVATE_KEY_PREFIX = "-----BEGIN RSA PRIVATE KEY-----";
	private static final String RSA_PRIVATE_KEY_SUFFIX = "-----END RSA PRIVATE KEY-----";
	public static final String RSA_PUBLIC_KEY_PREFIX = "-----BEGIN RSA PUBLIC KEY-----";
	private static final String RSA_PUBLIC_KEY_SUFFIX = "-----END RSA PUBLIC KEY-----";
	public static final String LINEBREAK = "\n";
	public static final Encoder BASE64_ENCODER = Base64.getMimeEncoder(76, "\n".getBytes(StandardCharsets.UTF_8));

	public static final Codec<PublicKey> RSA_PUBLIC_KEY_CODEC = Codec.STRING.comapFlatMap(
			key -> {
				try {
					return DataResult.success(decodeRsaPublicKeyPem(key));
				} catch (NetworkEncryptionException e) {
					return DataResult.error(e::getMessage);
				}
			}, NetworkEncryptionUtils::encodeRsaPublicKey
	);

	public static final Codec<PrivateKey> RSA_PRIVATE_KEY_CODEC = Codec.STRING.comapFlatMap(
			key -> {
				try {
					return DataResult.success(decodeRsaPrivateKeyPem(key));
				} catch (NetworkEncryptionException e) {
					return DataResult.error(e::getMessage);
				}
			}, NetworkEncryptionUtils::encodeRsaPrivateKey
	);

	public static SecretKey generateSecretKey() throws NetworkEncryptionException {
		try {
			KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
			keyGenerator.init(AES_KEY_LENGTH);
			return keyGenerator.generateKey();
		} catch (Exception e) {
			throw new NetworkEncryptionException(e);
		}
	}

	public static KeyPair generateServerKeyPair() throws NetworkEncryptionException {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA);
			keyPairGenerator.initialize(RSA_KEY_LENGTH);
			return keyPairGenerator.generateKeyPair();
		} catch (Exception e) {
			throw new NetworkEncryptionException(e);
		}
	}

	/**
	 * Вычисляет хэш идентификатора сервера для аутентификации через Mojang API.
	 * Использует SHA-1 от конкатенации baseServerId (ISO-8859-1), секретного ключа и публичного ключа.
	 */
	public static byte[] computeServerId(String baseServerId, PublicKey publicKey, SecretKey secretKey)
	throws NetworkEncryptionException {
		try {
			return hash(baseServerId.getBytes(ISO_8859_1), secretKey.getEncoded(), publicKey.getEncoded());
		} catch (Exception e) {
			throw new NetworkEncryptionException(e);
		}
	}

	private static byte[] hash(byte[]... parts) throws Exception {
		MessageDigest digest = MessageDigest.getInstance(SHA1);

		for (byte[] part : parts) {
			digest.update(part);
		}

		return digest.digest();
	}

	private static <T extends Key> T decodePem(
			String key,
			String prefix,
			String suffix,
			NetworkEncryptionUtils.KeyDecoder<T> decoder
	) throws NetworkEncryptionException {
		int prefixIndex = key.indexOf(prefix);
		if (prefixIndex != -1) {
			prefixIndex += prefix.length();
			int suffixIndex = key.indexOf(suffix, prefixIndex);
			key = key.substring(prefixIndex, suffixIndex + 1);
		}

		try {
			return decoder.apply(Base64.getMimeDecoder().decode(key));
		} catch (IllegalArgumentException e) {
			throw new NetworkEncryptionException(e);
		}
	}

	public static PrivateKey decodeRsaPrivateKeyPem(String key) throws NetworkEncryptionException {
		return decodePem(
				key,
				RSA_PRIVATE_KEY_PREFIX,
				RSA_PRIVATE_KEY_SUFFIX,
				NetworkEncryptionUtils::decodeEncodedRsaPrivateKey
		);
	}

	public static PublicKey decodeRsaPublicKeyPem(String key) throws NetworkEncryptionException {
		return decodePem(
				key,
				RSA_PUBLIC_KEY_PREFIX,
				RSA_PUBLIC_KEY_SUFFIX,
				NetworkEncryptionUtils::decodeEncodedRsaPublicKey
		);
	}

	public static String encodeRsaPublicKey(PublicKey key) {
		if (!RSA.equals(key.getAlgorithm())) {
			throw new IllegalArgumentException("Public key must be RSA");
		}

		return RSA_PUBLIC_KEY_PREFIX + LINEBREAK
				+ BASE64_ENCODER.encodeToString(key.getEncoded())
				+ LINEBREAK + RSA_PUBLIC_KEY_SUFFIX + LINEBREAK;
	}

	public static String encodeRsaPrivateKey(PrivateKey key) {
		if (!RSA.equals(key.getAlgorithm())) {
			throw new IllegalArgumentException("Private key must be RSA");
		}

		return RSA_PRIVATE_KEY_PREFIX + LINEBREAK
				+ BASE64_ENCODER.encodeToString(key.getEncoded())
				+ LINEBREAK + RSA_PRIVATE_KEY_SUFFIX + LINEBREAK;
	}

	private static PrivateKey decodeEncodedRsaPrivateKey(byte[] key) throws NetworkEncryptionException {
		try {
			EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(key);
			KeyFactory keyFactory = KeyFactory.getInstance(RSA);
			return keyFactory.generatePrivate(keySpec);
		} catch (Exception e) {
			throw new NetworkEncryptionException(e);
		}
	}

	public static PublicKey decodeEncodedRsaPublicKey(byte[] key) throws NetworkEncryptionException {
		try {
			EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
			KeyFactory keyFactory = KeyFactory.getInstance(RSA);
			return keyFactory.generatePublic(keySpec);
		} catch (Exception e) {
			throw new NetworkEncryptionException(e);
		}
	}

	public static SecretKey decryptSecretKey(PrivateKey privateKey, byte[] encryptedSecretKey)
	throws NetworkEncryptionException {
		byte[] keyBytes = decrypt(privateKey, encryptedSecretKey);

		try {
			return new SecretKeySpec(keyBytes, AES);
		} catch (Exception e) {
			throw new NetworkEncryptionException(e);
		}
	}

	public static byte[] encrypt(Key key, byte[] data) throws NetworkEncryptionException {
		return crypt(Cipher.ENCRYPT_MODE, key, data);
	}

	public static byte[] decrypt(Key key, byte[] data) throws NetworkEncryptionException {
		return crypt(Cipher.DECRYPT_MODE, key, data);
	}

	private static byte[] crypt(int opMode, Key key, byte[] data) throws NetworkEncryptionException {
		try {
			return createCipher(opMode, key.getAlgorithm(), key).doFinal(data);
		} catch (Exception e) {
			throw new NetworkEncryptionException(e);
		}
	}

	private static Cipher createCipher(int opMode, String algorithm, Key key) throws Exception {
		Cipher cipher = Cipher.getInstance(algorithm);
		cipher.init(opMode, key);
		return cipher;
	}

	/**
	 * Создаёт шифр AES/CFB8 для потокового шифрования пакетов.
	 * В качестве IV используются первые 16 байт самого ключа.
	 */
	public static Cipher cipherFromKey(int opMode, Key key) throws NetworkEncryptionException {
		try {
			Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
			cipher.init(opMode, key, new IvParameterSpec(key.getEncoded()));
			return cipher;
		} catch (Exception e) {
			throw new NetworkEncryptionException(e);
		}
	}

	interface KeyDecoder<T extends Key> {

		T apply(byte[] key) throws NetworkEncryptionException;
	}

	public static class SecureRandomUtil {

		private static final SecureRandom SECURE_RANDOM = new SecureRandom();

		public static long nextLong() {
			return SECURE_RANDOM.nextLong();
		}
	}

	/**
	 * Данные подписи сообщения: соль и байты подписи RSA.
	 */
	public record SignatureData(long salt, byte[] signature) {

		public static final NetworkEncryptionUtils.SignatureData NONE =
				new NetworkEncryptionUtils.SignatureData(0L, ByteArrays.EMPTY_ARRAY);

		public SignatureData(PacketByteBuf buf) {
			this(buf.readLong(), buf.readByteArray());
		}

		public boolean isSignaturePresent() {
			return signature.length > 0;
		}

		public static void write(PacketByteBuf buf, NetworkEncryptionUtils.SignatureData signatureData) {
			buf.writeLong(signatureData.salt);
			buf.writeByteArray(signatureData.signature);
		}

		public byte[] getSalt() {
			return Longs.toByteArray(salt);
		}
	}
}
