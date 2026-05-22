package net.minecraft.client.toast;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.util.ColorLerper;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Тост "Сейчас играет" — отображается поверх экрана при смене музыкального трека.
 * Содержит анимированную иконку нот с циклической сменой цвета и название трека.
 */
@Environment(EnvType.CLIENT)
public class NowPlayingToast implements Toast {

	private static final Identifier TEXTURE = Identifier.ofVanilla("toast/now_playing");
	private static final Identifier MUSIC_NOTES_ICON = Identifier.of("icon/music_notes");
	private static final int MARGIN = 7;
	private static final int MUSIC_NOTES_ICON_SIZE = 16;
	private static final int FADE_IN_TICKS = 30;
	private static final int DISPLAY_TICKS = 30;
	private static final long MUSIC_NOTE_COLOR_CHANGE_INTERVAL = 25L;
	private static final int TEXT_X_OFFSET = 30;
	private static final int TEXT_Y_OFFSET = 15;
	private static final int HALF_FONT_HEIGHT = 9 / 2;
	private static final int TEXT_COLOR = DyeColor.LIGHT_GRAY.getSignColor();

	private static int musicNoteColorChanges;
	private static long lastMusicNoteColorChangeTime;
	private static int musicNotesIconColor = -1;

	private boolean showing;
	private double displayTimeMultiplier;
	private final MinecraftClient client;
	private Toast.Visibility visibility = Toast.Visibility.HIDE;

	public NowPlayingToast() {
		this.client = MinecraftClient.getInstance();
	}

	public static void draw(DrawContext context, TextRenderer textRenderer) {
		String translationKey = getCurrentMusicTranslationKey();
		if (translationKey == null) {
			return;
		}

		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			TEXTURE,
			0,
			0,
			getMusicTextWidth(translationKey, textRenderer),
			FADE_IN_TICKS
		);
		context.drawGuiTexture(
			RenderPipelines.GUI_TEXTURED,
			MUSIC_NOTES_ICON,
			MARGIN,
			MARGIN,
			MUSIC_NOTES_ICON_SIZE,
			MUSIC_NOTES_ICON_SIZE,
			musicNotesIconColor
		);
		context.drawTextWithShadow(
			textRenderer,
			getMusicText(translationKey),
			TEXT_X_OFFSET,
			TEXT_Y_OFFSET - HALF_FONT_HEIGHT,
			TEXT_COLOR
		);
	}

	public static void tick() {
		if (getCurrentMusicTranslationKey() == null) {
			return;
		}

		long now = System.currentTimeMillis();
		if (now > lastMusicNoteColorChangeTime + MUSIC_NOTE_COLOR_CHANGE_INTERVAL) {
			musicNoteColorChanges++;
			lastMusicNoteColorChangeTime = now;
			musicNotesIconColor = ColorLerper.lerpColor(ColorLerper.Type.MUSIC_NOTE, musicNoteColorChanges);
		}
	}

	public void show(GameOptions options) {
		this.showing = true;
		this.displayTimeMultiplier = options.getNotificationDisplayTime().getValue();
		this.setVisibility(Toast.Visibility.SHOW);
	}

	@Override
	public void update(ToastManager manager, long time) {
		if (!this.showing) {
			return;
		}

		this.visibility = time < 5000.0 * this.displayTimeMultiplier
			? Toast.Visibility.SHOW
			: Toast.Visibility.HIDE;
		tick();
	}

	@Override
	public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
		draw(context, textRenderer);
	}

	@Override
	public void onFinishedRendering() {
		this.showing = false;
	}

	@Override
	public int getWidth() {
		return getMusicTextWidth(getCurrentMusicTranslationKey(), this.client.textRenderer);
	}

	@Override
	public int getHeight() {
		return DISPLAY_TICKS;
	}

	@Override
	public float getXPos(int scaledWindowWidth, float visibleWidthPortion) {
		return this.getWidth() * visibleWidthPortion - this.getWidth();
	}

	@Override
	public float getYPos(int topIndex) {
		return 0.0F;
	}

	@Override
	public Toast.Visibility getVisibility() {
		return this.visibility;
	}

	public void setVisibility(Toast.Visibility visibility) {
		this.visibility = visibility;
	}

	private static @Nullable String getCurrentMusicTranslationKey() {
		return MinecraftClient.getInstance().getMusicTracker().getCurrentMusicTranslationKey();
	}

	private static Text getMusicText(@Nullable String translationKey) {
		return translationKey == null
			? Text.empty()
			: Text.translatable(translationKey.replace("/", "."));
	}

	private static int getMusicTextWidth(@Nullable String translationKey, TextRenderer textRenderer) {
		return TEXT_X_OFFSET + textRenderer.getWidth(getMusicText(translationKey)) + MARGIN;
	}
}
