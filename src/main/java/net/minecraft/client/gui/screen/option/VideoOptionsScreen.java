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

/**
 * Экран настроек видео — управляет разрешением, качеством графики,
 * параметрами отображения и интерфейса. Показывает предупреждение при включении
 * улучшенной прозрачности на несовместимых видеокартах.
 */
@Environment(EnvType.CLIENT)
public class VideoOptionsScreen extends GameOptionsScreen {

	private static final Text TITLE_TEXT = Text.translatable("options.videoTitle");
	private static final Text IMPROVED_TRANSPARENCY_TEXT =
			Text.translatable("options.improvedTransparency").formatted(Formatting.ITALIC);
	private static final Text GRAPHICS_WARNING_MESSAGE_TEXT = Text.translatable(
			"options.graphics.warning.message", IMPROVED_TRANSPARENCY_TEXT, IMPROVED_TRANSPARENCY_TEXT
	);
	private static final Text GRAPHICS_WARNING_TITLE_TEXT =
			Text.translatable("options.graphics.warning.title").formatted(Formatting.RED);
	private static final Text GRAPHICS_WARNING_ACCEPT_TEXT = Text.translatable("options.graphics.warning.accept");
	private static final Text GRAPHICS_WARNING_CANCEL_TEXT = Text.translatable("options.graphics.warning.cancel");
	private static final Text DISPLAY_HEADER_TEXT = Text.translatable("options.video.display.header");
	private static final Text QUALITY_HEADER_TEXT = Text.translatable("options.video.quality.header");
	private static final Text INTERFACE_HEADER_TEXT = Text.translatable("options.video.preferences.header");
	private static final int DEFAULT_RESOLUTION_INDEX = -1;

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
		warningManager = client.getVideoWarningManager();
		warningManager.reset();
		if (gameOptions.getImprovedTransparency().getValue()) {
			warningManager.acceptAfterWarnings();
		}

