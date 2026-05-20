package net.minecraft.client.texture;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
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

@Environment(EnvType.CLIENT)
public class PlayerSkinCache {
   public static final RenderLayer DEFAULT_RENDER_LAYER = getRenderLayer(DefaultSkinHelper.getSteve());
   public static final Duration TIME_TO_LIVE = Duration.ofMinutes(5L);
   private final LoadingCache<ProfileComponent, CompletableFuture<Optional<PlayerSkinCache.Entry>>> fetchingCache = CacheBuilder.newBuilder()
      .expireAfterAccess(TIME_TO_LIVE)
      .build(
         new CacheLoader<ProfileComponent, CompletableFuture<Optional<PlayerSkinCache.Entry>>>() {
            public CompletableFuture<Optional<PlayerSkinCache.Entry>> load(ProfileComponent profileComponent) {
               return profileComponent.resolve(PlayerSkinCache.this.gameProfileResolver)
                  .thenCompose(
                     gameProfile -> PlayerSkinCache.this.playerSkinProvider
                        .fetchSkinTextures(gameProfile)
                        .thenApply(
                           optional -> optional.map(skinTextures -> PlayerSkinCache.this.new Entry(gameProfile, skinTextures, profileComponent.getOverride()))
                        )
                  );
            }
         }
      );
   private final LoadingCache<ProfileComponent, PlayerSkinCache.Entry> immediateCache = CacheBuilder.newBuilder()
      .expireAfterAccess(TIME_TO_LIVE)
      .build(new CacheLoader<ProfileComponent, PlayerSkinCache.Entry>() {
         public PlayerSkinCache.Entry load(ProfileComponent profileComponent) {
            GameProfile gameProfile = profileComponent.getGameProfile();
            return PlayerSkinCache.this.new Entry(gameProfile, DefaultSkinHelper.getSkinTextures(gameProfile), profileComponent.getOverride());
         }
      });
   final TextureManager textureManager;
   final PlayerSkinProvider playerSkinProvider;
   final GameProfileResolver gameProfileResolver;

   public PlayerSkinCache(TextureManager textureManager, PlayerSkinProvider playerSkinProvider, GameProfileResolver gameProfileResolver) {
      this.textureManager = textureManager;
      this.playerSkinProvider = playerSkinProvider;
      this.gameProfileResolver = gameProfileResolver;
   }

   public PlayerSkinCache.Entry get(ProfileComponent profile) {
      PlayerSkinCache.Entry entry = this.getFuture(profile).getNow(Optional.empty()).orElse(null);
      return entry != null ? entry : (PlayerSkinCache.Entry)this.immediateCache.getUnchecked(profile);
   }

   public Supplier<PlayerSkinCache.Entry> getSupplier(ProfileComponent profile) {
      PlayerSkinCache.Entry entry = (PlayerSkinCache.Entry)this.immediateCache.getUnchecked(profile);
      CompletableFuture<Optional<PlayerSkinCache.Entry>> completableFuture = (CompletableFuture<Optional<PlayerSkinCache.Entry>>)this.fetchingCache
         .getUnchecked(profile);
      Optional<PlayerSkinCache.Entry> optional = completableFuture.getNow(null);
      if (optional != null) {
         PlayerSkinCache.Entry entry2 = optional.orElse(entry);
         return () -> entry2;
      } else {
         return () -> completableFuture.getNow(Optional.empty()).orElse(entry);
      }
   }

   public CompletableFuture<Optional<PlayerSkinCache.Entry>> getFuture(ProfileComponent profile) {
      return (CompletableFuture<Optional<PlayerSkinCache.Entry>>)this.fetchingCache.getUnchecked(profile);
   }

   static RenderLayer getRenderLayer(SkinTextures skinTextures) {
      return SkullBlockEntityRenderer.getTranslucentRenderLayer(skinTextures.body().texturePath());
   }

   @Environment(EnvType.CLIENT)
   public final class Entry {
      private final GameProfile profile;
      private final SkinTextures textures;
      private @Nullable RenderLayer renderLayer;
      private @Nullable GpuTextureView textureView;
      private @Nullable TextRenderLayerSet textRenderLayers;

      public Entry(final GameProfile profile, final SkinTextures textures, final SkinTextures.SkinOverride skinOverride) {
         this.profile = profile;
         this.textures = textures.withOverride(skinOverride);
      }

      public GameProfile getProfile() {
         return this.profile;
      }

      public SkinTextures getTextures() {
         return this.textures;
      }

      public RenderLayer getRenderLayer() {
         if (this.renderLayer == null) {
            this.renderLayer = PlayerSkinCache.getRenderLayer(this.textures);
         }

         return this.renderLayer;
      }

      public GpuTextureView getTextureView() {
         if (this.textureView == null) {
            this.textureView = PlayerSkinCache.this.textureManager.getTexture(this.textures.body().texturePath()).getGlTextureView();
         }

         return this.textureView;
      }

      public TextRenderLayerSet getTextRenderLayers() {
         if (this.textRenderLayers == null) {
            this.textRenderLayers = TextRenderLayerSet.of(this.textures.body().texturePath());
         }

         return this.textRenderLayers;
      }

      @Override
      public boolean equals(Object o) {
         return this == o || o instanceof PlayerSkinCache.Entry entry && this.profile.equals(entry.profile) && this.textures.equals(entry.textures);
      }

      @Override
      public int hashCode() {
         int i = 1;
         i = 31 * i + this.profile.hashCode();
         return 31 * i + this.textures.hashCode();
      }
   }
}
