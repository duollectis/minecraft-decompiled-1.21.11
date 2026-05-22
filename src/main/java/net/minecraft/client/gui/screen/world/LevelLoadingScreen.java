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

/**
 * Экран загрузки уровня, отображающий прогресс загрузки чанков в виде цветной карты.
 * Поддерживает три режима фона: портал в Нижний мир, портал в Край и стандартная панорама.
 */
@Environment(EnvType.CLIENT)
public class LevelLoadingScreen extends Screen {

	private static final Text DOWNLOADING_TERRAIN_TEXT = Text.translatable("multiplayer.downloadingTerrain");
	private static final Text READY_TO_PLAY_MESSAGE = Text.translatable("narrator.ready_to_play");
	private static final long NARRATION_DELAY = 2000L;
	private static final int PROGRESS_BAR_WIDTH = 200;
	private static final float PROGRESS_SMOOTHING = 0.2F;
	private static final Object2IntMap<ChunkStatus> STATUS_TO_COLOR = Util.make(
			new Object2IntOpenHashMap(), map -> {
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
			}
	);

	private ClientChunkLoadProgress chunkLoadProgress;
	private float loadProgress;
	private long lastNarrationTime = -1L;
	private LevelLoadingScreen.WorldEntryReason reason;
	private @Nullable Sprite netherPortalSprite;

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
		if (chunkLoadProgress.hasProgress()) {
			builder.put(
					NarrationPart.TITLE,
					Text.translatable(
							"loading.progress",
							MathHelper.floor(chunkLoadProgress.getLoadProgress() * 100.0F)
					)
			);
		}
	}

	@Override
	public void tick() {
		super.tick();
		loadProgress = loadProgress + (chunkLoadProgress.getLoadProgress() - loadProgress) * PROGRESS_SMOOTHING;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);

		long currentTime = Util.getMeasuringTimeMs();
		if (currentTime - lastNarrationTime > NARRATION_DELAY) {
			lastNarrationTime = currentTime;
			narrateScreenIfNarrationEnabled(true);
		}

		int centerX = width / 2;
		int centerY = height / 2;
		ChunkLoadMap chunkLoadMap = chunkLoadProgress.getChunkLoadMap();
		int textY;

		if (chunkLoadMap != null) {
			drawChunkMap(context, centerX, centerY, 2, 0, chunkLoadMap);
			textY = centerY - chunkLoadMap.getRadius() * 2 - 9 * 3;
		}
		else {
			textY = centerY - 50;
		}

		context.drawCenteredTextWithShadow(textRenderer, DOWNLOADING_TERRAIN_TEXT, centerX, textY, -1);

		if (chunkLoadProgress.hasProgress()) {
			drawLoadingBar(context, centerX - 100, textY + 9 + 3, PROGRESS_BAR_WIDTH, 2, loadProgress);
		}
	}

	private void drawLoadingBar(DrawContext context, int x1, int y1, int barWidth, int barHeight, float progress) {
		context.fill(x1, y1, x1 + barWidth, y1 + barHeight, -16777216);
		context.fill(x1, y1, x1 + Math.round(progress * barWidth), y1 + barHeight, -16711936);
	}

	/**
	 * Отрисовывает карту загрузки чанков в виде цветной сетки.
	 * Каждый чанк окрашивается в цвет, соответствующий его статусу генерации.
	 */
	public static void drawChunkMap(
			DrawContext context,
			int centerX,
			int centerY,
			int chunkLength,
			int chunkGap,
			ChunkLoadMap map
	) {
		int step = chunkLength + chunkGap;
		int diameter = map.getRadius() * 2 + 1;
		int gridSize = diameter * step - chunkGap;
		int originX = centerX - gridSize / 2;
		int originY = centerY - gridSize / 2;

		if (MinecraftClient.getInstance().debugHudEntryList.isEntryVisible(DebugHudEntries.VISUALIZE_CHUNKS_ON_SERVER)) {
			int crossSize = step / 2 + 1;
			context.fill(centerX - crossSize, centerY - crossSize, centerX + crossSize, centerY + crossSize, -65536);
		}

		for (int col = 0; col < diameter; col++) {
			for (int row = 0; row < diameter; row++) {
				ChunkStatus status = map.getStatus(col, row);
				int chunkX = originX + col * step;
				int chunkY = originY + row * step;
				context.fill(
						chunkX,
						chunkY,
						chunkX + chunkLength,
						chunkY + chunkLength,
						ColorHelper.fullAlpha(STATUS_TO_COLOR.getInt(status))
				);
			}
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		switch (reason) {
			case NETHER_PORTAL -> context.drawSpriteStretched(
					RenderPipelines.GUI_OPAQUE_TEX_BG,
					getNetherPortalSprite(),
					0,
					0,
					context.getScaledWindowWidth(),
					context.getScaledWindowHeight()
			);
			case END_PORTAL -> {
				TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
				AbstractTexture skyTexture = textureManager.getTexture(AbstractEndPortalBlockEntityRenderer.SKY_TEXTURE);
				AbstractTexture portalTexture = textureManager.getTexture(AbstractEndPortalBlockEntityRenderer.PORTAL_TEXTURE);
				TextureSetup textureSetup = TextureSetup.of(
						skyTexture.getGlTextureView(),
						skyTexture.getSampler(),
						portalTexture.getGlTextureView(),
						portalTexture.getSampler()
				);
				context.fill(RenderPipelines.END_PORTAL, textureSetup, 0, 0, width, height);
			}
			case OTHER -> {
				renderPanoramaBackground(context, deltaTicks);
				applyBlur(context);
				renderDarkening(context);
			}
		}
	}

	private Sprite getNetherPortalSprite() {
		if (netherPortalSprite != null) {
			return netherPortalSprite;
		}

		netherPortalSprite = client
				.getBlockRenderManager()
				.getModels()
				.getModelParticleSprite(Blocks.NETHER_PORTAL.getDefaultState());

		return netherPortalSprite;
	}

	@Override
	public void close() {
		client.getNarratorManager().narrateSystemImmediately(READY_TO_PLAY_MESSAGE);
		super.close();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	/**
	 * Причина входа в мир, определяющая тип фонового экрана загрузки.
	 */
	@Environment(EnvType.CLIENT)
	public enum WorldEntryReason {
		NETHER_PORTAL,
		END_PORTAL,
		OTHER
	}
}