		mipmapLevels = gameOptions.getMipmapLevels().getValue();
		maxAnisotropy = gameOptions.getMaxAnisotropy().getValue();
		initialTextureFiltering = gameOptions.getTextureFiltering().getValue();
	}

	@Override
	protected void addOptions() {
		Window window = client.getWindow();
		Monitor monitor = window.getMonitor();
		int currentResolutionIndex = monitor == null
				? DEFAULT_RESOLUTION_INDEX
				: window.getFullscreenVideoMode()
						.map(monitor::findClosestVideoModeIndex)
						.orElse(DEFAULT_RESOLUTION_INDEX);

		SimpleOption<Integer> resolutionOption = new SimpleOption<>(
				"options.fullscreen.resolution",
				SimpleOption.emptyTooltip(),
				(optionText, value) -> {
					if (monitor == null) {
						return Text.translatable("options.fullscreen.unavailable");
					}

					if (value == DEFAULT_RESOLUTION_INDEX) {
						return GameOptions.getGenericValueText(
								optionText,
								Text.translatable("options.fullscreen.current")
						);
					}

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
				},
				new SimpleOption.ValidatingIntSliderCallbacks(
						DEFAULT_RESOLUTION_INDEX,
						monitor != null ? monitor.getVideoModeCount() - 1 : DEFAULT_RESOLUTION_INDEX
				),
				currentResolutionIndex,
				value -> {
					if (monitor != null) {
						window.setFullscreenVideoMode(
								value == DEFAULT_RESOLUTION_INDEX
										? Optional.empty()
										: Optional.of(monitor.getVideoMode(value))
						);
					}
				}
		);
		body.addHeader(DISPLAY_HEADER_TEXT);
		body.addSingleOptionEntry(resolutionOption);
		body.addAll(getDisplayOptions(gameOptions));
		body.addHeader(QUALITY_HEADER_TEXT);
		body.addSingleOptionEntry(gameOptions.getPreset());
		body.addAll(getQualityOptions(gameOptions));
		body.addHeader(INTERFACE_HEADER_TEXT);
		body.addAll(getInterfaceOptions(gameOptions));
	}

	@Override
	public void tick() {
		if (body != null
				&& body.getWidgetFor(gameOptions.getMaxAnisotropy()) instanceof SliderWidget sliderWidget
		) {
			sliderWidget.active = gameOptions.getTextureFiltering().getValue() == TextureFilteringMode.ANISOTROPIC;
		}

		super.tick();
	}

	@Override
	public void close() {
		client.getWindow().applyFullscreenVideoMode();
		super.close();
	}

	@Override
	public void removed() {
		if (gameOptions.getMipmapLevels().getValue() != mipmapLevels
				|| gameOptions.getMaxAnisotropy().getValue() != maxAnisotropy
				|| gameOptions.getTextureFiltering().getValue() != initialTextureFiltering
		) {
			client.setMipmapLevels(gameOptions.getMipmapLevels().getValue());
			client.reloadResourcesConcurrently();
		}

		super.removed();
	}

	/**
	 * Перехватывает клик после обработки родительским классом — если менеджер предупреждений
	 * требует показа диалога о совместимости графики, открывает экран предупреждения.
	 */
	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (!super.mouseClicked(click, doubled)) {
			return false;
		}

		if (!warningManager.shouldWarn()) {
			return true;
		}

		List<Text> warningLines = Lists.newArrayList(new Text[]{GRAPHICS_WARNING_MESSAGE_TEXT, ScreenTexts.LINE_BREAK});
		String rendererWarning = warningManager.getRendererWarning();
		if (rendererWarning != null) {
			warningLines.add(ScreenTexts.LINE_BREAK);
			warningLines.add(Text.translatable("options.graphics.warning.renderer", rendererWarning).formatted(Formatting.GRAY));
		}

		String vendorWarning = warningManager.getVendorWarning();
		if (vendorWarning != null) {
			warningLines.add(ScreenTexts.LINE_BREAK);
			warningLines.add(Text.translatable("options.graphics.warning.vendor", vendorWarning).formatted(Formatting.GRAY));
		}

		String versionWarning = warningManager.getVersionWarning();
		if (versionWarning != null) {
			warningLines.add(ScreenTexts.LINE_BREAK);
			warningLines.add(Text.translatable("options.graphics.warning.version", versionWarning).formatted(Formatting.GRAY));
		}

		client.setScreen(
				new GraphicsWarningScreen(
						GRAPHICS_WARNING_TITLE_TEXT,
						warningLines,
						ImmutableList.of(
								new GraphicsWarningScreen.ChoiceButton(
										GRAPHICS_WARNING_ACCEPT_TEXT, button -> {
											gameOptions.getImprovedTransparency().setValue(true);
											MinecraftClient.getInstance().worldRenderer.reload();
											warningManager.acceptAfterWarnings();
											client.setScreen(this);
										}
								),
								new GraphicsWarningScreen.ChoiceButton(
										GRAPHICS_WARNING_CANCEL_TEXT, button -> {
											warningManager.acceptAfterWarnings();
											gameOptions.getImprovedTransparency().setValue(false);
											updateImprovedTransparencyButtonValue();
											client.setScreen(this);
										}
								)
						)
				)
		);
		return true;
	}

	/**
	 * Обрабатывает прокрутку колёсиком мыши с зажатым Ctrl — изменяет масштаб GUI.
	 */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (!client.isCtrlPressed()) {
			return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}

		SimpleOption<Integer> guiScaleOption = gameOptions.getGuiScale();
		if (!(guiScaleOption.getCallbacks() instanceof SimpleOption.MaxSuppliableIntCallbacks maxCallbacks)) {
			return false;
		}

		int currentScale = guiScaleOption.getValue();
		int adjustedScale = currentScale == 0 ? maxCallbacks.maxInclusive() + 1 : currentScale;
		int newScale = adjustedScale + (int) Math.signum(verticalAmount);

		if (newScale == 0 || newScale > maxCallbacks.maxInclusive() || newScale < maxCallbacks.minInclusive()) {
			return false;
		}

		CyclingButtonWidget<Integer> scaleButton = (CyclingButtonWidget<Integer>) body.getWidgetFor(guiScaleOption);
		if (scaleButton == null) {
			return false;
		}

		guiScaleOption.setValue(newScale);
		scaleButton.setValue(newScale);
		body.setScrollY(0.0);
		return true;
	}

	public void updateFullscreenButtonValue(boolean fullscreen) {
		if (body == null) {
			return;
		}

		ClickableWidget widget = body.getWidgetFor(gameOptions.getFullscreen());
		if (widget != null) {
			((CyclingButtonWidget<Boolean>) widget).setValue(fullscreen);
		}
	}

	public void updateImprovedTransparencyButtonValue() {
		if (body == null) {
			return;
		}

		SimpleOption<Boolean> transparencyOption = gameOptions.getImprovedTransparency();
		ClickableWidget widget = body.getWidgetFor(transparencyOption);
		if (widget != null) {
			((CyclingButtonWidget<Boolean>) widget).setValue(transparencyOption.getValue());
		}
	}
}
