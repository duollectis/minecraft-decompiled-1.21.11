package net.minecraft.client.gui.screen.option;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.GraphicsWarningScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.option.TextureFilteringMode;
import net.minecraft.client.resource.VideoWarningManager;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.VideoMode;
import net.minecraft.client.util.Window;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
/**
 * {@code VideoOptionsScreen}.
 */
public class VideoOptionsScreen extends GameOptionsScreen {

	private static final Text TITLE_TEXT = Text.translatable("options.videoTitle");
	private static final Text
			IMPROVED_TRANSPARENCY_TEXT =
			Text.translatable("options.improvedTransparency").formatted(Formatting.ITALIC);
	private static final Text GRAPHICS_WARNING_MESSAGE_TEXT = Text.translatable(
			"options.graphics.warning.message", IMPROVED_TRANSPARENCY_TEXT, IMPROVED_TRANSPARENCY_TEXT
	);
	private static final Text
			GRAPHICS_WARNING_TITLE_TEXT =
			Text.translatable("options.graphics.warning.title").formatted(Formatting.RED);
	private static final Text GRAPHICS_WARNING_ACCEPT_TEXT = Text.translatable("options.graphics.warning.accept");
	private static final Text GRAPHICS_WARNING_CANCEL_TEXT = Text.translatable("options.graphics.warning.cancel");
	private static final Text DISPLAY_HEADER_TEXT = Text.translatable("options.video.display.header");
	private static final Text QUALITY_HEADER_TEXT = Text.translatable("options.video.quality.header");
	private static final Text INTERFACE_HEADER_TEXT = Text.translatable("options.video.preferences.header");
	private final VideoWarningManager warningManager;
	private final int mipmapLevels;
	private final int maxAnisotropy;
	private final TextureFilteringMode initialTextureFiltering;

	private static SimpleOption<?>[] getQualityOptions(GameOptions options) {
		return new SimpleOption[]{
				options.getBiomeBlendRadius(),
				options.getViewDistance(),
				options.getChunkBuilderMode(),
				options.getSimulationDistance(),
				options.getAo(),
				options.getCloudRenderMode(),
				options.getParticles(),
				options.getMipmapLevels(),
				options.getEntityShadows(),
				options.getEntityDistanceScaling(),
				options.getMenuBackgroundBlurriness(),
				options.getCloudRenderDistance(),
				options.getCutoutLeaves(),
				options.getImprovedTransparency(),
				options.getTextureFiltering(),
				options.getMaxAnisotropy(),
				options.getWeatherRadius()
		};
	}

	private static SimpleOption<?>[] getDisplayOptions(GameOptions options) {
		return new SimpleOption[]{
				options.getMaxFps(),
				options.getEnableVsync(),
				options.getInactivityFpsLimit(),
				options.getGuiScale(),
				options.getFullscreen(),
				options.getGamma()
		};
	}

	private static SimpleOption<?>[] getInterfaceOptions(GameOptions options) {
		return new SimpleOption[]{
				options.getShowAutosaveIndicator(),
				options.getVignette(),
				options.getAttackIndicator(),
				options.getChunkFade()
		};
	}

	public VideoOptionsScreen(Screen parent, MinecraftClient client, GameOptions gameOptions) {
		super(parent, gameOptions, TITLE_TEXT);
		this.warningManager = client.getVideoWarningManager();
		this.warningManager.reset();
		if (gameOptions.getImprovedTransparency().getValue()) {
			this.warningManager.acceptAfterWarnings();
		}

		this.mipmapLevels = gameOptions.getMipmapLevels().getValue();
		this.maxAnisotropy = gameOptions.getMaxAnisotropy().getValue();
		this.initialTextureFiltering = gameOptions.getTextureFiltering().getValue();
	}

	@Override
	protected void addOptions() {
		int i = -1;
		Window window = this.client.getWindow();
		Monitor monitor = window.getMonitor();
		int j;
		if (monitor == null) {
			j = -1;
		}
		else {
			Optional<VideoMode> optional = window.getFullscreenVideoMode();
			j = optional.map(monitor::findClosestVideoModeIndex).orElse(-1);
		}

		SimpleOption<Integer> simpleOption = new SimpleOption<>(
				"options.fullscreen.resolution",
				SimpleOption.emptyTooltip(),
				(optionText, value) -> {
					if (monitor == null) {
						return Text.translatable("options.fullscreen.unavailable");
					}
					else if (value == -1) {
						return GameOptions.getGenericValueText(
								optionText,
								Text.translatable("options.fullscreen.current")
						);
					}
					else {
						VideoMode videoMode = monitor.getVideoMode(value);
						return GameOptions.getGenericValueText(
								optionText,
								Text.translatable(
										"options.fullscreen.entry",
										videoMode.getWidth(),
										videoMode.getHeight(),
										videoMode.getRefreshRate(),
										videoMode.getRedBits() + videoMode.getGreenBits() + videoMode.getBlueBits()
								)
						);
					}
				},
				new SimpleOption.ValidatingIntSliderCallbacks(
						-1,
						monitor != null ? monitor.getVideoModeCount() - 1 : -1
				),
				j,
				value -> {
					if (monitor != null) {
						window.setFullscreenVideoMode(
								value == -1 ? Optional.empty() : Optional.of(monitor.getVideoMode(value)));
					}
				}
		);
		this.body.addHeader(DISPLAY_HEADER_TEXT);
		this.body.addSingleOptionEntry(simpleOption);
		this.body.addAll(getDisplayOptions(this.gameOptions));
		this.body.addHeader(QUALITY_HEADER_TEXT);
		this.body.addSingleOptionEntry(this.gameOptions.getPreset());
		this.body.addAll(getQualityOptions(this.gameOptions));
		this.body.addHeader(INTERFACE_HEADER_TEXT);
		this.body.addAll(getInterfaceOptions(this.gameOptions));
	}

