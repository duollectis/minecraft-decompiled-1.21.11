package net.minecraft.client;

import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.navigation.GuiNavigationType;
import net.minecraft.client.gui.screen.DebugOptionsScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.GameModeSwitcherScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.NarratorMode;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.util.Clipboard;
import net.minecraft.client.util.GlfwUtil;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.Window;
import net.minecraft.command.DefaultPermissions;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.c2s.play.ChangeGameModeC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.GameModeCommand;
import net.minecraft.server.command.VersionCommand;
import net.minecraft.storage.NbtWriteView;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.WinNativeModuleUtil;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.util.FeatureDebugLogger;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class Keyboard {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int DEBUG_CRASH_TIME = 10000;
   private final MinecraftClient client;
   private final Clipboard clipboard = new Clipboard();
   private long debugCrashStartTime = -1L;
   private long debugCrashLastLogTime = -1L;
   private long debugCrashElapsedTime = -1L;
   private boolean switchF3State;

   public Keyboard(MinecraftClient client) {
      this.client = client;
   }

   private boolean processDebugKeys(KeyInput input) {
      switch (input.key()) {
         case 69:
            if (this.client.player == null) {
               return false;
            }

            boolean bl = this.client.debugHudEntryList.toggleVisibility(DebugHudEntries.CHUNK_SECTION_PATHS);
            this.debugLog("SectionPath: " + (bl ? "shown" : "hidden"));
            return true;
         case 70:
            boolean bl3 = FogRenderer.toggleFog();
            this.debugLog("Fog: ", bl3);
            return true;
         case 71:
         case 72:
         case 73:
         case 74:
         case 75:
         case 77:
         case 78:
         case 80:
         case 81:
         case 82:
         case 83:
         case 84:
         default:
            return false;
         case 76:
            this.client.chunkCullingEnabled = !this.client.chunkCullingEnabled;
            this.debugLog("SmartCull: ", this.client.chunkCullingEnabled);
            return true;
         case 79:
            if (this.client.player == null) {
               return false;
            }

            boolean bl2 = this.client.debugHudEntryList.toggleVisibility(DebugHudEntries.CHUNK_SECTION_OCTREE);
            this.debugLog("Frustum culling Octree: ", bl2);
            return true;
         case 85:
            if (input.hasShift()) {
               this.client.worldRenderer.killFrustum();
               this.debugLog("Killed frustum");
            } else {
               this.client.worldRenderer.captureFrustum();
               this.debugLog("Captured frustum");
            }

            return true;
         case 86:
            if (this.client.player == null) {
               return false;
            }

            boolean bl4 = this.client.debugHudEntryList.toggleVisibility(DebugHudEntries.CHUNK_SECTION_VISIBILITY);
            this.debugLog("SectionVisibility: ", bl4);
            return true;
         case 87:
            this.client.wireFrame = !this.client.wireFrame;
            this.debugLog("WireFrame: ", this.client.wireFrame);
            return true;
      }
   }

   private void debugLog(String message, boolean value) {
      this.debugLog(message + (value ? "enabled" : "disabled"));
   }

   private void sendMessage(Text message) {
      this.client.inGameHud.getChatHud().addMessage(message);
      this.client.getNarratorManager().narrateSystemMessage(message);
   }

   private static Text getDebugMessage(Formatting formatting, Text message) {
      return Text.empty().append(Text.translatable("debug.prefix").formatted(formatting, Formatting.BOLD)).append(ScreenTexts.SPACE).append(message);
   }

   private void debugError(Text message) {
      this.sendMessage(getDebugMessage(Formatting.RED, message));
   }

   private void debugLog(Text text) {
      this.sendMessage(getDebugMessage(Formatting.YELLOW, text));
   }

   private void debugLog(String key, Object... args) {
      this.debugLog(Text.translatable(key, args));
   }

   private void debugLog(String message) {
      this.debugLog(Text.literal(message));
   }

   private boolean processF3(KeyInput key) {
      if (this.debugCrashStartTime > 0L && this.debugCrashStartTime < Util.getMeasuringTimeMs() - 100L) {
         return true;
      } else if (SharedConstants.HOTKEYS && this.processDebugKeys(key)) {
         return true;
      } else {
         if (SharedConstants.FEATURE_COUNT) {
            switch (key.key()) {
               case 76:
                  FeatureDebugLogger.dump();
                  return true;
               case 82:
                  FeatureDebugLogger.clear();
                  return true;
            }
         }

         GameOptions gameOptions = this.client.options;
         boolean bl = false;
         if (gameOptions.debugReloadChunkKey.matchesKey(key)) {
            this.client.worldRenderer.reload();
            this.debugLog("debug.reload_chunks.message");
            bl = true;
         }

         if (gameOptions.debugShowHitboxesKey.matchesKey(key) && this.client.player != null && !this.client.player.hasReducedDebugInfo()) {
            boolean bl2 = this.client.debugHudEntryList.toggleVisibility(DebugHudEntries.ENTITY_HITBOXES);
            this.debugLog(bl2 ? "debug.show_hitboxes.on" : "debug.show_hitboxes.off");
            bl = true;
         }

         if (gameOptions.debugClearChatKey.matchesKey(key)) {
            this.client.inGameHud.getChatHud().clear(false);
            bl = true;
         }

         if (gameOptions.debugShowChunkBordersKey.matchesKey(key) && this.client.player != null && !this.client.player.hasReducedDebugInfo()) {
            boolean bl2 = this.client.debugHudEntryList.toggleVisibility(DebugHudEntries.CHUNK_BORDERS);
            this.debugLog(bl2 ? "debug.chunk_boundaries.on" : "debug.chunk_boundaries.off");
            bl = true;
         }

         if (gameOptions.debugShowAdvancedTooltipsKey.matchesKey(key)) {
            gameOptions.advancedItemTooltips = !gameOptions.advancedItemTooltips;
            this.debugLog(gameOptions.advancedItemTooltips ? "debug.advanced_tooltips.on" : "debug.advanced_tooltips.off");
            gameOptions.write();
            bl = true;
         }

         if (gameOptions.debugCopyRecreateCommandKey.matchesKey(key)) {
            if (this.client.player != null && !this.client.player.hasReducedDebugInfo()) {
               this.copyLookAt(this.client.player.getPermissions().hasPermission(DefaultPermissions.GAMEMASTERS), !key.hasShift());
            }

            bl = true;
         }

         if (gameOptions.debugSpectateKey.matchesKey(key)) {
            if (this.client.player == null || !GameModeCommand.PERMISSION_CHECK.allows(this.client.player.getPermissions())) {
               this.debugLog("debug.creative_spectator.error");
            } else if (!this.client.player.isSpectator()) {
               this.client.player.networkHandler.sendPacket(new ChangeGameModeC2SPacket(GameMode.SPECTATOR));
            } else {
               GameMode gameMode = (GameMode)MoreObjects.firstNonNull(this.client.interactionManager.getPreviousGameMode(), GameMode.CREATIVE);
               this.client.player.networkHandler.sendPacket(new ChangeGameModeC2SPacket(gameMode));
            }

            bl = true;
         }

         if (gameOptions.debugSwitchGameModeKey.matchesKey(key) && this.client.world != null && this.client.currentScreen == null) {
            if (this.client.canSwitchGameMode() && GameModeCommand.PERMISSION_CHECK.allows(this.client.player.getPermissions())) {
               this.client.setScreen(new GameModeSwitcherScreen());
            } else {
               this.debugLog("debug.gamemodes.error");
            }

            bl = true;
         }

         if (gameOptions.debugOptionsKey.matchesKey(key)) {
            if (this.client.currentScreen instanceof DebugOptionsScreen) {
               this.client.currentScreen.close();
            } else if (this.client.canCurrentScreenInterruptOtherScreen()) {
               if (this.client.currentScreen != null) {
                  this.client.currentScreen.close();
               }

               this.client.setScreen(new DebugOptionsScreen());
            }

            bl = true;
         }

         if (gameOptions.debugFocusPauseKey.matchesKey(key)) {
            gameOptions.pauseOnLostFocus = !gameOptions.pauseOnLostFocus;
            gameOptions.write();
            this.debugLog(gameOptions.pauseOnLostFocus ? "debug.pause_focus.on" : "debug.pause_focus.off");
            bl = true;
         }

         if (gameOptions.debugDumpDynamicTexturesKey.matchesKey(key)) {
            Path path = this.client.runDirectory.toPath().toAbsolutePath();
            Path path2 = TextureUtil.getDebugTexturePath(path);
            this.client.getTextureManager().dumpDynamicTextures(path2);
            Text text = Text.literal(path.relativize(path2).toString())
               .formatted(Formatting.UNDERLINE)
               .styled(style -> style.withClickEvent(new ClickEvent.OpenFile(path2)));
            this.debugLog(Text.translatable("debug.dump_dynamic_textures", text));
            bl = true;
         }

         if (gameOptions.debugReloadResourcePacksKey.matchesKey(key)) {
            this.debugLog("debug.reload_resourcepacks.message");
            this.client.reloadResources();
            bl = true;
         }

         if (gameOptions.debugProfilingKey.matchesKey(key)) {
            if (this.client.toggleDebugProfiler(this::debugLog)) {
               this.debugLog(
                  Text.translatable(
                     "debug.profiling.start",
                     10,
                     gameOptions.debugModifierKey.getBoundKeyLocalizedText(),
                     gameOptions.debugProfilingKey.getBoundKeyLocalizedText()
                  )
               );
            }

            bl = true;
         }

         if (gameOptions.debugCopyLocationKey.matchesKey(key) && this.client.player != null && !this.client.player.hasReducedDebugInfo()) {
            this.debugLog("debug.copy_location.message");
            this.setClipboard(
               String.format(
                  Locale.ROOT,
                  "/execute in %s run tp @s %.2f %.2f %.2f %.2f %.2f",
                  this.client.player.getEntityWorld().getRegistryKey().getValue(),
                  this.client.player.getX(),
                  this.client.player.getY(),
                  this.client.player.getZ(),
                  this.client.player.getYaw(),
                  this.client.player.getPitch()
               )
            );
            bl = true;
         }

         if (gameOptions.debugDumpVersionKey.matchesKey(key)) {
            this.debugLog("debug.version.header");
            VersionCommand.acceptInfo(this::sendMessage);
            bl = true;
         }

         if (gameOptions.debugProfilingChartKey.matchesKey(key)) {
            this.client.getDebugHud().toggleRenderingChart();
            bl = true;
         }

         if (gameOptions.debugFpsChartsKey.matchesKey(key)) {
            this.client.getDebugHud().toggleRenderingAndTickCharts();
            bl = true;
         }

         if (gameOptions.debugNetworkChartsKey.matchesKey(key)) {
            this.client.getDebugHud().togglePacketSizeAndPingCharts();
            bl = true;
         }

         return bl;
      }
   }

   private void copyLookAt(boolean hasQueryPermission, boolean queryServer) {
      HitResult hitResult = this.client.crosshairTarget;
      if (hitResult != null) {
         switch (hitResult.getType()) {
            case BLOCK:
               BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
               World world = this.client.player.getEntityWorld();
               BlockState blockState = world.getBlockState(blockPos);
               if (hasQueryPermission) {
                  if (queryServer) {
                     this.client.player.networkHandler.getDataQueryHandler().queryBlockNbt(blockPos, nbt -> {
                        this.copyBlock(blockState, blockPos, nbt);
                        this.debugLog("debug.inspect.server.block");
                     });
                  } else {
                     BlockEntity blockEntity = world.getBlockEntity(blockPos);
                     NbtCompound nbtCompound = blockEntity != null ? blockEntity.createNbt(world.getRegistryManager()) : null;
                     this.copyBlock(blockState, blockPos, nbtCompound);
                     this.debugLog("debug.inspect.client.block");
                  }
               } else {
                  this.copyBlock(blockState, blockPos, null);
                  this.debugLog("debug.inspect.client.block");
               }
               break;
            case ENTITY:
               Entity entity = ((EntityHitResult)hitResult).getEntity();
               Identifier identifier = Registries.ENTITY_TYPE.getId(entity.getType());
               if (hasQueryPermission) {
                  if (queryServer) {
                     this.client.player.networkHandler.getDataQueryHandler().queryEntityNbt(entity.getId(), nbt -> {
                        this.copyEntity(identifier, entity.getEntityPos(), nbt);
                        this.debugLog("debug.inspect.server.entity");
                     });
                  } else {
                     try (ErrorReporter.Logging logging = new ErrorReporter.Logging(entity.getErrorReporterContext(), LOGGER)) {
                        NbtWriteView nbtWriteView = NbtWriteView.create(logging, entity.getRegistryManager());
                        entity.writeData(nbtWriteView);
                        this.copyEntity(identifier, entity.getEntityPos(), nbtWriteView.getNbt());
                     }

                     this.debugLog("debug.inspect.client.entity");
                  }
               } else {
                  this.copyEntity(identifier, entity.getEntityPos(), null);
                  this.debugLog("debug.inspect.client.entity");
               }
         }
      }
   }

   private void copyBlock(BlockState state, BlockPos pos, @Nullable NbtCompound nbt) {
      StringBuilder stringBuilder = new StringBuilder(BlockArgumentParser.stringifyBlockState(state));
      if (nbt != null) {
         stringBuilder.append(nbt);
      }

      String string = String.format(Locale.ROOT, "/setblock %d %d %d %s", pos.getX(), pos.getY(), pos.getZ(), stringBuilder);
      this.setClipboard(string);
   }

   private void copyEntity(Identifier id, Vec3d pos, @Nullable NbtCompound nbt) {
      String string2;
      if (nbt != null) {
         nbt.remove("UUID");
         nbt.remove("Pos");
         String string = NbtHelper.toPrettyPrintedText(nbt).getString();
         string2 = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f %s", id, pos.x, pos.y, pos.z, string);
      } else {
         string2 = String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f", id, pos.x, pos.y, pos.z);
      }

      this.setClipboard(string2);
   }

   private void onKey(long window, @KeyInput.KeyAction int action, KeyInput input) {
      Window window2 = this.client.getWindow();
      if (window == window2.getHandle()) {
         this.client.getInactivityFpsLimiter().onInput();
         GameOptions gameOptions = this.client.options;
         boolean bl = gameOptions.debugModifierKey.boundKey.getCode() == gameOptions.debugOverlayKey.boundKey.getCode();
         boolean bl2 = gameOptions.debugModifierKey.isPressed();
         boolean bl3 = !gameOptions.debugCrashKey.isUnbound() && InputUtil.isKeyPressed(this.client.getWindow(), gameOptions.debugCrashKey.boundKey.getCode());
         if (this.debugCrashStartTime > 0L) {
            if (!bl3 || !bl2) {
               this.debugCrashStartTime = -1L;
            }
         } else if (bl3 && bl2) {
            this.switchF3State = bl;
            this.debugCrashStartTime = Util.getMeasuringTimeMs();
            this.debugCrashLastLogTime = Util.getMeasuringTimeMs();
            this.debugCrashElapsedTime = 0L;
         }

         Screen screen = this.client.currentScreen;
         if (screen != null) {
            switch (input.key()) {
               case 258:
                  this.client.setNavigationType(GuiNavigationType.KEYBOARD_TAB);
               case 259:
               case 260:
               case 261:
               default:
                  break;
               case 262:
               case 263:
               case 264:
               case 265:
                  this.client.setNavigationType(GuiNavigationType.KEYBOARD_ARROW);
            }
         }

         if (action == 1
            && (!(this.client.currentScreen instanceof KeybindsScreen) || ((KeybindsScreen)screen).lastKeyCodeUpdateTime <= Util.getMeasuringTimeMs() - 20L)) {
            if (gameOptions.fullscreenKey.matchesKey(input)) {
               window2.toggleFullscreen();
               boolean bl4 = window2.isFullscreen();
               gameOptions.getFullscreen().setValue(bl4);
               gameOptions.write();
               if (this.client.currentScreen instanceof VideoOptionsScreen videoOptionsScreen) {
                  videoOptionsScreen.updateFullscreenButtonValue(bl4);
               }

               return;
            }

            if (gameOptions.screenshotKey.matchesKey(input)) {
               if (input.hasCtrlOrCmd() && SharedConstants.PANORAMA_SCREENSHOT) {
                  this.sendMessage(this.client.takePanorama(this.client.runDirectory));
               } else {
                  ScreenshotRecorder.saveScreenshot(
                     this.client.runDirectory, this.client.getFramebuffer(), message -> this.client.execute(() -> this.sendMessage(message))
                  );
               }

               return;
            }
         }

         if (action != 0) {
            boolean bl4 = screen == null || !(screen.getFocused() instanceof TextFieldWidget) || !((TextFieldWidget)screen.getFocused()).isActive();
            if (bl4) {
               if (input.hasCtrlOrCmd() && input.key() == 66 && this.client.getNarratorManager().isActive() && gameOptions.getNarratorHotkey().getValue()) {
                  boolean bl5 = gameOptions.getNarrator().getValue() == NarratorMode.OFF;
                  gameOptions.getNarrator().setValue(NarratorMode.byId(gameOptions.getNarrator().getValue().getId() + 1));
                  gameOptions.write();
                  if (screen != null) {
                     screen.refreshNarrator(bl5);
                  }
               }

               ClientPlayerEntity var21 = this.client.player;
            }
         }

         if (screen != null) {
            try {
               if (action != 1 && action != 2) {
                  if (action == 0 && screen.keyReleased(input)) {
                     if (gameOptions.debugModifierKey.matchesKey(input)) {
                        this.switchF3State = false;
                     }

                     return;
                  }
               } else {
                  screen.applyKeyPressNarratorDelay();
                  if (screen.keyPressed(input)) {
                     if (this.client.currentScreen == null) {
                        InputUtil.Key key = InputUtil.fromKeyCode(input);
                        KeyBinding.setKeyPressed(key, false);
                     }

                     return;
                  }
               }
            } catch (Throwable var17) {
               CrashReport crashReport = CrashReport.create(var17, "keyPressed event handler");
               screen.addCrashReportSection(crashReport);
               CrashReportSection crashReportSection = crashReport.addElement("Key");
               crashReportSection.add("Key", input.key());
               crashReportSection.add("Scancode", input.scancode());
               crashReportSection.add("Mods", input.modifiers());
               throw new CrashException(crashReport);
            }
         }

         InputUtil.Key key = InputUtil.fromKeyCode(input);
         boolean bl5 = this.client.currentScreen == null;
         boolean bl6 = bl5
            || this.client.currentScreen instanceof GameMenuScreen gameMenuScreen && !gameMenuScreen.shouldShowMenu()
            || this.client.currentScreen instanceof GameModeSwitcherScreen;
         if (bl && gameOptions.debugModifierKey.matchesKey(input) && action == 0) {
            if (this.switchF3State) {
               this.switchF3State = false;
            } else {
               this.client.debugHudEntryList.toggleF3Enabled();
            }
         } else if (!bl && gameOptions.debugOverlayKey.matchesKey(input) && action == 1) {
            this.client.debugHudEntryList.toggleF3Enabled();
         }

         if (action == 0) {
            KeyBinding.setKeyPressed(key, false);
         } else {
            boolean bl7 = false;
            if (bl6 && input.isEscape()) {
               this.client.openGameMenu(bl2);
               bl7 = bl2;
            } else if (bl2) {
               bl7 = this.processF3(input);
               if (bl7 && screen instanceof DebugOptionsScreen debugOptionsScreen) {
                  DebugOptionsScreen.OptionsListWidget optionsListWidget = debugOptionsScreen.getOptionsListWidget();
                  if (optionsListWidget != null) {
                     optionsListWidget.children().forEach(DebugOptionsScreen.AbstractEntry::init);
                  }
               }
            } else if (bl6 && gameOptions.toggleGuiKey.matchesKey(input)) {
               gameOptions.hudHidden = !gameOptions.hudHidden;
            } else if (bl6 && gameOptions.toggleSpectatorShaderEffectsKey.matchesKey(input)) {
               this.client.gameRenderer.togglePostProcessorEnabled();
            }

            if (bl) {
               this.switchF3State |= bl7;
            }

            if (this.client.getDebugHud().shouldShowRenderingChart() && !bl2) {
               int i = input.asNumber();
               if (i != -1) {
                  this.client.getDebugHud().getPieChart().select(i);
               }
            }

            if (bl5 || key == gameOptions.debugModifierKey.boundKey) {
               if (bl7) {
                  KeyBinding.setKeyPressed(key, false);
               } else {
                  KeyBinding.setKeyPressed(key, true);
                  KeyBinding.onKeyPressed(key);
               }
            }
         }
      }
   }

   private void onChar(long window, CharInput input) {
      if (window == this.client.getWindow().getHandle()) {
         Screen screen = this.client.currentScreen;
         if (screen != null && this.client.getOverlay() == null) {
            try {
               screen.charTyped(input);
            } catch (Throwable var8) {
               CrashReport crashReport = CrashReport.create(var8, "charTyped event handler");
               screen.addCrashReportSection(crashReport);
               CrashReportSection crashReportSection = crashReport.addElement("Key");
               crashReportSection.add("Codepoint", input.codepoint());
               crashReportSection.add("Mods", input.modifiers());
               throw new CrashException(crashReport);
            }
         }
      }
   }

   public void setup(Window window) {
      InputUtil.setKeyboardCallbacks(window, (handle, key, scancode, action, modifiers) -> {
         KeyInput keyInput = new KeyInput(key, scancode, modifiers);
         this.client.execute(() -> this.onKey(handle, action, keyInput));
      }, (windowx, codePoint, modifiers) -> {
         CharInput charInput = new CharInput(codePoint, modifiers);
         this.client.execute(() -> this.onChar(windowx, charInput));
      });
   }

   public String getClipboard() {
      return this.clipboard.get(this.client.getWindow(), (error, description) -> {
         if (error != 65545) {
            this.client.getWindow().logGlError(error, description);
         }
      });
   }

   public void setClipboard(String clipboard) {
      if (!clipboard.isEmpty()) {
         this.clipboard.set(this.client.getWindow(), clipboard);
      }
   }

   public void pollDebugCrash() {
      if (this.debugCrashStartTime > 0L) {
         long l = Util.getMeasuringTimeMs();
         long m = 10000L - (l - this.debugCrashStartTime);
         long n = l - this.debugCrashLastLogTime;
         if (m < 0L) {
            if (this.client.isCtrlPressed()) {
               GlfwUtil.makeJvmCrash();
            }

            String string = "Manually triggered debug crash";
            CrashReport crashReport = new CrashReport("Manually triggered debug crash", new Throwable("Manually triggered debug crash"));
            CrashReportSection crashReportSection = crashReport.addElement("Manual crash details");
            WinNativeModuleUtil.addDetailTo(crashReportSection);
            throw new CrashException(crashReport);
         }

         if (n >= 1000L) {
            if (this.debugCrashElapsedTime == 0L) {
               this.debugLog(
                  "debug.crash.message",
                  this.client.options.debugModifierKey.getBoundKeyLocalizedText().getString(),
                  this.client.options.debugCrashKey.getBoundKeyLocalizedText().getString()
               );
            } else {
               this.debugError(Text.translatable("debug.crash.warning", MathHelper.ceil((float)m / 1000.0F)));
            }

            this.debugCrashLastLogTime = l;
            this.debugCrashElapsedTime++;
         }
      }
   }
}
