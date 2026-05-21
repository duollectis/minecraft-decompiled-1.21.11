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

@Environment(EnvType.CLIENT)
/**
 * {@code SplashOverlay}.
 */
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
		textureManager.registerTexture(LOGO, (ReloadableTexture) (new SplashOverlay.LogoTexture()));
	}

	private static int withAlpha(int color, int alpha) {
		return color & 16777215 | alpha << 24;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		int i = context.getScaledWindowWidth();
		int j = context.getScaledWindowHeight();
		long l = Util.getMeasuringTimeMs();
		if (this.reloading && this.reloadStartTime == -1L) {
			this.reloadStartTime = l;
		}

		float f = this.reloadCompleteTime > -1L ? (float) (l - this.reloadCompleteTime) / 1000.0F : -1.0F;
		float g = this.reloadStartTime > -1L ? (float) (l - this.reloadStartTime) / 500.0F : -1.0F;
		float h;
		if (f >= 1.0F) {
			if (this.client.currentScreen != null) {
				this.client.currentScreen.renderWithTooltip(context, 0, 0, deltaTicks);
			}
			else {
				this.client.inGameHud.renderDeferredSubtitles();
			}

			int k = MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
			context.createNewRootLayer();
			context.fill(0, 0, i, j, withAlpha(BRAND_ARGB.getAsInt(), k));
			h = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
		}
		else if (this.reloading) {
			if (this.client.currentScreen != null && g < 1.0F) {
				this.client.currentScreen.renderWithTooltip(context, mouseX, mouseY, deltaTicks);
			}
			else {
				this.client.inGameHud.renderDeferredSubtitles();
			}

			int k = MathHelper.ceil(MathHelper.clamp((double) g, 0.15, 1.0) * 255.0);
			context.createNewRootLayer();
			context.fill(0, 0, i, j, withAlpha(BRAND_ARGB.getAsInt(), k));
			h = MathHelper.clamp(g, 0.0F, 1.0F);
		}
		else {
			int k = BRAND_ARGB.getAsInt();
			RenderSystem
					.getDevice()
					.createCommandEncoder()
					.clearColorTexture(this.client.getFramebuffer().getColorAttachment(), k);
			h = 1.0F;
		}

		int k = (int) (context.getScaledWindowWidth() * 0.5);
		int m = (int) (context.getScaledWindowHeight() * 0.5);
		double d = Math.min(context.getScaledWindowWidth() * 0.75, (double) context.getScaledWindowHeight()) * 0.25;
		int n = (int) (d * 0.5);
		double e = d * 4.0;
		int o = (int) (e * 0.5);
		int p = ColorHelper.getWhite(h);
		context.drawTexture(
				RenderPipelines.MOJANG_LOGO,
				LOGO,
				k - o,
				m - n,
				-0.0625F,
				0.0F,
				o,
				(int) d,
				120,
				60,
				120,
				120,
				p
		);
		context.drawTexture(
				RenderPipelines.MOJANG_LOGO,
				LOGO,
				k,
				m - n,
				0.0625F,
				60.0F,
				o,
				(int) d,
				120,
				60,
				120,
				120,
				p
		);
		int q = (int) (context.getScaledWindowHeight() * 0.8325);
		float r = this.reload.getProgress();
		this.progress = MathHelper.clamp(this.progress * 0.95F + r * 0.050000012F, 0.0F, 1.0F);
		if (f < 1.0F) {
			this.renderProgressBar(context, i / 2 - o, q - 5, i / 2 + o, q + 5, 1.0F - MathHelper.clamp(f, 0.0F, 1.0F));
		}

		if (f >= 2.0F) {
			this.client.setOverlay(null);
		}
	}

	@Override
	public void tick() {
		if (this.reloadCompleteTime == -1L && this.reload.isComplete() && this.isInGracePeriod()) {
			try {
				this.reload.throwException();
				this.exceptionHandler.accept(Optional.empty());
			}
			catch (Throwable var2) {
				this.exceptionHandler.accept(Optional.of(var2));
			}

			this.reloadCompleteTime = Util.getMeasuringTimeMs();
			if (this.client.currentScreen != null) {
				Window window = this.client.getWindow();
				this.client.currentScreen.init(window.getScaledWidth(), window.getScaledHeight());
			}
		}
	}

	private boolean isInGracePeriod() {
		return !this.reloading
				|| this.reloadStartTime > -1L && Util.getMeasuringTimeMs() - this.reloadStartTime >= 1000L;
	}

	private void renderProgressBar(DrawContext context, int minX, int minY, int maxX, int maxY, float opacity) {
		int i = MathHelper.ceil((maxX - minX - 2) * this.progress);
		int j = Math.round(opacity * 255.0F);
		int k = ColorHelper.getArgb(j, 255, 255, 255);
		context.fill(minX + 2, minY + 2, minX + i, maxY - 2, k);
		context.fill(minX + 1, minY, maxX - 1, minY + 1, k);
		context.fill(minX + 1, maxY, maxX - 1, maxY - 1, k);
		context.fill(minX, minY, minX + 1, maxY, k);
		context.fill(maxX, minY, maxX - 1, maxY, k);
	}

	@Override
	public boolean pausesGame() {
		return true;
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code LogoTexture}.
	 */
	static class LogoTexture extends ReloadableTexture {

		public LogoTexture() {
			super(SplashOverlay.LOGO);
		}

		@Override
		public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
			ResourceFactory resourceFactory = MinecraftClient.getInstance().getDefaultResourcePack().getFactory();

			TextureContents var4;
			try (InputStream inputStream = resourceFactory.open(SplashOverlay.LOGO)) {
				var4 =
						new TextureContents(
								NativeImage.read(inputStream),
								new TextureResourceMetadata(true, true, MipmapStrategy.MEAN, 0.0F)
						);
			}

			return var4;
		}
	}
}
