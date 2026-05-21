package net.minecraft.client.session;

import com.google.common.base.Strings;
import com.mojang.authlib.minecraft.InsecurePublicKeyException.MissingException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse.KeyPair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.network.encryption.NetworkEncryptionException;
import net.minecraft.network.encryption.NetworkEncryptionUtils;
import net.minecraft.network.encryption.PlayerKeyPair;
import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
/**
 * {@code ProfileKeysImpl}.
 */
public class ProfileKeysImpl implements ProfileKeys {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Duration TIME_UNTIL_FIRST_EXPIRY_CHECK = Duration.ofHours(1L);
	private static final Path PROFILE_KEYS_PATH = Path.of("profilekeys");
	private final UserApiService userApiService;
	private final Path jsonPath;
	private CompletableFuture<Optional<PlayerKeyPair>> keyFuture = CompletableFuture.completedFuture(Optional.empty());
	private Instant expiryCheckTime = Instant.EPOCH;

	public ProfileKeysImpl(UserApiService userApiService, UUID uuid, Path root) {
		this.userApiService = userApiService;
		this.jsonPath = root.resolve(PROFILE_KEYS_PATH).resolve(uuid + ".json");
	}

	@Override
	public CompletableFuture<Optional<PlayerKeyPair>> fetchKeyPair() {
		this.expiryCheckTime = Instant.now().plus(TIME_UNTIL_FIRST_EXPIRY_CHECK);
		this.keyFuture = this.keyFuture.thenCompose(this::getKeyPair);
		return this.keyFuture;
	}

	@Override
	public boolean isExpired() {
		return this.keyFuture.isDone() && Instant.now().isAfter(this.expiryCheckTime)
		       ? this.keyFuture.join().map(PlayerKeyPair::needsRefreshing).orElse(true)
		       : false;
	}

	private CompletableFuture<Optional<PlayerKeyPair>> getKeyPair(Optional<PlayerKeyPair> currentKey) {
		return CompletableFuture.supplyAsync(
				() -> {
					if (currentKey.isPresent() && !currentKey.get().needsRefreshing()) {
						if (!SharedConstants.isDevelopment) {
							this.saveKeyPairToFile(null);
						}

						return currentKey;
					}
					else {
						//            try {
						//               PlayerKeyPair playerKeyPair = this.fetchKeyPair(this.userApiService);
						//               this.saveKeyPairToFile(playerKeyPair);
						//               return Optional.ofNullable(playerKeyPair);
						//            } catch (NetworkEncryptionException | MinecraftClientException | IOException var3) {
						//               LOGGER.error("Failed to retrieve profile key pair", var3);
						//               this.saveKeyPairToFile(null);
						//               return currentKey;
						//            }
						return currentKey;
					}
				}, Util.getDownloadWorkerExecutor()
		);
	}

	private Optional<PlayerKeyPair> loadKeyPairFromFile() {
		if (Files.notExists(this.jsonPath)) {
			return Optional.empty();
		}
		else {
			try {
				Optional var2;
				try (BufferedReader bufferedReader = Files.newBufferedReader(this.jsonPath)) {
					var2 = PlayerKeyPair.CODEC.parse(JsonOps.INSTANCE, StrictJsonParser.parse(bufferedReader)).result();
				}

				return var2;
			}
			catch (Exception var6) {
				LOGGER.error("Failed to read profile key pair file {}", this.jsonPath, var6);
				return Optional.empty();
			}
		}
	}

	private void saveKeyPairToFile(@Nullable PlayerKeyPair keyPair) {
		try {
			Files.deleteIfExists(this.jsonPath);
		}
		catch (IOException var3) {
			LOGGER.error("Failed to delete profile key pair file {}", this.jsonPath, var3);
		}

		if (keyPair != null) {
			if (SharedConstants.isDevelopment) {
				PlayerKeyPair.CODEC.encodeStart(JsonOps.INSTANCE, keyPair).ifSuccess(json -> {
					try {
						Files.createDirectories(this.jsonPath.getParent());
						Files.writeString(this.jsonPath, json.toString());
					}
					catch (Exception var3x) {
						LOGGER.error("Failed to write profile key pair file {}", this.jsonPath, var3x);
					}
				});
			}
		}
	}

	private @Nullable PlayerKeyPair fetchKeyPair(UserApiService userApiService)
	throws NetworkEncryptionException, IOException {
		KeyPairResponse keyPairResponse = userApiService.getKeyPair();
		if (keyPairResponse != null) {
			PlayerPublicKey.PublicKeyData publicKeyData = decodeKeyPairResponse(keyPairResponse);
			return new PlayerKeyPair(
					NetworkEncryptionUtils.decodeRsaPrivateKeyPem(keyPairResponse.keyPair().privateKey()),
					new PlayerPublicKey(publicKeyData),
					Instant.parse(keyPairResponse.refreshedAfter())
			);
		}
		else {
			return null;
		}
	}

	private static PlayerPublicKey.PublicKeyData decodeKeyPairResponse(KeyPairResponse keyPairResponse)
	throws NetworkEncryptionException {
		KeyPair keyPair = keyPairResponse.keyPair();
		if (keyPair != null
				&& !Strings.isNullOrEmpty(keyPair.publicKey())
				&& keyPairResponse.publicKeySignature() != null
				&& keyPairResponse.publicKeySignature().array().length != 0) {
			try {
				Instant instant = Instant.parse(keyPairResponse.expiresAt());
				PublicKey publicKey = NetworkEncryptionUtils.decodeRsaPublicKeyPem(keyPair.publicKey());
				ByteBuffer byteBuffer = keyPairResponse.publicKeySignature();
				return new PlayerPublicKey.PublicKeyData(instant, publicKey, byteBuffer.array());
			}
			catch (IllegalArgumentException | DateTimeException var5) {
				throw new NetworkEncryptionException(var5);
			}
		}
		else {
			throw new NetworkEncryptionException(new MissingException("Missing public key"));
		}
	}
}
