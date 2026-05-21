package net.minecraft.client.option;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.input.SystemKeycodes;
import net.minecraft.client.render.ChunkBuilderMode;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.resource.VideoWarningManager;
import net.minecraft.client.sound.*;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Environment(EnvType.CLIENT)
/**
 * {@code GameOptions}.
 */
public class GameOptions {

	static final Logger LOGGER = LogUtils.getLogger();
	static final Gson GSON = new Gson();
	private static final TypeToken<List<String>> STRING_LIST_TYPE = new TypeToken<List<String>>() {};
	public static final int CHUNK_SECTION_SIZE = 4;
	public static final int RENDER_DISTANCE_NEAR = 12;
	public static final int RENDER_DISTANCE_NORMAL = 16;
	public static final int RENDER_DISTANCE_FAR = 32;
	private static final Splitter COLON_SPLITTER = Splitter.on(':').limit(2);
	public static final String EMPTY_STRING = "";
	private static final Text
			DARK_MOJANG_STUDIOS_BACKGROUND_COLOR_TOOLTIP =
			Text.translatable("options.darkMojangStudiosBackgroundColor.tooltip");
	private final SimpleOption<Boolean> monochromeLogo = SimpleOption.ofBoolean(
			"options.darkMojangStudiosBackgroundColor",
			SimpleOption.constantTooltip(DARK_MOJANG_STUDIOS_BACKGROUND_COLOR_TOOLTIP),
			false
	);
	private static final Text
			HIDE_LIGHTNING_FLASHES_TOOLTIP =
			Text.translatable("options.hideLightningFlashes.tooltip");
	private final SimpleOption<Boolean> hideLightningFlashes = SimpleOption.ofBoolean(
			"options.hideLightningFlashes", SimpleOption.constantTooltip(HIDE_LIGHTNING_FLASHES_TOOLTIP), false
	);
	private static final Text HIDE_SPLASH_TEXTS_TOOLTIP = Text.translatable("options.hideSplashTexts.tooltip");
	private final SimpleOption<Boolean> hideSplashTexts = SimpleOption.ofBoolean(
			"options.hideSplashTexts", SimpleOption.constantTooltip(HIDE_SPLASH_TEXTS_TOOLTIP), false
	);
	private final SimpleOption<Double> mouseSensitivity = new SimpleOption<>(
			"options.sensitivity", SimpleOption.emptyTooltip(), (optionText, value) -> {
		if (value == 0.0) {
			return getGenericValueText(optionText, Text.translatable("options.sensitivity.min"));
		}
		else {
			return value == 1.0 ? getGenericValueText(optionText, Text.translatable("options.sensitivity.max"))
			                    : getPercentValueText(optionText, 2.0 * value);
		}
	}, SimpleOption.DoubleSliderCallbacks.INSTANCE, 0.5, value -> {}
	);
	private final SimpleOption<Integer> viewDistance;
	private final SimpleOption<Integer> simulationDistance;
	private int serverViewDistance = 0;
	private final SimpleOption<Double> entityDistanceScaling = new SimpleOption<>(
			"options.entityDistanceScaling",
			SimpleOption.emptyTooltip(),
			GameOptions::getPercentValueText,
			new SimpleOption.ValidatingIntSliderCallbacks(2, 20).withModifier(
					sliderProgressValue -> sliderProgressValue / 4.0, value -> (int) (value * 4.0), true),
			Codec.doubleRange(0.5, 5.0),
			1.0,
			value -> this.onChangeGraphicsOption()
	);
	public static final int MAX_FPS_LIMIT = 260;
	private final SimpleOption<Integer> maxFps = new SimpleOption<>(
			"options.framerateLimit",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value == 260
			                       ? getGenericValueText(optionText, Text.translatable("options.framerateLimit.max"))
			                       : getGenericValueText(optionText, Text.translatable("options.framerate", value)),
			new SimpleOption.ValidatingIntSliderCallbacks(1, 26).withModifier(
					value -> value * 10,
					value -> value / 10,
					true
			),
			Codec.intRange(10, 260),
			120,
			value -> MinecraftClient.getInstance().getInactivityFpsLimiter().setMaxFps(value)
	);
	private boolean applyingGraphicsMode;
	private final SimpleOption<GraphicsMode> preset = new SimpleOption<>(
			"options.graphics.preset",
			SimpleOption.constantTooltip(Text.translatable("options.graphics.preset.tooltip")),
			(optionText, value) -> getGenericValueText(optionText, Text.translatable(value.getTranslationKey())),
			new SimpleOption.CategoricalSliderCallbacks<>(List.of(GraphicsMode.values()), GraphicsMode.CODEC),
			GraphicsMode.CODEC,
			GraphicsMode.FANCY,
			this::applyGraphicsMode
	);
	private static final Text
			INACTIVITY_FPS_LIMIT_MINIMIZED_TOOLTIP =
			Text.translatable("options.inactivityFpsLimit.minimized.tooltip");
	private static final Text
			INACTIVITY_FPS_LIMIT_AFK_TOOLTIP =
			Text.translatable("options.inactivityFpsLimit.afk.tooltip");
	private final SimpleOption<InactivityFpsLimit> inactivityFpsLimit = new SimpleOption<>(
			"options.inactivityFpsLimit",
			option -> {
				return switch (option) {
					case MINIMIZED -> Tooltip.of(INACTIVITY_FPS_LIMIT_MINIMIZED_TOOLTIP);
					case AFK -> Tooltip.of(INACTIVITY_FPS_LIMIT_AFK_TOOLTIP);
				};
			},
			(optionText, value) -> value.getText(),
			new SimpleOption.PotentialValuesBasedCallbacks<>(
					Arrays.asList(InactivityFpsLimit.values()),
					InactivityFpsLimit.CODEC
			),
			InactivityFpsLimit.AFK,
			inactivityFpsLimit -> {}
	);
	private final SimpleOption<CloudRenderMode> cloudRenderMode = new SimpleOption<>(
			"options.renderClouds",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value.getText(),
			new SimpleOption.PotentialValuesBasedCallbacks<>(
					Arrays.asList(CloudRenderMode.values()),
					Codec.withAlternative(
							CloudRenderMode.CODEC,
							Codec.BOOL,
							value -> value ? CloudRenderMode.FANCY : CloudRenderMode.OFF
					)
			),
			CloudRenderMode.FANCY,
			value -> this.onChangeGraphicsOption()
	);
	private final SimpleOption<Integer> cloudRenderDistance = new SimpleOption<>(
			"options.renderCloudsDistance",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> getGenericValueText(optionText, Text.translatable("options.chunks", value)),
			new SimpleOption.ValidatingIntSliderCallbacks(2, 128, true),
			128,
			value -> {
				refreshWorldRenderer(worldRenderer -> worldRenderer.getCloudRenderer().scheduleTerrainUpdate());
				this.onChangeGraphicsOption();
			}
	);
	private static final Text WEATHER_RADIUS_TOOLTIP = Text.translatable("options.weatherRadius.tooltip");
	private final SimpleOption<Integer> weatherRadius = new SimpleOption<>(
			"options.weatherRadius",
			SimpleOption.constantTooltip(WEATHER_RADIUS_TOOLTIP),
			(optionText, value) -> getGenericValueText(optionText, Text.translatable("options.blocks", value)),
			new SimpleOption.ValidatingIntSliderCallbacks(3, 10, true),
			10,
			value -> this.onChangeGraphicsOption()
	);
	private static final Text CUTOUT_LEAVES_TOOLTIP = Text.translatable("options.cutoutLeaves.tooltip");
	private final SimpleOption<Boolean> cutoutLeaves = SimpleOption.ofBoolean(
			"options.cutoutLeaves", SimpleOption.constantTooltip(CUTOUT_LEAVES_TOOLTIP), true, boolean_ -> {
				refreshWorldRenderer(WorldRenderer::reload);
				this.onChangeGraphicsOption();
			}
	);
	private static final Text VIGNETTE_TOOLTIP = Text.translatable("options.vignette.tooltip");
	private final SimpleOption<Boolean>
			vignette =
			SimpleOption.ofBoolean("options.vignette", SimpleOption.constantTooltip(VIGNETTE_TOOLTIP), true);
	private static final Text IMPROVED_TRANSPARENCY_TOOLTIP = Text.translatable("options.improvedTransparency.tooltip");
	private final SimpleOption<Boolean> improvedTransparency = SimpleOption.ofBoolean(
			"options.improvedTransparency",
			SimpleOption.constantTooltip(IMPROVED_TRANSPARENCY_TOOLTIP),
			false,
			value -> {
				MinecraftClient minecraftClient = MinecraftClient.getInstance();
				VideoWarningManager videoWarningManager = minecraftClient.getVideoWarningManager();
				if (value && videoWarningManager.canWarn()) {
					videoWarningManager.scheduleWarning();
				}
				else {
					refreshWorldRenderer(WorldRenderer::reload);
					this.onChangeGraphicsOption();
				}
			}
	);
	private final SimpleOption<Boolean> ao = SimpleOption.ofBoolean(
			"options.ao", true, value -> {
				refreshWorldRenderer(WorldRenderer::reload);
				this.onChangeGraphicsOption();
			}
	);
	private static final Text CHUNK_FADE_TOOLTIP = Text.translatable("options.chunkFade.tooltip");
	private final SimpleOption<Double> chunkFade = new SimpleOption<>(
			"options.chunkFade",
			SimpleOption.constantTooltip(CHUNK_FADE_TOOLTIP),
			(optionText, value) -> value <= 0.0
			                       ? Text.translatable("options.chunkFade.none")
			                       : Text.translatable(
					                       "options.chunkFade.seconds",
					                       String.format(Locale.ROOT, "%.2f", value)
			                       ),
			new SimpleOption.ValidatingIntSliderCallbacks(0, 40).withModifier(
					ticks -> ticks / 20.0,
					seconds -> (int) (seconds * 20.0),
					true
			),
			Codec.doubleRange(0.0, 2.0),
			0.75,
			value -> {}
	);
	private static final Text
			NONE_CHUNK_BUILDER_MODE_TOOLTIP =
			Text.translatable("options.prioritizeChunkUpdates.none.tooltip");
	private static final Text
			BY_PLAYER_CHUNK_BUILDER_MODE_TOOLTIP =
			Text.translatable("options.prioritizeChunkUpdates.byPlayer.tooltip");
	private static final Text
			NEARBY_CHUNK_BUILDER_MODE_TOOLTIP =
			Text.translatable("options.prioritizeChunkUpdates.nearby.tooltip");
	private final SimpleOption<ChunkBuilderMode> chunkBuilderMode = new SimpleOption<>(
			"options.prioritizeChunkUpdates",
			value -> {
				return switch (value) {
					case NONE -> Tooltip.of(NONE_CHUNK_BUILDER_MODE_TOOLTIP);
					case PLAYER_AFFECTED -> Tooltip.of(BY_PLAYER_CHUNK_BUILDER_MODE_TOOLTIP);
					case NEARBY -> Tooltip.of(NEARBY_CHUNK_BUILDER_MODE_TOOLTIP);
				};
			},
			(optionText, value) -> value.getText(),
			new SimpleOption.PotentialValuesBasedCallbacks<>(
					Arrays.asList(ChunkBuilderMode.values()),
					ChunkBuilderMode.CODEC
			),
			ChunkBuilderMode.NONE,
			value -> this.onChangeGraphicsOption()
	);
	public List<String> resourcePacks = Lists.newArrayList();
	public List<String> incompatibleResourcePacks = Lists.newArrayList();
	private final SimpleOption<ChatVisibility> chatVisibility = new SimpleOption<>(
			"options.chat.visibility",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value.getText(),
			new SimpleOption.PotentialValuesBasedCallbacks<>(
					Arrays.asList(ChatVisibility.values()),
					ChatVisibility.CODEC
			),
			ChatVisibility.FULL,
			value -> {}
	);
	private final SimpleOption<Double> chatOpacity = new SimpleOption<>(
			"options.chat.opacity",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> getPercentValueText(optionText, value * 0.9 + 0.1),
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			1.0,
			value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
	);
	private final SimpleOption<Double> chatLineSpacing = new SimpleOption<>(
			"options.chat.line_spacing",
			SimpleOption.emptyTooltip(),
			GameOptions::getPercentValueText,
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			0.0,
			value -> {}
	);
	private static final Text
			MENU_BACKGROUND_BLURRINESS_TOOLTIP =
			Text.translatable("options.accessibility.menu_background_blurriness.tooltip");
	private static final int DEFAULT_MENU_BACKGROUND_BLURRINESS = 5;
	private final SimpleOption<Integer> menuBackgroundBlurriness = new SimpleOption<>(
			"options.accessibility.menu_background_blurriness",
			SimpleOption.constantTooltip(MENU_BACKGROUND_BLURRINESS_TOOLTIP),
			GameOptions::getGenericValueOrOffText,
			new SimpleOption.ValidatingIntSliderCallbacks(0, 10),
			5,
			value -> this.onChangeGraphicsOption()
	);
	private final SimpleOption<Double> textBackgroundOpacity = new SimpleOption<>(
			"options.accessibility.text_background_opacity",
			SimpleOption.emptyTooltip(),
			GameOptions::getPercentValueText,
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			0.5,
			value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
	);
	private final SimpleOption<Double> panoramaSpeed = new SimpleOption<>(
			"options.accessibility.panorama_speed",
			SimpleOption.emptyTooltip(),
			GameOptions::getPercentValueText,
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			1.0,
			value -> {}
	);
	private static final Text HIGH_CONTRAST_TOOLTIP = Text.translatable("options.accessibility.high_contrast.tooltip");
	private final SimpleOption<Boolean> highContrast = SimpleOption.ofBoolean(
			"options.accessibility.high_contrast",
			SimpleOption.constantTooltip(HIGH_CONTRAST_TOOLTIP),
			false,
			value -> {
				ResourcePackManager resourcePackManager = MinecraftClient.getInstance().getResourcePackManager();
				boolean blx = resourcePackManager.getEnabledIds().contains("high_contrast");
				if (!blx && value) {
					if (resourcePackManager.enable("high_contrast")) {
						this.refreshResourcePacks(resourcePackManager);
					}
				}
				else if (blx && !value && resourcePackManager.disable("high_contrast")) {
					this.refreshResourcePacks(resourcePackManager);
				}
			}
	);
	private static final Text
			HIGH_CONTRAST_BLOCK_OUTLINE_TOOLTIP =
			Text.translatable("options.accessibility.high_contrast_block_outline.tooltip");
	private final SimpleOption<Boolean> highContrastBlockOutline = SimpleOption.ofBoolean(
			"options.accessibility.high_contrast_block_outline",
			SimpleOption.constantTooltip(HIGH_CONTRAST_BLOCK_OUTLINE_TOOLTIP),
			false
	);
	private final SimpleOption<Boolean> narratorHotkey = SimpleOption.ofBoolean(
			"options.accessibility.narrator_hotkey",
			SimpleOption.constantTooltip(
					SystemKeycodes.IS_MAC_OS
					? Text.translatable("options.accessibility.narrator_hotkey.mac.tooltip")
					: Text.translatable("options.accessibility.narrator_hotkey.tooltip")
			),
			true
	);
	public @Nullable String fullscreenResolution;
	public boolean hideServerAddress;
	public boolean advancedItemTooltips;
	public boolean pauseOnLostFocus = true;
	private final Set<PlayerModelPart> enabledPlayerModelParts = EnumSet.allOf(PlayerModelPart.class);
	private final SimpleOption<Arm> mainArm = new SimpleOption<>(
			"options.mainHand",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value.getText(),
			new SimpleOption.PotentialValuesBasedCallbacks<>(Arrays.asList(Arm.values()), Arm.CODEC),
			Arm.RIGHT,
			value -> {}
	);
	public int overrideWidth;
	public int overrideHeight;
	private final SimpleOption<Double> chatScale = new SimpleOption<>(
			"options.chat.scale",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> (Text) (value == 0.0 ? ScreenTexts.composeToggleText(optionText, false)
			                                            : getPercentValueText(optionText, value)
			),
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			1.0,
			value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
	);
	private final SimpleOption<Double> chatWidth = new SimpleOption<>(
			"options.chat.width",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> getPixelValueText(optionText, ChatHud.getWidth(value)),
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			1.0,
			value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
	);
	private final SimpleOption<Double> chatHeightUnfocused = new SimpleOption<>(
			"options.chat.height.unfocused",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> getPixelValueText(optionText, ChatHud.getHeight(value)),
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			ChatHud.getDefaultUnfocusedHeight(),
			value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
	);
	private final SimpleOption<Double> chatHeightFocused = new SimpleOption<>(
			"options.chat.height.focused",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> getPixelValueText(optionText, ChatHud.getHeight(value)),
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			1.0,
			value -> MinecraftClient.getInstance().inGameHud.getChatHud().reset()
	);
	private final SimpleOption<Double> chatDelay = new SimpleOption<>(
			"options.chat.delay_instant",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value <= 0.0
			                       ? Text.translatable("options.chat.delay_none")
			                       : Text.translatable("options.chat.delay", String.format(Locale.ROOT, "%.1f", value)),
			new SimpleOption.ValidatingIntSliderCallbacks(0, 60).withModifier(
					value -> value / 10.0,
					value -> (int) (value * 10.0),
					true
			),
			Codec.doubleRange(0.0, 6.0),
			0.0,
			value -> MinecraftClient.getInstance().getMessageHandler().setChatDelay(value)
	);
	private static final Text
			NOTIFICATION_DISPLAY_TIME_TOOLTIP =
			Text.translatable("options.notifications.display_time.tooltip");
	private final SimpleOption<Double> notificationDisplayTime = new SimpleOption<>(
			"options.notifications.display_time",
			SimpleOption.constantTooltip(NOTIFICATION_DISPLAY_TIME_TOOLTIP),
			(optionText, value) -> getGenericValueText(optionText, Text.translatable("options.multiplier", value)),
			new SimpleOption.ValidatingIntSliderCallbacks(5, 100).withModifier(
					sliderProgressValue -> sliderProgressValue / 10.0, value -> (int) (value * 10.0), true),
			Codec.doubleRange(0.5, 10.0),
			1.0,
			value -> {}
	);
	private final SimpleOption<Integer> mipmapLevels = new SimpleOption<>(
			"options.mipmapLevels",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> (Text) (value == 0 ? ScreenTexts.composeToggleText(optionText, false)
			                                          : getGenericValueText(optionText, value)
			),
			new SimpleOption.ValidatingIntSliderCallbacks(0, 4),
			4,
			value -> this.onChangeGraphicsOption()
	);
	private static final Text MAX_ANISOTROPY_TOOLTIP = Text.translatable("options.maxAnisotropy.tooltip");
	private final SimpleOption<Integer> maxAnisotropy = new SimpleOption<>(
			"options.maxAnisotropy",
			SimpleOption.constantTooltip(MAX_ANISOTROPY_TOOLTIP),
			(optionText, value) -> (Text) (value == 0
			                               ? ScreenTexts.composeToggleText(optionText, false)
			                               : getGenericValueText(
					                               optionText,
					                               Text.translatable("options.multiplier", Integer.toString(1 << value))
			                               )
			),
			new SimpleOption.ValidatingIntSliderCallbacks(1, 3),
			2,
			value -> {
				this.onChangeGraphicsOption();
				refreshWorldRenderer(WorldRenderer::refreshTerrainSampler);
			}
	);
	private static final Text
			TEXTURE_FILTERING_NONE_TOOLTIP =
			Text.translatable("options.textureFiltering.none.tooltip");
	private static final Text
			TEXTURE_FILTERING_RGSS_TOOLTIP =
			Text.translatable("options.textureFiltering.rgss.tooltip");
	private static final Text
			TEXTURE_FILTERING_ANISOTROPIC_TOOLTIP =
			Text.translatable("options.textureFiltering.anisotropic.tooltip");
	private final SimpleOption<TextureFilteringMode> textureFiltering = new SimpleOption<>(
			"options.textureFiltering",
			mode -> {
				return switch (mode) {
					case NONE -> Tooltip.of(TEXTURE_FILTERING_NONE_TOOLTIP);
					case RGSS -> Tooltip.of(TEXTURE_FILTERING_RGSS_TOOLTIP);
					case ANISOTROPIC -> Tooltip.of(TEXTURE_FILTERING_ANISOTROPIC_TOOLTIP);
				};
			},
			(optionText, value) -> value.getText(),
			new SimpleOption.PotentialValuesBasedCallbacks<>(
					Arrays.asList(TextureFilteringMode.values()),
					TextureFilteringMode.CODEC
			),
			TextureFilteringMode.NONE,
			textureFilteringMode -> {
				this.onChangeGraphicsOption();
				refreshWorldRenderer(WorldRenderer::refreshTerrainSampler);
			}
	);
	private boolean useNativeTransport = true;
	private final SimpleOption<AttackIndicator> attackIndicator = new SimpleOption<>(
			"options.attackIndicator",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value.getText(),
			new SimpleOption.PotentialValuesBasedCallbacks<>(
					Arrays.asList(AttackIndicator.values()),
					AttackIndicator.CODEC
			),
			AttackIndicator.CROSSHAIR,
			value -> {}
	);
	public TutorialStep tutorialStep = TutorialStep.MOVEMENT;
	public boolean joinedFirstServer = false;
	private final SimpleOption<Integer> biomeBlendRadius = new SimpleOption<>(
			"options.biomeBlendRadius", SimpleOption.emptyTooltip(), (optionText, value) -> {
		int i = value * 2 + 1;
		return getGenericValueText(optionText, Text.translatable("options.biomeBlendRadius." + i));
	}, new SimpleOption.ValidatingIntSliderCallbacks(0, 7, false), 2, value -> {
		refreshWorldRenderer(WorldRenderer::reload);
		this.onChangeGraphicsOption();
	}
	);
	private final SimpleOption<Double> mouseWheelSensitivity = new SimpleOption<>(
			"options.mouseWheelSensitivity",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> getGenericValueText(
					optionText,
					Text.literal(String.format(Locale.ROOT, "%.2f", value))
			),
			new SimpleOption.ValidatingIntSliderCallbacks(-200, 100)
					.withModifier(
							GameOptions::toMouseWheelSensitivityValue,
							GameOptions::toMouseWheelSensitivitySliderProgressValue,
							false
					),
			Codec.doubleRange(toMouseWheelSensitivityValue(-200), toMouseWheelSensitivityValue(100)),
			toMouseWheelSensitivityValue(0),
			value -> {}
	);
	private final SimpleOption<Boolean> rawMouseInput = SimpleOption.ofBoolean(
			"options.rawMouseInput", true, value -> {
				Window window = MinecraftClient.getInstance().getWindow();
				if (window != null) {
					window.setRawMouseMotion(value);
				}
			}
	);
	private static final Text ALLOW_CURSOR_CHANGES_TOOLTIP = Text.translatable("options.allowCursorChanges.tooltip");
	private final SimpleOption<Boolean> allowCursorChanges = SimpleOption.ofBoolean(
			"options.allowCursorChanges", SimpleOption.constantTooltip(ALLOW_CURSOR_CHANGES_TOOLTIP), true, value -> {
				Window window = MinecraftClient.getInstance().getWindow();
				if (window != null) {
					window.setAllowCursorChanges(value);
				}
			}
	);
	public int glDebugVerbosity = 1;
	private final SimpleOption<Boolean> autoJump = SimpleOption.ofBoolean("options.autoJump", false);
	private static final Text ROTATE_WITH_MINECART_TOOLTIP = Text.translatable("options.rotateWithMinecart.tooltip");
	private final SimpleOption<Boolean> rotateWithMinecart = SimpleOption.ofBoolean(
			"options.rotateWithMinecart", SimpleOption.constantTooltip(ROTATE_WITH_MINECART_TOOLTIP), false
	);
	private final SimpleOption<Boolean> operatorItemsTab = SimpleOption.ofBoolean("options.operatorItemsTab", false);
	private final SimpleOption<Boolean> autoSuggestions = SimpleOption.ofBoolean("options.autoSuggestCommands", true);
	private final SimpleOption<Boolean> chatColors = SimpleOption.ofBoolean("options.chat.color", true);
	private final SimpleOption<Boolean> chatLinks = SimpleOption.ofBoolean("options.chat.links", true);
	private final SimpleOption<Boolean> chatLinksPrompt = SimpleOption.ofBoolean("options.chat.links.prompt", true);
	private final SimpleOption<Boolean> enableVsync = SimpleOption.ofBoolean(
			"options.vsync", true, value -> {
				if (MinecraftClient.getInstance().getWindow() != null) {
					MinecraftClient.getInstance().getWindow().setVsync(value);
				}
			}
	);
	private final SimpleOption<Boolean> entityShadows = SimpleOption.ofBoolean(
			"options.entityShadows", SimpleOption.emptyTooltip(), true, value -> this.onChangeGraphicsOption()
	);
	private final SimpleOption<Boolean>
			forceUnicodeFont =
			SimpleOption.ofBoolean("options.forceUnicodeFont", false, value -> onFontOptionsChanged());
	private final SimpleOption<Boolean> japaneseGlyphVariants = SimpleOption.ofBoolean(
			"options.japaneseGlyphVariants",
			SimpleOption.constantTooltip(Text.translatable("options.japaneseGlyphVariants.tooltip")),
			shouldUseJapaneseGlyphsByDefault(),
			value -> onFontOptionsChanged()
	);
	private final SimpleOption<Boolean> invertMouseX = SimpleOption.ofBoolean("options.invertMouseX", false);
	private final SimpleOption<Boolean> invertMouseY = SimpleOption.ofBoolean("options.invertMouseY", false);
	private final SimpleOption<Boolean>
			discreteMouseScroll =
			SimpleOption.ofBoolean("options.discrete_mouse_scroll", false);
	private static final Text REALMS_NOTIFICATIONS_TOOLTIP = Text.translatable("options.realmsNotifications.tooltip");
	private final SimpleOption<Boolean> realmsNotifications = SimpleOption.ofBoolean(
			"options.realmsNotifications", SimpleOption.constantTooltip(REALMS_NOTIFICATIONS_TOOLTIP), true
	);
	private static final Text ALLOW_SERVER_LISTING_TOOLTIP = Text.translatable("options.allowServerListing.tooltip");
	private final SimpleOption<Boolean> allowServerListing = SimpleOption.ofBoolean(
			"options.allowServerListing", SimpleOption.constantTooltip(ALLOW_SERVER_LISTING_TOOLTIP), true, value -> {}
	);
	private final SimpleOption<Boolean> reducedDebugInfo = SimpleOption.ofBoolean(
			"options.reducedDebugInfo",
			SimpleOption.emptyTooltip(),
			false,
			value -> MinecraftClient.getInstance().debugHudEntryList.updateVisibleEntries()
	);
	private final Map<SoundCategory, SimpleOption<Double>> soundVolumeLevels = Util.mapEnum(
			SoundCategory.class,
			category -> this.createSoundVolumeOption("soundCategory." + category.getName(), category)
	);
	private static final Text SHOW_SUBTITLES_TOOLTIP = Text.translatable("options.showSubtitles.tooltip");
	private final SimpleOption<Boolean> showSubtitles = SimpleOption.ofBoolean(
			"options.showSubtitles", SimpleOption.constantTooltip(SHOW_SUBTITLES_TOOLTIP), false
	);
	private static final Text DIRECTIONAL_AUDIO_ON_TOOLTIP = Text.translatable("options.directionalAudio.on.tooltip");
	private static final Text DIRECTIONAL_AUDIO_OFF_TOOLTIP = Text.translatable("options.directionalAudio.off.tooltip");
	private final SimpleOption<Boolean> directionalAudio = SimpleOption.ofBoolean(
			"options.directionalAudio",
			value -> value ? Tooltip.of(DIRECTIONAL_AUDIO_ON_TOOLTIP) : Tooltip.of(DIRECTIONAL_AUDIO_OFF_TOOLTIP),
			false,
			value -> {
				SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
				soundManager.reloadSounds();
				soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
	);
	private final SimpleOption<Boolean> backgroundForChatOnly = new SimpleOption<>(
			"options.accessibility.text_background",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value
			                       ? Text.translatable("options.accessibility.text_background.chat")
			                       : Text.translatable("options.accessibility.text_background.everywhere"),
			SimpleOption.BOOLEAN,
			true,
			value -> {}
	);
	private final SimpleOption<Boolean> touchscreen = SimpleOption.ofBoolean("options.touchscreen", false);
	private final SimpleOption<Boolean> fullscreen = SimpleOption.ofBoolean(
			"options.fullscreen", false, value -> {
				MinecraftClient minecraftClient = MinecraftClient.getInstance();
				if (minecraftClient.getWindow() != null && minecraftClient.getWindow().isFullscreen() != value) {
					minecraftClient.getWindow().toggleFullscreen();
					this.getFullscreen().setValue(minecraftClient.getWindow().isFullscreen());
				}
			}
	);
	private final SimpleOption<Boolean> bobView = SimpleOption.ofBoolean("options.viewBobbing", true);
	private static final Text TOGGLE_KEY_TEXT = Text.translatable("options.key.toggle");
	private static final Text HOLD_KEY_TEXT = Text.translatable("options.key.hold");
	private final SimpleOption<Boolean> sneakToggled = new SimpleOption<>(
			"key.sneak",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value ? TOGGLE_KEY_TEXT : HOLD_KEY_TEXT,
			SimpleOption.BOOLEAN,
			false,
			value -> {}
	);
	private final SimpleOption<Boolean> sprintToggled = new SimpleOption<>(
			"key.sprint",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value ? TOGGLE_KEY_TEXT : HOLD_KEY_TEXT,
			SimpleOption.BOOLEAN,
			false,
			value -> {}
	);
	private final SimpleOption<Boolean> attackToggled = new SimpleOption<>(
			"key.attack",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value ? TOGGLE_KEY_TEXT : HOLD_KEY_TEXT,
			SimpleOption.BOOLEAN,
			false,
			value -> {}
	);
	private final SimpleOption<Boolean> useToggled = new SimpleOption<>(
			"key.use",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value ? TOGGLE_KEY_TEXT : HOLD_KEY_TEXT,
			SimpleOption.BOOLEAN,
			false,
			value -> {}
	);
	private static final Text SPRINT_WINDOW_TOOLTIP = Text.translatable("options.sprintWindow.tooltip");
	private final SimpleOption<Integer> sprintWindow = new SimpleOption<>(
			"options.sprintWindow",
			SimpleOption.constantTooltip(SPRINT_WINDOW_TOOLTIP),
			(optionText, value) -> value == 0
			                       ? getGenericValueText(optionText, Text.translatable("options.off"))
			                       : getGenericValueText(optionText, Text.translatable("options.value", value)),
			new SimpleOption.ValidatingIntSliderCallbacks(0, 10),
			7,
			value -> {}
	);
	public boolean skipMultiplayerWarning;
	private static final Text HIDE_MATCHED_NAMES_TOOLTIP = Text.translatable("options.hideMatchedNames.tooltip");
	private final SimpleOption<Boolean> hideMatchedNames = SimpleOption.ofBoolean(
			"options.hideMatchedNames", SimpleOption.constantTooltip(HIDE_MATCHED_NAMES_TOOLTIP), true
	);
	private final SimpleOption<Boolean>
			showAutosaveIndicator =
			SimpleOption.ofBoolean("options.autosaveIndicator", true);
	private static final Text ONLY_SHOW_SECURE_CHAT_TOOLTIP = Text.translatable("options.onlyShowSecureChat.tooltip");
	private final SimpleOption<Boolean> onlyShowSecureChat = SimpleOption.ofBoolean(
			"options.onlyShowSecureChat", SimpleOption.constantTooltip(ONLY_SHOW_SECURE_CHAT_TOOLTIP), false
	);
	private static final Text CHAT_DRAFTS_TOOLTIP = Text.translatable("options.chat.drafts.tooltip");
	private final SimpleOption<Boolean>
			chatDrafts =
			SimpleOption.ofBoolean("options.chat.drafts", SimpleOption.constantTooltip(CHAT_DRAFTS_TOOLTIP), false);
	public final KeyBinding forwardKey = new KeyBinding("key.forward", 87, KeyBinding.Category.MOVEMENT);
	public final KeyBinding leftKey = new KeyBinding("key.left", 65, KeyBinding.Category.MOVEMENT);
	public final KeyBinding backKey = new KeyBinding("key.back", 83, KeyBinding.Category.MOVEMENT);
	public final KeyBinding rightKey = new KeyBinding("key.right", 68, KeyBinding.Category.MOVEMENT);
	public final KeyBinding jumpKey = new KeyBinding("key.jump", 32, KeyBinding.Category.MOVEMENT);
	public final KeyBinding
			sneakKey =
			new StickyKeyBinding("key.sneak", 340, KeyBinding.Category.MOVEMENT, this.sneakToggled::getValue, true);
	public final KeyBinding
			sprintKey =
			new StickyKeyBinding("key.sprint", 341, KeyBinding.Category.MOVEMENT, this.sprintToggled::getValue, true);
	public final KeyBinding inventoryKey = new KeyBinding("key.inventory", 69, KeyBinding.Category.INVENTORY);
	public final KeyBinding swapHandsKey = new KeyBinding("key.swapOffhand", 70, KeyBinding.Category.INVENTORY);
	public final KeyBinding dropKey = new KeyBinding("key.drop", 81, KeyBinding.Category.INVENTORY);
	public final KeyBinding
			useKey =
			new StickyKeyBinding(
					"key.use",
					InputUtil.Type.MOUSE,
					1,
					KeyBinding.Category.GAMEPLAY,
					this.useToggled::getValue,
					false
			);
	public final KeyBinding attackKey = new StickyKeyBinding(
			"key.attack", InputUtil.Type.MOUSE, 0, KeyBinding.Category.GAMEPLAY, this.attackToggled::getValue, true
	);
	public final KeyBinding
			pickItemKey =
			new KeyBinding("key.pickItem", InputUtil.Type.MOUSE, 2, KeyBinding.Category.GAMEPLAY);
	public final KeyBinding chatKey = new KeyBinding("key.chat", 84, KeyBinding.Category.MULTIPLAYER);
	public final KeyBinding playerListKey = new KeyBinding("key.playerlist", 258, KeyBinding.Category.MULTIPLAYER);
	public final KeyBinding commandKey = new KeyBinding("key.command", 47, KeyBinding.Category.MULTIPLAYER);
	public final KeyBinding
			socialInteractionsKey =
			new KeyBinding("key.socialInteractions", 80, KeyBinding.Category.MULTIPLAYER);
	public final KeyBinding screenshotKey = new KeyBinding("key.screenshot", 291, KeyBinding.Category.MISC);
	public final KeyBinding
			togglePerspectiveKey =
			new KeyBinding("key.togglePerspective", 294, KeyBinding.Category.MISC);
	public final KeyBinding
			smoothCameraKey =
			new KeyBinding("key.smoothCamera", InputUtil.UNKNOWN_KEY.getCode(), KeyBinding.Category.MISC);
	public final KeyBinding fullscreenKey = new KeyBinding("key.fullscreen", 300, KeyBinding.Category.MISC);
	public final KeyBinding advancementsKey = new KeyBinding("key.advancements", 76, KeyBinding.Category.MISC);
	public final KeyBinding quickActionsKey = new KeyBinding("key.quickActions", 71, KeyBinding.Category.MISC);
	public final KeyBinding toggleGuiKey = new KeyBinding("key.toggleGui", 290, KeyBinding.Category.MISC);
	public final KeyBinding
			toggleSpectatorShaderEffectsKey =
			new KeyBinding("key.toggleSpectatorShaderEffects", 293, KeyBinding.Category.MISC);
	public final KeyBinding[] hotbarKeys = new KeyBinding[]{
			new KeyBinding("key.hotbar.1", 49, KeyBinding.Category.INVENTORY),
			new KeyBinding("key.hotbar.2", 50, KeyBinding.Category.INVENTORY),
			new KeyBinding("key.hotbar.3", 51, KeyBinding.Category.INVENTORY),
			new KeyBinding("key.hotbar.4", 52, KeyBinding.Category.INVENTORY),
			new KeyBinding("key.hotbar.5", 53, KeyBinding.Category.INVENTORY),
			new KeyBinding("key.hotbar.6", 54, KeyBinding.Category.INVENTORY),
			new KeyBinding("key.hotbar.7", 55, KeyBinding.Category.INVENTORY),
			new KeyBinding("key.hotbar.8", 56, KeyBinding.Category.INVENTORY),
			new KeyBinding("key.hotbar.9", 57, KeyBinding.Category.INVENTORY)
	};
	public final KeyBinding
			saveToolbarActivatorKey =
			new KeyBinding("key.saveToolbarActivator", 67, KeyBinding.Category.CREATIVE);
	public final KeyBinding
			loadToolbarActivatorKey =
			new KeyBinding("key.loadToolbarActivator", 88, KeyBinding.Category.CREATIVE);
	public final KeyBinding
			spectatorOutlinesKey =
			new KeyBinding("key.spectatorOutlines", InputUtil.UNKNOWN_KEY.getCode(), KeyBinding.Category.SPECTATOR);
	public final KeyBinding
			spectatorHotbarKey =
			new KeyBinding("key.spectatorHotbar", InputUtil.Type.MOUSE, 2, KeyBinding.Category.SPECTATOR);
	public final KeyBinding
			debugOverlayKey =
			new KeyBinding("key.debug.overlay", InputUtil.Type.KEYSYM, 292, KeyBinding.Category.DEBUG, -2);
	public final KeyBinding
			debugModifierKey =
			new KeyBinding("key.debug.modifier", InputUtil.Type.KEYSYM, 292, KeyBinding.Category.DEBUG, -1);
	public final KeyBinding
			debugCrashKey =
			new KeyBinding("key.debug.crash", InputUtil.Type.KEYSYM, 67, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugReloadChunkKey =
			new KeyBinding("key.debug.reloadChunk", InputUtil.Type.KEYSYM, 65, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugShowHitboxesKey =
			new KeyBinding("key.debug.showHitboxes", InputUtil.Type.KEYSYM, 66, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugClearChatKey =
			new KeyBinding("key.debug.clearChat", InputUtil.Type.KEYSYM, 68, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugShowChunkBordersKey =
			new KeyBinding("key.debug.showChunkBorders", InputUtil.Type.KEYSYM, 71, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugShowAdvancedTooltipsKey =
			new KeyBinding("key.debug.showAdvancedTooltips", InputUtil.Type.KEYSYM, 72, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugCopyRecreateCommandKey =
			new KeyBinding("key.debug.copyRecreateCommand", InputUtil.Type.KEYSYM, 73, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugSpectateKey =
			new KeyBinding("key.debug.spectate", InputUtil.Type.KEYSYM, 78, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugSwitchGameModeKey =
			new KeyBinding("key.debug.switchGameMode", InputUtil.Type.KEYSYM, 293, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugOptionsKey =
			new KeyBinding("key.debug.debugOptions", InputUtil.Type.KEYSYM, 295, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugFocusPauseKey =
			new KeyBinding("key.debug.focusPause", InputUtil.Type.KEYSYM, 80, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugDumpDynamicTexturesKey =
			new KeyBinding("key.debug.dumpDynamicTextures", InputUtil.Type.KEYSYM, 83, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugReloadResourcePacksKey =
			new KeyBinding("key.debug.reloadResourcePacks", InputUtil.Type.KEYSYM, 84, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugProfilingKey =
			new KeyBinding("key.debug.profiling", InputUtil.Type.KEYSYM, 76, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugCopyLocationKey =
			new KeyBinding("key.debug.copyLocation", InputUtil.Type.KEYSYM, 67, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugDumpVersionKey =
			new KeyBinding("key.debug.dumpVersion", InputUtil.Type.KEYSYM, 86, KeyBinding.Category.DEBUG);
	public final KeyBinding
			debugProfilingChartKey =
			new KeyBinding("key.debug.profilingChart", InputUtil.Type.KEYSYM, 49, KeyBinding.Category.DEBUG, 1);
	public final KeyBinding
			debugFpsChartsKey =
			new KeyBinding("key.debug.fpsCharts", InputUtil.Type.KEYSYM, 50, KeyBinding.Category.DEBUG, 2);
	public final KeyBinding
			debugNetworkChartsKey =
			new KeyBinding("key.debug.networkCharts", InputUtil.Type.KEYSYM, 51, KeyBinding.Category.DEBUG, 3);
	public final KeyBinding[] debugKeys = new KeyBinding[]{
			this.debugReloadChunkKey,
			this.debugShowHitboxesKey,
			this.debugClearChatKey,
			this.debugCrashKey,
			this.debugShowChunkBordersKey,
			this.debugShowAdvancedTooltipsKey,
			this.debugCopyRecreateCommandKey,
			this.debugSpectateKey,
			this.debugSwitchGameModeKey,
			this.debugOptionsKey,
			this.debugFocusPauseKey,
			this.debugDumpDynamicTexturesKey,
			this.debugReloadResourcePacksKey,
			this.debugProfilingKey,
			this.debugCopyLocationKey,
			this.debugDumpVersionKey,
			this.debugProfilingChartKey,
			this.debugFpsChartsKey,
			this.debugNetworkChartsKey
	};
	public final KeyBinding[] allKeys = Stream.of(
			                                          new KeyBinding[]{
					                                          this.attackKey,
					                                          this.useKey,
					                                          this.forwardKey,
					                                          this.leftKey,
					                                          this.backKey,
					                                          this.rightKey,
					                                          this.jumpKey,
					                                          this.sneakKey,
					                                          this.sprintKey,
					                                          this.dropKey,
					                                          this.inventoryKey,
					                                          this.chatKey,
					                                          this.playerListKey,
					                                          this.pickItemKey,
					                                          this.commandKey,
					                                          this.socialInteractionsKey,
					                                          this.toggleGuiKey,
					                                          this.toggleSpectatorShaderEffectsKey,
					                                          this.screenshotKey,
					                                          this.togglePerspectiveKey,
					                                          this.smoothCameraKey,
					                                          this.fullscreenKey,
					                                          this.spectatorOutlinesKey,
					                                          this.spectatorHotbarKey,
					                                          this.swapHandsKey,
					                                          this.saveToolbarActivatorKey,
					                                          this.loadToolbarActivatorKey,
					                                          this.advancementsKey,
					                                          this.quickActionsKey,
					                                          this.debugOverlayKey,
					                                          this.debugModifierKey
			                                          },
			                                          this.hotbarKeys,
			                                          this.debugKeys
	                                          )
	                                          .flatMap(Stream::of)
	                                          .toArray(KeyBinding[]::new);
	protected MinecraftClient client;
	private final File optionsFile;
	public boolean hudHidden;
	private Perspective perspective = Perspective.FIRST_PERSON;
	public String lastServer = "";
	public boolean smoothCameraEnabled;
	private final SimpleOption<Integer> fov = new SimpleOption<>(
			"options.fov",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> {
				return switch (value) {
					case 70 -> getGenericValueText(optionText, Text.translatable("options.fov.min"));
					case 110 -> getGenericValueText(optionText, Text.translatable("options.fov.max"));
					default -> getGenericValueText(optionText, value);
				};
			},
			new SimpleOption.ValidatingIntSliderCallbacks(30, 110),
			Codec.DOUBLE.xmap(value -> (int) (value * 40.0 + 70.0), value -> (value.intValue() - 70.0) / 40.0),
			70,
			value -> refreshWorldRenderer(WorldRenderer::scheduleTerrainUpdate)
	);
	private static final Text TELEMETRY_TOOLTIP = Text.translatable(
			"options.telemetry.button.tooltip",
			Text.translatable("options.telemetry.state.minimal"),
			Text.translatable("options.telemetry.state.all")
	);
	private final SimpleOption<Boolean> telemetryOptInExtra = SimpleOption.ofBoolean(
			"options.telemetry.button",
			SimpleOption.constantTooltip(TELEMETRY_TOOLTIP),
			(optionText, value) -> {
				MinecraftClient minecraftClient = MinecraftClient.getInstance();
				if (!minecraftClient.isTelemetryEnabledByApi()) {
					return Text.translatable("options.telemetry.state.none");
				}
				else {
					return value && minecraftClient.isOptionalTelemetryEnabledByApi()
					       ? Text.translatable("options.telemetry.state.all")
					       : Text.translatable("options.telemetry.state.minimal");
				}
			},
			false,
			value -> {}
	);
	private static final Text SCREEN_EFFECT_SCALE_TOOLTIP = Text.translatable("options.screenEffectScale.tooltip");
	private final SimpleOption<Double> distortionEffectScale = new SimpleOption<>(
			"options.screenEffectScale",
			SimpleOption.constantTooltip(SCREEN_EFFECT_SCALE_TOOLTIP),
			GameOptions::getPercentValueOrOffText,
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			1.0,
			value -> {}
	);
	private static final Text FOV_EFFECT_SCALE_TOOLTIP = Text.translatable("options.fovEffectScale.tooltip");
	private final SimpleOption<Double> fovEffectScale = new SimpleOption<>(
			"options.fovEffectScale",
			SimpleOption.constantTooltip(FOV_EFFECT_SCALE_TOOLTIP),
			GameOptions::getPercentValueOrOffText,
			SimpleOption.DoubleSliderCallbacks.INSTANCE.withModifier(MathHelper::square, Math::sqrt),
			Codec.doubleRange(0.0, 1.0),
			1.0,
			value -> {}
	);
	private static final Text DARKNESS_EFFECT_SCALE_TOOLTIP = Text.translatable("options.darknessEffectScale.tooltip");
	private final SimpleOption<Double> darknessEffectScale = new SimpleOption<>(
			"options.darknessEffectScale",
			SimpleOption.constantTooltip(DARKNESS_EFFECT_SCALE_TOOLTIP),
			GameOptions::getPercentValueOrOffText,
			SimpleOption.DoubleSliderCallbacks.INSTANCE.withModifier(MathHelper::square, Math::sqrt),
			1.0,
			value -> {}
	);
	private static final Text GLINT_SPEED_TOOLTIP = Text.translatable("options.glintSpeed.tooltip");
	private final SimpleOption<Double> glintSpeed = new SimpleOption<>(
			"options.glintSpeed",
			SimpleOption.constantTooltip(GLINT_SPEED_TOOLTIP),
			GameOptions::getPercentValueOrOffText,
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			0.5,
			value -> {}
	);
	private static final Text GLINT_STRENGTH_TOOLTIP = Text.translatable("options.glintStrength.tooltip");
	private final SimpleOption<Double> glintStrength = new SimpleOption<>(
			"options.glintStrength",
			SimpleOption.constantTooltip(GLINT_STRENGTH_TOOLTIP),
			GameOptions::getPercentValueOrOffText,
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			0.75,
			value -> {}
	);
	private static final Text DAMAGE_TILT_STRENGTH_TOOLTIP = Text.translatable("options.damageTiltStrength.tooltip");
	private final SimpleOption<Double> damageTiltStrength = new SimpleOption<>(
			"options.damageTiltStrength",
			SimpleOption.constantTooltip(DAMAGE_TILT_STRENGTH_TOOLTIP),
			GameOptions::getPercentValueOrOffText,
			SimpleOption.DoubleSliderCallbacks.INSTANCE,
			1.0,
			double_ -> {}
	);
	private final SimpleOption<Double> gamma = new SimpleOption<>(
			"options.gamma", SimpleOption.emptyTooltip(), (optionText, value) -> {
		int i = (int) (value * 100.0);
		if (i == 0) {
			return getGenericValueText(optionText, Text.translatable("options.gamma.min"));
		}
		else if (i == 50) {
			return getGenericValueText(optionText, Text.translatable("options.gamma.default"));
		}
		else {
			return i == 100 ? getGenericValueText(optionText, Text.translatable("options.gamma.max"))
			                : getGenericValueText(optionText, i);
		}
	}, SimpleOption.DoubleSliderCallbacks.INSTANCE, 0.5, value -> {}
	);
	public static final int GUI_SCALE_AUTO = 0;
	private static final int MAX_SERIALIZABLE_GUI_SCALE = 2147483646;
	private final SimpleOption<Integer> guiScale = new SimpleOption<>(
			"options.guiScale",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value == 0 ? Text.translatable("options.guiScale.auto")
			                                  : Text.literal(Integer.toString(value)),
			new SimpleOption.MaxSuppliableIntCallbacks(
					0, () -> {
				MinecraftClient minecraftClient = MinecraftClient.getInstance();
				return !minecraftClient.isRunning() ? 2147483646 : minecraftClient
				                                                   .getWindow()
				                                                   .calculateScaleFactor(
						                                                   0,
						                                                   minecraftClient.forcesUnicodeFont()
				                                                   );
			}, 2147483646
			),
			0,
			value -> this.client.onResolutionChanged()
	);
	private final SimpleOption<ParticlesMode> particles = new SimpleOption<>(
			"options.particles",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> value.getText(),
			new SimpleOption.PotentialValuesBasedCallbacks<>(
					Arrays.asList(ParticlesMode.values()),
					ParticlesMode.CODEC
			),
			ParticlesMode.ALL,
			value -> this.onChangeGraphicsOption()
	);
	private final SimpleOption<NarratorMode> narrator = new SimpleOption<>(
			"options.narrator",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> (Text) (this.client.getNarratorManager().isActive() ? value.getName()
			                                                                           : Text.translatable(
					                                                                           "options.narrator.notavailable")
			),
			new SimpleOption.PotentialValuesBasedCallbacks<>(Arrays.asList(NarratorMode.values()), NarratorMode.CODEC),
			NarratorMode.OFF,
			value -> this.client.getNarratorManager().onModeChange(value)
	);
	public String language = "en_us";
	private final SimpleOption<String> soundDevice = new SimpleOption<>(
			"options.audioDevice",
			SimpleOption.emptyTooltip(),
			(optionText, value) -> {
				if ("".equals(value)) {
					return Text.translatable("options.audioDevice.default");
				}
				else {
					return value.startsWith("OpenAL Soft on ")
					       ? Text.literal(value.substring(SoundSystem.OPENAL_SOFT_ON_LENGTH)) : Text.literal(value);
				}
			},
			new SimpleOption.LazyCyclingCallbacks<>(
					() -> Stream
							.concat(
									Stream.of(""),
									MinecraftClient.getInstance().getSoundManager().getSoundDevices().stream()
							)
							.toList(),
					value -> MinecraftClient.getInstance().isRunning()
							         && value != ""
							         && !MinecraftClient
							.getInstance()
							.getSoundManager()
							.getSoundDevices()
							.contains(value)
					         ? Optional.empty()
					         : Optional.of(value),
					Codec.STRING
			),
			"",
			value -> {
				SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
				soundManager.reloadSounds();
				soundManager.play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
	);
	public boolean onboardAccessibility = true;
	private static final Text MUSIC_FREQUENCY_TOOLTIP = Text.translatable("options.music_frequency.tooltip");
	private final SimpleOption<MusicTracker.MusicFrequency> musicFrequency = new SimpleOption<>(
			"options.music_frequency",
			SimpleOption.constantTooltip(MUSIC_FREQUENCY_TOOLTIP),
			(optionText, value) -> value.getText(),
			new SimpleOption.PotentialValuesBasedCallbacks<>(
					Arrays.asList(MusicTracker.MusicFrequency.values()),
					MusicTracker.MusicFrequency.CODEC
			),
			MusicTracker.MusicFrequency.DEFAULT,
			value -> MinecraftClient.getInstance().getMusicTracker().setMusicFrequency(value)
	);
	private final SimpleOption<MusicToastMode> musicToast = new SimpleOption<>(
			"options.musicToast",
			value -> Tooltip.of(value.getTooltipText()),
			(optionText, value) -> value.getText(),
			new SimpleOption.PotentialValuesBasedCallbacks<>(
					Arrays.asList(MusicToastMode.values()),
					MusicToastMode.CODEC
			),
			MusicToastMode.NEVER,
			value -> this.client.getToastManager().onMusicToastModeUpdated(value)
	);
	public boolean syncChunkWrites;
	public boolean startedCleanly = true;

	private static void refreshWorldRenderer(Consumer<WorldRenderer> refresher) {
		WorldRenderer worldRenderer = MinecraftClient.getInstance().worldRenderer;
		if (worldRenderer != null) {
			refresher.accept(worldRenderer);
		}
	}

	public SimpleOption<Boolean> getMonochromeLogo() {
		return this.monochromeLogo;
	}

	public SimpleOption<Boolean> getHideLightningFlashes() {
		return this.hideLightningFlashes;
	}

	public SimpleOption<Boolean> getHideSplashTexts() {
		return this.hideSplashTexts;
	}

	public SimpleOption<Double> getMouseSensitivity() {
		return this.mouseSensitivity;
	}

	public SimpleOption<Integer> getViewDistance() {
		return this.viewDistance;
	}

	public SimpleOption<Integer> getSimulationDistance() {
		return this.simulationDistance;
	}

	public SimpleOption<Double> getEntityDistanceScaling() {
		return this.entityDistanceScaling;
	}

	public SimpleOption<Integer> getMaxFps() {
		return this.maxFps;
	}

	public void applyGraphicsMode(GraphicsMode mode) {
		this.applyingGraphicsMode = true;
		mode.apply(this.client);
		this.applyingGraphicsMode = false;
	}

	public SimpleOption<GraphicsMode> getPreset() {
		return this.preset;
	}

	public SimpleOption<InactivityFpsLimit> getInactivityFpsLimit() {
		return this.inactivityFpsLimit;
	}

	public SimpleOption<CloudRenderMode> getCloudRenderMode() {
		return this.cloudRenderMode;
	}

	public SimpleOption<Integer> getCloudRenderDistance() {
		return this.cloudRenderDistance;
	}

	public SimpleOption<Integer> getWeatherRadius() {
		return this.weatherRadius;
	}

	public SimpleOption<Boolean> getCutoutLeaves() {
		return this.cutoutLeaves;
	}

	public SimpleOption<Boolean> getVignette() {
		return this.vignette;
	}

	public SimpleOption<Boolean> getImprovedTransparency() {
		return this.improvedTransparency;
	}

	public SimpleOption<Boolean> getAo() {
		return this.ao;
	}

	public SimpleOption<Double> getChunkFade() {
		return this.chunkFade;
	}

	public SimpleOption<ChunkBuilderMode> getChunkBuilderMode() {
		return this.chunkBuilderMode;
	}

	public void refreshResourcePacks(ResourcePackManager resourcePackManager) {
		List<String> list = ImmutableList.copyOf(this.resourcePacks);
		this.resourcePacks.clear();
		this.incompatibleResourcePacks.clear();

		for (ResourcePackProfile resourcePackProfile : resourcePackManager.getEnabledProfiles()) {
			if (!resourcePackProfile.isPinned()) {
				this.resourcePacks.add(resourcePackProfile.getId());
				if (!resourcePackProfile.getCompatibility().isCompatible()) {
					this.incompatibleResourcePacks.add(resourcePackProfile.getId());
				}
			}
		}

		this.write();
		List<String> list2 = ImmutableList.copyOf(this.resourcePacks);
		if (!list2.equals(list)) {
			this.client.reloadResources();
		}
	}

	public SimpleOption<ChatVisibility> getChatVisibility() {
		return this.chatVisibility;
	}

	public SimpleOption<Double> getChatOpacity() {
		return this.chatOpacity;
	}

	public SimpleOption<Double> getChatLineSpacing() {
		return this.chatLineSpacing;
	}

	public SimpleOption<Integer> getMenuBackgroundBlurriness() {
		return this.menuBackgroundBlurriness;
	}

	public int getMenuBackgroundBlurrinessValue() {
		return this.getMenuBackgroundBlurriness().getValue();
	}

	public SimpleOption<Double> getTextBackgroundOpacity() {
		return this.textBackgroundOpacity;
	}

	public SimpleOption<Double> getPanoramaSpeed() {
		return this.panoramaSpeed;
	}

	public SimpleOption<Boolean> getHighContrast() {
		return this.highContrast;
	}

	public SimpleOption<Boolean> getHighContrastBlockOutline() {
		return this.highContrastBlockOutline;
	}

	public SimpleOption<Boolean> getNarratorHotkey() {
		return this.narratorHotkey;
	}

	public SimpleOption<Arm> getMainArm() {
		return this.mainArm;
	}

	public SimpleOption<Double> getChatScale() {
		return this.chatScale;
	}

	public SimpleOption<Double> getChatWidth() {
		return this.chatWidth;
	}

	public SimpleOption<Double> getChatHeightUnfocused() {
		return this.chatHeightUnfocused;
	}

	public SimpleOption<Double> getChatHeightFocused() {
		return this.chatHeightFocused;
	}

	public SimpleOption<Double> getChatDelay() {
		return this.chatDelay;
	}

	public SimpleOption<Double> getNotificationDisplayTime() {
		return this.notificationDisplayTime;
	}

	public SimpleOption<Integer> getMipmapLevels() {
		return this.mipmapLevels;
	}

	public SimpleOption<Integer> getMaxAnisotropy() {
		return this.maxAnisotropy;
	}

	public int getEffectiveAnisotropy() {
		return Math.min(1 << this.maxAnisotropy.getValue(), RenderSystem.getDevice().getMaxSupportedAnisotropy());
	}

	public SimpleOption<TextureFilteringMode> getTextureFiltering() {
		return this.textureFiltering;
	}

	public SimpleOption<AttackIndicator> getAttackIndicator() {
		return this.attackIndicator;
	}

	public SimpleOption<Integer> getBiomeBlendRadius() {
		return this.biomeBlendRadius;
	}

	private static double toMouseWheelSensitivityValue(int value) {
		return Math.pow(10.0, value / 100.0);
	}

	private static int toMouseWheelSensitivitySliderProgressValue(double value) {
		return MathHelper.floor(Math.log10(value) * 100.0);
	}

	public SimpleOption<Double> getMouseWheelSensitivity() {
		return this.mouseWheelSensitivity;
	}

	public SimpleOption<Boolean> getRawMouseInput() {
		return this.rawMouseInput;
	}

	public SimpleOption<Boolean> getAllowCursorChanges() {
		return this.allowCursorChanges;
	}

	public SimpleOption<Boolean> getAutoJump() {
		return this.autoJump;
	}

	public SimpleOption<Boolean> getRotateWithMinecart() {
		return this.rotateWithMinecart;
	}

	public SimpleOption<Boolean> getOperatorItemsTab() {
		return this.operatorItemsTab;
	}

	public SimpleOption<Boolean> getAutoSuggestions() {
		return this.autoSuggestions;
	}

	public SimpleOption<Boolean> getChatColors() {
		return this.chatColors;
	}

	public SimpleOption<Boolean> getChatLinks() {
		return this.chatLinks;
	}

	public SimpleOption<Boolean> getChatLinksPrompt() {
		return this.chatLinksPrompt;
	}

	public SimpleOption<Boolean> getEnableVsync() {
		return this.enableVsync;
	}

	public SimpleOption<Boolean> getEntityShadows() {
		return this.entityShadows;
	}

	private static void onFontOptionsChanged() {
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		if (minecraftClient.getWindow() != null) {
			minecraftClient.onFontOptionsChanged();
			minecraftClient.onResolutionChanged();
		}
	}

	public SimpleOption<Boolean> getForceUnicodeFont() {
		return this.forceUnicodeFont;
	}

	private static boolean shouldUseJapaneseGlyphsByDefault() {
		return Locale.getDefault().getLanguage().equalsIgnoreCase("ja");
	}

	public SimpleOption<Boolean> getJapaneseGlyphVariants() {
		return this.japaneseGlyphVariants;
	}

	public SimpleOption<Boolean> getInvertMouseX() {
		return this.invertMouseX;
	}

	public SimpleOption<Boolean> getInvertMouseY() {
		return this.invertMouseY;
	}

	public SimpleOption<Boolean> getDiscreteMouseScroll() {
		return this.discreteMouseScroll;
	}

	public SimpleOption<Boolean> getRealmsNotifications() {
		return this.realmsNotifications;
	}

	public SimpleOption<Boolean> getAllowServerListing() {
		return this.allowServerListing;
	}

	public SimpleOption<Boolean> getReducedDebugInfo() {
		return this.reducedDebugInfo;
	}

	public final float getSoundVolume(SoundCategory category) {
		return category == SoundCategory.MASTER
		       ? this.getCategorySoundVolume(category)
		       : this.getCategorySoundVolume(category) * this.getCategorySoundVolume(SoundCategory.MASTER);
	}

	public final float getCategorySoundVolume(SoundCategory category) {
		return this.getSoundVolumeOption(category).getValue().floatValue();
	}

	public final SimpleOption<Double> getSoundVolumeOption(SoundCategory category) {
		return Objects.requireNonNull(this.soundVolumeLevels.get(category));
	}

	private SimpleOption<Double> createSoundVolumeOption(String key, SoundCategory category) {
		return new SimpleOption<>(
				key,
				SimpleOption.emptyTooltip(),
				GameOptions::getPercentValueOrOffText,
				SimpleOption.DoubleSliderCallbacks.INSTANCE,
				1.0,
				volume -> {
					MinecraftClient minecraftClient = MinecraftClient.getInstance();
					SoundManager soundManager = minecraftClient.getSoundManager();
					if ((category == SoundCategory.MASTER || category == SoundCategory.MUSIC)
							&& this.getSoundVolume(SoundCategory.MUSIC) > 0.0F) {
						minecraftClient.getMusicTracker().tryShowToast();
					}

					soundManager.refreshSoundVolumes(category);
					if (minecraftClient.world == null) {
						SoundPreviewer.preview(soundManager, category, volume.floatValue());
					}
				}
		);
	}

	public SimpleOption<Boolean> getShowSubtitles() {
		return this.showSubtitles;
	}

	public SimpleOption<Boolean> getDirectionalAudio() {
		return this.directionalAudio;
	}

	public SimpleOption<Boolean> getBackgroundForChatOnly() {
		return this.backgroundForChatOnly;
	}

	public SimpleOption<Boolean> getTouchscreen() {
		return this.touchscreen;
	}

	public SimpleOption<Boolean> getFullscreen() {
		return this.fullscreen;
	}

	public SimpleOption<Boolean> getBobView() {
		return this.bobView;
	}

	public SimpleOption<Boolean> getSneakToggled() {
		return this.sneakToggled;
	}

	public SimpleOption<Boolean> getSprintToggled() {
		return this.sprintToggled;
	}

	public SimpleOption<Boolean> getAttackToggled() {
		return this.attackToggled;
	}

	public SimpleOption<Boolean> getUseToggled() {
		return this.useToggled;
	}

	public SimpleOption<Integer> getSprintWindow() {
		return this.sprintWindow;
	}

	public SimpleOption<Boolean> getHideMatchedNames() {
		return this.hideMatchedNames;
	}

	public SimpleOption<Boolean> getShowAutosaveIndicator() {
		return this.showAutosaveIndicator;
	}

	public SimpleOption<Boolean> getOnlyShowSecureChat() {
		return this.onlyShowSecureChat;
	}

	public SimpleOption<Boolean> getChatDrafts() {
		return this.chatDrafts;
	}

	private void onChangeGraphicsOption() {
		if (!this.applyingGraphicsMode) {
			this.preset.setValue(GraphicsMode.CUSTOM);
			if (this.client.currentScreen instanceof GameOptionsScreen gameOptionsScreen) {
				gameOptionsScreen.update(this.preset);
			}
		}
	}

	public SimpleOption<Integer> getFov() {
		return this.fov;
	}

	public SimpleOption<Boolean> getTelemetryOptInExtra() {
		return this.telemetryOptInExtra;
	}

	public SimpleOption<Double> getDistortionEffectScale() {
		return this.distortionEffectScale;
	}

	public SimpleOption<Double> getFovEffectScale() {
		return this.fovEffectScale;
	}

	public SimpleOption<Double> getDarknessEffectScale() {
		return this.darknessEffectScale;
	}

	public SimpleOption<Double> getGlintSpeed() {
		return this.glintSpeed;
	}

	public SimpleOption<Double> getGlintStrength() {
		return this.glintStrength;
	}

	public SimpleOption<Double> getDamageTiltStrength() {
		return this.damageTiltStrength;
	}

	public SimpleOption<Double> getGamma() {
		return this.gamma;
	}

	public SimpleOption<Integer> getGuiScale() {
		return this.guiScale;
	}

	public SimpleOption<ParticlesMode> getParticles() {
		return this.particles;
	}

	public SimpleOption<NarratorMode> getNarrator() {
		return this.narrator;
	}

	public SimpleOption<String> getSoundDevice() {
		return this.soundDevice;
	}

	public void setAccessibilityOnboarded() {
		this.onboardAccessibility = false;
		this.write();
	}

	public SimpleOption<MusicTracker.MusicFrequency> getMusicFrequency() {
		return this.musicFrequency;
	}

	public SimpleOption<MusicToastMode> getMusicToast() {
		return this.musicToast;
	}

	public GameOptions(MinecraftClient client, File optionsFile) {
		this.client = client;
		this.optionsFile = new File(optionsFile, "options.txt");
		boolean bl = Runtime.getRuntime().maxMemory() >= 1000000000L;
		this.viewDistance = new SimpleOption<>(
				"options.renderDistance",
				SimpleOption.emptyTooltip(),
				(optionText, value) -> getGenericValueText(optionText, Text.translatable("options.chunks", value)),
				new SimpleOption.ValidatingIntSliderCallbacks(2, bl ? 32 : 16, false),
				12,
				value -> {
					refreshWorldRenderer(WorldRenderer::scheduleTerrainUpdate);
					this.onChangeGraphicsOption();
				}
		);
		this.simulationDistance = new SimpleOption<>(
				"options.simulationDistance",
				SimpleOption.emptyTooltip(),
				(optionText, value) -> getGenericValueText(optionText, Text.translatable("options.chunks", value)),
				new SimpleOption.ValidatingIntSliderCallbacks(
						SharedConstants.ALLOW_LOW_SIM_DISTANCE ? 2 : 5,
						bl ? 32 : 16,
						false
				),
				12,
				value -> this.onChangeGraphicsOption()
		);
		this.syncChunkWrites = Util.getOperatingSystem() == Util.OperatingSystem.WINDOWS;
		this.load();
	}

	public float getTextBackgroundOpacity(float fallback) {
		return this.backgroundForChatOnly.getValue() ? fallback
		                                             : this.getTextBackgroundOpacity().getValue().floatValue();
	}

	public int getTextBackgroundColor(float fallbackOpacity) {
		return ColorHelper.fromFloats(this.getTextBackgroundOpacity(fallbackOpacity), 0.0F, 0.0F, 0.0F);
	}

	public int getTextBackgroundColor(int fallbackColor) {
		return this.backgroundForChatOnly.getValue()
		       ? fallbackColor
		       : ColorHelper.fromFloats(this.textBackgroundOpacity.getValue().floatValue(), 0.0F, 0.0F, 0.0F);
	}

	private void acceptProfiledOptions(GameOptions.OptionVisitor visitor) {
		visitor.accept("ao", this.ao);
		visitor.accept("biomeBlendRadius", this.biomeBlendRadius);
		visitor.accept("chunkSectionFadeInTime", this.chunkFade);
		visitor.accept("cutoutLeaves", this.cutoutLeaves);
		visitor.accept("enableVsync", this.enableVsync);
		visitor.accept("entityDistanceScaling", this.entityDistanceScaling);
		visitor.accept("entityShadows", this.entityShadows);
		visitor.accept("forceUnicodeFont", this.forceUnicodeFont);
		visitor.accept("japaneseGlyphVariants", this.japaneseGlyphVariants);
		visitor.accept("fov", this.fov);
		visitor.accept("fovEffectScale", this.fovEffectScale);
		visitor.accept("darknessEffectScale", this.darknessEffectScale);
		visitor.accept("glintSpeed", this.glintSpeed);
		visitor.accept("glintStrength", this.glintStrength);
		visitor.accept("graphicsPreset", this.preset);
		visitor.accept("prioritizeChunkUpdates", this.chunkBuilderMode);
		visitor.accept("fullscreen", this.fullscreen);
		visitor.accept("gamma", this.gamma);
		visitor.accept("guiScale", this.guiScale);
		visitor.accept("maxAnisotropyBit", this.maxAnisotropy);
		visitor.accept("textureFiltering", this.textureFiltering);
		visitor.accept("maxFps", this.maxFps);
		visitor.accept("improvedTransparency", this.improvedTransparency);
		visitor.accept("inactivityFpsLimit", this.inactivityFpsLimit);
		visitor.accept("mipmapLevels", this.mipmapLevels);
		visitor.accept("narrator", this.narrator);
		visitor.accept("particles", this.particles);
		visitor.accept("reducedDebugInfo", this.reducedDebugInfo);
		visitor.accept("renderClouds", this.cloudRenderMode);
		visitor.accept("cloudRange", this.cloudRenderDistance);
		visitor.accept("renderDistance", this.viewDistance);
		visitor.accept("simulationDistance", this.simulationDistance);
		visitor.accept("screenEffectScale", this.distortionEffectScale);
		visitor.accept("soundDevice", this.soundDevice);
		visitor.accept("vignette", this.vignette);
		visitor.accept("weatherRadius", this.weatherRadius);
	}

	private void accept(GameOptions.Visitor visitor) {
		this.acceptProfiledOptions(visitor);
		visitor.accept("autoJump", this.autoJump);
		visitor.accept("rotateWithMinecart", this.rotateWithMinecart);
		visitor.accept("operatorItemsTab", this.operatorItemsTab);
		visitor.accept("autoSuggestions", this.autoSuggestions);
		visitor.accept("chatColors", this.chatColors);
		visitor.accept("chatLinks", this.chatLinks);
		visitor.accept("chatLinksPrompt", this.chatLinksPrompt);
		visitor.accept("discrete_mouse_scroll", this.discreteMouseScroll);
		visitor.accept("invertXMouse", this.invertMouseX);
		visitor.accept("invertYMouse", this.invertMouseY);
		visitor.accept("realmsNotifications", this.realmsNotifications);
		visitor.accept("showSubtitles", this.showSubtitles);
		visitor.accept("directionalAudio", this.directionalAudio);
		visitor.accept("touchscreen", this.touchscreen);
		visitor.accept("bobView", this.bobView);
		visitor.accept("toggleCrouch", this.sneakToggled);
		visitor.accept("toggleSprint", this.sprintToggled);
		visitor.accept("toggleAttack", this.attackToggled);
		visitor.accept("toggleUse", this.useToggled);
		visitor.accept("sprintWindow", this.sprintWindow);
		visitor.accept("darkMojangStudiosBackground", this.monochromeLogo);
		visitor.accept("hideLightningFlashes", this.hideLightningFlashes);
		visitor.accept("hideSplashTexts", this.hideSplashTexts);
		visitor.accept("mouseSensitivity", this.mouseSensitivity);
		visitor.accept("damageTiltStrength", this.damageTiltStrength);
		visitor.accept("highContrast", this.highContrast);
		visitor.accept("highContrastBlockOutline", this.highContrastBlockOutline);
		visitor.accept("narratorHotkey", this.narratorHotkey);
		this.resourcePacks =
				visitor.visitObject("resourcePacks", this.resourcePacks, GameOptions::parseList, GSON::toJson);
		this.incompatibleResourcePacks =
				visitor.visitObject(
						"incompatibleResourcePacks",
						this.incompatibleResourcePacks,
						GameOptions::parseList,
						GSON::toJson
				);
		this.lastServer = visitor.visitString("lastServer", this.lastServer);
		this.language = visitor.visitString("lang", this.language);
		visitor.accept("chatVisibility", this.chatVisibility);
		visitor.accept("chatOpacity", this.chatOpacity);
		visitor.accept("chatLineSpacing", this.chatLineSpacing);
		visitor.accept("textBackgroundOpacity", this.textBackgroundOpacity);
		visitor.accept("backgroundForChatOnly", this.backgroundForChatOnly);
		this.hideServerAddress = visitor.visitBoolean("hideServerAddress", this.hideServerAddress);
		this.advancedItemTooltips = visitor.visitBoolean("advancedItemTooltips", this.advancedItemTooltips);
		this.pauseOnLostFocus = visitor.visitBoolean("pauseOnLostFocus", this.pauseOnLostFocus);
		this.overrideWidth = visitor.visitInt("overrideWidth", this.overrideWidth);
		this.overrideHeight = visitor.visitInt("overrideHeight", this.overrideHeight);
		visitor.accept("chatHeightFocused", this.chatHeightFocused);
		visitor.accept("chatDelay", this.chatDelay);
		visitor.accept("chatHeightUnfocused", this.chatHeightUnfocused);
		visitor.accept("chatScale", this.chatScale);
		visitor.accept("chatWidth", this.chatWidth);
		visitor.accept("notificationDisplayTime", this.notificationDisplayTime);
		this.useNativeTransport = visitor.visitBoolean("useNativeTransport", this.useNativeTransport);
		visitor.accept("mainHand", this.mainArm);
		visitor.accept("attackIndicator", this.attackIndicator);
		this.tutorialStep =
				visitor.visitObject("tutorialStep", this.tutorialStep, TutorialStep::byName, TutorialStep::getName);
		visitor.accept("mouseWheelSensitivity", this.mouseWheelSensitivity);
		visitor.accept("rawMouseInput", this.rawMouseInput);
		visitor.accept("allowCursorChanges", this.allowCursorChanges);
		this.glDebugVerbosity = visitor.visitInt("glDebugVerbosity", this.glDebugVerbosity);
		this.skipMultiplayerWarning = visitor.visitBoolean("skipMultiplayerWarning", this.skipMultiplayerWarning);
		visitor.accept("hideMatchedNames", this.hideMatchedNames);
		this.joinedFirstServer = visitor.visitBoolean("joinedFirstServer", this.joinedFirstServer);
		this.syncChunkWrites = visitor.visitBoolean("syncChunkWrites", this.syncChunkWrites);
		visitor.accept("showAutosaveIndicator", this.showAutosaveIndicator);
		visitor.accept("allowServerListing", this.allowServerListing);
		visitor.accept("onlyShowSecureChat", this.onlyShowSecureChat);
		visitor.accept("saveChatDrafts", this.chatDrafts);
		visitor.accept("panoramaScrollSpeed", this.panoramaSpeed);
		visitor.accept("telemetryOptInExtra", this.telemetryOptInExtra);
		this.onboardAccessibility = visitor.visitBoolean("onboardAccessibility", this.onboardAccessibility);
		visitor.accept("menuBackgroundBlurriness", this.menuBackgroundBlurriness);
		this.startedCleanly = visitor.visitBoolean("startedCleanly", this.startedCleanly);
		visitor.accept("musicToast", this.musicToast);
		visitor.accept("musicFrequency", this.musicFrequency);

		for (KeyBinding keyBinding : this.allKeys) {
			String string = keyBinding.getBoundKeyTranslationKey();
			String string2 = visitor.visitString("key_" + keyBinding.getId(), string);
			if (!string.equals(string2)) {
				keyBinding.setBoundKey(InputUtil.fromTranslationKey(string2));
			}
		}

		for (SoundCategory soundCategory : SoundCategory.values()) {
			visitor.accept("soundCategory_" + soundCategory.getName(), this.soundVolumeLevels.get(soundCategory));
		}

		for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
			boolean bl = this.enabledPlayerModelParts.contains(playerModelPart);
			boolean bl2 = visitor.visitBoolean("modelPart_" + playerModelPart.getName(), bl);
			if (bl2 != bl) {
				this.setPlayerModelPart(playerModelPart, bl2);
			}
		}
	}

	public void load() {
		try {
			if (!this.optionsFile.exists()) {
				return;
			}

			NbtCompound nbtCompound = new NbtCompound();

			try (BufferedReader bufferedReader = Files.newReader(this.optionsFile, StandardCharsets.UTF_8)) {
				bufferedReader.lines().forEach(line -> {
					try {
						Iterator<String> iterator = COLON_SPLITTER.split(line).iterator();
						nbtCompound.putString(iterator.next(), iterator.next());
					}
					catch (Exception var3) {
						LOGGER.warn("Skipping bad option: {}", line);
					}
				});
			}

			final NbtCompound nbtCompound2 = this.update(nbtCompound);
			this.accept(
					new GameOptions.Visitor() {
						private @Nullable String find(String key) {
							NbtElement nbtElement = nbtCompound2.get(key);
							if (nbtElement == null) {
								return null;
							}
							else if (nbtElement instanceof NbtString(String var7x)) {
								return var7x;
							}
							else {
								throw new IllegalStateException(
										"Cannot read field of wrong type, expected string: " + nbtElement);
							}
						}

						@Override
						public <T> void accept(String key, SimpleOption<T> option) {
							String string = this.find(key);
							if (string != null) {
								JsonElement jsonElement = LenientJsonParser.parse(string.isEmpty() ? "\"\"" : string);
								option.getCodec()
								      .parse(JsonOps.INSTANCE, jsonElement)
								      .ifError(
										      error -> GameOptions.LOGGER.error(
												      "Error parsing option value {} for option {}: {}",
												      new Object[]{string, option, error.message()}
										      )
								      )
								      .ifSuccess(option::setValue);
							}
						}

						@Override
						public int visitInt(String key, int current) {
							String string = this.find(key);
							if (string != null) {
								try {
									return Integer.parseInt(string);
								}
								catch (NumberFormatException var5) {
									GameOptions.LOGGER.warn(
											"Invalid integer value for option {} = {}",
											new Object[]{key, string, var5}
									);
								}
							}

							return current;
						}

						@Override
						public boolean visitBoolean(String key, boolean current) {
							String string = this.find(key);
							return string != null ? GameOptions.isTrue(string) : current;
						}

						@Override
						public String visitString(String key, String current) {
							return (String) MoreObjects.firstNonNull(this.find(key), current);
						}

						@Override
						public float visitFloat(String key, float current) {
							String string = this.find(key);
							if (string == null) {
								return current;
							}
							else if (GameOptions.isTrue(string)) {
								return 1.0F;
							}
							else if (GameOptions.isFalse(string)) {
								return 0.0F;
							}
							else {
								try {
									return Float.parseFloat(string);
								}
								catch (NumberFormatException var5) {
									GameOptions.LOGGER.warn(
											"Invalid floating point value for option {} = {}",
											new Object[]{key, string, var5}
									);
									return current;
								}
							}
						}

						@Override
						public <T> T visitObject(
								String key,
								T current,
								Function<String, T> decoder,
								Function<T, String> encoder
						) {
							String string = this.find(key);
							return string == null ? current : decoder.apply(string);
						}
					}
			);
			nbtCompound2.getString("fullscreenResolution").ifPresent(string -> this.fullscreenResolution = string);
			KeyBinding.updateKeysByCode();
		}
		catch (Exception var7) {
			LOGGER.error("Failed to load options", var7);
		}
	}

	static boolean isTrue(String value) {
		return "true".equals(value);
	}

	static boolean isFalse(String value) {
		return "false".equals(value);
	}

	private NbtCompound update(NbtCompound nbt) {
		int i = 0;

		try {
			i = nbt.getString("version").map(Integer::parseInt).orElse(0);
		}
		catch (RuntimeException var4) {
		}

		return DataFixTypes.OPTIONS.update(this.client.getDataFixer(), nbt, i);
	}

	public void write() {
		try (final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(this.optionsFile),
				StandardCharsets.UTF_8
		))
		) {
			printWriter.println("version:" + SharedConstants.getGameVersion().dataVersion().id());
			this.accept(
					new GameOptions.Visitor() {
						public void print(String key) {
							printWriter.print(key);
							printWriter.print(':');
						}

						@Override
						public <T> void accept(String key, SimpleOption<T> option) {
							option.getCodec()
							      .encodeStart(JsonOps.INSTANCE, option.getValue())
							      .ifError(error -> GameOptions.LOGGER.error(
									      "Error saving option {}: {}",
									      option,
									      error.message()
							      ))
							      .ifSuccess(json -> {
								      this.print(key);
								      printWriter.println(GameOptions.GSON.toJson(json));
							      });
						}

						@Override
						public int visitInt(String key, int current) {
							this.print(key);
							printWriter.println(current);
							return current;
						}

						@Override
						public boolean visitBoolean(String key, boolean current) {
							this.print(key);
							printWriter.println(current);
							return current;
						}

						@Override
						public String visitString(String key, String current) {
							this.print(key);
							printWriter.println(current);
							return current;
						}

						@Override
						public float visitFloat(String key, float current) {
							this.print(key);
							printWriter.println(current);
							return current;
						}

						@Override
						public <T> T visitObject(
								String key,
								T current,
								Function<String, T> decoder,
								Function<T, String> encoder
						) {
							this.print(key);
							printWriter.println(encoder.apply(current));
							return current;
						}
					}
			);
			String string = this.getFullscreenResolution();
			if (string != null) {
				printWriter.println("fullscreenResolution:" + string);
			}
		}
		catch (Exception var6) {
			LOGGER.error("Failed to save options", var6);
		}

		this.sendClientSettings();
	}

	private @Nullable String getFullscreenResolution() {
		Window window = this.client.getWindow();
		if (window == null) {
			return this.fullscreenResolution;
		}
		else {
			return window.getFullscreenVideoMode().isPresent() ? window.getFullscreenVideoMode().get().asString()
			                                                   : null;
		}
	}

	public SyncedClientOptions getSyncedOptions() {
		int i = 0;

		for (PlayerModelPart playerModelPart : this.enabledPlayerModelParts) {
			i |= playerModelPart.getBitFlag();
		}

		return new SyncedClientOptions(
				this.language,
				this.viewDistance.getValue(),
				this.chatVisibility.getValue(),
				this.chatColors.getValue(),
				i,
				this.mainArm.getValue(),
				this.client.shouldFilterText(),
				this.allowServerListing.getValue(),
				this.particles.getValue()
		);
	}

	public void sendClientSettings() {
		if (this.client.player != null) {
			this.client.player.networkHandler.syncOptions(this.getSyncedOptions());
		}
	}

	public void setPlayerModelPart(PlayerModelPart part, boolean enabled) {
		if (enabled) {
			this.enabledPlayerModelParts.add(part);
		}
		else {
			this.enabledPlayerModelParts.remove(part);
		}
	}

	public boolean isPlayerModelPartEnabled(PlayerModelPart part) {
		return this.enabledPlayerModelParts.contains(part);
	}

	public CloudRenderMode getCloudRenderModeValue() {
		return this.cloudRenderMode.getValue();
	}

	public boolean shouldUseNativeTransport() {
		return this.useNativeTransport;
	}

	public void addResourcePackProfilesToManager(ResourcePackManager manager) {
		Set<String> set = Sets.newLinkedHashSet();
		Iterator<String> iterator = this.resourcePacks.iterator();

		while (iterator.hasNext()) {
			String string = iterator.next();
			ResourcePackProfile resourcePackProfile = manager.getProfile(string);
			if (resourcePackProfile == null && !string.startsWith("file/")) {
				resourcePackProfile = manager.getProfile("file/" + string);
			}

			if (resourcePackProfile == null) {
				LOGGER.warn("Removed resource pack {} from options because it doesn't seem to exist anymore", string);
				iterator.remove();
			}
			else if (!resourcePackProfile.getCompatibility().isCompatible() && !this.incompatibleResourcePacks.contains(
					string)) {
				LOGGER.warn("Removed resource pack {} from options because it is no longer compatible", string);
				iterator.remove();
			}
			else if (resourcePackProfile.getCompatibility().isCompatible() && this.incompatibleResourcePacks.contains(
					string)) {
				LOGGER.info("Removed resource pack {} from incompatibility list because it's now compatible", string);
				this.incompatibleResourcePacks.remove(string);
			}
			else {
				set.add(resourcePackProfile.getId());
			}
		}

		manager.setEnabledProfiles(set);
	}

	public Perspective getPerspective() {
		return this.perspective;
	}

	public void setPerspective(Perspective perspective) {
		this.perspective = perspective;
	}

	private static List<String> parseList(String content) {
		List<String> list = JsonHelper.deserialize(GSON, content, STRING_LIST_TYPE);
		return (List<String>) (list != null ? list : Lists.newArrayList());
	}

	public File getOptionsFile() {
		return this.optionsFile;
	}

	public String collectProfiledOptions() {
		final List<Pair<String, Object>> list = new ArrayList<>();
		this.acceptProfiledOptions(new GameOptions.OptionVisitor() {
			@Override
			public <T> void accept(String key, SimpleOption<T> option) {
				list.add(Pair.of(key, option.getValue()));
			}
		});
		list.add(Pair.of("fullscreenResolution", String.valueOf(this.fullscreenResolution)));
		list.add(Pair.of("glDebugVerbosity", this.glDebugVerbosity));
		list.add(Pair.of("overrideHeight", this.overrideHeight));
		list.add(Pair.of("overrideWidth", this.overrideWidth));
		list.add(Pair.of("syncChunkWrites", this.syncChunkWrites));
		list.add(Pair.of("useNativeTransport", this.useNativeTransport));
		list.add(Pair.of("resourcePacks", this.resourcePacks));
		return list.stream()
		           .sorted(Comparator.comparing(Pair::getFirst))
		           .map(option -> (String) option.getFirst() + ": " + option.getSecond())
		           .collect(Collectors.joining(System.lineSeparator()));
	}

	public void setServerViewDistance(int serverViewDistance) {
		this.serverViewDistance = serverViewDistance;
	}

	public int getClampedViewDistance() {
		return this.serverViewDistance > 0 ? Math.min(this.viewDistance.getValue(), this.serverViewDistance)
		                                   : this.viewDistance.getValue();
	}

	private static Text getPixelValueText(Text prefix, int value) {
		return Text.translatable("options.pixel_value", prefix, value);
	}

	private static Text getPercentValueText(Text prefix, double value) {
		return Text.translatable("options.percent_value", prefix, (int) (value * 100.0));
	}

	public static Text getGenericValueText(Text prefix, Text value) {
		return Text.translatable("options.generic_value", prefix, value);
	}

	public static Text getGenericValueText(Text prefix, int value) {
		return getGenericValueText(prefix, Text.literal(Integer.toString(value)));
	}

	public static Text getGenericValueOrOffText(Text prefix, int value) {
		return value == 0 ? getGenericValueText(prefix, ScreenTexts.OFF) : getGenericValueText(prefix, value);
	}

	private static Text getPercentValueOrOffText(Text prefix, double value) {
		return value == 0.0 ? getGenericValueText(prefix, ScreenTexts.OFF) : getPercentValueText(prefix, value);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code OptionVisitor}.
	 */
	interface OptionVisitor {

		<T> void accept(String key, SimpleOption<T> option);
	}

	@Environment(EnvType.CLIENT)
	/**
	 * {@code Visitor}.
	 */
	interface Visitor extends GameOptions.OptionVisitor {

		int visitInt(String key, int current);

		boolean visitBoolean(String key, boolean current);

		String visitString(String key, String current);

		float visitFloat(String key, float current);

		<T> T visitObject(String key, T current, Function<String, T> decoder, Function<T, String> encoder);
	}
}
