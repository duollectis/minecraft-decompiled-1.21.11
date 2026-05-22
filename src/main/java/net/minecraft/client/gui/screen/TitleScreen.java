package net.minecraft.client.gui.screen;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screen.option.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screen.option.LanguageOptionsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.realms.gui.screen.RealmsNotificationsScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.storage.LevelStorage;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Objects;

/**
 * Главный экран меню Minecraft. Отображает логотип, кнопки навигации,
 * сплэш-текст и уведомления Realms.
 */
@Environment(EnvType.CLIENT)
public class TitleScreen extends Screen {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Text NARRATOR_SCREEN_TITLE = Text.translatable("narrator.screen.title");
	private static final Text COPYRIGHT = Text.translatable("title.credits");
	private static final String DEMO_WORLD_NAME = "Demo_World";
	private @Nullable SplashTextRenderer splashText;
	private @Nullable RealmsNotificationsScreen realmsNotificationGui;
	private boolean doBackgroundFade;
	private long backgroundFadeStart;
	private final LogoDrawer logoDrawer;

	public TitleScreen() {
		this(false);
	}

	public TitleScreen(boolean doBackgroundFade) {
		this(doBackgroundFade, null);
	}

	public TitleScreen(boolean doBackgroundFade, @Nullable LogoDrawer logoDrawer) {
		super(NARRATOR_SCREEN_TITLE);
		this.doBackgroundFade = doBackgroundFade;
		this.logoDrawer = Objects.requireNonNullElseGet(logoDrawer, () -> new LogoDrawer(false));
	}

	private boolean isRealmsNotificationsGuiDisplayed() {
		return realmsNotificationGui != null;
	}

	@Override
	public void tick() {
		if (isRealmsNotificationsGuiDisplayed()) {
			realmsNotificationGui.tick();
		}
	}

