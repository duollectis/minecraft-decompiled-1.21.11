package net.minecraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Оверлей заставки Mojang Studios, отображаемый при загрузке ресурсов.
 * Управляет анимацией логотипа, прогресс-баром и плавным переходом к основному экрану.
 */
@Environment(EnvType.CLIENT)
public class SplashOverlay extends Overlay {

	public static final Identifier LOGO = Identifier.ofVanilla("textures/gui/title/mojangstudios.png");
	private static final int MOJANG_RED = ColorHelper.getArgb(255, 239, 50, 61);
	private static final int MONOCHROME_BLACK = ColorHelper.getArgb(255, 0, 0, 0);
	private static final IntSupplier
			BRAND_ARGB =
			() -> MinecraftClient.getInstance().options.getMonochromeLogo().getValue() ? MONOCHROME_BLACK : MOJANG_RED;
	private static final int LOGO_ALPHA_MAX = 240;
	private static final float LOGO_RIGHT_HALF_V = 60.0F;
	private static final int FADE_IN_TICKS = 60;
	private static final int FADE_OUT_TICKS = 120;
	private static final float LOGO_OVERLAP = 0.0625F;
	private static final float PROGRESS_LERP_DELTA = 0.95F;
	public static final long RELOAD_COMPLETE_FADE_DURATION = 1000L;
	public static final long RELOAD_START_FADE_DURATION = 500L;
	private final MinecraftClient client;
	private final ResourceReload reload;
	private final Consumer<Optional<Throwable>> exceptionHandler;
	private final boolean reloading;
	private float progress;
	private long reloadCompleteTime = -1L;
	private long reloadStartTime = -1L;

	public SplashOverlay(
			MinecraftClient client,
			ResourceReload monitor,
			Consumer<Optional<Throwable>> exceptionHandler,
			boolean reloading
	) {
		this.client = client;
		this.reload = monitor;
		this.exceptionHandler = exceptionHandler;
		this.reloading = reloading;
	}

	public static void init(TextureManager textureManager) {
		textureManager.registerTexture(LOGO, new LogoTexture());
	}

	private static int withAlpha(int color, int alpha) {
		return color & 16777215 | alpha << 24;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int windowWidth = context.getScaledWindowWidth();
		int windowHeight = context.getScaledWindowHeight();
		long now = Util.getMeasuringTimeMs();

		if (reloading && reloadStartTime == -1L) {
			reloadStartTime = now;
		}

		float fadeOutProgress = reloadCompleteTime > -1L
			? (float) (now - reloadCompleteTime) / (float) RELOAD_COMPLETE_FADE_DURATION
			: -1.0F;
		float fadeInProgress = reloadStartTime > -1L
			? (float) (now - reloadStartTime) / (float) RELOAD_START_FADE_DURATION
			: -1.0F;

		float logoAlpha;
		if (fadeOutProgress >= 1.0F) {
			if (client.currentScreen != null) {
				client.currentScreen.renderWithTooltip(context, 0, 0, deltaTicks);
			} else {
				client.inGameHud.renderDeferredSubtitles();
			}

			int overlayAlpha = MathHelper.ceil((1.0F - MathHelper.clamp(fadeOutProgress - 1.0F, 0.0F, 1.0F)) * 255.0F);
			context.createNewRootLayer();
			context.fill(0, 0, windowWidth, windowHeight, withAlpha(BRAND_ARGB.getAsInt(), overlayAlpha));
			logoAlpha = 1.0F - MathHelper.clamp(fadeOutProgress - 1.0F, 0.0F, 1.0F);
		} else if (reloading) {
			if (client.currentScreen != null && fadeInProgress < 1.0F) {
				client.currentScreen.renderWithTooltip(context, mouseX, mouseY, deltaTicks);
			} else {
				client.inGameHud.renderDeferredSubtitles();
			}

			int overlayAlpha = MathHelper.ceil(MathHelper.clamp((double) fadeInProgress, 0.15, 1.0) * 255.0);
			context.createNewRootLayer();
			context.fill(0, 0, windowWidth, windowHeight, withAlpha(BRAND_ARGB.getAsInt(), overlayAlpha));
			logoAlpha = MathHelper.clamp(fadeInProgress, 0.0F, 1.0F);
		} else {
			int brandColor = BRAND_ARGB.getAsInt();
			RenderSystem
				.getDevice()
				.createCommandEncoder()
				.clearColorTexture(client.getFramebuffer().getColorAttachment(), brandColor);
			logoAlpha = 1.0F;
		}

		int centerX = (int) (context.getScaledWindowWidth() * 0.5);
		int centerY = (int) (context.getScaledWindowHeight() * 0.5);
		double logoHeight = Math.min(context.getScaledWindowWidth() * 0.75, (double) context.getScaledWindowHeight()) * 0.25;
		int halfLogoHeight = (int) (logoHeight * 0.5);
		double logoWidth = logoHeight * 4.0;
		int halfLogoWidth = (int) (logoWidth * 0.5);
		int logoColor = ColorHelper.getWhite(logoAlpha);

		context.drawTexture(
			RenderPipelines.MOJANG_LOGO,
			LOGO,
			centerX - halfLogoWidth,
			centerY - halfLogoHeight,
			-LOGO_OVERLAP,
			0.0F,
			halfLogoWidth,
			(int) logoHeight,
			FADE_OUT_TICKS,
			FADE_IN_TICKS,
			FADE_OUT_TICKS,
			FADE_OUT_TICKS,
			logoColor
		);
		context.drawTexture(
			RenderPipelines.MOJANG_LOGO,
			LOGO,
			centerX,
			centerY - halfLogoHeight,
			LOGO_OVERLAP,
			LOGO_RIGHT_HALF_V,
			halfLogoWidth,
			(int) logoHeight,
			FADE_OUT_TICKS,
			FADE_IN_TICKS,
			FADE_OUT_TICKS,
			FADE_OUT_TICKS,
			logoColor
		);

		int progressBarY = (int) (context.getScaledWindowHeight() * 0.8325);
		progress = MathHelper.clamp(progress * PROGRESS_LERP_DELTA + reload.getProgress() * 0.050000012F, 0.0F, 1.0F);

		if (fadeOutProgress < 1.0F) {
			renderProgressBar(
				context,
				windowWidth / 2 - halfLogoWidth,
				progressBarY - 5,
				windowWidth / 2 + halfLogoWidth,
				progressBarY + 5,
				1.0F - MathHelper.clamp(fadeOutProgress, 0.0F, 1.0F)
			);
		}

		if (fadeOutProgress >= 2.0F) {
			client.setOverlay(null);
		}
	}

