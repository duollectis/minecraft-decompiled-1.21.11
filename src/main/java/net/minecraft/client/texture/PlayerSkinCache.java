package net.minecraft.client.texture;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderLayerSet;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.server.GameProfileResolver;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Двухуровневый кэш скинов игроков.
 *
 * <p>{@code fetchingCache} асинхронно загружает скин с серверов Mojang и хранит
 * {@link CompletableFuture}, пока загрузка не завершена. {@code immediateCache}
 * немедленно возвращает скин по умолчанию, пока идёт асинхронная загрузка.
 * Оба кэша имеют TTL {@link #TIME_TO_LIVE}.
 */
@Environment(EnvType.CLIENT)
public class PlayerSkinCache {

	public static final RenderLayer DEFAULT_RENDER_LAYER = getRenderLayer(DefaultSkinHelper.getSteve());
	public static final Duration TIME_TO_LIVE = Duration.ofMinutes(5L);

	private final LoadingCache<ProfileComponent, CompletableFuture<Optional<Entry>>> fetchingCache =
		CacheBuilder.newBuilder()
			.expireAfterAccess(TIME_TO_LIVE)
			.build(new CacheLoader<>() {
				@Override
				public CompletableFuture<Optional<Entry>> load(ProfileComponent profileComponent) {
					return profileComponent.resolve(gameProfileResolver)
						.thenCompose(
							gameProfile -> playerSkinProvider
								.fetchSkinTextures(gameProfile)
								.thenApply(
									optional -> optional.map(
										skinTextures -> new Entry(
											gameProfile,
											skinTextures,
											profileComponent.getOverride()
										)
									)
								)
						);
				}
			});

	private final LoadingCache<ProfileComponent, Entry> immediateCache =
		CacheBuilder.newBuilder()
			.expireAfterAccess(TIME_TO_LIVE)
			.build(new CacheLoader<>() {
				@Override
				public Entry load(ProfileComponent profileComponent) {
					GameProfile gameProfile = profileComponent.getGameProfile();
					return new Entry(
						gameProfile,
						DefaultSkinHelper.getSkinTextures(gameProfile),
						profileComponent.getOverride()
					);
				}
			});

	final TextureManager textureManager;
	final PlayerSkinProvider playerSkinProvider;
	final GameProfileResolver gameProfileResolver;

	public PlayerSkinCache(
		TextureManager textureManager,
		PlayerSkinProvider playerSkinProvider,
		GameProfileResolver gameProfileResolver
	) {
		this.textureManager = textureManager;
		this.playerSkinProvider = playerSkinProvider;
		this.gameProfileResolver = gameProfileResolver;
	}

	/**
	 * Возвращает запись кэша немедленно: если асинхронная загрузка уже завершена —
	 * возвращает реальный скин, иначе — скин по умолчанию из {@code immediateCache}.
	 */
	public Entry get(ProfileComponent profile) {
		Entry loaded = getFuture(profile).getNow(Optional.empty()).orElse(null);
		return loaded != null ? loaded : immediateCache.getUnchecked(profile);
	}

	/**
	 * Возвращает {@link Supplier}, который при каждом вызове отдаёт актуальную запись:
	 * если загрузка завершена — реальный скин, иначе — скин по умолчанию.
	 */
	public Supplier<Entry> getSupplier(ProfileComponent profile) {
		Entry fallback = immediateCache.getUnchecked(profile);
		CompletableFuture<Optional<Entry>> future = fetchingCache.getUnchecked(profile);
		Optional<Entry> resolved = future.getNow(null);

		if (resolved != null) {
			Entry resolved2 = resolved.orElse(fallback);
			return () -> resolved2;
		}

		return () -> future.getNow(Optional.empty()).orElse(fallback);
	}

	public CompletableFuture<Optional<Entry>> getFuture(ProfileComponent profile) {
		return fetchingCache.getUnchecked(profile);
	}

	static RenderLayer getRenderLayer(SkinTextures skinTextures) {
		return SkullBlockEntityRenderer.getTranslucentRenderLayer(skinTextures.body().texturePath());
	}

	/**
	 * Запись кэша, хранящая профиль игрока и его текстуры скина.
	 * Лениво инициализирует {@link RenderLayer}, {@link GpuTextureView}
	 * и {@link TextRenderLayerSet} при первом обращении.
	 */
	@Environment(EnvType.CLIENT)
	public final class Entry {

		private final GameProfile profile;
		private final SkinTextures textures;
		private @Nullable RenderLayer renderLayer;
		private @Nullable GpuTextureView textureView;
		private @Nullable TextRenderLayerSet textRenderLayers;

		public Entry(
			final GameProfile profile,
			final SkinTextures textures,
			final SkinTextures.SkinOverride skinOverride
		) {
			this.profile = profile;
			this.textures = textures.withOverride(skinOverride);
		}

		public GameProfile getProfile() {
			return profile;
		}

		public SkinTextures getTextures() {
			return textures;
		}

		public RenderLayer getRenderLayer() {
			if (renderLayer == null) {
				renderLayer = PlayerSkinCache.getRenderLayer(textures);
			}

			return renderLayer;
		}

		public GpuTextureView getTextureView() {
			if (textureView == null) {
				textureView = textureManager
					.getTexture(textures.body().texturePath())
					.getGlTextureView();
			}

			return textureView;
		}

		public TextRenderLayerSet getTextRenderLayers() {
			if (textRenderLayers == null) {
				textRenderLayers = TextRenderLayerSet.of(textures.body().texturePath());
			}

			return textRenderLayers;
		}

		@Override
		public boolean equals(Object o) {
			return this == o
				|| o instanceof Entry entry
				&& profile.equals(entry.profile)
				&& textures.equals(entry.textures);
		}

		@Override
		public int hashCode() {
			int hash = 1;
			hash = 31 * hash + profile.hashCode();
			return 31 * hash + textures.hashCode();
		}
	}
}
