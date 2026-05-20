package net.minecraft.client.gui.hud;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.client.gui.hud.bar.ExperienceBar;
import net.minecraft.client.gui.hud.bar.JumpBar;
import net.minecraft.client.gui.hud.bar.LocatorBar;
import net.minecraft.client.gui.hud.debug.DebugHudEntries;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.Window;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.JumpingMount;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nullables;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class InGameHud {
   private static final Identifier CROSSHAIR_TEXTURE = Identifier.ofVanilla("hud/crosshair");
   private static final Identifier CROSSHAIR_ATTACK_INDICATOR_FULL_TEXTURE = Identifier.ofVanilla("hud/crosshair_attack_indicator_full");
   private static final Identifier CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_TEXTURE = Identifier.ofVanilla("hud/crosshair_attack_indicator_background");
   private static final Identifier CROSSHAIR_ATTACK_INDICATOR_PROGRESS_TEXTURE = Identifier.ofVanilla("hud/crosshair_attack_indicator_progress");
   private static final Identifier EFFECT_BACKGROUND_AMBIENT_TEXTURE = Identifier.ofVanilla("hud/effect_background_ambient");
   private static final Identifier EFFECT_BACKGROUND_TEXTURE = Identifier.ofVanilla("hud/effect_background");
   private static final Identifier HOTBAR_TEXTURE = Identifier.ofVanilla("hud/hotbar");
   private static final Identifier HOTBAR_SELECTION_TEXTURE = Identifier.ofVanilla("hud/hotbar_selection");
   private static final Identifier HOTBAR_OFFHAND_LEFT_TEXTURE = Identifier.ofVanilla("hud/hotbar_offhand_left");
   private static final Identifier HOTBAR_OFFHAND_RIGHT_TEXTURE = Identifier.ofVanilla("hud/hotbar_offhand_right");
   private static final Identifier HOTBAR_ATTACK_INDICATOR_BACKGROUND_TEXTURE = Identifier.ofVanilla("hud/hotbar_attack_indicator_background");
   private static final Identifier HOTBAR_ATTACK_INDICATOR_PROGRESS_TEXTURE = Identifier.ofVanilla("hud/hotbar_attack_indicator_progress");
   private static final Identifier ARMOR_EMPTY_TEXTURE = Identifier.ofVanilla("hud/armor_empty");
   private static final Identifier ARMOR_HALF_TEXTURE = Identifier.ofVanilla("hud/armor_half");
   private static final Identifier ARMOR_FULL_TEXTURE = Identifier.ofVanilla("hud/armor_full");
   private static final Identifier FOOD_EMPTY_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_empty_hunger");
   private static final Identifier FOOD_HALF_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_half_hunger");
   private static final Identifier FOOD_FULL_HUNGER_TEXTURE = Identifier.ofVanilla("hud/food_full_hunger");
   private static final Identifier FOOD_EMPTY_TEXTURE = Identifier.ofVanilla("hud/food_empty");
   private static final Identifier FOOD_HALF_TEXTURE = Identifier.ofVanilla("hud/food_half");
   private static final Identifier FOOD_FULL_TEXTURE = Identifier.ofVanilla("hud/food_full");
   private static final Identifier AIR_TEXTURE = Identifier.ofVanilla("hud/air");
   private static final Identifier AIR_BURSTING_TEXTURE = Identifier.ofVanilla("hud/air_bursting");
   private static final Identifier AIR_EMPTY_TEXTURE = Identifier.ofVanilla("hud/air_empty");
   private static final Identifier VEHICLE_CONTAINER_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/vehicle_container");
   private static final Identifier VEHICLE_FULL_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/vehicle_full");
   private static final Identifier VEHICLE_HALF_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/vehicle_half");
   private static final Identifier VIGNETTE_TEXTURE = Identifier.ofVanilla("textures/misc/vignette.png");
   public static final Identifier NAUSEA_TEXTURE = Identifier.ofVanilla("textures/misc/nausea.png");
   private static final Identifier SPYGLASS_SCOPE = Identifier.ofVanilla("textures/misc/spyglass_scope.png");
   private static final Identifier POWDER_SNOW_OUTLINE = Identifier.ofVanilla("textures/misc/powder_snow_outline.png");
   private static final Comparator<ScoreboardEntry> SCOREBOARD_ENTRY_COMPARATOR = Comparator.comparing(ScoreboardEntry::value)
      .reversed()
      .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER);
   private static final Text DEMO_EXPIRED_MESSAGE = Text.translatable("demo.demoExpired");
   private static final Text SAVING_LEVEL_TEXT = Text.translatable("menu.savingLevel");
   private static final float field_32168 = 5.0F;
   private static final int field_59816 = 100;
   private static final int field_32169 = 10;
   private static final int field_32170 = 10;
   private static final String SCOREBOARD_JOINER = ": ";
   private static final float field_32172 = 0.2F;
   private static final int field_33942 = 9;
   private static final int field_33943 = 8;
   private static final int field_54914 = 10;
   private static final int field_54915 = 9;
   private static final int field_54916 = 8;
   private static final int field_54917 = 2;
   private static final int SUBMERGED_IN_WATER_AIR_BUBBLE_DELAY = 1;
   private static final float field_54920 = 0.5F;
   private static final float field_54921 = 0.1F;
   private static final float field_54922 = 1.0F;
   private static final float field_54923 = 0.1F;
   private static final int field_54924 = 3;
   private static final int field_54925 = 5;
   private static final float field_35431 = 0.2F;
   private static final int field_52769 = 5;
   private static final int field_52770 = 5;
   private final Random random = Random.create();
   private final MinecraftClient client;
   private final ChatHud chatHud;
   private int ticks;
   private @Nullable Text overlayMessage;
   private int overlayRemaining;
   private boolean overlayTinted;
   private boolean canShowChatDisabledScreen;
   public float vignetteDarkness = 1.0F;
   private int heldItemTooltipFade;
   private ItemStack currentStack = ItemStack.EMPTY;
   private final DebugHud debugHud;
   private final SubtitlesHud subtitlesHud;
   private final SpectatorHud spectatorHud;
   private final PlayerListHud playerListHud;
   private final BossBarHud bossBarHud;
   private int titleRemainTicks;
   private @Nullable Text title;
   private @Nullable Text subtitle;
   private int titleFadeInTicks;
   private int titleStayTicks;
   private int titleFadeOutTicks;
   private int lastHealthValue;
   private int renderHealthValue;
   private long lastHealthCheckTime;
   private long heartJumpEndTick;
   private int lastBurstBubble;
   private @Nullable Runnable deferredSubtitleRenderer;
   private float autosaveIndicatorAlpha;
   private float lastAutosaveIndicatorAlpha;
   private Pair<InGameHud.BarType, Bar> currentBar = Pair.of(InGameHud.BarType.EMPTY, Bar.EMPTY);
   private final Map<InGameHud.BarType, Supplier<Bar>> bars;
   private float spyglassScale;

   public InGameHud(MinecraftClient client) {
      this.client = client;
      this.debugHud = new DebugHud(client);
      this.spectatorHud = new SpectatorHud(client);
      this.chatHud = new ChatHud(client);
      this.playerListHud = new PlayerListHud(client, this);
      this.bossBarHud = new BossBarHud(client);
      this.subtitlesHud = new SubtitlesHud(client);
      this.bars = ImmutableMap.of(
         InGameHud.BarType.EMPTY,
         (Supplier<Bar>)() -> Bar.EMPTY,
         InGameHud.BarType.EXPERIENCE,
         (Supplier<Bar>)() -> new ExperienceBar(client),
         InGameHud.BarType.LOCATOR,
         (Supplier<Bar>)() -> new LocatorBar(client),
         InGameHud.BarType.JUMPABLE_VEHICLE,
         (Supplier<Bar>)() -> new JumpBar(client)
      );
      this.setDefaultTitleFade();
   }

   public void setDefaultTitleFade() {
      this.titleFadeInTicks = 10;
      this.titleStayTicks = 70;
      this.titleFadeOutTicks = 20;
   }

   public void render(DrawContext context, RenderTickCounter tickCounter) {
      if (!(this.client.currentScreen instanceof LevelLoadingScreen)) {
         if (!this.client.options.hudHidden) {
            this.renderMiscOverlays(context, tickCounter);
            this.renderCrosshair(context, tickCounter);
            context.createNewRootLayer();
            this.renderMainHud(context, tickCounter);
            this.renderStatusEffectOverlay(context, tickCounter);
            this.renderBossBarHud(context, tickCounter);
         }

         this.renderSleepOverlay(context, tickCounter);
         if (!this.client.options.hudHidden) {
            this.renderDemoTimer(context, tickCounter);
            this.renderScoreboardSidebar(context, tickCounter);
            this.renderOverlayMessage(context, tickCounter);
            this.renderTitleAndSubtitle(context, tickCounter);
            this.renderChat(context, tickCounter);
            this.renderPlayerList(context, tickCounter);
            this.renderSubtitlesHud(context, this.client.currentScreen == null || this.client.currentScreen.deferSubtitles());
         } else if (this.client.currentScreen != null && this.client.currentScreen.deferSubtitles()) {
            this.renderSubtitlesHud(context, true);
         }
      }
   }

   private void renderBossBarHud(DrawContext context, RenderTickCounter tickCounter) {
      this.bossBarHud.render(context);
   }

   public void renderDebugHud(DrawContext context) {
      this.debugHud.render(context);
   }

   private void renderSubtitlesHud(DrawContext context, boolean defer) {
      if (defer) {
         this.deferredSubtitleRenderer = () -> this.subtitlesHud.render(context);
      } else {
         this.deferredSubtitleRenderer = null;
         this.subtitlesHud.render(context);
      }
   }

   public void renderDeferredSubtitles() {
      if (this.deferredSubtitleRenderer != null) {
         this.deferredSubtitleRenderer.run();
         this.deferredSubtitleRenderer = null;
      }
   }

   private void renderMiscOverlays(DrawContext context, RenderTickCounter tickCounter) {
      if (this.client.options.getVignette().getValue()) {
         this.renderVignetteOverlay(context, this.client.getCameraEntity());
      }

      ClientPlayerEntity clientPlayerEntity = this.client.player;
      float f = tickCounter.getDynamicDeltaTicks();
      this.spyglassScale = MathHelper.lerp(0.5F * f, this.spyglassScale, 1.125F);
      if (this.client.options.getPerspective().isFirstPerson()) {
         if (clientPlayerEntity.isUsingSpyglass()) {
            this.renderSpyglassOverlay(context, this.spyglassScale);
         } else {
            this.spyglassScale = 0.5F;

            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
               ItemStack itemStack = clientPlayerEntity.getEquippedStack(equipmentSlot);
               EquippableComponent equippableComponent = itemStack.get(DataComponentTypes.EQUIPPABLE);
               if (equippableComponent != null && equippableComponent.slot() == equipmentSlot && equippableComponent.cameraOverlay().isPresent()) {
                  this.renderOverlay(context, equippableComponent.cameraOverlay().get().withPath(overlayTexture -> "textures/" + overlayTexture + ".png"), 1.0F);
               }
            }
         }
      }

      if (clientPlayerEntity.getFrozenTicks() > 0) {
         this.renderOverlay(context, POWDER_SNOW_OUTLINE, clientPlayerEntity.getFreezingScale());
      }

      float g = tickCounter.getTickProgress(false);
      float h = MathHelper.lerp(g, clientPlayerEntity.lastNauseaIntensity, clientPlayerEntity.nauseaIntensity);
      float i = clientPlayerEntity.getEffectFadeFactor(StatusEffects.NAUSEA, g);
      if (h > 0.0F) {
         this.renderPortalOverlay(context, h);
      } else if (i > 0.0F) {
         float j = this.client.options.getDistortionEffectScale().getValue().floatValue();
         if (j < 1.0F) {
            float k = i * (1.0F - j);
            this.renderNauseaOverlay(context, k);
         }
      }
   }

   private void renderSleepOverlay(DrawContext context, RenderTickCounter tickCounter) {
      if (this.client.player.getSleepTimer() > 0) {
         Profilers.get().push("sleep");
         context.createNewRootLayer();
         float f = this.client.player.getSleepTimer();
         float g = f / 100.0F;
         if (g > 1.0F) {
            g = 1.0F - (f - 100.0F) / 10.0F;
         }

         int i = (int)(220.0F * g) << 24 | 1052704;
         context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), i);
         Profilers.get().pop();
      }
   }

   private void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter) {
      TextRenderer textRenderer = this.getTextRenderer();
      if (this.overlayMessage != null && this.overlayRemaining > 0) {
         Profilers.get().push("overlayMessage");
         float f = this.overlayRemaining - tickCounter.getTickProgress(false);
         int i = (int)(f * 255.0F / 20.0F);
         if (i > 255) {
            i = 255;
         }

         if (i > 0) {
            context.createNewRootLayer();
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(context.getScaledWindowWidth() / 2, context.getScaledWindowHeight() - 68);
            int j;
            if (this.overlayTinted) {
               j = MathHelper.hsvToArgb(f / 50.0F, 0.7F, 0.6F, i);
            } else {
               j = ColorHelper.whiteWithAlpha(i);
            }

            int k = textRenderer.getWidth(this.overlayMessage);
            context.drawTextWithBackground(textRenderer, this.overlayMessage, -k / 2, -4, k, j);
            context.getMatrices().popMatrix();
         }

         Profilers.get().pop();
      }
   }

   private void renderTitleAndSubtitle(DrawContext context, RenderTickCounter tickCounter) {
      if (this.title != null && this.titleRemainTicks > 0) {
         TextRenderer textRenderer = this.getTextRenderer();
         Profilers.get().push("titleAndSubtitle");
         float f = this.titleRemainTicks - tickCounter.getTickProgress(false);
         int i = 255;
         if (this.titleRemainTicks > this.titleFadeOutTicks + this.titleStayTicks) {
            float g = this.titleFadeInTicks + this.titleStayTicks + this.titleFadeOutTicks - f;
            i = (int)(g * 255.0F / this.titleFadeInTicks);
         }

         if (this.titleRemainTicks <= this.titleFadeOutTicks) {
            i = (int)(f * 255.0F / this.titleFadeOutTicks);
         }

         i = MathHelper.clamp(i, 0, 255);
         if (i > 0) {
            context.createNewRootLayer();
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(context.getScaledWindowWidth() / 2, context.getScaledWindowHeight() / 2);
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(4.0F, 4.0F);
            int j = textRenderer.getWidth(this.title);
            int k = ColorHelper.whiteWithAlpha(i);
            context.drawTextWithBackground(textRenderer, this.title, -j / 2, -10, j, k);
            context.getMatrices().popMatrix();
            if (this.subtitle != null) {
               context.getMatrices().pushMatrix();
               context.getMatrices().scale(2.0F, 2.0F);
               int l = textRenderer.getWidth(this.subtitle);
               context.drawTextWithBackground(textRenderer, this.subtitle, -l / 2, 5, l, k);
               context.getMatrices().popMatrix();
            }

            context.getMatrices().popMatrix();
         }

         Profilers.get().pop();
      }
   }

   private void renderChat(DrawContext context, RenderTickCounter tickCounter) {
      if (!this.chatHud.isChatFocused()) {
         Window window = this.client.getWindow();
         int i = MathHelper.floor(this.client.mouse.getScaledX(window));
         int j = MathHelper.floor(this.client.mouse.getScaledY(window));
         context.createNewRootLayer();
         this.chatHud.render(context, this.getTextRenderer(), this.ticks, i, j, false, false);
      }
   }

   private void renderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter) {
      Scoreboard scoreboard = this.client.world.getScoreboard();
      ScoreboardObjective scoreboardObjective = null;
      Team team = scoreboard.getScoreHolderTeam(this.client.player.getNameForScoreboard());
      if (team != null) {
         ScoreboardDisplaySlot scoreboardDisplaySlot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
         if (scoreboardDisplaySlot != null) {
            scoreboardObjective = scoreboard.getObjectiveForSlot(scoreboardDisplaySlot);
         }
      }

      ScoreboardObjective scoreboardObjective2 = scoreboardObjective != null
         ? scoreboardObjective
         : scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      if (scoreboardObjective2 != null) {
         context.createNewRootLayer();
         this.renderScoreboardSidebar(context, scoreboardObjective2);
      }
   }

   private void renderPlayerList(DrawContext context, RenderTickCounter tickCounter) {
      Scoreboard scoreboard = this.client.world.getScoreboard();
      ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
      if (!this.client.options.playerListKey.isPressed()
         || this.client.isInSingleplayer() && this.client.player.networkHandler.getListedPlayerListEntries().size() <= 1 && scoreboardObjective == null) {
         this.playerListHud.setVisible(false);
      } else {
         this.playerListHud.setVisible(true);
         context.createNewRootLayer();
         this.playerListHud.render(context, context.getScaledWindowWidth(), scoreboard, scoreboardObjective);
      }
   }

   private void renderCrosshair(DrawContext context, RenderTickCounter tickCounter) {
      GameOptions gameOptions = this.client.options;
      if (gameOptions.getPerspective().isFirstPerson()) {
         if (this.client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR || this.shouldRenderSpectatorCrosshair(this.client.crosshairTarget)) {
            if (!this.client.debugHudEntryList.isEntryVisible(DebugHudEntries.THREE_DIMENSIONAL_CROSSHAIR)) {
               context.createNewRootLayer();
               int i = 15;
               context.drawGuiTexture(
                  RenderPipelines.CROSSHAIR, CROSSHAIR_TEXTURE, (context.getScaledWindowWidth() - 15) / 2, (context.getScaledWindowHeight() - 15) / 2, 15, 15
               );
               if (this.client.options.getAttackIndicator().getValue() == AttackIndicator.CROSSHAIR) {
                  float f = this.client.player.getAttackCooldownProgress(0.0F);
                  boolean bl = false;
                  if (this.client.targetedEntity != null && this.client.targetedEntity instanceof LivingEntity && f >= 1.0F) {
                     bl = this.client.player.getAttackCooldownProgressPerTick() > 5.0F;
                     bl &= this.client.targetedEntity.isAlive();
                     AttackRangeComponent attackRangeComponent = this.client.player.getActiveOrMainHandStack().get(DataComponentTypes.ATTACK_RANGE);
                     bl &= attackRangeComponent == null || attackRangeComponent.isWithinRange(this.client.player, this.client.crosshairTarget.getPos());
                  }

                  int j = context.getScaledWindowHeight() / 2 - 7 + 16;
                  int k = context.getScaledWindowWidth() / 2 - 8;
                  if (bl) {
                     context.drawGuiTexture(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_FULL_TEXTURE, k, j, 16, 16);
                  } else if (f < 1.0F) {
                     int l = (int)(f * 17.0F);
                     context.drawGuiTexture(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_TEXTURE, k, j, 16, 4);
                     context.drawGuiTexture(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_PROGRESS_TEXTURE, 16, 4, 0, 0, k, j, l, 4);
                  }
               }
            }
         }
      }
   }

   private boolean shouldRenderSpectatorCrosshair(@Nullable HitResult hitResult) {
      if (hitResult == null) {
         return false;
      } else if (hitResult.getType() == HitResult.Type.ENTITY) {
         return ((EntityHitResult)hitResult).getEntity() instanceof NamedScreenHandlerFactory;
      } else if (hitResult.getType() == HitResult.Type.BLOCK) {
         BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
         World world = this.client.world;
         return world.getBlockState(blockPos).createScreenHandlerFactory(world, blockPos) != null;
      } else {
         return false;
      }
   }

   private void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter) {
      Collection<StatusEffectInstance> collection = this.client.player.getStatusEffects();
      if (!collection.isEmpty() && (this.client.currentScreen == null || !this.client.currentScreen.showsStatusEffects())) {
         int i = 0;
         int j = 0;

         for (StatusEffectInstance statusEffectInstance : Ordering.natural().reverse().sortedCopy(collection)) {
            RegistryEntry<StatusEffect> registryEntry = statusEffectInstance.getEffectType();
            if (statusEffectInstance.shouldShowIcon()) {
               int k = context.getScaledWindowWidth();
               int l = 1;
               if (this.client.isDemo()) {
                  l += 15;
               }

               if (registryEntry.value().isBeneficial()) {
                  i++;
                  k -= 25 * i;
               } else {
                  j++;
                  k -= 25 * j;
                  l += 26;
               }

               float f = 1.0F;
               if (statusEffectInstance.isAmbient()) {
                  context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_AMBIENT_TEXTURE, k, l, 24, 24);
               } else {
                  context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_TEXTURE, k, l, 24, 24);
                  if (statusEffectInstance.isDurationBelow(200)) {
                     int m = statusEffectInstance.getDuration();
                     int n = 10 - m / 20;
                     f = MathHelper.clamp(m / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
                        + MathHelper.cos(m * (float) Math.PI / 5.0F) * MathHelper.clamp(n / 10.0F * 0.25F, 0.0F, 0.25F);
                     f = MathHelper.clamp(f, 0.0F, 1.0F);
                  }
               }

               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, getEffectTexture(registryEntry), k + 3, l + 3, 18, 18, ColorHelper.getWhite(f));
            }
         }
      }
   }

   public static Identifier getEffectTexture(RegistryEntry<StatusEffect> effect) {
      return effect.getKey().map(RegistryKey::getValue).map(id -> id.withPrefixedPath("mob_effect/")).orElseGet(MissingSprite::getMissingSpriteId);
   }

   private void renderMainHud(DrawContext context, RenderTickCounter tickCounter) {
      if (this.client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
         this.spectatorHud.renderSpectatorMenu(context);
      } else {
         this.renderHotbar(context, tickCounter);
      }

      if (this.client.interactionManager.hasStatusBars()) {
         this.renderStatusBars(context);
      }

      this.renderMountHealth(context);
      InGameHud.BarType barType = this.getCurrentBarType();
      if (barType != this.currentBar.getKey()) {
         this.currentBar = Pair.of(barType, this.bars.get(barType).get());
      }

      ((Bar)this.currentBar.getValue()).renderBar(context, tickCounter);
      if (this.client.interactionManager.hasExperienceBar() && this.client.player.experienceLevel > 0) {
         Bar.drawExperienceLevel(context, this.client.textRenderer, this.client.player.experienceLevel);
      }

      ((Bar)this.currentBar.getValue()).renderAddons(context, tickCounter);
      if (this.client.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR) {
         this.renderHeldItemTooltip(context);
      } else if (this.client.player.isSpectator()) {
         this.spectatorHud.render(context);
      }
   }

   private void renderHotbar(DrawContext context, RenderTickCounter tickCounter) {
      PlayerEntity playerEntity = this.getCameraPlayer();
      if (playerEntity != null) {
         ItemStack itemStack = playerEntity.getOffHandStack();
         Arm arm = playerEntity.getMainArm().getOpposite();
         int i = context.getScaledWindowWidth() / 2;
         int j = 182;
         int k = 91;
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, i - 91, context.getScaledWindowHeight() - 22, 182, 22);
         context.drawGuiTexture(
            RenderPipelines.GUI_TEXTURED,
            HOTBAR_SELECTION_TEXTURE,
            i - 91 - 1 + playerEntity.getInventory().getSelectedSlot() * 20,
            context.getScaledWindowHeight() - 22 - 1,
            24,
            23
         );
         if (!itemStack.isEmpty()) {
            if (arm == Arm.LEFT) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_TEXTURE, i - 91 - 29, context.getScaledWindowHeight() - 23, 29, 24);
            } else {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_RIGHT_TEXTURE, i + 91, context.getScaledWindowHeight() - 23, 29, 24);
            }
         }

         int l = 1;

         for (int m = 0; m < 9; m++) {
            int n = i - 90 + m * 20 + 2;
            int o = context.getScaledWindowHeight() - 16 - 3;
            this.renderHotbarItem(context, n, o, tickCounter, playerEntity, playerEntity.getInventory().getStack(m), l++);
         }

         if (!itemStack.isEmpty()) {
            int m = context.getScaledWindowHeight() - 16 - 3;
            if (arm == Arm.LEFT) {
               this.renderHotbarItem(context, i - 91 - 26, m, tickCounter, playerEntity, itemStack, l++);
            } else {
               this.renderHotbarItem(context, i + 91 + 10, m, tickCounter, playerEntity, itemStack, l++);
            }
         }

         if (this.client.options.getAttackIndicator().getValue() == AttackIndicator.HOTBAR) {
            float f = this.client.player.getAttackCooldownProgress(0.0F);
            if (f < 1.0F) {
               int n = context.getScaledWindowHeight() - 20;
               int o = i + 91 + 6;
               if (arm == Arm.RIGHT) {
                  o = i - 91 - 22;
               }

               int p = (int)(f * 19.0F);
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_BACKGROUND_TEXTURE, o, n, 18, 18);
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_PROGRESS_TEXTURE, 18, 18, 0, 18 - p, o, n + 18 - p, 18, p);
            }
         }
      }
   }

   private void renderHeldItemTooltip(DrawContext context) {
      Profilers.get().push("selectedItemName");
      if (this.heldItemTooltipFade > 0 && !this.currentStack.isEmpty()) {
         MutableText mutableText = Text.empty().append(this.currentStack.getName()).formatted(this.currentStack.getRarity().getFormatting());
         if (this.currentStack.contains(DataComponentTypes.CUSTOM_NAME)) {
            mutableText.formatted(Formatting.ITALIC);
         }

         int i = this.getTextRenderer().getWidth(mutableText);
         int j = (context.getScaledWindowWidth() - i) / 2;
         int k = context.getScaledWindowHeight() - 59;
         if (!this.client.interactionManager.hasStatusBars()) {
            k += 14;
         }

         int l = (int)(this.heldItemTooltipFade * 256.0F / 10.0F);
         if (l > 255) {
            l = 255;
         }

         if (l > 0) {
            context.drawTextWithBackground(this.getTextRenderer(), mutableText, j, k, i, ColorHelper.whiteWithAlpha(l));
         }
      }

      Profilers.get().pop();
   }

   private void renderDemoTimer(DrawContext context, RenderTickCounter tickCounter) {
      if (this.client.isDemo()) {
         Profilers.get().push("demo");
         context.createNewRootLayer();
         Text text;
         if (this.client.world.getTime() >= 120500L) {
            text = DEMO_EXPIRED_MESSAGE;
         } else {
            text = Text.translatable(
               "demo.remainingTime", StringHelper.formatTicks((int)(120500L - this.client.world.getTime()), this.client.world.getTickManager().getTickRate())
            );
         }

         int i = this.getTextRenderer().getWidth(text);
         int j = context.getScaledWindowWidth() - i - 10;
         int k = 5;
         context.drawTextWithBackground(this.getTextRenderer(), text, j, 5, i, -1);
         Profilers.get().pop();
      }
   }

   private void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective) {
      Scoreboard scoreboard = objective.getScoreboard();
      NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED);

      @Environment(EnvType.CLIENT)
      record SidebarEntry(Text name, Text score, int scoreWidth) {
      }

      SidebarEntry[] sidebarEntrys = scoreboard.getScoreboardEntries(objective)
         .stream()
         .filter(score -> !score.hidden())
         .sorted(SCOREBOARD_ENTRY_COMPARATOR)
         .limit(15L)
         .map(scoreboardEntry -> {
            Team team = scoreboard.getScoreHolderTeam(scoreboardEntry.owner());
            Text textx = scoreboardEntry.name();
            Text text2 = Team.decorateName(team, textx);
            Text text3 = scoreboardEntry.formatted(numberFormat);
            int ix = this.getTextRenderer().getWidth(text3);
            return new SidebarEntry(text2, text3, ix);
         })
         .toArray(SidebarEntry[]::new);
      Text text = objective.getDisplayName();
      int i = this.getTextRenderer().getWidth(text);
      int j = i;
      int k = this.getTextRenderer().getWidth(": ");

      for (SidebarEntry sidebarEntry : sidebarEntrys) {
         j = Math.max(j, this.getTextRenderer().getWidth(sidebarEntry.name) + (sidebarEntry.scoreWidth > 0 ? k + sidebarEntry.scoreWidth : 0));
      }

      int m = sidebarEntrys.length;
      int n = m * 9;
      int o = context.getScaledWindowHeight() / 2 + n / 3;
      int p = 3;
      int q = context.getScaledWindowWidth() - j - 3;
      int r = context.getScaledWindowWidth() - 3 + 2;
      int s = this.client.options.getTextBackgroundColor(0.3F);
      int t = this.client.options.getTextBackgroundColor(0.4F);
      int u = o - m * 9;
      context.fill(q - 2, u - 9 - 1, r, u - 1, t);
      context.fill(q - 2, u - 1, r, o, s);
      context.drawText(this.getTextRenderer(), text, q + j / 2 - i / 2, u - 9, -1, false);

      for (int v = 0; v < m; v++) {
         SidebarEntry sidebarEntry2 = sidebarEntrys[v];
         int w = o - (m - v) * 9;
         context.drawText(this.getTextRenderer(), sidebarEntry2.name, q, w, -1, false);
         context.drawText(this.getTextRenderer(), sidebarEntry2.score, r - sidebarEntry2.scoreWidth, w, -1, false);
      }
   }

   private @Nullable PlayerEntity getCameraPlayer() {
      return this.client.getCameraEntity() instanceof PlayerEntity playerEntity ? playerEntity : null;
   }

   private @Nullable LivingEntity getRiddenEntity() {
      PlayerEntity playerEntity = this.getCameraPlayer();
      if (playerEntity != null) {
         Entity entity = playerEntity.getVehicle();
         if (entity == null) {
            return null;
         }

         if (entity instanceof LivingEntity) {
            return (LivingEntity)entity;
         }
      }

      return null;
   }

   private int getHeartCount(@Nullable LivingEntity entity) {
      if (entity != null && entity.isLiving()) {
         float f = entity.getMaxHealth();
         int i = (int)(f + 0.5F) / 2;
         if (i > 30) {
            i = 30;
         }

         return i;
      } else {
         return 0;
      }
   }

   private int getHeartRows(int heartCount) {
      return (int)Math.ceil(heartCount / 10.0);
   }

   private void renderStatusBars(DrawContext context) {
      PlayerEntity playerEntity = this.getCameraPlayer();
      if (playerEntity != null) {
         int i = MathHelper.ceil(playerEntity.getHealth());
         boolean bl = this.heartJumpEndTick > this.ticks && (this.heartJumpEndTick - this.ticks) / 3L % 2L == 1L;
         long l = Util.getMeasuringTimeMs();
         if (i < this.lastHealthValue && playerEntity.timeUntilRegen > 0) {
            this.lastHealthCheckTime = l;
            this.heartJumpEndTick = this.ticks + 20;
         } else if (i > this.lastHealthValue && playerEntity.timeUntilRegen > 0) {
            this.lastHealthCheckTime = l;
            this.heartJumpEndTick = this.ticks + 10;
         }

         if (l - this.lastHealthCheckTime > 1000L) {
            this.renderHealthValue = i;
            this.lastHealthCheckTime = l;
         }

         this.lastHealthValue = i;
         int j = this.renderHealthValue;
         this.random.setSeed(this.ticks * 312871);
         int k = context.getScaledWindowWidth() / 2 - 91;
         int m = context.getScaledWindowWidth() / 2 + 91;
         int n = context.getScaledWindowHeight() - 39;
         float f = Math.max((float)playerEntity.getAttributeValue(EntityAttributes.MAX_HEALTH), (float)Math.max(j, i));
         int o = MathHelper.ceil(playerEntity.getAbsorptionAmount());
         int p = MathHelper.ceil((f + o) / 2.0F / 10.0F);
         int q = Math.max(10 - (p - 2), 3);
         int r = n - 10;
         int s = -1;
         if (playerEntity.hasStatusEffect(StatusEffects.REGENERATION)) {
            s = this.ticks % MathHelper.ceil(f + 5.0F);
         }

         Profilers.get().push("armor");
         renderArmor(context, playerEntity, n, p, q, k);
         Profilers.get().swap("health");
         this.renderHealthBar(context, playerEntity, k, n, q, s, f, i, j, o, bl);
         LivingEntity livingEntity = this.getRiddenEntity();
         int t = this.getHeartCount(livingEntity);
         if (t == 0) {
            Profilers.get().swap("food");
            this.renderFood(context, playerEntity, n, m);
            r -= 10;
         }

         Profilers.get().swap("air");
         this.renderAirBubbles(context, playerEntity, t, r, m);
         Profilers.get().pop();
      }
   }

   private static void renderArmor(DrawContext context, PlayerEntity player, int y, int i, int healthBarLines, int x) {
      int j = player.getArmor();
      if (j > 0) {
         int k = y - (i - 1) * healthBarLines - 10;

         for (int l = 0; l < 10; l++) {
            int m = x + l * 8;
            if (l * 2 + 1 < j) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ARMOR_FULL_TEXTURE, m, k, 9, 9);
            }

            if (l * 2 + 1 == j) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ARMOR_HALF_TEXTURE, m, k, 9, 9);
            }

            if (l * 2 + 1 > j) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, ARMOR_EMPTY_TEXTURE, m, k, 9, 9);
            }
         }
      }
   }

   private void renderHealthBar(
      DrawContext context,
      PlayerEntity player,
      int x,
      int y,
      int lines,
      int regeneratingHeartIndex,
      float maxHealth,
      int lastHealth,
      int health,
      int absorption,
      boolean blinking
   ) {
      InGameHud.HeartType heartType = InGameHud.HeartType.fromPlayerState(player);
      boolean bl = player.getEntityWorld().getLevelProperties().isHardcore();
      int i = MathHelper.ceil(maxHealth / 2.0);
      int j = MathHelper.ceil(absorption / 2.0);
      int k = i * 2;

      for (int l = i + j - 1; l >= 0; l--) {
         int m = l / 10;
         int n = l % 10;
         int o = x + n * 8;
         int p = y - m * lines;
         if (lastHealth + absorption <= 4) {
            p += this.random.nextInt(2);
         }

         if (l < i && l == regeneratingHeartIndex) {
            p -= 2;
         }

         this.drawHeart(context, InGameHud.HeartType.CONTAINER, o, p, bl, blinking, false);
         int q = l * 2;
         boolean bl2 = l >= i;
         if (bl2) {
            int r = q - k;
            if (r < absorption) {
               boolean bl3 = r + 1 == absorption;
               this.drawHeart(context, heartType == InGameHud.HeartType.WITHERED ? heartType : InGameHud.HeartType.ABSORBING, o, p, bl, false, bl3);
            }
         }

         if (blinking && q < health) {
            boolean bl4 = q + 1 == health;
            this.drawHeart(context, heartType, o, p, bl, true, bl4);
         }

         if (q < lastHealth) {
            boolean bl4 = q + 1 == lastHealth;
            this.drawHeart(context, heartType, o, p, bl, false, bl4);
         }
      }
   }

   private void drawHeart(DrawContext context, InGameHud.HeartType type, int x, int y, boolean hardcore, boolean blinking, boolean half) {
      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, type.getTexture(hardcore, half, blinking), x, y, 9, 9);
   }

   private void renderAirBubbles(DrawContext context, PlayerEntity player, int heartCount, int top, int left) {
      int i = player.getMaxAir();
      int j = Math.clamp((long)player.getAir(), 0, i);
      boolean bl = player.isSubmergedIn(FluidTags.WATER);
      if (bl || j < i) {
         top = this.getAirBubbleY(heartCount, top);
         int k = getAirBubbles(j, i, -2);
         int l = getAirBubbles(j, i, 0);
         int m = 10 - getAirBubbles(j, i, getAirBubbleDelay(j, bl));
         boolean bl2 = k != l;
         if (!bl) {
            this.lastBurstBubble = 0;
         }

         for (int n = 1; n <= 10; n++) {
            int o = left - (n - 1) * 8 - 9;
            if (n <= k) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, AIR_TEXTURE, o, top, 9, 9);
            } else if (bl2 && n == l && bl) {
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, AIR_BURSTING_TEXTURE, o, top, 9, 9);
               this.playBurstSound(n, player, m);
            } else if (n > 10 - m) {
               int p = m == 10 && this.ticks % 2 == 0 ? this.random.nextInt(2) : 0;
               context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, AIR_EMPTY_TEXTURE, o, top + p, 9, 9);
            }
         }
      }
   }

   private int getAirBubbleY(int heartCount, int top) {
      int i = this.getHeartRows(heartCount) - 1;
      return top - i * 10;
   }

   private static int getAirBubbles(int air, int maxAir, int delay) {
      return MathHelper.ceil((float)((air + delay) * 10) / maxAir);
   }

   private static int getAirBubbleDelay(int air, boolean submergedInWater) {
      return air != 0 && submergedInWater ? 1 : 0;
   }

   private void playBurstSound(int bubble, PlayerEntity player, int burstBubbles) {
      if (this.lastBurstBubble != bubble) {
         float f = 0.5F + 0.1F * Math.max(0, burstBubbles - 3 + 1);
         float g = 1.0F + 0.1F * Math.max(0, burstBubbles - 5 + 1);
         player.playSound(SoundEvents.UI_HUD_BUBBLE_POP, f, g);
         this.lastBurstBubble = bubble;
      }
   }

   private void renderFood(DrawContext context, PlayerEntity player, int top, int right) {
      HungerManager hungerManager = player.getHungerManager();
      int i = hungerManager.getFoodLevel();

      for (int j = 0; j < 10; j++) {
         int k = top;
         Identifier identifier;
         Identifier identifier2;
         Identifier identifier3;
         if (player.hasStatusEffect(StatusEffects.HUNGER)) {
            identifier = FOOD_EMPTY_HUNGER_TEXTURE;
            identifier2 = FOOD_HALF_HUNGER_TEXTURE;
            identifier3 = FOOD_FULL_HUNGER_TEXTURE;
         } else {
            identifier = FOOD_EMPTY_TEXTURE;
            identifier2 = FOOD_HALF_TEXTURE;
            identifier3 = FOOD_FULL_TEXTURE;
         }

         if (player.getHungerManager().getSaturationLevel() <= 0.0F && this.ticks % (i * 3 + 1) == 0) {
            k = top + (this.random.nextInt(3) - 1);
         }

         int l = right - j * 8 - 9;
         context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, l, k, 9, 9);
         if (j * 2 + 1 < i) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier3, l, k, 9, 9);
         }

         if (j * 2 + 1 == i) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier2, l, k, 9, 9);
         }
      }
   }

   private void renderMountHealth(DrawContext context) {
      LivingEntity livingEntity = this.getRiddenEntity();
      if (livingEntity != null) {
         int i = this.getHeartCount(livingEntity);
         if (i != 0) {
            int j = (int)Math.ceil(livingEntity.getHealth());
            Profilers.get().swap("mountHealth");
            int k = context.getScaledWindowHeight() - 39;
            int l = context.getScaledWindowWidth() / 2 + 91;
            int m = k;

            for (int n = 0; i > 0; n += 20) {
               int o = Math.min(i, 10);
               i -= o;

               for (int p = 0; p < o; p++) {
                  int q = l - p * 8 - 9;
                  context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, VEHICLE_CONTAINER_HEART_TEXTURE, q, m, 9, 9);
                  if (p * 2 + 1 + n < j) {
                     context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, VEHICLE_FULL_HEART_TEXTURE, q, m, 9, 9);
                  }

                  if (p * 2 + 1 + n == j) {
                     context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, VEHICLE_HALF_HEART_TEXTURE, q, m, 9, 9);
                  }
               }

               m -= 10;
            }
         }
      }
   }

   private void renderOverlay(DrawContext context, Identifier texture, float opacity) {
      int i = ColorHelper.getWhite(opacity);
      context.drawTexture(
         RenderPipelines.GUI_TEXTURED,
         texture,
         0,
         0,
         0.0F,
         0.0F,
         context.getScaledWindowWidth(),
         context.getScaledWindowHeight(),
         context.getScaledWindowWidth(),
         context.getScaledWindowHeight(),
         i
      );
   }

   private void renderSpyglassOverlay(DrawContext context, float scale) {
      float f = Math.min(context.getScaledWindowWidth(), context.getScaledWindowHeight());
      float h = Math.min(context.getScaledWindowWidth() / f, context.getScaledWindowHeight() / f) * scale;
      int i = MathHelper.floor(f * h);
      int j = MathHelper.floor(f * h);
      int k = (context.getScaledWindowWidth() - i) / 2;
      int l = (context.getScaledWindowHeight() - j) / 2;
      int m = k + i;
      int n = l + j;
      context.drawTexture(RenderPipelines.GUI_TEXTURED, SPYGLASS_SCOPE, k, l, 0.0F, 0.0F, i, j, i, j);
      context.fill(RenderPipelines.GUI, 0, n, context.getScaledWindowWidth(), context.getScaledWindowHeight(), -16777216);
      context.fill(RenderPipelines.GUI, 0, 0, context.getScaledWindowWidth(), l, -16777216);
      context.fill(RenderPipelines.GUI, 0, l, k, n, -16777216);
      context.fill(RenderPipelines.GUI, m, l, context.getScaledWindowWidth(), n, -16777216);
   }

   private void updateVignetteDarkness(Entity entity) {
      BlockPos blockPos = BlockPos.ofFloored(entity.getX(), entity.getEyeY(), entity.getZ());
      float f = LightmapTextureManager.getBrightness(entity.getEntityWorld().getDimension(), entity.getEntityWorld().getLightLevel(blockPos));
      float g = MathHelper.clamp(1.0F - f, 0.0F, 1.0F);
      this.vignetteDarkness = this.vignetteDarkness + (g - this.vignetteDarkness) * 0.01F;
   }

   private void renderVignetteOverlay(DrawContext context, @Nullable Entity entity) {
      WorldBorder worldBorder = this.client.world.getWorldBorder();
      float f = 0.0F;
      if (entity != null) {
         float g = (float)worldBorder.getDistanceInsideBorder(entity);
         double d = Math.min(worldBorder.getShrinkingSpeed() * worldBorder.getWarningTime(), Math.abs(worldBorder.getSizeLerpTarget() - worldBorder.getSize()));
         double e = Math.max((double)worldBorder.getWarningBlocks(), d);
         if (g < e) {
            f = 1.0F - (float)(g / e);
         }
      }

      int i;
      if (f > 0.0F) {
         f = MathHelper.clamp(f, 0.0F, 1.0F);
         i = ColorHelper.fromFloats(1.0F, 0.0F, f, f);
      } else {
         float h = this.vignetteDarkness;
         h = MathHelper.clamp(h, 0.0F, 1.0F);
         i = ColorHelper.fromFloats(1.0F, h, h, h);
      }

      context.drawTexture(
         RenderPipelines.VIGNETTE,
         VIGNETTE_TEXTURE,
         0,
         0,
         0.0F,
         0.0F,
         context.getScaledWindowWidth(),
         context.getScaledWindowHeight(),
         context.getScaledWindowWidth(),
         context.getScaledWindowHeight(),
         i
      );
   }

   private void renderPortalOverlay(DrawContext context, float nauseaStrength) {
      if (nauseaStrength < 1.0F) {
         nauseaStrength *= nauseaStrength;
         nauseaStrength *= nauseaStrength;
         nauseaStrength = nauseaStrength * 0.8F + 0.2F;
      }

      int i = ColorHelper.getWhite(nauseaStrength);
      Sprite sprite = this.client.getBlockRenderManager().getModels().getModelParticleSprite(Blocks.NETHER_PORTAL.getDefaultState());
      context.drawSpriteStretched(RenderPipelines.GUI_TEXTURED, sprite, 0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), i);
   }

   private void renderNauseaOverlay(DrawContext context, float nauseaStrength) {
      int i = context.getScaledWindowWidth();
      int j = context.getScaledWindowHeight();
      context.getMatrices().pushMatrix();
      float f = MathHelper.lerp(nauseaStrength, 2.0F, 1.0F);
      context.getMatrices().translate(i / 2.0F, j / 2.0F);
      context.getMatrices().scale(f, f);
      context.getMatrices().translate(-i / 2.0F, -j / 2.0F);
      float g = 0.2F * nauseaStrength;
      float h = 0.4F * nauseaStrength;
      float k = 0.2F * nauseaStrength;
      context.drawTexture(RenderPipelines.GUI_NAUSEA_OVERLAY, NAUSEA_TEXTURE, 0, 0, 0.0F, 0.0F, i, j, i, j, ColorHelper.fromFloats(1.0F, g, h, k));
      context.getMatrices().popMatrix();
   }

   private void renderHotbarItem(DrawContext context, int x, int y, RenderTickCounter tickCounter, PlayerEntity player, ItemStack stack, int seed) {
      if (!stack.isEmpty()) {
         float f = stack.getBobbingAnimationTime() - tickCounter.getTickProgress(false);
         if (f > 0.0F) {
            float g = 1.0F + f / 5.0F;
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(x + 8, y + 12);
            context.getMatrices().scale(1.0F / g, (g + 1.0F) / 2.0F);
            context.getMatrices().translate(-(x + 8), -(y + 12));
         }

         context.drawItem(player, stack, x, y, seed);
         if (f > 0.0F) {
            context.getMatrices().popMatrix();
         }

         context.drawStackOverlay(this.client.textRenderer, stack, x, y);
      }
   }

   public void tick(boolean paused) {
      this.tickAutosaveIndicator();
      if (!paused) {
         this.tick();
      }
   }

   private void tick() {
      if (this.overlayRemaining > 0) {
         this.overlayRemaining--;
      }

      if (this.titleRemainTicks > 0) {
         this.titleRemainTicks--;
         if (this.titleRemainTicks <= 0) {
            this.title = null;
            this.subtitle = null;
         }
      }

      this.ticks++;
      Entity entity = this.client.getCameraEntity();
      if (entity != null) {
         this.updateVignetteDarkness(entity);
      }

      if (this.client.player != null) {
         ItemStack itemStack = this.client.player.getInventory().getSelectedStack();
         if (itemStack.isEmpty()) {
            this.heldItemTooltipFade = 0;
         } else if (this.currentStack.isEmpty() || !itemStack.isOf(this.currentStack.getItem()) || !itemStack.getName().equals(this.currentStack.getName())) {
            this.heldItemTooltipFade = (int)(40.0 * this.client.options.getNotificationDisplayTime().getValue());
         } else if (this.heldItemTooltipFade > 0) {
            this.heldItemTooltipFade--;
         }

         this.currentStack = itemStack;
      }

      this.chatHud.tickRemovalQueueIfExists();
   }

   private void tickAutosaveIndicator() {
      MinecraftServer minecraftServer = this.client.getServer();
      boolean bl = minecraftServer != null && minecraftServer.isSaving();
      this.lastAutosaveIndicatorAlpha = this.autosaveIndicatorAlpha;
      this.autosaveIndicatorAlpha = MathHelper.lerp(0.2F, this.autosaveIndicatorAlpha, bl ? 1.0F : 0.0F);
   }

   public void setRecordPlayingOverlay(Text description) {
      Text text = Text.translatable("record.nowPlaying", description);
      this.setOverlayMessage(text, true);
      this.client.getNarratorManager().narrateSystemImmediately(text);
   }

   public void setOverlayMessage(Text message, boolean tinted) {
      this.setCanShowChatDisabledScreen(false);
      this.overlayMessage = message;
      this.overlayRemaining = 60;
      this.overlayTinted = tinted;
   }

   public void setCanShowChatDisabledScreen(boolean canShowChatDisabledScreen) {
      this.canShowChatDisabledScreen = canShowChatDisabledScreen;
   }

   public boolean shouldShowChatDisabledScreen() {
      return this.canShowChatDisabledScreen && this.overlayRemaining > 0;
   }

   public void setTitleTicks(int fadeInTicks, int stayTicks, int fadeOutTicks) {
      if (fadeInTicks >= 0) {
         this.titleFadeInTicks = fadeInTicks;
      }

      if (stayTicks >= 0) {
         this.titleStayTicks = stayTicks;
      }

      if (fadeOutTicks >= 0) {
         this.titleFadeOutTicks = fadeOutTicks;
      }

      if (this.titleRemainTicks > 0) {
         this.titleRemainTicks = this.titleFadeInTicks + this.titleStayTicks + this.titleFadeOutTicks;
      }
   }

   public void setSubtitle(Text subtitle) {
      this.subtitle = subtitle;
   }

   public void setTitle(Text title) {
      this.title = title;
      this.titleRemainTicks = this.titleFadeInTicks + this.titleStayTicks + this.titleFadeOutTicks;
   }

   public void clearTitle() {
      this.title = null;
      this.subtitle = null;
      this.titleRemainTicks = 0;
   }

   public ChatHud getChatHud() {
      return this.chatHud;
   }

   public int getTicks() {
      return this.ticks;
   }

   public TextRenderer getTextRenderer() {
      return this.client.textRenderer;
   }

   public SpectatorHud getSpectatorHud() {
      return this.spectatorHud;
   }

   public PlayerListHud getPlayerListHud() {
      return this.playerListHud;
   }

   public void clear() {
      this.playerListHud.clear();
      this.bossBarHud.clear();
      this.client.getToastManager().clear();
      this.debugHud.clear();
      this.chatHud.clear(true);
      this.clearTitle();
      this.setDefaultTitleFade();
   }

   public BossBarHud getBossBarHud() {
      return this.bossBarHud;
   }

   public DebugHud getDebugHud() {
      return this.debugHud;
   }

   public void resetDebugHudChunk() {
      this.debugHud.resetChunk();
   }

   public void renderAutosaveIndicator(DrawContext context, RenderTickCounter tickCounter) {
      if (this.client.options.getShowAutosaveIndicator().getValue() && (this.autosaveIndicatorAlpha > 0.0F || this.lastAutosaveIndicatorAlpha > 0.0F)) {
         int i = MathHelper.floor(
            255.0F
               * MathHelper.clamp(MathHelper.lerp(tickCounter.getFixedDeltaTicks(), this.lastAutosaveIndicatorAlpha, this.autosaveIndicatorAlpha), 0.0F, 1.0F)
         );
         if (i > 0) {
            TextRenderer textRenderer = this.getTextRenderer();
            int j = textRenderer.getWidth(SAVING_LEVEL_TEXT);
            int k = ColorHelper.withAlpha(i, -1);
            int l = context.getScaledWindowWidth() - j - 5;
            int m = context.getScaledWindowHeight() - 9 - 5;
            context.createNewRootLayer();
            context.drawTextWithBackground(textRenderer, SAVING_LEVEL_TEXT, l, m, j, k);
         }
      }
   }

   private boolean shouldShowExperienceBar() {
      return this.client.player.experienceBarDisplayStartTime + 100 > this.client.player.age;
   }

   private boolean shouldShowJumpBar() {
      return this.client.player.getMountJumpStrength() > 0.0F
         || Nullables.mapOrElse(this.client.player.getJumpingMount(), JumpingMount::getJumpCooldown, 0) > 0;
   }

   private InGameHud.BarType getCurrentBarType() {
      boolean bl = this.client.player.networkHandler.getWaypointHandler().hasWaypoint();
      boolean bl2 = this.client.player.getJumpingMount() != null;
      boolean bl3 = this.client.interactionManager.hasExperienceBar();
      if (bl) {
         if (bl2 && this.shouldShowJumpBar()) {
            return InGameHud.BarType.JUMPABLE_VEHICLE;
         } else {
            return bl3 && this.shouldShowExperienceBar() ? InGameHud.BarType.EXPERIENCE : InGameHud.BarType.LOCATOR;
         }
      } else if (bl2) {
         return InGameHud.BarType.JUMPABLE_VEHICLE;
      } else {
         return bl3 ? InGameHud.BarType.EXPERIENCE : InGameHud.BarType.EMPTY;
      }
   }

   @Environment(EnvType.CLIENT)
   static enum BarType {
      EMPTY,
      EXPERIENCE,
      LOCATOR,
      JUMPABLE_VEHICLE;
   }

   @Environment(EnvType.CLIENT)
   static enum HeartType {
      CONTAINER(
         Identifier.ofVanilla("hud/heart/container"),
         Identifier.ofVanilla("hud/heart/container_blinking"),
         Identifier.ofVanilla("hud/heart/container"),
         Identifier.ofVanilla("hud/heart/container_blinking"),
         Identifier.ofVanilla("hud/heart/container_hardcore"),
         Identifier.ofVanilla("hud/heart/container_hardcore_blinking"),
         Identifier.ofVanilla("hud/heart/container_hardcore"),
         Identifier.ofVanilla("hud/heart/container_hardcore_blinking")
      ),
      NORMAL(
         Identifier.ofVanilla("hud/heart/full"),
         Identifier.ofVanilla("hud/heart/full_blinking"),
         Identifier.ofVanilla("hud/heart/half"),
         Identifier.ofVanilla("hud/heart/half_blinking"),
         Identifier.ofVanilla("hud/heart/hardcore_full"),
         Identifier.ofVanilla("hud/heart/hardcore_full_blinking"),
         Identifier.ofVanilla("hud/heart/hardcore_half"),
         Identifier.ofVanilla("hud/heart/hardcore_half_blinking")
      ),
      POISONED(
         Identifier.ofVanilla("hud/heart/poisoned_full"),
         Identifier.ofVanilla("hud/heart/poisoned_full_blinking"),
         Identifier.ofVanilla("hud/heart/poisoned_half"),
         Identifier.ofVanilla("hud/heart/poisoned_half_blinking"),
         Identifier.ofVanilla("hud/heart/poisoned_hardcore_full"),
         Identifier.ofVanilla("hud/heart/poisoned_hardcore_full_blinking"),
         Identifier.ofVanilla("hud/heart/poisoned_hardcore_half"),
         Identifier.ofVanilla("hud/heart/poisoned_hardcore_half_blinking")
      ),
      WITHERED(
         Identifier.ofVanilla("hud/heart/withered_full"),
         Identifier.ofVanilla("hud/heart/withered_full_blinking"),
         Identifier.ofVanilla("hud/heart/withered_half"),
         Identifier.ofVanilla("hud/heart/withered_half_blinking"),
         Identifier.ofVanilla("hud/heart/withered_hardcore_full"),
         Identifier.ofVanilla("hud/heart/withered_hardcore_full_blinking"),
         Identifier.ofVanilla("hud/heart/withered_hardcore_half"),
         Identifier.ofVanilla("hud/heart/withered_hardcore_half_blinking")
      ),
      ABSORBING(
         Identifier.ofVanilla("hud/heart/absorbing_full"),
         Identifier.ofVanilla("hud/heart/absorbing_full_blinking"),
         Identifier.ofVanilla("hud/heart/absorbing_half"),
         Identifier.ofVanilla("hud/heart/absorbing_half_blinking"),
         Identifier.ofVanilla("hud/heart/absorbing_hardcore_full"),
         Identifier.ofVanilla("hud/heart/absorbing_hardcore_full_blinking"),
         Identifier.ofVanilla("hud/heart/absorbing_hardcore_half"),
         Identifier.ofVanilla("hud/heart/absorbing_hardcore_half_blinking")
      ),
      FROZEN(
         Identifier.ofVanilla("hud/heart/frozen_full"),
         Identifier.ofVanilla("hud/heart/frozen_full_blinking"),
         Identifier.ofVanilla("hud/heart/frozen_half"),
         Identifier.ofVanilla("hud/heart/frozen_half_blinking"),
         Identifier.ofVanilla("hud/heart/frozen_hardcore_full"),
         Identifier.ofVanilla("hud/heart/frozen_hardcore_full_blinking"),
         Identifier.ofVanilla("hud/heart/frozen_hardcore_half"),
         Identifier.ofVanilla("hud/heart/frozen_hardcore_half_blinking")
      );

      private final Identifier fullTexture;
      private final Identifier fullBlinkingTexture;
      private final Identifier halfTexture;
      private final Identifier halfBlinkingTexture;
      private final Identifier hardcoreFullTexture;
      private final Identifier hardcoreFullBlinkingTexture;
      private final Identifier hardcoreHalfTexture;
      private final Identifier hardcoreHalfBlinkingTexture;

      private HeartType(
         final Identifier fullTexture,
         final Identifier fullBlinkingTexture,
         final Identifier halfTexture,
         final Identifier halfBlinkingTexture,
         final Identifier hardcoreFullTexture,
         final Identifier hardcoreFullBlinkingTexture,
         final Identifier hardcoreHalfTexture,
         final Identifier hardcoreHalfBlinkingTexture
      ) {
         this.fullTexture = fullTexture;
         this.fullBlinkingTexture = fullBlinkingTexture;
         this.halfTexture = halfTexture;
         this.halfBlinkingTexture = halfBlinkingTexture;
         this.hardcoreFullTexture = hardcoreFullTexture;
         this.hardcoreFullBlinkingTexture = hardcoreFullBlinkingTexture;
         this.hardcoreHalfTexture = hardcoreHalfTexture;
         this.hardcoreHalfBlinkingTexture = hardcoreHalfBlinkingTexture;
      }

      public Identifier getTexture(boolean hardcore, boolean half, boolean blinking) {
         if (!hardcore) {
            if (half) {
               return blinking ? this.halfBlinkingTexture : this.halfTexture;
            } else {
               return blinking ? this.fullBlinkingTexture : this.fullTexture;
            }
         } else if (half) {
            return blinking ? this.hardcoreHalfBlinkingTexture : this.hardcoreHalfTexture;
         } else {
            return blinking ? this.hardcoreFullBlinkingTexture : this.hardcoreFullTexture;
         }
      }

      static InGameHud.HeartType fromPlayerState(PlayerEntity player) {
         InGameHud.HeartType heartType;
         if (player.hasStatusEffect(StatusEffects.POISON)) {
            heartType = POISONED;
         } else if (player.hasStatusEffect(StatusEffects.WITHER)) {
            heartType = WITHERED;
         } else if (player.isFrozen()) {
            heartType = FROZEN;
         } else {
            heartType = NORMAL;
         }

         return heartType;
      }
   }

   @Environment(EnvType.CLIENT)
   public interface Renderable {
      void render(DrawContext context, RenderTickCounter tickCounter);
   }
}
