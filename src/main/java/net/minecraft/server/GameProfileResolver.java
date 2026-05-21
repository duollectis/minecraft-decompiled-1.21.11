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
 * {@code GameProfileResolver}.
 */
public interface GameProfileResolver {

	Optional<GameProfile> getProfileByName(String name);

	Optional<GameProfile> getProfileById(UUID id);

	default Optional<GameProfile> getProfile(Either<String, UUID> either) {
		return (Optional<GameProfile>) either.map(this::getProfileByName, this::getProfileById);
	}

	/**
	 * {@code CachedSessionProfileResolver}.
	 */
	public static class CachedSessionProfileResolver implements GameProfileResolver {

		private final LoadingCache<String, Optional<GameProfile>> nameCache;
		final LoadingCache<UUID, Optional<GameProfile>> idCache;

		public CachedSessionProfileResolver(MinecraftSessionService sessionService, NameToIdCache cache) {
			this.idCache = CacheBuilder.newBuilder()
			                           .expireAfterAccess(Duration.ofMinutes(10L))
			                           .maximumSize(256L)
			                           .build(new CacheLoader<UUID, Optional<GameProfile>>() {
				                           /**
				                            * Load.
				                            *
				                            * @param uUID u u i d
				                            *
				                            * @return Optional — результат операции
				                            */
				                           public Optional<GameProfile> load(UUID uUID) {
					                           ProfileResult profileResult = sessionService.fetchProfile(uUID, true);
					                           return Optional.ofNullable(profileResult).map(ProfileResult::profile);
				                           }
			                           });
			this.nameCache = CacheBuilder.newBuilder()
			                             .expireAfterAccess(Duration.ofMinutes(10L))
			                             .maximumSize(256L)
			                             .build(
					                             new CacheLoader<String, Optional<GameProfile>>() {
						                             /**
						                              * Load.
						                              *
						                              * @param string string
						                              *
						                              * @return Optional — результат операции
						                              */
						                             public Optional<GameProfile> load(String string) {
							                             return cache.findByName(string)
							                                         .flatMap(entry -> (Optional<? extends GameProfile>) CachedSessionProfileResolver.this.idCache.getUnchecked(
									                                         entry.id()));
						                             }
					                             }
			                             );
		}

		@Override
		public Optional<GameProfile> getProfileByName(String name) {
			return StringHelper.isValidPlayerName(name) ? (Optional) this.nameCache.getUnchecked(name)
			                                            : Optional.empty();
		}

		@Override
		public Optional<GameProfile> getProfileById(UUID id) {
			return (Optional<GameProfile>) this.idCache.getUnchecked(id);
		}
	}
}
