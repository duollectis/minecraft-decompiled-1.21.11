package net.minecraft.server;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.datafixers.util.Either;
import net.minecraft.util.NameToIdCache;
import net.minecraft.util.StringHelper;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Резолвер игровых профилей по имени или UUID.
 */
public interface GameProfileResolver {

	Optional<GameProfile> getProfileByName(String name);

	Optional<GameProfile> getProfileById(UUID id);

	default Optional<GameProfile> getProfile(Either<String, UUID> either) {
		return either.map(this::getProfileByName, this::getProfileById);
	}

	/**
	 * Реализация с двухуровневым кешем (по имени и по UUID).
	 * Кеш истекает через 10 минут после последнего обращения, максимум 256 записей.
	 */
	class CachedSessionProfileResolver implements GameProfileResolver {

		private static final Duration CACHE_EXPIRY = Duration.ofMinutes(10L);
		private static final long CACHE_MAX_SIZE = 256L;

		private final LoadingCache<String, Optional<GameProfile>> nameCache;
		final LoadingCache<UUID, Optional<GameProfile>> idCache;

		public CachedSessionProfileResolver(MinecraftSessionService sessionService, NameToIdCache cache) {
			this.idCache = CacheBuilder.newBuilder()
				.expireAfterAccess(CACHE_EXPIRY)
				.maximumSize(CACHE_MAX_SIZE)
				.build(new CacheLoader<>() {
					@Override
					public Optional<GameProfile> load(UUID id) {
						ProfileResult result = sessionService.fetchProfile(id, true);
						return Optional.ofNullable(result).map(ProfileResult::profile);
					}
				});

			this.nameCache = CacheBuilder.newBuilder()
				.expireAfterAccess(CACHE_EXPIRY)
				.maximumSize(CACHE_MAX_SIZE)
				.build(new CacheLoader<>() {
					@Override
					public Optional<GameProfile> load(String name) {
						return cache.findByName(name)
							.flatMap(entry -> idCache.getUnchecked(entry.id()));
					}
				});
		}

		@Override
		public Optional<GameProfile> getProfileByName(String name) {
			return StringHelper.isValidPlayerName(name)
				? nameCache.getUnchecked(name)
				: Optional.empty();
		}

		@Override
		public Optional<GameProfile> getProfileById(UUID id) {
			return idCache.getUnchecked(id);
		}
	}
}
