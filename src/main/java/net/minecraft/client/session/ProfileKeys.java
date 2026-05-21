package net.minecraft.client.session;

import com.mojang.authlib.minecraft.UserApiService;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.encryption.PlayerKeyPair;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
/**
 * {@code ProfileKeys}.
 */
public interface ProfileKeys {

	ProfileKeys MISSING = new ProfileKeys() {
		@Override
		public CompletableFuture<Optional<PlayerKeyPair>> fetchKeyPair() {
			return CompletableFuture.completedFuture(Optional.empty());
		}

		@Override
		public boolean isExpired() {
			return false;
		}
	};

	static ProfileKeys create(UserApiService userApiService, Session session, Path root) {
		return new ProfileKeysImpl(userApiService, session.getUuidOrNull(), root);
	}

	CompletableFuture<Optional<PlayerKeyPair>> fetchKeyPair();

	boolean isExpired();
}
