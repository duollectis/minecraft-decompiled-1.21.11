package net.minecraft.client.gui.screen;

import com.mojang.authlib.minecraft.BanDetails;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.Objects;
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
      return this.realmsNotificationGui != null;
   }

   @Override
   public void tick() {
      if (this.isRealmsNotificationsGuiDisplayed()) {
         this.realmsNotificationGui.tick();
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

   @Override
   protected void init() {
      if (this.splashText == null) {
         this.splashText = this.client.getSplashTextLoader().get();
      }

      int i = this.textRenderer.getWidth(COPYRIGHT);
      int j = this.width - i - 2;
      int k = 24;
      int l = this.height / 4 + 48;
      if (this.client.isDemo()) {
         l = this.addDemoWidgets(l, 24);
      } else {
         l = this.addNormalWidgets(l, 24);
      }

      l = this.addDevelopmentWidgets(l, 24);
      TextIconButtonWidget textIconButtonWidget = this.addDrawableChild(
         AccessibilityOnboardingButtons.createLanguageButton(
            20, button -> this.client.setScreen(new LanguageOptionsScreen(this, this.client.options, this.client.getLanguageManager())), true
         )
      );
      int var10001 = this.width / 2 - 124;
      l += 36;
      textIconButtonWidget.setPosition(var10001, l);
      this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("menu.options"), button -> this.client.setScreen(new OptionsScreen(this, this.client.options)))
            .dimensions(this.width / 2 - 100, l, 98, 20)
            .build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("menu.quit"), button -> this.client.scheduleStop()).dimensions(this.width / 2 + 2, l, 98, 20).build()
      );
      TextIconButtonWidget textIconButtonWidget2 = this.addDrawableChild(
         AccessibilityOnboardingButtons.createAccessibilityButton(
            20, button -> this.client.setScreen(new AccessibilityOptionsScreen(this, this.client.options)), true
         )
      );
      textIconButtonWidget2.setPosition(this.width / 2 + 104, l);
      this.addDrawableChild(
         new PressableTextWidget(
            j, this.height - 10, i, 10, COPYRIGHT, button -> this.client.setScreen(new CreditsAndAttributionScreen(this)), this.textRenderer
         )
      );
      if (this.realmsNotificationGui == null) {
         this.realmsNotificationGui = new RealmsNotificationsScreen();
      }

      if (this.isRealmsNotificationsGuiDisplayed()) {
         this.realmsNotificationGui.init(this.width, this.height);
      }
   }

   private int addDevelopmentWidgets(int y, int spacingY) {
      if (SharedConstants.isDevelopment) {
         this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Create Test World"), button -> CreateWorldScreen.showTestWorld(this.client, () -> this.client.setScreen(this)))
               .dimensions(this.width / 2 - 100, y += spacingY, 200, 20)
               .build()
         );
      }

      return y;
   }

   private int addNormalWidgets(int y, int spacingY) {
      this.addDrawableChild(
         ButtonWidget.builder(Text.translatable("menu.singleplayer"), button -> this.client.setScreen(new SelectWorldScreen(this)))
            .dimensions(this.width / 2 - 100, y, 200, 20)
            .build()
      );
      Text text = this.getMultiplayerDisabledText();
      boolean bl = text == null;
      Tooltip tooltip = text != null ? Tooltip.of(text) : null;
      int var6;
      this.addDrawableChild(ButtonWidget.builder(Text.translatable("menu.multiplayer"), button -> {
         Screen screen = (Screen)(this.client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this));
         this.client.setScreen(screen);
      }).dimensions(this.width / 2 - 100, var6 = y + spacingY, 200, 20).tooltip(tooltip).build()).active = bl;
      this.addDrawableChild(
            ButtonWidget.builder(Text.translatable("menu.online"), button -> this.client.setScreen(new RealmsMainScreen(this)))
               .dimensions(this.width / 2 - 100, y = var6 + spacingY, 200, 20)
               .tooltip(tooltip)
               .build()
         )
         .active = bl;
      return y;
   }

   private @Nullable Text getMultiplayerDisabledText() {
      if (this.client.isMultiplayerEnabled()) {
         return null;
      } else if (this.client.isUsernameBanned()) {
         return Text.translatable("title.multiplayer.disabled.banned.name");
      } else {
         BanDetails banDetails = this.client.getMultiplayerBanDetails();
         if (banDetails != null) {
            return banDetails.expires() != null
               ? Text.translatable("title.multiplayer.disabled.banned.temporary")
               : Text.translatable("title.multiplayer.disabled.banned.permanent");
         } else {
            return Text.translatable("title.multiplayer.disabled");
         }
      }
   }

   private int addDemoWidgets(int y, int spacingY) {
      boolean bl = this.canReadDemoWorldData();
      this.addDrawableChild(
         ButtonWidget.builder(
               Text.translatable("menu.playdemo"),
               button -> {
                  if (bl) {
                     this.client.createIntegratedServerLoader().start("Demo_World", () -> this.client.setScreen(this));
                  } else {
                     this.client
                        .createIntegratedServerLoader()
                        .createAndStart("Demo_World", MinecraftServer.DEMO_LEVEL_INFO, GeneratorOptions.DEMO_OPTIONS, WorldPresets::createDemoOptions, this);
                  }
               }
            )
            .dimensions(this.width / 2 - 100, y, 200, 20)
            .build()
      );
      int var5;
      ButtonWidget buttonWidget = this.addDrawableChild(
         ButtonWidget.builder(
               Text.translatable("menu.resetdemo"),
               button -> {
                  LevelStorage levelStorage = this.client.getLevelStorage();

                  try (LevelStorage.Session session = levelStorage.createSessionWithoutSymlinkCheck("Demo_World")) {
                     if (session.levelDatExists()) {
                        this.client
                           .setScreen(
                              new ConfirmScreen(
                                 this::onDemoDeletionConfirmed,
                                 Text.translatable("selectWorld.deleteQuestion"),
                                 Text.translatable("selectWorld.deleteWarning", MinecraftServer.DEMO_LEVEL_INFO.getLevelName()),
                                 Text.translatable("selectWorld.deleteButton"),
                                 ScreenTexts.CANCEL
                              )
                           );
                     }
                  } catch (IOException var8) {
                     SystemToast.addWorldAccessFailureToast(this.client, "Demo_World");
                     LOGGER.warn("Failed to access demo world", var8);
                  }
               }
            )
            .dimensions(this.width / 2 - 100, var5 = y + spacingY, 200, 20)
            .build()
      );
      buttonWidget.active = bl;
      return var5;
   }

   private boolean canReadDemoWorldData() {
      try {
         boolean var2;
         try (LevelStorage.Session session = this.client.getLevelStorage().createSessionWithoutSymlinkCheck("Demo_World")) {
            var2 = session.levelDatExists();
         }

         return var2;
      } catch (IOException var6) {
         SystemToast.addWorldAccessFailureToast(this.client, "Demo_World");
         LOGGER.warn("Failed to read demo world data", var6);
         return false;
      }
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      if (this.backgroundFadeStart == 0L && this.doBackgroundFade) {
         this.backgroundFadeStart = Util.getMeasuringTimeMs();
      }

      float f = 1.0F;
      if (this.doBackgroundFade) {
         float g = (float)(Util.getMeasuringTimeMs() - this.backgroundFadeStart) / 2000.0F;
         if (g > 1.0F) {
            this.doBackgroundFade = false;
         } else {
            g = MathHelper.clamp(g, 0.0F, 1.0F);
            f = MathHelper.clampedMap(g, 0.5F, 1.0F, 0.0F, 1.0F);
         }

         this.setWidgetAlpha(f);
      }

      this.renderPanoramaBackground(context, deltaTicks);
      super.render(context, mouseX, mouseY, deltaTicks);
      this.logoDrawer.draw(context, this.width, this.logoDrawer.shouldIgnoreAlpha() ? 1.0F : f);
      if (this.splashText != null && !this.client.options.getHideSplashTexts().getValue()) {
         this.splashText.render(context, this.width, this.textRenderer, f);
      }

      String string = "Minecraft " + SharedConstants.getGameVersion().name();
      if (this.client.isDemo()) {
         string = string + " Demo";
      } else {
         string = string + ("release".equalsIgnoreCase(this.client.getVersionType()) ? "" : "/" + this.client.getVersionType());
      }

      if (MinecraftClient.getModStatus().isModded()) {
         string = string + I18n.translate("menu.modded");
      }

      context.drawTextWithShadow(this.textRenderer, string, 2, this.height - 10, ColorHelper.getWhite(f));
      if (this.isRealmsNotificationsGuiDisplayed() && f >= 1.0F) {
         this.realmsNotificationGui.render(context, mouseX, mouseY, deltaTicks);
      }
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      return super.mouseClicked(click, doubled) ? true : this.isRealmsNotificationsGuiDisplayed() && this.realmsNotificationGui.mouseClicked(click, doubled);
   }

   @Override
   public void removed() {
      if (this.realmsNotificationGui != null) {
         this.realmsNotificationGui.removed();
      }
   }

   @Override
   public void onDisplayed() {
      super.onDisplayed();
      if (this.realmsNotificationGui != null) {
         this.realmsNotificationGui.onDisplayed();
      }
   }

   private void onDemoDeletionConfirmed(boolean delete) {
      if (delete) {
         try (LevelStorage.Session session = this.client.getLevelStorage().createSessionWithoutSymlinkCheck("Demo_World")) {
            session.deleteSessionLock();
         } catch (IOException var7) {
            SystemToast.addWorldDeleteFailureToast(this.client, "Demo_World");
            LOGGER.warn("Failed to delete demo world", var7);
         }
      }

      this.client.setScreen(this);
   }

   @Override
   public boolean canInterruptOtherScreen() {
      return true;
   }
}
