package net.minecraft.client.gui.screen.option;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Urls;

import java.util.Arrays;

/**
 * Экран настроек доступности — управляет нарратором, субтитрами, контрастностью
 * и другими параметрами для пользователей с ограниченными возможностями.
 */
@Environment(EnvType.CLIENT)
public class AccessibilityOptionsScreen extends GameOptionsScreen {

	public static final Text TITLE_TEXT = Text.translatable("options.accessibility.title");

	private static SimpleOption<?>[] getOptions(GameOptions gameOptions) {
		return new SimpleOption[]{
			gameOptions.getNarrator(),
			gameOptions.getShowSubtitles(),
			gameOptions.getHighContrast(),
			gameOptions.getMenuBackgroundBlurriness(),
			gameOptions.getTextBackgroundOpacity(),
			gameOptions.getBackgroundForChatOnly(),
			gameOptions.getChatOpacity(),
			gameOptions.getChatLineSpacing(),
			gameOptions.getChatDelay(),
			gameOptions.getNotificationDisplayTime(),
			gameOptions.getBobView(),
			gameOptions.getDistortionEffectScale(),
			gameOptions.getFovEffectScale(),
			gameOptions.getDarknessEffectScale(),
			gameOptions.getDamageTiltStrength(),
			gameOptions.getGlintSpeed(),
			gameOptions.getGlintStrength(),
			gameOptions.getHideLightningFlashes(),
			gameOptions.getMonochromeLogo(),
			gameOptions.getPanoramaSpeed(),
			gameOptions.getHideSplashTexts(),
			gameOptions.getNarratorHotkey(),
			gameOptions.getRotateWithMinecart(),
			gameOptions.getHighContrastBlockOutline()
		};
	}

	public AccessibilityOptionsScreen(Screen parent, GameOptions gameOptions) {
		super(parent, gameOptions, TITLE_TEXT);
	}

	@Override
	protected void init() {
		super.init();
		ClickableWidget highContrastWidget = body.getWidgetFor(gameOptions.getHighContrast());
		if (highContrastWidget != null && !client.getResourcePackManager().getIds().contains("high_contrast")) {
			highContrastWidget.active = false;
			highContrastWidget.setTooltip(Tooltip.of(Text.translatable("options.accessibility.high_contrast.error.tooltip")));
		}

		ClickableWidget minecartWidget = body.getWidgetFor(gameOptions.getRotateWithMinecart());
		if (minecartWidget != null) {
			minecartWidget.active = isMinecartImprovementsExperimentEnabled();
		}
	}

	@Override
	protected void addOptions() {
		SimpleOption<?>[] options = getOptions(gameOptions);
		ButtonWidget controlsButton = ButtonWidget.builder(
			OptionsScreen.CONTROL_TEXT,
			button -> client.setScreen(new ControlsOptionsScreen(this, gameOptions))
		).build();
		SimpleOption<?> narratorOption = options[0];
		body.addWidgetEntry(narratorOption.createWidget(gameOptions), gameOptions.getNarrator(), controlsButton);
		body.addAll(Arrays.stream(options).filter(opt -> opt != narratorOption).toArray(SimpleOption[]::new));
	}

	@Override
	protected void initFooter() {
		DirectionalLayoutWidget footerLayout = layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		footerLayout.add(
			ButtonWidget.builder(
				Text.translatable("options.accessibility.link"),
				ConfirmLinkScreen.opening(this, Urls.JAVA_ACCESSIBILITY)
			).build()
		);
		footerLayout.add(ButtonWidget.builder(ScreenTexts.DONE, button -> client.setScreen(parent)).build());
	}

	@Override
	protected boolean allowRotatingPanorama() {
		return !(parent instanceof AccessibilityOnboardingScreen);
	}

	private boolean isMinecartImprovementsExperimentEnabled() {
		return client.world != null && client.world.getEnabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
	}
}
