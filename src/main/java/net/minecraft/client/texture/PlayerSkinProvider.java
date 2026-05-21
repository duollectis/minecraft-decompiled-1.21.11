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

@Environment(EnvType.CLIENT)
/**
 * {@code PlayerSkinProvider}.
 */
public class PlayerSkinProvider {

	static final Logger LOGGER = LogUtils.getLogger();
	private final ApiServices apiServices;
	final PlayerSkinTextureDownloader downloader;
	private final LoadingCache<PlayerSkinProvider.Key, CompletableFuture<Optional<SkinTextures>>> cache;
	private final PlayerSkinProvider.FileCache skinCache;
	private final PlayerSkinProvider.FileCache capeCache;
	private final PlayerSkinProvider.FileCache elytraCache;

	public PlayerSkinProvider(
			Path cacheDirectory,
			ApiServices apiServices,
			PlayerSkinTextureDownloader downloader,
			Executor executor
	) {
		this.apiServices = apiServices;
		this.downloader = downloader;
		this.skinCache = new PlayerSkinProvider.FileCache(cacheDirectory, Type.SKIN);
		this.capeCache = new PlayerSkinProvider.FileCache(cacheDirectory, Type.CAPE);
		this.elytraCache = new PlayerSkinProvider.FileCache(cacheDirectory, Type.ELYTRA);
		this.cache = CacheBuilder.newBuilder()
		                         .expireAfterAccess(Duration.ofSeconds(15L))
		                         .build(
				                         new CacheLoader<PlayerSkinProvider.Key, CompletableFuture<Optional<SkinTextures>>>() {
					                         /**
					                          * Load.
					                          *
					                          * @param key key
					                          *
					                          * @return CompletableFuture> — результат операции
					                          */
					                         public CompletableFuture<Optional<SkinTextures>> load(PlayerSkinProvider.Key key) {
						                         return CompletableFuture.<MinecraftProfileTextures>supplyAsync(
								                                                 () -> {
									                                                 Property property = key.packedTextures();
									                                                 if (property == null) {
										                                                 return MinecraftProfileTextures.EMPTY;
									                                                 }
									                                                 else {
										                                                 MinecraftProfileTextures
												                                                 minecraftProfileTextures =
												                                                 apiServices
														                                                 .sessionService()
														                                                 .unpackTextures(property);
										                                                 if (minecraftProfileTextures.signatureState()
												                                                 == SignatureState.INVALID) {
											                                                 PlayerSkinProvider.LOGGER.warn(
													                                                 "Profile contained invalid signature for textures property (profile id: {})",
													                                                 key.profileId()
											                                                 );
										                                                 }

										                                                 return minecraftProfileTextures;
									                                                 }
								                                                 }, Util.getMainWorkerExecutor().named("unpackSkinTextures")
						                                                 )
						                                                 .thenComposeAsync(
								                                                 textures -> PlayerSkinProvider.this.fetchSkinTextures(
										                                                 key.profileId(),
										                                                 textures
								                                                 ), executor
						                                                 )
						                                                 .handle((skinTextures, throwable) -> {
							                                                 if (throwable != null) {
								                                                 PlayerSkinProvider.LOGGER.warn(
										                                                 "Failed to load texture for profile {}",
										                                                 key.profileId,
										                                                 throwable
								                                                 );
							                                                 }

							                                                 return Optional.ofNullable(skinTextures);
						                                                 });
					                         }
				                         }
		                         );
	}

	/**
	 * Supply skin textures.
	 *
	 * @param profile profile
	 * @param requireSecure require secure
	 *
	 * @return Supplier — результат операции
	 */
	public Supplier<SkinTextures> supplySkinTextures(GameProfile profile, boolean requireSecure) {
		CompletableFuture<Optional<SkinTextures>> completableFuture = this.fetchSkinTextures(profile);
		SkinTextures skinTextures = DefaultSkinHelper.getSkinTextures(profile);
		if (SharedConstants.DEFAULT_SKIN_OVERRIDE) {
			return () -> skinTextures;
		}
		else {
			Optional<SkinTextures> optional = completableFuture.getNow(null);
			if (optional != null) {
				SkinTextures
						skinTextures2 =
						optional.filter(skinTexturesx -> !requireSecure || skinTexturesx.secure()).orElse(skinTextures);
				return () -> skinTextures2;
			}
			else {
				return () -> completableFuture
						.getNow(Optional.empty())
						.filter(skinTexturesxx -> !requireSecure || skinTexturesxx.secure())
						.orElse(skinTextures);
			}
		}
	}

