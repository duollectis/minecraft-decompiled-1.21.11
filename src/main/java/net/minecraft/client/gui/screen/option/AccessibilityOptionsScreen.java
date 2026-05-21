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

@Environment(EnvType.CLIENT)
/**
 * {@code AccessibilityOptionsScreen}.
 */
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
		ClickableWidget clickableWidget = this.body.getWidgetFor(this.gameOptions.getHighContrast());
		if (clickableWidget != null && !this.client.getResourcePackManager().getIds().contains("high_contrast")) {
			clickableWidget.active = false;
			clickableWidget.setTooltip(Tooltip.of(Text.translatable("options.accessibility.high_contrast.error.tooltip")));
		}

		ClickableWidget clickableWidget2 = this.body.getWidgetFor(this.gameOptions.getRotateWithMinecart());
		if (clickableWidget2 != null) {
			clickableWidget2.active = this.isMinecartImprovementsExperimentEnabled();
		}
	}

	@Override
	protected void addOptions() {
		SimpleOption<?>[] simpleOptions = getOptions(this.gameOptions);
		ButtonWidget buttonWidget = ButtonWidget.builder(
				                                        OptionsScreen.CONTROL_TEXT,
				                                        buttonWidgetx -> this.client.setScreen(new ControlsOptionsScreen(this, this.gameOptions))
		                                        )
		                                        .build();
		SimpleOption<?> simpleOption = simpleOptions[0];
		this.body.addWidgetEntry(
				simpleOption.createWidget(this.gameOptions),
				this.gameOptions.getNarrator(),
				buttonWidget
		);
		this.body.addAll(Arrays
				.stream(simpleOptions)
				.filter(simpleOption2 -> simpleOption2 != simpleOption)
				.toArray(SimpleOption[]::new));
	}

	@Override
	protected void initFooter() {
		DirectionalLayoutWidget
				directionalLayoutWidget =
				this.layout.addFooter(DirectionalLayoutWidget.horizontal().spacing(8));
		directionalLayoutWidget.add(
				ButtonWidget
						.builder(
								Text.translatable("options.accessibility.link"),
								ConfirmLinkScreen.opening(this, Urls.JAVA_ACCESSIBILITY)
						)
						.build()
		);
		directionalLayoutWidget.add(ButtonWidget
				.builder(ScreenTexts.DONE, button -> this.client.setScreen(this.parent))
				.build());
	}

	@Override
	protected boolean allowRotatingPanorama() {
		return !(this.parent instanceof AccessibilityOnboardingScreen);
	}

	private boolean isMinecartImprovementsExperimentEnabled() {
		return this.client.world != null && this.client.world
				.getEnabledFeatures()
				.contains(FeatureFlags.MINECART_IMPROVEMENTS);
	}
}
