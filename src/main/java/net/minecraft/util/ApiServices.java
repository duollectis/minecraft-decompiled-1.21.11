package net.minecraft.util;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.network.encryption.SignatureVerifier;
import net.minecraft.server.GameProfileResolver;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * {@code ApiServices}.
 */
public record ApiServices(
		MinecraftSessionService sessionService,
		ServicesKeySet servicesKeySet,
		GameProfileRepository profileRepository,
		NameToIdCache nameToIdCache,
		GameProfileResolver profileResolver
) {

	private static final String USER_CACHE_FILE_NAME = "usercache.json";

	/**
	 * Create.
	 *
	 * @param authenticationService authentication service
	 * @param rootDirectory root directory
	 *
	 * @return ApiServices — результат операции
	 */
	public static ApiServices create(YggdrasilAuthenticationService authenticationService, File rootDirectory) {
		MinecraftSessionService minecraftSessionService = authenticationService.createMinecraftSessionService();
		GameProfileRepository gameProfileRepository = authenticationService.createProfileRepository();
		NameToIdCache nameToIdCache = new UserCache(gameProfileRepository, new File(rootDirectory, "usercache.json"));
		GameProfileResolver
				gameProfileResolver =
				new GameProfileResolver.CachedSessionProfileResolver(minecraftSessionService, nameToIdCache);
		return new ApiServices(
				minecraftSessionService,
				authenticationService.getServicesKeySet(),
				gameProfileRepository,
				nameToIdCache,
				gameProfileResolver
		);
	}

	/**
	 * Service signature verifier.
	 *
	 * @return @Nullable SignatureVerifier — результат операции
	 */
	public @Nullable SignatureVerifier serviceSignatureVerifier() {
		return SignatureVerifier.create(this.servicesKeySet, ServicesKeyType.PROFILE_KEY);
	}

	/**
	 * Provides profile keys.
	 *
	 * @return boolean — результат операции
	 */
	public boolean providesProfileKeys() {
		return !this.servicesKeySet.keys(ServicesKeyType.PROFILE_KEY).isEmpty();
	}
}