	public static void registerTextures(TextureManager textureManager) {
		textureManager.registerTexture(LogoDrawer.LOGO_TEXTURE);
		textureManager.registerTexture(LogoDrawer.EDITION_TEXTURE);
		textureManager.registerTexture(RotatingCubeMapRenderer.OVERLAY_TEXTURE);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	private static final int BUTTON_SPACING = 24;
	private static final int BUTTON_WIDTH_WIDE = 200;
	private static final int BUTTON_WIDTH_NORMAL = 98;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BOTTOM_ROW_OFFSET = 36;
	private static final int LANG_BUTTON_X_OFFSET = 124;
	private static final int ACCESS_BUTTON_X_OFFSET = 104;

	@Override
	protected void init() {
		if (splashText == null) {
			splashText = client.getSplashTextLoader().get();
		}

		int copyrightWidth = textRenderer.getWidth(COPYRIGHT);
		int copyrightX = width - copyrightWidth - 2;
		int buttonY = height / 4 + 48;

		buttonY = client.isDemo()
			? addDemoWidgets(buttonY, BUTTON_SPACING)
			: addNormalWidgets(buttonY, BUTTON_SPACING);

		buttonY = addDevelopmentWidgets(buttonY, BUTTON_SPACING);
		buttonY += BOTTOM_ROW_OFFSET;

		TextIconButtonWidget langButton = addDrawableChild(
			AccessibilityOnboardingButtons.createLanguageButton(
				BUTTON_HEIGHT,
				button -> client.setScreen(new LanguageOptionsScreen(this, client.options, client.getLanguageManager())),
				true
			)
		);
		langButton.setPosition(width / 2 - LANG_BUTTON_X_OFFSET, buttonY);

		addDrawableChild(
			ButtonWidget.builder(
				Text.translatable("menu.options"),
				button -> client.setScreen(new OptionsScreen(this, client.options))
			).dimensions(width / 2 - 100, buttonY, BUTTON_WIDTH_NORMAL, BUTTON_HEIGHT).build()
		);
		addDrawableChild(
			ButtonWidget.builder(
				Text.translatable("menu.quit"),
				button -> client.scheduleStop()
			).dimensions(width / 2 + 2, buttonY, BUTTON_WIDTH_NORMAL, BUTTON_HEIGHT).build()
		);

		TextIconButtonWidget accessButton = addDrawableChild(
			AccessibilityOnboardingButtons.createAccessibilityButton(
				BUTTON_HEIGHT,
				button -> client.setScreen(new AccessibilityOptionsScreen(this, client.options)),
				true
			)
		);
		accessButton.setPosition(width / 2 + ACCESS_BUTTON_X_OFFSET, buttonY);

		addDrawableChild(
			new PressableTextWidget(
				copyrightX,
				height - 10,
				copyrightWidth,
				10,
				COPYRIGHT,
				button -> client.setScreen(new CreditsAndAttributionScreen(this)),
				textRenderer
			)
		);

		if (realmsNotificationGui == null) {
			realmsNotificationGui = new RealmsNotificationsScreen();
		}

		if (isRealmsNotificationsGuiDisplayed()) {
			realmsNotificationGui.init(width, height);
		}
	}

	private int addDevelopmentWidgets(int y, int spacingY) {
		if (SharedConstants.isDevelopment) {
			addDrawableChild(
				ButtonWidget.builder(
					Text.literal("Create Test World"),
					button -> CreateWorldScreen.showTestWorld(client, () -> client.setScreen(this))
				).dimensions(width / 2 - 100, y += spacingY, BUTTON_WIDTH_WIDE, BUTTON_HEIGHT).build()
			);
		}

		return y;
	}

	private int addNormalWidgets(int y, int spacingY) {
		addDrawableChild(
			ButtonWidget.builder(
				Text.translatable("menu.singleplayer"),
				button -> client.setScreen(new SelectWorldScreen(this))
			).dimensions(width / 2 - 100, y, BUTTON_WIDTH_WIDE, BUTTON_HEIGHT).build()
		);

		Text disabledText = getMultiplayerDisabledText();
		boolean multiplayerEnabled = disabledText == null;
		Tooltip tooltip = disabledText != null ? Tooltip.of(disabledText) : null;

		int multiplayerY = y + spacingY;
		addDrawableChild(
			ButtonWidget.builder(
				Text.translatable("menu.multiplayer"),
				button -> {
					Screen screen = client.options.skipMultiplayerWarning
						? new MultiplayerScreen(this)
						: new MultiplayerWarningScreen(this);
					client.setScreen(screen);
				}
			).dimensions(width / 2 - 100, multiplayerY, BUTTON_WIDTH_WIDE, BUTTON_HEIGHT).tooltip(tooltip).build()
		).active = multiplayerEnabled;

		int realmsY = multiplayerY + spacingY;
		addDrawableChild(
			ButtonWidget.builder(
				Text.translatable("menu.online"),
				button -> client.setScreen(new RealmsMainScreen(this))
			).dimensions(width / 2 - 100, realmsY, BUTTON_WIDTH_WIDE, BUTTON_HEIGHT).tooltip(tooltip).build()
		).active = multiplayerEnabled;

		return realmsY;
	}

	private @Nullable Text getMultiplayerDisabledText() {
		if (client.isMultiplayerEnabled()) {
			return null;
		}

		if (client.isUsernameBanned()) {
			return Text.translatable("title.multiplayer.disabled.banned.name");
		}

		BanDetails banDetails = client.getMultiplayerBanDetails();
		if (banDetails == null) {
			return Text.translatable("title.multiplayer.disabled");
		}

		return banDetails.expires() != null
			? Text.translatable("title.multiplayer.disabled.banned.temporary")
			: Text.translatable("title.multiplayer.disabled.banned.permanent");
	}

	private int addDemoWidgets(int y, int spacingY) {
		boolean demoExists = canReadDemoWorldData();
		addDrawableChild(
			ButtonWidget.builder(
				Text.translatable("menu.playdemo"),
				button -> {
					if (demoExists) {
						client.createIntegratedServerLoader().start(DEMO_WORLD_NAME, () -> client.setScreen(this));
					} else {
						client.createIntegratedServerLoader().createAndStart(
							DEMO_WORLD_NAME,
							MinecraftServer.DEMO_LEVEL_INFO,
							GeneratorOptions.DEMO_OPTIONS,
							WorldPresets::createDemoOptions,
							this
						);
					}
				}
			).dimensions(width / 2 - 100, y, BUTTON_WIDTH_WIDE, BUTTON_HEIGHT).build()
		);

		int resetY = y + spacingY;
		ButtonWidget resetButton = addDrawableChild(
			ButtonWidget.builder(
				Text.translatable("menu.resetdemo"),
				button -> {
					LevelStorage levelStorage = client.getLevelStorage();
					try (LevelStorage.Session session = levelStorage.createSessionWithoutSymlinkCheck(DEMO_WORLD_NAME)) {
						if (session.levelDatExists()) {
							client.setScreen(new ConfirmScreen(
								this::onDemoDeletionConfirmed,
								Text.translatable("selectWorld.deleteQuestion"),
								Text.translatable("selectWorld.deleteWarning", MinecraftServer.DEMO_LEVEL_INFO.getLevelName()),
								Text.translatable("selectWorld.deleteButton"),
								ScreenTexts.CANCEL
							));
						}
					} catch (IOException exception) {
						SystemToast.addWorldAccessFailureToast(client, DEMO_WORLD_NAME);
						LOGGER.warn("Failed to access demo world", exception);
					}
				}
			).dimensions(width / 2 - 100, resetY, BUTTON_WIDTH_WIDE, BUTTON_HEIGHT).build()
		);
		resetButton.active = demoExists;
		return resetY;
	}

	private boolean canReadDemoWorldData() {
		try (LevelStorage.Session session = client.getLevelStorage().createSessionWithoutSymlinkCheck(DEMO_WORLD_NAME)) {
			return session.levelDatExists();
		} catch (IOException exception) {
			SystemToast.addWorldAccessFailureToast(client, DEMO_WORLD_NAME);
			LOGGER.warn("Failed to read demo world data", exception);
			return false;
		}
	}

	private static final float FADE_DURATION_MS = 2000.0F;

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		if (backgroundFadeStart == 0L && doBackgroundFade) {
			backgroundFadeStart = Util.getMeasuringTimeMs();
		}

		float alpha = 1.0F;
		if (doBackgroundFade) {
			float fadeProgress = (float) (Util.getMeasuringTimeMs() - backgroundFadeStart) / FADE_DURATION_MS;
			if (fadeProgress > 1.0F) {
				doBackgroundFade = false;
			} else {
				fadeProgress = MathHelper.clamp(fadeProgress, 0.0F, 1.0F);
				alpha = MathHelper.clampedMap(fadeProgress, 0.5F, 1.0F, 0.0F, 1.0F);
			}

			setWidgetAlpha(alpha);
		}

		renderPanoramaBackground(context, deltaTicks);
		super.render(context, mouseX, mouseY, deltaTicks);
		logoDrawer.draw(context, width, logoDrawer.shouldIgnoreAlpha() ? 1.0F : alpha);

		if (splashText != null && !client.options.getHideSplashTexts().getValue()) {
			splashText.render(context, width, textRenderer, alpha);
		}

		String versionText = "Minecraft " + SharedConstants.getGameVersion().name();
		if (client.isDemo()) {
			versionText = versionText + " Demo";
		} else {
			versionText = versionText + ("release".equalsIgnoreCase(client.getVersionType())
				? ""
				: "/" + client.getVersionType());
		}

		if (MinecraftClient.getModStatus().isModded()) {
			versionText = versionText + I18n.translate("menu.modded");
		}

		context.drawTextWithShadow(textRenderer, versionText, 2, height - 10, ColorHelper.getWhite(alpha));

		if (isRealmsNotificationsGuiDisplayed() && alpha >= 1.0F) {
			realmsNotificationGui.render(context, mouseX, mouseY, deltaTicks);
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		return super.mouseClicked(click, doubled)
			|| isRealmsNotificationsGuiDisplayed() && realmsNotificationGui.mouseClicked(click, doubled);
	}

	@Override
	public void removed() {
		if (realmsNotificationGui != null) {
			realmsNotificationGui.removed();
		}
	}

	@Override
	public void onDisplayed() {
		super.onDisplayed();
		if (realmsNotificationGui != null) {
			realmsNotificationGui.onDisplayed();
		}
	}

	private void onDemoDeletionConfirmed(boolean delete) {
		if (delete) {
			try (LevelStorage.Session session = client.getLevelStorage().createSessionWithoutSymlinkCheck(DEMO_WORLD_NAME)) {
				session.deleteSessionLock();
			} catch (IOException exception) {
				SystemToast.addWorldDeleteFailureToast(client, DEMO_WORLD_NAME);
				LOGGER.warn("Failed to delete demo world", exception);
			}
		}

		client.setScreen(this);
	}

	@Override
	public boolean canInterruptOtherScreen() {
		return true;
	}
}
