package net.minecraft.client.texture;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.properties.Property;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.ApiServices;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Загружает и кэширует текстуры скинов, плащей и элитр игроков.
 *
 * <p>Использует Guava {@link LoadingCache} с TTL 15 секунд для дедупликации
 * параллельных запросов к серверам Mojang. Каждый тип текстуры хранится
 * в отдельном {@link FileCache}, который сохраняет загруженные файлы на диск.
 */
@Environment(EnvType.CLIENT)
public class PlayerSkinProvider {

	static final Logger LOGGER = LogUtils.getLogger();

	private final ApiServices apiServices;
	final PlayerSkinTextureDownloader downloader;
	private final LoadingCache<Key, CompletableFuture<Optional<SkinTextures>>> cache;
	private final FileCache skinCache;
	private final FileCache capeCache;
	private final FileCache elytraCache;

	public PlayerSkinProvider(
		Path cacheDirectory,
		ApiServices apiServices,
		PlayerSkinTextureDownloader downloader,
		Executor executor
	) {
		this.apiServices = apiServices;
		this.downloader = downloader;
		skinCache = new FileCache(cacheDirectory, Type.SKIN);
		capeCache = new FileCache(cacheDirectory, Type.CAPE);
		elytraCache = new FileCache(cacheDirectory, Type.ELYTRA);
		cache = CacheBuilder.newBuilder()
			.expireAfterAccess(Duration.ofSeconds(15L))
			.build(new CacheLoader<>() {
				@Override
				public CompletableFuture<Optional<SkinTextures>> load(Key key) {
					return CompletableFuture.<MinecraftProfileTextures>supplyAsync(
						() -> {
							Property property = key.packedTextures();
							if (property == null) {
								return MinecraftProfileTextures.EMPTY;
							}

							MinecraftProfileTextures profileTextures = apiServices
								.sessionService()
								.unpackTextures(property);

							if (profileTextures.signatureState() == SignatureState.INVALID) {
								LOGGER.warn(
									"Profile contained invalid signature for textures property (profile id: {})",
									key.profileId()
								);
							}

							return profileTextures;
						},
						Util.getMainWorkerExecutor().named("unpackSkinTextures")
					)
					.thenComposeAsync(
						textures -> fetchSkinTextures(key.profileId(), textures),
						executor
					)
					.handle((skinTextures, throwable) -> {
						if (throwable != null) {
							LOGGER.warn(
								"Failed to load texture for profile {}",
								key.profileId,
								throwable
							);
						}

						return Optional.ofNullable(skinTextures);
					});
				}
			});
	}

	/**
	 * Возвращает {@link Supplier}, который при каждом вызове отдаёт актуальные текстуры.
	 * Если загрузка ещё не завершена — возвращает скин по умолчанию.
	 * Если {@code requireSecure} — фильтрует неподписанные текстуры.
	 */
	public Supplier<SkinTextures> supplySkinTextures(GameProfile profile, boolean requireSecure) {
		SkinTextures defaultTextures = DefaultSkinHelper.getSkinTextures(profile);

		if (SharedConstants.DEFAULT_SKIN_OVERRIDE) {
			return () -> defaultTextures;
		}

		CompletableFuture<Optional<SkinTextures>> future = fetchSkinTextures(profile);
		Optional<SkinTextures> resolved = future.getNow(null);

		if (resolved != null) {
			SkinTextures resolved2 = resolved
				.filter(tex -> !requireSecure || tex.secure())
				.orElse(defaultTextures);
			return () -> resolved2;
		}

		return () -> future
			.getNow(Optional.empty())
			.filter(tex -> !requireSecure || tex.secure())
			.orElse(defaultTextures);
	}

