package net.minecraft.client.gui.screen;

import com.mojang.text2speech.Narrator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.GameOptions;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;

/**
 * Экран первоначальной настройки доступности, отображаемый при первом запуске игры.
 * Предлагает включить нарратор, перейти в настройки доступности или сменить язык.
 * Поддерживает анимацию появления и исчезновения (fade-in/fade-out).
 */
@Environment(EnvType.CLIENT)
public class AccessibilityOnboardingScreen extends Screen {

	private static final Text TITLE_TEXT = Text.translatable("accessibility.onboarding.screen.title");
	private static final Text NARRATOR_PROMPT = Text.translatable("accessibility.onboarding.screen.narrator");
	private static final int WIDGET_MARGIN = 4;
	private static final int NARRATOR_PROMPT_TICKS = 40;
	private static final float CLOSE_FADE_DURATION_MS = 1000.0F;
	private static final float OPEN_FADE_DURATION_MS = 2000.0F;
	private static final int CONTENT_WIDTH = 374;
	private static final int LOGO_Y_MARGIN = 90;
	private static final int LAYOUT_HEADER_HEIGHT = 33;

	private final LogoDrawer logoDrawer;
	private final GameOptions gameOptions;
	private final boolean isNarratorUsable;
	private final Runnable onClose;
	private final ThreePartsLayoutWidget layout = new ThreePartsLayoutWidget(this, LOGO_Y_MARGIN, LAYOUT_HEADER_HEIGHT);
	private @Nullable CyclingButtonWidget<?> narratorToggleButton;
	private boolean narratorPrompted;
	private float narratorPromptTimer;
	private float fadeTime;
	private boolean fading = true;
	private float closeTime;

	public AccessibilityOnboardingScreen(GameOptions gameOptions, Runnable onClose) {
		super(TITLE_TEXT);
		this.gameOptions = gameOptions;
		this.onClose = onClose;
		logoDrawer = new LogoDrawer(true);
		isNarratorUsable = MinecraftClient.getInstance().getNarratorManager().isActive();
	}

	@Override
	public void init() {
		DirectionalLayoutWidget contentLayout = layout.addBody(DirectionalLayoutWidget.vertical());
		contentLayout.getMainPositioner().alignHorizontalCenter().margin(WIDGET_MARGIN);
		contentLayout.add(
			NarratedMultilineTextWidget.builder(title, textRenderer).width(CONTENT_WIDTH).build(),
			positioner -> positioner.margin(8)
		);

		if (gameOptions.getNarrator().createWidget(gameOptions) instanceof CyclingButtonWidget<?> cyclingButton) {
			narratorToggleButton = cyclingButton;
			narratorToggleButton.active = isNarratorUsable;
			contentLayout.add(narratorToggleButton);
		}

		contentLayout.add(
			AccessibilityOnboardingButtons.createAccessibilityButton(
				150, button -> setScreen(new AccessibilityOptionsScreen(this, client.options)), false
			)
		);
		contentLayout.add(
			AccessibilityOnboardingButtons.createLanguageButton(
				150,
				button -> setScreen(new LanguageOptionsScreen(this, client.options, client.getLanguageManager())),
				false
			)
		);

		layout.addFooter(ButtonWidget.builder(ScreenTexts.CONTINUE, button -> close()).build());
		layout.forEachChild(this::addDrawableChild);
		refreshWidgetPositions();
	}

	@Override
	protected void refreshWidgetPositions() {
		layout.refreshPositions();
	}

	@Override
	protected void setInitialFocus() {
		if (isNarratorUsable && narratorToggleButton != null) {
			setInitialFocus(narratorToggleButton);
			return;
		}

		super.setInitialFocus();
	}

	@Override
	public void close() {
		if (closeTime == 0.0F) {
			closeTime = (float) Util.getMeasuringTimeMs();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		super.render(context, mouseX, mouseY, deltaTicks);
		tickNarratorPrompt();

		if (fadeTime == 0.0F && fading) {
			fadeTime = (float) Util.getMeasuringTimeMs();
		}

		if (fadeTime > 0.0F) {
			float progress = ((float) Util.getMeasuringTimeMs() - fadeTime) / OPEN_FADE_DURATION_MS;

			if (progress >= 1.0F) {
				fading = false;
				fadeTime = 0.0F;
				setWidgetAlpha(1.0F);
			} else {
				float alpha = MathHelper.clampedMap(MathHelper.clamp(progress, 0.0F, 1.0F), 0.5F, 1.0F, 0.0F, 1.0F);
				setWidgetAlpha(alpha);
			}
		}

		if (closeTime > 0.0F) {
			float remaining = 1.0F - ((float) Util.getMeasuringTimeMs() - closeTime) / CLOSE_FADE_DURATION_MS;

			if (remaining <= 0.0F) {
				closeTime = 0.0F;
				saveAndRun(true, onClose);
			} else {
				float alpha = MathHelper.clampedMap(MathHelper.clamp(remaining, 0.0F, 1.0F), 0.5F, 1.0F, 0.0F, 1.0F);
				setWidgetAlpha(alpha);
			}
		}

		logoDrawer.draw(context, width, 1.0F);
	}

	@Override
	protected boolean allowRotatingPanorama() {
		return false;
	}

	private void setScreen(Screen screen) {
		saveAndRun(false, () -> client.setScreen(screen));
	}

	private void saveAndRun(boolean dontShowAgain, Runnable callback) {
		if (dontShowAgain) {
			gameOptions.setAccessibilityOnboarded();
		}

		Narrator.getNarrator().clear();
		callback.run();
	}

	private void tickNarratorPrompt() {
		if (narratorPrompted || !isNarratorUsable) {
			return;
		}

		if (narratorPromptTimer < NARRATOR_PROMPT_TICKS) {
			narratorPromptTimer++;
			return;
		}

		if (client.isWindowFocused()) {
			Narrator.getNarrator().say(
				NARRATOR_PROMPT.getString(),
				true,
				client.options.getSoundVolume(SoundCategory.VOICE)
			);
			narratorPrompted = true;
		}
	}
}