	@Override
	public void tick() {
		if (this.body != null
				&& this.body.getWidgetFor(this.gameOptions.getMaxAnisotropy()) instanceof SliderWidget sliderWidget) {
			sliderWidget.active = this.gameOptions.getTextureFiltering().getValue() == TextureFilteringMode.ANISOTROPIC;
		}

		super.tick();
	}

	@Override
	public void close() {
		this.client.getWindow().applyFullscreenVideoMode();
		super.close();
	}

	@Override
	public void removed() {
		if (this.gameOptions.getMipmapLevels().getValue() != this.mipmapLevels
				|| this.gameOptions.getMaxAnisotropy().getValue() != this.maxAnisotropy
				|| this.gameOptions.getTextureFiltering().getValue() != this.initialTextureFiltering) {
			this.client.setMipmapLevels(this.gameOptions.getMipmapLevels().getValue());
			this.client.reloadResourcesConcurrently();
		}

		super.removed();
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (super.mouseClicked(click, doubled)) {
			if (this.warningManager.shouldWarn()) {
				List<Text> list = Lists.newArrayList(new Text[]{GRAPHICS_WARNING_MESSAGE_TEXT, ScreenTexts.LINE_BREAK});
				String string = this.warningManager.getRendererWarning();
				if (string != null) {
					list.add(ScreenTexts.LINE_BREAK);
					list.add(Text.translatable("options.graphics.warning.renderer", string).formatted(Formatting.GRAY));
				}

				String string2 = this.warningManager.getVendorWarning();
				if (string2 != null) {
					list.add(ScreenTexts.LINE_BREAK);
					list.add(Text.translatable("options.graphics.warning.vendor", string2).formatted(Formatting.GRAY));
				}

				String string3 = this.warningManager.getVersionWarning();
				if (string3 != null) {
					list.add(ScreenTexts.LINE_BREAK);
					list.add(Text.translatable("options.graphics.warning.version", string3).formatted(Formatting.GRAY));
				}

				this.client
						.setScreen(
								new GraphicsWarningScreen(
										GRAPHICS_WARNING_TITLE_TEXT, list, ImmutableList.of(
										new GraphicsWarningScreen.ChoiceButton(
												GRAPHICS_WARNING_ACCEPT_TEXT, button -> {
											this.gameOptions.getImprovedTransparency().setValue(true);
											MinecraftClient.getInstance().worldRenderer.reload();
											this.warningManager.acceptAfterWarnings();
											this.client.setScreen(this);
										}
										), new GraphicsWarningScreen.ChoiceButton(
												GRAPHICS_WARNING_CANCEL_TEXT, button -> {
											this.warningManager.acceptAfterWarnings();
											this.gameOptions.getImprovedTransparency().setValue(false);
											this.updateImprovedTransparencyButtonValue();
											this.client.setScreen(this);
										}
										)
								)
								)
						);
			}

			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (this.client.isCtrlPressed()) {
			SimpleOption<Integer> simpleOption = this.gameOptions.getGuiScale();
			if (simpleOption.getCallbacks() instanceof SimpleOption.MaxSuppliableIntCallbacks maxSuppliableIntCallbacks) {
				int i = simpleOption.getValue();
				int j = i == 0 ? maxSuppliableIntCallbacks.maxInclusive() + 1 : i;
				int k = j + (int) Math.signum(verticalAmount);
				if (k != 0 && k <= maxSuppliableIntCallbacks.maxInclusive()
						&& k >= maxSuppliableIntCallbacks.minInclusive()) {
					CyclingButtonWidget<Integer>
							cyclingButtonWidget =
							(CyclingButtonWidget<Integer>) this.body.getWidgetFor(simpleOption);
					if (cyclingButtonWidget != null) {
						simpleOption.setValue(k);
						cyclingButtonWidget.setValue(k);
						this.body.setScrollY(0.0);
						return true;
					}
				}
			}

			return false;
		}
		else {
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}
	}

	/**
	 * Обновляет fullscreen button value.
	 *
	 * @param fullscreen fullscreen
	 */
	public void updateFullscreenButtonValue(boolean fullscreen) {
		if (this.body != null) {
			ClickableWidget clickableWidget = this.body.getWidgetFor(this.gameOptions.getFullscreen());
			if (clickableWidget != null) {
				CyclingButtonWidget<Boolean> cyclingButtonWidget = (CyclingButtonWidget<Boolean>) clickableWidget;
				cyclingButtonWidget.setValue(fullscreen);
			}
		}
	}

	/**
	 * Обновляет improved transparency button value.
	 */
	public void updateImprovedTransparencyButtonValue() {
		if (this.body != null) {
			SimpleOption<Boolean> simpleOption = this.gameOptions.getImprovedTransparency();
			ClickableWidget clickableWidget = this.body.getWidgetFor(simpleOption);
			if (clickableWidget != null) {
				CyclingButtonWidget<Boolean> cyclingButtonWidget = (CyclingButtonWidget<Boolean>) clickableWidget;
				cyclingButtonWidget.setValue(simpleOption.getValue());
			}
		}
	}
}
