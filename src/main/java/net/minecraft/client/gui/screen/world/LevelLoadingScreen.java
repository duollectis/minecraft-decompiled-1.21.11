package net.minecraft.client.gui.screen.world;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.render.block.entity.AbstractEndPortalBlockEntityRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.world.ClientChunkLoadProgress;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkLoadMap;
import net.minecraft.world.chunk.ChunkStatus;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class LevelLoadingScreen extends Screen {
   private static final Text DOWNLOADING_TERRAIN_TEXT = Text.translatable("multiplayer.downloadingTerrain");
   private static final Text READY_TO_PLAY_MESSAGE = Text.translatable("narrator.ready_to_play");
   private static final long NARRATION_DELAY = 2000L;
   private static final int field_61630 = 200;
   private ClientChunkLoadProgress chunkLoadProgress;
   private float loadProgress;
   private long lastNarrationTime = -1L;
   private LevelLoadingScreen.WorldEntryReason reason;
   private @Nullable Sprite netherPortalSprite;
   private static final Object2IntMap<ChunkStatus> STATUS_TO_COLOR = Util.make(new Object2IntOpenHashMap(), map -> {
      map.defaultReturnValue(0);
      map.put(ChunkStatus.EMPTY, 5526612);
      map.put(ChunkStatus.STRUCTURE_STARTS, 10066329);
      map.put(ChunkStatus.STRUCTURE_REFERENCES, 6250897);
      map.put(ChunkStatus.BIOMES, 8434258);
      map.put(ChunkStatus.NOISE, 13750737);
      map.put(ChunkStatus.SURFACE, 7497737);
      map.put(ChunkStatus.CARVERS, 3159410);
      map.put(ChunkStatus.FEATURES, 2213376);
      map.put(ChunkStatus.INITIALIZE_LIGHT, 13421772);
      map.put(ChunkStatus.LIGHT, 16769184);
      map.put(ChunkStatus.SPAWN, 15884384);
      map.put(ChunkStatus.FULL, 16777215);
   });

   public LevelLoadingScreen(ClientChunkLoadProgress progressProvider, LevelLoadingScreen.WorldEntryReason reason) {
      super(NarratorManager.EMPTY);
      this.chunkLoadProgress = progressProvider;
      this.reason = reason;
   }

   public void init(ClientChunkLoadProgress chunkLoadProgress, LevelLoadingScreen.WorldEntryReason reason) {
      this.chunkLoadProgress = chunkLoadProgress;
      this.reason = reason;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   protected boolean hasUsageText() {
      return false;
   }

   @Override
   protected void addElementNarrations(NarrationMessageBuilder builder) {
      if (this.chunkLoadProgress.hasProgress()) {
         builder.put(NarrationPart.TITLE, Text.translatable("loading.progress", MathHelper.floor(this.chunkLoadProgress.getLoadProgress() * 100.0F)));
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.loadProgress = this.loadProgress + (this.chunkLoadProgress.getLoadProgress() - this.loadProgress) * 0.2F;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      long l = Util.getMeasuringTimeMs();
      if (l - this.lastNarrationTime > 2000L) {
         this.lastNarrationTime = l;
         this.narrateScreenIfNarrationEnabled(true);
      }

      int i = this.width / 2;
      int j = this.height / 2;
      ChunkLoadMap chunkLoadMap = this.chunkLoadProgress.getChunkLoadMap();
      int m;
      if (chunkLoadMap != null) {
         int k = 2;
         drawChunkMap(context, i, j, 2, 0, chunkLoadMap);
         m = j - chunkLoadMap.getRadius() * 2 - 9 * 3;
      } else {
         m = j - 50;
      }

      context.drawCenteredTextWithShadow(this.textRenderer, DOWNLOADING_TERRAIN_TEXT, i, m, -1);
      if (this.chunkLoadProgress.hasProgress()) {
         this.drawLoadingBar(context, i - 100, m + 9 + 3, 200, 2, this.loadProgress);
      }
   }

   private void drawLoadingBar(DrawContext context, int x1, int y1, int width, int height, float delta) {
      context.fill(x1, y1, x1 + width, y1 + height, -16777216);
      context.fill(x1, y1, x1 + Math.round(delta * width), y1 + height, -16711936);
   }

   public static void drawChunkMap(DrawContext context, int centerX, int centerY, int chunkLength, int chunkGap, ChunkLoadMap map) {
      int i = chunkLength + chunkGap;
      int j = map.getRadius() * 2 + 1;
      int k = j * i - chunkGap;
      int l = centerX - k / 2;
      int m = centerY - k / 2;
      if (MinecraftClient.getInstance().debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_CHUNKS_ON_SERVER)) {
         int n = i / 2 + 1;
         context.fill(centerX - n, centerY - n, centerX + n, centerY + n, -65536);
      }

      for (int n = 0; n < j; n++) {
         for (int o = 0; o < j; o++) {
            ChunkStatus chunkStatus = map.getStatus(n, o);
            int p = l + n * i;
            int q = m + o * i;
            context.fill(p, q, p + chunkLength, q + chunkLength, ColorHelper.fullAlpha(STATUS_TO_COLOR.getInt(chunkStatus)));
         }
      }
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      switch (this.reason) {
         case NETHER_PORTAL:
            context.drawSpriteStretched(
               RenderPipelines.GUI_OPAQUE_TEX_BG, this.getNetherPortalSprite(), 0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight()
            );
            break;
         case END_PORTAL:
            TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
            AbstractTexture abstractTexture = textureManager.getTexture(AbstractEndPortalBlockEntityRenderer.SKY_TEXTURE);
            AbstractTexture abstractTexture2 = textureManager.getTexture(AbstractEndPortalBlockEntityRenderer.PORTAL_TEXTURE);
            TextureSetup textureSetup = TextureSetup.of(
               abstractTexture.getGlTextureView(), abstractTexture.getSampler(), abstractTexture2.getGlTextureView(), abstractTexture2.getSampler()
            );
            context.fill(RenderPipelines.END_PORTAL, textureSetup, 0, 0, this.width, this.height);
            break;
         case OTHER:
            this.renderPanoramaBackground(context, deltaTicks);
            this.applyBlur(context);
            this.renderDarkening(context);
      }
   }

   private Sprite getNetherPortalSprite() {
      if (this.netherPortalSprite != null) {
         return this.netherPortalSprite;
      } else {
         this.netherPortalSprite = this.client.getBlockRenderManager().getModels().getModelParticleSprite(Blocks.NETHER_PORTAL.getDefaultState());
         return this.netherPortalSprite;
      }
   }

   @Override
   public void close() {
      this.client.getNarratorManager().narrateSystemImmediately(READY_TO_PLAY_MESSAGE);
      super.close();
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Environment(EnvType.CLIENT)
   public static enum WorldEntryReason {
      NETHER_PORTAL,
      END_PORTAL,
      OTHER;
   }
}