	/**
	 * Запускает (или возвращает уже запущенную) асинхронную загрузку текстур профиля.
	 * При включённом {@link SharedConstants#DEFAULT_SKIN_OVERRIDE} немедленно возвращает
	 * скин по умолчанию без обращения к серверам.
	 */
	public CompletableFuture<Optional<SkinTextures>> fetchSkinTextures(GameProfile profile) {
		if (SharedConstants.DEFAULT_SKIN_OVERRIDE) {
			SkinTextures skinTextures = DefaultSkinHelper.getSkinTextures(profile);
			return CompletableFuture.completedFuture(Optional.of(skinTextures));
		}

		Property property = apiServices.sessionService().getPackedTextures(profile);
		return cache.getUnchecked(new Key(profile.id(), property));
	}

	CompletableFuture<SkinTextures> fetchSkinTextures(UUID uuid, MinecraftProfileTextures textures) {
		MinecraftProfileTexture skinTexture = textures.skin();
		CompletableFuture<AssetInfo.TextureAsset> skinFuture;
		PlayerSkinType skinType;

		if (skinTexture != null) {
			skinFuture = skinCache.get(skinTexture);
			skinType = PlayerSkinType.byModelMetadata(skinTexture.getMetadata("model"));
		} else {
			SkinTextures defaultTextures = DefaultSkinHelper.getSkinTextures(uuid);
			skinFuture = CompletableFuture.completedFuture(defaultTextures.body());
			skinType = defaultTextures.model();
		}

		MinecraftProfileTexture capeTexture = textures.cape();
		CompletableFuture<AssetInfo.TextureAsset> capeFuture = capeTexture != null
			? capeCache.get(capeTexture)
			: CompletableFuture.completedFuture(null);

		MinecraftProfileTexture elytraTexture = textures.elytra();
		CompletableFuture<AssetInfo.TextureAsset> elytraFuture = elytraTexture != null
			? elytraCache.get(elytraTexture)
			: CompletableFuture.completedFuture(null);

		return CompletableFuture.allOf(skinFuture, capeFuture, elytraFuture)
			.thenApply(
				ignored -> new SkinTextures(
					skinFuture.join(),
					capeFuture.join(),
					elytraFuture.join(),
					skinType,
					textures.signatureState() == SignatureState.SIGNED
				)
			);
	}

	/**
	 * Локальный дисковый кэш текстур одного типа (скин / плащ / элитра).
	 * Дедуплицирует запросы по SHA-1 хэшу URL текстуры.
	 */
	@Environment(EnvType.CLIENT)
	class FileCache {

		private final Path directory;
		private final Type type;
		private final Map<String, CompletableFuture<AssetInfo.TextureAsset>> hashToTexture =
			new Object2ObjectOpenHashMap<>();

		FileCache(final Path directory, final Type type) {
			this.directory = directory;
			this.type = type;
		}

		public CompletableFuture<AssetInfo.TextureAsset> get(MinecraftProfileTexture texture) {
			String hash = texture.getHash();
			CompletableFuture<AssetInfo.TextureAsset> cached = hashToTexture.get(hash);

			if (cached == null) {
				cached = store(texture);
				hashToTexture.put(hash, cached);
			}

			return cached;
		}

		private CompletableFuture<AssetInfo.TextureAsset> store(MinecraftProfileTexture texture) {
			String sha1 = Hashing.sha1().hashUnencodedChars(texture.getHash()).toString();
			Identifier texturePath = getTexturePath(sha1);
			Path filePath = directory
				.resolve(sha1.length() > 2 ? sha1.substring(0, 2) : "xx")
				.resolve(sha1);

			return downloader.downloadAndRegisterTexture(
				texturePath,
				filePath,
				texture.getUrl(),
				type == Type.SKIN
			);
		}

		private Identifier getTexturePath(String hash) {
			String folder = switch (type) {
				case SKIN -> "skins";
				case CAPE -> "capes";
				case ELYTRA -> "elytra";
				default -> throw new MatchException(null, null);
			};
			return Identifier.ofVanilla(folder + "/" + hash);
		}
	}

	@Environment(EnvType.CLIENT)
	record Key(UUID profileId, @Nullable Property packedTextures) {
	}
}