	/**
	 * Fetch skin textures.
	 *
	 * @param profile profile
	 *
	 * @return CompletableFuture> — результат операции
	 */
	public CompletableFuture<Optional<SkinTextures>> fetchSkinTextures(GameProfile profile) {
		if (SharedConstants.DEFAULT_SKIN_OVERRIDE) {
			SkinTextures skinTextures = DefaultSkinHelper.getSkinTextures(profile);
			return CompletableFuture.completedFuture(Optional.of(skinTextures));
		}
		else {
			Property property = this.apiServices.sessionService().getPackedTextures(profile);
			return (CompletableFuture<Optional<SkinTextures>>) this.cache.getUnchecked(new PlayerSkinProvider.Key(
					profile.id(),
					property
			));
		}
	}

	CompletableFuture<SkinTextures> fetchSkinTextures(UUID uuid, MinecraftProfileTextures textures) {
		MinecraftProfileTexture minecraftProfileTexture = textures.skin();
		CompletableFuture<AssetInfo.TextureAsset> completableFuture;
		PlayerSkinType playerSkinType;
		if (minecraftProfileTexture != null) {
			completableFuture = this.skinCache.get(minecraftProfileTexture);
			playerSkinType = PlayerSkinType.byModelMetadata(minecraftProfileTexture.getMetadata("model"));
		}
		else {
			SkinTextures skinTextures = DefaultSkinHelper.getSkinTextures(uuid);
			completableFuture = CompletableFuture.completedFuture(skinTextures.body());
			playerSkinType = skinTextures.model();
		}

		MinecraftProfileTexture minecraftProfileTexture2 = textures.cape();
		CompletableFuture<AssetInfo.TextureAsset> completableFuture2 = minecraftProfileTexture2 != null
		                                                               ? this.capeCache.get(minecraftProfileTexture2)
		                                                               : CompletableFuture.completedFuture(null);
		MinecraftProfileTexture minecraftProfileTexture3 = textures.elytra();
		CompletableFuture<AssetInfo.TextureAsset> completableFuture3 = minecraftProfileTexture3 != null
		                                                               ? this.elytraCache.get(minecraftProfileTexture3)
		                                                               : CompletableFuture.completedFuture(null);
		return CompletableFuture.allOf(completableFuture, completableFuture2, completableFuture3)
		                        .thenApply(
				                        void_ -> new SkinTextures(
						                        completableFuture.join(),
						                        completableFuture2.join(),
						                        completableFuture3.join(),
						                        playerSkinType,
						                        textures.signatureState() == SignatureState.SIGNED
				                        )
		                        );
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code FileCache}.
	 */
	class FileCache {

		private final Path directory;
		private final Type type;
		private final Map<String, CompletableFuture<AssetInfo.TextureAsset>>
				hashToTexture =
				new Object2ObjectOpenHashMap();

		FileCache(final Path directory, final Type type) {
			this.directory = directory;
			this.type = type;
		}

		public CompletableFuture<AssetInfo.TextureAsset> get(MinecraftProfileTexture texture) {
			String string = texture.getHash();
			CompletableFuture<AssetInfo.TextureAsset> completableFuture = this.hashToTexture.get(string);
			if (completableFuture == null) {
				completableFuture = this.store(texture);
				this.hashToTexture.put(string, completableFuture);
			}

			return completableFuture;
		}

		private CompletableFuture<AssetInfo.TextureAsset> store(MinecraftProfileTexture texture) {
			String string = Hashing.sha1().hashUnencodedChars(texture.getHash()).toString();
			Identifier identifier = this.getTexturePath(string);
			Path path = this.directory.resolve(string.length() > 2 ? string.substring(0, 2) : "xx").resolve(string);
			return PlayerSkinProvider.this.downloader.downloadAndRegisterTexture(
					identifier,
					path,
					texture.getUrl(),
					this.type == Type.SKIN
			);
		}

		private Identifier getTexturePath(String hash) {
			String string = switch (this.type) {
				case SKIN -> "skins";
				case CAPE -> "capes";
				case ELYTRA -> "elytra";
				default -> throw new MatchException(null, null);
			};
			return Identifier.ofVanilla(string + "/" + hash);
		}
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Key}.
	 */
	record Key(UUID profileId, @Nullable Property packedTextures) {
	}
}