	@Override
	public void tick() {
		if (reloadCompleteTime == -1L && reload.isComplete() && isInGracePeriod()) {
			try {
				reload.throwException();
				exceptionHandler.accept(Optional.empty());
			} catch (Throwable throwable) {
				exceptionHandler.accept(Optional.of(throwable));
			}

			reloadCompleteTime = Util.getMeasuringTimeMs();
			if (client.currentScreen != null) {
				Window window = client.getWindow();
				client.currentScreen.init(window.getScaledWidth(), window.getScaledHeight());
			}
		}
	}

	private boolean isInGracePeriod() {
		return !reloading
			|| reloadStartTime > -1L && Util.getMeasuringTimeMs() - reloadStartTime >= RELOAD_COMPLETE_FADE_DURATION;
	}

	private void renderProgressBar(DrawContext context, int minX, int minY, int maxX, int maxY, float opacity) {
		int filledWidth = MathHelper.ceil((maxX - minX - 2) * progress);
		int alpha = Math.round(opacity * 255.0F);
		int color = ColorHelper.getArgb(alpha, 255, 255, 255);
		context.fill(minX + 2, minY + 2, minX + filledWidth, maxY - 2, color);
		context.fill(minX + 1, minY, maxX - 1, minY + 1, color);
		context.fill(minX + 1, maxY, maxX - 1, maxY - 1, color);
		context.fill(minX, minY, minX + 1, maxY, color);
		context.fill(maxX, minY, maxX - 1, maxY, color);
	}

	@Override
	public boolean pausesGame() {
		return true;
	}

	/**
	 * Текстура логотипа Mojang, загружаемая из встроенного пакета ресурсов.
	 */
	@Environment(EnvType.CLIENT)
	static class LogoTexture extends ReloadableTexture {

		public LogoTexture() {
			super(SplashOverlay.LOGO);
		}

		@Override
		public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
			ResourceFactory resourceFactory = MinecraftClient.getInstance().getDefaultResourcePack().getFactory();

			try (InputStream inputStream = resourceFactory.open(SplashOverlay.LOGO)) {
				return new TextureContents(
					NativeImage.read(inputStream),
					new TextureResourceMetadata(true, true, MipmapStrategy.MEAN, 0.0F)
				);
			}
		}
	}
}
